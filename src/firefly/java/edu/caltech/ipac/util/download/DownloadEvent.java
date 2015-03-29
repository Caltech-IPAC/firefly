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

    private long    _max    = 0;
    private long    _current= 0;
    private String _message= "";
    private long   _elapseSec;
    private long   _remainingSec;
    private String _elapseStr;
    private String _remainingStr;
    private Object _downloadObj;

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
