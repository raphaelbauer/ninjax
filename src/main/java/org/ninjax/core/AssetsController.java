//package org.ninjax.core;
//
//import com.google.common.io.ByteStreams;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.net.URL;
//import java.net.URLConnection;
//
//public class AssetsController {
//
//    public final static String FILENAME_PATH_PARAM = "fileName";
//
//    public Result serveStatic(Context context) {
//        String fileName = getFileNameFromPathOrReturnRequestPath(context);
//        URL url = getStaticFileFromAssetsDir(fileName);
//        streamOutUrlEntity(url, context, result);
//    }
//    
//     private void streamOutUrlEntity(URL url, Context context, Result result) {
//        // check if stream exists. if not print a notfound exception
//        if (url == null) {
//            context.finalizeHeadersWithoutFlashAndSessionCookie(Results.notFound());
//        } else if (assetsControllerHelper.isDirectoryURL(url)) {
//            // Disable listing of directory contents
//            context.finalizeHeadersWithoutFlashAndSessionCookie(Results.notFound());
//        } else {
//            try {
//                URLConnection urlConnection = url.openConnection();
//                Long lastModified = urlConnection.getLastModified();
//                httpCacheToolkit.addEtag(context, result, lastModified);
//
//                if (result.getStatusCode() == Result.SC_304_NOT_MODIFIED) {
//                    // Do not stream anything out. Simply return 304
//                    context.finalizeHeadersWithoutFlashAndSessionCookie(result);
//                } else {
//                    result.status(200);
//
//                    // Try to set the mimetype:
//                    String mimeType = mimeTypes.getContentType(context,
//                            url.getFile());
//
//                    if (mimeType != null && !mimeType.isEmpty()) {
//                        result.contentType(mimeType);
//                    }
//
//                    ResponseStreams responseStreams = context
//                            .finalizeHeadersWithoutFlashAndSessionCookie(result);
//
//                    try (InputStream inputStream = urlConnection.getInputStream();
//                        OutputStream outputStream = responseStreams.getOutputStream()) {
//                        ByteStre2ams.copy(inputStream, outputStream);
//                    }
//
//                }
//
//            } catch (IOException e) {
//                logger.error("error streaming file", e);
//            }
//
//        }
//
//    }
//
//    private static String getFileNameFromPathOrReturnRequestPath(Context context) {
//
//        //String fileName = context.getPathParameter(FILENAME_PATH_PARAM);
//        //if (fileName == null) {
//        String fileName = context.route().path();
//        //}
//        return fileName;
//
//    }
//    
//     /**
//     * Loads files from assets directory. This is the default directory
//     * of Ninja where to store stuff. Usually in src/main/java/assets/.
//     */
//    private URL getStaticFileFromAssetsDir(String fileName) {
//
//        //URL url;
//
////        if (ninjaProperties.isDev() 
////                // Testing that the file exists is important because
////                // on some dev environments we do not get the correct asset dir
////                // via System.getPropery("user.dir").
////                // In that case we fall back to trying to load from classpath
////                && new File(assetsDirInDevModeWithoutTrailingSlash()).exists()) {
////            String finalNameWithoutLeadingSlash = assetsControllerHelper.normalizePathWithoutLeadingSlash(fileName, false);
////            File possibleFile = new File(
////                    assetsDirInDevModeWithoutTrailingSlash() 
////                            + File.separator 
////                            + finalNameWithoutLeadingSlash);
////            url = getUrlForFile(possibleFile);
////        } else {
//            String finalNameWithoutLeadingSlash = assetsControllerHelper.normalizePathWithoutLeadingSlash(fileName, true);
//            URL url = this.getClass().getClassLoader()
//                    .getResource(ASSETS_DIR
//                                 + "/"
//                                 + finalNameWithoutLeadingSlash);
//        //}
//
//        return url;
//    }
//    
//    
//    /**
//     * If we get - for whatever reason - a relative URL like
//     * assets/../conf/application.conf we expand that to the "real" path. In the
//     * above case conf/application.conf.
//     *
//     * You should then add the assets prefix.
//     *
//     * Otherwise someone can create an attack and read all resources of our app.
//     * If we expand and normalize the incoming path this is no longer possible.
//     *
//     * @param fileName A potential "fileName"
//     * @param enforceUnixSeparator If true it will force the usage of the unix separator '/'
//     *                             If false it will use the separator of the underlying system.
//     *                             usually '/' in case of unix and '\' in case of windows.
//     * @return A normalized fileName.
//     */
//    public String normalizePathWithoutLeadingSlash(String fileName, boolean enforceUnixSeparator) {
//        String fileNameNormalized = enforceUnixSeparator
//                ? FilenameUtils.normalize(fileName, true)
//                : FilenameUtils.normalize(fileName);
//        return StringUtils.removeStart(fileNameNormalized, "/");
//    }
//
//}
