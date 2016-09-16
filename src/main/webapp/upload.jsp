<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<%
    
    String message = (String)request.getAttribute("message");
    if (message != null) {
        message = "<div class=\"error\">" + message + "</div><br>";
    } else {
        message = "";
    }
    String username = (String)session.getAttribute("firstname");
%>
<html>
    
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Upload a photo</title>
        <link href="css/general.css" rel="stylesheet" type="text/css">
        <script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jquery/3.1.0/jquery.min.js"></script>
        <script type="text/javascript" async="" src="js/upload.js"></script>
        
    <body>
        <div class="head">
            <div class="hello">Hi <%= username%>!</div>
            <div class="right">
                <a href="logout">
                    <img title="Logout" border="0" alt="Logout" src="imgs/logout.png" width="100%">
                </a>
            </div>
            <div class="right">
                <a href="userHome.jsp">
                    <img title="Upload" border="0" alt="Upload" src="imgs/home.png" width="100%">
                </a>
            </div>
        </div>
        <form name="uploadForm" method="POST" action="upload" enctype="multipart/form-data" id="upform"> 
            <h2>Upload new photos</h2> 
            <div id="labelcentering" class="centering">
                <input type="file" name="file" id="inputfile" class="inputfile" data-multiple-caption="{count} files selected" multiple/>
                <label for="inputfile">Select files</label></div>
            <div class="centering"><input type="submit" value="Upload" id="uploadButton" name="SubmitButton"/></div>
            <div class="foot" id="uploadProgress">
                <div class="notVisible" id="progress"></div>
                <div class="notVisible" id="counter"></div>
                <div id="progressBar"></div>
            </div>
            <div class="foot" id="message"></div>
            <div class="foot" id="details"></div>
        </form>
        <%= message %>
    </body>
</html>
