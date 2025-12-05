package mg.miniframework.modules;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.miniframework.annotation.RequestAttribute;
import mg.miniframework.annotation.UrlParam;
import mg.miniframework.modules.LogManager.LogStatus;
import mg.miniframework.utils.DataTypeUtils;

public class MethodManager {

    private LogManager logManager;

    public MethodManager() {
        logManager = new LogManager();
    }

    @Deprecated
    private Object handleArrayType() {
        return null;
    }

    private Object getObjectInstanceFromRequest(Class<?> clazz, HttpServletRequest request)
            throws Exception {

        Field[] classFields = clazz.getDeclaredFields();
        String className = clazz.getSimpleName().toLowerCase();

        logManager.insertLog("class name : " + className, LogStatus.DEBUG);
        logManager.insertLog("==== found " + classFields.length + " fields", LogStatus.DEBUG);

        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();

        for (int n = 0; n < classFields.length; n++) {
            Field field = classFields[n];
            field.setAccessible(true);

            if (!DataTypeUtils.isArrayType(field.getType())) {
                String attributeName = className + "." + field.getName();
                attributeName = attributeName.strip();
                logManager.insertLog(
                        "---- object parameter found :[" + field.getName() + " ::'" + field.getType().getName()
                                + "'] => " + attributeName,
                        LogStatus.DEBUG);

                String fieldValue = request.getParameter(attributeName);

                if (fieldValue != null && !fieldValue.isEmpty()) {
                    Object converted = DataTypeUtils.convertParam(fieldValue, field.getType());
                    field.set(instance, converted);
                }
            } else {
                ArrayList<Object> valueList = new ArrayList<>();
                logManager.insertLog("array attributes found : "+field.getName()+" :: "+field.getType(), LogStatus.DEBUG);
                for (Enumeration<String> paramEnumeration = request.getParameterNames(); paramEnumeration
                        .hasMoreElements();) {
                    String paramName = paramEnumeration.nextElement();

                    String attributeBaseName = className + "." + field.getName() + "[";
                    attributeBaseName = attributeBaseName.strip();
                    if (paramName.startsWith(attributeBaseName)) {
                        int indexStart = paramName.indexOf('[') + 1;
                        int indexEnd = paramName.indexOf(']');
                        int index = Integer.parseInt(paramName.substring(indexStart, indexEnd));
                        String attributeName = className+"."+field.getName()+"["+index+"]";
                        logManager.insertLog("===== param name : "+attributeName, LogStatus.DEBUG);
                        String fieldValue = request.getParameter(attributeName);
                        Object converted = DataTypeUtils.convertParam(fieldValue,DataTypeUtils.getContentType(field));
                        valueList.add(index, converted);
                        logManager.insertLog("===== values "+index+" : "+fieldValue+" :: "+DataTypeUtils.getContentType(field).getSimpleName() , LogStatus.DEBUG);
                    }
                }
                field.set(instance, DataTypeUtils.convertListToTargetType(valueList, field.getType(), DataTypeUtils.getContentType(field)));
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
        PrintWriter writer = resp.getWriter();

        Map<String, Object> mapParameters = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();

        while (parameterNames.hasMoreElements()) {
            String param = parameterNames.nextElement();
            mapParameters.put(param, request.getParameter(param));
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

                String parameter = request.getParameter(param.getName());

                if (parameter != null && parameter != "") {
                    rawValue = parameter;
                    continue;
                }

                if (param.getType().equals(Map.class)) {
                    boolean ok = true;
                    for (Map.Entry<?, ?> e : mapParameters.entrySet()) {
                        if (!(e.getKey() instanceof String)) {
                            ok = false;
                            break;
                        }
                    }

                    if (ok) {
                        args[i] = mapParameters;
                        continue;
                    }
                }

                args[i] = getObjectInstanceFromRequest(param.getType(), request);
                continue;
            }

            args[i] = DataTypeUtils.convertParam(rawValue, param.getType());
        }

        return method.invoke(instance, args);
    }
}
