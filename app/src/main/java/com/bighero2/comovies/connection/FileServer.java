package com.bighero2.comovies.connection;

import android.os.Environment;
import android.util.Log;

import com.bighero2.comovies.NanoHTTPD.NanoHTTPD;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Chau Pham on 4/15/2016.
 */
public class FileServer extends NanoHTTPD{
    public FileServer() {
        super(8080);
    }

    private String fileToServe ="";
    private String serveFileType = "";

    // set file to serve
    public void setFileToServe(String path, String fileType){
        fileToServe = path;
        if(fileType.equals("mp4")){
            serveFileType = "video/mp4";
        }
        else
            Log.d("test", "File type not supported.");
    }

    @Override
    public Response serve(String uri, Method method,
                          Map<String, String> header, Map<String, String> parameters,
                          Map<String, String> files) {
        String answer = "";


        File fis = new File(fileToServe);

        return serveFile(uri, header, fis, serveFileType);
    }

    private Response createResponse(Response.Status status, String mimeType,
                                    InputStream message) {
        Response res = newChunkedResponse(status, serveFileType, message);

        //new Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    private Response serveFile(String uri, Map<String, String> header,
                               File file, String mime) {
        Response res;
        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath()
                    + file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range
                                    .substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Change return code and add Content-Range header when skipping is
            // requested
            long fileLen = file.length();
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    res = createResponse(Response.Status.RANGE_NOT_SATISFIABLE,
                            NanoHTTPD.MIME_PLAINTEXT, new ByteArrayInputStream(new String("").getBytes()));
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    final long dataLen = newLen;
                    FileInputStream fis = new FileInputStream(file) {
                        @Override
                        public int available() throws IOException {
                            return (int) dataLen;
                        }
                    };
                    fis.skip(startFrom);

                    res = createResponse(Response.Status.PARTIAL_CONTENT, mime,
                            fis);
                    res.addHeader("Content-Length", "" + dataLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-"
                            + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (etag.equals(header.get("if-none-match")))
                    res = createResponse(Response.Status.NOT_MODIFIED, mime, new ByteArrayInputStream(new String("").getBytes()));
                else {
                    res = createResponse(Response.Status.OK, mime,
                            new FileInputStream(file));
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = createResponse(Response.Status.FORBIDDEN,
                    NanoHTTPD.MIME_PLAINTEXT, new ByteArrayInputStream(new String("FORBIDDEN: Reading file failed.").getBytes()));
        }

        return res;
    }
}
