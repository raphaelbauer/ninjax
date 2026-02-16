package org.r10r.ninjax.core;

import java.io.InputStream;

public record FileItem(
        String fileName,
        String contentType,
        long size,
        InputStream inputStream) {

}
