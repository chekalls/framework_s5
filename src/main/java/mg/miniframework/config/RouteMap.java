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
    }

    public void addController(Class<?> controller) {
        Controller controllerAnnotation = controller.getAnnotation(Controller.class);
        String baseUrl = controllerAnnotation.mapping();
        
        List<Method> annotatedMethods = new ArrayList<>();
        for (Method m : controller.getDeclaredMethods()) {
            if (m.isAnnotationPresent(mg.miniframework.annotation.UrlMap.class)) {
                UrlMap urlMapAnnotation = m.getAnnotation(UrlMap.class);
                annotatedMethods.add(m);
                urlMethodsMap.put(new Url(baseUrl+urlMapAnnotation.value(),urlMapAnnotation.method()), m);
            }
        }
        methodMaps.put(controller, annotatedMethods);
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
