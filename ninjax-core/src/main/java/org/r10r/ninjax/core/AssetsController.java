package org.r10r.ninjax.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AssetsController {

    private static final Logger logger = Logger.getLogger(AssetsController.class.getName());

    private static final String FILENAME_PATH_PARAM = "fileName";
    private static final Path BASE_DIR = Paths.get("assets");

    public Result serveStatic(Request request) {
        String requestedPath = request.getPathParameter(FILENAME_PATH_PARAM)
                .orElseGet(request::getRequestPath);

        if (requestedPath == null || requestedPath.isEmpty()) {
            logger.log(Level.WARNING, "No requested file found based on param '{0}'.", FILENAME_PATH_PARAM);
            return Result.builder().notFound().build();
        }

        // normalize request path
        if (requestedPath.startsWith("/")) {
            requestedPath = requestedPath.substring(1);
        }
        if (requestedPath.startsWith("assets/")) {
            requestedPath = requestedPath.substring("assets/".length());
        }

        Path normalized = Paths.get(requestedPath).normalize();

        // prevent directory traversal
        if (normalized.isAbsolute() || normalized.startsWith("..")) {
            logger.log(Level.WARNING, "Rejected potentially dangerous static file request: {0}", requestedPath);
            return Result.builder().notFound().build();
        }

        Path resourcePath = BASE_DIR.resolve(normalized).normalize();
        String resourcePathString = "/" + resourcePath.toString().replace('\\', '/');

        InputStream resourceStream = getClass().getResourceAsStream(resourcePathString);
        if (resourceStream == null) {
            logger.log(Level.FINE, "Static resource not found: {0}", resourcePathString);
            return Result.builder().notFound().build();
        }

        String fileName = resourcePath.getFileName().toString();
        String mimeType = Optional.ofNullable(URLConnection.guessContentTypeFromName(fileName))
                .orElse("application/octet-stream");

        return Result.builder()
                .ok()
                .contentType(mimeType)
                .stream(out -> {
                    try (InputStream in = resourceStream) {
                        in.transferTo(out);
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error streaming static resource " + resourcePathString, e);
                        throw new RuntimeException("Error streaming static resource", e);
                    }
                })
                .build();
    }
}
