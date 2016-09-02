/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import nom.tam.fits.FitsException;
import uk.ac.starlink.table.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Convert an FITS file or FITS binary table(s) to list of DataGroup.
*/
public final class FITSTableReader
{
    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private static Pattern TDISP = Pattern.compile("([ALIBOZFEGD])[NS]?(\\d+)?(\\.\\d+)?");


    public static boolean debug = true;

    /**
     * Strategies to handle FITS data with column-repeat-count greater than 1(TFORMn = rT):
     *  where n = column index, T = data type, r = repeat count
     *
     * DEFAULT: Set DataType to 'ary'.  Store the full array in DataGroup as an object.
     * When DataGroup is written out into IPAC format, it should only describe the data
     * as type = 'char' and value as type[length].  This is the default strategy if not given.
     *
     * TOP_MOST: Ignore the repeat count portion of the TFORMn Keyword
     * returning only the first datum of the field, even if repeat count is more than 1.
     * This should produce exactly one DataGroup per table.
     *
     *
     *
     * ( experiemental.. not sure how it would be use )
     * FULLY_FLATTEN: Generates one DataGroup row for each value of an HDU field.
     * Because each field may have different repeat count (dimension), insert blank
     * when no data is available. This should produce exactly one DataGroup per table.
     * No attribute data added to DataGroup with "FULLY_FLATTEN". Should pass headerCols = null.
     *
     * EXPAND_BEST_FIT: Expands each HDU row into one DataGroup. Fields with lesser count (dimension) will be filled with blanks.
     * EXPAND_REPEAT: Expands each HDU row into one DataGroup. Fields with lesser dimension will be filled with previous values.
     */
    public static final String DEFAULT = "DEFAULT";
    public static final String TOP_MOST = "TOP_MOST";
    public static final String FULLY_FLATTEN = "FULLY_FLATTEN";
    public static final String EXPAND_BEST_FIT = "EXPAND_BEST_FIT";
    public static final String EXPAND_REPEAT = "EXPAND_REPEAT";

    private static final int maxNumAttributeValues = 80;

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

        String strategy = EXPAND_BEST_FIT;
        //String strategy = EXPAND_REPEAT;
        //String strategy = TOP_MOST;
        //String strategy = FULLY_FLATTEN;

        //Test src-004192-i4-0300.fits:
        String[] dataCols =   {"flags", "id", "coord_ra", "coord_dec", "parent", "footprint", "base_ClassificationExtendedness_value", "base_SdssCentroid_xSigma"};
        String[] headerCols = null;
        if (!strategy.equals(FULLY_FLATTEN)){
            headerCols = new String[] {"flags", "id", "coord_ra", "coord_dec", "parent", "footprint", "base_ClassificationExtendedness_value", "base_SdssCentroid_xSigma"};
        }

        /**
        //Test calexp-004192-i4-0300.fits:
        String[] dataCols = {"id", "cat.archive", "name", "kernel", "center_x", "spatialfunctions", "components", "coefficients", "image", "cd", "ctype1", "A", "Ap"};
        String[] headerCols = null;
        if (!strategy.equals(FULLY_FLATTEN)) {
            headerCols = new String[] {"id", "name", "kernel", "center_x", "spatialfunctions", "components", "coefficients", "image", "cd", "ctype1", "A", "Ap"};
        }
         */

        /**
         * // Test all nulls:
        strategy = null;
        dataCols = null;
        headerCols = null;
         */


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
            whichDG = 0;
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
            List<DataGroup> dgList = fits_to_ipac.convertFitsToDataGroup(fits_filename, null, null, TOP_MOST);

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
            DataGroup dataGroup = convertStarTableToDataGroup(table, dataCols, headerCols, strategy);
            dgListTotal.add(dataGroup);
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

    /**
     * Convert a StarTable into DataGroup(s).  Depending on the strategy, this function may return
     * more then one DataGroup.
     * @param table input StarTable from one HDU
     * @param inclCols: The names of the columns to include in the DataGroups.
     *                If inclCols = null, includes all the columns into the DataGroup.
     * @param inclHeaders: The names of the headers to include in the DataGroups.
     *                If inclHeaders = null, includes all headers into the DataGroup.
     * @param strategy The strategy used to deal with column(s) with repeat count (dimension) > 1.
     * @return List<DataGroup> A list of DataGroups
     */

    /**
     *
     * @param table
     * @param inclCols
     * @param inclHeaders
     * @param strategy
     * @return
     * @throws FitsException
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public static DataGroup convertStarTableToDataGroup(StarTable table,
                                                        String[] inclCols,
                                                        String[] inclHeaders,
                                                        String strategy)
            throws FitsException, IllegalArgumentException, IOException {

        if (strategy == null) {
            strategy = DEFAULT;
        }

        //creating DataType list ... column info
        ArrayList<DataType> dataTypes = new ArrayList<>();
        LinkedHashMap<ColumnInfo, Integer> colIdxMap = new LinkedHashMap<>();
        List<String> colList = inclCols == null ? null : Arrays.asList(inclCols);

        for (int colIdx = 0; colIdx < table.getColumnCount(); colIdx++) {
            ColumnInfo colInfo = table.getColumnInfo(colIdx);
            if ( colList == null || colList.contains(colInfo.getName() ) ) {
                DataType dt = convertToDataType(table, colIdx, strategy);
                dataTypes.add(dt);
                colIdxMap.put(colInfo, colIdx);
            }
        }

        // creating DataGroup rows.
        DataGroup dataGroup = new DataGroup(table.getName(), dataTypes);
        for (long row = 0; row < table.getRowCount(); row++){
            addRowToDataGroup(table, dataGroup, colIdxMap, row, strategy);
        }

        // setting DataGroup meta info
        for(int colIdx = 0; colIdx < dataTypes.size(); colIdx++) {
            DataType dt = dataTypes.get(colIdx);
            if (!StringUtils.isEmpty(dt.getShortDesc())) {
                dataGroup.addAttribute(DataSetParser.makeAttribKey(DataSetParser.DESC_TAG, dt.getKeyName()), dt.getShortDesc());
            }
            String format = getParam(table, "TDISP" + (colIdx + 1), 20);
            format = format == null ? null : convertFormat(format);
            if (Double.class.isAssignableFrom(dt.getDataType()) ||
                Float.class.isAssignableFrom(dt.getDataType())) {
                format = format == null && Double.class.isAssignableFrom(dt.getDataType()) ? "%.9g" : format;
                dataGroup.addAttribute(DataSetParser.makeAttribKey(DataSetParser.FORMAT_TAG, dt.getKeyName()), "NONE");
            }
            if (!StringUtils.isEmpty(format)) {
                dataGroup.addAttribute(DataSetParser.makeAttribKey(DataSetParser.FORMAT_DISP_TAG, dt.getKeyName()), format);
            }
        }

        List<String> hdList = inclHeaders == null ? null : Arrays.asList(inclHeaders);
        List params = table.getParameters();
        if (params != null) {
            for (Object p : params) {
                if (p instanceof DescribedValue) {
                    DescribedValue dv = (DescribedValue) p;
                    String n = dv.getInfo().getName();
                    String v = dv.getValueAsString(200);
                    if (hdList == null || hdList.contains(n)) {
                        dataGroup.addAttribute(n, v);
                    }
                }
            }
        }
        dataGroup.shrinkToFitData();
        return dataGroup;
    }

    private static void addRowToDataGroup(StarTable table, DataGroup dataGroup, LinkedHashMap<ColumnInfo, Integer> colIdxMap, long rowIdx, String strategy) {
        DataObject aRow = new DataObject(dataGroup);
        ColumnInfo[] colAry = colIdxMap.keySet().toArray(new ColumnInfo[colIdxMap.size()]);
        try {
            for (int dtIdx = 0; dtIdx < dataGroup.getDataDefinitions().length; dtIdx++) {
                DataType dt = dataGroup.getDataDefinitions()[dtIdx];
                ColumnInfo colInfo = colAry[dtIdx];
                int colIdx = colIdxMap.get(colInfo);
                Object data = table.getCell(rowIdx, colIdx);
                if (colInfo.isArray()) {
                    if (Objects.equals(strategy, DEFAULT)) {
                        String desc = DefaultValueInfo.formatClass(colInfo.getContentClass());
                        data = desc.replace("[]", "[" + Array.getLength(data) + "]");
                    } else  if (Objects.equals(strategy, TOP_MOST)) {
                        data = Array.get(data, 0);
                    }
                }
                aRow.setDataElement(dt, data);
            }
            dataGroup.add(aRow);
        } catch (IOException e) {
            logger.error("Unable to read StarTable row:" + rowIdx + "   msg:" + e.getMessage());
        }
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
     *
     * @param table
     * @param colIdx
     * @param strategy
     * @return dataType
     * @throws FitsException
     */
    public static DataType convertToDataType(StarTable table, int colIdx, String strategy)
            throws FitsException{

        ColumnInfo colInfo = table.getColumnInfo(colIdx);
        String colName = colInfo.getName();
        String classType = DefaultValueInfo.formatClass(colInfo.getContentClass());
        String unit = colInfo.getUnitString();
        String nullString = null;
        String desc = colInfo.getDescription();
        desc = desc == null ? getParam(table, "TDOC" + (colIdx+1), 200) : desc; // this is for LSST.. not sure it applies to others.

        DataType dataType = new DataType(colName, null);
        Class java_class = null;

        if ((classType.contains("boolean")) || (classType.contains("Boolean"))) {
            java_class = Boolean.class;
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
            nullString = "NaN";
        } else if ((classType.contains("double")) || (classType.contains("Double"))) {
            java_class = Double.class;
            nullString = "NaN";
        } else if ((classType.contains("char")) || (classType.contains("String"))) {
            java_class = String.class;
        } else {
            throw new FitsException(
                    "Unrecognized format character in FITS table file: " + classType);
        }

        if (Objects.equals(strategy, DEFAULT) && colInfo.isArray()) {
            java_class = String.class;
        }

        dataType.setDataType(java_class);
        dataType.setUnits(unit);
        dataType.setNullString(nullString);
        dataType.setShortDesc(desc);

        return dataType;
    }

    private static String getParam(StarTable table, String key, int maxWidth) {
        DescribedValue p = table.getParameterByName(key);
        return p == null ? null : p.getValueAsString(maxWidth);
    }

    /**
     * converts FITS table keyword TDISPn into java format
     * see http://archive.stsci.edu/fits/fits_standard/node69.html#SECTION001232060000000000000
     * @param format
     * @return
     */
    private static String convertFormat(String format) {
        Matcher m = TDISP.matcher(format);
        if (m.find()) {
            int count = m.groupCount();
            String conv = String.valueOf(m.group(1));
            String width = "";  // count > 1 ? m.group(2) : "";     ignores width for now.
            String prec = count > 2 ? m.group(3) : "";
            width = width == null ? "" : width;
            prec = prec == null ? "" : prec;
            if (conv.matches("D|E")) {
                return "%" + width + prec + "e";
            } else if (conv.equals("G")) {
                return "%" + width + prec + "g";
            } else if (conv.equals("F")) {
                return "%" + width + prec + "f";
            }
        }
        // not implemented or supported.. will print
        return null;
    }

    /**
     * Fill a dataGroup with a row of data from a table.
     * Each dataObj contains data from all the columns at a repeat.
     * Convert the data type of each element from the star types to IPAC types if needed.
     * If strategy = EXPAND_BESET_FIT or EXPAND_REPEAT, fill in null or the last data value when no data is available in the column.
     *
     * @param table
     * @param dataTypeList
     * @param maxRepeat
     * @param row
     * @param dataCols
     * @param strategy
     * @param dataGroup
     * @return dataGroup
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
                            dataElement = convertStarToIpacType(Array.get(cell, rpt), classType, originalType);
                        }
                        else {
                            if (strategy.equals(EXPAND_REPEAT)) {
                                dataElement = convertStarToIpacType(Array.get(cell, Array.getLength(cell) - 1), classType, originalType);
                            }
                            else if ((strategy.equals(EXPAND_BEST_FIT)) || (strategy.equals(FULLY_FLATTEN)) ) {
                                dataElement = null;
                            }
                        }
                    }
                    else {
                        if (rpt == 0) {
                            dataElement = convertStarToIpacType(cell, classType, originalType);
                        }
                        else {
                            if (strategy.equals(EXPAND_REPEAT)) {
                                dataElement = convertStarToIpacType(cell, classType, originalType);
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
     * Convert the data from star type to IPAC type:
     * Since Starlink converts bits to boolean (1 to true and 0 to false), this method converts boolean(bits) to int (true to 1 and false to 0).
     * Convert boolean (logic) to String "true" or "false".
     * Convert byte/short to int as IPAC table doesn't support byte and short.
     *
     * @param obj
     * @param classType
     * @param originalType
     * @return objNew
     */
    private static Object convertStarToIpacType(Object obj,
                                                String classType,
                                                String originalType) {

        Object objNew = obj;

        if (classType.contains("boolean")){
            if (originalType.contains("L")){
                objNew = String.valueOf(obj);
            }
            else if (originalType.contains("X")){
                objNew = (Boolean)obj? 1:0;
            }
        }
        else if (classType.contains("byte") || classType.contains("short") ){
            objNew = (Integer)obj; // Q: Need to cast to Integer?
        }

        return objNew;
    }

    /**
     * Concatenate the element(s) in the obj and the divider "," to a String attributeValue.
     * Convert the data type of each element from the star types to IPAC types if needed.
     * @param obj
     * @param isArray
     * @param classType
     * @param originalType
     * @return attributeValue String
     */
    private static String getAttributeValue(Object obj,
                                            boolean isArray,
                                            String classType,
                                            String originalType) {
        String attributeValue;
        if (isArray){
            int attributeNum = Array.getLength(obj) < maxNumAttributeValues? Array.getLength(obj) : maxNumAttributeValues;
            Object [] data = new Object[attributeNum];
            for (int rpt = 0; rpt < attributeNum; rpt ++){
                data[rpt] = convertStarToIpacType(Array.get(obj,rpt), classType, originalType);
            }
            attributeValue = StringUtils.toString(data, ",");
        }
        else {
            attributeValue = String.valueOf(convertStarToIpacType(obj, classType, originalType));
        }
        return attributeValue;
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