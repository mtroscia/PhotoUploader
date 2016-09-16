/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.servlets;

import com.google.gson.Gson;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
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
@WebServlet(name = "GetPhotosList", urlPatterns = {"/getPhotosList"})
public class GetPhotosList extends HttpServlet {
    private Mongo client;
    private DB db;
    private ServletContext context;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        context = config.getServletContext();
        // create a MongDB connection
        client = (Mongo)context.getAttribute("mongoClient");
        if (client == null) {
            client = new Mongo(Upload.address, 27017);
            context.setAttribute("mongoClient", client);
        }
        db = (DB)context.getAttribute("mongoDB");
        if (db == null) {
            db = client.getDB("images");
            context.setAttribute("mongoDB", db);
        }
    }
    
    @Override
    public void destroy() {
        // close the MongoDB connection
        client.close();
        context.removeAttribute("mongoClient");
        context.removeAttribute("mongoDB");
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
        List<String> photoList = new ArrayList<>();
        String username;
        // check if the session exists
        HttpSession session = request.getSession(false);
        if (session == null || (username = (String)session.getAttribute("username")) == null) {
            sendResponse(response, "Invalid session.");
            return;
        }
        // check if up-to-date list has already been sent
        Date lastUpdatedList = (Date)session.getAttribute("lastUpdatedList");
        if (lastUpdatedList != null) {
            if (lastUpdatedList == (Date)session.getAttribute("lastSentList")) {
                // user already has up-to-date photos list
                sendResponse(response, "");
                return;
            }
        } else {
            lastUpdatedList = new Date();
            session.setAttribute("lastUpdatedList", lastUpdatedList);
        }
        // this stores all the data for a photo
        DBObject file;
        // this stores the creation date, the width and height of the photo
        DBObject metadata;
        String md5;
        DBCollection collection = db.getCollection(username + ".files");
        Date date;
        int width;
        int height;
        Cursor cursor = collection.find();
        // check if at least one photo is present
        if (!cursor.hasNext()) {
            sendResponse(response, "0");
            return;
        }
        while (cursor.hasNext()) {
            file = cursor.next();
            md5 = (String)file.get("md5");
            metadata = (DBObject)file.get("metadata");
            // extract date, width and height from the metadata object
            date = (Date)metadata.get("creationDate");
            width = (int)metadata.get("width");
            height = (int)metadata.get("height");
            // add the data retreived to the list to communicate to the client
            photoList.add(md5 + ',' + date.getTime() + ',' + width + ',' + height +(cursor.hasNext()?'|':""));
        }
        String message = "";
        for (String s : photoList) {
            message += s;
        }
        session.setAttribute("lastSentList", lastUpdatedList);
        sendResponse(response, message);
    }
    
    private void sendResponse(HttpServletResponse response, String message) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            PrintWriter out = response.getWriter();
            out.write(new Gson().toJson(message));
            out.close();
        } catch (IOException e) { }
    }
}
