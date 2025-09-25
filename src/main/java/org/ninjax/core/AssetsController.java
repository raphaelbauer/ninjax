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
        Optional<String> requestedFileOpt
                = request.getPathParameter(FILENAME_PATH_PARAM)
                        // or... to serve '/favicon.ico' for instance
                        .or(() -> Optional.of(request.getRequestPath()));

        if (requestedFileOpt.isEmpty()) {
            logger.warn("Opsi. Not able to find: {} - based on param {}.", requestedFileOpt, FILENAME_PATH_PARAM);
            return Result.notFound();
        }

        // Normalize the path to remove dangerous sequences
        Path resourcePath = Paths.get(BASE_DIR, requestedFileOpt.get()).normalize();

        // Prevent directory traversal attacks
        if (!resourcePath.startsWith(BASE_DIR)) {
            logger.warn("Wow. That is strange. I got a request for file {} - but I was not able to normalize that path ({}). Looks dangerous. Returning 404. ",
                    resourcePath.toString(),
                    requestedFileOpt.get());

            return Result.notFound();
        }

        // Load the resource as a stream
        try (InputStream resourceStream = getClass().getResourceAsStream(resourcePath.toString())) {

            if (resourceStream == null) {
                logger.debug("Not able to find resource {}. Returning 404.", resourcePath.toString());
                return Result.notFound();
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
                    throw new RuntimeException("Opsi. An error occurred while reading resource and sending to user.", e);
                }
            });

            return result;

        } catch (IOException ex) {
            logger.error("Error serving resource '{}': {}", resourcePath, ex.getMessage());
            return Result.notFound();
        }
    }

}
