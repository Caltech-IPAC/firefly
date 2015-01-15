/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

import java.util.EventObject;

/**
 * The event that is passed when bytes are downloaded
 * @author Trey Roby
 */
public class DownloadEvent extends EventObject {

    private static final int PROGRESS_ONLY= 999;
    private static final int MESSAGE_ONLY = 888;
    private static final int BOTH         = 777;

    private long    _max    = 0;
    private long    _current= 0;
    private String _message= "";
    private int    _type;
    private long   _elapseSec;
    private long   _remainingSec;
    private String _elapseStr;
    private String _remainingStr;
    private Object _downloadObj;
    /**
     * Create a new DownloadEvent with progress information 
     * @param source source of event
     * @param current number of bytes downloaded
     * @param max    maximum number of bytes to download
     */
    public DownloadEvent (Object source, int current, int max) {
        super(source);
        _current= current;
        _max    = max;
        _type= PROGRESS_ONLY;
    }

    /**
     * Create a new DownloadEvent with message information
     * @param source of event
     * @param mess message
     */
    public DownloadEvent (Object source,  String mess) {
        super(source);
        _type= MESSAGE_ONLY;
        _message= mess;
    }

    /**
     * Create a new DownloadEvent with progress and message information
     * @param  source of event
     * @param current    current number of bytes downloaded
     * @param max    maximum number of bytes to download
     * @param downloadObj
     * @param mess message
     */
    public DownloadEvent (Object source, 
                          long   current,
                          long   max,
                          Object downloadObj,
                          String mess) {
      this(source, current, max, 0, 0, "", "", downloadObj, mess);
    }
    /**
     * Create a new DownloadEvent with progress and message information
     * @param  source of event
     * @param current    current number of bytes downloaded
     * @param max    maximum number of bytes to download
     * @param downloadObj
     * @param mess message
     */
    public DownloadEvent (Object source, 
                          long   current,
                          long   max,
                          long   elapseSec,
                          long   remainingSec,
                          String elapseStr,
                          String remainingStr,
                          Object downloadObj,
                          String mess) {
        super(source);
        _current        = current;
        _max            = max;
        _message        = mess;
        _elapseSec      = elapseSec;
        _remainingSec   = remainingSec;
        _elapseStr      = elapseStr;
        _remainingStr   = remainingStr;
        _downloadObj    = downloadObj;
        _type= PROGRESS_ONLY;
    }

    public long    getCurrent()         { return _current;}
    public long    getMax()             { return _max;}

    public long    getElapseSec()       { return _elapseSec; }
    public long    getRemainingSec()    { return _remainingSec; }

    public String getElapseString()    { return _elapseStr; }
    public String getRemainingString() { return _remainingStr; }

    public Object getDownloadObj()     { return _downloadObj;}

    public String getMessage()         { return _message;}
}
