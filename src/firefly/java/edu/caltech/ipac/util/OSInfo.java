/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;


public class OSInfo {

  //  --- These are the Specific OS types
  //  WINDOWS and UNKNOWN_UNIX  are for os types we have not yet
  //  specifically identified

    public static final String SUN           = "SunOS";
    public static final String LINUX         = "Linux";
    public static final String LINUX64       = "Linux64";
    public static final String UNKNOWN_UNIX  = "UnknownUnix";

    public static final String WIN_95    = "Windows 95";
    public static final String WIN_98    = "Windows 98";
    public static final String WIN_ME    = "Windows Me";
    public static final String WIN_XP    = "Windows XP";
    public static final String WIN_2000  = "Windows 2000";
    public static final String WIN_NT    = "NT";
    public static final String WIN_VISTA = "Windows Vista";
    public static final String FUTURE_WINDOWS= "FutureWindows";

    public static final String MAC       = "MAC";
    public static final String MAC_LEOPARD_OR_LATER = "MAC_LEOPARD_OR_LATER";
    public static final String MAC_LION_OR_LATER = "MAC_LION_OR_LATER";
    public static final String MAC64 = "MAC64";

  //  --- These are the general categories of OS types
    public static final String ANY_WINDOWS      = "AnyWindows";
    public static final String ANY_WIN_95_98_ME = "ANY_WIN_95_98_ME";
    public static final String ANY_WIN_NT       = "NT";
    public static final String ANY_UNIX         = "AnyUnix";
    public static final String ANY_UNIX_OR_MAC  = "AnyUnixOrMac";



    public static final int BASED_ON_DOS     = 55;
    public static final int BASED_ON_NT      = 56;
    public static final int BASED_ON_UNIX    = 57;

    public static final String WINDOWS_STR = "windows";

    private static final String _platform;
    
    private static final int    _baseOStype;

    private final static boolean _anyWindows;
    private final static boolean _any9598ME;
    private final static boolean _anyNT;
    private final static boolean _anyUnix;
    private final static boolean _leopardPlus;
    private final static boolean _lionPlus;


    static {
        String osname  = System.getProperty("os.name");
        String version  = System.getProperty("os.version");
        boolean anyWindows = false;
        boolean any9598ME  = false;
        boolean anyNT      = false;
        boolean anyUnix    = false;
        boolean leopardPlus= false;
        boolean lionPlus   = false;
        if (osname.indexOf(SUN) >-1) {
            _platform   = SUN;
            _baseOStype = BASED_ON_UNIX;
            anyUnix    = true;
        }
        else if (osname.indexOf(LINUX) >-1) {
            _platform   = LINUX;
            _baseOStype = BASED_ON_UNIX;
            anyUnix    = true;
        }
        else if (osname.indexOf(WIN_95) >-1) {
            _platform   = WIN_95;
            _baseOStype = BASED_ON_DOS;
            any9598ME  = true;
            anyWindows = true;
        }
        else if (osname.indexOf(WIN_98) >-1) {
            _platform   = WIN_98;
            _baseOStype = BASED_ON_DOS;
            any9598ME  = true;
            anyWindows = true;
        }
        else if (osname.indexOf(WIN_ME) >-1) {
            _platform   = WIN_ME;
            _baseOStype = BASED_ON_DOS;
            any9598ME  = true;
            anyWindows = true;
        }
        else if (osname.toLowerCase().indexOf(WIN_NT.toLowerCase()) >-1) {
            _platform   = WIN_NT;
            _baseOStype = BASED_ON_NT;
            anyNT      = true;
            anyWindows = true;
        }
        else if (osname.toLowerCase().indexOf(WIN_XP.toLowerCase()) >-1) {
            _platform   = WIN_XP;
            _baseOStype = BASED_ON_NT;
            anyNT      = true;
            anyWindows = true;
        }
        else if (osname.toLowerCase().indexOf(WIN_2000.toLowerCase()) >-1) {
            _platform   = WIN_2000;
            _baseOStype = BASED_ON_NT;
            anyNT      = true;
            anyWindows = true;
        }
        else if (osname.toLowerCase().indexOf(WIN_VISTA.toLowerCase()) >-1) {
            _platform   = WIN_VISTA;
            _baseOStype = BASED_ON_NT;
            anyNT      = true;
            anyWindows = true;
        }
        else if (osname.toLowerCase().indexOf(WINDOWS_STR.toLowerCase()) >-1) {
            _platform   = FUTURE_WINDOWS;
            _baseOStype = BASED_ON_NT;
            anyNT      = true;
            anyWindows = true;
        }
        else if (osname.toLowerCase().indexOf(MAC.toLowerCase()) >-1) {
            _platform   = MAC;
            _baseOStype = BASED_ON_UNIX;
            String v[]= version.split("\\.");
            if (v.length>1) {
                int major= Integer.valueOf(v[0]);
                int minor= Integer.valueOf(v[1]);
                leopardPlus= (major>=10 && minor>=5);
                lionPlus= (major>=10 && minor>=7);
            }
        }
        else { // guess - the guess some unsupported unix
            _platform   = UNKNOWN_UNIX;
            _baseOStype = BASED_ON_UNIX;
            anyUnix    = true;
        }
        _anyWindows= anyWindows;
        _any9598ME= any9598ME;
        _anyNT= anyNT;
        _anyUnix= anyUnix;
        _leopardPlus= leopardPlus;
        _lionPlus = lionPlus;
    }


    public static boolean isPlatform(String p) {
       boolean retval= false;
       if (p.equals(_platform)) {
         retval= true;
       }
       else {
         if      (p.equals(ANY_WINDOWS))      retval= _anyWindows;
         else if (p.equals(ANY_WIN_95_98_ME)) retval= _any9598ME;
         else if (p.equals(ANY_WIN_NT))       retval= _anyNT;
         else if (p.equals(ANY_UNIX))         retval= _anyUnix;
         else if (p.equals(ANY_UNIX_OR_MAC))  retval= _anyUnix ||
                                                       _platform.equals(MAC);
         else if (p.equals(MAC_LEOPARD_OR_LATER)) retval= _platform.equals(MAC) && _leopardPlus;
         else if (p.equals(MAC64)) retval= _platform.equals(MAC) &&
                 System.getProperty("os.arch")!=null && System.getProperty("os.arch").contains("64");
         else if (p.equals(MAC_LION_OR_LATER)) retval= _platform.equals(MAC) && _lionPlus;
         else if (p.equals(LINUX64)) retval= _platform.equals(LINUX) &&
                 System.getProperty("os.arch")!=null && System.getProperty("os.arch").contains("64");

       }
       return retval;
    }

    public static int    getBaseOStype()         { return _baseOStype; }
    public static String getRecognizedPlatform() { return _platform; }
    public static String getOSName() { return System.getProperty("os.name"); }


   public static void main( String[] args ) {
       System.out.println("platform:     " + getRecognizedPlatform() );
       System.out.println("OS name:      " + getOSName() );
       System.out.println("Base OS type: " + getBaseOStype() );
//       System.out.println("jar dir:      " + Installer.findDirWithJars() );
//       System.out.println("install root: "  +
//                               Installer.deriveInstallationRoot());
   }
}
