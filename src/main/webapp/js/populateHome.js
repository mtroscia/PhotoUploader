// this mantains all the photos data available in the server
var list;
// this mantains the number of photos that have been downloaded
var displayedPhotos;
// this mantains the selected photos to download or to delete
var selectedPhotos = [];

function getPhotosList() {
    displayedPhotos = -1;
    $.ajax({
        url : "getPhotosList",
        type : "POST",
        contentType : "text/plain",
        success : elaborateList
    });
}

function elaborateList(data, textStatus, jqXHR) {
    if (data == "0") {
        // when data == "0" means that the client has not uploaded any photo yet
        noPhotoToDisplay();
        return;
    } else if (data == "") {
        // if the server gives back an empty list, it means that the client
        // already has the updated list saved in the cookie
        list = getCookie("photosList").split('|');
    } else {
        // save the photos' list in the cookie
        setCookie("photosList", data, 1);
        list = data.split('|');
    }
    for (var i = 0; i < list.length; i++) {
        list[i] = list[i].split(',');
        // convert the date in a long number
        list[i][1] = parseFloat(list[i][1]);
        // convert the width in a int
        list[i][2] = parseInt(list[i][2]);
        // convert the heigth in a int
        list[i][3] = parseInt(list[i][3]);
    }
    list.sort(compareElements);
    window.onscroll = getPhotos;
    window.onresize = refillRows;
    getPhotos();
}

function compareElements(a, b) {
    if (a[1] > b[1]) return -1;
    else if (a[1] == b[1]) return 0;
    else return 1;
}

function noPhotoToDisplay() {
    
}

function getOffset(element) {
  element = element.getBoundingClientRect();
  return {
    left: element.left + window.pageXOffest,
    top: element.top + window.pageYOffset
  };
}

function getPhotos() {
    var pageWidth = window.innerWidth ||
            document.documentElement.clientWidth ||
            document.body.clientWidth;
    document.getElementById("photos").style.width = (pageWidth - 30) + "px";
    var lastDisplayedPhoto;
    while(list.length > displayedPhotos+1) {
        if (displayedPhotos > -1) {
            lastDisplayedPhoto = document.getElementById(list[displayedPhotos][0]);
            var offsetY = getOffset(lastDisplayedPhoto).top;
            var pageHeight = window.innerHeight ||
                    document.documentElement.clientHeight ||
                    document.body.clientHeight;
            if (offsetY > window.pageYOffset + pageHeight) {
                return;
            }
        }
        fillARow();
    }
    if (displayedPhotos+1 == list.length) {
        window.onscroll = null;
    }
}

function fillARow() {
    var pageWidth = document.getElementById("photos").offsetWidth - 4;
    var row = [];
    var scale;
    for (var i = displayedPhotos + 1; i < list.length; i++) {
        row.push([list[i][0], list[i][2], list[i][3]]);
        // get the scale factor to have the same height as the first photo in the row
        scale = row[0][2] / row[row.length-1][2];
        row[row.length-1][3] = scale;
        var totalWidth = 0;
        row.forEach(function(element) {
            totalWidth += element[1]*element[3];
        });
        scale = (pageWidth - row.length * 4) / totalWidth;
        if (row[0][2] * scale < 300) {
            break;
        }
    }
    // the last row can have a height greater than 300px:
    // force the height to 300px
    if (row[0][2] * scale > 300) {
        scale = scale * (300 / (row[0][2] * scale));
    }
    row.forEach(function(element) {
        var photoDiv = document.createElement("div");
        photoDiv.style.width = Math.floor(element[1] * element[3] * scale) + "px";
        photoDiv.style.height = Math.floor(element[2] * element[3] * scale) + "px";
        photoDiv.id = element[0];
        photoDiv.setAttribute("class", "image");
        var photo = createImgElement(element[0]);
        var selectionDiv = createSelectionButton();
        photoDiv.appendChild(photo);
        photoDiv.appendChild(selectionDiv);
        photoDiv.setAttribute("onmouseover", "displaySelection(this);");
        photoDiv.setAttribute("onmouseout", "displaySelection(this);");
        photoDiv.setAttribute("selected", "false");
        document.getElementById("photos").appendChild(photoDiv);
    });
    displayedPhotos += row.length;
}

function refillRows() {
    var pageWidth = window.innerWidth ||
            document.documentElement.clientWidth ||
            document.body.clientWidth;
    document.getElementById("photos").style.width = (pageWidth - 30) + "px";
    pageWidth = document.getElementById("photos").offsetWidth - 4;
    var row = [];
    var scale;
    var i = 0;
    var added = 0;
    do {
        do {
            row.push([list[i][0], list[i][2], list[i][3]]);
            i++;
            // get the scale factor to have the same height as the first photo in the row
            scale = row[added][2] / row[row.length-1][2];
            row[row.length-1][3] = scale;
            var totalWidth = 0;
            for (var j = added; j < row.length; j++) {
                totalWidth += row[j][1] * row[j][3];
            }
            scale = (pageWidth - (row.length - added) * 4) / totalWidth;
        } while (row[added][2] * scale >= 300 && i < displayedPhotos+1);
        // the last row can have a height greater than 300px:
        // force the height to 300px
        if (row[added][2] * scale > 300) {
            scale = scale * (300 / (row[added][2] * scale));
        }
        for (var j = added; j < row.length; j++) {
            row[j][3] *= scale;
        }
        added += row.length - added;
    } while (i < displayedPhotos+1);
    var children = document.getElementById("photos").childNodes;
    for (var h = 0; h < row.length; h++) {
        children[h].style.width = Math.floor(row[h][1] * row[h][3]) + "px";
        children[h].style.height = Math.floor(row[h][2] * row[h][3]) + "px";
    }
}

function createImgElement(id) {
    var photo = document.createElement("img");
    photo.setAttribute("border", "0");
    photo.setAttribute("src", "getThumbnail/" + id);
    photo.setAttribute("height", "100%");
    photo.setAttribute("width", "100%");
    return photo;
}

function createSelectionButton() {
    var button = document.createElement("div");
    button.style.display = "none";
    button.setAttribute("class", "selection");
    var img = document.createElement("img");
    img.setAttribute("border", "0");
    img.setAttribute("src", "imgs/selectOut.png");
    img.setAttribute("height", "100%");
    button.appendChild(img);
    button.setAttribute("onclick", "selectPhoto(this);");
    button.setAttribute("onmouseover", "selectOver(this);");
    button.setAttribute("onmouseout", "selectOut(this);");
    return button;
}

function displaySelection(e) {
    var selection = e.lastChild;
    if (e.getAttribute("selected") == "false") {
        if (selection.style.display == "none") {
            selection.style.display = "inline";
        } else {
            selection.style.display = "none";
        }
    }
}

function selectOver(e) {
    if (e.parentNode.getAttribute("selected") == "false") {
        e.firstChild.setAttribute("src", "imgs/selectOver.png");
    }
}

function selectOut(e) {
    if (e.parentNode.getAttribute("selected") == "false") {
        e.firstChild.setAttribute("src", "imgs/selectOut.png");
    }
}

function selectPhoto(e) {
    var parent = e.parentNode;
    if (parent.getAttribute("selected") == "false") {
        parent.setAttribute("selected", "true");
        e.firstChild.setAttribute("src", "imgs/selected.png");
        selectedPhotos.push(parent);
        document.getElementById("hello").style.display = "none";
        var selection = document.getElementById("selection");
        selection.style.display = "inline";
        document.getElementById("numberSelected").innerHTML = selectedPhotos.length + " photo" +
                (selectedPhotos.length > 1 ? "s":"") + " selected";
    } else {
        parent.setAttribute("selected", "false");
        e.firstChild.setAttribute("src", "imgs/selectOver.png");
        var index = selectedPhotos.indexOf(parent);
        selectedPhotos.splice(index, 1);
        document.getElementById("numberSelected").innerHTML = selectedPhotos.length + " photo" +
                (selectedPhotos.length > 1 ? "s":"") + " selected";
        if (selectedPhotos.length == 0) {
            document.getElementById("selection").style.display = "none";
            document.getElementById("hello").style.display = "inline";
        }
    }
}

function deselectAll() {
    while (selectedPhotos.length > 0) {
        var element = selectedPhotos.pop();
        element.setAttribute("selected", "false");
        element.lastChild.firstChild.setAttribute("src", "imgs/selectOut.png");
        element.lastChild.style.display = "none";
    }
    document.getElementById("selection").style.display = "none";
    document.getElementById("hello").style.display = "inline";
}

function deletePhotos() {
    var data = selectedPhotos.pop().id;
    while (selectedPhotos.length > 0) {
        data += ";" + selectedPhotos.pop().id;
    }
    $.ajax({
        url : "delete",
        type : "POST",
        contentType : "text/plain",
        data : data,
        success : refreshPage
    });
}

function downloadPhotos() {
    var data = selectedPhotos[0].id;
    for (var i = 1; i < selectedPhotos.length; i++) {
        data += ";" + selectedPhotos[i].id;
    }
    window.location = "download/photos.zip?photos="+data;
    deselectAll();
}

function refreshPage() {
    document.getElementById("photos").innerHTML = "";
    getPhotosList();
    document.getElementById("selection").style.display = "none";
    document.getElementById("hello").style.display = "inline";
}