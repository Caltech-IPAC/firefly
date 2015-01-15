/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.ClientLog;

import java.util.HashMap;
import java.util.Map;

/**
 * a singleton
 * @author Trey Roby
 * @version $Id: NetworkManager.java,v 1.9 2012/07/30 23:34:39 roby Exp $
 */
public class NetworkManager {

   public static final String MISSION_SERVER        = "MissionServer";
   public static final String MISSION_SECURE_SERVER = "MissionSecureServer";
   public static final String NED_SERVER            = "NEDServer";
   public static final String NED_NAME_RESOLVER     = "NEDNameResolver";
   public static final String SIMBAD_NAME_RESOLVER  = "SimbadNameResolver";
   public static final String SIMBAD4_NAME_RESOLVER = "Simbad4NameResolver";
   public static final String DSS_SERVER            = "DSSServer";
   public static final String ISO_SERVER            = "IsoServer";
   public static final String EPHEMERIS_PAIR_SERVER = "EphemerisPairServer";
   public static final String SKYVIEW_SERVER        = "SkyviewServer";
   public static final String HEASARC_SERVER        = "HeasarcServer";
   public static final String VIZIER_SERVER         = "VizierServer";
   public static final String AUTO_UPDATE_SERVER    = "AutoUpdateServer";
   public static final String IRSA                  = "IRSA";
   public static final String SPIZTER_POPULAR       = "SpitzerPopular";
   public static final String SPITZER_ARCHIVE       = "SpitzerArchive";
   public static final String HORIZONS_NAIF         = "HorizonsNaif";
   public static final String SDSS_SERVER           = "SDSSServer";


     private static NetworkManager _theInstance= null;
     private Map<String,HostPort> _servers   = new HashMap<String,HostPort>(11);
     private boolean  _logAdds = false;

     protected NetworkManager() {

       
       _logAdds= AppProperties.getBooleanPreference(
                                      "NetworkManager.logServerLoads", 
                                                                false);
 
       addServerWithProp(MISSION_SERVER,       "soas.ipac.caltech.edu",    80 );
       addServerWithProp(NED_SERVER,           "nedwww.ipac.caltech.edu",  80);
       addServerWithProp(NED_NAME_RESOLVER,    "nedsrv.ipac.caltech.edu",  10011);
       addServerWithProp(SIMBAD_NAME_RESOLVER, "simbad.harvard.edu",       80);
       addServerWithProp(SIMBAD4_NAME_RESOLVER,"simbad.u-strasbg.fr",      80);
       addServerWithProp(DSS_SERVER,           "archive.stsci.edu",        80);
       //addServerWithProp(ISO_SERVER,           "pma.iso.vilspa.esa.es",  8080);
       addServerWithProp(ISO_SERVER,           "ida.esac.esa.int",  8080);
       addServerWithProp(EPHEMERIS_PAIR_SERVER,"soas.ipac.caltech.edu",    80);
       addServerWithProp(SKYVIEW_SERVER,       "skys.gsfc.nasa.gov",       80);
       addServerWithProp(HEASARC_SERVER,       "heasarc.gsfc.nasa.gov",    80);
       addServerWithProp(VIZIER_SERVER,        "vizier.u-strasbg.fr",      80);
       addServerWithProp(IRSA,                 "irsa.ipac.caltech.edu",    80);
       addServerWithProp(AUTO_UPDATE_SERVER,   "soas.ipac.caltech.edu",    80);
       addServerWithProp(SPITZER_ARCHIVE,     "archive.spitzer.caltech.edu",80);
       addServerWithProp(SPIZTER_POPULAR,      "data.spitzer.caltech.edu",80);
       addServerWithProp(HORIZONS_NAIF,        "ssd.jpl.nasa.gov"          ,80);
       addServerWithProp(SDSS_SERVER,          "cas.sdss.org"             ,80);
     }

     public static NetworkManager getInstance() {
         if (_theInstance == null) _theInstance= new NetworkManager();
         return _theInstance;
     }

     public void addServer( String serverName, HostPort server) {
         String action;
         if (_logAdds) {
            if (_servers.get(serverName) ==null) {
                 action= "Adding-   ";
            }
            else {
                 action= "Updating- ";
            }
            ClientLog.brief(action + serverName + ": " + 
                                server.getHost()+ ":" +  server.getPort() );
         }
         _servers.put(serverName, server);
     }



     public HostPort getServer(String serverName) {
         return _servers.get(serverName);
     }


//===================================================================
//-------------------------- Private / Protected Methods ------------
//===================================================================


    private void addServerWithProp(String serverName, 
                                   String defaultHost,
                                   int    defaultPort) {

        String hostProp= "NetworkManager." + serverName + ".Host";
        String portProp= "NetworkManager." + serverName + ".Port";
        String host= AppProperties.getPreference(hostProp, defaultHost);
        int    port= AppProperties.getIntPreference(portProp, defaultPort);
        addServer(serverName,    new HostPort(host, port) );
    }


}
