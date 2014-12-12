package edu.caltech.ipac.client;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;

import javax.swing.SwingUtilities;
import java.io.File;


public class InterfaceToMac implements ApplicationListener {

    private Application  _app;

   public InterfaceToMac() {
       _app= new Application();
       _app.addApplicationListener(this);
   }

   public void handleAbout(ApplicationEvent ev) {
       ClientEvent cevent= new ClientEvent(this,ClientEvent.REQUEST_ABOUT);
       ClientEventManager.fireClientEvent(cevent);
       ev.setHandled(true);
   }

   public void handleQuit(ApplicationEvent ev) {
       SwingUtilities.invokeLater(new Runnable() {
           public void run() {
               ClientEvent cevent= new ClientEvent(this,ClientEvent.REQUEST_QUIT,0);
               ClientEventManager.fireClientEvent(cevent);
           }
       });
       ev.setHandled(false);
       throw new IllegalStateException(
                                     "Stop Pending User Confirmation - this exception is Normal "+
                                     "on the Mac everytime the quit menu is activated (OSX bug)");
   }

    public void handleOpenApplication(ApplicationEvent ev) {
        fireOpenFile(ev);
    }
    
    public void handleOpenFile(ApplicationEvent ev) {
        fireOpenFile(ev);
    }

    public void handlePreferences(ApplicationEvent e) { }
    public void handlePrintFile(ApplicationEvent ev) { }

    public void handleReOpenApplication(ApplicationEvent ev) {
        fireOpenFile(ev);
    }


    private void fireOpenFile(ApplicationEvent appEv) {
        File f= (appEv.getFilename()!=null) ? new File(appEv.getFilename()) : null;
        if (f!=null) {
            ClientEvent ev= new ClientEvent(this,ClientEvent.REQUEST_OPEN_FILE,null,f);
            ClientEventManager.fireClientEvent(ev);
        }
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
