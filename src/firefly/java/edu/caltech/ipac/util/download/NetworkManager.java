/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

import edu.caltech.ipac.util.AppProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * a singleton
 * @author Trey Roby
 */
public class NetworkManager {

    public static final String NED_SERVER            = "NEDServer";
    public static final String DSS_SERVER            = "DSSServer";
    public static final String IRSA                  = "IRSA";
    public static final String HORIZONS_NAIF         = "HorizonsNaif";
    public static final String SDSS_SERVER           = "SDSSServer";

    private static NetworkManager _theInstance= null;
    private Map<String,HostPort> _servers   = new HashMap<String,HostPort>(23);

    protected NetworkManager() {
        addServerWithProp(NED_SERVER,      "ned.ipac.caltech.edu",  443);
        addServerWithProp(DSS_SERVER,      "archive.stsci.edu",     443);
        addServerWithProp(IRSA,            "irsa.ipac.caltech.edu", 443);
        addServerWithProp(HORIZONS_NAIF,   "ssd.jpl.nasa.gov",      443);
        addServerWithProp(SDSS_SERVER,     "cas.sdss.org",          443);
    }

    public static NetworkManager getInstance() {
        if (_theInstance == null) _theInstance= new NetworkManager();
        return _theInstance;
    }

    public void addServer( String serverName, HostPort server) {
        _servers.put(serverName, server);
    }

    public HostPort getServer(String serverName) { return _servers.get(serverName); }


//===================================================================
//-------------------------- Private / Protected Methods ------------
//===================================================================

    private void addServerWithProp(String serverName,  String defaultHost, int    defaultPort) {
        String hostProp= "NetworkManager." + serverName + ".Host";
        String portProp= "NetworkManager." + serverName + ".Port";
        String host= AppProperties.getProperty(hostProp, defaultHost);
        int    port= AppProperties.getIntProperty(portProp, defaultPort);
        addServer(serverName,    new HostPort(host, port) );
    }
}