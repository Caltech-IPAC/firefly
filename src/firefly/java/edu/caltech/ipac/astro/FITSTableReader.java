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
        String[] dataCols = {"name", "kernel", "components", "coefficients", "image", "ctype1", "A", "Ap"};
        //String[] headerCols = {"id", "cat.archive", "cat.persistable", "spatialfunctions", "components", "name"};
        String[] headerCols = {"name", "kernel", "components", "coefficients", "image", "ctype1", "A", "Ap"};

        //String strategy = "EXPAND_BEST_FIT";
        //String strategy = "EXPAND_REPEAT";
        //String strategy = "TOP_MOST";
        String strategy = "FULLY_FLATTEN";



        try {
            List<DataGroup> dgListTotal = fits_to_ipac.convertFITSToDataGroup(
                    FITS_filename,
                    dataCols,
                    headerCols,
                    strategy);

            File output_file = new File(ipac_filename);
            DataGroup dg = dgListTotal.get(0);
            IpacTableWriter.save(output_file, dg);
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
        throws TableFormatException, IOException {


        TableSequence tseq = null;
        StarTableFactory stFactory = new StarTableFactory();
        List tList = new ArrayList();
        try {
            tseq = stFactory.makeStarTables(FITS_filename, null);
            for (StarTable tbl; (tbl = tseq.nextTable()) != null; ) {
                tList.add(tbl);
            }
        } catch (TableFormatException te){
            te.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        List<DataGroup> dgListTotal = new ArrayList<DataGroup>();
        try {
            //for testing:
            //for (int i = 6; i < 7; i ++) {
            for (int i = 0; i < tList.size(); i++) {
                StarTable table = (StarTable) tList.get(i);

                List<DataGroup> dgList = convertFITSToDataGroup(table, dataCols, headerCols, strategy);
                dgListTotal.addAll(dgList);
            }
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
            throws FitsException, IOException, IpacTableException {

        int nColumns = table.getColumnCount();
        long nRows = table.getRowCount();
        String tableName = table.getName();

        //Handle the data types:

        List<ColumnInfo> columnInfoList = new ArrayList<ColumnInfo>(nColumns);

        String colName[] = new String[nColumns];
        String[] classType = new String[nColumns];
        String [] originalType = new String[nColumns];
        int[] repeats = new int[nColumns];
        int maxRepeat = 0;
        boolean[] isCellArray = new boolean[nColumns];

        for (int col = 0; col < nColumns; col++) {
            ColumnInfo colInfo = table.getColumnInfo(col);
            colName[col] = colInfo.getName();
            classType[col] = DefaultValueInfo.formatClass(colInfo.getContentClass());
            originalType[col] = (String)((DescribedValue)colInfo.getAuxData().get(0)).getValue();
            isCellArray[col] = colInfo.isArray();
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
        }

        List<DataType> dataTypeList = new ArrayList<DataType>(dataCols.length);
        dataTypeList = getDataTypeList(colName, classType, originalType, dataCols);

        List<DataGroup> dataGroupList = new ArrayList<DataGroup>();

        if (strategy.equals("TOP_MOST")) {
            // One data group per table:
            DataGroup dataGroup = new DataGroup(tableName, dataTypeList);
            for (long row = 0; row < nRows; row++) {
                // Save data into an arrayList and then use:
                List<Object> dataArrayList = new ArrayList<Object>(maxRepeat);
                for (int col = 0; col < nColumns; col++) {
                    Object cell = table.getCell(row, col);
                    if (Arrays.asList(dataCols).contains(colName[col])){
                        getDataArrayList(cell,
                                isCellArray[col],
                                classType[col],
                                originalType[col],
                                1,
                                maxRepeat,
                                strategy,
                                dataArrayList);
                    }
                }

                // Add dataObj/row to the dataGroup:
                DataObject dataObj = new DataObject(dataGroup);
                int dataTypeIndex = 0;
                for (int col = 0; col < nColumns; col++) {
                    if (Arrays.asList(dataCols).contains(colName[col])) {
                        dataTypeIndex++;
                        String type = dataTypeList.get(dataTypeIndex - 1).toString();
                        Object value = dataArrayList.get(dataTypeIndex - 1);
                        if (type.contains("Integer")){
                            dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((int [])value)[0]);
                        }
                        else if (type.contains("Long")){
                            dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((long [])value)[0]);
                        }
                        else if (type.contains("Float")){
                            dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((float [])value)[0]);
                        }
                        else if (type.contains("Double")){
                            dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((double [])value)[0]);
                        }
                        else if ((type.contains("String")) | (type.contains("Character"))) {
                            dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((String [])value)[0]);
                        }
                    }
                }
                dataGroup.add(dataObj);
            }

            // Add attributes to dataGroup:
            DataGroup.Attribute attribute;
            for (int col = 0; col < nColumns; col++) {
                if (Arrays.asList(headerCols).contains(colName[col])) {
                    List<Object> attArrayListTotal = new ArrayList<Object>();
                    for (long row = 0; row < nRows; row ++) {
                        Object cell = table.getCell(row, col);
                        List<Object> attArrayList = new ArrayList<Object>();
                        attArrayList = getAttArrayList(cell, isCellArray[col], originalType[col], classType[col]);
                        attArrayListTotal.add(attArrayList.get(0));
                    }
                    attribute = new DataGroup.Attribute(colName[col], getAttributeValue(attArrayListTotal));

                    dataGroup.addAttributes(attribute);
                }
            }
            dataGroupList.add(dataGroup);

        } else if (strategy.equals("FULLY_FLATTEN")) {

            // define one whole data group per table:
            DataGroup dataGroup = new DataGroup(tableName, dataTypeList);

            List<Object> dataArrayListTotal = new ArrayList<Object>();
            for (long row = 0; row < nRows; row++) {
                // Save data into an arrayList and then use:
                List<Object> dataArrayList = new ArrayList<Object>(maxRepeat);
                for (int col = 0; col < nColumns; col++) {
                    Object cell = table.getCell(row, col);
                    if (Arrays.asList(dataCols).contains(colName[col])) {
                        getDataArrayList(cell,
                                isCellArray[col],
                                classType[col],
                                originalType[col],
                                repeats[col],
                                maxRepeat,
                                strategy,
                                dataArrayList);
                    }
                    dataArrayListTotal.addAll(dataArrayList);
                }
            }

            for (long row = 0; row < nRows; row++) {
                // Add dataObj per repeat to the dataGroup:
                for (int repeat = 0; repeat < maxRepeat; repeat++) {
                    DataObject dataObj = new DataObject(dataGroup);
                    int dataTypeIndex = 0;
                    for (int col = 0; col < nColumns; col++) {
                        //for (Object o : dataArrayList){
                        if (Arrays.asList(dataCols).contains(colName[col])) {
                            dataTypeIndex++;
                            String type = dataTypeList.get(dataTypeIndex - 1).toString();
                            Object value = dataArrayListTotal.get(dataTypeIndex - 1);
                            if (type.contains("Integer")){
                                dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((int [])value)[repeat]);
                            }
                            else if (type.contains("Long")){
                                dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((long [])value)[repeat]);
                            }
                            else if (type.contains("Float")){
                                dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((float [])value)[repeat]);
                            }
                            else if (type.contains("Double")){
                                dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((double [])value)[repeat]);
                            }
                            else if ((type.contains("String")) | (type.contains("Character"))) {
                                dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((String [])value)[repeat]);
                            }
                            //dataGroup.add(dataObj);
                        }
                    }
                    dataGroup.add(dataObj);
                }
            }

            // Add attributes to dataGroup:
            DataGroup.Attribute attribute;
            for (int col = 0; col < nColumns; col++) {
                if (Arrays.asList(headerCols).contains(colName[col])) {
                    List<Object> attArrayListTotal = new ArrayList<Object>();
                    for (int row = 0; row < nRows; row++) {
                        List<Object> attArrayList = new ArrayList<Object>();
                        Object cell = table.getCell(row, col);
                        attArrayList = getAttArrayList(cell, isCellArray[col], originalType[col], classType[col]);
                        attArrayListTotal.addAll(attArrayList);
                    }
                    attribute = new DataGroup.Attribute(colName[col], getAttributeValue(attArrayListTotal));
                    dataGroup.addAttributes(attribute);
                }
            }

            dataGroupList.add(dataGroup);

        } else {

            for (int row = 0; row < nRows; row++) {

                // define dataGroup per row:
                String dgTitle = tableName + " Row#: " + row;

                //One data group per row:
                DataGroup dataGroup = new DataGroup(dgTitle, dataTypeList);

                // Save data into an arrayList and then use:
                List<Object> dataArrayList = new ArrayList<Object>(maxRepeat);
                for (int col = 0; col < nColumns; col++) {
                    Object cell = table.getCell(row, col);
                    if (Arrays.asList(dataCols).contains(colName[col])){
                        getDataArrayList(cell,
                                isCellArray[col],
                                classType[col],
                                originalType[col],
                                repeats[col],
                                maxRepeat,
                                strategy,
                                dataArrayList);
                    }
                }
                // Add dataObj per repeat to the dataGroup:
                for (int repeat = 0; repeat < maxRepeat; repeat++) {
                    DataObject dataObj = new DataObject(dataGroup);
                    int dataTypeIndex = 0;
                    for (int col = 0; col < nColumns; col++) {
                        //for (Object o : dataArrayList){
                        if (Arrays.asList(dataCols).contains(colName[col])) {
                            dataTypeIndex++;
                            String type = dataTypeList.get(dataTypeIndex - 1).toString();
                            Object value = dataArrayList.get(dataTypeIndex - 1);
                            if (type.contains("Integer")){
                                dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((int [])value)[repeat]);
                            }
                            else if (type.contains("Long")){
                                dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((long [])value)[repeat]);
                            }
                            else if (type.contains("Float")){
                                dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((float [])value)[repeat]);
                            }
                            else if (type.contains("Double")){
                                dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((double [])value)[repeat]);
                            }
                            else if ((type.contains("String")) | (type.contains("Character"))) {
                                dataObj.setDataElement(dataTypeList.get(dataTypeIndex - 1), ((String [])value)[repeat]);
                            }
                        }
                    }
                    dataGroup.add(dataObj);
                }
                // Add attributes to dataGroup:
                DataGroup.Attribute attribute;
                for (int col = 0; col < nColumns; col++) {
                    if (Arrays.asList(headerCols).contains(colName[col])) {
                        List<Object> attArrayList = new ArrayList<Object>(repeats[col]);
                        Object cell = table.getCell(row, col);
                        attArrayList = getAttArrayList(cell, isCellArray[col], originalType[col], classType[col]);
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
     * @param cell
     * @param isCellArray
     * @param classType
     * @param originalType
     * @param repeats
     * @param maxRepeat
     * @param strategy
     * @return dataArrayList
     */
    private static void getDataArrayList(Object cell,
                                                  boolean isCellArray,
                                                  String classType,
                                                  String originalType,
                                                  int repeats,
                                                  int maxRepeat,
                                                  String strategy,
                                                  List<Object> dataArrayList)
    throws FitsException {

        //List<Object> dataArrayList = new ArrayList<Object>();

        if ((classType.contains("boolean")) | (classType.contains("Boolean"))){
            boolean [] data = new boolean[repeats];
            if (isCellArray) {
                data = (boolean [])cell;
            }
            else {
                data[0] = (Boolean) cell;
            }

            if (originalType.contains("L")){
                //Logical:
                String [] dataOut = new String[maxRepeat];
                for (int repeat = 0; repeat < maxRepeat; repeat ++){
                    if (repeat < data.length) {
                        dataOut[repeat] = Boolean.toString(data[repeat]);
                    }
                    else {
                        if (strategy.equals("EXPAND_REPEAT")) {
                            dataOut[repeat] = Boolean.toString(data[data.length -1 ]);
                        }
                        else {
                            dataOut[repeat] = null;
                        }
                    }
                }
                dataArrayList.add(dataOut);
            }
            else if (originalType.contains("X")){
                //Bits:
                int[] dataOut = new int[maxRepeat];
                for (int repeat = 0; repeat < maxRepeat; repeat ++){
                    if (repeat < data.length) {
                        dataOut[repeat] = data[repeat] ? 1 : 0;
                    }
                    else{
                        if (strategy.equals("EXPAND_REPEAT")) {
                            dataOut[repeat] = data[data.length - 1] ? 1:0;
                        }
                        else {
                            dataOut[repeat] = Integer.parseInt(null);
                        }
                    }
                }
                dataArrayList.add(dataOut);
            }
        }
        else if ((classType.contains("short")) | (classType.contains("Short"))) {
            int [] data = new int[repeats];
            if (isCellArray) {
                data = (int[])cell;
            }
            else {
                data[0] = (Integer) cell;
            }
            int[] dataOut = new int[maxRepeat];
            for (int repeat = 0; repeat < maxRepeat; repeat++){
                if (repeat < data.length) {
                    dataOut[repeat] = data[repeat];
                }
                else {
                    if (strategy.equals("EXPAND_REPEAT")) {
                        dataOut[repeat] = data[data.length - 1];
                    }
                    else {
                        dataOut[repeat] = Integer.MIN_VALUE;
                    }
                }
            }
            dataArrayList.add(dataOut);
        }
        else if ((classType.contains("int")) | (classType.contains("Integer"))) {
            int [] data = new int[repeats];
            if (isCellArray) {
                data = (int[])cell;;
            }
            else {
                data[0] = (Integer) cell;
            }
            int[] dataOut = new int[maxRepeat];
            for (int repeat = 0; repeat < maxRepeat; repeat++){
                if (repeat < data.length) {
                    dataOut[repeat] = data[repeat];
                }
                else {
                    if (strategy.equals("EXPAND_REPEAT")) {
                        dataOut[repeat] = data[data.length - 1];
                    }
                    else {
                        dataOut[repeat] = Integer.MIN_VALUE;
                    }
                }
            }
            dataArrayList.add(dataOut);
        }
        else if ((classType.contains("long")) | (classType.contains("Long"))) {
            long [] data = new long[repeats];
            if (isCellArray) {
                data = (long[])cell;;
            }
            else {
                data[0] = (Long) cell;
            }
            long[] dataOut = new long[maxRepeat];
            for (int repeat = 0; repeat < maxRepeat; repeat++){
                if (repeat < data.length) {
                    dataOut[repeat] = data[repeat];
                }
                else {
                    if (strategy.equals("EXPAND_REPEAT")) {
                        dataOut[repeat] = data[data.length - 1];
                    }
                    else {
                        dataOut[repeat] = Long.MIN_VALUE;
                    }
                }
            }
            dataArrayList.add(dataOut);
        }
        else if ((classType.contains("float")) | (classType.contains("Float"))) {
            float [] data = new float[repeats];
            if (isCellArray) {
                data = (float[])cell;;
            }
            else {
                data[0] = (Float) cell;
            }
            float[] dataOut = new float[maxRepeat];
            for (int repeat = 0; repeat < maxRepeat; repeat++){
                if (repeat < data.length) {
                    dataOut[repeat] = data[repeat];
                }
                else {
                    if (strategy.equals("EXPAND_REPEAT")) {
                        dataOut[repeat] = data[data.length - 1];
                    }
                    else {
                        dataOut[repeat] = Float.NaN;
                    }
                }
            }
            dataArrayList.add(dataOut);
        }
        else if ((classType.contains("double")) | (classType.contains("Double"))) {
            double [] data = new double[repeats];
            if (isCellArray) {
                data = (double[])cell;;
            }
            else {
                data[0] = (Double) cell;
            }
            double[] dataOut = new double[maxRepeat];
            for (int repeat = 0; repeat < maxRepeat; repeat++){
                if (repeat < data.length) {
                    dataOut[repeat] = data[repeat];
                }
                else {
                    if (strategy.equals("EXPAND_REPEAT")) {
                        dataOut[repeat] = data[data.length - 1];
                    }
                    else {
                        dataOut[repeat] = Double.NaN;
                    }
                }
            }
            dataArrayList.add(dataOut);
        }
        else if ((classType.contains("char")) | (classType.contains("String"))) {
            //char [] data = new char[repeats];
            String[] data = new String[repeats];
            if (isCellArray) {
                data = (String[])cell;
            }
            else {
                data[0] = (String)cell;
            }
            String[] dataOut = new String[maxRepeat];
            for (int repeat = 0; repeat < maxRepeat; repeat++){
                if (repeat < data.length) {
                    dataOut[repeat] = data[repeat];
                }
                else {
                    if (strategy.equals("EXPAND_REPEAT")) {
                        dataOut[repeat] = data[data.length - 1];
                    }
                    else {
                        dataOut[repeat] = null;
                    }
                }
            }
            dataArrayList.add(dataOut);
        }
        //return dataArrayList;
    }

    /** Get attribute data array from a cell.
     * @param cell: Input cell data
     * @param isCellArray: if the cell is an array
     * @param originalType
     * @param classType: The class type of this cell
     * @return attArrayList: attribute data
     */
    private static List<Object> getAttArrayList(Object cell,
                                                boolean isCellArray,
                                                String originalType,
                                                String classType){

        List<Object> attArrayList = new ArrayList<Object>();

        if (isCellArray) {

            if ((classType.contains("boolean")) | (classType.contains("boolean"))){
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
            else if ((classType.contains("short")) | (classType.contains("Short"))) {
                int[] dataOut = (int [])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("int")) | (classType.contains("Integer"))) {
                int[] dataOut = (int [])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("long")) | (classType.contains("Long"))) {
                long[] dataOut = (long [])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("float")) | (classType.contains("Float"))) {
                int[] dataOut = (int [])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("double")) | (classType.contains("Double"))) {
                double[] dataOut = (double [])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
            else if ((classType.contains("char")) | (classType.contains("String"))) {
                String[] dataOut = (String [])cell;
                for (int i = 0; i < dataOut.length; i++){
                    attArrayList.add(dataOut[i]);
                }
            }
        } else {
            attArrayList.add(cell);
        }
        return attArrayList;
    }

    /** Get attribute value (String) from one cell
     * @param attArrayList: attribute data list
     * @return attribute value (concatenated string)
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
     * Define data type list
     * @param colName
     * @param classType
     * @param originalType
     * @param dataCols
     * @return dataTypeList
     */
    public static List<DataType> getDataTypeList(String [] colName, String[] classType, String [] originalType, String [] dataCols)
            throws FitsException
    {

        int nColumns = classType.length;
        List<DataType> dataTypeList = new ArrayList<DataType>(nColumns);
        Class java_class = null;
        String primitive_type = null;

        for (int col = 0; col < nColumns; col ++){
            //ColumnInfo colInfo = columnInfoList.get(i);

            if (Arrays.asList(dataCols).contains(colName[col])){
                //classType = DefaultValueInfo.formatClass(colInfo.getContentClass());
                if ((classType[col].contains("boolean")) | (classType[col].contains("Boolean"))) {
                    if (originalType[col].contains("L")){
                        //Logical:
                        java_class = String.class;
                        primitive_type = "char";
                    }
                    else if (originalType[col].contains("X")){
                        //Bits:
                        java_class = Integer.class;
                        primitive_type = "int";
                    }
                }
                else if ((classType[col].contains("byte")) | (classType[col].contains("Byte"))) {
                    java_class = Integer.class;
                    primitive_type = "int";
                }
                else if ((classType[col].contains("short")) | (classType[col].contains("Short"))) {
                    java_class = Integer.class;
                    primitive_type = "int";
                }
                else if ((classType[col].contains("int")) | (classType[col].contains("Integer"))) {
                    java_class = Integer.class;
                    primitive_type = "int";
                }
                else if ((classType[col].contains("long")) | (classType[col].contains("Long"))) {
                    java_class = Long.class;
                    primitive_type = "long";
                }
                else if ((classType[col].contains("float")) | (classType[col].contains("Float"))) {
                    java_class = Float.class;
                    primitive_type = "float";
                }
                else if ((classType[col].contains("double")) | (classType[col].contains("Double"))) {
                    java_class = Double.class;
                    primitive_type = "double";
                }
                else if ((classType[col].contains("String")) | (classType[col].contains("char"))) {
                    java_class = String.class;
                    primitive_type = "char";
                }
                else
                {
                    throw new FitsException(
                            "Unrecognized format character in FITS table file: " + classType);
                }
                DataType dataType = new DataType(colName[col], java_class);
                dataType.setTypeDesc(primitive_type);

                dataTypeList.add(dataType);
            }

        }
        return (dataTypeList);
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

