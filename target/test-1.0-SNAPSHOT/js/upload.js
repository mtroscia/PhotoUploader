var button = window.document.getElementById("uploadButton");
var message = window.document.getElementById("message");
var progress = window.document.getElementById("progress");
var timeoutGetProgress;
var timeoutDoProgressLoop;
button.disabled = true;
var inputForm = document.getElementById('inputfile');
// label is the html element which is the adjacent sibiling of the inputForm element
var label = inputForm.nextElementSibling,
// at first it takes the default content "Select files"
labelVal = label.innerHTML;

inputForm.addEventListener('change', function(e) {
    var fileName = '';
    // input[type=file] changes the file lists when files are selected
    if (this.files && this.files.length > 1)
        // more than one file has been selected
        // fileName takes the content "# files selected"
        fileName = (this.getAttribute('data-multiple-caption') || '').replace('{count}', this.files.length);
    else {
        // only one file has been selected
        fileName = e.target.value.split('\\').pop();
        // if the file name is too long, it is truncated
        if (fileName.length > 20) {
            fileName = fileName.substring(0, 8) + '...' + fileName.substring(fileName.length - 9, fileName.length);
        }
    }
    if(fileName) {
        // one or more file has been selected
        label.innerHTML = fileName;
        document.getElementById("uploadButton").disabled = false;
    } else {
        // no new file has been selected and the label does not change its content
        label.innerHTML = labelVal;
    }
});

// function called when files are submitted
$('#upform').submit(function(e) {
    // when submitting, do not change the current page (prevent default behavior)
    e.preventDefault();

    // do not permit any other upload during the upload process
    button.disabled = true;
    button.value = "Uploading...";
    // cancell all the messages coming from previous uploads
    message.innerHTML = "";
    // show the progress bar
    document.getElementById("progressBar").style.display = "block";
    document.getElementById("uploadProgress").style.display = "block";
    // send the form content
    $.ajax( {
        url: $(this).attr('action'),
        type: 'POST',
        data: new FormData( this ),
        processData: false,
        contentType: false,
        success : function(data, textStatus, jqXHR) {
            var contentType = jqXHR.getResponseHeader("Content-Type");
            // if the server sends back another page, write the page
            // e.g. if there is no session, the login page is sent back
            if (contentType.indexOf("text/html") != -1) {
                document.write(data);
            // the server sends the upload results
            } else if (contentType.indexOf("application/json") != -1) {
                resetForm();
                // If the upload has been too fast, the progress bar may not be
                // at 100%, so set it at 100%
                updateProgressBar(100);
                // If some photos have not been uploaded a message starting
                // with "The following file..." is displayed
                var index = data.indexOf("The following");
                if (index != -1) {
                    // create the button to show/hide upload details
                    message.innerHTML = data.substring(0, index);
                    var show = document.createElement("a");
                    show.setAttribute("onclick", "manageDetails(this)");
                    show.setAttribute("href", "#");
                    show.setAttribute("class", "topmargin");
                    // in order to apply the css style margin-top the html <a>
                    // element must be displayed inline-block
                    show.style.display = "inline-block";
                    show.appendChild(document.createTextNode("Show details"));
                    // create the div with the upload details
                    var details = document.createElement("div");
                    details.id = "details";
                    details.style.display = "none";
                    details.innerHTML = data.substring(index);
                    message.appendChild(show);
                    message.appendChild(details);
                } else {
                    message.innerHTML = data;
                }
            }
        },
        error : function(jqXHR, textStatus, errorThrown) {
            resetForm();
            message.innerHTML = errorThrown;
        }
    });
    doProgress();
});

function resetForm() {
    button.value = "Upload";
    // reset the file input form
    var inputForm = document.getElementById("inputfile");
    inputForm.value = "";
    // reset the label to choose files
    inputForm.nextElementSibling.innerHTML = "Select files";
    /* the following must be set to 0 as if it were left be (with the value of
     * 100), in the case of another upload, the function doProgressLoop would
     * stop after the first cycle as the function getProgress would update this
     * value only after receiving the server response (which usally happens
     * after the call to doProgressLoop)
     */
    progress.innerHTML = 0;
    clearTimeout(timeoutGetProgress);
    clearTimeout(timeoutDoProgressLoop);
}

function doProgress() {
    var max = 100;
    var prog = 0;
    var counter = 0;
    getProgress();
    doProgressLoop(prog, max, counter);
}

function getProgress() {
    $.ajax( {
       url : "uploadProgress",
       type : "POST",
       success : function(data, textStatus, jqXHR) {
           progress.innerHTML = data;
       }
    });
}

function doProgressLoop(prog, max, counter) {
    var x = parseInt(progress.innerHTML);
    if (!isNaN(x)) {
        prog = x;
    }
    updateProgressBar(prog);
    counter = counter + 1;
    document.getElementById("counter").innerHTML = counter;
    if (prog < 100) {
        timeoutGetProgress = setTimeout("getProgress()", 500);
        timeoutDoProgressLoop = setTimeout("doProgressLoop(" + prog + "," + max + "," + counter + ")", 1000);
    }
    else {
        message.innerHTML = "Please wait while the server is saving your photos."
    }
}

function updateProgressBar(progress) {
    var progressBar = document.getElementById("progressBar");
    var totWidth = document.getElementById("uploadProgress").offsetWidth;
    progressBar.style.width = Math.ceil((progress/100 * totWidth)) + "px";
}

function manageDetails(e) {
    var details = document.getElementById("details");
    var displayStatus = details.currentStyle ? details.currentStyle.display :
        getComputedStyle(details, null).display;
    if(displayStatus == "block") {
            e.innerHTML = "Show details";
            details.style.display = "none";
    } else {
        e.innerHTML = "Hide details";
        details.style.display = "block";
    }
}

