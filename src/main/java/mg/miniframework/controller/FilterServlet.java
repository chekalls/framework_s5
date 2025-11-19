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
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import mg.miniframework.config.RouteMap;
import mg.miniframework.modules.ModelView;
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
		PrintWriter out = resp.getWriter();
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

		if (servletContext.getAttribute("settingMap") == null || servletContext.getAttribute("settingMap") == "") {
			Map<String, String> setContainer = getAllProperties();
			servletContext.setAttribute("settingMap", setContainer);
			out.print("null le izy teto");
		}

		Map<String, String> mapSetting = (Map<String, String>) servletContext.getAttribute("settingMap");
		for (Map.Entry<String, String> iterable_element : mapSetting.entrySet()) {
			out.println("setting :" + iterable_element.getKey() + " -> " + iterable_element.getValue());
		}
		if (mapSetting.containsKey("jsp_base_path")) {
			baseFile = mapSetting.get("jsp_base_path");
		} else {

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

		String requestUri = req.getRequestURI();
		String urlPath = requestUri.substring(contextPath.length());
		String httpMethod = req.getMethod();

		boolean urlExists = false;
		for (Map.Entry<Url, Method> entry : routeMap.getUrlMethodsMap().entrySet()) {
			Url url = entry.getKey();
			Method method = entry.getValue();
			if (url.getUrlPath().equals(urlPath) && url.getMethod().toString().equalsIgnoreCase(httpMethod)) {
				urlExists = true;
				out.print("class :" + method.getDeclaringClass().getName() + " method:" + method.getName() + "\n");

				try {
					Object result = invokeCorrespondingMethod(method, method.getDeclaringClass());
					if (result instanceof String) {
						out.print(result);
					} else if (result instanceof ModelView) {
						ModelView modelView = (ModelView) result;
						String jspFile = modelView.getView();

						if (!jspFile.startsWith("/")) {
							jspFile = "/" + jspFile;
						}

						String forwardPath = baseFile + jspFile;
						forwardPath = forwardPath.replaceAll("//+", "/");

						realPath = req.getServletContext().getRealPath(forwardPath);
						File jspRealFile = (realPath != null) ? new File(realPath) : null;

						System.out.println("[FilterServlet] Forward vers : " + forwardPath);
						System.out.println("[FilterServlet] Fichier réel : " + realPath);
						System.out.println("[FilterServlet] Existe : " + (jspRealFile != null && jspRealFile.exists()));

						if (jspRealFile != null && jspRealFile.exists()) {
							RequestDispatcher dispatcher = req.getRequestDispatcher(forwardPath);
							dispatcher.forward(req, resp);
							return;
						} else {
							resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
							resp.setContentType("text/html;charset=UTF-8");
							PrintWriter writer = resp.getWriter();
							writer.println("<h2>Erreur : vue introuvable</h2>");
							writer.println("<p> base Path :" + baseFile + " </p>");
							writer.println("<p>Chemin demandé : " + forwardPath + "</p>");
							if (realPath != null)
								writer.println("<p>Fichier réel : " + realPath + "</p>");
							writer.flush();
							return;
						}
					}
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
					out.print(e.getMessage());
				}
				break;
			}
		}

		realPath = servletContext.getRealPath(urlPath);
		out.println("real path : " + realPath);
		if (realPath != null) {
			File resource = new File(realPath);
			out.println("resource exist :" + (resource.exists() && !resource.isDirectory()));
			if (resource.exists() && !resource.isDirectory()) {
				chain.doFilter(request, response);
				return;
			}
		}

		if (!urlExists) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			out.println("<html><body>");
			out.println("<h1>404 - Page Not Found</h1>");
			out.println("<p>L'URL <b>" + urlPath + "</b> avec la méthode <b>" + httpMethod + "</b> n'existe pas.</p>");
			out.println("<p>Routes disponibles :</p>");
			out.println("<ul>");
			for (Map.Entry<Url, Method> entry : routeMap.getUrlMethodsMap().entrySet()) {
				out.println("<li>" + entry.getKey().getMethod() + " " + entry.getKey().getUrlPath() + " -> "
						+ entry.getValue().getName() + "</li>");
			}
			out.println("</ul>");
			out.println("</body></html>");
			return;
		}

		chain.doFilter(request, response);
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

	private Object invokeCorrespondingMethod(Method method, Class<?> clazz)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		Object instance = clazz.getDeclaredConstructor().newInstance();
		Parameter[] parameters = method.getParameters();
		

		Object result = method.invoke(instance);

		return result;
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

	@Override
	public void destroy() {
	}
}
