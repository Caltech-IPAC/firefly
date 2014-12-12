package edu.caltech.ipac.client;

import edu.caltech.ipac.util.Assert;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.Serializable;


public class DataPackage implements Serializable {


    public static final int NO_ANSWER      = 600;
    public static final int ALIVE_QUESTION = 677;
    public static final int ALIVE_CONFIRM  = 678;
    public static final int QUIT           = 680;
    public static final int QUIT_IF_IDLE   = 681;
    public static final int QUIT_CONFIRM   = 682;
    public static final int QUIT_REJECTED  = 683;
    public static final int DATA           = 684;
    public static final int DATA_CONFIRM   = 685;
    public static final int PROPERTY_CHANGE= 686;
    public static final int PLOT_FILE      = 687;
//    public static final int PROCESSED_FILE = 688;
//    public static final int ACCEPTED_FILE  = 689;
    public static final int INTERPROCESS_EVENT = 670;
//    public static final int ASK_FOR_FILE_MANAGER  = 671;

    public static final String NO_ANSWER_STR        = "NO_ANSWER";
    public static final String ALIVE_QUESTION_STR   = "ALIVE_QUESTION";
    public static final String ALIVE_CONFIRM_STR    = "ALIVE_CONFIRM";
    public static final String QUIT_STR             = "QUIT";
    public static final String QUIT_IF_IDLE_STR     = "QUIT_IF_IDLE";
    public static final String QUIT_CONFIRM_STR     = "QUIT_CONFIRM";
    public static final String QUIT_REJECTED_STR    = "QUIT_REJECTED";
    public static final String DATA_STR             = "DATA";
    public static final String DATA_CONFIRM_STR     = "DATA_CONFIRM";
    public static final String PROPERTY_CHANGE_STR  = "PROPERTY_CHANGE";
    public static final String PLOT_FILE_STR        = "PLOT_FILE";
//    public static final String PROCESSED_FILE_STR   = "PROCESSED_FILE";
//    public static final String ACCEPTED_FILE_STR    = "ACCEPTED_FILE";
    public static final String INTERPROCESS_EVENT_STR = "INTERPROCESS_EVENT";
//    public static final String ASK_FOR_FILE_MANAGER_STR = "ASK_FOR_FILE_MANAGER";

    private int    _packageType;
    private Object _data;
    private Sender _sender;

    public DataPackage(int packageType) {
       this(packageType,null,null);
    }

    public DataPackage(int packageType, String sender) {
       this(packageType,sender,null);
    }

    public DataPackage(int packageType, String sender, Object data) {
       _packageType= packageType;
       _sender     = new Sender(sender);
       _data       = data;
    }


    public static DataPackage makeAliveQuestion(String sender) {
       return new DataPackage(ALIVE_QUESTION,sender);
    }

    public static DataPackage makeQuitRequest(String sender) {
       return new DataPackage(QUIT,sender);
    }
    public static DataPackage makeQuitIfIdleRequest(String sender) {
       return new DataPackage(QUIT_IF_IDLE,sender);
    }

    public static DataPackage makePlotFileRequest(String sender, File f) {
        return new DataPackage(PLOT_FILE,sender,f);
    }


//    public static DataPackage makeProcessedFileRequest(File f) {
//        ApplicationVersion av= ApplicationVersion.getInstalledApplicationVersion();
//        return makeProcessedFileRequest(av.getAppName(),f);
//    }

//    public static DataPackage makeProcessedFileRequest(String sender, File f) {
//        return new DataPackage(PROCESSED_FILE,sender,f);
//    }


//    public static DataPackage makeAcceptedFileRequest(File f) {
//        ApplicationVersion av= ApplicationVersion.getInstalledApplicationVersion();
//        return makeAcceptedFileRequest(av.getAppName(),f);
//    }

//    public static DataPackage makeAcceptedFileRequest(String sender, File f) {
//        return new DataPackage(ACCEPTED_FILE,sender,f);
//    }

    public static DataPackage makeInterprocessEvent(String sender, ClientEvent ev) {
        return new DataPackage(INTERPROCESS_EVENT,sender,ev);
    }

    public static DataPackage makeInterprocessEvent(ClientEvent ev) {
        ApplicationVersion av= ApplicationVersion.getInstalledApplicationVersion();
        return makeInterprocessEvent(av.getAppName(),ev);
    }


    public static DataPackage makePropertyChange(String              sender,
                                                 PropertyChangeEvent ev ) {
       return new DataPackage(PROPERTY_CHANGE,sender, ev);
    }

    public int    getType()   { return _packageType; }
    public Object getData()   { return _data; }
    public Sender getSender() { return _sender; }


    private String typeToStr(int type) {
       String retval= null;
       switch (type) {
           case  NO_ANSWER            : retval= NO_ANSWER_STR;       break;
           case  ALIVE_QUESTION       : retval= ALIVE_QUESTION_STR;  break;
           case  ALIVE_CONFIRM        : retval= ALIVE_CONFIRM_STR;   break;
           case  QUIT                 : retval= QUIT_STR;            break;
           case  QUIT_IF_IDLE         : retval= QUIT_IF_IDLE_STR;    break;
           case  QUIT_CONFIRM         : retval= QUIT_CONFIRM_STR;    break;
           case  QUIT_REJECTED        : retval= QUIT_REJECTED_STR;   break;
           case  DATA                 : retval= DATA_STR;            break;
           case  DATA_CONFIRM         : retval= DATA_CONFIRM_STR;    break;
           case  PROPERTY_CHANGE      : retval= PROPERTY_CHANGE_STR; break;
           case  PLOT_FILE            : retval= PLOT_FILE_STR; break;
           case  INTERPROCESS_EVENT   : retval= INTERPROCESS_EVENT_STR; break;
//           case  PROCESSED_FILE       : retval= PROCESSED_FILE_STR; break;
//           case  ACCEPTED_FILE        : retval= ACCEPTED_FILE_STR; break;
//           case  FILE_MANAGER         : retval= FILE_MANAGER_STR; break;
//           case  ASK_FOR_FILE_MANAGER : retval= ASK_FOR_FILE_MANAGER_STR; break;
           default : Assert.tst(false); break;
       }
       return retval;
    }


    public String toString() {
         String dataStr;
         if (_data instanceof PropertyChangeEvent) {
            PropertyChangeEvent ev= (PropertyChangeEvent)_data;
            String oldValue= (ev.getOldValue())==null ? null :
                                          ev.getOldValue().toString();
            String newValue= (ev.getNewValue())==null ? null :
                                          ev.getNewValue().toString();
            dataStr= "Property Name: " + ev.getPropertyName()    + "\n" +
                     "old value:     " + oldValue                + "\n" +
                     "new value:     " + newValue;

         } 
         else {
           dataStr= "data:   " + _data;
         } 
         return "Type: " + typeToStr(_packageType)        +  "\n" +
                "sender name: " + _sender.getSenderName() +  "\n" +
                "sender id: "   + _sender.getSenderID()   +  "\n" +
                dataStr;
    }

 //=====================================================================
 //---------------- Public Inner classes -------------------------------
 //=====================================================================

    public static class Sender implements Serializable {
       private String _senderName;
       private long   _senderID;
       public Sender(String senderName) {
          _senderName= senderName;
          _senderID  = PortWatcher.getProgramID();
       }

       public String getSenderName() { return _senderName; }
       public long   getSenderID()   { return _senderID; }
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
