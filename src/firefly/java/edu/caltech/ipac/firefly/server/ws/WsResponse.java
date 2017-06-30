package edu.caltech.ipac.firefly.server.ws;

import edu.caltech.ipac.firefly.data.WspaceMeta;

import java.util.List;

/**
 * Created by ejoliet on 6/16/17.
 */
public class WsResponse {

    private String statusCode = "";
    private String statusText = "";
    private String response = "";
    private List<WspaceMeta> meta;
    public WsResponse() {
    }

    public WsResponse(String code, String text, String responseString) {
        statusCode = code;
        statusText = text;
        response = responseString;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public void setWspaceMeta(List<WspaceMeta> meta) {
        this.meta = meta;
    }

    public List<WspaceMeta> getWspaceMeta() {
        return this.meta;
    }

    public String toString() {
        String result = "status code : " + statusCode + "\n" +
                "status text : " + statusText + "\n" +
                "   response : " + response;
        return result;
    }

    public boolean doContinue(){
        return true;
    }

}
