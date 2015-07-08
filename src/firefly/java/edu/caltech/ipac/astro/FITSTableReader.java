/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro;

import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import uk.ac.starlink.table.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* Convert an Ipac table file to a FITS binary table file 
*/
public final class FITSTableReader
{
    public static boolean debug = true;

    private static final int TABLE_INT = 1;
    private static final int TABLE_DOUBLE = 2;
    private static final int TABLE_FLOAT = 3;
    private static final int TABLE_STRING = 4;
    private static final int TABLE_SHORT = 5;

    private static final int maxNumAttriValues = 80;

    static void usage()
    {
	System.out.println("usage java edu.caltech.ipac.astro.FITSTableReader <FITS_filename> <ipac_filename>");
	System.exit(1);
    }

    public static void main(String[] args)
    {
        if (args.length != 2)
        {
            usage();
        }
        String FITS_filename = args[0];
        String ipac_filename = args[1];


        FITSTableReader fits_to_ipac = new FITSTableReader();


        //for lsst:
        //String[] dataCols = {"flags", "id", "coord ra", "coord_dec", "base_ClassificationExtendedness_value", "base_SdssCentroid_xSigma"};
        //String[] dataCols = {"id", "coord ra", "coord_dec", "base_ClassificationExtendedness_value", "base_SdssCentroid_xSigma"};
        //String[] dataCols = {"flags"};
        //String[] headerCols = {"id", "coord_ra","coord_dec", "parent", "footprint","base_ClassificationExtendedness_value", "base_SdssCentroid_xSigma"};
        //String[] headerCols = { "base_ClassificationExtendedness_value", "base_SdssCentroid_xSigma", "coord_ra","coord_dec", "parent", "footprint"};
        //String[] headerCols ={"flags"};

        //for lsst_cat:
        //String[] dataCols = {"id", "cat.archive", "cat.persistable", "spatialfunctions", "components","name" };
        String[] dataCols = {"id", "name", "kernel", "center_x", "spatialfunctions", "components", "coefficients", "image", "cd", "ctype1", "A", "Ap"};
        //String[] headerCols = {"id", "cat.archive", "cat.persistable", "spatialfunctions", "components", "name"};
        String[] headerCols = {"id", "name", "kernel", "center_x", "spatialfunctions", "components", "coefficients", "image", "cd", "ctype1", "A", "Ap"};

        //String strategy = "EXPAND_BEST_FIT";
        //String strategy = "EXPAND_REPEAT";
        String strategy = "TOP_MOST";
        //String strategy = "FULLY_FLATTEN";

        int whichDG = 3;

        try {
            List<DataGroup> dgListTotal = fits_to_ipac.convertFITSToDataGroup(
                    FITS_filename,
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
            List<DataGroup> dgList = fits_to_ipac.convertFITSToDataGroup(FITS_filename, null);

            File output_file = new File(ipac_filename);
            DataGroup dg = dgList.get(1);
            IpacTableWriter.save(output_file, dg);
        }
        catch (FitsException fe)
        {
            System.out.println("got FitsException: " + fe.getMessage());
            fe.printStackTrace();
        }
        catch (IpacTableException ite)
        {
            System.out.println(ite.getMessage());
        }
        catch (IOException ioe)
        {
            System.out.println("got IOException: " + ioe.getMessage());
            ioe.printStackTrace();
        }

    }

    public static List<DataGroup> convertFITSToDataGroup(String FITS_filename,
                                                         String[] dataCols,
                                                         String[] headerCols,
                                                         String strategy)
        throws FitsException, IOException {


        TableSequence tseq;
        StarTableFactory stFactory = new StarTableFactory();
        List tList = new ArrayList();
        try {
            tseq = stFactory.makeStarTables(FITS_filename, null);
            for (StarTable tbl; (tbl = tseq.nextTable()) != null; ) {
                tList.add(tbl);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        List<DataGroup> dgListTotal = new ArrayList<DataGroup>();
        try {
            //for testing:
            //for (int i = 2; i < 3; i++){
            for (int i = 0; i < tList.size(); i++) {
                StarTable table = (StarTable) tList.get(i);

                List<DataGroup> dgList = convertFITSToDataGroup(table, dataCols, headerCols, strategy);
                dgListTotal.addAll(dgList);
            }
        }
        catch (IOException ioe)
        {
            System.out.println("got IOException: " + ioe.getMessage());
            ioe.printStackTrace();
        }
        return dgListTotal;
    }

    public static List<DataGroup> convertFITSToDataGroup(int index,
                                                         String FITS_filename,
                                                         String[] dataCols,
                                                         String[] headerCols,
                                                         String strategy)
            throws FitsException, TableFormatException, IOException {

        List<DataGroup> dgListTotal = new ArrayList<DataGroup>();


        return dgListTotal;
    }

    /** Convert a binary table hdu (StarTable) into an ipac table.
     * @param table input StarTable from one HDU
     * @param dataCols An array of column names used to construct the ipac table data.
     * @param headerCols An array of column names used to construct the ipac table header.
     * @param strategy A list of strategies used to deal with the repeat count (dimension) of the data in the given dataCols columns.
     * @return List<DataGroup> A list of DataGroups
     */
    public static List<DataGroup> convertFITSToDataGroup(StarTable table,
                                                         String[] dataCols,
                                                         String[] headerCols,
                                                         String strategy)
            throws FitsException, TableFormatException, IllegalArgumentException, IOException {

        if (!(strategy.equals("FULLY_FLATTEN")) && !(strategy.equals("EXPAND_BEST_FIT")) && !(strategy.equals("EXPAND_REPEAT"))){
            strategy = "TOP_MOST";
        }

        int nColumns = table.getColumnCount();
        long nRows = table.getRowCount();
        String tableName = table.getName();

        //Handle the data types:
        List<ColumnInfo> columnInfoList = new ArrayList<ColumnInfo>(nColumns);
        String colName[] = new String[nColumns];
        int[] repeats = new int[nColumns];
        int maxRepeat = 0;
        List<DataType> dataTypeList = new ArrayList<DataType>(dataCols.length);

        for (int col = 0; col < nColumns; col++) {
            ColumnInfo colInfo = table.getColumnInfo(col);
            colName[col] = colInfo.getName();
            columnInfoList.add(colInfo);
            if (colInfo.getShape() != null) {
                repeats[col] = colInfo.getShape()[0];
            } else {
                repeats[col] = 1;
            }
            if (Arrays.asList(dataCols).contains(colName[col])){
                if (repeats[col] > maxRepeat) {
                    maxRepeat = repeats[col];
                }
            }
            if (Arrays.asList(dataCols).contains(colName[col])){
                dataTypeList.add(convertToDataTypeList(colInfo));
            }
        }

        //Build the data objects and put them into the data groups:
        List<DataGroup> dataGroupList = new ArrayList<DataGroup>();
        if (strategy.equals("TOP_MOST")) {
            /**
             * "TOP_MOST": Ignore the repeat count portion of the TFORMn Keyword
             * returning only the first datum of the field, even if repeat count is more than 1.
             * This should produce exactly one DataGroup. This is the default strategy if not given.
             */

            int repeat = 1;

            // One data group per table:
            DataGroup dataGroup = new DataGroup(tableName, dataTypeList);
            for (long row = 0; row < nRows; row++) {
                // Save data into an arrayList and then use:
                List<Object> dataArrayList = new ArrayList<Object>(maxRepeat);
                DataObject dataObj = new DataObject(dataGroup);
                int dataTypeIndex = 0;
                for (int col = 0; col < nColumns; col++) {
                    if (Arrays.asList(dataCols).contains(colName[col])){
                        dataTypeIndex++;
                        ColumnInfo colInfo = columnInfoList.get(col);
                        Object cell = table.getCell(row, col);
                        dataArrayList = getDataArrayList(cell,
                                colInfo,
                                repeat,
                                maxRepeat,
                                strategy,
                                dataArrayList);
                        String type = dataTypeList.get(dataTypeIndex - 1).getDataType().toString();
                        Object data = dataArrayList.get(dataTypeIndex - 1);
                        if (type.contains("Integer")){
                            dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((int [])data)[0]);
                        }
                        else if (type.contains("Long")){
                            dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((long [])data)[0]);
                        }
                        else if (type.contains("Float")){
                            dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((float [])data)[0]);
                        }
                        else if (type.contains("Double")){
                            dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((double [])data)[0]);
                        }
                        else if ((type.contains("String")) || (type.contains("char"))) {
                            dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((String [])data)[0]);
                        }
                        else {
                            throw new FitsException(
                                    "Unrecognized format character in FITS table file: " + type);
                        }
                    }
                }
                dataGroup.add(dataObj);
            }

            // Add attributes to dataGroup:
            DataGroup.Attribute attribute;
            for (int col = 0; col < nColumns; col++) {
                if (Arrays.asList(headerCols).contains(colName[col])) {
                    ColumnInfo colInfo = columnInfoList.get(col);
                    List<Object> attArrayListTotal = new ArrayList<Object>();
                    for (long row = 0; row < nRows; row ++) {
                        Object cell = table.getCell(row, col);
                        List<Object> attArrayList; //= new ArrayList<Object>();
                        attArrayList = getAttArrayList(cell, colInfo);
                        attArrayListTotal.add(attArrayList.get(0));
                    }
                    attribute = new DataGroup.Attribute(colName[col], getAttributeValue(attArrayListTotal));
                    dataGroup.addAttributes(attribute);
                }
            }
            dataGroupList.add(dataGroup);


        } else if (strategy.equals("FULLY_FLATTEN")) {
            /**
             * "FULLY_FLATTEN": Generates one DataGroup row for each value of an HDU field.
             * Because each field may have different repeat count (dimension), insert blank
             * when no data is available. This should produce exactly one DataGroup.
             */

            // define one whole data group per table:
            DataGroup dataGroup = new DataGroup(tableName, dataTypeList);

            //List<Object> dataArrayListTotal = new ArrayList<Object>();
            for (long row = 0; row < nRows; row++) {
                // Save data into an arrayList and then use:
                List<Object> dataArrayList = new ArrayList<Object>(maxRepeat);
                dataArrayList = getDataArrayList(table, row, repeats, maxRepeat, dataCols, strategy, dataArrayList);

                // Fill the data into the dataGroup:
                dataGroup = fillDataToGroup(nColumns, maxRepeat, dataCols, colName, dataArrayList, dataTypeList, dataGroup);

            }

            // Add attributes to dataGroup:
            DataGroup.Attribute attribute;
            for (int col = 0; col < nColumns; col++) {
                if (Arrays.asList(headerCols).contains(colName[col])) {
                    ColumnInfo colInfo = columnInfoList.get(col);
                    List<Object> attArrayListTotal = new ArrayList<Object>();
                    for (int row = 0; row < nRows; row++) {
                        List<Object> attArrayList = new ArrayList<Object>();
                        Object cell = table.getCell(row, col);
                        attArrayList = getAttArrayList(cell, colInfo);
                        attArrayListTotal.addAll(attArrayList);
                    }
                    attribute = new DataGroup.Attribute(colName[col], getAttributeValue(attArrayListTotal));
                    dataGroup.addAttributes(attribute);
                }
            }
            //Only one dataGroup for each table:
            dataGroupList.add(dataGroup);

        } else if ((strategy.equals("EXPAND_BEST_FIT")) || strategy.equals("EXPAND_REPEAT")) {
            /**
             * "EXPAND_BEST_FIT": Expands each HDU row into one DataGroup. Fields with lesser count (dimension) will be filled with blanks.
             * "EXPAND_REPEAT": Expands each HDU row into one DataGroup. Fields with lesser dimension will be filled with previous values.
             */

            for (int row = 0; row < nRows; row++) {
                // define dataGroup per row:
                String dgTitle = tableName + " Row#: " + row;

                //One data group per row:
                DataGroup dataGroup = new DataGroup(dgTitle, dataTypeList);

                // Save data into an arrayList and then use:
                List<Object> dataArrayList = new ArrayList<Object>(maxRepeat);
                dataArrayList = getDataArrayList(table, row, repeats, maxRepeat, dataCols, strategy, dataArrayList);

                // Fill the data into the dataGroup:
                dataGroup = fillDataToGroup(nColumns, maxRepeat, dataCols, colName, dataArrayList, dataTypeList, dataGroup);

                // Add attributes to dataGroup:
                DataGroup.Attribute attribute;
                for (int col = 0; col < nColumns; col++) {
                    if (Arrays.asList(headerCols).contains(colName[col])) {
                        ColumnInfo colInfo = columnInfoList.get(col);
                        List<Object> attArrayList;
                        Object cell = table.getCell(row, col);
                        attArrayList = getAttArrayList(cell, colInfo);
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
     * Fill the dataObj with the dataArralyList and dataTypeList and then add the dataObj to the dataGroup.
     * One dataObj collects the data from all the columns at one repeat.
     * One dataGroup contains dataObjs from all the repeats.
     * @param nColumns
     * @param maxRepeat
     * @param dataCols
     * @param colName
     * @param dataArrayList
     * @param dataTypeList
     * @param dataGroup
     * @return
     * @throws FitsException
     */
    private static DataGroup fillDataToGroup(int nColumns,
                                           int maxRepeat,
                                           String[] dataCols,
                                           String[] colName,
                                           List<Object> dataArrayList,
                                           List<DataType> dataTypeList,
                                           DataGroup dataGroup)
    throws FitsException {

        for (int repeat = 0; repeat < maxRepeat; repeat++) {
            DataObject dataObj = new DataObject(dataGroup);
            int dataTypeIndex = 0;
            for (int col = 0; col < nColumns; col++) {
                if (Arrays.asList(dataCols).contains(colName[col])) {
                    dataTypeIndex++;
                    DataType dataType = dataTypeList.get(dataTypeIndex - 1);
                    String type = dataTypeList.get(dataTypeIndex - 1).getDataType().toString();
                    Object data = dataArrayList.get(dataTypeIndex - 1);
                    dataObj = fillDataObj(repeat, data, type, dataType, dataObj);
                }
            }
            dataGroup.add(dataObj);
        }

        return dataGroup;
    }

    /**
     * Depending on the data type, cast data array into a corresponding primitive type array.
     * Fill the dataObj with the data at [repeat] and the dataType.
     * Will handle complex type later.
     *
     * @param repeat
     * @param data
     * @param type
     * @param dataType
     * @param dataObj
     * @return
     * @throws FitsException
     */
    private static DataObject fillDataObj(int repeat,
                                          Object data,
                                          String type,
                                          DataType dataType,
                                          DataObject dataObj)
    throws FitsException {

        if (type.contains("Integer")){
            dataObj.setDataElement(dataType, ((int [])data)[repeat]);
        }
        else if (type.contains("Long")){
            dataObj.setDataElement(dataType, ((long [])data)[repeat]);
        }
        else if (type.contains("Float")){
            dataObj.setDataElement(dataType, ((float [])data)[repeat]);
        }
        else if (type.contains("Double")){
            dataObj.setDataElement(dataType, ((double [])data)[repeat]);
        }
        else if ((type.contains("String")) || (type.contains("char"))) {
            dataObj.setDataElement(dataType, ((String [])data)[repeat]);
        }
        else {
            throw new FitsException(
                    "Unrecognized format character in FITS table file: " + type);
        }

        return dataObj;
    }

    /**
     * To get dataArrayList which contains the data from one row in the table.
     * Each element in dataArrayList is  the data in the cell[row, col] which could be a singl datum or a data array.
     * Only the columns in the list dataCols will be put in dataArrayList.
     * @param table
     * @param row: the row number
     * @param repeats: Repeats at all the columns.
     * @param maxRepeat: The maximum repeat.
     * @param dataCols: The columns the caller wants to put in the IPAC table data part.
     * @param strategy: "TOP_MOST", "EXPAND_BEST_FIT", "EXPAND_REPEAT", "FULLY_FLATTEN".
     *
     * @return dataArrayList
     */
    private static List<Object> getDataArrayList(StarTable table,
                                                 long row,
                                                 int[] repeats,
                                                 int maxRepeat,
                                                 String[] dataCols,
                                                 String strategy,
                                                 List<Object> dataArrayList)
            throws FitsException, IOException {

        int nColumns = table.getColumnCount();
        for (int col = 0; col < nColumns; col++) {
            ColumnInfo colInfo = table.getColumnInfo(col);
            String colName = colInfo.getName();
            if (Arrays.asList(dataCols).contains(colName)) {
                int repeat = repeats[col];
                Object cell = table.getCell(row, col);
                dataArrayList = getDataArrayList(cell,
                        colInfo,
                        repeat,
                        maxRepeat,
                        strategy,
                        dataArrayList);
            }
        }
        return dataArrayList;
    }

    /**
     * To get dataArrayList: If the dataArrayList has content, add the cell data to it. If the dataArrayList is empty, make a new one and add the cell data to it.
     * Cast the cell to a primitive data array, data[], based on the class type of the cell.
     * Convert data[repeat] to dataOut[maxRepeat]: fill the missing data with null or the last value, based on the strategy.
     * Convert data type to dataOut type: short or Short to dataOut int[], char or String to dataOut String[].
     * Since StarTable package converts bits type to boolean, we need to find out the original type for the boolean inputs:
     *          if the original type was bits, convert boolean to integer, 0 or 1; if it was logical, convert boolean to String 'true' or 'false'.
     * (remaining issue: how to fill null if the data is int[] or long[]?  and else for strategy?)
     *
     * @param cell the data at the row and the column
     * @param colInfo the column info at the col.
     * @param repeat
     * @param maxRepeat
     * @param strategy
     * @param dataArrayList
     * @return dataArrayList
     */
    private static List<Object> getDataArrayList(Object cell,
                                         ColumnInfo colInfo,
                                         int repeat,
                                         int maxRepeat,
                                         String strategy,
                                         List<Object> dataArrayList)
    throws FitsException {

        boolean isCellArray = colInfo.isArray();
        String classType = DefaultValueInfo.formatClass(colInfo.getContentClass());
        String originalType = (String)((DescribedValue)colInfo.getAuxData().get(0)).getValue();
        dataArrayList = (dataArrayList.size() ==0)?  new ArrayList<Object>() : dataArrayList;

        if ((classType.contains("boolean")) || (classType.contains("Boolean"))){
            boolean [] data = new boolean[repeat];
            if (isCellArray) {
                data = (boolean [])cell;
            }
            else {
                data[0] = (Boolean) cell;
            }

            if (originalType.contains("L")){
                //Logical:
                String [] dataOut = new String[maxRepeat];
                for (int rpt = 0; rpt < maxRepeat; rpt ++){
                    if (rpt < data.length) {
                        dataOut[rpt] = Boolean.toString(data[rpt]);
                    }
                    else {
                        if (strategy.equals("EXPAND_REPEAT")) {
                            dataOut[rpt] = Boolean.toString(data[data.length -1 ]);
                        }
                        else if (strategy.equals("EXPAND_BEST_FIT")){
                            dataOut[rpt] = null;
                        }
                        else {
                            //dataOut[rpt] = null;
                        }
                    }
                }
                dataArrayList.add(dataOut);
            }
            else if (originalType.contains("X")){
                //Bits:
                int[] dataOut = new int[maxRepeat];
                for (int rpt = 0; rpt < maxRepeat; rpt ++){
                    if (rpt < data.length) {
                        dataOut[rpt] = data[rpt] ? 1 : 0;
                    }
                    else{
                        if (strategy.equals("EXPAND_REPEAT")) {
                            dataOut[rpt] = data[data.length - 1] ? 1:0;
                        }
                        else if (strategy.equals("EXPAND_BEST_FIT")){
                            dataOut[rpt] = Integer.parseInt(null);
                        }
                        else {
                            //
                        }
                    }
                }
                dataArrayList.add(dataOut);
            }
        }
        else if ((classType.contains("short")) || (classType.contains("Short"))) {
            int [] data = new int[repeat];
            if (isCellArray) {
                data = (int[])cell;
            }
            else {
                data[0] = (Integer) cell;
            }
            int[] dataOut = new int[maxRepeat];
            for (int rpt = 0; rpt < maxRepeat; rpt++){
                if (rpt < data.length) {
                    dataOut[rpt] = data[rpt];
                }
                else {
                    if (strategy.equals("EXPAND_REPEAT")) {
                        dataOut[rpt] = data[data.length - 1];
                    }
                    else if (strategy.equals("EXPAND_BEST_FIT")){
                        dataOut[rpt] = Integer.MIN_VALUE;
                    }
                    else {
                        //
                    }
                }
            }
            dataArrayList.add(dataOut);
        }
        else if ((classType.contains("int")) || (classType.contains("Integer"))) {
            int [] data = new int[repeat];
            if (isCellArray) {
                data = (int[])cell;
            }
            else {
                data[0] = (Integer) cell;
            }
            int[] dataOut = new int[maxRepeat];
            for (int rpt = 0; rpt < maxRepeat; rpt++){
                if (rpt < data.length) {
                    dataOut[rpt] = data[rpt];
                }
                else {
                    if (strategy.equals("EXPAND_REPEAT")) {
                        dataOut[rpt] = data[data.length - 1];
                    }
                    else if (strategy.equals("EXPAND_BEST_FIT")){
                        dataOut[rpt] = Integer.MIN_VALUE;
                    }
                    else {
                        //
                    }
                }
            }
            dataArrayList.add(dataOut);
        }
        else if ((classType.contains("long")) || (classType.contains("Long"))) {
            long [] data = new long[repeat];
            if (isCellArray) {
                data = (long[])cell;
            }
            else {
                data[0] = (Long) cell;
            }
            long[] dataOut = new long[maxRepeat];
            for (int rpt = 0; rpt < maxRepeat; rpt++){
                if (rpt < data.length) {
                    dataOut[rpt] = data[rpt];
                }
                else {
                    if (strategy.equals("EXPAND_REPEAT")) {
                        dataOut[rpt] = data[data.length - 1];
                    }
                    else if (strategy.equals("EXPAND_BEST_FIT")){
                        dataOut[rpt] = Long.MIN_VALUE;
                    }
                    else {
                        //
                    }
                }
            }
            dataArrayList.add(dataOut);
        }
        else if ((classType.contains("float")) || (classType.contains("Float"))) {
            float [] data = new float[repeat];
            if (isCellArray) {
                data = (float[])cell;
            }
            else {
                data[0] = (Float) cell;
            }
            float[] dataOut = new float[maxRepeat];
            for (int rpt = 0; rpt < maxRepeat; rpt++){
                if (rpt < data.length) {
                    dataOut[rpt] = data[rpt];
                }
                else {
                    if (strategy.equals("EXPAND_REPEAT")) {
                        dataOut[rpt] = data[data.length - 1];
                    }
                    else if (strategy.equals("EXPAND_BEST_FIT")){
                        dataOut[rpt] = Float.NaN;
                    }
                    else {
                        //
                    }
                }
            }
            dataArrayList.add(dataOut);
        }
        else if ((classType.contains("double")) || (classType.contains("Double"))) {
            double [] data = new double[repeat];
            if (isCellArray) {
                data = (double[])cell;
            }
            else {
                data[0] = (Double) cell;
            }
            double[] dataOut = new double[maxRepeat];
            for (int rpt = 0; rpt < maxRepeat; rpt++){
                if (rpt < data.length) {
                    dataOut[rpt] = data[rpt];
                }
                else {
                    if (strategy.equals("EXPAND_REPEAT")) {
                        dataOut[rpt] = data[data.length - 1];
                    }
                    else if (strategy.equals("EXPAND_BEST_FIT")){
                        dataOut[rpt] = Double.NaN;
                    }
                    else {
                        //
                    }
                }
            }
            dataArrayList.add(dataOut);
        }
        else if ((classType.contains("char")) || (classType.contains("String"))) {
            String[] data = new String[repeat];
            if (isCellArray) {
                data = (String[])cell;
            }
            else {
                data[0] = (String)cell;
            }
            String[] dataOut = new String[maxRepeat];
            for (int rpt = 0; rpt < maxRepeat; rpt++){
                if (rpt < data.length) {
                    dataOut[rpt] = data[rpt];
                }
                else {
                    if (strategy.equals("EXPAND_REPEAT")) {
                        dataOut[rpt] = data[data.length - 1];
                    }
                    else if (strategy.equals("EXPAND_BEST_FIT")){
                        dataOut[rpt] = null;
                    }
                    else {
                        //
                    }
                }
            }
            dataArrayList.add(dataOut);
        }
        else {
            throw new FitsException(
                    "Unrecognized format character in FITS table file: " + classType);
        }
        return dataArrayList;
    }

    /**
     * To get the attribute data ArrayList, attArrayList, from a cell. Each element is one datum in the cell.
     * Cast the cell to a primitive data array, dataOut[], based on the class type of the cell.
     * Convert the type from the cell to dataOut:
     *      short or Short to dataOut int[];
     *      char or String to dataOut String[].
     *      Since StarTable package converts FITS bits type to boolean, we need to convert
     *          boolean to int (1 or 0) if original is bits;
     *          boolean to to String ('true' or 'false') if original is logical.
     *
     * @param cell: Input cell data
     * @param colInfo: column information
     * @return attArrayList: attribute data
     */
    private static List<Object> getAttArrayList(Object cell,
                                                ColumnInfo colInfo)
    throws FitsException {

        String classType = DefaultValueInfo.formatClass(colInfo.getContentClass());
        String originalType = (String)((DescribedValue)colInfo.getAuxData().get(0)).getValue();

        List<Object> attArrayList = new ArrayList<Object>();

        if (colInfo.isArray()) {

            if ((classType.contains("boolean")) || (classType.contains("boolean"))){
                boolean[] data = (boolean [])cell;
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
            else if ((classType.contains("short")) || (classType.contains("Short"))) {
                int[] dataOut = (int [])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("int")) || (classType.contains("Integer"))) {
                int[] dataOut = (int [])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("long")) || (classType.contains("Long"))) {
                long[] dataOut = (long [])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("float")) || (classType.contains("Float"))) {
                int[] dataOut = (int [])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("double")) || (classType.contains("Double"))) {
                double[] dataOut = (double [])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("char")) || (classType.contains("String"))) {
                String[] dataOut = (String [])cell;
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
     * Concatenate the numbers in the list and the divider "," to a string, value, and return.
     *
     * @param attArrayList: attribute data list
     * @return attribute value
     */
    private static String getAttributeValue(List<Object> attArrayList) throws IOException {

        String value = "";
        //for (int repeat = 0; repeat < cell.length; repeat ++){
        int maxRepeat = attArrayList.size()< maxNumAttriValues? attArrayList.size() : maxNumAttriValues;
        for (int repeat = 0; repeat < maxRepeat; repeat ++) {
            value = value.concat(String.valueOf(attArrayList.get(repeat)));
            value = value.concat(",");
        }
        return value;
    }

    /**
     *
     * @param colInfo
     * @return dataType
     * @throws FitsException
     */
    public static DataType convertToDataTypeList (ColumnInfo colInfo)
    throws FitsException{

        String colName = colInfo.getName();
        String classType = DefaultValueInfo.formatClass(colInfo.getContentClass());
        String originalType = (String)((DescribedValue)colInfo.getAuxData().get(0)).getValue();
        String unit = colInfo.getUnitString();

        DataType dataType = new DataType(colName, null);
        Class java_class = null;
        String primitive_type = null;

        if ((classType.contains("boolean")) || (classType.contains("Boolean"))) {
            if (originalType.contains("L")) {
                //Logical:
                java_class = String.class;
                primitive_type = "char";
            } else if (originalType.contains("X")) {
                //Bits:
                java_class = Integer.class;
                primitive_type = "int";
            }
        } else if ((classType.contains("byte")) || (classType.contains("Byte"))) {
            java_class = Integer.class;
            primitive_type = "int";
        } else if ((classType.contains("short")) || (classType.contains("Short"))) {
            java_class = Integer.class;
            primitive_type = "int";
        } else if ((classType.contains("int")) || (classType.contains("Integer"))) {
            java_class = Integer.class;
            primitive_type = "int";
        } else if ((classType.contains("long")) || (classType.contains("Long"))) {
            java_class = Long.class;
            primitive_type = "long";
        } else if ((classType.contains("float")) || (classType.contains("Float"))) {
            java_class = Float.class;
            primitive_type = "float";
        } else if ((classType.contains("double")) || (classType.contains("Double"))) {
            java_class = Double.class;
            primitive_type = "double";
        } else if ((classType.contains("String")) || (classType.contains("char"))) {
            java_class = String.class;
            primitive_type = "char";
        } else {
            throw new FitsException(
                    "Unrecognized format character in FITS table file: " + classType);
        }
        dataType.setDataType(java_class);
        dataType.setTypeDesc(primitive_type);
        dataType.setUnits(unit);
        return dataType;
    }


    /**
    * Convert a FITS binary table file on disk to a list of DataGroup
    * @param FITS_filename input_filename
    * @param catName data group title
    */
    public static List<DataGroup> convertFITSToDataGroup(String FITS_filename,
	String catName)
	throws FitsException, IOException, IpacTableException
    {

	DataGroup   _dataGroup;
	List<DataGroup> _dataGroupList = new ArrayList<DataGroup>();

	Fits fits_file = new Fits(FITS_filename);
	while (true)
	{
	BasicHDU current_hdu = fits_file.readHDU();
	if (current_hdu == null)
	{
	    break;
	}
	if (current_hdu instanceof BinaryTableHDU)
	{
	    BinaryTableHDU bhdu = (BinaryTableHDU) current_hdu;

	    int nrows = bhdu.getNRows();
	    //System.out.println("nrows = " + nrows);
	    int ncolumns = bhdu.getNCols();
	    //System.out.println("getNCols() = " + ncolumns);
	    int format[] = new int[ncolumns];
	    List<DataType> extraDataList = new ArrayList<DataType>(ncolumns);
	    DataType extraData[];
	    DataType dataType= null;
	    String primitive_type;
	    Class java_class;
	    int width = 0;
	    for (int i = 0; i < ncolumns; i++)
	    {
		String table_column_format = bhdu.getColumnFormat(i);
		String table_column_name = bhdu.getColumnName(i);
		DataType.FormatInfo.Align data_align;
		//System.out.println("Column " + i + ":  format = " +
		//    table_column_format + " Name = " + table_column_name); 
		if (table_column_format.contains("A"))
		{
		    format[i] = TABLE_STRING;
		    java_class = String.class;
		    primitive_type = "char";
		    String string_width = table_column_format.substring(
			 0, table_column_format.indexOf('A'));
		    //System.out.println("width string = [" + string_width + "]");
		    width = Integer.valueOf(string_width);
		    data_align = DataType.FormatInfo.Align.LEFT;

		}
		else if (table_column_format.contains("J"))
		{
		    format[i] = TABLE_INT;
		    java_class = Integer.class;
		    primitive_type = "int";
		    width = 10;
		    data_align = DataType.FormatInfo.Align.RIGHT;
		}
		else if (table_column_format.contains("I"))
		{
		    format[i] = TABLE_SHORT;
		    java_class = Short.class;
		    primitive_type = "short";
		    width = 10;
		    data_align = DataType.FormatInfo.Align.RIGHT;
		}
		else if (table_column_format.contains("D")) 
		{
		    format[i] = TABLE_DOUBLE;
		    java_class = Double.class;
		    primitive_type = "double";
		    width = 10;
		    data_align = DataType.FormatInfo.Align.RIGHT;
		}
		else if (table_column_format.contains("E"))
		{
		    format[i] = TABLE_FLOAT;
		    java_class = Float.class;
		    primitive_type = "float";
		    width = 10;
		    data_align = DataType.FormatInfo.Align.RIGHT;
		}
		else
		{
		    throw new FitsException(
		    "Unrecognized format character in FITS table file: " + 
		    format[i]);
		}
		if (width < table_column_name.length())
		{
		    width = table_column_name.length();
		}
		dataType= new DataType(table_column_name, java_class);
		dataType.setTypeDesc(primitive_type);
		DataType.FormatInfo fi = new DataType.FormatInfo(width + 1);
		fi.setDataAlign(data_align);
		//String format_string = "%s";
		//fi.setDataFormat(format_string);
		dataType.setFormatInfo(fi);
		extraDataList.add(dataType);
	    }
	    extraData = extraDataList.toArray(new DataType[extraDataList.size()]);
	    _dataGroup= new DataGroup(catName, extraData);

	    int int_value;
	    double double_value;
	    float float_value;
	    String string_value;
	    short short_value;
	    DataObject dataObj = null;
	    for (int row_number = 0; row_number < nrows; row_number++)
	    {
		//System.out.println("Starting row " + row_number );
		Object[] row = bhdu.getRow(row_number);
		dataObj = new DataObject(_dataGroup);
	    for (int i = 0; i < row.length; i++)
	    {
		//System.out.print("Starting column " + i + ":  " );

		switch(format[i])
		{
		    case TABLE_INT:
			int int_array[] = (int[])(row[i]);

			int_value = int_array[0];
			//System.out.println("got int value : " + int_value);
			Integer java_int = new Integer(int_value);
			dataObj.setDataElement(extraData[i], java_int);
			break;
		    case TABLE_SHORT:
			short short_array[] = (short[])(row[i]);
			short_value = short_array[0];
			//System.out.println("got short value : " + short_value);
			Short java_short = new Short(short_value);
			dataObj.setDataElement(extraData[i], java_short);
			break;
		    case TABLE_STRING:
			string_value = (String)row[i];
			//System.out.println("got string value : " + string_value);
			dataObj.setDataElement(extraData[i], string_value);
			break;
		    case TABLE_DOUBLE:
			double double_array[] = (double[])(row[i]);
			double_value = double_array[0];
			//System.out.println("got double value : "+ double_value);
			Double v = new Double(double_value);
			dataObj.setDataElement(extraData[i], v);
			break;
		    case TABLE_FLOAT:
			float float_array[] = (float[])(row[i]);
			float_value = float_array[0];
			//System.out.println("got float value : "+ float_value);
			Float vfloat = new Float(float_value);
			dataObj.setDataElement(extraData[i], vfloat);
			break;
		}
	    }
            _dataGroup.add(dataObj);
	    }
	    _dataGroupList.add(_dataGroup);
	}
	}
	return _dataGroupList;
    }
}

