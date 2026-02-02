package mg.miniframework.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import jakarta.servlet.RequestDispatcher;
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
import mg.miniframework.modules.security.SecurityManager;
import mg.miniframework.modules.security.AuthenticationProvider;
import mg.miniframework.utils.RoutePatternUtils;

@WebServlet(name = "FrontControllerServlet", urlPatterns = "/*")
@MultipartConfig
public class FrontControllerServlet extends HttpServlet {

    private String baseFile;
    private MethodManager methodeManager;
    private LogManager logManager;
    private ConfigLoader configLoader;
    private ContentRenderManager contentRenderManager;
    private MetricsManager metricsManager;
    private SecurityManager securityManager;
    private String controllerPackage;

    @Override
    public void init() throws ServletException {
        this.methodeManager = new MethodManager();
        this.logManager = new LogManager();
        this.configLoader = new ConfigLoader();
        this.contentRenderManager = new ContentRenderManager();
        this.metricsManager = new MetricsManager();
        this.securityManager = new SecurityManager();

        ServletContext ctx = getServletContext();
        String userAttName = ctx.getInitParameter("security.user.attributeName");
        String rolesAttNAme = ctx.getInitParameter("security.role.attributeName");

        try {
            logManager.insertLog("user att name :" + userAttName, LogStatus.INFO);
            logManager.insertLog("roles att name :" + rolesAttNAme, LogStatus.INFO);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        securityManager.setConnectedUserVarName(userAttName);
        securityManager.setUserRolesVarName(rolesAttNAme);

        this.controllerPackage = ctx.getInitParameter("controller.package");
        try {
            logManager.insertLog("contoller base " + this.controllerPackage, LogStatus.INFO);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String securityEnabled = ctx.getInitParameter("security.enabled");
        if (securityEnabled != null) {
            securityManager.setEnabled(Boolean.parseBoolean(securityEnabled));
        }

        String loginUrl = ctx.getInitParameter("security.loginUrl");
        if (loginUrl != null && !loginUrl.isEmpty()) {
            securityManager.setLoginUrl(loginUrl);
        }

        String accessDeniedUrl = ctx.getInitParameter("security.accessDeniedUrl");
        if (accessDeniedUrl != null && !accessDeniedUrl.isEmpty()) {
            securityManager.setAccessDeniedUrl(accessDeniedUrl);
        }

        String authProviderClass = ctx.getInitParameter("security.authenticationProvider");
        if (authProviderClass != null && !authProviderClass.isEmpty()) {
            try {
                Class<?> providerClass = Class.forName(authProviderClass);
                AuthenticationProvider provider = (AuthenticationProvider) providerClass.getDeclaredConstructor()
                        .newInstance();
                securityManager.setAuthenticationProvider(provider);
                logManager.insertLog("Authentication provider loaded: " + authProviderClass, LogStatus.INFO);
            } catch (Exception e) {
                try {
                    logManager.insertLog("Failed to load authentication provider: " + e.getMessage(), LogStatus.ERROR);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        metricsManager.incrementRequestCount();

        ServletContext servletContext = req.getServletContext();
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String relativePath = requestURI.substring(contextPath.length());
        String realPath = servletContext.getRealPath(relativePath);

        if (relativePath.equals("/metrics")) {
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().print(metricsManager.exportMetrics());
            long duration = System.currentTimeMillis() - startTime;
            metricsManager.addRequestDuration(duration);
            return;
        }

        Map<String, String> requestData = Map.of("requestURI", requestURI, "contextPath", contextPath, "relativePath",
                relativePath, "realPath", realPath);

        logManager.insertLog("request data :" + contentRenderManager.convertToJson(requestData), LogStatus.DEBUG);

        try {
            if (realPath != null) {
                File resource = new File(realPath);
                if (resource.exists() && !resource.isDirectory()) {
                    if (relativePath.endsWith(".jsp")) {
                        RequestDispatcher jspDispatcher = servletContext.getNamedDispatcher("jsp");
                        if (jspDispatcher != null) {
                            req.setAttribute("jakarta.servlet.jsp.jspFile", relativePath);
                            jspDispatcher.forward(req, resp);
                        } else {
                            servletContext.getRequestDispatcher(relativePath).forward(req, resp);
                        }
                        return;
                    }

                    if (relativePath.matches(".*\\.(css|js|png|jpg|jpeg|gif)$")) {
                        RequestDispatcher defaultDispatcher = servletContext.getNamedDispatcher("default");
                        if (defaultDispatcher != null) {
                            req.setAttribute("jakarta.servlet.include.request_uri", relativePath);
                            req.setAttribute("jakarta.servlet.include.servlet_path", relativePath);
                            defaultDispatcher.forward(req, resp);
                        } else {
                            servletContext.getRequestDispatcher(relativePath).forward(req, resp);
                        }
                        return;
                    }
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
            if (mapSetting.containsKey("upload_path")) {
                methodeManager.setFileSavePath(mapSetting.get("upload_path"));
            }

            if (servletContext.getAttribute("rolePermissionLoader") == null) {
                mg.miniframework.modules.security.RolePermissionLoader loader = new mg.miniframework.modules.security.RolePermissionLoader();
                loader.loadFromConfig(mapSetting);
                servletContext.setAttribute("rolePermissionLoader", loader);
                securityManager.setRolePermissionLoader(loader);

                injectRolePermissionLoader(loader);

                try {
                    logManager.insertLog("Security roles and permissions loaded from config", LogStatus.INFO);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                mg.miniframework.modules.security.RolePermissionLoader loader = (mg.miniframework.modules.security.RolePermissionLoader) servletContext
                        .getAttribute("rolePermissionLoader");
                securityManager.setRolePermissionLoader(loader);

                injectRolePermissionLoader(loader);
            }

            if (servletContext.getAttribute("routeMap") == null) {
                RouteMap routeMap = new RouteMap();
                List<Class<?>> controllers = trouverClassesAvecAnnotation(Controller.class);
                for (Class<?> c : controllers) {
                    logManager.insertLog("===================================================================",
                            LogStatus.INFO);
                    logManager.insertLog("" + c.getSimpleName(), LogStatus.INFO);
                    logManager.insertLog("===================================================================",
                            LogStatus.INFO);
                    routeMap.addController(c);
                }
                servletContext.setAttribute("routeMap", routeMap);
            }

            RouteMap routeMap = (RouteMap) servletContext.getAttribute("routeMap");

            Url url = new Url();
            url.setMethod(Url.Method.valueOf(req.getMethod()));
            url.setUrlPath(relativePath);

            Map<Url, Pattern> routePatterns = new HashMap<>();

            for (Map.Entry<Url, CachedMethodInfo> entry : routeMap.getUrlMethodsMap().entrySet()) {
                routePatterns.put(
                        entry.getKey(),
                        RoutePatternUtils.convertRouteToPattern(entry.getKey().getUrlPath()));
            }

            // for (Map.Entry<Url, Method> e : routeMap.getUrlMethodsMap().entrySet()) {
            // routePatterns.put(
            // e.getKey(),
            // RoutePatternUtils.convertRouteToPattern(e.getKey().getUrlPath()));
            // }

            Integer status = gererRoutes(
                    url,
                    routePatterns,
                    routeMap.getUrlMethodsMap(),
                    req,
                    resp);

            if (status.equals(RouteStatus.NOT_FOUND.getCode())) {
                print404(req, resp, relativePath, req.getMethod(), routeMap);
            }

            if (status.equals(RouteStatus.RETURN_TYPE_UNKNOWN.getCode())) {
                metricsManager.incrementErrorCount();
            }

            long duration = System.currentTimeMillis() - startTime;
            metricsManager.addRequestDuration(duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            metricsManager.addRequestDuration(duration);
            metricsManager.incrementErrorCount();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("Erreur interne : " + e.getMessage());
        }
    }

    private Integer gererRoutes(
            Url requestURL,
            Map<Url, Pattern> routes,
            Map<Url, CachedMethodInfo> methodsMap,
            HttpServletRequest req,
            HttpServletResponse resp) throws IOException {

        PrintWriter out = resp.getWriter();

        for (Map.Entry<Url, Pattern> entry : routes.entrySet()) {
            Url routeURL = entry.getKey();

            if (entry.getValue().matcher(requestURL.getUrlPath()).matches()
                    && requestURL.getMethod() == routeURL.getMethod()) {

                try {
                    CachedMethodInfo cachedInfo = methodsMap.get(routeURL);
                    Method method = cachedInfo.getMethod();

                    if (!securityManager.isAccessAllowed(method, req, resp)) {
                        return RouteStatus.RETURN_TYPE_UNKNOWN.getCode();
                    }

                    Map<String, String> pathParams = RoutePatternUtils.extractPathParams(
                            routeURL.getUrlPath(),
                            requestURL.getUrlPath());

                    Object result = methodeManager.invokeCorrespondingMethod(
                            cachedInfo,
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

        metricsManager.incrementErrorCount();

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.setContentType("text/html;charset=UTF-8");

        PrintWriter out = resp.getWriter();
        out.println("<html><body>");
        out.println("<h1>404 - Page non trouvée</h1>");
        out.println("<p><b>" + httpMethod + "</b> " + urlPath + "</p>");
        out.println("<ul>");

        routeMap.getUrlMethodsMap()
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(
                        e -> e.getKey().getUrlPath(),
                        String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> {
                    out.println("<li>"
                            + entry.getKey().getMethod()
                            + " "
                            + entry.getKey().getUrlPath()
                            + "</li>");
                });
    }

    private List<Class<?>> trouverClassesAvecAnnotation(Class<?> annotationClass, String basePackage)
            throws IOException {
        List<Class<?>> result = new ArrayList<>();

        try (ScanResult scanResult = new ClassGraph()
                .enableAllInfo()
                .acceptPackages(basePackage)
                .scan()) {

            for (ClassInfo ci : scanResult.getClassesWithAnnotation(annotationClass.getName())) {
                try {
                    Class<?> clazz = ci.loadClass();
                    result.add(clazz);
                    logManager.insertLog("===================================================================",
                            LogStatus.INFO);
                    logManager.insertLog("" + clazz.getSimpleName(), LogStatus.INFO);
                    logManager.insertLog("===================================================================",
                            LogStatus.INFO);
                    logManager.insertLog("Controller found: " + clazz.getName(), LogStatus.INFO);
                } catch (Throwable e) {
                    logManager.insertLog(
                            "Erreur lors du chargement de " + ci.getName() + " : " + e,
                            LogStatus.ERROR);
                }
            }

        } catch (Throwable e) {
            logManager.insertLog("Erreur lors du scan des classes : " + e, LogStatus.ERROR);
        }

        return result;
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

    /**
     * Injecte le RolePermissionLoader dans l'AuthenticationProvider via réflexion
     * si le provider a une méthode setRolePermissionLoader
     */
    private void injectRolePermissionLoader(mg.miniframework.modules.security.RolePermissionLoader loader) {
        AuthenticationProvider provider = securityManager.getAuthenticationProvider();
        if (provider != null) {
            try {
                java.lang.reflect.Method setter = provider.getClass()
                        .getMethod("setRolePermissionLoader",
                                mg.miniframework.modules.security.RolePermissionLoader.class);
                setter.invoke(provider, loader);
                logManager.insertLog("RolePermissionLoader injected into AuthenticationProvider", LogStatus.DEBUG);
            } catch (NoSuchMethodException e) {
                // Le provider n'a pas de setter, ce n'est pas grave
                try {
                    logManager.insertLog("AuthenticationProvider does not have setRolePermissionLoader method",
                            LogStatus.DEBUG);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (Exception e) {
                try {
                    logManager.insertLog("Failed to inject RolePermissionLoader: " + e.getMessage(), LogStatus.WARN);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
