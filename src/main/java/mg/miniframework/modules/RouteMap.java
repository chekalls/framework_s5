package mg.miniframework.modules;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mg.miniframework.annotation.Controller;
import mg.miniframework.annotation.GetMapping;
import mg.miniframework.annotation.PostMapping;
import mg.miniframework.annotation.UrlMap;

public class RouteMap {

    private Map<Class<?>, List<Method>> methodMaps;
    private Map<Url, Method> urlMethodsMap;

    public RouteMap() {
        methodMaps = new HashMap<>();
        urlMethodsMap = new HashMap<>();
    }

    public void addController(Class<?> controller) throws Exception {
        Controller controllerAnnotation = controller.getAnnotation(Controller.class);

        if (controllerAnnotation == null) {
            throw new IllegalArgumentException(
                    "La classe " + controller.getName() + " n'est pas annotée avec @Controller.");
        }

        String baseUrl = controllerAnnotation.mapping();

        List<Method> annotatedMethods = new ArrayList<>();


        for (Method m : controller.getDeclaredMethods()) {
            if (m.isAnnotationPresent(mg.miniframework.annotation.UrlMap.class)) {
                UrlMap urlMapAnnotation = m.getAnnotation(UrlMap.class);
                annotatedMethods.add(m);

                String fullUrl = normalizeUrl(baseUrl, urlMapAnnotation.value());

                Url newUrl = new Url();
                newUrl.setUrlPath(fullUrl);
                if (isAMapping(m, newUrl)) {
                    urlMethodsMap.put(newUrl, m);
                }
            }
        }
        methodMaps.put(controller, annotatedMethods);
    }

    private boolean isAMapping(Method method, Url newUrl) throws Exception{
        Map<Class<? extends Annotation>, Url.Method> mappingList = Map.of(
                PostMapping.class, Url.Method.POST,
                GetMapping.class, Url.Method.GET);

        for (var mapping : mappingList.entrySet()) {
            if(method.getAnnotation(mapping.getKey())!=null){
                newUrl.setMethod(mapping.getValue());
                return true;
            }
        }
        return false;
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
