package mg.miniframework.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
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

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		PrintWriter out = resp.getWriter();
		// req.getServletContext().setAttribute(null, out);

		if(req.getServletContext().getAttribute("routeMap")==null){
			try {
				RouteMap routeMap = new RouteMap();
				List<Class<?>> controllers = trouverClassesAvecAnnotation(Controller.class);

				for (Class<?> class1 : controllers) {
					routeMap.addController(class1);
				}
				req.getServletContext().setAttribute("routeMap", routeMap);				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		RouteMap routeMap = (RouteMap) req.getServletContext().getAttribute("routeMap");

		String requestUri = req.getRequestURI();
		String contextPath = req.getContextPath();
		String urlPath = requestUri.substring(contextPath.length());
		String httpMethod = req.getMethod();

		boolean urlExists = false;
		for (Map.Entry<Url, Method> entry : routeMap.getUrlMethodsMap().entrySet()) {
			Url url = entry.getKey();
			Method method = entry.getValue();
			if (url.getUrlPath().equals(urlPath) && url.getMethod().toString().equalsIgnoreCase(httpMethod)) {
				urlExists = true;
				out.print("class :"+method.getDeclaringClass().getName() + " method:"+method.getName());

				try {
				 	Object result = invokeCorrespondingMethod(method,method.getDeclaringClass());
					if(result instanceof String){
						out.print(result);
					}else if(result instanceof ModelView){
						ModelView modelView = (ModelView) result;
						String jspFile = modelView.getView();
						out.print(jspFile);
					}
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
				break;
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
				out.println("<li>" + entry.getKey().getMethod() + " " + entry.getKey().getUrlPath() + " -> " + entry.getValue().getName() + "</li>");
			}
			out.println("</ul>");
			out.println("</body></html>");
			return;
		}

		chain.doFilter(request, response);
	}

	private Object invokeCorrespondingMethod(Method method,Class<?> clazz) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException{
		Object instance = clazz.getDeclaredConstructor().newInstance();
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
