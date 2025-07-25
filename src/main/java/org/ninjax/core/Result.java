package org.ninjax.core;

// FIXME NOT IMMUTABLE

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Result {
    
    public final static String CONTENT_TYPE_TEXT_HTML = "text/html";
    public final static String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    
    // FIXME... THIS IS ALL NOT FINAL!
    int status = 200;
    String contentType = CONTENT_TYPE_TEXT_PLAIN;
    
    String content;
    
    OutputStreamRenderer outputStreamRenderer;
    
    public Result() {
        //this.outputStream = outputStream;
    }
    
    public static Result ok() {
        return new Result().status(200);
    }
    
    public Result html(String content) {
        this.contentType = CONTENT_TYPE_TEXT_HTML;
        this.content = content;
    
        return this;
    }
    
    public Result status(int status) {
        this.status = status;
        return this;
    }
    
    public Result text(String content) {
        this.contentType = CONTENT_TYPE_TEXT_PLAIN;
        this.content = content;
    
        return this;
    }
    
    public Result stream(OutputStreamRenderer outputStreamRenderer) {
        this.outputStreamRenderer = outputStreamRenderer;
        return this;
    }

    public interface OutputStreamRenderer {
       void resultCreatorMethod(OutputStream outputStream);
    }

}
