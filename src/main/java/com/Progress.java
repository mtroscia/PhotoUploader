/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com; 

import org.apache.commons.fileupload.ProgressListener;

public class Progress implements ProgressListener {
    private int progress;
    private String message;
    private int block = -1;
    @Override
    public void update(long bytesRead, long contentLength, int items) {
        // performance issue: this method, depending on the servlet engine,
        // can be called every network packet. Solution:
        // update values only when bytesRead exceeds groups of 100k bytes.
        // when the upload reaches the 100% the progress value is always updated
        int kBytesRead = (int)bytesRead / 100000;
        if (block == kBytesRead && contentLength != bytesRead) {
            return;
        }
        block = kBytesRead;
        
        // check if the length of the uploaded file is available
        if (contentLength != -1) {
            // total content length is available
            progress = (int)(((float)bytesRead / (float)contentLength) * 100.0);
            message = bytesRead + " of " + contentLength + " B transmitted.";
        } else {
            // total content length is not available
            progress = -1;
            message = bytesRead + "B transmitted.";
        }
    }
    public int getProgress() {
        return progress;
    }
    public String getMessage() {
        return message;
    }
}
