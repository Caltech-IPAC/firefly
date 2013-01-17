package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.RequestType;

import java.util.HashMap;
import java.util.Map;
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 10:45:12 AM
 */


/**
 * @author Trey Roby
 */
public final class FileRetrieverFactory {

    private static final FileRetrieverFactory _instance= new FileRetrieverFactory();
    private final Map<RequestType, FileRetriever> _types=
                      new HashMap<RequestType, FileRetriever>();

    private FileRetrieverFactory() {
        _types.put(RequestType.FILE,      new LocalFileRetriever());
        _types.put(RequestType.URL,       new URLFileRetriever());
        _types.put(RequestType.SERVICE,   new ServiceRetriever());
        _types.put(RequestType.ALL_SKY,   new AllSkyRetriever());
        _types.put(RequestType.PROCESSOR, new ProcessorFileRetriever());
        _types.put(RequestType.BLANK,     new BlankFileRetriever());
        _types.put(RequestType.TRY_FILE_THEN_URL, new TryFileThenURLRetriever());
    }

    public static FileRetriever getRetriever(WebPlotRequest request) {

        RequestType type;
        if (request.containsParam(WebPlotRequest.TYPE)) {
            type= request.getRequestType();
        }
        else {
            if (request.containsParam(WebPlotRequest.FILE))             type= RequestType.FILE;
            else if (request.containsParam(WebPlotRequest.URL))         type= RequestType.URL;
            else if (request.containsParam(WebPlotRequest.SURVEY_KEY))  type= RequestType.SERVICE;
            else if (request.containsParam(WebPlotRequest.SURVEY_KEY))  type= RequestType.SERVICE;
            else if (request.hasID())                                   type= RequestType.PROCESSOR;
            else                                                        type= RequestType.ALL_SKY;
        }
        return _instance._types.get(type);
    }

}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
