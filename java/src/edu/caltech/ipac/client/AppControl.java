package edu.caltech.ipac.client;

import edu.caltech.ipac.util.Installer;

import java.io.File;
import java.io.IOException;
/**
 * User: roby
 * Date: Jun 5, 2008
 * Time: 1:39:36 PM
 */


/**
 * @author Trey Roby
 */
public class AppControl {

    public static final String APP_CONTROL= "AppControl";
    private static PortWatcher _portWatcher;
    private boolean _fileManager= true;
    private FileManagerListener _fmListener= new FileManagerListener();

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public AppControl (String dirBase) throws IOException {
        if (_portWatcher==null) {
            Platform.setGUI(false);
            Platform.getInstance().setDirectoryBase(dirBase);
            _portWatcher  = new PortWatcher(
                                          new DefaultReceiveDataFactory(), APP_CONTROL, true);
            _portWatcher.waitForReady();
        }
        ClientEventManager.addWeakClientEventListener(_fmListener,ClientEvent.REQUEST_FILE_MANAGER);

    }


    public void freeResources() {
        _portWatcher.stop();
        _portWatcher= null;
    }

    public void sendFile(String appName, File f) throws IOException {
            int port= AppInterface.getPortForProg(appName, APP_CONTROL);
            DataPackage dp= DataPackage.makePlotFileRequest(APP_CONTROL, f);
            AppInterface.sendMessageToProg(port,dp);
    }

    public Process startApplication(String appName,
                                    String params[]) throws IOException {
        Platform platform= Platform.getInstance();
        File dir= platform.findAppInstallationDir(appName);
        if (dir==null || !dir.exists()) {
            throw new IOException("The application "+appName+
                                  " has never been run with the in directory of " +
                                platform.getWorkingDirectory().getPath()+
                                ". You probably set the dirBase wrong.");
        }
        return Installer.launchApp(dir,appName,params);
    }


    public void loadFile(String appName, File f) {

        try {
            if (AppInterface.isAlive(appName, APP_CONTROL)) {
                sendFile(appName, f);
            }
            else {
                startApplication(appName, new String[] {f.getPath()});
            }
        } catch (IOException e) {
            System.out.println(e);
        }

    }



//=======================================================================
//-------------- Private inner classes ----------------------------------
//=======================================================================

    private class FileManagerListener implements ClientEventListener {
        public void eventNotify(ClientEvent ev) {
            if (_fileManager) {
                ClientEvent newEv= new ClientEvent(this,ClientEvent.FILE_MANAGER,null,true);
                ClientEventManager.fireClientEvent(newEv);
            }
        }
    }




//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    public static void main(String args[]) {
        try {
            AppControl appc= new AppControl(args[1]);
//            appc.loadFile(args[0], new File("/Users/roby/fits/dss-m31.fits"));
            appc.loadFile(args[0], new File(args[2]));
        } catch (IOException e) {
            e.printStackTrace();
        }
        ClientEventManager.addClientEventListener(new ClientEventListener() {
            public void eventNotify(ClientEvent ev) {
                System.out.println("got processed event - file:"+ ((File)ev.getData()).getPath());
            }
        }, ClientEvent.FILE_PROCESSED);

        ClientEventManager.addClientEventListener(new ClientEventListener() {
            public void eventNotify(ClientEvent ev) {
                System.out.println("got accepted event - file:"+ ((File)ev.getData()).getPath());
            }
        }, ClientEvent.FILE_ACCEPTED);
//        System.exit(0);
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
