package edu.caltech.ipac.client;


import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;

import java.beans.PropertyChangeEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Receive files to download.
 */

public class ReceiveData implements Runnable,
                                    ClientEventListener {

     private Thread     _thread;
     private Socket     _socket;
     private boolean    _quitStarted= false;
     private static boolean  _updatingProperties= false;

     public ReceiveData(Socket socket) {
         _socket= socket;
         if (_thread == null) {
             _thread= new Thread(this, "ReceiveData");
             _thread.start();
         }
     }

//     public QuitAction getQuitAction() { return _qa; }


     public void quitWithStatus(int stat) {
         ClientEventManager.fireClientEvent(
                    new ClientEvent(this,ClientEvent.REQUEST_QUIT,stat));
     }

     public void run() {
        ObjectInputStream objIS= null;
        try {
           _socket.setSoTimeout(4000);
           objIS = new ObjectInputStream(
                      new BufferedInputStream(_socket.getInputStream()) );
           DataPackage packet= 
                   (DataPackage)objIS.readObject();  // read the object

           switch (packet.getType()) {

               case DataPackage.ALIVE_QUESTION : respondAlive(
                                                 packet.getSender());
                                                 break;

               case DataPackage.QUIT           : processQuit(
                                                 packet.getSender());
                                                 break;

               case DataPackage.QUIT_IF_IDLE   : processQuitIfIdle();
                                                 break;

               case DataPackage.PROPERTY_CHANGE: updatePreferences(
                                                 (PropertyChangeEvent)packet.getData());
                                                  break;

               case DataPackage.PLOT_FILE      : confirmData(packet);
                                                 fireFileEvent((File)packet.getData(),
                                                               ClientEvent.REQUEST_OPEN_FILE);
                                                  break;

//               case DataPackage.PROCESSED_FILE : confirmData(packet);
//                                                 fireFileEvent((File)packet.getData(),
//                                                                ClientEvent.FILE_PROCESSED);
//                                                  break;
//
//               case DataPackage.ACCEPTED_FILE : confirmData(packet);
//                                                fireFileEvent((File)packet.getData(),
//                                                              ClientEvent.FILE_ACCEPTED);
//                                                break;

               case DataPackage.DATA           : confirmData(packet);
                                                 processData(packet.getData());
                                                  break;
               case DataPackage.INTERPROCESS_EVENT  : confirmData(packet);
                                                      replicateEvent((ClientEvent)packet.getData());
                                                      break;
               default                         : Assert.tst(false);
                                                 break;
           }
          
        } catch (IOException e) {
            ClientLog.warning("Failed: " + e);
        } catch (ClassNotFoundException e) {
            ClientLog.warning("Failed: " + e);
        } finally {
            FileUtil.silentClose(objIS);
            FileUtil.silentClose(_socket);
        }
     }



     protected void confirmData(DataPackage packet) {
         ObjectOutputStream objOS =null;
         try {
             objOS=new ObjectOutputStream(
                           new BufferedOutputStream(_socket.getOutputStream()) );
             objOS.writeObject( new DataPackage(DataPackage.DATA_CONFIRM) );
             objOS.flush();
         } catch (IOException e) {
             ClientLog.warning("Failed to confirm DataPackage:",
                               packet.toString(),
                               "Exception: " + e);
         } finally {
             FileUtil.silentClose(objOS);
         }
     }


     protected void respondAlive(DataPackage.Sender requester) 
                                                       throws IOException {
        ObjectOutputStream objOS = new ObjectOutputStream(
                      new BufferedOutputStream(_socket.getOutputStream()) );
        objOS.writeObject( new DataPackage(DataPackage.ALIVE_CONFIRM) );
        objOS.flush();
        objOS.close();
        ClientLog.message("Alive response to: " +  requester.getSenderName() +
                          "   id: "             +  requester.getSenderID() );
     }

     protected void processQuit(DataPackage.Sender requester) 
                                                       throws IOException {
           ClientLog.message("Quit started by: " +  requester.getSenderName() );
           _quitStarted= true;
           ClientEventManager.addWeakClientEventListener(this,ClientEvent.QUITTING);
           quitWithStatus(0);
           quitResponse(DataPackage.QUIT_REJECTED); // if quit sucessful this line will never execute
     }

     private void processQuitIfIdle() throws IOException {
        quitIfIdle();
        try {
            quitResponse(DataPackage.QUIT_CONFIRM);
        } catch (IOException e) {
            ClientLog.warning("processQuitIfIdle: confirm failed: " + e);
        }
     }


     private void quitResponse(int response) throws IOException {
        Assert.tst(response==DataPackage.QUIT_CONFIRM  ||
                   response==DataPackage.QUIT_REJECTED);
        String rStr;
        if (response==DataPackage.QUIT_CONFIRM) {
            rStr= "confirmed";
        }
        else {
            rStr= "rejected";
        }
        ObjectOutputStream objOS = new ObjectOutputStream(
                      new BufferedOutputStream(_socket.getOutputStream()) );
        objOS.writeObject( new DataPackage(response) );
        objOS.flush();
        objOS.close();
        ClientLog.message("quit response: " +  rStr);
     }


    protected void fireFileEvent(File f, EventName name) {
        ClientEvent ev= new ClientEvent(this,name,null,f);
        ClientEventManager.fireClientEvent(ev);
    }

    protected void replicateEvent(ClientEvent ev) {
        ClientEvent newEv= new ClientEvent(this,ev.getName(),ev.getData());
        newEv.setFromInterprocess(true);
        ClientEventManager.fireClientEvent(newEv);
    }



//     public void quit(QuitEvent ev) {
//     }


    public void eventNotify(ClientEvent ev) {
        Assert.tst(ev.getName()==ClientEvent.QUITTING);
        try {
            if (_quitStarted) quitResponse(DataPackage.QUIT_CONFIRM);
        } catch (IOException e) {
            ClientLog.warning("quit: confirm failed: " + e);
        }
    }

    public void updatePreferences(PropertyChangeEvent ev) {
         _updatingProperties= true;
         AppProperties.setPreferenceWithNoPersistence(
                        ev.getPropertyName(), ev.getNewValue().toString() );
         _updatingProperties= false;
     }

     static boolean isDoingPropertyUpdate() { return _updatingProperties; }

     protected void processData(Object data) {}
     protected void quitIfIdle() { }

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
