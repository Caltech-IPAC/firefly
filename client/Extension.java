package edu.caltech.ipac.client;

import edu.caltech.ipac.gui.CascadeAction;
import edu.caltech.ipac.gui.CommandTable;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.Installer;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.action.AutoCreateConstructor;
import edu.caltech.ipac.util.action.AutoCreateFactoryMethod;
import edu.caltech.ipac.util.action.GeneralAction;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
/**
 * User: roby
 * Date: Mar 29, 2007
 * Time: 1:47:46 PM
 */

/**
 *
 * @author Trey Roby
 */
public class Extension {


    public static final String SP_EXTENSIONS= "spot-common-extensions";
    public static final String EXT_PACKAGES= "Extension-packages";
    public static final String ACTION_EXT= "Action-Extensions";
    public static final String LOC_ROOT= "-location";
    public static final String CAS_EXT= "Cascade-extensions";
    public static final String APP_TOOLBAR= "app-toolbar";
    public static final String VIS_TOOLBAR= "vis-toolbar";

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    /**
     * Search for any extenion jars in either the ~/.spot/extenions directory or the
     * installdir/extensions directory.  If there is a jar in one of these directory
     * the add the jar to the classpath.  The look to manifest entries in the
     * jar for what sort of extensions it supports. If there is a package of
     * exdtensions actions then call the loadActions() method for each extension found.
     * @param appCom the ApplicationComponents class, a class containing
     *                all the parameters the we know about
     */
    public void searchExtensions(ApplicationComponents appCom) {
        // make a list of jars from ~/.spot/extensions & installroot/extensions
        // add jars to classpath
        // filter jars to see if it has manifest information
        //

        File sysExtDir= Installer.getSysExtensionDirFile();
        File userExtDir= new File(Platform.getInstance().getWorkingDirectory(),
                                  Installer.getUserExtensionDir());
        File sysJars[]=FileUtil.listJarFiles(sysExtDir);
        File userJars[]=FileUtil.listJarFiles(userExtDir);
        int len= (sysJars==null ? 0 : sysJars.length) +
                 (userJars==null ? 0 : userJars.length);
        if (len>0) {
            int idx= 0;
            File allJars[]= new File[len];
            if (sysJars!=null) for(File f : allJars) allJars[idx++]= f;
            if (userJars!=null) for(File f : userJars) allJars[idx++]= f;
            

            List<URL> jarUrlList= new ArrayList<URL>(allJars.length);


            ClassLoader prevCl = Thread.currentThread().getContextClassLoader();

//            ClassLoader urlCl = URLClassLoader.newInstance(
//                                          jarUrlList.toArray(new URL[jarUrlList.size()]), prevCl);
            for(File jf : allJars) {
                addToList(jarUrlList, jf);
            }

            ClassLoader urlCl = new MyClassLoader(
                                jarUrlList.toArray(new URL[jarUrlList.size()]),
                                prevCl);
            Thread.currentThread().setContextClassLoader(urlCl);

            
            for(File jf : allJars) {
                searchForExtentions(appCom, jf);
            }

        }
    }

    /**
     * Load all the actions in the package specified that contain either the
     * AutoCreateConstructor or the AutoCreateFactoryMethod annotations.
     * If this parameters to invoke the methods or constructor exist in the
     * ApplicationComponents object then attempt to invoke the method
     * @param inPackage the package to search for automatically created actions
     * @param appComp the ApplicationComponents class, a class containing
     *                all the parameters the we know about
     */
    public void loadActions(Package inPackage,
                            ApplicationComponents appComp) {
        File file = findClassLocation(inPackage);
        List<String> foundClasses;
        String replaceSymbol;
        if (file.isDirectory()) {
            foundClasses= getClassesFromDir(inPackage, file);
            replaceSymbol= File.separator;
        }
        else {
            foundClasses= getClassesFromJar(inPackage, file);
            replaceSymbol= "/";
        }
        ClassLoader cl= Thread.currentThread().getContextClassLoader();
        Constructor constructors[];


        for(String cStr : foundClasses) {
            String className= cStr.replace(replaceSymbol, ".").replace(".class", "");
            try {
                Class c= cl.loadClass(className);
                if (GeneralAction.class.isAssignableFrom(c)) {
                    constructors= c.getConstructors();
                    for(Constructor con : constructors) {
                        if (con.isAnnotationPresent(AutoCreateConstructor.class)) {
                            createByConstructor(appComp, c, con);
                        }
                    }
                    checkAllMethods(appComp, c);

                }
            } catch (ClassNotFoundException e) {
                ClientLog.warning(className + ": not found");
            }
        }
    }



//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private void searchForExtentions(ApplicationComponents appCom, File file) {

        try {
            MyClassLoader cl= (MyClassLoader)Thread.currentThread().getContextClassLoader();
            JarFile jar= new JarFile(file);
            Manifest manifest= jar.getManifest();
            Attributes att= manifest.getAttributes(SP_EXTENSIONS);
            if (att != null) {
                loadAllClassesInJar(file);
                if (att.containsKey(new Attributes.Name(EXT_PACKAGES))) {
                    String pcks=att.getValue(EXT_PACKAGES);
                    String pckAry[]= pcks.split(" ");
                    Package packagePtr;
                    for(String p : pckAry) {
                        packagePtr =  cl.getPackage(p);
                        loadActions(packagePtr, appCom);
                    }
                    associateActionsWithUI(att);
                }
            }
        } catch (IOException e) {

            ClientLog.warning("could not open: "+ file.getPath(),
                              e.toString());
        }

    }



    private void associateActionsWithUI(Attributes att) {
        ApplicationVersion av= ApplicationVersion.getInstalledApplicationVersion();
        String appName= av.getAppName();
        if (att.containsKey(new Attributes.Name(ACTION_EXT))) {
            String cmdAry[]= att.getValue(ACTION_EXT).split(" ");
            for(String cmd : cmdAry) {
                if (att.containsKey(new Attributes.Name(cmd+LOC_ROOT))) {
                    String locAry[]= att.getValue(cmd+LOC_ROOT).split(" ");
                    for(String loc : locAry) {
                        addCommandToLocation(cmd, loc, appName);

                    } // end loop
                } // end if
            } // end loop
        }
        if (att.containsKey(new Attributes.Name(CAS_EXT))) {
            CommandTable com= CommandTable.getInstance();
            String cmdAry[]= att.getValue(CAS_EXT).split(" ");
            String newCmd;
            for(String cmd : cmdAry) {
                if (att.containsKey(new Attributes.Name(cmd+LOC_ROOT))) {
                    String loc= att.getValue(cmd+LOC_ROOT);
                    newCmd= appName +"."+cmd;
                    GeneralAction casAction= new CascadeAction(newCmd);
                    casAction.setName(cmd);
                    com.addCommand(casAction);
                    addCommandToLocation(newCmd,loc, appName);
                } // end if
            } // end loop

        }
    }


    private void addCommandToLocation(String cmd, String loc, String appName) {
        CommandTable     com= CommandTable.getInstance();
        GeneralAction action= com.findCommand(cmd);
        if (action!=null) {
            String key;
            if (APP_TOOLBAR.equalsIgnoreCase(loc)) {
                key= appName+".ToolBar";

            }
            else if (VIS_TOOLBAR.equalsIgnoreCase(loc)) {
                key= "vis.ToolBar";

            }
            else {
                key= appName +"."+ loc;
            }
            action.addRequestedLocation(key);
        }
    }




    private void checkAllMethods(ApplicationComponents appCom,
                                Class                 c) {
        Class genActClass= GeneralAction.class;
        for(Method meth : c.getMethods()) {
            if (meth.isAnnotationPresent(AutoCreateFactoryMethod.class)) {
                if (genActClass.isAssignableFrom( meth.getReturnType())) {
                    createByFactoryMethod(appCom, c, meth);
                }
                else {
                    ClientLog.warning(
                           "Cannot call method" +
                           "Method does not return type: " + genActClass,
                           "Method: " + meth.getName(),
                           "Class:  " + c.getName());

                }
            }
        }

    }

    private void createByFactoryMethod(ApplicationComponents appCom,
                                       Class                 c,
                                       Method                meth) {

        try {
            Class paramsClass[]= meth.getParameterTypes();
            Object objs[]= appCom.getObjects(paramsClass);
            if (objs!=null) {
                GeneralAction a= (GeneralAction)meth.invoke(null, objs);
                CommandTable     com= CommandTable.getInstance();
                com.addCommand(a);
                logSuccess(a);
            }
            else {
                logParamMismatch(appCom, c, paramsClass);
            }
        } catch (InvocationTargetException e) {
            ClientLog.warning("Could not instanciate class" + c.getName(),
                              e.toString(), "Traceback follows");
            e.printStackTrace();
        } catch (Exception e) {
            ClientLog.warning("Could not instanciate: " + c.getName(),
                              e.toString());
        }
    }

    private void createByConstructor(ApplicationComponents appCom,
                                     Class                 c,
                                     Constructor           constructor) {

        try {

            Class paramsClass[]= constructor.getParameterTypes();
            Object objs[]= appCom.getObjects(paramsClass);
            if (objs!=null) {
                GeneralAction a= (GeneralAction)constructor.newInstance(objs);
                CommandTable     com= CommandTable.getInstance();
                com.addCommand(a);
                logSuccess(a);
            }
            else {
                logParamMismatch(appCom, c, paramsClass);
            }
        } catch (InvocationTargetException e) {
            ClientLog.warning("Could not instanciate class" + c.getName(),
                              e.toString(), "Traceback follows");
            e.printStackTrace();
        } catch (Exception e) {
            ClientLog.warning("Could not instanciate class" + c.getName(),
                              e.toString());
        }

    }

    private void logParamMismatch(ApplicationComponents appCom, Class c,
                                  Class params[]) {
        ClientLog.warning("Could not instanciate: " + c.getName(),
                          "It wants an parameter I cannot provide.",
                          "Requesting Parameters: ",
                          Arrays.toString( params),
                          "Available parameters: ",
                          appCom.listClasses());
    }

    private void logSuccess(GeneralAction a) {
        ClientLog.brief("Instanciating : " + StringUtil.getShortClassName(a.getClass()) +
                        ", Command: " + a.getActionCommand());
    }

    private void loadAllClassesInJar(File inJarFile) {
        try {
            MyClassLoader cl= (MyClassLoader)Thread.currentThread().getContextClassLoader();
            JarFile jarFile= new JarFile(inJarFile);
            Enumeration<JarEntry> eList= jarFile.entries();
            JarEntry je;
            String jeName;
            String cStr= "";
            while(eList.hasMoreElements()) {
                je= eList.nextElement();
                jeName= je.getName();
                if (!je.isDirectory() && jeName.endsWith(".class")) {
                    try {
                        cStr= jeName.substring(0,jeName.indexOf(".class"));
                        cStr= cStr.replace("/", ".");
                        cl.loadClass(cStr);
                    } catch (ClassNotFoundException e) {
                        ClientLog.warning("Could not load class:" + cStr);
                    }
                }
            }
        } catch (IOException e)  {
            ClientLog.warning(true, e.toString());
        }
    }

    private List<String> getClassesFromDir(Package inPackage, File dir) {
        List<String> retList= new ArrayList<String>(100);
        String pFileDir= inPackage.getName().replace(".", File.separator);
        File fileList[]= FileUtil.listFilesWithExtension(dir,"class");
        String s;
        for(File f : fileList) {
            s= f.getPath();
            s = s.substring(s.indexOf(pFileDir));
            retList.add(s);
        }
        return retList;
    }

    private List<String> getClassesFromJar(Package inPackage, File inJarFile) {
        List<String> retList= new ArrayList<String>(100);
        try {
            String pFileDir= inPackage.getName().replace(".", "/");

            JarFile jarFile= new JarFile(inJarFile);
            Enumeration<JarEntry> eList= jarFile.entries();
            JarEntry je;
            String jeName;
            File tstDir= new File(pFileDir);
            File jeAsFile;
            while(eList.hasMoreElements()) {
                je= eList.nextElement();
                jeName= je.getName();
                jeAsFile= new File(jeName);
                if (!je.isDirectory() &&
                    jeName.endsWith(".class") &&
                    jeAsFile.getParentFile().equals(tstDir)) {
                    retList.add(jeName);

//                    System.out.println("found: "+ jeName);
                }
            }
        } catch (IOException e)  {
            ClientLog.warning(true, e.toString());
        }
        return retList;
    }


    private File findClassLocation(Package inPackage) {

        File retval= null;

        String pName= inPackage.getName();
        String pFileName= pName.replace(".", "/");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL thisPackage= cl.getResource(pFileName);

        String urlStr= thisPackage.toString();
        int start= urlStr.indexOf('/');
        int end= urlStr.lastIndexOf('!');
        String fileStr;
        if (start < end) {
            fileStr= urlStr.substring(start, end);
            File jarFile= new File(fileStr);

            String cleanValue;
            String preClean= jarFile.getPath();


            // There is the issues with the plus sign (+) and decode
            // decode automaticly turns the plus to a blank.  That is not
            // good if there is a plus sign in the path.  However, decode
            // is still nice to use.  The next section of code uses docode
            // when there is no plus sign and uses the old way when there is.
            // There old way just looks for spaces a %20 and turns them into
            // spaces.
            if (preClean.indexOf('+')<0) {
                try {
                    cleanValue= URLDecoder.decode(preClean, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    cleanValue= jarFile.getPath().replaceAll("%20", " ");
                }
            }
            else {
                cleanValue= jarFile.getPath().replaceAll("%20", " ");
            }


            File finalJarFile= new File(cleanValue);
            if (finalJarFile.exists()) {
                retval= finalJarFile;
            }
        }
        else { // i should have a directory
            // todo: add support for directories
           fileStr= urlStr.substring(start);
            retval= new File(fileStr);
        }
        return retval;
    }

    private void addToList(List<URL> jarList, File jarFile) {
        try {
            URL url= jarFile.toURI().toURL();
            jarList.add(url);
        } catch (MalformedURLException e) {
            ClientLog.warning(true, "could not add " + jarFile.getPath() +
                                    " to classpath", e.toString() );
        }
    }


// =====================================================================
// -------------------- InnerClasses --------------------------------
// =====================================================================
    private class MyClassLoader extends URLClassLoader {
        public MyClassLoader(URL urls[], ClassLoader parent) {
            super(urls,parent);
        }

        @Override
        public Package getPackage(String s) {
            return super.getPackage(s);
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
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
