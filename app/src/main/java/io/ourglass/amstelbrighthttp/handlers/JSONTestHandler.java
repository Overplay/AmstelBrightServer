package io.ourglass.amstelbrighthttp.handlers;

import java.io.ByteArrayInputStream;
import java.util.Map;

import io.ourglass.amstelbrighthttp.NanoHTTPD;
import io.ourglass.amstelbrighthttp.OGRouterNanoHTTPD;

/**
 * Created by mkahn on 5/9/16.
 */
public class JSONTestHandler extends OGRouterNanoHTTPD.DefaultHandler {

    private Boolean should404;

    @Override
    public String getText() {
        return "not implemented";
    }

    public String getText(Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {

        String text = "<html><body>User handler. Method: " + session.getMethod().toString() + "<br>";

        text += "<h1>Uri parameters bitches!:</h1>";

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
    public NanoHTTPD.Response.IStatus getStatus() {
        return should404 ? NanoHTTPD.Response.Status.NOT_FOUND :  NanoHTTPD.Response.Status.OK;
    }

    public NanoHTTPD.Response get(OGRouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
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
