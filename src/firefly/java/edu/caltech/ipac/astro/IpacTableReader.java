/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro;

import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.action.ClassProperties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;

/**
 * read in the file in IPAC table format
 *
 * @author Xiuqin Wu
 */

//TODO: must work with IrsaAncilDataGetter

public final class IpacTableReader {

    private static ClassProperties _prop = new ClassProperties(
            IpacTableReader.class);

    private static final String NO_DATA = _prop.getError("noData");

    /**
     * Parse the file in IPAC table format and put the data in a
     * DataObjectGroup. If there is no data in the file, throw
     * IpacTableException
     */
    public static DataGroup readIpacTable(Reader fr, String catName) throws IpacTableException {
        return readIpacTable(fr, catName, null, true);
    }


    /**
     * Parse the file in IPAC table format and put the data in a
     * DataObjectGroup. If there is no data in the file, throw
     * IpacTableException
     *
     * @param isHeadersOnlyAllow set to true to allow ipac table with only headers(refer to as
     *                           attributes in this class) information.
     *                           don't confuse this with column's headers(refer to as headers in this class).
     */
    public static DataGroup readIpacTable(Reader fr,
                                          String catName,
                                          String onlyColumns[],
                                          boolean isHeadersOnlyAllow) throws IpacTableException {
        try {
            DataGroup retval = DataGroupReader.read(fr, true, false, false, onlyColumns);
            ensureIpac(retval, catName, isHeadersOnlyAllow);
            return retval;
        } catch (IOException e) {
            throw new IpacTableException(e.getMessage(), e);
        }
    }


    public static DataGroup readIpacTable(File f, String catName) throws IpacTableException {
        return readIpacTable(f, null, catName);
    }

    public static DataGroup readIpacTable(File f,
                                          String onlyColumns[],
                                          String catName) throws IpacTableException {
        return readIpacTable(f, onlyColumns, catName, true);
    }

    /**
     * Parse the file in IPAC table format and put the data in a
     * DataObjectGroup. If there is no data in the file, throw
     * IpacTableException
     */
    public static DataGroup readIpacTable(File f,
                                          String onlyColumns[],
                                          String catName,
                                          boolean isHeadersOnlyAllow) throws IpacTableException {
        try {
            DataGroup retval = DataGroupReader.read(f, true, false, false, onlyColumns);
            ensureIpac(retval, catName, isHeadersOnlyAllow);
            return retval;
        } catch (FileNotFoundException fnfe) {
            System.out.println("File not found Exception");
            throw new IpacTableException("File or object not found");
        } catch (IOException e) {
            throw new IpacTableException(e.getMessage(), e);
        }
    }


    private static void ensureIpac(DataGroup retval, String catName, boolean isHeadersOnlyAllow) throws IpacTableException {
        retval.setTitle(catName);
        if (retval.size() == 0) {
            if (!isHeadersOnlyAllow) {
                String name = AppProperties.getProperty("CatalogDialog.cats."
                        + catName + ".ShortName");
                if (name == null)
                    name = catName;
                throw new IpacTableException(NO_DATA + ": " + name);
            }
        }
    }


    public static void main(String args[]) {

        if (args.length > 0) {
            try {
                DataGroup dg = IpacTableReader.readIpacTable(
                        new File(args[0]), null, "test");
            } catch (IpacTableException e) {
                e.printStackTrace();
            }
        } else {
            try {
                System.in.read();
            } catch (IOException e) {
            }
            DataGroup IRAC1fixedGroup;
            File f = new File("2massmag3_formatted.tbl");

            String onlyColumns[] = {"ra", "dec", "name", "mag"};
            String catName = "IRAC1";
            try {
                IRAC1fixedGroup =
                        IpacTableReader.readIpacTable(f, onlyColumns, catName);
            } catch (Exception e) {
                System.out.println("got an exception:  " + e);
                e.printStackTrace();
            }
        }
    }

}
