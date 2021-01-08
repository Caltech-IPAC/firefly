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

    private final long    _max;
    private final long    _current;
    private final String _message;
    private final long   _elapseSec;
    private final long   _remainingSec;
    private final String _elapseStr;
    private final String _remainingStr;

    /**
     * Create a new DownloadEvent with progress and message information
     * @param  source of event
     * @param current    current number of bytes downloaded
     * @param max    maximum number of bytes to download
     * @param mess message
     */
    public DownloadEvent (Object source,
                          long   current,
                          long   max,
                          long   elapseSec,
                          long   remainingSec,
                          String elapseStr,
                          String remainingStr,
                          String mess) {
        super(source);
        _current        = current;
        _max            = max;
        _message        = mess;
        _elapseSec      = elapseSec;
        _remainingSec   = remainingSec;
        _elapseStr      = elapseStr;
        _remainingStr   = remainingStr;
    }

    public long    getCurrent()         { return _current;}
    public long    getMax()             { return _max;}
    public long    getElapseSec()       { return _elapseSec; }
    public long    getRemainingSec()    { return _remainingSec; }
    public String getElapseString()    { return _elapseStr; }
    public String getRemainingString() { return _remainingStr; }
    public String getMessage()         { return _message;}
}
