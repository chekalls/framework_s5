package mg.miniframework.modules;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.miniframework.utils.JsonUtils;

public class ContentRenderManager {
    private LogManager logManager;
    private String baseJspPath;

    public ContentRenderManager() {
        logManager = new LogManager();
        baseJspPath = ""; 
    }

    public String convertToJson(Object object) {
        if (object instanceof ModelView) {
            Map<String, Object> data = ((ModelView) object).getDataMap();
            return JsonUtils.mapToJson(data);
        } else {
            return JsonUtils.objectToJson(object);
        }
    }

    public int renderContent(Object resultType, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (resultType == null) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return RouteStatus.RETURN_TYPE_UNKNOWN.getCode();
        }

        if (resultType instanceof String) {
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.print((String) resultType);
            out.flush();
            return RouteStatus.RETURN_STRING.getCode();

        } else if (resultType instanceof ModelView) {
            ModelView mv = (ModelView) resultType;

            for (Map.Entry<String, Object> entry : mv.getDataMap().entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }

            String jspFile = mv.getView();
            if (!jspFile.startsWith("/")) {
                jspFile = "/" + jspFile;
            }
            String forwardPath = baseJspPath + jspFile;

            RequestDispatcher dispatcher = request.getRequestDispatcher(forwardPath);
            dispatcher.forward(request, response);
            return RouteStatus.RETURN_MODEL_VIEW.getCode();
        }


        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("Return type inconnu : " + resultType.getClass().getName());
        out.flush();
        return RouteStatus.RETURN_TYPE_UNKNOWN.getCode();
    }

    // Getter / Setter
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
        this.baseJspPath = baseJspPath;
    }
}
