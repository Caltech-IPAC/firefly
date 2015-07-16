/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro;

import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import nom.tam.fits.FitsException;
import uk.ac.starlink.table.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* Convert an FITS file or FITS binary table(s) to list of DataGroup.
*/
public final class FITSTableReader
{
    public static boolean debug = true;

    /**
     * Declare the strategies to handle the FITS data:
     *
     * TOP_MOST: Ignore the repeat count portion of the TFORMn Keyword
     * returning only the first datum of the field, even if repeat count is more than 1.
     * This should produce exactly one DataGroup per table. This is the default strategy if not given.
     *
     * FULLY_FLATTEN: Generates one DataGroup row for each value of an HDU field.
     * Because each field may have different repeat count (dimension), insert blank
     * when no data is available. This should produce exactly one DataGroup per table.
     *
     * EXPAND_BEST_FIT: Expands each HDU row into one DataGroup. Fields with lesser count (dimension) will be filled with blanks.
     * EXPAND_REPEAT: Expands each HDU row into one DataGroup. Fields with lesser dimension will be filled with previous values.
     */
    private static final String TOP_MOST = "TOP_MOST";
    private static final String EXPAND_BEST_FIT = "EXPAND_BEST_FIT";
    private static final String EXPAND_REPEAT = "EXPAND_REPEAT";
    private static final String FULLY_FLATTEN = "FULLY_FLATTEN";

    private static final int maxNumAttriValues = 80;

    static void usage()
    {
	System.out.println("usage java edu.caltech.ipac.astro.FITSTableReader <fits_filename> <ipac_filename>");
	System.exit(1);
    }

    public static void main(String[] args)
    {
        if (args.length != 2)
        {
            usage();
        }
        String fits_filename = args[0];
        String ipac_filename = args[1];


        FITSTableReader fits_to_ipac = new FITSTableReader();

        //for lsst:
        String[] dataCols = {"flags", "id", "coord ra", "coord_dec", "parent", "footprint", "base_ClassificationExtendedness_value", "base_SdssCentroid_xSigma"};
        //String[] dataCols = null;
        String[] headerCols = {"flags", "id", "coord_ra","coord_dec", "parent", "footprint","base_ClassificationExtendedness_value", "base_SdssCentroid_xSigma"};
        //String[] headerCols = null;

        //for lsst_cat:
        //String[] dataCols = {"id", "cat.archive", "name", "kernel", "center_x", "spatialfunctions", "components", "coefficients", "image", "cd", "ctype1", "A", "Ap"};
        //String[] dataCols = null;
        //String[] headerCols = {"id", "name", "kernel", "center_x", "spatialfunctions", "components", "coefficients", "image", "cd", "ctype1", "A", "Ap"};
        //String[] headerCols = null;

        //String strategy = EXPAND_BEST_FIT;
        String strategy = EXPAND_REPEAT;
        //String strategy = TOP_MOST;
        //String strategy = FULLY_FLATTEN;

        int whichDG = 0;
        if (strategy == EXPAND_BEST_FIT){
            whichDG = 0;
        }
        else if (strategy == EXPAND_REPEAT) {
            whichDG = 0;//13;
        }
        else if (strategy == TOP_MOST) {
            whichDG = 0;
        }
        else if (strategy == FULLY_FLATTEN) {
            whichDG = 4;
        }

        try {
            List<DataGroup> dgListTotal = fits_to_ipac.convertFitsToDataGroup(
                    fits_filename,
                    dataCols,
                    headerCols,
                    strategy);

            File output_file = new File(ipac_filename);
            DataGroup dg = dgListTotal.get(whichDG);
            IpacTableWriter.save(output_file, dg);
        }
        catch (FitsException fe)
        {
            System.out.println("got FitsException: " + fe.getMessage());
            fe.printStackTrace();
        }
        catch (TableFormatException te)
        {
            System.out.println("got TableFormatException: " + te.getMessage());
            te.printStackTrace();
        }
        catch (IOException ioe)
        {
            System.out.println("got IOException: " + ioe.getMessage());
            ioe.printStackTrace();
        }

        if (true) return;


        try
        {
            //List<DataGroup> dgList = fits_to_ipac.convertFITSToDataGroup(fits_filename, null);
            List<DataGroup> dgList = fits_to_ipac.convertFitsToDataGroup(fits_filename, null, null, "TOP_MOST");

            File output_file = new File(ipac_filename);
            DataGroup dg = dgList.get(0);
            IpacTableWriter.save(output_file, dg);
        }
        catch (FitsException fe)
        {
            System.out.println("got FitsException: " + fe.getMessage());
            fe.printStackTrace();
        }
        catch (IOException ioe)
        {
            System.out.println("got IOException: " + ioe.getMessage());
            ioe.printStackTrace();
        }

    }


    /**
     * Convert a FITS file to a list of DataGroup.
     * @param fits_filename
     * @param dataCols: The names of the columns which will be copied to the data section of the DataGroups.
     *                If dataCols = null, get all the columns into the data group.
     * @param headerCols: The names of the columns which will be copied to the header section of the DataGroups.
     *                If headerCols = null, get none of the columns into the header of the data group.
     * @param strategy The strategy used to deal with the repeat count  of the data in the given dataCols columns.
     * @return
     * @throws FitsException
     * @throws IOException
     */
    public static List<DataGroup> convertFitsToDataGroup(String fits_filename,
                                                         String[] dataCols,
                                                         String[] headerCols,
                                                         String strategy)
        throws FitsException, IOException {

        List tList = getStarTableList(fits_filename);

        List<DataGroup> dgListTotal = new ArrayList();
        // for testing:
        //for (int i = 4; i < tList.size(); i++) {
        for (int i = 0; i < tList.size(); i++) {
            StarTable table = (StarTable) tList.get(i);
            List<DataGroup> dgList = convertFitsToDataGroup(table, dataCols, headerCols, strategy);
            dgListTotal.addAll(dgList);
        }
        return dgListTotal;
    }

    /**
     * Get a list of star tables from a FITS file.
     * @param fits_filename
     * @return List of star tables.
     * @throws IOException
     */
    public static List getStarTableList(String fits_filename) throws IOException {

        TableSequence tseq;
        StarTableFactory stFactory = new StarTableFactory();
        tseq = stFactory.makeStarTables(fits_filename, null);
        List tList = new ArrayList();
        for (StarTable tbl; (tbl = tseq.nextTable()) != null; ) {
            tList.add(tbl);
        }
        return tList;
    }

    /**

    public static List<DataGroup> convertFitsToDataGroup(int index,
                                                         String fits_filename,
                                                         String[] dataCols,
                                                         String[] headerCols,
                                                         String strategy)
            throws FitsException, TableFormatException, IOException {

        List<DataGroup> dgListTotal = new ArrayList<DataGroup>();


        return dgListTotal;
    }

     */

    /** Convert a binary table (StarTable) into a list of DataGroup.
     * @param table input StarTable from one HDU
     * @param dataCols: The names of the columns which will be copied to the data section of the DataGroups.
     *                If dataCols = null, get all the columns into the data group.
     * @param headerCols: The names of the columns which will be copied to the header section of the DataGroups.
     *                If headerCols = null, get none of the columns into the header of the data group.
     * @param strategy A list of strategies used to deal with the repeat count (dimension) of the data in the given dataCols columns.
     * @return List<DataGroup> A list of DataGroups
     */
    public static List<DataGroup> convertFitsToDataGroup(StarTable table,
                                                         String[] dataCols,
                                                         String[] headerCols,
                                                         String strategy)
            throws FitsException, TableFormatException, IllegalArgumentException, IOException {

        if (!(strategy.equals(FULLY_FLATTEN)) && !(strategy.equals(EXPAND_BEST_FIT)) && !(strategy.equals(EXPAND_REPEAT))){
            strategy = TOP_MOST;
        }

        int nColumns = table.getColumnCount();
        long nRows = table.getRowCount();
        String tableName = table.getName();

        //Handle the data types:
        List<ColumnInfo> columnInfoList = new ArrayList(nColumns);
        String colName[] = new String[nColumns];
        int[] repeats = new int[nColumns];
        int maxRepeat = 0;
        List<DataType> dataTypeList = new ArrayList();

        for (int col = 0; col < nColumns; col++) {
            ColumnInfo colInfo = table.getColumnInfo(col);
            colName[col] = colInfo.getName();
            columnInfoList.add(colInfo);
            if (colInfo.getShape() != null) {
                repeats[col] = colInfo.getShape()[0];
            } else {
                repeats[col] = 1;
            }
            if ((dataCols == null) || (Arrays.asList(dataCols).contains(colName[col]))){
                if (repeats[col] > maxRepeat) {
                    maxRepeat = repeats[col];
                }
                dataTypeList.add(convertToDataType(colInfo));
            }
        }

        //Build the data objects and put them into the data groups:
        List<DataGroup> dataGroupList = new ArrayList();
        if (strategy.equals(TOP_MOST)) {
            /**
             * "TOP_MOST": Ignore the repeat count portion of the TFORMn Keyword
             * returning only the first datum of the field, even if repeat count is more than 1.
             * This should produce exactly one DataGroup per table. This is the default strategy if not given.
             */

            // One data group per table:
            DataGroup dataGroup = new DataGroup(tableName, dataTypeList);
            for (long row = 0; row < nRows; row++){
                dataGroup = fillDataGroup(table, dataTypeList, maxRepeat, row, dataCols, strategy, dataGroup);
            }

            // Add attributes to dataGroup:
            DataGroup.Attribute attribute;
            for (int col = 0; col < nColumns; col++) {
                if ((headerCols != null) && (Arrays.asList(headerCols).contains(colName[col]))) {
                    ColumnInfo colInfo = columnInfoList.get(col);
                    List attArrayListTotal = new ArrayList();
                    for (long row = 0; row < nRows; row ++) {
                        Object cell = table.getCell(row, col);
                        List attArrayList;
                        attArrayList = getAttArrayList(cell, colInfo);
                        attArrayListTotal.add(attArrayList.get(0));
                    }
                    attribute = new DataGroup.Attribute(colName[col], getAttributeValue(attArrayListTotal));
                    dataGroup.addAttributes(attribute);
                }
            }
            dataGroupList.add(dataGroup);

        } else if (strategy.equals(FULLY_FLATTEN)) {
            /**
             * FULLY_FLATTEN: Generates one DataGroup row for each value of an HDU field.
             * Because each field may have different repeat count (dimension), insert blank
             * when no data is available. This should produce exactly one DataGroup per table.
             */

            // define one whole data group per table:
            DataGroup dataGroup = new DataGroup(tableName, dataTypeList);

            for (long row = 0; row < nRows; row++) {
                // Fill the data into the dataGroup:
                dataGroup = fillDataGroup(table, dataTypeList, maxRepeat, row, dataCols, strategy, dataGroup);
            }

            // Add attributes to dataGroup:
            DataGroup.Attribute attribute;
            for (int col = 0; col < nColumns; col++) {
                if ((headerCols != null) && (Arrays.asList(headerCols).contains(colName[col]))) {
                    ColumnInfo colInfo = columnInfoList.get(col);
                    List attArrayListTotal = new ArrayList();
                    for (int row = 0; row < nRows; row++) {
                        Object cell = table.getCell(row, col);
                        List attArrayList = getAttArrayList(cell, colInfo);
                        attArrayListTotal.addAll(attArrayList);
                    }
                    attribute = new DataGroup.Attribute(colName[col], getAttributeValue(attArrayListTotal));
                    dataGroup.addAttributes(attribute);
                }
            }
            //Only one dataGroup for each table:
            dataGroupList.add(dataGroup);

        } else if ((strategy.equals(EXPAND_BEST_FIT)) || strategy.equals(EXPAND_REPEAT)) {
            /**
             * EXPAND_BEST_FIT: Expands each HDU row into one DataGroup. Fields with lesser count (dimension) will be filled with blanks.
             * EXPAND_REPEAT: Expands each HDU row into one DataGroup. Fields with lesser dimension will be filled with previous values.
             */

            for (int row = 0; row < nRows; row++) {
                // define dataGroup per row:
                String dgTitle = tableName + " Row #: " + row;

                //One data group per row:
                DataGroup dataGroup = new DataGroup(dgTitle, dataTypeList);
                dataGroup = fillDataGroup(table, dataTypeList, maxRepeat, row, dataCols, strategy, dataGroup);

                // Add attributes to dataGroup:
                DataGroup.Attribute attribute;
                for (int col = 0; col < nColumns; col++) {
                    if ((headerCols != null) && (Arrays.asList(headerCols).contains(colName[col]))) {
                        ColumnInfo colInfo = columnInfoList.get(col);
                        Object cell = table.getCell(row, col);
                        List attArrayList = getAttArrayList(cell, colInfo);
                        attribute = new DataGroup.Attribute(colName[col], getAttributeValue(attArrayList));
                        dataGroup.addAttributes(attribute);
                    }
                }
                //one data group per row; add to the list.
                dataGroupList.add(dataGroup);
            }
        }
        return dataGroupList;
    }


    /**
     *
     * @param table
     * @param dataTypeList
     * @param maxRepeat
     * @param row
     * @param dataCols
     * @param strategy
     * @param dataGroup
     * @return
     * @throws IOException
     * @throws FitsException
     */

    private static DataGroup fillDataGroup(StarTable table,
                                           List<DataType> dataTypeList,
                                           int maxRepeat,
                                           long row,
                                           String[] dataCols,
                                           String strategy,
                                           DataGroup dataGroup)
            throws IOException, FitsException {

        if (strategy.equals(TOP_MOST)){
            maxRepeat = 1;
        }

        for (int rpt = 0; rpt < maxRepeat; rpt ++) {
            DataObject dataObj = new DataObject(dataGroup);
            int dataCol = 0;
            for (int col = 0; col < table.getColumnCount(); col++) {
                String colName = table.getColumnInfo(col).getName();
                if ((dataCols == null) || (Arrays.asList(dataCols).contains(colName))) {
                    dataCol ++;
                    ColumnInfo colInfo = table.getColumnInfo(col);
                    String classType = DefaultValueInfo.formatClass(colInfo.getContentClass());
                    String originalType = (String)((DescribedValue)colInfo.getAuxData().get(0)).getValue();
                    Object cell = table.getCell(row, col);

                    Object dataElement = null;
                    if (table.getColumnInfo(col).isArray()) {
                        if (rpt < Array.getLength(cell)) {
                            dataElement = getDataElement(Array.get(cell, rpt), classType, originalType);
                        }
                        else {
                            if (strategy.equals(EXPAND_REPEAT)) {
                                dataElement = getDataElement(Array.get(cell, Array.getLength(cell)-1), classType, originalType);
                            }
                            else if ((strategy.equals(EXPAND_BEST_FIT)) || (strategy.equals(FULLY_FLATTEN)) ) {
                                dataElement = null;
                            }
                        }
                    }
                    else {
                        if (rpt == 0) {
                            dataElement = getDataElement(cell, classType, originalType);
                        }
                        else {
                            if (strategy.equals(EXPAND_REPEAT)) {
                                dataElement = getDataElement(cell, classType, originalType);
                            }
                            else if ((strategy.equals(EXPAND_BEST_FIT)) || (strategy.equals(FULLY_FLATTEN)) ) {
                                dataElement = null;
                            }
                        }
                    }
                    dataObj.setDataElement(dataTypeList.get(dataCol -1), dataElement);
                }
            }
            dataGroup.add(dataObj);
        }
        return dataGroup;
    }

    /**
     *
     * @param obj
     * @param classType
     * @param originalType
     * @return
     */
    private static Object getDataElement(Object obj, String classType, String originalType) {

        Object dataElement = obj;

        if (classType.contains("boolean")){
            if (originalType.contains("L")){
                dataElement = String.valueOf(obj);
            }
            else if (originalType.contains("X")){
                dataElement = (Boolean)obj? 1:0;
            }
        }
        else if (classType.contains("byte") || classType.contains("short") ){
            dataElement = (int)(Integer)obj;
        }

        return dataElement;
    }


    /**
     * To get the attribute data ArrayList, attArrayList, from a cell (a single value or an array). Each element in the arraylist is one datum from the cell.
     * Only convert the data type from boolean to int (if original type is bits) or String (if original type is logical).
     *
     * @param cell: A single value or an array
     * @param colInfo: The column information of the cell
     * @return attArrayList: attribute data
     */
    private static List getAttArrayList(Object cell,
                                                ColumnInfo colInfo)
    throws FitsException {

        String classType = DefaultValueInfo.formatClass(colInfo.getContentClass());
        String originalType = (String)((DescribedValue)colInfo.getAuxData().get(0)).getValue();

        List attArrayList = new ArrayList();

        if (colInfo.isArray()) {

            if ((classType.contains("boolean")) || (classType.contains("Boolean"))){
                boolean[] data = (boolean[])cell;
                for (int repeat = 0; repeat < data.length; repeat ++){
                    if (originalType.contains("L")){
                        //Logical:
                        String dataOut = Boolean.toString(data[repeat]);
                        attArrayList.add(dataOut);
                    }
                    else if (originalType.contains("X")){
                        int dataOut = data[repeat]? 1:0;
                        attArrayList.add(dataOut);
                    }
                }
            }
            else if ((classType.contains("byte")) || (classType.contains("Byte"))) {
                byte[] dataOut = (byte[])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("short")) || (classType.contains("Short"))) {
                short[] dataOut = (short[])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("int")) || (classType.contains("Integer"))) {
                int[] dataOut = (int[])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("long")) || (classType.contains("Long"))) {
                long[] dataOut = (long[])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("float")) || (classType.contains("Float"))) {
                float[] dataOut = (float[])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("double")) || (classType.contains("Double"))) {
                double[] dataOut = (double[])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("char")) || (classType.contains("String"))) {
                String[] dataOut = (String[])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else {
                throw new FitsException(
                        "Unrecognized format character in FITS table file: " + classType);
            }
        } else {
            attArrayList.add(cell);
        }
        return attArrayList;
    }

    /** Get the attribute value (String) from the attArrayList:
     * Concatenate the values in the list and the divider "," to a string and return.
     *
     * @param attArrayList: attribute data list
     * @return attribute value
     */
    private static String getAttributeValue(List attArrayList) throws IOException {

        String value = "";
        int maxRepeat = attArrayList.size()< maxNumAttriValues? attArrayList.size() : maxNumAttriValues;
        for (int repeat = 0; repeat < maxRepeat; repeat ++) {
            value = value.concat(String.valueOf(attArrayList.get(repeat)));
            value = value.concat(",");
        }
        return value;
    }

    /**
     * Convert the column info, provided by StarTable, to dataType.
     * The originalType is the original data type stored in the FITS table. StarTable stores it in the column info.
     * The classType is the data type StarTable converts from the originalType (say Bits -> Boolean).
     * Based on classType and originalType, we set java_class (dataType.setDataType(java_class)):
     *  (1)Boolean/boolean -> Integer if originalType is Bits; Boolean/boolean -> String if originalType is logical
     *  (2)Short/short -> Integer
     *  (3)char -> String
     *  (4)No change for other types
     * Set unit.
     *
     * @param colInfo
     * @return dataType
     * @throws FitsException
     */
    public static DataType convertToDataType (ColumnInfo colInfo)
    throws FitsException{

        String colName = colInfo.getName();
        String classType = DefaultValueInfo.formatClass(colInfo.getContentClass());
        String originalType = (String)((DescribedValue)colInfo.getAuxData().get(0)).getValue();
        String unit = colInfo.getUnitString();

        DataType dataType = new DataType(colName, null);
        Class java_class = null;

        if ((classType.contains("boolean")) || (classType.contains("Boolean"))) {
            if (originalType.contains("L")) {
                //Logical:
                java_class = String.class;
            } else if (originalType.contains("X")) {
                //Bits:
                java_class = Integer.class;
            }
        } else if ((classType.contains("byte")) || (classType.contains("Byte"))) {
            java_class = Integer.class;
        } else if ((classType.contains("short")) || (classType.contains("Short"))) {
            java_class = Integer.class;
        } else if ((classType.contains("int")) || (classType.contains("Integer"))) {
            java_class = Integer.class;
        } else if ((classType.contains("long")) || (classType.contains("Long"))) {
            java_class = Long.class;
        } else if ((classType.contains("float")) || (classType.contains("Float"))) {
            java_class = Float.class;
        } else if ((classType.contains("double")) || (classType.contains("Double"))) {
            java_class = Double.class;
        } else if ((classType.contains("char")) || (classType.contains("String"))) {
            java_class = String.class;
        } else {
            throw new FitsException(
                    "Unrecognized format character in FITS table file: " + classType);
        }
        dataType.setDataType(java_class);
        dataType.setUnits(unit);
        return dataType;
    }

    /**
    * Convert a FITS binary table file on disk to a list of DataGroup
    * @param FITS_filename input_filename
    * @param catName data group title (catName is not used).
    * @deprecated
    */
    public static List<DataGroup> convertFITSToDataGroup(String FITS_filename, String catName)
	throws FitsException, IOException
    {
        return convertFitsToDataGroup(FITS_filename, null, null, TOP_MOST);
    }
}