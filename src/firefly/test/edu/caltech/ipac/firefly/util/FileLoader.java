package edu.caltech.ipac.firefly.util;


import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.visualize.plot.*;
import nom.tam.fits.Fits;

import java.io.File;
import java.nio.file.Paths;

/**
 * Created by zhang on 10/19/16.
 * This is an utility class which loads data files in firefly_test_data.
 *
 * The data is stored in parallel as in the source codes under the java tree.  For example, the Histogram.java
 * is in the package "edu.caltech.ipac.visualize.plot".  The testing data will be located in the same
 * package under the test tree.
 *
 */
public class FileLoader {

    public static final String TEST_DATA_ROOT = "firefly_test_data/";

    /**
     * This method returns the data path for where the test class is located and where is the data is stored.
     * @param cls
     * @return
     * @throws ClassNotFoundException
     */
    public static String getDataPath(Class cls) {


        String rootPath =Paths.get("").toAbsolutePath().getParent().toUri().getPath();//"/hydra/cm/"; when test it in IntelliJ
        String testDataPath = TEST_DATA_ROOT+cls.getCanonicalName().replaceAll("\\.", "/")
                .replace(cls.getSimpleName(), "");

        String dataPath = rootPath + testDataPath;
        return dataPath;
    }


    public static FitsRead loadFitsRead(Class cls, String fitsFile)  {

        try {
            String inFitsName = getDataPath(cls) + fitsFile;

            Fits fits = new Fits(inFitsName);
            return FitsRead.createFitsReadArray(fits)[0];
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    public static FitsRead[] loadFitsReadArray(Class cls, String fitsFile)  {

        try {
            String inFitsName = getDataPath(cls) + fitsFile;

            Fits fits = new Fits(inFitsName);
            return FitsRead.createFitsReadArray(fits);
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    public static Fits loadFits(Class cls, String fitsFile)  {

        try {
            String inFitsName = getDataPath(cls) + fitsFile;

            return   new Fits(inFitsName);

        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    public static DataGroup loadIpacTable(Class cls, String tblFile) {

        if (!tblFile.endsWith(".tbl")){
            throw new IllegalArgumentException("Wrong file type, the file has to be a .tbl file");
        }

        try {
            File inFile = new File(getDataPath(cls) + tblFile);

            return IpacTableReader.readIpacTable(inFile, null, "inputTable");
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

     public static File resolveFile(Class cls, String fileName)  {

         try {
             return new File(getDataPath(cls) + fileName);
         }
         catch (Exception e){
             e.printStackTrace();
             return null;
         }
    }


}
