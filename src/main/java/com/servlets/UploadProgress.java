/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.servlets;

import com.Progress;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Lorenzo
 */
@WebServlet(name = "UploadProgress", urlPatterns = {"/uploadProgress"})
public class UploadProgress extends HttpServlet {
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // set the content type of the response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // do not allow response caching: each time the resouce is requested its
        // content must not be retrived from cache because it may be out of date
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setHeader("Expires", "0"); // Proxies.
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            sendResponse(response, "");
            return;
        }
        Progress progressListener = (Progress)session.getAttribute("progress");
        if (progressListener == null) {
            sendResponse(response, "");
            return;
        }
        sendResponse(response, progressListener.getProgress());
    }

    /**
     * In case of errors the user is notified.
     * @param request servlet request
     */
    private void sendResponse(HttpServletResponse response, Object message) {
        try {
            PrintWriter out = response.getWriter();
            out.write(new Gson().toJson(message));
            out.close();
        } catch (IOException e) { }
    }
}
