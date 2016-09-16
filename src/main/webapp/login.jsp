<%@page contentType="text/html" pageEncoding="UTF-8" session="false"%>
<!DOCTYPE html>
<%
    String message = (String)request.getAttribute("message");
    String username = (String)request.getAttribute("username");
    if (username == null) username = "";
    String tag = "";
    if (message != null) {
        tag = "<div class=\"error\">"+message+"</div><br>";
    }
%>
<html>    
    <head>
        <title>Login</title>  
        <meta charset="UTF-8">  
        <meta name="viewport" content="width=device-width">  
        <link href="css/general.css" rel="stylesheet" type="text/css">
    </head>
    <body>
        <form name="InputForm" id="logform" method="post" action="login">  
            <h2>Login</h2>
            <div class="formelem">Username:</div>
            <input type="text" name="username" value="<%= username %>"/>
            <div class="formelem">Password:</div>
            <input type="password" name="password"/>
            <div class="centering"><input type="submit" value="Submit" name="SubmitButton"/></div> 
        </form>
        <%= tag %>
        <div class="foot"><a href="registration.jsp">Sign in</a> as new user</div>
    </body>
</html>