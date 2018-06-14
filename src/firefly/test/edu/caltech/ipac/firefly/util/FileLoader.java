package edu.caltech.ipac.firefly.util;


import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.DataGroup;
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
     * @return the path to firefly_test_data.
     */
    public static String getTestDataRoot() {
        String rootPath = Paths.get("").toAbsolutePath().getParent().toUri().getPath();
        return rootPath + TEST_DATA_ROOT;
    }

    /**
     * This method returns the data path for where the test class is located and where is the data is stored.
     * @param cls
     * @return
     * @throws ClassNotFoundException
     */
    public static String getDataPath(Class cls) {

        String clsDataPath = cls.getCanonicalName().replaceAll("\\.", "/")
                .replace(cls.getSimpleName(), "");

        String dataPath = getTestDataRoot() + clsDataPath;
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

        try {
            File inFile = new File(getDataPath(cls) + tblFile);

            return IpacTableReader.read(inFile);
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
