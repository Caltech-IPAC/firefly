package edu.caltech.ipac.client.net;

import edu.caltech.ipac.client.ClientLog;
import edu.caltech.ipac.client.Platform;
import edu.caltech.ipac.util.ExecStatusConst;
import edu.caltech.ipac.util.ExecStatus;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.action.ClassProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Vector;

   
/**
 * all the SOAS that the middle ware provides
 * @author Trey Roby
 * @version $Id: SoasInterface.java,v 1.4 2007/11/01 21:29:19 xiuqin Exp $
 */
public class SoasInterface implements Ping {

   private final static int BUFF_SIZE = 4096;
   private final static ClassProperties _prop= new ClassProperties(
                                                    SoasInterface.class);
   private final static String EMAIL_ERROR=  
                                 _prop.getError("email");

   //private final static String SOAS_METHOD= "SoasInterface: ";
   private final static String SPOT_CERTS= "spot_cacerts";

   private final static String CERT_OVERIRDE= 
                                "SoasInterface.cert.override.file.Name";

   // add a static block to set up for secured server connection

   static {
      try {
          //copyCerts();
          loadCerts();
          java.security.Security.addProvider(
               new com.sun.net.ssl.internal.ssl.Provider());
          System.setProperty("java.protocol.handler.pkgs", 
                             "com.sun.net.ssl.internal.www.protocol");
      } catch (Exception e) {
          String msg[]= {"https not working", e.toString() };
          ClientLog.warning(msg); 
      }
   }

   static public Vector processSecureSOASRequest(Vector   vIn, 
                                                 String   className, 
                                                 String   request,
                                                 HostPort server) 
                                         throws IOException, 
                                                ClassNotFoundException,
                                                FailedRequestException {
       return processRequest(vIn, null, "https:", className, request, server);
   }

   static public Vector processSecureSOASRequest(Vector   vIn, 
                                                 File     file,
                                                 String   className, 
                                                 String   request,
                                                 HostPort server) 
                                         throws IOException, 
                                                ClassNotFoundException,
                                                FailedRequestException {
       return processRequest(vIn, file, "https:", className, request, server);
   }

   static public Vector processSOASRequest(Vector   vIn, 
                                           String   className, 
                                           String   request,
                                           HostPort server) 
                                         throws IOException, 
                                                ClassNotFoundException,
                                                FailedRequestException {
       return processRequest(vIn, null, "http:", className, request, server);
  }

   static private Vector processRequest(Vector   vIn, 
                                        File     file,
                                        String   protocal,
                                        String   className, 
                                        String   request,
                                        HostPort server) 
                                         throws IOException, 
                                                ClassNotFoundException,
                                                FailedRequestException {
        Vector vOut;
        String theURL;
        theURL = protocal + "//" + server.getHost() +":"+ 
                                   server.getPort() + request;
                
        logMessage("Loading URL:" + theURL);
            
        // Create the URL object and get the connection
        URL aURL = new URL(theURL);
        URLConnection con = aURL.openConnection();
                
        // do both input and output
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false); // do not cache

        if (className == null) {
           con.setRequestProperty("Content-Type", "text/plain");
        }
        else {
           con.setRequestProperty("Content-Type", "java-internal/" + className);
        }
 

        if (vIn != null) {   // create object output , write and close buffer
            logMessage( "Writing the objects to stream...");
            ObjectOutputStream   objOS  = new ObjectOutputStream(
                          new BufferedOutputStream(con.getOutputStream())  );
            objOS.writeObject(vIn);
            if (file != null) submitFile(file,objOS);
            objOS.flush();
            objOS.close();
        }
                
        // get the input stream
        logMessage("Reading object from stream...");
        ObjectInputStream objIS = new ObjectInputStream(
                              new BufferedInputStream(con.getInputStream()) );
        try {
               vOut = (Vector)objIS.readObject();  // read the object
               if (vOut.size()==0) {
                   FailedRequestException e= new FailedRequestException(
                                  _prop.getError("general.noData"),
                      "A 0 length vector was returned from the network call");
                   logMessage("Failed: exception follows:\n"+ e);
                   throw e;
               }
               ExecStatus stat= (ExecStatus)vOut.elementAt(0);
               if (!stat.isOK()) {
                     FailedRequestException e= new FailedRequestException(
                                                  formatSOASUserError(stat),
                                                  stat.toString(),
                                                  stat.getException());
                     logMessage("Failed: exception follows:\n"+ e);
                     throw e;
               }
               objIS.close();
        } catch (IOException ioe) {
               objIS.close();
               throw ioe;
        } catch (ClassNotFoundException cnfe) {
               objIS.close();
               throw cnfe;
        }
        logMessage("Success!");
        return vOut;
   }

   static private void submitFile(File file, ObjectOutputStream objOS ) 
                                               throws IOException {

       logMessage("file length = " + file.length());
       FileInputStream in = new FileInputStream(file);
       BufferedInputStream buffIn = new BufferedInputStream(in, BUFF_SIZE);

       // write the file to the output stream
       // this must be done at the same time as the proposal data
       byte fileBuff[] = new byte[BUFF_SIZE];
       int numBytes;
       int totalBytes = 0;
       while ((numBytes = buffIn.read(fileBuff)) != -1) {
                objOS.write(fileBuff, 0, numBytes);
                totalBytes += numBytes;
       }
       logMessage("Total bytes written = " + totalBytes);

   }

   static public void confirmKeyCrack(HostPort hp) {
       if (!hp.getFirstSecureAccessMade()) {
             doSecuredSOASPing(hp.getHost(), hp.getPort() );
             hp.setFirstSecureAccessMade(true);
       }
   }

    /**
     * Ping the SOAS server on designated port.
     *
     * @param name the host
     * @param port the port
     * @return true if ping successul, otherwise false
     */
  static public boolean doSOASPing(String name, int port) {

      boolean retval= false;
      try {

        String theURL = "http://" + name + ":" + port + 
                        "/SutAPI/SutAPI?cmd=Ping";
        URL aURL = new URL(theURL);
        URLConnection con = aURL.openConnection();
                            
        // do both input and output
        con.setDoInput(true);
        con.setDoOutput(false);
                            
        // do not cache
        con.setUseCaches(false);

        // set the content type
        con.setRequestProperty("Content-Type", "text/html");
        // create and object output stream on the connection
                        
        // get the input stream
        InputStreamReader isr = new InputStreamReader(con.getInputStream());
        BufferedReader br = new BufferedReader(isr);
        String input;
        StringBuffer pingResponse = new StringBuffer();
        while((input = br.readLine()) != null) {
            pingResponse.append(input);
        }
        br.close();
        retval= true;
         //System.out.println (name + " is alive");
      } catch (UnknownHostException e) {
         //System.out.println(name+" unknown host");
         //System.out.println(e);
      } catch (IOException e2) {
         //System.out.println ("socket exception");
         //System.out.println (e2);
      }
      return retval;
  }

 /*
 * Ping the SOAS secured server on designated port.
 * 
 */
  static public boolean doSecuredSOASPing(String name, int port) {

      boolean retval= false;
      try {

        String theURL = "https://" + name + ":" + port + "/index.html";
        URL verisign = new URL(theURL);

        BufferedReader in = new BufferedReader(
				new InputStreamReader(
				verisign.openStream()));

	String inputLine;

	while ((inputLine = in.readLine()) != null)
	    System.out.println(inputLine);

	in.close();
	retval = true;

      } catch (Exception e) {
          // if there is a exception of anytype the ping just returns false
      }
      return retval;
   }

   public static String formatSOASUserError(ExecStatus stat) {
       String retval;
       int minor= stat.getStatusMinor();
       int major= stat.getStatusMajor();
       if (major == ExecStatusConst.SERVER_ERROR_GENERAL) { // major == 100
           if (minor == ExecStatusConst.GENERAL_UNKNOWN_OBJECT_TYPE ||
               minor == ExecStatusConst.GENERAL_EXCEPTION) {//minor==1 or 3
                    retval= _prop.getError("general.mismatch");
	    }
            else {
                    retval= _prop.getError("general.network");
            }
       }
       else if (major == ExecStatusConst.SERVER_ERROR_VIS) { // major == 700
          switch (minor) {
             case ExecStatusConst.VIS_SERVER_PROCESSING_ERROR: //minor==1
                      retval= _prop.getError("cspice.cspiceUnable") +
		                            "\n" + stat.getProcessMsg();
		      break;
             case ExecStatusConst.VIS_SERVER_CONNECTION_REFUSED : //minor==4
		      // Weblogic server is up but CSPICE server is down
		      // (fall through)
             case ExecStatusConst.VIS_TIMEOUT_WAITING_FOR_SERVER : //minor==2
             case ExecStatusConst.VIS_SERVER_NOT_AVAILALABLE : //minor==3
                     retval= _prop.getError("cspice.net");
                     break;
             default: 
                     retval= _prop.getError("cspice.net");
                     break;
         }
       }
       else if (major == ExecStatusConst.SERVER_ERROR_AIRE) {// major == 300
          switch (minor) {
             case ExecStatusConst.AIRE_SERVER_NOT_AVAILABLE://minor==2
		      retval = _prop.getError("aire.unavailable");
		      break;
             default: 
                      retval = _prop.getError("aire.unexpected") +
                               stat.getProcessMsg();
                      break;
         }
       }
       else if (major == ExecStatusConst.SERVER_ERROR_SECURITY) { // major==800
          switch (minor){
             case ExecStatusConst.SECURITY_USER_AUTHENTICATION://minor==1
		      retval = _prop.getError("security.authentication");
		      break;
             default: 
                      retval = _prop.getError("security.other") +
                               stat.getProcessMsg();
                      break;
              }
       }
       else if (major == ExecStatusConst.SERVER_ERROR_PERSISTENCE){//major==900
          switch (minor){
             case ExecStatusConst.PERSISTENCE_GENERAL_EXCEPTION://minor==1
		      retval = _prop.getError("persistence.general");
		      break;
             case ExecStatusConst.PERSISTENCE_PROGRAM_DOES_NOT_EXIST://minor==2
		      retval = _prop.getError("persistence.dne");
		      break;
             case ExecStatusConst.PERSISTENCE_INCORRECT_PASSWORD_FOR_PROGRAM :
                                                                    //minor==3
		      retval = _prop.getError("persistence.pwd");
		      break;
             default: 
                      retval = _prop.getError("persistence.other") +
                               stat.getProcessMsg();
                      break;
            }
       }
       else if (major == ExecStatusConst.SERVER_ERROR_EMAIL) { // major==1100
          switch (minor){
             case ExecStatusConst.EMAIL_SEND_FAILED: // minor==
		      retval =  EMAIL_ERROR +_prop.getError("email.send"); 
		      break;
             case ExecStatusConst.EMAIL_ADDRESS: // minor==
		      retval =  EMAIL_ERROR + _prop.getError("email.address");
		      break;
             default: 
                      retval = EMAIL_ERROR + stat.getProcessMsg();
                      break;
          }
       }
       else if (major == 
               ExecStatusConst.SERVER_ERROR_PROPOSAL_SUBMISSION) {//major==1200
          switch (minor){
            case ExecStatusConst.PROPSOSAL_SUBMISSION_SUBMISSION_SYSTEM_CLOSED:
                                                            //minor==3
		      //retval =  _prop.getError("proposal.closed");  AR7609 XW 10/30/2007
            retval =  stat.getStatusDesc();
		      break;
            default: 
                      retval = _prop.getError("proposal") +
                                stat.getProcessMsg();
                      break;
          }
       }
       else if (major == ExecStatusConst.SERVER_ERROR_PERCY) {//major==200
              // this should never happen - we don't use percy
            Assert.tst(false, "This should neve happen- we don't use Percy");
            retval = getUnexpected(stat);
       }
       else if (major == ExecStatusConst.SERVER_ERROR_COORD) {//major==400
            Assert.tst(false, "This should neve happen- " +
                        "we don't use server cooridinate transformation");
            retval = getUnexpected(stat);
       }
       else if (major==ExecStatusConst.SERVER_ERROR_SOFTWARE_UPDATE) {
                                                                //major==1000
            Assert.tst(false, "This should neve happen- " +
                    "we don't use software update from this interface");
            retval = getUnexpected(stat);
       }
       else {
            retval = getUnexpected(stat);
       }
       return retval;

    }

    private static String getUnexpected(ExecStatus stat) {
        //return UNEXPECTED_ERROR + stat.getStatusMajor() + "." +
        //                          stat.getStatusMinor();
        return _prop.getError("general.unexpected") +
               stat.getStatusMajor() + "." + stat.getStatusMinor();
    }

    private static void logMessage(String s) {
        //String tName= Thread.currentThread().getName();
        //Date   date= new Date();
        //System.out.println("_________________");
        //System.out.println(SOAS_METHOD+ "Thread: "+tName+": "+ date + ": " );
        //System.out.println("       " + s);
        //System.out.println("~~~~~~~~~~~~~~~~~");
        ClientLog.message(s); 
    }



    private static void loadCerts() {
      File certFile= null;
      String certOverrideStr= AppProperties.getPreference(CERT_OVERIRDE);
      if (certOverrideStr!=null) {
          certFile= new File(certOverrideStr);
          if (!certFile.canRead()) {
              System.out.println("Could not find override cert file: ");
              System.out.println(certFile.getPath());
              System.out.println("Using standard cert file.");
              certFile= null;
          }
      }
      if (certFile== null) {
          certFile= createDefaultCertFile();
          logMessage("Creating default cert file: "+certFile.getName() );
      }
      else {
          logMessage("Using override cert file: "+certFile.getName() );
      }

      System.setProperty("javax.net.ssl.trustStore",
                           certFile.getAbsolutePath());
    }


    private static File createDefaultCertFile() {

      Platform pf = Platform.getInstance();
      File     dir= pf.getWorkingDirectory();
      File     f  = new File(dir,SPOT_CERTS);
      if (f.canWrite()) f.delete();

      ClassLoader cl = SoasInterface.class.getClassLoader();
      Assert.tst(cl!=null);
      URL url= ClassLoader.getSystemResource(SPOT_CERTS);
      ClientLog.message("Atempting to readcert file from: ",
                         url.toString());

      DataInputStream       in  = null;
      BufferedOutputStream  out = null;
      URLConnection         conn;
      try {
	 conn = url.openConnection();
	 in   = new DataInputStream(new BufferedInputStream(
                                        conn.getInputStream()));
	 out  = new BufferedOutputStream(new FileOutputStream(f), 4096);
       
         try {
	     while(true) {
	         out.write(in.readByte());
             }
          } catch (EOFException e) {
             FileUtil.silentClose(in);
             FileUtil.silentClose(out);
          }
      } catch (IOException ioe) {
          ClientLog.warning("copyCert: " + ioe);
          f= null;
          FileUtil.silentClose(in);
          FileUtil.silentClose(out);
      }
      return f;
      //System.setProperty("javax.net.ssl.trustStore", f.getAbsolutePath());
      //System.out.println("try to load : "+  f.getAbsolutePath() );
    }

    public boolean doPing(String name, int port) {
        return doSOASPing(name, port);
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
