/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
@WebServlet(name = "Login", urlPatterns = {"/login"})
public class Login extends HttpServlet {
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
        String username = "";
        // Check if the driver for database connections has been loaded
        if (driverLoaded == false) {
            message = "unable to load driver class.";
            handleError(request, response, message, username);
            return;
        }
        // Check if the user has already logged in
        HttpSession session = request.getSession(false);
        // If there is a session, redirect to the user's home page
        if (session != null) {
            // The request will be handled by IndexFilter
            // User's request is forwarded to userHome.jsp
            getServletContext().getRequestDispatcher("/").forward(request, response); 
            return;
        }
        // Get the data provided by the form filled by the user
        username = request.getParameter("username"); 
        String password = request.getParameter("password");

        // Connect to db
        Connection conn;
        try {
            conn = DriverManager.getConnection("jdbc:derby://localhost:1527/db_test;create=true;user=root;password=pass");
        } catch (SQLException ex) {
            message = "unable to connect to database.";
            handleError(request, response, message, username);
            return;
        }
        // Execute SQL query
        Statement stmt = null;
        ResultSet results = null;
        try {
            stmt = conn.createStatement();
            // Check if username and password are in the database
            results = stmt.executeQuery("select firstname from "+table+" where username='"+username+"' and password='"+password+"'");
            // If there is a match the variable results contains a row
            if (results.next()) {
                // LOGIN SUCCEEDS
                // Create a session for the logged user
                session = request.getSession();
                session.setAttribute("username", username);
                session.setAttribute("firstname", (String)results.getObject("firstname"));
                // Send user data to his/her home page
                request.getRequestDispatcher("userHome.jsp").forward(request, response);
            }
            else {
                // LOGIN FAILED
                // User remains in the login page with an error notification
                message = "either username or password is wrong.<br>Please, try again.";
                handleError(request, response, message, username);
            }
        } catch (SQLException ex) {
            message = "SQL query failed.";
            handleError(request, response, message, username);
        } finally {
            try {
                conn.close();
            } catch(SQLException ex) {
                conn = null;
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch(SQLException ex) {
                    stmt = null;
                }
            }
            if (results != null) {
                try {
                    results.close();
                } catch(SQLException ex) {
                    results = null;
                }
            }
        }
    }

    /**
     * In case of errors the user is notified. User's username is saved so 
     * he/she does not need to insert it again
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException 
     */
    private void handleError(HttpServletRequest request, HttpServletResponse response, String message, String username) throws ServletException, IOException {
        System.out.println("Error: " + message);
        request.setAttribute("message", "Error: " + message);
        request.setAttribute("username", username);
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }
}