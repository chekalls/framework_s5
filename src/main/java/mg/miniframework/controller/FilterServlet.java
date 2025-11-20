package mg.miniframework.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.lang.reflect.Parameter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
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
import mg.miniframework.annotation.RequestAttribute;
import mg.miniframework.annotation.UrlParam;
import mg.miniframework.config.RouteMap;
import mg.miniframework.modules.ModelView;
import mg.miniframework.modules.RouteStatus;
import mg.miniframework.modules.Url;

@WebFilter(filterName = "resourceExistenceFilter", urlPatterns = "/*")
public class FilterServlet implements Filter {

	private String baseFile;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@SuppressWarnings("unchecked")
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		baseFile = new String();
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		ServletContext servletContext = req.getServletContext();

		String requestURI = req.getRequestURI();
		String contextPath = req.getContextPath();
		String relativePath = requestURI.substring(contextPath.length());
		String realPath = req.getServletContext().getRealPath(relativePath);

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
			Map<String, String> setContainer = getAllProperties();
			servletContext.setAttribute("settingMap", setContainer);
		}

		Map<String, String> mapSetting = (Map<String, String>) servletContext.getAttribute("settingMap");
		if (mapSetting.containsKey("jsp_base_path")) {
			baseFile = mapSetting.get("jsp_base_path");
		}

		if (servletContext.getAttribute("routeMap") == null) {
			try {
				RouteMap routeMap = new RouteMap();
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
			routePattern.put(entry.getKey(), convertRouteToPattern(entry.getKey().getUrlPath()));
		}

		Integer status = gererRoutes(url, routePattern, routeMap.getUrlMethodsMap(), req, resp);

		if (status.equals(RouteStatus.NOT_FOUND.getCode())) {
			print404(resp, urlPath, req.getMethod(), routeMap);
			return;
		}

		if (!status.equals(RouteStatus.NOT_FOUND.getCode())) {
			return;
		}

		chain.doFilter(req, resp);
	}

	private void print404(HttpServletResponse resp, String urlPath, String httpMethod, RouteMap routeMap)
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
			out.println("<li><b>" + entry.getKey().getMethod() + "</b> : " +
					entry.getKey().getUrlPath() + " → " +
					entry.getValue().getDeclaringClass().getSimpleName() + "." +
					entry.getValue().getName() + "()</li>");
		}
		out.println("</ul>");

		out.println("</body></html>");
	}

	private Map<String, String> getAllProperties() {
		Map<String, String> map = new HashMap<>();

		try {
			Enumeration<URL> resources = getClass().getClassLoader().getResources("");

			while (resources.hasMoreElements()) {
				URL url = resources.nextElement();
				Path root = Paths.get(url.toURI());

				Files.walk(root)
						.filter(p -> p.toString().endsWith(".properties"))
						.forEach(p -> {
							try (InputStream in = Files.newInputStream(p)) {
								Properties props = new Properties();
								props.load(in);
								props.forEach((k, v) -> map.put(k.toString(), v.toString()));
							} catch (IOException ignored) {
							}
						});
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return map;
	}

	private Object convertParam(String value, Class<?> type) {

		if (type == String.class)
			return value;
		if (type == int.class || type == Integer.class)
			return Integer.parseInt(value);
		if (type == long.class || type == Long.class)
			return Long.parseLong(value);
		if (type == double.class || type == Double.class)
			return Double.parseDouble(value);
		if (type == float.class || type == Float.class)
			return Float.parseFloat(value);
		if (type == boolean.class || type == Boolean.class)
			return Boolean.parseBoolean(value);
		return value;
	}

	private Object invokeCorrespondingMethod(Method method, Class<?> clazz, Map<String, String> params,
			HttpServletRequest request,
			HttpServletResponse resp)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, IOException {

		Object instance = clazz.getDeclaredConstructor().newInstance();
		Parameter[] parameters = method.getParameters();
		Object[] args = new Object[parameters.length];
		PrintWriter writer = resp.getWriter();

		for (int i = 0; i < parameters.length; i++) {
			Parameter param = parameters[i];
			String rawValue = null;
			UrlParam urlParamAnnotation = param.getAnnotation(UrlParam.class);
			RequestAttribute requestAttributeAnnotation = param.getAnnotation(RequestAttribute.class);

			if (urlParamAnnotation != null) {
				rawValue = params.getOrDefault(urlParamAnnotation.name(),
						params.getOrDefault(param.getName(), null));

			} else if (requestAttributeAnnotation != null) {
				rawValue = (String) request.getParameter(requestAttributeAnnotation.paramName());
				if (rawValue == null || rawValue == "") {
					rawValue = (String) requestAttributeAnnotation.defaultValue();
				}
			} else {
				rawValue = (String) request.getParameter(param.getName());
			}
			// String rawValue = request.getParameter(param.getName());

			// writer.println("attribut : " + param.getName() + " type :" + param.getType().getSimpleName().toString()
			// 		+ " value : " + rawValue);
			args[i] = convertParam(rawValue, param.getType());
		}

		return method.invoke(instance, args);
	}

	public static Map<String, String> extractPathParams(String pattern, String url) {
		List<String> names = new ArrayList<>();
		Matcher nameMatcher = Pattern.compile("\\{([^/]+)}").matcher(pattern);
		while (nameMatcher.find()) {
			names.add(nameMatcher.group(1));
		}

		String regex = pattern.replaceAll("\\{[^/]+}", "([^/]+)");

		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(url);

		Map<String, String> params = new LinkedHashMap<>();

		if (m.matches()) {
			for (int i = 0; i < names.size(); i++) {
				params.put(names.get(i), m.group(i + 1));
			}
		}

		return params;
	}

	private List<Class<?>> trouverClassesAvecAnnotation(Class<?> annotationClass) throws Exception {
		List<Class<?>> resultat = new ArrayList<>();
		String classesPath = getClass().getClassLoader().getResource("").getPath();
		Path basePath = Paths.get(classesPath);

		Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.toString().endsWith(".class")) {
					String relativePath = basePath.relativize(file).toString();
					String className = relativePath
							.replace(File.separator, ".")
							.replaceAll("\\.class$", "");
					try {
						Class<?> clazz = Class.forName(className);
						if (clazz.isAnnotationPresent(
								annotationClass.asSubclass(java.lang.annotation.Annotation.class))) {
							resultat.add(clazz);
						}
					} catch (Throwable t) {
					}
				}
				return FileVisitResult.CONTINUE;
			}
		});

		return resultat;
	}

	private Pattern convertRouteToPattern(String route) {
		String regex = route.replaceAll("\\{[^/]+\\}", "([^/]+)");
		regex = "^" + regex + "$";
		return Pattern.compile(regex);
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
					Map<String, String> params = extractPathParams(originalPattern, requestURL.getUrlPath());
					Object result = invokeCorrespondingMethod(method, method.getDeclaringClass(), params, req, resp);

					if (result instanceof String) {
						resp.setContentType("text/html; charset=UTF-8");
						out.print((String) result);
						return RouteStatus.RETURN_STRING.getCode();
					}

					if (result instanceof ModelView) {
						ModelView mv = (ModelView) result;

						for (Map.Entry<String, Object> data : mv.getDataMap().entrySet()) {
							req.setAttribute(data.getKey(), data.getValue());
						}

						String jspFile = mv.getView();
						if (!jspFile.startsWith("/"))
							jspFile = "/" + jspFile;
						String forwardPath = baseFile + jspFile;

						RequestDispatcher dispatcher = req.getRequestDispatcher(forwardPath);
						dispatcher.forward(req, resp);
						return RouteStatus.RETURN_MODEL_VIEW.getCode();
					}

					out.println("Return type inconnu : " + result.getClass().getName());
					return RouteStatus.RETURN_TYPE_UNKNOWN.getCode();

				} catch (Exception e) {
					e.printStackTrace();
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
