package mg.miniframework.config;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mg.miniframework.annotation.Controller;
import mg.miniframework.annotation.UrlMap;
import mg.miniframework.modules.Url;

public class RouteMap {
    private Map<Class<?>, List<Method>> methodMaps;
    private Map<Url,Method> urlMethodsMap;

    public RouteMap(){
        methodMaps = new HashMap<>();
        urlMethodsMap = new HashMap<>();
    }

    public void addController(Class<?> controller) {
        Controller controllerAnnotation = controller.getAnnotation(Controller.class);
        String baseUrl = controllerAnnotation.mapping();
        
        List<Method> annotatedMethods = new ArrayList<>();
        for (Method m : controller.getDeclaredMethods()) {
            if (m.isAnnotationPresent(mg.miniframework.annotation.UrlMap.class)) {
                UrlMap urlMapAnnotation = m.getAnnotation(UrlMap.class);
                annotatedMethods.add(m);
                
                    String fullUrl = normalizeUrl(baseUrl, urlMapAnnotation.value());
                
                urlMethodsMap.put(new Url(fullUrl, urlMapAnnotation.method()), m);
            }
        }
        methodMaps.put(controller, annotatedMethods);
    }
    
    private String normalizeUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        String fullUrl = baseUrl + path;
        fullUrl = fullUrl.replaceAll("/+", "/");
        
        return fullUrl;
    }

    public Map<Class<?>, List<Method>> getMethodMaps() {
        return methodMaps;
    }

    public void setMethodMaps(Map<Class<?>, List<Method>> methodMaps) {
        this.methodMaps = methodMaps;
    }

    public Map<Url, Method> getUrlMethodsMap() {
        return urlMethodsMap;
    }

    public void setUrlMethodsMap(Map<Url, Method> urlMethodsMap) {
        this.urlMethodsMap = urlMethodsMap;
    }
}
