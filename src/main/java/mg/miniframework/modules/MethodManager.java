package mg.miniframework.modules;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import mg.miniframework.annotation.RequestAttribute;
import mg.miniframework.annotation.UrlParam;
import mg.miniframework.modules.LogManager.LogStatus;
import mg.miniframework.utils.DataTypeUtils;

public class MethodManager {

    private LogManager logManager;
    private String fileSavePath;

    public MethodManager() {
        logManager = new LogManager();
        fileSavePath = new String();
    }

    private Object getObjectInstanceFromRequest(Class<?> clazz, HttpServletRequest request, String prefix)
            throws Exception {

        if (DataTypeUtils.isArrayType(clazz)) {
            logManager.insertLog("array type parameter found : " + clazz.getSimpleName(), LogStatus.DEBUG);
            logManager.insertLog("unsupported function parameter : " + clazz.getName(), LogStatus.ERROR);
            return null;
        }

        Field[] classFields = clazz.getDeclaredFields();
        String className = clazz.getSimpleName().toLowerCase();

        logManager.insertLog("class name : " + className, LogStatus.DEBUG);
        logManager.insertLog("==== found " + classFields.length + " fields", LogStatus.DEBUG);

        String basePrefix = (prefix == null || prefix.isEmpty()) ? className : prefix;

        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();

        for (int n = 0; n < classFields.length; n++) {
            Field field = classFields[n];
            field.setAccessible(true);

            if (!DataTypeUtils.isArrayType(field.getType())) {
                String attributeName = (basePrefix + "." + field.getName()).strip();
                logManager.insertLog(
                        "---- object parameter found :[" + field.getName() + " ::'" + field.getType().getName()
                                + "'] => " + attributeName,
                        LogStatus.DEBUG);

                String fieldValue = request.getParameter(attributeName);

                if (fieldValue != null && !fieldValue.isEmpty()) {
                    Object converted = DataTypeUtils.convertParam(fieldValue, field.getType());
                    field.set(instance, converted);
                } else if (!DataTypeUtils.isPrimitiveOrWrapper(field.getType())
                        && !field.getType().equals(String.class)) {
                    Object subObject = getObjectInstanceFromRequest(field.getType(), request, attributeName);
                    field.set(instance, subObject);
                }
            } else {
                logManager.insertLog("class foundd ===>" + DataTypeUtils.getContentType(field).getSimpleName(),
                        LogStatus.DEBUG);
                if (DataTypeUtils.isPrimitiveOrWrapper(DataTypeUtils.getContentType(field))) {
                    logManager.insertLog("is primitive", LogStatus.DEBUG);

                    ArrayList<Object> valueList = new ArrayList<>();
                    logManager.insertLog("array attributes found : " + field.getName() + " :: " + field.getType(),
                            LogStatus.DEBUG);
                    for (Enumeration<String> paramEnumeration = request.getParameterNames(); paramEnumeration
                            .hasMoreElements();) {
                        String paramName = paramEnumeration.nextElement();

                        String attributeBaseName = (basePrefix + "." + field.getName() + "[").strip();
                        if (paramName.startsWith(attributeBaseName)) {
                            int indexStart = paramName.indexOf('[') + 1;
                            int indexEnd = paramName.indexOf(']');
                            int index = Integer.parseInt(paramName.substring(indexStart, indexEnd));
                            String attributeName = basePrefix + "." + field.getName() + "[" + index + "]";
                            logManager.insertLog("===== param name : " + attributeName, LogStatus.DEBUG);
                            String fieldValue = request.getParameter(attributeName);
                            Object converted = DataTypeUtils.convertParam(fieldValue,
                                    DataTypeUtils.getContentType(field));
                            valueList.add(index, converted);
                            logManager.insertLog("===== values " + index + " : " + fieldValue + " :: "
                                    + DataTypeUtils.getContentType(field).getSimpleName(), LogStatus.DEBUG);
                        }
                    }
                    field.set(instance, DataTypeUtils.convertListToTargetType(valueList, field.getType(),
                            DataTypeUtils.getContentType(field)));
                } else {
                    ArrayList<Object> valueList = new ArrayList<>();
                    String attributeBaseName = (basePrefix + "." + field.getName() + "[").strip();

                    ArrayList<Integer> indices = new ArrayList<>();
                    for (Enumeration<String> paramEnumeration = request.getParameterNames(); paramEnumeration
                            .hasMoreElements();) {
                        String paramName = paramEnumeration.nextElement();
                        if (paramName.startsWith(attributeBaseName)) {
                            int indexStart = paramName.indexOf('[') + 1;
                            int indexEnd = paramName.indexOf(']');
                            if (indexStart > 0 && indexEnd > indexStart) {
                                try {
                                    int index = Integer.parseInt(paramName.substring(indexStart, indexEnd));
                                    if (!indices.contains(index)) {
                                        indices.add(index);
                                    }
                                } catch (NumberFormatException ex) {
                                    // ignore malformed index
                                }
                            }
                        }
                    }

                    Class<?> contentType = DataTypeUtils.getContentType(field);
                    for (Integer idx : indices) {
                        String elementPrefix = basePrefix + "." + field.getName() + "[" + idx + "]";
                        Object elementInstance = getObjectInstanceFromRequest(contentType, request, elementPrefix);
                        while (valueList.size() <= idx) {
                            valueList.add(null);
                        }
                        valueList.set(idx, elementInstance);
                    }

                    field.set(instance, DataTypeUtils.convertListToTargetType(valueList, field.getType(),
                            DataTypeUtils.getContentType(field)));
                }
            }
        }

        return instance;
    }

    public Object invokeCorrespondingMethod(Method method, Class<?> clazz, Map<String, String> params,
            HttpServletRequest request,
            HttpServletResponse resp)
            throws Exception {

        Object instance = clazz.getDeclaredConstructor().newInstance();
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        Map<String, Object> mapParameters = new HashMap<>();
        Map<Path, byte[]> fileMap = new HashMap<>();
        Map<Path,mg.miniframework.modules.File> fileMap2 = new HashMap<>();

        boolean isMultipart = request.getContentType() != null
                && request.getContentType().toLowerCase().startsWith("multipart/");

        if (isMultipart) {
            Charset encoding = getRequestEncoding(request);
            for (Part part : request.getParts()) {
                String submittedName = part.getSubmittedFileName();
                boolean isFilePart = submittedName != null && !submittedName.isBlank();

                if (!isFilePart) {
                    mapParameters.put(part.getName(), readPartValue(part, encoding));
                    continue;
                }

                String uploadPath = "/public";
                String realUploadPath = request.getServletContext().getRealPath(uploadPath);
                Path uploadDir = Path.of(realUploadPath);
                Files.createDirectories(uploadDir);
                Path relativeUploadDir = Path.of(uploadPath);

                Path fileName = relativeUploadDir.resolve(submittedName);
                Path absoluteFileName = uploadDir.resolve(submittedName);
                if (fileName == null) {
                    continue;
                }

                logManager.insertLog("fileName rested " + fileName.toAbsolutePath(), LogStatus.DEBUG);

                byte[] content = readPartBytes(part);
                fileMap.put(fileName, content);
                mg.miniframework.modules.File uploadFile = new mg.miniframework.modules.File();
                uploadFile.setAbsolutePath(absoluteFileName);
                uploadFile.setContextPath(fileName);
                uploadFile.setContent(content);
                uploadFile.setLogManager(logManager);

                fileMap2.put(fileName, uploadFile);


                logManager.insertLog(
                        "uploaded file captured : " + fileName + " (" + content.length + " bytes)",
                        LogStatus.DEBUG);
            }
        } else {
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String param = parameterNames.nextElement();
                mapParameters.put(param, request.getParameter(param));
            }
        }

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String rawValue = null;

            UrlParam urlParamAnnotation = param.getAnnotation(UrlParam.class);
            RequestAttribute requestAttributeAnnotation = param.getAnnotation(RequestAttribute.class);
            if (urlParamAnnotation != null && requestAttributeAnnotation == null) {
                rawValue = params.getOrDefault(urlParamAnnotation.name(),
                        params.getOrDefault(param.getName(), null));

            } else if (urlParamAnnotation == null && requestAttributeAnnotation != null) {
                rawValue = request.getParameter(requestAttributeAnnotation.paramName());
                if (rawValue == null || rawValue.isEmpty()) {
                    rawValue = requestAttributeAnnotation.defaultValue();
                }
            } else if (urlParamAnnotation == null && requestAttributeAnnotation == null) {

                if (Map.class.isAssignableFrom(param.getType())) {

                    Type paramType = param.getParameterizedType();
                    args[i] =  DataTypeUtils.resolveMapForParameter(fileMap2,fileMap, paramType, mapParameters);
                    // args[i] = DataTypeUtils.resolveMapForParameter(paramType, fileMap, mapParameters);
                    continue;
                }
                args[i] = getObjectInstanceFromRequest(param.getType(), request, "");
                continue;
            }

            args[i] = DataTypeUtils.convertParam(rawValue, param.getType());
        }

        return method.invoke(instance, args);
    }

    private Charset getRequestEncoding(HttpServletRequest request) {
        String encoding = request.getCharacterEncoding();
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }

        try {
            return Charset.forName(encoding);
        } catch (Exception ex) {
            return StandardCharsets.UTF_8;
        }
    }

    private String readPartValue(Part part, Charset encoding) throws IOException {
        try (InputStream input = part.getInputStream()) {
            byte[] bytes = input.readAllBytes();
            return new String(bytes, encoding);
        }
    }

    private byte[] readPartBytes(Part part) throws IOException {
        try (InputStream input = part.getInputStream()) {
            return input.readAllBytes();
        }
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }

    public String getFileSavePath() {
        return fileSavePath;
    }

    public void setFileSavePath(String fileSavePath) {
        this.fileSavePath = fileSavePath;
    }
}
