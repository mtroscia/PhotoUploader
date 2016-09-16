<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<%
    String username = (String)session.getAttribute("firstname");
%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title><%= username%>'s photos</title>
        <link href="css/general.css" rel="stylesheet" type="text/css">
        <script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jquery/3.1.0/jquery.min.js"></script>
        <script type="text/javascript" async="" src="js/populateHome.js"></script>
        <script type="text/javascript" async="" src="js/handleCookies.js"></script>
    </head>
    <body onload="getPhotosList();">
        <div class="head">
            <div id="hello">
                <div class="hello">Hi <%= username%>!</div>
                <div class="right">
                    <a href="logout">
                        <img title="Logout" border="0" alt="Logout" src="imgs/logout.png" width="100%">
                    </a>
                </div>
                <div class="right">
                    <a href="upload.jsp">
                        <img title="Upload" border="0" alt="Upload" src="imgs/upload.png" width="100%">
                    </a>
                </div>
            </div>
            <div id="selection">
                <div class="hello" id="numberSelected"></div>
                <div class="right">
                    <a href="#" onclick="deselectAll();">
                        <img title="Undo" border="0" alt="Undo" src="imgs/deselect.png" width="100%">
                    </a>
                </div>
                <div class="right">
                    <a href="#" onclick="deletePhotos();">
                        <img title="Delete" border="0" alt="Delete" src="imgs/delete.png" height="100%">
                    </a>
                </div>
                <div class="right">
                    <a href="#" onclick="downloadPhotos();">
                        <img title="Download" border="0" alt="Download" src="imgs/download.png" width="100%">
                    </a>
                </div>
            </div>
        </div>
            <div id="photos"></div>
    </body>
</html>
