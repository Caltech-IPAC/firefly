/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.action.Prop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Date: Nov 9, 2007
 *
 * @author loi
 * @version $Id: WebPropertyLoader.java,v 1.12 2012/09/06 22:42:46 loi Exp $
 */
public class WebPropertyLoader {


    public static final String ACCESSABLE_SERVER_PROPS= "client.accessible.props";
    public static final String WEBAPP_PROPERTIES = "webapp-properties";
    public static final String WEB_PROPERTIES_DIR = "properties-dir";
    public static final String THIS_JAR = "firefly.jar";
    public static final String SERVER_PROP_FILE = "_server.prop";
    private static final Properties _webPdb= new Properties();
    private static final Properties _webServerAccessibleOnlyPdb= new Properties();
//    private static final Properties _webPdb= null;


//    public static HashMap<String,String> getPropertyMap() {
//        System.out.println("Getting properties...");
//        return AppProperties.convertMainPropertiesToMap();
//    }



    public static String getAllPropertiesAsString() {
        return AppProperties.convertPropertiesToString(_webPdb);
    }

    public static String getServerAccessiblePropertiesAsString() {
        return AppProperties.convertPropertiesToString(_webServerAccessibleOnlyPdb);
    }

    /**
     * Loads all of the properties file in the given resources directory.
     * It will search first the class path, and then from file system.
     * @param resourcesDir the directory where the jars are found
     */
    public static void loadDirectoryProperties(String resourcesDir, boolean loadAllClientProperties) {

        try {
            URL url = WebPropertyLoader.class.getResource(resourcesDir );
            Properties targetPdb;

            File resDir;
            if ( url != null ) {
                resDir = new File(url.toURI());
            } else {
                resDir = new File(resourcesDir);
            }
            List<String> logList= new ArrayList<String>(50);
            if (resDir.isDirectory()) {
                File[] props = resDir.listFiles(new FilenameFilter(){
                            public boolean accept(File dir, String name) {
                                return name.endsWith(".prop") || name.endsWith(".properties");
                            }
                        });
                for(File f : props) {
                    try {
                        boolean serverOnly= f.getName().equals(SERVER_PROP_FILE );
                        targetPdb= serverOnly ? null : _webPdb;
                        if (loadAllClientProperties || serverOnly) {
                            AppProperties.addApplicationProperties(f, false,targetPdb);
                            logList.add( (serverOnly ? "Loaded server only file: ": "Loaded file: ") +f.getName() );
                        }
                    } catch (IOException e) {
                        logList.add("Could not load: " + f.getPath());
                        logList.add("        "+ e.toString());
                    }
                }
            }
            Logger.info(logList.toArray(new String[logList.size()]));
        } catch (URISyntaxException e) {
            Logger.warn("Unable to load resources from directory:" + resourcesDir, e.toString());
        } catch (IllegalArgumentException e) {
            Logger.warn("Unable to load resources from directory:" + resourcesDir, e.toString());
        }
    }


    public static void loadAllProperties(String resourcesDir, boolean loadAllClientProperties) {
        URL url= getThisClassURL();
        String urlStr= null;
        try {
            urlStr = URLDecoder.decode(url.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        int start= urlStr.indexOf('/');
        int end= urlStr.lastIndexOf('!');
        String fileStr;
        if (start < end) {
            fileStr= urlStr.substring(start, end);
            File jarFile= new File(fileStr);
            File jarsDir= jarFile.getParentFile();
            loadAllProps(new File(jarsDir,THIS_JAR), jarsDir, loadAllClientProperties );
        }
        else {
            System.out.println("Installer: Not a standard installation.");
            System.out.println("           "+ getThisClassFileName() +
                               " is not in a jar file.");
        }

        loadDirectoryProperties(resourcesDir, loadAllClientProperties);
        


        String aProps[]= Prop.getItems(ACCESSABLE_SERVER_PROPS);
        if (aProps != null) {
            for(String prop : aProps) {
                String value= AppProperties.getProperty(prop, null);
                if (value!=null) {
                    _webPdb.put(prop,value);
                    _webServerAccessibleOnlyPdb.put(prop,value);
                }
            }
        }

        // load runtime config properties if a client_override.prop exist.
        File clientOverride = ServerContext.getConfigFile("client_override.prop");
        if (clientOverride != null && clientOverride.canRead()) {
            System.out.println("Loading client_override.prop file...");
            loadProps(clientOverride, _webPdb);
        }
    }

    private static void loadProps(File source, Properties dest) {

        Properties props = new Properties();
        Reader sreader = null;
        try {
            sreader = new BufferedReader(new FileReader(source));
            props.load(sreader);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtil.silentClose(sreader);
        }

        if (props.size() > 0) {
            dest.putAll(props);
        }
    }

    private static void loadAllProps(File thisJarFile,  File jarDir, boolean loadAllClientProperties) {
        File jarFiles[]= FileUtil.listJarFiles(jarDir);
        if (thisJarFile!=null)  loadJar(thisJarFile, loadAllClientProperties);

        for(File jarFile : jarFiles) {
            if (!jarFile.equals(thisJarFile))  loadJar(jarFile, loadAllClientProperties);
        }
    }


    private static void loadJar(File jarFile, boolean loadAllClientProperties) {
        try {
            loadPropertiesFromJar(new JarFile(jarFile), loadAllClientProperties);
        } catch (IOException e) {
            System.out.println("Could not open: "+jarFile.getPath());
        }
    }

    private static void loadPropertiesFromJar(JarFile jf, boolean loadAllClientProperties) throws IOException {
        ZipEntry ze;
        InputStream is;
        String name;
        Properties targetPdb;

        Attributes att= jf.getManifest().getAttributes(WEBAPP_PROPERTIES);
        if (att!=null && att.containsKey(new Attributes.Name(WEB_PROPERTIES_DIR ))) {
            String directory= att.getValue(WEB_PROPERTIES_DIR );
            String dirStr= directory + "/";
            Enumeration<JarEntry> entries= jf.entries();
            List<String> logList= new ArrayList<String>(50);
            while (entries.hasMoreElements()) {
                ze= entries.nextElement();
                name= ze.getName();
                if (!ze.isDirectory() && name.startsWith(dirStr)) {
                    boolean serverOnly= name.endsWith(SERVER_PROP_FILE );
                    targetPdb= serverOnly ? null : _webPdb;
                    if (name.endsWith(".prop") || name.endsWith(".properties")) {
                        try {
                            is= jf.getInputStream(ze);
                            targetPdb= serverOnly ? null : _webPdb;
                            if (loadAllClientProperties || serverOnly) {
                                AppProperties.addApplicationProperties(is,targetPdb);
                                logList.add( (serverOnly ? "Loaded server only file: ": "Loaded file: ") +name );
                            }
                        } catch (IOException e) {
                            logList.add("Could not load: " + name);
                            logList.add("        "+ e.toString());
                        }
                    }
                }
            }
            Logger.info(logList.toArray(new String[logList.size()]));
            FileUtil.silentClose(jf);
        }
    }






    private static URL getThisClassURL() {
        return WebPropertyLoader.class.getClassLoader().getResource(getThisClassFileName());
    }

    private static String getThisClassFileName() {
        String cName= WebPropertyLoader.class.getName();
        return cName.replace(".", "/") + ".class";
    }





}
