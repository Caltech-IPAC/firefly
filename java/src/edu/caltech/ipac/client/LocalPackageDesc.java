package edu.caltech.ipac.client;

import edu.caltech.ipac.util.software.SoftwarePackage;
import edu.caltech.ipac.util.Installer;
import edu.caltech.ipac.util.JarVersion;
import edu.caltech.ipac.util.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;

class LocalPackageDesc {
    private String          _jarName;
    private boolean         _critical;
    private boolean         _inClassPath      = false;
    private String          _userDescription;
    private JarVersion      _jv               = null;
    private File            _jarFile          = null;
    private boolean         _downloaded       = false;
    private SoftwarePackage _serverPackage    = null;
    private File            _expandTarget     = new File(".");
    private final String          _source;

    public LocalPackageDesc(String jarName, String source) {
        this(jarName,false,"", source);
    }

    public LocalPackageDesc(File jarFile, String source) {
        this(jarFile.getName(),false,"", source);
    }

    /**
     *
     * @param jarName the jar file name
     * @param criticalFallback is this a critical jar, can be overrided by jar
     * @param fallbackUserDescription the user description to use if it is not
     *                          already specified in the jar file
     * @param source description of where the jar came from
     */
    public LocalPackageDesc(String  jarName,
                            boolean criticalFallback,
                            String  fallbackUserDescription,
                            String  source) {
       _jarName        = jarName;
       _critical       = criticalFallback;
       _userDescription= fallbackUserDescription;
        _source= source;
       init();
    }

    public boolean    isCritical()         { return _critical; }
    public String     getJarName()         { return _jarName; }
    public String     getUserDescription() { return _userDescription; }
    public boolean    isInClassPath()      { return _inClassPath; }
    public JarVersion getJarVersion()      { return _jv; }
    public File       getJarFile()         { return _jarFile; }
    public String     getSource()          { return _source; }



    public void setDownloaded(boolean d)   { _downloaded= d; }
    public boolean isDownloaded()          { return _downloaded; }

    public void setExpandTarget(File f)  { _expandTarget= f; }
    public File getExpandTarget()  { return _expandTarget; }

    public void setServerPackage(SoftwarePackage pkg) { _serverPackage= pkg; }
    public SoftwarePackage getServerPackage() { return _serverPackage; }

    private void init() {
       String classPath      = System.getProperty("java.class.path");
       String sep            = System.getProperty("path.separator");
       StringTokenizer st    = new StringTokenizer(classPath, sep);
       File            aFile;
       boolean         inCp  = false;

       while(st.hasMoreTokens() && !inCp) {
          aFile= new File(st.nextToken());
          if (aFile.getName().equals(_jarName) &&
                                         aFile.exists() &&
                                         !aFile.isDirectory()) {
              inCp = true;
              _jarFile= aFile;
              _jv= findJarVersion(aFile);
          }
       } // end loop
       if (_jv == null) { // look for it in lib dir
          File    libDir     = Installer.getInstallationLibDirFile();
          aFile= new File(libDir, _jarName);
          _jv= findJarVersion(aFile);
       }
       _inClassPath= inCp;
    }

    private JarVersion findJarVersion(File f) {
        JarVersion jv;
        try {
            jv= new JarVersion(f);
            initParams(jv, f.getName());
        } catch (FileNotFoundException fnfe) {
            jv= null;
            File propFile= new File(f.getPath() + ".prop");
            if (propFile.canRead()) {
                try {
                    InputStream in= new FileInputStream (propFile);
                    jv= new JarVersion(in, FileUtil.getBase(f));
                    initParams(jv, f.getName());
                } catch (IOException e) {
                    jv= null;
                }
            }
        }
        return jv;
    }


    private void initParams(JarVersion jv, String name) {
        String jvName= jv.getDisplayedName();
        if (!jvName.equals(name)) {
            _userDescription= jvName;
        }
        if (!_critical) {
            _critical= jv.containsAttribute(JarVersion.CRITICAL_ATTRIBUTE);
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
