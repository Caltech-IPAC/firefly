/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;


/**
 * @author Trey Roby
 * @version $Id: HostPort.java,v 1.2 2005/12/08 22:30:43 tatianag Exp $
 */
public class HostPort {
    private String   _host;
    private int      _port;

    public HostPort(String host, int port) {
         _host= host;
         _port= port;
    }
    public int    getPort() { return _port; }
    public String getHost() { return _host; }
    public String toString(){
       return      "Server Host: " + _host + ":" + _port;
    }
}
