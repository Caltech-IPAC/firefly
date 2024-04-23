/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util.download;


import edu.caltech.ipac.firefly.server.query.DataAccessException;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 *
 * @author Trey Roby
 */
public class ResponseMessage {
    public static String getHttpResponseMessage(int reposeCode) {
        return switch (reposeCode) {
            case 100 -> "Continue";
            case 101 -> "Switching Protocols";
            case 200 -> "OK";
            case 201 -> "Created";
            case 202 -> "Accepted";
            case 203 -> "Non-Authoritative Information";
            case 204 -> "No Content";
            case 205 -> "Reset Content";
            case 206 -> "Partial Content";
            case 207 -> "Multi-Status";
            case 300 -> "Multiple Choices";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 303 -> "See Other";
            case 304 -> "Not Modified";
            case 305 -> "Use Proxy";
            case 307 -> "Temporary Redirect";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 402 -> "Payment Required";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 406 -> "Not Acceptable";
            case 407 -> "Proxy Authentication Required";
            case 408 -> "Request Time-out";
            case 409 -> "Conflict";
            case 410 -> "Gone";
            case 411 -> "Length Required";
            case 412 -> "Precondition Failed";
            case 413 -> "Request Entity Too Large";
            case 414 -> "Request-URI Too Large";
            case 415 -> "Unsupported Media Type";
            case 416 -> "Requested range not satisfiable";
            case 417 -> "Expectation Failed";
            case 419 -> "Insufficient Space On Resource";
            case 420 -> "Method Failure";
            case 422 -> "Unprocessable Entity";
            case 423 -> "Locked";
            case 424 -> "Failed Dependency";
            case 495 -> "SSL Certificate Error";
            case 500 -> "Internal Server Error";
            case 501 -> "Not Implemented";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Time-out";
            case 505 -> "HTTP Version not supported";
            case 507 -> "Insufficient Storage";
            default -> "Error in retrieving file";
        };
        
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
            return "URL file not found";
        }
        else if (e instanceof UnknownHostException) {
            return "Unknown host: "+ e.getMessage();
        }
        else if (e instanceof SocketTimeoutException) {
            return "Retrieval timed out";
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
