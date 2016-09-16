<%@page contentType="text/html" pageEncoding="UTF-8" session="false"%>
<!DOCTYPE html>
<%
    String firstname = (String)request.getAttribute("firstname");
    if (firstname == null) firstname = "";
    String lastname = (String)request.getAttribute("lastname");
    if (lastname == null) lastname = "";
    String message = (String)request.getAttribute("message");
    if (message != null) {
        message = "<div class=\"error\">" + message + "</div><br>";
    } else {
        message = "";
    }
%>
<html>
    <head>
        <title>New user registration</title>  
        <meta charset="UTF-8">  
        <meta name="viewport" content="width=device-width">
        <link href="css/general.css" rel="stylesheet" type="text/css">
    </head>  
    <body>
        <form name="InputForm" id="regform" method="post" action="signin"> 
            <h2>Sign in</h2> 
            <div class="formelem">Username:</div>
            <input type="text" name="username"/>
            <div class="formelem">Password:</div>
            <input type="password" name="password"/>
            <div class="formelem">First Name:</div>
            <input type="text" name="firstname" value="<%= firstname %>"/>
            <div class="formelem">Last Name:</div>
            <input type="text" name="lastname" value="<%= lastname %>"/>
            <div class="centering"><input type="submit" value="Submit" name="SubmitButton"/></div>
        </form>
        <%= message %>
        <div class="foot"><a href="login.jsp">Log in</a> if you have already signed in</div>
    </body>
</html>
