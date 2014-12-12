package edu.caltech.ipac.client.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

public class HttpPing implements Ping {

    /**
     * tries to open the any port, return true if successful
     */
    public boolean doPing(String name, int port) {
        return doPing(name, port, false);
    }

    /**
     * tries to open the any port, return true if successful
     */
    public static boolean doPing(String name, int port, boolean printStatus) {
        boolean       retval=false;
        URLConnection conn  = null;
        InputStream   in    = null;

        try {
            URL url=new URL("http://"+name+":"+port);
            conn=url.openConnection();
            in= conn.getInputStream();
            retval=true;
            if(printStatus) System.out.println(name+" is alive");
        } catch (UnknownHostException e) {
            System.out.println(name+":"+port +" unknown host");
            System.out.println(e);
        } catch (IOException e2) {
            System.out.println(name+":"+port +" "+ e2);
        } finally {
            try {
                if (in!=null) in.close();
                if (conn!=null) ((HttpURLConnection)conn).disconnect();
            } catch (IOException e) { /* do nothing */ }
        }
        return retval;
    }


    public static void main(String args[]) {
        //try {System.in.read(); } catch (IOException e) {}
        doPing(args[0], Integer.parseInt(args[1]), true);
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
