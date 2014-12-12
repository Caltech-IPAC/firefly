package edu.caltech.ipac.client;


import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Receive files to download.
 */

public class PortWatcher implements Runnable,
                                    ClientEventListener,
                                    PropertyChangeListener {



    private static List<PortWatcher> _portWatcherList=
                                  new ArrayList<PortWatcher>(2);
    private static long        _progID= new Date().getTime();

    private ServerSocket       _listenSocket;
    private int                _port;
    private Thread             _thread;
    private String             _progName;
    private ReceiveDataFactory _receiveDataFactory;
    private boolean            _ready= false;
//     private QuitAction         _qa= null;

    public static final String PROG_EXT= ".prog";

    private static final Object INIT_WAIT_LOCK = new Object();

     public PortWatcher(ReceiveDataFactory receiveDataFactory,
                        String             progName,
                        boolean            startNow) throws IOException {
        _receiveDataFactory= receiveDataFactory;
        _progName          = progName;
        findSocket();
        addProgToAliveDir(); 
        enablePreferenceSharing();
        ClientEventManager.addClientEventListener(this);
        if (startNow) start();
     }


     public PortWatcher(ReceiveDataFactory receiveDataFactory,
                        String             progName) throws IOException {

         this(receiveDataFactory,progName,true);
     }

     public void start() {
        if (_thread == null) {
            _portWatcherList.add(this);
            _thread= new Thread(this, "PortWatcher");
            _thread.start();
        }
     }

//     public void setQuitAction(QuitAction qa) { _qa= qa; }

     public void stop() {
        Thread t= _thread;
        _portWatcherList.remove(this);
        _thread= null;
        t.interrupt();
        removeProgFromAliveDir(); 
     }

    public void waitForReady() {
        if (!_ready) {
            synchronized (INIT_WAIT_LOCK)  {
                try {
                    INIT_WAIT_LOCK.wait();
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }
    }

    public void run() {
        try {
            _ready= true;
            synchronized (INIT_WAIT_LOCK)  { INIT_WAIT_LOCK.notifyAll(); }
            while (_thread != null) {
                Socket clientSocket = _listenSocket.accept();
                if ( validatedAddress(clientSocket.getInetAddress()) ) {
                    _receiveDataFactory.makeReceiveData(clientSocket);
                }
                else {
                    clientSocket.getInputStream().close();
                }
            }
        } catch (ClosedByInterruptException cd) {
            ClientLog.message(true, "Normal stop");
        } catch (IOException e) {
            ClientLog.warning(true, "Failed: " + e);
        }
    }


    public int getPort() { return _port; }
    public String getProgName() { return _progName; }


    public static void confirmShowingAlive() {
         for(PortWatcher watcher: _portWatcherList) {
            watcher.confirmProgInAliveDir();
         }
    }

    public static Iterator<PortWatcher> getPortWatcherListIterator() {
         return _portWatcherList.iterator();
    }

    public static PortWatcher getPortWatcherByProgName(String name) {
        PortWatcher retval= null;
        for(PortWatcher watcher: _portWatcherList) {
            if (watcher._progName.equals(name)) {
                retval= watcher;
            }
        }
        return retval;
    }
    
    public static int getPortWatcherCnt() { return _portWatcherList.size();  }


//    public static int[] getAllPorts() {
//        int retval[]= new int[_portWatcherList.size()];
//        for(int i= 0; (i<retval.length); i++) {
//            retval[i]= _portWatcherList.get(i)._port;
//        }
//        return retval;
//    }

//=====================================================================
//------------------- Public Static -----------------------------------
//=====================================================================

     public static long getProgramID() { return _progID; } 

//=====================================================================
//------------------- Methods from ClientEventListener
//=====================================================================

    public void eventNotify(ClientEvent ev) {
        if (ev.getName().equals(ClientEvent.QUITTING)) {
            Assert.tst(ev.getName()==ClientEvent.QUITTING);
            stop();
        }
        if (ev.isSendInterprocess()) {
            DataPackage dp= DataPackage.makeInterprocessEvent(ev);
            AppInterface.sendMessageToAll(dp);
        }
    }


//=====================================================================
//------------------- Method from PropertyChangeListener Interface ----
//=====================================================================

     public void propertyChange(PropertyChangeEvent ev) {
         Assert.tst(ev.getSource()==AppProperties.class);
         if (!ReceiveData.isDoingPropertyUpdate())  {
             AppInterface.sendPropChangeToAll(ev, _port, _progName);
         }
     }

//=====================================================================
//------------------- Private / Protected Methods ---------------------
//=====================================================================

     private int startPort() {
        return 4331;
     }


     private boolean validatedAddress(InetAddress ip) {
        boolean retval;
        retval= ip.getHostAddress().equals("127.0.0.1");
        //System.out.println("ip.getHostAddress()= " + ip.getHostAddress());
        return retval;
     }


     private void findSocket() throws IOException {
       boolean found= false;
       int i;
       for(i=0; (i<1000 && !found); i++) {
           _port= startPort() + i;
           try {
               _listenSocket = new ServerSocket(_port);
               found= true;
           } catch (BindException e) {
               // that did not work, keep trying
           }
       }
       if (i==1000) throw new IOException("Could not find a port to bind to");
    }

    private void confirmProgInAliveDir() {
        File f= makeFileName();
        if (!(f.exists() && f.canRead())) {
              try {
                 addProgToAliveDir();
              }
              catch (IOException e) {
                 ClientLog.warning(true, "could not create " + f.getPath(),
                                         e.toString() );
              }
        }
    }


    private void addProgToAliveDir() throws IOException {
        AppInterface.deleteAllWithPort(_port);
        FileWriter fw= new FileWriter(makeFileName());
        fw.write(_port+ " " + _progID);
        fw.close();
    }


    private void removeProgFromAliveDir() {
        File f= makeFileName();
        if (f.exists()) {
            if (!f.canWrite()) {
                ClientLog.warning("File not writable but should be: " + f);
            }
            boolean success= f.delete();
            if (!success) {
                ClientLog.warning("failed to remove: " + f);
            }
        }
        else {
            ClientLog.warning("File does not exist: " + f,
              "Posibly deleted by another program because of unresponsivness");
        }


    }

    private File makeFileName() {
        File dir= Platform.getInstance().getAliveDirectory();
        return new File(dir, _progName + "-"+ _port + PROG_EXT);
    }

   
    private void enablePreferenceSharing() {
        AppProperties.addPropertyChangeListener(this);
    }

    public static void main(String args[]) {
        if (args.length==3) {
            PortWatcher portWatcher= null;
            Platform.getInstance().setDirectoryBase(args[0]);
            try {
                portWatcher  = new PortWatcher(
                               new DefaultReceiveDataFactory(), "tester", true);
            } catch (IOException e) {
                System.out.println(e);
            }
            try {Thread.sleep(3000); } catch (InterruptedException e) {}
            AppProperties.setPreference(args[1], args[2]);
            System.out.println("done.");
            try {Thread.sleep(1000); } catch (InterruptedException e) {}
            portWatcher.stop();
            System.exit(0);
        }
        else {
            System.out.println(
                   "usage: PortWatcher directoryBase property propertyValue");
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
