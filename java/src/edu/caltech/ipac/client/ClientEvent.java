package edu.caltech.ipac.client;

import edu.caltech.ipac.util.Assert;

import javax.swing.JFrame;
import java.io.Serializable;
import java.util.EventObject;

/**
 * The event that is passed when ...
 * @author Trey Roby
 */
public class ClientEvent extends EventObject implements Serializable {


    // Predefined event names

    public static final EventName REQUEST_OPEN_FILE=
                                  new EventName("RequestOpenFile",
                                                "Event request some components to open a file, " +
                                                "data should be file that should be opened");

    public static final EventName REQUEST_QUIT=
                                  new EventName("RequestQuit",
                                                "sender is requesting that the application quit, " +
                                                "if data is included it should be the integer exit status");

    public static final EventName REQUEST_ABOUT=
                                  new EventName("RequestAbout",
                                                "sender is requesting that the application show the about dialog, " +
                                                "no data should be included");

    public static final EventName REQUEST_CLOSE_ALL_FRAME=
                                  new EventName("RequestCloseAllFrames",
                                                "sender is requesting that all frames to be closed," +
                                                "if data is included, false means single frame," +
                                                "true means all frames.");

    public static final EventName REQUEST_CLOSE_DOC=
                              new EventName("RequestCloseDoc",
                                            "sender is requesting that document to be closed," +
                                            "no data should be included");

    public static final EventName REQUEST_SAVE_ALL=
                                  new EventName("RequestSaveAll",
                                                "sender is requesting that all file be saved");

    public static final EventName REQUEST_SAVE_ALL_DONT_ASK=
                                  new EventName("RequestSaveAllDontAsk",
                                                "sender is requesting that all file be saved " +
                                                "don't ask the user any questions");

    public static final EventName REQUEST_DISCARD_ALL=
                                  new EventName("RequestSaveAllDontAsk",
                                                "sender is requesting that all files be disgared ");

    public static final EventName REQUEST_DISCARD_ALL_DONT_ASK=
                                  new EventName("RequestSaveAllDontAsk",
                                                "sender is requesting that all files be disgared " +
                                                "don't ask the user any questions");

    public static final EventName QUITTING=
                                  new EventName("Quitting", "this application is quitting");

    public static final EventName RESTARTING=
                                  new EventName("Restarting", "this application is restarting");


    public static final EventName PROCESS_COMPLETED=
                                  new EventName("ProcessCompleted",
                                                "some process has completed," +
                                                "Data is context specific");



    public static final EventName FILE_PROCESSED=
                                  new EventName("FileProcessed",
                                                "A FITS file has been processed, " +
                                                "Data should be a processed file that could be opened");

    public static final EventName FILE_ACCEPTED=
                                  new EventName("FileAccepted",
                                                "A FITS file has been processed and the user has approved it. " +
                                                "Data should be a accepted, processed file that could be opened");

    public static final EventName FILE_MANAGER=
                                  new EventName("FileManager",
                                                "A Notification that a File manager is available, "+
                                                "no data should be included, " +
                                                "typically used for interprocess response");

    public static final EventName REQUEST_FILE_MANAGER=
                                  new EventName("AskForFileManager",
                                                "Some other program is looking for a file manager, "+
                                                "no data should be included, "+
                                                "typically used for interprocess request");

    public static final EventName INPUT_FILE_LOADED=
                                  new EventName("InputFileLoadedSuccessfully",
                                                "A FITS file has been loaded successfully, "+
                                                "Data should be an accepted, processed file that could be opened");

    private final EventName _name;
    private final JFrame _frame;
    private final Object _data;
    private final boolean _sendInterprocess;
    private boolean _fromInterprocess= false;
    private boolean _eventHandled= false;


    /**
     * Create a client event
     * @param source source of the event, may not be null.
     * @param name the name of the event, may not be null
     * @throws IllegalArgumentException  if either source or name is null
     */
    public ClientEvent(Object source, EventName name) {
        this(source,name,null,null);
    }


    /**
     * Create a client event
     * @param source source of the event, may not be null.
     * @param name the name of the event, may not be null
     * @param data data associated with this event (if any)
     * @throws IllegalArgumentException  if either source or name is null
     */
    public ClientEvent(Object source, EventName name, Object data) {
        this(source,name,null,data);
    }

    /**
     * Create a client event.  If interProcess is true then the data should be
     * serializable or a IllegalArumentException is thrown
     * @param source source of the event, may not be null.
     * @param name the name of the event, may not be null
     * @param data data associated with this event (if any)
     * @param sendInterProcess this event should be replicated across other processes
     * @throws IllegalArgumentException  if interProcess is true and the data is not Serializable
     *                                   or if either source or name is null
     */
    public ClientEvent(Object source,
                       EventName name,
                       Object data,
                       boolean sendInterProcess) {
        super(source);
        Assert.argTst(source!=null && name!=null, "You must pass a non-null value " +
                                                  "for both source and name");
        _name= name;
        _frame= null;
        _sendInterprocess= sendInterProcess;

        Assert.argTst( ((data==null) ||
                        (sendInterProcess && (data instanceof Serializable)) ||
                        !sendInterProcess ),
                  "You are disinating this event for interprocess communication but " +
                  "the data is not serializable");

        if (sendInterProcess) {
            _data= (data instanceof Serializable) ? data : null;
        }
        else {
            _data= data;
        }
    }



    /**
     * Create a client event
     * @param source source of the event, may not be null.
     * @param name the name of the event, may not be null
     * @param frame the JFrame (if any) this event is associated with
     * @param data data associated with this event (if any)
     * @throws IllegalArgumentException  if either source or name is null
     */
    public ClientEvent(Object source, EventName name, JFrame frame, Object data) {
        super(source);
        Assert.argTst(source!=null && name!=null, "You must pass a non-null value " +
                                                  "for both source and name");
        _name= name;
        _frame= frame;
        _data= data;
        _sendInterprocess= false;
    }

    /**
     * set that this event has been handled. This is just a hint to let other
     * components know. The event will continued to be sent to all components
     * who can also handle it or ignore it.
     * @param h true if handled, false otherwise
     */
    public void setHandled(boolean h) {
        _eventHandled= h;
    }

    /**
     * Return true if this event has been handled. This is just a hint to let other
     * components know. The event will continued to be sent to all components
     * who can also handle it or ignore it.
     *
     * @return true if handled, false otherwise
     */
    public boolean isHandled() {
        return _eventHandled;
    }

    public EventName getName() { return _name; }
    public JFrame getJFrame() { return _frame; }
    public Object getData() { return _data; }

    public boolean isSendInterprocess() { return _sendInterprocess; }

    public boolean isFromInterprocess() { return _fromInterprocess; }
    public void setFromInterprocess(boolean fromInterprocess) {
        _fromInterprocess= fromInterprocess;
    }

    public String toString() {
        return "ClientEvent- "+ _name +", Source: " + getSource() + ", Data: " + _data;
    }

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
