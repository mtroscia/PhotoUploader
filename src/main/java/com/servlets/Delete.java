package com.servlets;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFS;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "Delete", urlPatterns = {"/delete"})
public class Delete extends HttpServlet {
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
        HttpSession session = request.getSession(false);
        String username;
        if (session == null || (username = (String)session.getAttribute("username")) == null) {
            return;
        }
        String reqBody = getRequestBody(request);
        String photosToDelete[] = reqBody.split(";");
        GridFS gfsPhoto = new GridFS(db, username);
        for (int i = 0; i < photosToDelete.length; i++) {
            DBObject query = new BasicDBObject("md5", photosToDelete[i]);
            // Remove the thumbnail
            db.getCollection(username + ".chunks").remove(
                    new BasicDBObject(
                            "files_id",
                            gfsPhoto.findOne(query).getMetaData().get("thumbnail")
                    )
            );
            // Remove the photo
            gfsPhoto.remove(query);
        }
        session.setAttribute("lastUpdatedList", new Date());
    }
    
    private String getRequestBody(HttpServletRequest request) {
        String body = null;
        try {
            StringBuilder buffer = new StringBuilder();
            BufferedReader reader = request.getReader();

            String line;
            while((line = reader.readLine()) != null){
                buffer.append(line);
            }

            body = buffer.toString();
        } catch (IOException e) { }
        return body;
    }
}
