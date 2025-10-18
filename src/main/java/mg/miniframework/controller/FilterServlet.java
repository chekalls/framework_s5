package mg.miniframework.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

		String requestUri = req.getRequestURI();
		String contextPath = req.getContextPath();
		if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
			requestUri = requestUri.substring(contextPath.length());
		}

		String method = req.getMethod();
		boolean isGetLike = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);

		String resourcePath = null;
		if ("/".equals(requestUri)) {
			if (request.getServletContext().getResource("/index.html") != null) {
				resourcePath = "/index.html";
			}
		} else {
			if (request.getServletContext().getResource(requestUri) != null) {
				resourcePath = requestUri;
			} else if (request.getServletContext().getResource(requestUri + "/index.html") != null) {
				resourcePath = requestUri + "/index.html";
			}
		}

		if (isGetLike && resourcePath != null) {
			String mime = request.getServletContext().getMimeType(resourcePath);
			if (mime != null && !mime.isBlank()) {
				resp.setContentType(mime);
			}

			if ("HEAD".equalsIgnoreCase(method)) {
				return;
			}

			try (InputStream in = request.getServletContext().getResourceAsStream(resourcePath);
				 OutputStream out = resp.getOutputStream()) {
				if (in == null) {
					chain.doFilter(request, response);
					return;
				}
				in.transferTo(out);
				out.flush();
				return;
			}
		}

		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}
}
