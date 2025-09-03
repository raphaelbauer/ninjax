package org.ninjax.core;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetsController {

    private final static Logger logger = LoggerFactory
            .getLogger(AssetsController.class);
    
    private final static String FILENAME_PATH_PARAM = "fileName";
    private static final String BASE_DIR = "/assets/";
    
    public Result serveStatic(Request request) {
        
        // Get the requested file path
        Optional<String> requestedFileOpt = 
                request.getPathParameter(FILENAME_PATH_PARAM)
                // or... to serve '/favicon.ico' for instance
                .or(() -> Optional.of(request.getRequestPath()));
        
        
        if (requestedFileOpt.isEmpty()) {
            throw new RuntimeException("opsi. Not able to find: " + requestedFileOpt + " - based on param " + FILENAME_PATH_PARAM);
        //            response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
//            return;

        }


        // Normalize the path to remove dangerous sequences
        Path resourcePath = Paths.get(BASE_DIR, requestedFileOpt.get()).normalize();

        // Prevent directory traversal attacks
        if (!resourcePath.startsWith(BASE_DIR)) {
            throw new RuntimeException("opsi");
            //response.sendError(HttpServletResponse.SC_FORBIDDEN); // 403.
            //return;
        }

        // Load the resource as a stream
        InputStream resourceStream = getClass().getResourceAsStream(resourcePath.toString());

        if (resourceStream == null) {
            throw new RuntimeException("opsi");
            //response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
            //return;
        }

        // Determine MIME type
        String mimeType = URLConnection.guessContentTypeFromName(resourcePath.getFileName().toString());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        
        var result = Result.ok().contentType(mimeType).stream(outputStream -> {
            try {
               ByteStreams.copy(resourceStream, outputStream);  
            } catch (IOException e) {
                throw new RuntimeException("opsi", e);
            }
        });
        
        return result;
    }

}
