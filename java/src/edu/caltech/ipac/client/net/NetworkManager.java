package edu.caltech.ipac.client.net;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.ClientLog;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;

/**
 * a singleton
 * @author Trey Roby
 * @version $Id: NetworkManager.java,v 1.9 2012/07/30 23:34:39 roby Exp $
 */
public class NetworkManager implements PropertyChangeListener {

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
     private PropertyChangeSupport _propChange= new PropertyChangeSupport(this);
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
       //ProxySetupDialog.setupProxyFromSavedProperties();
       AppProperties.addPropertyChangeListener(this);
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
//-------------------------- PropertyChangeListener Interface -------
//===================================================================
    public void propertyChange(PropertyChangeEvent ev) {
    }

//===================================================================
//--------------- Public Add / Remove Property Change Methods -------
//===================================================================

    /**
     * Add a property changed listener.
     * @param p PropertyChangeListener the listener
     */
    public void addPropertyChangeListener (PropertyChangeListener p) {
       _propChange.addPropertyChangeListener (p);
    }

    /**
     * Remove a property changed listener.
     * @param p PropertyChangeListener  the listener
     */
    public void removePropertyChangeListener (PropertyChangeListener p) {
       _propChange.removePropertyChangeListener (p);
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
