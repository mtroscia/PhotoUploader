package com.servlets;

import com.Progress;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.FileCleanerCleanup;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.commons.io.FilenameUtils;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Rotation;

@WebServlet(name = "Upload", urlPatterns = {"/upload"})
public class Upload extends HttpServlet {
    public static final String address = "131.114.237.126";
    
    private final int MAX_SIZE = 4;
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
            client = new Mongo(address, 27017);
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
        String username;
        // these 3 lists stores the name of the not saved files
        List<String> invalidType = new ArrayList<>();
        List<String> cannotRead = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();

        // check the session
        HttpSession session = request.getSession(false);
        if (session == null || (username = (String)session.getAttribute("username")) == null) {
            handleError(request, response, "login.jsp",
                    "Invalid or expired session, please login before uploading files.");
            return;
        }
        // check if the request comes from a form with enctype="multipart/form-data"
        if (!ServletFileUpload.isMultipartContent(request)) {
            // no file to upload: redirect to upload.jsp to provide the
            // correct html form to upload files
            handleError(request, response, "upload.jsp",
                    "Invalid files upload request.");
            return;
        }
        
        //////////////////
        // Files upload //
        //////////////////
        
        // class which stores files
        DiskFileItemFactory factory = newDiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        
        // register a progress listener to the upload handler
        Progress progressListener = new Progress();
        upload.setProgressListener(progressListener);
        // now progressListener keeps track of the upload progress
        
        // save the upload progress in the user's session as an attribute
        session.setAttribute("progress", progressListener);
        // now, reqests sent to UploadProgress servlet can obtain the progress
        // attribute from the session
        
        ///////////////////
        // MongoDB stuff //
        ///////////////////
        
        // create the file system class
        GridFS gfsPhoto = new GridFS(db, username);

        int filesCount = 0;
        int filesSaved = 0;
        // transfer content in the database
        try {
            // obtain the list of fields from the upload handler
            List<FileItem> fields = upload.parseRequest(request); // throws FileUploadException 
            Iterator<FileItem> fileIterator = fields.iterator();
            // check if at least one field has been uploaded
            if (!fileIterator.hasNext()) {
                // no field uploaded
                sendResponse(response, "No file to upload.");
                return;
            }
            while(fileIterator.hasNext()) {
                String fileName;
                String mimeType;
                FileItem fileItem = fileIterator.next();
                // the list contains both html form fields and files
                if (fileItem.isFormField()) {
                    // it is a form field value
                    System.out.println("Form field name: " + fileItem.getFieldName());
                } else {
                    // it is a file
                    filesCount++;
                    fileName = FilenameUtils.getBaseName(fileItem.getName()) + '.' + FilenameUtils.getExtension(fileItem.getName());
                    System.out.println("File name: " + fileName);
                    System.out.println("File size: " + fileItem.getSize());
                    System.out.println("File content type: " + fileItem.getContentType());
                    System.out.println("File content: " + fileItem.toString());
                    
                    // use BufferedInputStream as it provides metadata access
                    InputStream stream = new BufferedInputStream(fileItem.getInputStream());
                    // check file content type getting the mime type
                    mimeType = URLConnection.guessContentTypeFromStream(stream);
                    // if getting the mime-type from the stream content fails,
                    // get it from the file name (using the extension)
                    if (mimeType == null) {
                        mimeType = fileItem.getContentType();
                    }
                    stream.close();
                    
                    /////////////////////////////
                    // check if it is an image //
                    /////////////////////////////
                    if (!mimeType.startsWith("image")) {
                        invalidType.add(fileName);
                        // do not save the file and continue with the next one
                        continue;
                    }
                    
                    //////////////////////////
                    // check for duplicates //
                    //////////////////////////
                    stream = new BufferedInputStream(fileItem.getInputStream());
                    String md5 = computeMD5(stream);
                    if (db.getCollection(username + ".files").find(new BasicDBObject("md5", md5)).hasNext()) {
                        duplicates.add(fileName);
                        continue;
                    }
                    stream.close();
                    
                    ////////////////////////////////////////////////////////////
                    // Get the creation date of the image and its orientation //
                    ////////////////////////////////////////////////////////////
                    Date date = null;
                    Rotation orientation = null;
                    // Try to get exif metadata
                    try {
                        stream = new BufferedInputStream(fileItem.getInputStream());
                        Metadata metadata = ImageMetadataReader.readMetadata(stream);
                        orientation = getOrientation(metadata);
                        ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                        if (directory != null) {
                            date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                        }
                        stream.close();
                    } catch (ImageProcessingException e) {
                    } finally {
                        // failed to get exif metadata
                        if (date == null) {
                            // set the creation date as the current day
                            date = new Date();
                        }
                    }
                    
                    /////////////////////////////////////////////////////////
                    // Get image's width and height and create a thumbnail //
                    /////////////////////////////////////////////////////////
                    int width;
                    int height;
                    GridFSInputFile thumbnail;
                    try {
                        stream = new BufferedInputStream(fileItem.getInputStream()); 
                        BufferedImage img = ImageIO.read(stream);
                        // ImageIO.read() cannot read the orientation giving the
                        // width and the height swapped in case of portrait
                        if (orientation == Rotation.CW_90 || orientation == Rotation.CW_270) {
                            // The image is portrait, swap width and height
                            width = img.getHeight();
                            height = img.getWidth();
                        } else {
                            // The image is landscape, leave width and heght as they are
                            width = img.getWidth();
                            height = img.getHeight();
                        }
                        // resize the image to get the thumbnail
                        if (orientation != null) {
                            // In case of portrait image, it must be rotate
                            img = Scalr.rotate(
                                    Scalr.resize(img, Scalr.Method.SPEED, Scalr.Mode.FIT_TO_WIDTH, 300, (int)Math.floor(300.0/height*width)),
                                    orientation);
                        } else {
                            // In case of landscape image, just resize it
                            img = Scalr.resize(img, Scalr.Method.SPEED, Scalr.Mode.FIT_TO_HEIGHT, (int)Math.floor(300.0/height*width), 300);
                        }
                        // convert BufferedImage class into InputStream to save
                        // it inside the GridFSInputFile
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        ImageIO.write(img, "jpeg", os);
                        InputStream is = new ByteArrayInputStream(os.toByteArray());
                        // save the InputSteam into the GridFSInputFile
                        thumbnail = gfsPhoto.createFile(is);
                        // NOTE: save only the chunks for the thumbnail, in this way the file
                        // does not appear in the "user".files collection (just in the
                        // "user".chunks collection)
                        thumbnail.saveChunks();
                        is.close();
                        os.close();
                        stream.close();
                    } catch (IOException | NullPointerException e) {
                        cannotRead.add(fileName);
                        continue;
                    }
                    
                    // save the extracted metadata in a DBObject
                    DBObject metadata = new BasicDBObject();
                    metadata.put("creationDate", date);
                    metadata.put("height", height);
                    metadata.put("width", width);
                    metadata.put("thumbnail", thumbnail.getId());
                    
                    // create the file from the stream
                    stream = new BufferedInputStream(fileItem.getInputStream());
                    GridFSInputFile imageFile = gfsPhoto.createFile(stream);
                    
                    // save all the associated data
                    imageFile.setFilename(fileName);
                    imageFile.setContentType(mimeType);
                    imageFile.setMetaData(metadata);
                    
                    imageFile.save();
                    filesSaved++;
                    stream.close();
                }
            }
        } catch (FileUploadException e){
            sendResponse(response, "Upload failed.");
            return;
        }
        // the photos' list has been changed, so it has to be sent again once
        // the client goes back to their home page
        session.setAttribute("lastUpdatedList", new Date());
        
        String message = getSummary(filesCount, filesSaved, invalidType, cannotRead, duplicates);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.write(new Gson().toJson(message));
        out.close();
        invalidType.clear();
        cannotRead.clear();
        duplicates.clear();
    }
    
    /**
     * Create a repository where uploaded files can be saved if their size is
     * grater than MAX_SIZE * 1024 * 1024, in order to save memory
     * @return the factory which handles the files saving
     */
    private DiskFileItemFactory newDiskFileItemFactory() {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        // create temporary file where to store too big uploaded files
        File repository = new File("C:/data/db/tmp/");
        if (!repository.exists())
            repository.mkdirs();
        // if the file is larger than MAX_SIZE MB it is stored in hard drive
        // temporarily
        factory.setSizeThreshold(MAX_SIZE * 1024 * 1024);
        // set the temporary repository where to store too big files
        factory.setRepository(repository);
        // once the temporary file is not needed anymore it has to be deleted
        // fileCleaningTracker keeps trak of the temporary files and deletes
        // them automatically when the factory is garbage collected
        FileCleaningTracker fileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(getServletContext());
        factory.setFileCleaningTracker(fileCleaningTracker);
        return factory;
    }

    /**
     * Compute the MD5 of a stream
     * @param stream the stream of which the MD5 has to be computed
     * @return MD5 of the input stream
     */
    private String computeMD5(InputStream stream) {
        byte[] buffer = new byte[8192];
        DigestInputStream dis = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            dis = new DigestInputStream(stream, md);
            while (dis.read(buffer) != -1);
            return DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException | IOException e) {
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e){
                    dis = null;
                }
            }
        }
        return null;
    }
    
    private Rotation getOrientation(Metadata metadata) {
        ExifIFD0Directory exifIFD0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (exifIFD0 == null) {
            return null;
        }
        int orientation;
        try {
            orientation = exifIFD0.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (MetadataException e) {
            orientation = 0;
        }
        switch (orientation) {
            case 6: // [Exif IFD0] Orientation - Right side, top (Rotate 90 CW)
              return Rotation.CW_90;
            case 3: // [Exif IFD0] Orientation - Bottom, right side (Rotate 180)
              return Rotation.CW_180;
            case 8: // [Exif IFD0] Orientation - Left side, bottom (Rotate 270 CW)
              return Rotation.CW_270;
        }
        return null;
    }
    
    /**
     * In case of errors the user is notified.
     * @param request servlet request
     * @param response servlet response
     * @param forward the forwarded page to the client
     * @param message the error message
     * @throws ServletException
     * @throws IOException 
     */
    private void handleError(HttpServletRequest request, HttpServletResponse response, String forward, String message) throws ServletException, IOException {
        System.out.println("Error: " + message);
        request.setAttribute("message", message);
        request.getRequestDispatcher(forward).forward(request, response);
    }
    
    /**
     * Prepare a response to the client with the process outcome
     * @param response servlet response
     * @param message the process outcome
     * @throws IOException 
     */
    private void sendResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.write(new Gson().toJson(message));
        out.close();
    }
    
    /**
     * Build the string with the process outcome with HTML tag
     * @param filesCount total number of uploaded file
     * @param filesSaved total number of saved file in the database
     * @param invalidType list of file names not saved due to invalid type
     * @param cannotRead list of file names not saved due to read issues
     * @param duplicates list of file names not saved due to a duplicate
     * @return the summary of the uploading process
     */
    private String getSummary(int filesCount, int filesSaved, List<String> invalidType, List<String> cannotRead, List<String> duplicates) {
        String message = "Upload process completed:<br>"
                + filesSaved + '/' + filesCount + " files saved.<br>";
        int notSaved = invalidType.size() + cannotRead.size() + duplicates.size();
        if (notSaved > 0) {
            message += "The following file" + (notSaved>1?"s were":" was") + " not saved:<ul>";
            String list = getListOfFile(invalidType);
            if (list != "") {
                message += "<li>Due to invalid type:<ul>" + list + "</ul></li>";
            }
            list = getListOfFile(cannotRead);
            if (list != "") {
                message += "<li>Due to an error while parsing:<ul>" + list + "</ul></li>";
            }
            list = getListOfFile(duplicates);
            if (list != "") {
                message += "<li>Due to a duplicate:<ul>" + list + "</ul></li>";
            }
            message += "</ul>";
        }
        return message;
    }
    
    /**
     * Build a string with HTML list tag from a List<String> class
     * @param list the list to process
     * @return HTML list
     */
    private String getListOfFile(List<String> list) {
        String l = "";
        for (String element : list) {
            l += "<li>" + element + "</li>";
        }
        return l;
    }
}
