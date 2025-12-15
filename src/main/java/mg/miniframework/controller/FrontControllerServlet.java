package mg.miniframework.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import mg.miniframework.annotation.Controller;
import mg.miniframework.annotation.JsonUrl;
import mg.miniframework.modules.*;
import mg.miniframework.modules.LogManager.LogStatus;
import mg.miniframework.utils.RoutePatternUtils;

@WebServlet(name = "FrontControllerServlet", urlPatterns = "/*")
@MultipartConfig
public class FrontControllerServlet extends HttpServlet {

    private String baseFile;
    private MethodManager methodeManager;
    private LogManager logManager;
    private ConfigLoader configLoader;
    private ContentRenderManager contentRenderManager;

    @Override
    public void init() throws ServletException {
        this.methodeManager = new MethodManager();
        this.logManager = new LogManager();
        this.configLoader = new ConfigLoader();
        this.contentRenderManager = new ContentRenderManager();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        ServletContext servletContext = req.getServletContext();
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String relativePath = requestURI.substring(contextPath.length());
        String realPath = servletContext.getRealPath(relativePath);

        try {
            if (realPath != null) {
                File resource = new File(realPath);
                if (resource.exists() && !resource.isDirectory() &&
                        relativePath.matches(".*\\.(jsp|css|js|png|jpg|jpeg|gif)$")) {
                    req.getRequestDispatcher(relativePath).forward(req, resp);
                    return;
                }
            }
            if (servletContext.getAttribute("settingMap") == null) {
                Map<String, String> settings = configLoader.getAllProperties(servletContext);
                settings.forEach((k, v) -> {
                    try {
                        logManager.insertLog("config found [" + k + ":" + v + "]", LogStatus.INFO);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                servletContext.setAttribute("settingMap", settings);
            }

            Map<String, String> mapSetting = (Map<String, String>) servletContext.getAttribute("settingMap");

            if (mapSetting.containsKey("jsp_base_path")) {
                baseFile = mapSetting.get("jsp_base_path");
                contentRenderManager.setBaseJspPath(baseFile);
            }

            if (servletContext.getAttribute("routeMap") == null) {
                RouteMap routeMap = new RouteMap();
                List<Class<?>> controllers = trouverClassesAvecAnnotation(Controller.class);
                for (Class<?> c : controllers) {
                    routeMap.addController(c);
                }
                servletContext.setAttribute("routeMap", routeMap);
            }

            RouteMap routeMap = (RouteMap) servletContext.getAttribute("routeMap");

            Url url = new Url();
            url.setMethod(Url.Method.valueOf(req.getMethod()));
            url.setUrlPath(relativePath);

            Map<Url, Pattern> routePatterns = new HashMap<>();
            for (Map.Entry<Url, Method> e : routeMap.getUrlMethodsMap().entrySet()) {
                routePatterns.put(
                        e.getKey(),
                        RoutePatternUtils.convertRouteToPattern(e.getKey().getUrlPath()));
            }

            Integer status = gererRoutes(
                    url,
                    routePatterns,
                    routeMap.getUrlMethodsMap(),
                    req,
                    resp);

            if (status.equals(RouteStatus.NOT_FOUND.getCode())) {
                print404(req, resp, relativePath, req.getMethod(), routeMap);
            }

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("Erreur interne : " + e.getMessage());
        }
    }

    private Integer gererRoutes(
            Url requestURL,
            Map<Url, Pattern> routes,
            Map<Url, Method> methodsMap,
            HttpServletRequest req,
            HttpServletResponse resp) throws IOException {

        PrintWriter out = resp.getWriter();

        for (Map.Entry<Url, Pattern> entry : routes.entrySet()) {
            Url routeURL = entry.getKey();

            if (entry.getValue().matcher(requestURL.getUrlPath()).matches()
                    && requestURL.getMethod() == routeURL.getMethod()) {

                try {
                    Method method = methodsMap.get(routeURL);

                    Map<String, String> pathParams = RoutePatternUtils.extractPathParams(
                            routeURL.getUrlPath(),
                            requestURL.getUrlPath());

                    Object result = methodeManager.invokeCorrespondingMethod(
                            method,
                            method.getDeclaringClass(),
                            pathParams,
                            req,
                            resp);

                    if (method.isAnnotationPresent(JsonUrl.class)) {
                        resp.setContentType("application/json;charset=UTF-8");
                        out.print(contentRenderManager.convertToJson(result));
                        out.flush();
                        return RouteStatus.RETURN_JSON.getCode();
                    }

                    return contentRenderManager.renderContent(result, req, resp);
                    // return RouteStatus.NOT_FOUND.getCode();

                } catch (Exception e) {
                    out.print("Erreur interne : " + e.getMessage());
                    return RouteStatus.RETURN_TYPE_UNKNOWN.getCode();
                }
            }
        }

        return RouteStatus.NOT_FOUND.getCode();
    }

    private void print404(HttpServletRequest req, HttpServletResponse resp,
            String urlPath, String httpMethod,
            RouteMap routeMap) throws IOException {

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.setContentType("text/html;charset=UTF-8");

        PrintWriter out = resp.getWriter();
        out.println("<html><body>");
        out.println("<h1>404 - Page non trouvée</h1>");
        out.println("<p><b>" + httpMethod + "</b> " + urlPath + "</p>");
        out.println("<ul>");

        for (Map.Entry<Url, Method> e : routeMap.getUrlMethodsMap().entrySet()) {
            out.println("<li>" + e.getKey().getMethod()
                    + " " + e.getKey().getUrlPath() + "</li>");
        }

        out.println("</ul></body></html>");
    }

    private List<Class<?>> trouverClassesAvecAnnotation(Class<?> annotationClass)
            throws Exception {

        List<Class<?>> result = new ArrayList<>();
        Path basePath = Paths.get(getClass().getClassLoader().getResource("").getPath());

        Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {

                if (file.toString().endsWith(".class")) {
                    String className = basePath.relativize(file)
                            .toString()
                            .replace(File.separatorChar, '.')
                            .replace(".class", "");

                    try {
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(
                                annotationClass.asSubclass(java.lang.annotation.Annotation.class))) {
                            result.add(clazz);
                            logManager.insertLog("Controller found: " + className, LogStatus.INFO);
                        }
                    } catch (Throwable ignored) {
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return result;
    }
}
