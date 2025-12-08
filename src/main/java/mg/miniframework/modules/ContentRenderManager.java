package mg.miniframework.modules;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.miniframework.utils.JsonUtils;

public class ContentRenderManager {
    private LogManager logManager;
    private String forwardPath;

    public ContentRenderManager() {
        logManager = new LogManager();
    }

    public String convertToJson(Object object) {
        if (object instanceof ModelView) {
            Map<String, Object> data = ((ModelView) object).getDataMap();
            return JsonUtils.mapToJson(data);
        }else{
            return JsonUtils.objectToJson(object);
        }
    }

    public int renderContent(Object resultType, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();

        if (resultType instanceof String) {
            response.setContentType("text/html; charset=UTF-8");
            out.print((String) resultType);
            return RouteStatus.RETURN_STRING.getCode();

        } else if (resultType instanceof ModelView) {
            ModelView mv = (ModelView) resultType;

            for (Map.Entry<String, Object> data : mv.getDataMap().entrySet()) {
                request.setAttribute(data.getKey(), data.getValue());
            }

            String jspFile = mv.getView();
            if (!jspFile.startsWith("/"))
                jspFile = "/" + jspFile;

            RequestDispatcher dispatcher = request.getRequestDispatcher(forwardPath);
            dispatcher.forward(request, response);
            return RouteStatus.RETURN_MODEL_VIEW.getCode();
        }

        out.println("Return type inconnu : " + resultType.getClass().getName());
        return RouteStatus.RETURN_TYPE_UNKNOWN.getCode();

    }

    public LogManager getLogManager() {
        return logManager;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }

    public String getForwardPath() {
        return forwardPath;
    }

    public void setForwardPath(String forwardPath) {
        this.forwardPath = forwardPath;
    }
}
