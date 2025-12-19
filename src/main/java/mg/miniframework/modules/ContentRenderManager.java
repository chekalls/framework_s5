package mg.miniframework.modules;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.miniframework.utils.JsonUtils;

public class ContentRenderManager {

    private LogManager logManager;
    private String baseJspPath = "";

    public ContentRenderManager() {
        this.logManager = new LogManager();
    }

    /* =======================
       JSON
       ======================= */
    public String convertToJson(Object object) {
        if (object instanceof ModelView mv) {
            return JsonUtils.mapToJson(mv.getDataMap());
        }
        return JsonUtils.objectToJson(object);
    }

    /* =======================
       RENDER
       ======================= */
    public int renderContent(
            Object result,
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        if (result == null) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return RouteStatus.NOT_FOUND.getCode();
        }

        /* =======================
           STRING
           ======================= */
        if (result instanceof String str) {
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().print(str);
            return RouteStatus.RETURN_STRING.getCode();
        }

        /* =======================
           MODEL VIEW (JSP)
           ======================= */
        if (result instanceof ModelView mv) {

            for (Map.Entry<String, Object> entry : mv.getDataMap().entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }

            String jsp = normalizeJspPath(mv.getView());
            String forwardPath = baseJspPath + jsp;

            RequestDispatcher dispatcher =
                    request.getRequestDispatcher(forwardPath);

            dispatcher.forward(request, response);
            return RouteStatus.RETURN_MODEL_VIEW.getCode();
        }

        /* =======================
           UNKNOWN
           ======================= */
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println(
                "Return type inconnu : " + result.getClass().getName()
        );

        return RouteStatus.RETURN_TYPE_UNKNOWN.getCode();
    }

    /* =======================
       UTILS
       ======================= */
    private String normalizeJspPath(String jsp) {
        if (jsp == null || jsp.isEmpty()) {
            throw new IllegalArgumentException("Le nom de la vue JSP est vide");
        }
        return jsp.startsWith("/") ? jsp : "/" + jsp;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }

    public String getBaseJspPath() {
        return baseJspPath;
    }

    public void setBaseJspPath(String baseJspPath) {
        if (baseJspPath == null) {
            this.baseJspPath = "";
        } else {
            this.baseJspPath = baseJspPath.endsWith("/")
                    ? baseJspPath.substring(0, baseJspPath.length() - 1)
                    : baseJspPath;
        }
    }
}
