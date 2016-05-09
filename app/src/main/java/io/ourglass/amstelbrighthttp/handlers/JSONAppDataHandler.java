package io.ourglass.amstelbrighthttp.handlers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.Map;

import io.ourglass.amstelbrighthttp.NanoHTTPD;
import io.ourglass.amstelbrighthttp.OGRouterNanoHTTPD;

/**
 * Created by mkahn on 5/9/16.
 */
public class JSONAppDataHandler extends OGRouterNanoHTTPD.DefaultHandler {

    private NanoHTTPD.Response.IStatus responseStatus;

    @Override
    public String getText() {
        return "not implemented";
    }

    public String getText(Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {


        JSONObject responseJson = new JSONObject();

        try {
            responseJson.put("verb", session.getMethod().toString());

            // The "slug" params arrive in a hashmap like "slug:" : "value"
            // So if the slug is /user/:id and you GET /user/steve the HashMap is "id" : "steve"
            JSONArray paramsArr = new JSONArray();
            for (Map.Entry<String, String> entry : urlParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                JSONObject pobj = new JSONObject();
                pobj.put(key, value);
                paramsArr.put(pobj);
            }

            responseJson.put("params", paramsArr);

            // The Query params also arrive (eventually) as a hash of key/value by making
            // the chained call on the session variable

            JSONArray queryArr = new JSONArray();

            for (Map.Entry<String, String> entry : session.getParms().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                queryArr.put( new JSONObject().put(key, value));
            }

            responseJson.put("query", queryArr);
            responseStatus = NanoHTTPD.Response.Status.OK;

        } catch ( Exception e ){

            //responseJson.put("error", e.toString());
            responseStatus = NanoHTTPD.Response.Status.INTERNAL_ERROR;
        }


        return responseJson.toString();
    }

    @Override
    public String getMimeType() {
        // So we'd need to change this for JSON, obviously
        return "application/json";
    }

    @Override
    public NanoHTTPD.Response.IStatus getStatus() {

        return responseStatus;
    }

    public NanoHTTPD.Response get(OGRouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {

        String text = getText(urlParams, session);
        ByteArrayInputStream inp = new ByteArrayInputStream(text.getBytes());
        int size = text.getBytes().length;
        // If you want to add to the response header, you can do that because this method
        // returns a standard response object. See:
        // http://stackoverflow.com/questions/25361457/how-to-send-file-name-with-nanohttpd-response
        return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), inp, size);
    }

}
