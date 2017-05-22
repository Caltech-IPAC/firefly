/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util.download;
/**
 * User: roby
 * Date: 5/18/17
 * Time: 12:04 PM
 */


import edu.caltech.ipac.firefly.server.query.DataAccessException;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * @author Trey Roby
 */
public class ResponseMessage {
    public static String getHttpResponseMessage(int reponseCode) {
        switch (reponseCode) {
            case  100 : return "Continue";
            case  101 : return "Switching Protocols";
            case  200 : return "OK";
            case  201 : return "Created";
            case  202 : return "Accepted";
            case  203 : return "Non-Authoritative Information";
            case  204 : return "No Content";
            case  205 : return "Reset Content";
            case  206 : return "Partial Content";
            case  300 : return "Multiple Choices";
            case  301 : return "Moved Permanently";
            case  302 : return "Found";
            case  303 : return "See Other";
            case  304 : return "Not Modified";
            case  305 : return "Use Proxy";
            case  307 : return "Temporary Redirect";
            case  400 : return "Bad Request";
            case  401 : return "Unauthorized";
            case  402 : return "Payment Required";
            case  403 : return "Forbidden";
            case  404 : return "Not Found";
            case  405 : return "Method Not Allowed";
            case  406 : return "Not Acceptable";
            case  407 : return "Proxy Authentication Required";
            case  408 : return "Request Time-out";
            case  409 : return "Conflict";
            case  410 : return "Gone";
            case  411 : return "Length Required";
            case  412 : return "Precondition Failed";
            case  413 : return "Request Entity Too Large";
            case  414 : return "Request-URI Too Large";
            case  415 : return "Unsupported Media Type";
            case  416 : return "Requested range not satisfiable";
            case  417 : return "Expectation Failed";
            case  500 : return "Internal Server Error";
            case  501 : return "Not Implemented";
            case  502 : return "Bad Gateway";
            case  503 : return "Service Unavailable";
            case  504 : return "Gateway Time-out";
            case  505 : return "HTTP Version not supported";
            default:    return "Error in retrieving file";
        }
        
    }


    public static FailedRequestException simplifyNetworkCallException(Exception e) {

        if (e instanceof FailedRequestException) {
            return (FailedRequestException)e;
        }
        else if (e.getCause() instanceof FailedRequestException) {
            return new FailedRequestException(e.getCause().getMessage(), "", e);
        }
        else {
            return new FailedRequestException(getNetworkCallFailureMessage(e), "", e);
        }
    }

    public static String getNetworkCallFailureMessage(Exception e) {
        if (e.getCause() instanceof FailedRequestException) {
            return e.getCause().getMessage();
        }
        else if (e instanceof DataAccessException) {
            return e.getMessage();
        }
        else if (e instanceof MalformedURLException) {
            return "Invalid URL";
        }
        else if (e instanceof FileNotFoundException) {
            return "URL File not found";
        }
        else if (e instanceof UnknownHostException) {
            return "Unknown host: "+ e.getMessage();
        }
        else if (e instanceof SocketTimeoutException) {
            return "Retrieval Timed out";
        }
        else if (e instanceof SocketException) {
            return "Could not connect to service";
        }
        else if (e instanceof IOException) {
            return (e.getCause() instanceof EOFException) ? "No data returned" : "IO problem";
        }
        else {
            return e.getMessage();
        }

    }
}
