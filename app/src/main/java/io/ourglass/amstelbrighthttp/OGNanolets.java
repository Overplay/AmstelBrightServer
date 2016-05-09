package io.ourglass.amstelbrighthttp;

import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import io.ourglass.amstelbrighthttp.handlers.JSONTestHandler;


public class OGNanolets extends OGRouterNanoHTTPD {

    private static final int PORT = 9090;

    /**
     * Create the server instance
     */
    public OGNanolets() throws IOException {
        super(PORT);
        addMappings();
        start();
    }

    /**
     * This is an example handler that comes with NanoHTTP example. Keep it here so we
     * can use it as, well, and example :D (MAK)
     *
     */
    public static class UserHandler extends DefaultHandler {

        private Boolean should404;

        @Override
        public String getText() {
            return "not implemented";
        }

        public String getText(Map<String, String> urlParams, IHTTPSession session) {

            String text = "<html><body>User handler. Method: " + session.getMethod().toString() + "<br>";

            text += "<h1>Uri parameters:</h1>";

            // The "slug" params arrive in a hashmap like "slug:" : "value"
            // So if the slug is /user/:id and you GET /user/steve the HashMap is "id" : "steve"
            for (Map.Entry<String, String> entry : urlParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                text += "<div> Param: " + key + "&nbsp;Value: " + value + "</div>";
            }


            text += "<h1>Query parameters:</h1>";

            // The Query params also arrive (eventually) as a hash of key/value by making
            // the chained call on the session variable
            for (Map.Entry<String, String> entry : session.getParms().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                // MAK added this to make sure we could set error codes
                if ("404".equals(value)){
                    should404 = true;
                }
                text += "<div> Query Param: " + key + "&nbsp;Value: " + value + "</div>";
            }
            text += "</body></html>";

            return text;
        }

        @Override
        public String getMimeType() {
            // So we'd need to change this for JSON, obviously
            return "text/html";
        }

        @Override
        public Response.IStatus getStatus() {
            return should404 ? Response.Status.NOT_FOUND :  Response.Status.OK;
        }

        public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            should404 = false;
            String text = getText(urlParams, session);
            ByteArrayInputStream inp = new ByteArrayInputStream(text.getBytes());
            int size = text.getBytes().length;
            // If you want to add to the response header, you can do that because this method
            // returns a standard response object. See:
            // http://stackoverflow.com/questions/25361457/how-to-send-file-name-with-nanohttpd-response
            return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), inp, size);
        }

    }

    static public class StreamUrl extends DefaultStreamHandler {

        @Override
        public String getMimeType() {
            return "text/plain";
        }

        @Override
        public Response.IStatus getStatus() {
            return Response.Status.OK;
        }

        @Override
        public InputStream getData() {
            return new ByteArrayInputStream("a stream of data ;-)".getBytes());
        }

    }

    static class StaticPageTestHandler extends StaticPageHandler {

        @Override
        protected BufferedInputStream fileToInputStream(File fileOrdirectory) throws IOException {
            if ("exception.html".equals(fileOrdirectory.getName())) {
                throw new IOException("trigger something wrong");
            }
            return super.fileToInputStream(fileOrdirectory);
        }
    }


    /**
     * Add the routes Every route is an absolute path Parameters starts with ":"
     * Handler class should implement @UriResponder interface If the handler not
     * implement UriResponder interface - toString() is used
     */
    @Override
    public void addMappings() {
        super.addMappings();
        addRoute("/user", UserHandler.class);
        addRoute("/user/:id", UserHandler.class);
        addRoute("/user/help", GeneralHandler.class);
        addRoute("/json/:id/:eid", JSONTestHandler.class);
        addRoute("/general/:param1/:param2", GeneralHandler.class);
        addRoute("/photos/:customer_id/:photo_id", null);
        addRoute("/test", String.class);
        addRoute("/interface", UriResponder.class); // this will cause an error
        // when called
        addRoute("/toBeDeleted", String.class);
        removeRoute("/toBeDeleted");
        addRoute("/stream", StreamUrl.class);
        addRoute("/www/(.)+", StaticPageTestHandler.class, new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/www").getAbsoluteFile());
    }


}
