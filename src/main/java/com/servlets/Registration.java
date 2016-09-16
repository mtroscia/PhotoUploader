package com.servlets;

import java.io.IOException; 
import javax.servlet.ServletException; 
import javax.servlet.annotation.WebServlet; 
import javax.servlet.http.HttpServlet; 
import javax.servlet.http.HttpServletRequest; 
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
  
// Specify the name of the servlet and the URL for it 
@WebServlet(name = "Registration", urlPatterns = {"/signin"}) 
public class Registration extends HttpServlet { 
    private final String table = "login";
    private boolean driverLoaded;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext context = config.getServletContext();
        
        // Load db driver
        Class<?> driver = (Class<?>)context.getAttribute("jdbcDriver");
        if (driver == null) {
            try {
                driver = Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
                context.setAttribute("jdbcDriver", driver);
            }
            catch(ClassNotFoundException ex) {
                driverLoaded = false;
                return;
            }
        }
        driverLoaded = true;
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
        String message;
        // Check if the user has already logged in
        HttpSession session = request.getSession(false);
        // If there is a session, redirect to the user's home page
        if (session != null) {
            response.sendRedirect("");
            return;
        }
        // Retrieve the user's input values from the Welcome Page 
        String username = request.getParameter("username"); 
        String password = request.getParameter("password");
        String firstname = request.getParameter("firstname");
        String lastname = request.getParameter("lastname");
        // Connect to database
        if (driverLoaded == false) {
            message = "unable to load driver class.";
            handleError(request, response, message, firstname, lastname);
            return;
        }
        Connection conn;
        try {
            conn = DriverManager.getConnection("jdbc:derby://localhost:1527/db_test;create=true;user=root;password=pass");
        } catch (SQLException ex) {
            message = "unable to connect to database.";
            handleError(request, response, message, firstname, lastname);
            return;
        }
        Statement stmt = null;
        ResultSet results = null;
        try {
            stmt = conn.createStatement();
            results = stmt.executeQuery("select username from "+table+" where username='"+username+"'");
            if (results.next()) {
                message = "user '" + username +"' has been already used. Please, choose another username.";
                handleError(request, response, message, firstname, lastname);
                return;
            } else {
                stmt.executeUpdate("INSERT INTO " + table +
                        " (username, password, firstname, lastname) VALUES('"
                        + username + "','" + password + "','"
                        + firstname + "','" + lastname + "')");
            }
        } catch (SQLException ex) {
            message = "SQL Query failed.";
            handleError(request, response, message, firstname, lastname);
            return;
        } finally {
            if (stmt != null) {
                try { stmt.close(); }
                catch (SQLException ex) {
                    stmt = null;
                }
            }
            if (results != null) {
                try { results.close(); }
                catch (SQLException ex) {
                    results = null;
                }
            }
            try { conn.close(); }
            catch (SQLException ex) {
                conn = null;
            }
        }
        message = "User '" + username +"' successifully signed in.<br>You can now log in with the data provided.";
        request.setAttribute("message", message);
        // The username will be displayed in the form to log in
        request.setAttribute("username", username);
        request.getRequestDispatcher("login.jsp").forward(request, response); 
    }
    
    /**
     * In case of errors the user is notified. User's first and last name are 
     * saved so he/she does not need to insert them again
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException
     * @throws IOException 
     */
    private void handleError(HttpServletRequest request, HttpServletResponse response, String message, String firstname, String lastname) throws ServletException, IOException {
        System.out.println("Error: " + message);
        request.setAttribute("message", "Error: " + message);
        request.setAttribute("firstname", firstname);
        request.setAttribute("lastname", lastname);
        request.getRequestDispatcher("registration.jsp").forward(request, response);
    }
}
