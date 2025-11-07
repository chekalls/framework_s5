package mg.miniframework.controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "dispatcher", urlPatterns = "/*")
public class DispatcherServlet extends HttpServlet {
    @Override
    public void init() throws ServletException{

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        gererUrl(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        gererUrl(req, resp);
    }

    private void gererUrl(HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse) throws IOException{
        String chemin = httpServletRequest.getPathInfo();
        httpServletResponse.setContentType("text/plain;charset=UTF-8");
    }
}