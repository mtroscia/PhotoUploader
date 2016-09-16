package com.servlets;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "Download", urlPatterns = {"/download/*"})
public class Download extends HttpServlet {
    private Mongo client;
    private DB db;
    static final int BUFFER = 2048;
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
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Check if session is present
        HttpSession session = request.getSession(false);
        String username;
        if (session == null || (username = (String)session.getAttribute("username")) == null) {
            return;
        }
        // Get the list of the files the client asked to download
        // The client sends the md5 of the photos he/she is interested in
        String reqBody = request.getParameter("photos");
        String photosToDownload[] = reqBody.split(";");
        // Connect with the data base file system
        GridFS gfsPhoto = new GridFS(db, username);
        // Create a zip stream which writes directly into the response body
        byte data[] = new byte[BUFFER];
        ZipOutputStream out = new ZipOutputStream(response.getOutputStream());
        long fileLength = 0;
        for (int i = 0; i < photosToDownload.length; i++) {
            // create the query object
            DBObject query = new BasicDBObject("md5", photosToDownload[i]);
            // get the file from the database
            GridFSDBFile photo = gfsPhoto.findOne(query);
            // create an input stream from the file in order to transfer it
            // to the zip stream
            InputStream origin = new BufferedInputStream(photo.getInputStream());
            // zip stream needs to be notified every time a new file is put in
            // the archive by inserting a new zip entry (the file name)
            ZipEntry ze = new ZipEntry(photo.getFilename());
            out.putNextEntry(ze);
            int count;
            // copy the content of the file in the zip stream
            while((count = origin.read(data, 0, BUFFER)) != -1) {
               out.write(data, 0, count);
               fileLength += count;
            }
            origin.close();
        }
        out.close();
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "filename=\"photos.zip\"");
        response.setContentLengthLong(fileLength);
    }
}
