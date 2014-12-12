package edu.caltech.ipac.client;


import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;

import java.beans.PropertyChangeEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.InetAddress;


public class AppInterface {


    public static final String SPOT      = "spot";
    public static final String LEOPARD   = "leopard";
    public static final String SUBSCRIBER= "subscriber";


    public static boolean isAlive(int port, String sender) {
       boolean            retval = false;
       ObjectOutputStream objOS  = null;
       Socket             socket = null;
       DataPackage        answerDP;
       try {
           DataPackage dp     = DataPackage.makeAliveQuestion(sender);
           socket  = makeSocket(port);
           objOS   = send(socket,dp);
           answerDP= getAnswer(socket);
           objOS.close();
           socket.close();
           if (answerDP.getType() == DataPackage.ALIVE_CONFIRM) {
             retval= true;
           }
       } catch (IOException e) {
           try { if (objOS!= null) objOS.close(); }   catch (IOException e3){}
           try { if (socket!= null) socket.close(); } catch (IOException e4){}
           retval= false;
       } catch (ClassNotFoundException cnfe) {
           retval= false;
       }
       ClientLog.message(true,"port: " + port +
                          "   response: " + retval);
       return retval;
    }

    public static int sendQuitToProg(int port, DataPackage dp) 
                                                     throws IOException {
       boolean            retval= false;
       ObjectOutputStream objOS = null;
       Socket             socket= null;
       DataPackage        answerDP;
       int                answer= DataPackage.NO_ANSWER;
       try {
           socket  = makeSocket(port);
           objOS   = send(socket,dp);
           answerDP= getAnswer(socket);
           objOS.close();
           socket.close();
           answer= answerDP.getType();
           if (answer != DataPackage.QUIT_CONFIRM &&
               answer != DataPackage.QUIT_REJECTED) {
              throw new IOException("No confirmation on data");
           }
       } catch (IOException e) {
           try { if (objOS!= null) objOS.close(); }   catch (IOException e3){}
           try { if (socket!= null) socket.close(); } catch (IOException e4){}
           throw e;
       } catch (ClassNotFoundException cnfe) {
           retval= false;
           Assert.tst(false, "This should never happen");
       }
       ClientLog.message(true,"port: " + port + "   Data send successfully");
       return answer;
    }

    public static void sendMessageToProg(int port, DataPackage dp) 
                                                     throws IOException {
       sendMessageToProg(port,dp,true);
    }

    public static void sendMessageToProg(int         port, 
                                         DataPackage dp,
                                         boolean     requireAnswer) 
                                                     throws IOException {
       boolean            retval= false;
       ObjectOutputStream objOS = null;
       Socket             socket= null;
       DataPackage        answerDP;
       try {
           socket  = makeSocket(port);
           objOS   = send(socket,dp);
           if (requireAnswer) {
               answerDP= getAnswer(socket);
               if (answerDP.getType()!=DataPackage.DATA_CONFIRM) {
                        throw new IOException("No confirmation on data");
               }
           }
       } catch (IOException e) {
           throw e;
       } catch (ClassNotFoundException cnfe) {
           retval= false;
           Assert.tst(false, "This should never happen");
       } finally {
           try { if (objOS!= null) objOS.close(); }   catch (IOException e3){}
           try { if (socket!= null) socket.close(); } catch (IOException e4){}
       }
       ClientLog.message(true,"port: " + port + "   Data send successfully",
                              dp.toString());
    }




    public static int getAliveCount(String sender) {
        int    i;
        File   dir       = Platform.getInstance().getAliveDirectory();
        String fileList[]= dir.list( new FileEndFilter());

        for (i= 0; (i<fileList.length); i++) {
              portByFile(new File(dir,fileList[i]), sender);
        }
        return i;
    }

    public static boolean isAlive(String progName, String sender) {
        File   dir = Platform.getInstance().getAliveDirectory();
        File   f   = null;
        int    port= -1;
        ProgFileContents contents;
        String fileList[]= dir.list( new FileStartFilter(progName) );
        for (int i= 0; (i<fileList.length && (port<1) ); i++) {
              f= new File(dir,fileList[i]);
              contents= portByFile(f, sender);
              if (contents!=null) port= contents.getPort();
        }
        return (port > 0);
    }

    public static int getPortForProg(String progName, String sender) {
        File   dir = Platform.getInstance().getAliveDirectory();
        File   f;
        int    port= -1;
        ProgFileContents contents;
        String fileList[]= dir.list( new FileStartFilter(progName) );
        for (int i= 0; (i<fileList.length && (port<1)); i++) {
              f= new File(dir,fileList[i]);
              contents= portByFile(f, sender);
              if (contents!=null) port= contents.getPort();
        }
        return port;
    }

 
    public static boolean quitNow(String progName, String sender) {
        boolean retval= true;
        int port= getPortForProg(progName, sender);
        if (port>-1) {
           try {
               DataPackage qPackage= DataPackage.makeQuitRequest(progName);
               int answer= sendQuitToProg(port, qPackage);
               retval= (answer==DataPackage.QUIT_CONFIRM);
                
           } catch (IOException e) {
               retval= false;
           }
        }
        return retval;
    }


    public static void quitIfIdle(String progName, String sender) {
        int port= getPortForProg(progName, sender);
        if (port>-1) {
           try {
               DataPackage qPackage= 
                                 DataPackage.makeQuitIfIdleRequest(progName);
               sendQuitToProg(port, qPackage);
                
           } catch (IOException e) { }
        }
    }

    public static void sendPropChangeToAll(PropertyChangeEvent ev, 
                                           int                 myPort,
                                           String              sender) {
        ProgFileContents contents;
        File        f         = null;
        File        dir       = Platform.getInstance().getAliveDirectory();
        DataPackage dp        = DataPackage.makePropertyChange(sender, ev);
        String      fileList[]= dir.list( new FileEndFilter() );
        int         port;
        for (int i= 0; (i<fileList.length); i++) {
            f= new File(dir,fileList[i]);
            contents= portByFile(f,sender,myPort);
            if (contents!=null) {
                port= contents.getPort();
                if (port!=myPort) {
                   try {
                      sendMessageToProg(port,dp,false);
                   } catch (IOException e) {
                      ClientLog.message(true, 
                          "failed (ignoring) to send to:" +
                           FileUtil.getBase(f.getName()) +
                           " port: " + port, e.toString());
                   }
                } // end if
            } // end if
        } // end loop
    }



    public static void sendMessageToAll(DataPackage dp) {
        ProgFileContents contents;
        File        f;
        File        dir       = Platform.getInstance().getAliveDirectory();
        String      fileList[]= dir.list( new FileEndFilter() );
        PortWatcher pw= PortWatcher.getPortWatcherByProgName(
                                      dp.getSender().getSenderName());
        if (pw==null) {
            pw= PortWatcher.getPortWatcherListIterator().next();
        }
        if (pw!=null) {
            int         port;
            int         myPort= pw.getPort();
            for (String fileStr : fileList) {
                f= new File(dir,fileStr);
                contents= portByFile(f,pw.getProgName(),myPort);
                if (contents!=null) {
                    port= contents.getPort();
                    if (port!=myPort) {
                        try {
                            sendMessageToProg(port,dp,false);
                        } catch (IOException e) {
                            ClientLog.message(true,
                                              "failed (ignoring) to send to:" +
                                              FileUtil.getBase(f.getName()) +
                                              " port: " + port, e.toString());
                        }
                    } // end if
                } // end if
            } // end loop
        }
        else {
            ClientLog.warning(true,"Could not send the DataPackage.",
                              "This program has not started a PortWatcher");
        }

    }



//=====================================================================
//------------------- Private / Protected Methods ---------------------
//=====================================================================

    static void deleteAllWithPort(int port) {
        ProgFileContents contents;
        File   dir= Platform.getInstance().getAliveDirectory();
        String fileList[]= dir.list( new FileEndFilter());
        File   f;
        for (int i= 0; (i<fileList.length); i++) {
              f= new File(dir,fileList[i]);
              contents= parseFile(f);
              if (contents!=null && contents.getPort()==port) f.delete(); 
        }

    }

    private static ProgFileContents portByFile(File f, String sender) {
        return portByFile(f,sender,-1);
    }

/*
    private static ProgFileContents portByFile(File    f, 
                                               String  sender, 
                                               boolean checkAlive,
                                               int     myPort) {
       LineNumberReader lnr = null;
       int              port= -1;
       long             id  =  0;
       ProgFileContents retval;
       try {
          lnr= new LineNumberReader(new FileReader(f));
          String inStr= lnr.readLine();
          if (inStr==null) 
                     throw new IOException("Nothing in file" + f.getName() );
          String inStrAry[]= inStr.split(" ");
          if (inStrAry.length!=2) 
                    throw new IOException(
                        "file contents invalid, wrong number of fields");
          try {
             port= Integer.parseInt(inStrAry[0]);
             id  = Long.parseLong(  inStrAry[1]);
             if (myPort>-1 && port==myPort) { // I know I am alive
                checkAlive= false;
             }
             if (checkAlive && !isAlive(port, sender)) {
                  port= -1;
                  f.delete();
             }
          } catch (NumberFormatException nfe) {
             throw new IOException(
                         "file contents invalid, could not parse contents");
          }
       } catch (IOException e) {
          port= -1;
          if (f!=null) f.delete();
       } finally {
          try { if (lnr!=null) lnr.close(); } catch (IOException e2) {}
       }
       return new ProgFileContents(port,id);
    }
*/

    private static ProgFileContents portByFile(File    f, 
                                               String  sender, 
                                               int     myPort) {
      ProgFileContents retval= parseFile(f);
      if (retval!=null) {
           int port= retval.getPort();
           if (port!=myPort) { // I know I am alive
              if (!isAlive(port, sender)) {
                    f.delete();
              }
           }
      }
      else {
           f.delete();
      }
      return retval;
    }

     /**
      * Look for a file with one line.  The line of the file there should
      * contain two numbers, the port number and the program ID.
      */
    private static ProgFileContents parseFile(File f) {
       LineNumberReader lnr    = null;
       ProgFileContents retval = null;
       try {
         lnr= new LineNumberReader(new FileReader(f));
         String inStr= lnr.readLine();
         if (inStr==null) 
                    throw new IOException("Nothing in file" + f.getName() );
         String inStrAry[]= inStr.split(" ");
         if (inStrAry.length!=2) 
                   throw new IOException(
                       "file contents invalid, wrong number of fields");
         try {
            retval= new ProgFileContents( Integer.parseInt(inStrAry[0]),
                                          Long.parseLong(  inStrAry[1])   );
         } catch (NumberFormatException nfe) {
            throw new IOException(
                        "file contents invalid, could not parse contents");
         }
       } catch (IOException e) {
             // do nothing 
       } finally {
         try { if (lnr!=null) lnr.close(); } catch (IOException e2) {}
       }
       return retval;
    }


    private static ObjectOutputStream  send(Socket socket, DataPackage dp) 
                                                  throws IOException {
        PortWatcher.confirmShowingAlive();
        ObjectOutputStream   objOS  = new ObjectOutputStream(
                       new BufferedOutputStream(socket.getOutputStream())  );
        objOS.writeObject(dp);
        objOS.flush();
        return objOS;
     }

    private static DataPackage getAnswer(Socket socket) 
                                            throws IOException,
                                                   ClassNotFoundException  {
        socket.setSoTimeout(4000);
        ObjectInputStream   objIS  = new ObjectInputStream(
                       new BufferedInputStream(socket.getInputStream())  );
        DataPackage packet= 
                   (DataPackage)objIS.readObject();  // read the object
        objIS.close();
        return packet;
     }


    private static Socket makeSocket(int port) throws IOException {
       byte localhost[]= {127,0,0,1};
       InetAddress addr= InetAddress.getByAddress(localhost);
       return new Socket(addr, port);
        //return new Socket("localhost", port);
    }



   
    private static class FileStartFilter implements FilenameFilter {
        private String _start;

        public FileStartFilter(String start) {
          _start= start;
        }
        public boolean accept( File dir, String name ) {
            return (name.endsWith(PortWatcher.PROG_EXT) &&
                    name.startsWith(_start + "-"));
        }
    }

    private static class FileEndFilter implements FilenameFilter {

        public FileEndFilter() {
        }
        public boolean accept( File dir, String name ) {
                 return (name.endsWith(PortWatcher.PROG_EXT) &&
                          name.indexOf('-') > 1);
        }
    }


    private static class ProgFileContents {
        private int  _port;
        private long _id;         // not yet implemented
        ProgFileContents(int port, long id) {
          _port= port;
          _id  = id;
        }

        int  getPort()  { return _port; }
        long getID()    { return _id; }
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
