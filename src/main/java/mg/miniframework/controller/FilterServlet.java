package mg.miniframework.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

		// Vérifie si les controllers sont déjà en session
		if (req.getSession().getAttribute("controllersMap") == null) {
			try {
				List<Class<?>> controllers = trouverClassesAvecAnnotation(Controller.class);
				Map<Class<?>, List<Method>> map = new HashMap<>();

				for (Class<?> cls : controllers) {
					List<Method> annotatedMethods = new ArrayList<>();
					for (Method m : cls.getDeclaredMethods()) {
						if (m.isAnnotationPresent(mg.miniframework.annotation.UrlMap.class)) {
							annotatedMethods.add(m);
						}
					}
					map.put(cls, annotatedMethods);
				}

				req.getSession().setAttribute("controllersMap", map);
				System.out.println("Controllers et méthodes UrlMap stockés en session : " + map.size());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@SuppressWarnings("unchecked")
		Map<Class<?>, List<Method>> controllersMap = (Map<Class<?>, List<Method>>) req.getSession()
				.getAttribute("controllersMap");

		PrintWriter out = resp.getWriter();
		out.println("<h3>Controllers et méthodes @UrlMap :</h3>");
		for (Map.Entry<Class<?>, List<Method>> entry : controllersMap.entrySet()) {
			Class<?> cls = entry.getKey();
			List<Method> methods = entry.getValue();
			out.println("<b>" + cls.getName() + "</b>");
			for (Method m : methods) {
				mg.miniframework.annotation.UrlMap urlMap = m.getAnnotation(mg.miniframework.annotation.UrlMap.class);
				out.println("<div>→ " + m.getName() + " : " + urlMap.value() + "</div>");
			}
		}

		// chain.doFilter(request, response);
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
                        if (clazz.isAnnotationPresent(annotationClass.asSubclass(java.lang.annotation.Annotation.class))) {
                            resultat.add(clazz);
                        }
                    } catch (Throwable t) {
                        Ignore les classes non chargées
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
