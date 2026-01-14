package org.ninja.core;

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

    private static final Logger logger = LoggerFactory.getLogger(AssetsController.class);

    private static final String FILENAME_PATH_PARAM = "fileName";
    private static final Path BASE_DIR = Paths.get("assets");

    public Result serveStatic(Request request) {
        String requestedPath = request.getPathParameter(FILENAME_PATH_PARAM)
                .orElseGet(request::getRequestPath);

        if (requestedPath == null || requestedPath.isEmpty()) {
            logger.warn("No requested file found based on param '{}'.", FILENAME_PATH_PARAM);
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
            logger.warn("Rejected potentially dangerous static file request: {}", requestedPath);
            return Result.builder().notFound().build();
        }

        Path resourcePath = BASE_DIR.resolve(normalized).normalize();
        String resourcePathString = "/" + resourcePath.toString().replace('\\', '/');

        InputStream resourceStream = getClass().getResourceAsStream(resourcePathString);
        if (resourceStream == null) {
            logger.debug("Static resource not found: {}", resourcePathString);
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
                        ByteStreams.copy(in, out);
                    } catch (IOException e) {
                        logger.error("Error streaming static resource {}", resourcePathString, e);
                        throw new RuntimeException("Error streaming static resource", e);
                    }
                })
                .build();
    }
}
