package com.servlets;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.io.FilenameUtils;

@WebServlet(name = "GetThumbnail", urlPatterns = {"/getThumbnail/*"})
public class GetThumbnail extends HttpServlet {
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
        HttpSession session = request.getSession(false);
        String username;
        if (session == null || (username = (String)session.getAttribute("username")) == null) {
            return;
        }
        String uri = request.getRequestURI();
        String md5 = FilenameUtils.getBaseName(uri);
        // create the file system class
        GridFS gfsPhoto = new GridFS(db, username);
        // create the file to query the database
        DBObject obj = new BasicDBObject("md5", md5);
        // find the file with the provided md5
        GridFSDBFile photo = gfsPhoto.findOne(obj);
        // get the thumbnail
        DBCursor cursor = db.getCollection(username + ".chunks")
                .find(new BasicDBObject("files_id",
                ((DBObject)photo.get("metadata")).get("thumbnail")));
        OutputStream out = response.getOutputStream();
        while(cursor.hasNext()) {
            DBObject chunk = cursor.next();
            byte[] outputBytes = (byte[])chunk.get("data");
            out.write(outputBytes);
        }
        response.setContentType("image/jpeg");
        out.close();
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
    }
}
