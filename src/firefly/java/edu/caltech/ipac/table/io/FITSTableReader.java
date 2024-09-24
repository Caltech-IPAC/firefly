/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
import nom.tam.fits.AbstractTableData;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.TableHDU;
import nom.tam.fits.UndefinedHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.Cursor;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.caltech.ipac.util.StringUtils.isEmpty;
import static edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil.dataArrayFromFitsFile;

/**
* Convert an FITS file or FITS binary table(s) to list of DataGroup.
*/
public final class FITSTableReader
{
    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private static final Pattern TDISP = Pattern.compile("(A|I|B|O|Z|F|E|EN|ES|G|D)(\\d+)?(?:\\.(\\d+))?.*");
    private static final Pattern EXPONENTIAL = Pattern.compile("E|EN|ES|D");                                    // Table 20 from https://fits.gsfc.nasa.gov/standard30/fits_standard30aa.pdf

    public static boolean debug = true;

    /**
     * Strategies to handle FITS data with column-repeat-count greater than 1(TFORMn = rT):
     *  where n = column index, T = data type, r = repeat count

     * DEFAULT: Set DataType to 'ary'.  Store the full array in DataGroup as an object.
     * When DataGroup is written out into IPAC format, it should only describe the data
     * as type = 'char' and value as type[length].  This is the default strategy if not given.

     * TOP_MOST: Ignore the repeat count portion of the TFORMn Keyword
     * returning only the first datum of the field, even if repeat count is more than 1.
     * This should produce exactly one DataGroup per table.

     * ( experiemental.. not sure how it would be use )
     * FULLY_FLATTEN: Generates one DataGroup row for each value of an HDU field.
     * Because each field may have different repeat count (dimension), insert blank
     * when no data is available. This should produce exactly one DataGroup per table.
     * No attribute data added to DataGroup with "FULLY_FLATTEN". Should pass headerCols = null.

     * EXPAND_BEST_FIT: Expands each HDU row into one DataGroup. Fields with lesser count (dimension) will be filled with blanks.
     * EXPAND_REPEAT: Expands each HDU row into one DataGroup. Fields with lesser dimension will be filled with previous values.
     */
    public static final String DEFAULT = "DEFAULT";

    public static DataGroup convertFitsToDataGroup(String fits_filename, TableServerRequest request,
                                                   String strategy, int table_idx) throws FitsException, IOException {
        return convertFitsToDataGroup(fits_filename,null,null,strategy,request,table_idx);
    }

    /**
     * Convert a table from a FITS file to DataGroup based on table index
     * @param fits_filename the file name
     * @param dataCols The names of the columns which will be copied to the data section of the DataGroups.
     *                If dataCols = null, get all the columns into the data group.
     * @param headerCols The names of the columns which will be copied to the header section of the DataGroups.
     *                If headerCols = null, get none of the columns into the header of the data group.
     * @param strategy The strategy used to deal with the repeat count  of the data in the given dataCols columns.
     * @param table_idx table index, i.e. HDU number in FITS
     * @return the data group
     */
    public static DataGroup convertFitsToDataGroup(String fits_filename,
                                                         String[] dataCols,
                                                         String[] headerCols,
                                                         String strategy,
                                                         TableServerRequest request,
                                                         int table_idx) throws FitsException, IOException {


        FitsFactory.useThreadLocalSettings(true);
        var metaInfo= request!=null ? request.getMeta() : null;
        try (Fits fits = new Fits(fits_filename)) {
            // disable long string for HeaderCard creation while collecting table with table_idx from StarTableFactory to work around
            // the exception error sent from nom.tam.fits.
            DataGroup result;
            ;
            BasicHDU<?>[] hdus = fits.read();

            if (table_idx >= hdus.length) {
                throw new FitsException( "table index of " +table_idx+" exceeds the number of HDUS " + hdus.length);
            }
            BasicHDU<?> hdu= hdus[table_idx];

            if (table_idx==0) { //FITS tables are not at 0, if at zero try to read it as an image first
                result = getFitsImageAsTable(hdu,request);
                if (result==null && hdus.length>1) {
                    FitsFactory.setLongStringsEnabled(false);
                    result = readFitsTable(hdus[1], fits_filename, dataCols, headerCols, table_idx);
                }
            }
            else { // if >0 then try to read it as a FITS table first.
                FitsFactory.setLongStringsEnabled(false);
                result = readFitsTable(hdu, fits_filename, dataCols, headerCols, table_idx);
                FitsFactory.setLongStringsEnabled(true);
                if (result == null) result = getFitsImageAsTable(hdu,request);
            }
            String dataTypeHint= metaInfo !=null ? metaInfo.getOrDefault(MetaConst.DATA_TYPE_HINT,"").toLowerCase() : "";
            if (result != null) SpectrumMetaInspector.searchForSpectrum(result,hdus[table_idx], dataTypeHint.equals("spectrum"));
            return result;
        } catch (FitsException|IOException e) {
            logTableReadError(fits_filename,table_idx,e.getMessage());
            throw e;
        } finally {
            FitsFactory.useThreadLocalSettings(false);
        }
    }

    private static boolean is1dImage(BasicHDU<?> hdu) {
        Header header = hdu.getHeader();
        int naxis = header.getIntValue("NAXIS", 0);
        if (naxis<1) return false;
        boolean hasNAxis1Data= header.getIntValue("NAXIS1",0)>0;
        if (naxis==1 && hasNAxis1Data) return true;
        boolean otherDimsAre1= true;
        for(int i=2;(i<=naxis);i++) {
            if  (header.getIntValue("NAXIS"+i,0)>1) otherDimsAre1= false;
        }
        return hasNAxis1Data && otherDimsAre1;
    }

    private static DataGroup getFitsImageAsTable(BasicHDU<?> hdu,TableServerRequest request)
                                                 throws FitsException, IOException {

        Header header = hdu.getHeader();
        var indexColName= header.getStringValue("CNAME1", "Index");
        String[] colNames= new String[]{indexColName};
        String[] colUnits= null;
        int planeNumber= request!=null ? request.getIntParam("cubePlane",0) : 0;
        var metaInfo= request!=null ? request.getMeta() : null;
        if (metaInfo!=null) {
            String colNameStr = metaInfo.get(MetaConst.IMAGE_AS_TABLE_COL_NAMES);
            if (colNameStr != null && colNameStr.length() > 1) colNames =colNameStr.split(",");
            String colUnitsStr = metaInfo.get(MetaConst.IMAGE_AS_TABLE_UNITS);
            if (colUnitsStr != null && colUnitsStr.length() > 1) colUnits =colUnitsStr.split(",");
        }

        if ((hdu instanceof ImageHDU) || (hdu instanceof CompressedImageHDU || hdu instanceof UndefinedHDU)) {
            int naxis = FitsReadUtil.getNaxis(header);
            if (naxis < 1) return null;
            int naxis1 = FitsReadUtil.getNaxis1(header);
            if (naxis1 < 1) return null;
            int naxis2 = FitsReadUtil.getNaxis2(header);
            String desc = FitsReadUtil.getExtName(header);
            if (desc == null) desc = header.getStringValue("NAME");
            if (desc == null) desc = "No Name";
            ArrayList<DataType> dataTypes = new ArrayList<>();
            DataType idxDT = new DataType(colNames[0], colNames[0], Integer.class);
            idxDT.setUnits("pixel");
            if (colUnits!=null) idxDT.setUnits(colUnits[0]);
            dataTypes.add(idxDT);

            if (is1dImage(hdu)) {
                double[] data = FitsReadUtil.getImageHDUDataInDoubleArray(hdu);
                if (data == null) return null;
                String bunit= FitsReadUtil.getBUnit(header);
                String extname= FitsReadUtil.getExtName(header);

                String dataCName = (colNames.length > 1) ? colNames[1] : !isEmpty(extname) ? extname : "value";
                DataType dataDT = new DataType(dataCName, dataCName, Double.class);
                if (!isEmpty(bunit)) dataDT.setUnits(bunit);
                if (colUnits!=null && colUnits.length>1) idxDT.setUnits(colUnits[1]);
                dataTypes.add(dataDT);
                DataGroup dataGroup = new DataGroup(desc, dataTypes);
                dataGroup.setInitCapacity(data.length);
                for (int i = 0; (i < data.length); i++) {
                    DataObject aRow = new DataObject(dataGroup);
                    aRow.setDataElement(idxDT, i);
                    aRow.setDataElement(dataDT, data[i]);
                    dataGroup.add(aRow);
                }
                dataGroup.trimToSize();
                return dataGroup;
            } else if ((naxis == 2 || naxis == 3)  && naxis2 > 0) {
                double[][] data= null;
                if (naxis2 > 30) return null; // right now we only support 30 columns, this could be a parameter
                if ((hdu instanceof ImageHDU) || (hdu instanceof CompressedImageHDU)) {
                    ImageHDU imageHDU = (hdu instanceof CompressedImageHDU) ?
                            ((CompressedImageHDU) hdu).asImageHDU() : (ImageHDU) hdu;
                    if (naxis==2) {
                        ImageData imageDataObj = imageHDU.getData();
                        data = (double[][]) ArrayFuncs.convertArray(imageDataObj.getData(), Double.TYPE, true);
                    }
                    else if (naxis2==1) {
                        double[] data1D = (double[])dataArrayFromFitsFile(imageHDU,0,0,naxis1,naxis2, planeNumber,Double.TYPE);
                        data= new double[1][data1D.length];
                        data[0]= data1D;
                    }
                } else { //hdu instanceof UndefinedHDU is always true here
                    data = (double[][]) ArrayFuncs.convertArray(hdu.getData().getData(), Double.TYPE, true);
                }
                if (data == null) return null;

                for (int i = 0; (i < data.length); i++) {
                    String cName = (i + 1 < colNames.length) ? colNames[i+1] : "data" + i;
                    DataType dt= new DataType(cName, cName, Double.class);
                    if (colUnits!=null && colUnits.length>i) dt.setUnits(colUnits[i+1]);
                    dataTypes.add(dt);
                }

                DataGroup dataGroup = new DataGroup(desc, dataTypes);
                dataGroup.setInitCapacity(data[0].length);

                DataType[] dd;
                for (int row = 0; row < data[0].length; row++) {
                    DataObject aRow = new DataObject(dataGroup);
                    dd = dataGroup.getDataDefinitions();
                    aRow.setDataElement(dd[0], row);
                    for (int dtIdx = 1; dtIdx < dd.length; dtIdx++) {
                        aRow.setDataElement(dd[dtIdx], data[dtIdx - 1][row]);
                    }
                    dataGroup.add(aRow);
                }
                dataGroup.trimToSize();
                return dataGroup;
            }
        }
        return null;
    }


    private static void logTableReadError(String fitsFilename, int tableIdx, String reason) {
        logger.error("Unable to get table from fits file: " + fitsFilename +
                ", HDU#: " + tableIdx + ", reason: "+reason);
    }

    //This function is loosely based on the packagedType function from the FitsStarTable class (uk.ac.starlink.fits package)
    private static Class<?> getClassType(Object base, int icol, boolean[] isScaled, String colFormat) {
        if (base == null) {
            if (isScaled[icol]) return Double.class;
            return getClassByTform(colFormat);
        } else {
            Class<?> cls = base.getClass().getComponentType();
            if (cls != null && Array.getLength(base) == 1) {
                if (isScaled[icol]) {
                    return Double.class;
                }
                return cls;
            } else if (cls != null && cls.isArray()) {
                return ArrayFuncs.flatten(base).getClass();
            }
            return base.getClass();
        }
    }

    private static Class<?> getClassByTform(String tform) {
        if (tform==null) return String.class;
        Pattern pattern = Pattern.compile("^[0-9]*");
        Matcher m= pattern.matcher(tform);
        var startStr= m.find() ? m.group() : "";

        int arrayLen;
        try {
            arrayLen= !startStr.isEmpty() ? Integer.parseInt(startStr) : 0;

        } catch (NumberFormatException e) {
            arrayLen= 0;
        }
        var isArray= arrayLen>0;

        char typeChar= startStr.length()<tform.length() ? tform.charAt(startStr.length()) : 'A';
        return switch (typeChar) {
            case 'X', 'B', 'I', 'J' -> isArray ? int[].class : int.class;
            case 'K' -> isArray ? long[].class : long.class;
            case 'E' -> isArray ? float[].class : float.class;
            case 'D' -> isArray ? double[].class : double.class;
            case 'L' -> boolean.class;
            default -> String.class;
        };
    }

    /**
     *
     * @param hdu hdu to use for reading
     * @param fitsFilename fits fileName string
     * @param inclCols cols to include when creating DataType entries
     * @param inclHeaders headers to include when creating DataGroup TableMeta
     * @param tableIdx the hdu index to read
     * @return DataGroup converted from the hdu
     * @throws IOException thrown if error reading table entry
     * @throws FitsException call to convertHDUToDataType may throw FitsException
     */
    private static DataGroup readFitsTable(BasicHDU<?> hdu,
                                          String fitsFilename,
                                          String[] inclCols,
                                          String[] inclHeaders,
                                           int tableIdx) throws IOException, FitsException {

        if (!(hdu instanceof TableHDU<?> hduTable)) {
            logTableReadError(fitsFilename,tableIdx,"HDU is not a table hdu");
            return null;
        }

        AbstractTableData data = (AbstractTableData) hdu.getData();

        int colCount = data.getNCols();
        Class<?>[] bases = new Class[colCount];
        String[] colNames = new String[colCount];

        int nrow = hduTable.getNRows();
        int ncol = hduTable.getNCols();

        double[] scales = new double[ncol];
        double[] zeros = new double[ncol];
        boolean[] isScaled = new boolean[ncol];
        long[] blanks = new long[ncol];
        boolean[] hasBlank = new boolean[ncol];

        Arrays.fill(scales, 1.0);

        try {
            for (int icol = 0; icol < ncol; ++icol) {
                colNames[icol] = hduTable.getColumnName(icol);

                String tscal = hduTable.getColumnMeta(icol, "TSCAL");
                String tzero = hduTable.getColumnMeta(icol, "TZERO");
                double zeroval;
                if (tscal != null) {
                    zeroval = Double.parseDouble(tscal);
                    scales[icol] = zeroval;
                }
                if (tzero != null) {
                    zeroval = Double.parseDouble(tzero);
                    zeros[icol] = zeroval;
                }
                if (scales[icol] != 1.0 || zeros[icol] != 0.0) {
                    isScaled[icol] = true;
                }

                String blankKey = "TNULL" + (icol + 1);
                if (hduTable.getHeader().containsKey(blankKey)) {
                    long nullval = hduTable.getHeader().getLongValue(blankKey); //hduTable.getBlankValue();
                    blanks[icol] = nullval;
                    hasBlank[icol] = true;
                }

                Object entry = null;
                try {
                    for (int irow = 0; entry == null && irow < nrow; ++irow) {
                        entry = hduTable.getElement(irow, icol);
                    }
                } catch (Exception e) {
                    throw new IOException("Error reading table entry");
                }

                bases[icol] =
                        hduTable.getNRows()==0 ? getClassByTform(hduTable.getColumnFormat(icol)) :
                                getClassType(entry, icol, isScaled, hduTable.getColumnFormat(icol));
            }
        }
        catch (NumberFormatException e) {
            logger.error("Number format exception reading column meta: " + e.getMessage());
        }
        //creating DataType list ... column info
        ArrayList<DataType> dataTypes = new ArrayList<>();
        List<String> colList = inclCols == null ? null : Arrays.asList(inclCols);

        for (int colIdx = 0; colIdx < colCount; colIdx++) {
            if ((colList == null) || colList.contains(colNames[colIdx])) {
                DataType dt = convertHDUToDataType(colNames, bases, hduTable, colIdx);
                dataTypes.add(dt);
            }
        }

        DataGroup dataGroup = new DataGroup(fitsFilename, dataTypes);
        // creating DataGroup rows.
        dataGroup.setInitCapacity(nrow);
        DataType[] dataDefinitions= dataGroup.getDataDefinitions();
        for (int row = 0; row < nrow; row++){
            addRowToDG(dataGroup, dataDefinitions, row, hduTable, hasBlank, blanks, isScaled, scales, zeros);
        }

        // setting DataGroup meta info
        for(int colIdx = 0; colIdx < dataTypes.size(); colIdx++) {
            DataType dt = dataTypes.get(colIdx);
            String format = hduTable.getColumnMeta(colIdx, "TDISP");
            convertFormat(format, dt);
        }

        HashMap<String, String> headerParams = new HashMap<>();
        Cursor<String, HeaderCard> iter = hduTable.getHeader().iterator(); //to iterate over Header Cards
        while (iter.hasNext()) {
            HeaderCard hCard = iter.next();
            String key = hCard.getKey();
            String value = hCard.getValue();
            if (key.equalsIgnoreCase("END")) continue; //this just signifies end of cards, ignore
            if (headerParams.containsKey(key)) {
                if (value != null) {
                    value = headerParams.get(key) + value;
                }
            }
            headerParams.put(key, value);
        }

        List<String> hdList = inclHeaders == null ? null : Arrays.asList(inclHeaders);
        for (Map.Entry<String, String> entry : headerParams.entrySet()) {
            String n = entry.getKey();
            String v = entry.getValue();
            if (hdList == null || hdList.contains(n)) {
                dataGroup.getTableMeta().addKeyword(n, v); //should keywords be in order as they appear in the Table Header?
            }
        }
        dataGroup.trimToSize();
        return dataGroup;
    }

    record EvalVal(long blank, boolean scaled, boolean hasBlank, double scale, double zero) {
        public Number evalValue(Number val) {
            if (hasBlank && val.doubleValue() == blank) return null;
            if (!scaled) return val;
            return val.doubleValue() * scale + zero;
        }
    }

    //This function is loosely based on the packageValue function from the FitsStarTable class in the uk.ac.starlink.fits package
    private static Object getValAsObject(Object elem, int icol, boolean[] hasBlank, long[] blanks, boolean[] isScaled,
                                         double[] scales, double[] zeros) throws FitsException {
            if (elem == null) {
                return null;
            }
            else if (!elem.getClass().isArray()) {
                if (elem instanceof String) return isEmpty((String)elem) ? null : elem;
                if (elem instanceof Byte || elem instanceof Short) return ((Number)elem).intValue();
                return elem;
            }
            else if (Array.getLength(elem) == 1) {
                String cls = elem.getClass().getComponentType().toString();
                EvalVal evaluator = new EvalVal(blanks[icol], isScaled[icol], hasBlank[icol], scales[icol], zeros[icol]);
                return switch (cls) {
                    case "byte" -> evaluator.evalValue((int)((byte[])(elem))[0]);
                    case "short" -> evaluator.evalValue((int)((short[])(elem))[0]);
                    case "int" -> evaluator.evalValue(((int[])(elem))[0]);
                    case "long" -> evaluator.evalValue(((long[])(elem))[0]);
                    case "float" -> evaluator.evalValue(((float[])(elem))[0]);
                    case "double" -> evaluator.evalValue(((double[])(elem))[0]);
                    case "boolean" -> Boolean.valueOf(((boolean[])(elem))[0]);
                    case "class java.lang.String" -> isEmpty(((String[])(elem))[0]) ? null : ((String[])(elem))[0];
                    default -> throw new FitsException( "Unrecognized class type in FITS table file entry: " + cls);
                };
            }
            else {
                return ArrayFuncs.flatten(elem);
            }
    }

    private static void addRowToDG(DataGroup dataGroup, DataType[] dataDefinitions, int rowIdx, TableHDU<?> hduTable, boolean[] hasBlank, long[] blanks, boolean[] isScaled,
                                   double[] scales, double[] zeros) {
        DataObject aRow = new DataObject(dataGroup);
        try {
            Object[] rowData= hduTable.getRow(rowIdx);
            for (int dtIdx = 0; dtIdx < dataGroup.getDataDefinitions().length; dtIdx++) {
                //so cast the val object to an array of its type by calling the getValAsObject function
                Object unpackedVal = getValAsObject(rowData[dtIdx], dtIdx, hasBlank, blanks, isScaled, scales, zeros);
                aRow.setDataElement(dataDefinitions[dtIdx], unpackedVal);
            }
            dataGroup.add(aRow);
        } catch (Exception e) {
            logger.error("Unable to read table row:" + rowIdx + "   msg:" + e.getMessage());
        }
    }

    public static Class<?> formatClass(Class<?> c) throws FitsException {
        String cname = c.getName();
        //check if dimension is 0 then cname is of type "boolean" or "byte", etc.
        //but if dimension is > 0, then cname may be of type "[[[B" or "[S", etc.
        if (cname.contains("boolean") || (cname.contains("[") && cname.contains("Z"))) {
            return Boolean.class;
        }
        else if (cname.contains("byte") || cname.contains("short") || cname.contains("int")  ||
                (cname.contains("[") && (cname.contains("B") || cname.contains("S") || cname.contains("I")))) {
            return Integer.class;
        }
        else if (cname.contains("long") || (cname.contains("[") && cname.contains("J"))) {
            return Long.class;
        }
        else if (cname.contains("float") || (cname.contains("[") && cname.contains("F"))) {
            return Float.class;
        }
        else if (cname.contains("double") || (cname.contains("[") && cname.contains("D"))) {
            return Double.class;
        }
        else if (cname.contains("char") || cname.contains("String") ||
                (cname.contains("[") && cname.contains("C"))) {
            return String.class;
        }
        else {
            throw new FitsException(
                    "Unrecognized format character in FITS table file: " + cname);
        }
    }

    private static DataType convertHDUToDataType(String[] colNames, Class<?>[] bases, TableHDU<?> hduTable, int colIdx)
            throws FitsException{

        String colName = colNames[colIdx] !=null ? colNames[colIdx] : "column-"+colIdx;
        DataType dataType = new DataType(colName, null);

        if (bases[colIdx].isArray()) {
            int[] shape = getShape(hduTable, colIdx); //parse TDIM value
            String arraySize = Arrays.stream(shape)
                    .mapToObj(d -> d > 0 ? d+"" : "*")
                    .collect(Collectors.joining("x"));
            dataType.setArraySize(arraySize); //TDIM
        }
        dataType.setDataType(formatClass(bases[colIdx]));
        String tunit = hduTable.getColumnMeta(colIdx, "TUNIT");
        if (tunit != null) {
            dataType.setUnits(tunit);
        }

        String tcomm = hduTable.getColumnMeta(colIdx, "TCOMM");
        String desc = tcomm == null ? hduTable.getColumnMeta(colIdx, "TDOC") : tcomm; // this is for LSST.. not sure it applies to others.
        dataType.setDesc(desc);

        String tucd = hduTable.getColumnMeta(colIdx, "TUCD");
        if (tucd != null) {
            dataType.setUCD(tucd);
        }

        String tutype = hduTable.getColumnMeta(colIdx, "TUTYP");
        if (tutype != null) {
            dataType.setUType(tutype);
        }

        return dataType;
    }

    private static int[] getShape(TableHDU<?> hduTable, int colIdx) {
        String tdim = hduTable.getColumnMeta(colIdx, "TDIM");
        if (tdim != null) {
            tdim = tdim.trim();
            if (tdim.charAt(0) == '(' && tdim.charAt(tdim.length() - 1) == ')') {
                tdim = tdim.substring(1, tdim.length() - 1).trim();
                String[] sdims = tdim.split(",");
                if (sdims.length > 0) {
                    try {
                        int[] dims = new int[sdims.length];

                        for (int i = 0; i < sdims.length; ++i) {
                            dims[i] = Integer.parseInt(sdims[i].trim());
                        }
                        return dims;
                    } catch (NumberFormatException e) {
                        logger.error("Number format exception parsing dimension: " + e.getMessage());
                    }
                }
            }
        }
        return new int[]{-1};
    }

    /**
     * converts FITS table keyword TDISPn into firefly's precision/width attributes
     * see http://archive.stsci.edu/fits/fits_standard/node69.html#SECTION001232060000000000000
     * @param format a format string taken from TDISPn
     * @param dt     the column this format belongs to
     */
    private static void convertFormat(String format, DataType dt) {
        if (!isEmpty(format)) {
            String[] parts = StringUtils.groupMatch(TDISP, format);     // 0:conversion code, 1: width, 2:precision
            if (parts == null) return;

            String code = parts.length > 0 ? parts[0] : "";
            int width = parts.length > 1 ? StringUtils.getInt(parts[1], 0) : 0;
            String prec = parts.length > 2 ? parts[2] : "";

            if (width > 0) dt.setWidth(width);

            if (code != null) {
                if (EXPONENTIAL.matcher(code).matches()) {
                    dt.setPrecision("E" + prec);
                } else if(code.equals("F")) {
                    dt.setPrecision("F" + prec);
                } else if(code.equals("G")) {
                    dt.setPrecision("G" + prec);
                }
            }
        }
    }
}