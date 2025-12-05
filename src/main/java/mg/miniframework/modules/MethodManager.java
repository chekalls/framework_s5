package mg.miniframework.modules;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.http.HttpRequest;
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

    private Object getObjectInstanceFromRequest(Class<?> clazz, HttpServletRequest request)
            throws IOException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Field[] classFields = clazz.getDeclaredFields();
        logManager.insertLog("class name : " + clazz.getSimpleName(), LogStatus.DEBUG);
        logManager.insertLog("==== found " + classFields.length + " fields", LogStatus.DEBUG);

        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();

        for (int n = 0; n < classFields.length; n++) {
            Field field = classFields[n];
            field.setAccessible(true);

            logManager.insertLog(
                    "---- object parameter found :[" + field.getName() + " ::'" + field.getType().getName()
                            + "']",
                    LogStatus.DEBUG);

            String fieldValue = request.getParameter(field.getName());

            if (fieldValue != null && !fieldValue.isEmpty()) {
                Object converted = DataTypeUtils.convertParam(fieldValue, field.getType());
                field.set(instance, converted);
            }
        }

        return instance;
    }

    public Object invokeCorrespondingMethod(Method method, Class<?> clazz, Map<String, String> params,
            HttpServletRequest request,
            HttpServletResponse resp)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException, IOException {

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
