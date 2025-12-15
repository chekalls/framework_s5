package mg.miniframework.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.miniframework.annotation.Controller;
import mg.miniframework.annotation.JsonUrl;
import mg.miniframework.modules.ConfigLoader;
import mg.miniframework.modules.ContentRenderManager;
import mg.miniframework.modules.LogManager;
import mg.miniframework.modules.MethodManager;
import mg.miniframework.modules.ModelView;
import mg.miniframework.modules.RouteMap;
import mg.miniframework.modules.RouteStatus;
import mg.miniframework.modules.Url;
import mg.miniframework.modules.LogManager.LogStatus;
import mg.miniframework.utils.RoutePatternUtils;

@WebFilter(filterName = "resourceExistenceFilter", urlPatterns = "/*")
public class FilterServlet implements Filter {

	private String baseFile;
	private MethodManager methodeManager;
	private LogManager logManager;
	private ConfigLoader configLoader;
	private ContentRenderManager contentRenderManager;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		methodeManager = new MethodManager();
		this.logManager = new LogManager();
		this.configLoader = new ConfigLoader();
		this.contentRenderManager = new ContentRenderManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		baseFile = "";
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		ServletContext servletContext = req.getServletContext();

		String requestURI = req.getRequestURI();
		String contextPath = req.getContextPath();
		String relativePath = requestURI.substring(contextPath.length());
		String realPath = req.getServletContext().getRealPath(relativePath);

		try {
			if (realPath != null) {
				File resource = new File(realPath);
				if (resource.exists() && !resource.isDirectory() &&
						(relativePath.endsWith(".jsp")
								|| relativePath.endsWith(".css")
								|| relativePath.endsWith(".js")
								|| relativePath.endsWith(".png")
								|| relativePath.endsWith(".jpg")
								|| relativePath.endsWith(".jpeg")
								|| relativePath.endsWith(".gif"))) {

					chain.doFilter(request, response);
					return;
				}
			}

			if (servletContext.getAttribute("settingMap") == null) {
				Map<String, String> setContainer = configLoader.getAllProperties(servletContext);
				for (Map.Entry<String, String> element : setContainer.entrySet()) {
					logManager.insertLog("config found [" + element.getKey() + ":" + element.getValue() + "]",
							LogStatus.INFO);
				}
				servletContext.setAttribute("settingMap", setContainer);
			}

			Map<String, String> mapSetting = (Map<String, String>) servletContext.getAttribute("settingMap");
			if (mapSetting.containsKey("jsp_base_path")) {
				baseFile = mapSetting.get("jsp_base_path");
				this.contentRenderManager.setBaseJspPath(baseFile);

			}

			if (servletContext.getAttribute("routeMap") == null) {
				RouteMap routeMap = new RouteMap();
				try {
					List<Class<?>> controllers = trouverClassesAvecAnnotation(Controller.class);

					for (Class<?> class1 : controllers) {
						routeMap.addController(class1);
					}
					servletContext.setAttribute("routeMap", routeMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			RouteMap routeMap = (RouteMap) servletContext.getAttribute("routeMap");

			String urlPath = requestURI.substring(contextPath.length());
			Url url = new Url();
			url.setMethod(Url.Method.valueOf(req.getMethod()));
			url.setUrlPath(urlPath);

			Map<Url, Pattern> routePattern = new HashMap<>();
			for (Map.Entry<Url, Method> entry : routeMap.getUrlMethodsMap().entrySet()) {
				routePattern.put(entry.getKey(), RoutePatternUtils.convertRouteToPattern(entry.getKey().getUrlPath()));
			}

			Integer status = gererRoutes(url, routePattern, routeMap.getUrlMethodsMap(), req, resp);

			if (status.equals(RouteStatus.NOT_FOUND.getCode())) {
				print404(req, resp, urlPath, req.getMethod(), routeMap);
				return;
			}

			return;

		} catch (Exception e) {
			PrintWriter out = resp.getWriter();
			out.print(e.getMessage());
		}

		chain.doFilter(req, resp);
	}

	private void print404(HttpServletRequest req, HttpServletResponse resp, String urlPath, String httpMethod,
			RouteMap routeMap)
			throws IOException {

		resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		resp.setContentType("text/html;charset=UTF-8");

		PrintWriter out = resp.getWriter();

		out.println("<html><body>");
		out.println("<h1>404 - Page non trouvée</h1>");
		out.println("<p>L'URL <b>" + urlPath + "</b> avec la méthode <b>" + httpMethod + "</b> n'existe pas.</p>");

		out.println("<h3>Routes disponibles :</h3>");
		out.println("<ul>");

		for (Map.Entry<Url, Method> entry : routeMap.getUrlMethodsMap().entrySet()) {

			String methodHttp = entry.getKey().getMethod().toString();
			String path = entry.getKey().getUrlPath();
			Method methodObj = entry.getValue();

			// Crée un lien cliquable vers la route
			String link = "<a href=\"" + req.getContextPath() + path + "\">" + path + "</a>";

			out.println("<li>"
					+ "<b>" + methodHttp + "</b> : "
					+ link
					+ " → "
					+ methodObj.getDeclaringClass().getSimpleName() + "."
					+ methodObj.getName() + "()"
					+ "</li>");
		}

		out.println("</ul>");

		out.println("<p>Total : " + routeMap.getUrlMethodsMap().size() + "</p>");
		out.println("</body></html>");
	}

	private List<Class<?>> trouverClassesAvecAnnotation(Class<?> annotationClass) throws Exception {
		List<Class<?>> resultat = new ArrayList<>();
		String classesPath = getClass().getClassLoader().getResource("").getPath();
		logManager.insertLog("classes base path found :" + classesPath, LogStatus.INFO);
		Path basePath = Paths.get(classesPath);

		Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.toString().endsWith(".class")) {
					String relativePath = basePath.relativize(file).toString();
					String className = relativePath
							.replace(File.separatorChar, '.')
							.replaceAll("\\.class$", "");
					if (className.startsWith(".")) {
						className = className.substring(1);
					}
					try {
						Class<?> clazz = Class.forName(className);
						if (clazz.isAnnotationPresent(
								annotationClass.asSubclass(java.lang.annotation.Annotation.class))) {
							resultat.add(clazz);
							logManager.insertLog("controller class found " + className, LogStatus.INFO);
						}
					} catch (Throwable t) {
					}
				}
				return FileVisitResult.CONTINUE;
			}
		});

		return resultat;
	}

	private Integer gererRoutes(Url requestURL, Map<Url, Pattern> routes, Map<Url, Method> methodsMap,
			HttpServletRequest req, HttpServletResponse resp) throws IOException {

		PrintWriter out = resp.getWriter();

		for (Map.Entry<Url, Pattern> patternEntry : routes.entrySet()) {
			Url routeURL = patternEntry.getKey();

			if (patternEntry.getValue().matcher(requestURL.getUrlPath()).matches()
					&& requestURL.getMethod() == routeURL.getMethod()) {

				try {
					String originalPattern = routeURL.getUrlPath();

					Method method = methodsMap.get(routeURL);
					Map<String, String> params = RoutePatternUtils.extractPathParams(originalPattern,
							requestURL.getUrlPath());
					Object result = methodeManager.invokeCorrespondingMethod(method, method.getDeclaringClass(), params,
							req, resp);

					if (method.isAnnotationPresent(JsonUrl.class)) {
						String jsonContent = contentRenderManager.convertToJson(result);

						resp.setContentType("application/json; charset=UTF-8");
						resp.setCharacterEncoding("UTF-8");

						out.print(jsonContent);
						out.flush();
						return RouteStatus.RETURN_JSON.getCode();
					}

					return contentRenderManager.renderContent(result, req, resp);
					// return RouteStatus.NOT_FOUND.getCode();

				} catch (Exception e) {
					out.println("Erreur interne : " + e.getMessage());
					return RouteStatus.RETURN_TYPE_UNKNOWN.getCode();
				}
			}
		}

		return RouteStatus.NOT_FOUND.getCode();
	}

	@Override
	public void destroy() {
	}
}
