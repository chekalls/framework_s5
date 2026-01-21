package mg.miniframework.modules;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mg.miniframework.annotation.FormParam;
import mg.miniframework.annotation.RequestAttribute;
import mg.miniframework.annotation.UrlParam;

public class CachedMethodInfo {
    private final Method method;
    private final List<ParameterInfo> paramInfos;

    public CachedMethodInfo(Method method) {
        this.method = method;
        this.paramInfos = buildParamInfos(method);
    }

    private List<ParameterInfo> buildParamInfos(Method method) {
        Parameter[] parameters = method.getParameters();
        List<ParameterInfo> infos = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            ParameterInfo info = new ParameterInfo(param.getType(), param.getParameterizedType());

            UrlParam urlParam = param.getAnnotation(UrlParam.class);
            if (urlParam != null) {
                info.setUrlParamName(urlParam.name());
            }

            RequestAttribute reqAttr = param.getAnnotation(RequestAttribute.class);
            if (reqAttr != null) {
                info.setRequestParamName(reqAttr.paramName());
                info.setDefaultValue(reqAttr.defaultValue());
            }

            FormParam formParam = param.getAnnotation(FormParam.class);
            if (formParam != null) {
                info.setFormParamName(formParam.name());
                info.setRequired(formParam.required());
            }

            infos.add(info);
        }
        return infos;
    }

    public Method getMethod() {
        return method;
    }

    public List<ParameterInfo> getParamInfos() {
        return paramInfos;
    }

    public static class ParameterInfo {
        private final Class<?> type;
        private final java.lang.reflect.Type genericType;
        private String urlParamName;
        private String requestParamName;
        private String defaultValue;
        private String formParamName;
        private boolean required;

        public ParameterInfo(Class<?> type, java.lang.reflect.Type genericType) {
            this.type = type;
            this.genericType = genericType;
        }

        public Class<?> getType() { return type; }
        public java.lang.reflect.Type getGenericType() { return genericType; }
        public String getUrlParamName() { return urlParamName; }
        public void setUrlParamName(String urlParamName) { this.urlParamName = urlParamName; }
        public String getRequestParamName() { return requestParamName; }
        public void setRequestParamName(String requestParamName) { this.requestParamName = requestParamName; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        public String getFormParamName() { return formParamName; }
        public void setFormParamName(String formParamName) { this.formParamName = formParamName; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
    
}
}