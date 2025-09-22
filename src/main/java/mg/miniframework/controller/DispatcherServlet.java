package mg.miniframework.controller;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DispatcherServlet extends HttpServlet {
    @Override
    public void init() throws ServletException{

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        gererUrl(req, resp);
    }

    private void gererUrl(HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse) throws IOException{
        String chemin = httpServletRequest.getPathInfo();
        PrintWriter out = httpServletResponse.getWriter();
        out.print("chemin : "+chemin);
    }
}