/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
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
    private static final int MAX_IMAGE_COLS= 30;

    record FitsTableReadInfo(DataGroup dataGroup, EvalVal[] evaluator) {};

    public static boolean debug = true;

    /**
     * Convert a table from a FITS file to DataGroup based on table index
     * @param fits_filename the file name
     * @param table_idx table index, i.e. HDU number in FITS
     * @return the data group
     */
    public static DataGroup readFitsTable(String fits_filename, TableServerRequest request, int table_idx)
            throws FitsException, IOException {

        int workingTableIdx= table_idx;
        try (Fits fits = new Fits(fits_filename)) {
            FitsFactory.useThreadLocalSettings(true);
            DataGroup result= null;

            BasicHDU<?>[] hdus = fits.read();

            if (table_idx >= hdus.length) {
                throw new FitsException("table index of " + table_idx + " exceeds the number of HDUS " + hdus.length);
            }
            workingTableIdx= normalizeTableIndex(hdus,table_idx);
            BasicHDU<?> hdu= hdus[workingTableIdx];

            if (hdu instanceof TableHDU<?>) {
                result = doReadFitsTable(hdu, fits_filename, table_idx);
            }
            else if (isImageLike(hdu) && canReadImageAsTable(hdu)) {
                result = getFitsImageAsTable(hdu, request,null);
            }
            if (result != null) SpectrumMetaInspector.searchForSpectrum(result, hdus[table_idx], isSpectrumHint(request));
            return result;
        } catch (FitsException | IOException e) {
            logTableReadError(fits_filename, workingTableIdx, e.getMessage());
            throw e;
        } finally {
            FitsFactory.useThreadLocalSettings(false);
        }
    }



    public static boolean ingestFitsTable(TableParseHandler handler, String fits_filename, TableServerRequest request, int table_idx)
                throws FitsException, IOException {

        FitsFactory.useThreadLocalSettings(true);
        int workingTableIdx= table_idx;
        try (Fits fits = new Fits(fits_filename)) {
            BasicHDU<?>[] hdus = fits.read();
            if (table_idx >= hdus.length) {
                throw new FitsException( "table index of " +table_idx+" exceeds the number of HDUS " + hdus.length);
            }
            workingTableIdx= normalizeTableIndex(hdus,table_idx);
            BasicHDU<?> hdu= hdus[workingTableIdx];
            if (hdu instanceof TableHDU<?> hduTable) {
                doIngestFitsTable(handler,hduTable,fits_filename,workingTableIdx,isSpectrumHint(request));
                return true;
            } else if (isImageLike(hdu) && canReadImageAsTable(hdu)) {
                getFitsImageAsTable(hdu, request,handler);
                return true;
            }
            else {
                return false;
            }
        } catch (FitsException|IOException e) {
            logTableReadError(fits_filename,workingTableIdx,e.getMessage());
            throw e;
        } finally {
            FitsFactory.useThreadLocalSettings(false);
        }
    }

    public static DataGroup readTableHeader(BasicHDU<?> hdu) {
        if (!(hdu instanceof TableHDU<?> hduTable)) return null;
        try {
            var fitsTableReadInfo= doReadFitsTableHeader(hduTable,"unknown");
            var dataGroup= fitsTableReadInfo.dataGroup();
            dataGroup.trimToSize();
            return dataGroup;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * tables are never at hdu 0, if zero is passed and there is not an image hdu at 0 and there is more than 1 hdu
     * then return 1. Otherwise, return the passed index.
     * @param hdus hdu array
     * @param table_idx hdu index of the table
     * @return new index
     */
    private static int normalizeTableIndex(BasicHDU<?>[] hdus, int table_idx) {
        if (table_idx!= 0 || hdus.length==0) return table_idx; // only do a change it idx is 0 and there is more than 0 hdu
        return isImageLike(hdus[table_idx]) && canReadImageAsTable(hdus[table_idx]) ? table_idx : 1;
    }

    private static boolean isSpectrumHint(TableServerRequest request) {
        var metaInfo= request!=null ? request.getMeta() : null;
        String dataTypeHint= metaInfo !=null ? metaInfo.getOrDefault(MetaConst.DATA_TYPE_HINT,"").toLowerCase() : "";
        return dataTypeHint.equalsIgnoreCase(FileAnalysisReport.TableDataType.Spectrum.name());
    }

    private static boolean isImageLike(BasicHDU<?> hdu) {
        return ((hdu instanceof ImageHDU || hdu instanceof CompressedImageHDU || hdu instanceof UndefinedHDU));
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

    private static boolean canReadImageAsTable(BasicHDU<?> hdu) {
        if (!isImageLike(hdu)) return false;
        Header header = hdu.getHeader();
        int naxis = FitsReadUtil.getNaxis(header);
        if (naxis < 1) return false;
        int naxis1 = FitsReadUtil.getNaxis1(header);
        if (naxis1 < 1) return false;
        int naxis2 = FitsReadUtil.getNaxis2(header);
        if (is1dImage(hdu)) return true;
        return (naxis2 <= MAX_IMAGE_COLS);
    }


    private static DataGroup getFitsImageAsTable(BasicHDU<?> hdu,TableServerRequest request, TableParseHandler handler)
                                                 throws FitsException, IOException {

        if (!isImageLike(hdu)) return null;
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
            load1dRows(dataGroup,data,handler);
            return dataGroup;
        } else if ((naxis == 2 || naxis == 3)  && naxis2 > 0) {
            double[][] data= null;
            if (naxis2 > MAX_IMAGE_COLS) return null; // right now we only support 30 columns, this could be a parameter
            if ((hdu instanceof ImageHDU) || (hdu instanceof CompressedImageHDU)) {
                ImageHDU imageHDU = (hdu instanceof CompressedImageHDU cihdu) ? cihdu.asImageHDU() : (ImageHDU) hdu;
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
            load2dRows(dataGroup,data,handler);
            return dataGroup;
        }
        return null;
    }


    private static void load2dRows(DataGroup dataGroup, double[][] data, TableParseHandler handler) throws FitsException, IOException {
        DataType[] dd= dataGroup.getDataDefinitions();;
        if (handler!=null) {
            try {
                handler.start();
                handler.startTable(0);
                handler.header(dataGroup);
                Object [] outRow;

                for (int row = 0; row < data[0].length; row++) {
                    DataObject aRow = new DataObject(dataGroup);
                    outRow= new Object[dd.length];
                    outRow[0] = row;
                    for (int dtIdx = 1; dtIdx < dd.length; dtIdx++) {
                        outRow[dtIdx] = data[dtIdx - 1][row];
                    }
                    handler.data(outRow);
                    dataGroup.add(aRow);
                }
            } catch (Exception e) {
                throw new IOException(e.toString(),e);
            } finally {
                handler.endTable(0);
                handler.end();
                FitsFactory.setLongStringsEnabled(false);
            }
        }
        else {
            dataGroup.setInitCapacity(data[0].length);
            for (int row = 0; row < data[0].length; row++) {
                DataObject aRow = new DataObject(dataGroup);
                aRow.setDataElement(dd[0], row);
                for (int dtIdx = 1; dtIdx < dd.length; dtIdx++) {
                    aRow.setDataElement(dd[dtIdx], data[dtIdx - 1][row]);
                }
                dataGroup.add(aRow);
            }
        }
        dataGroup.trimToSize();
    }


    private static void load1dRows(DataGroup dataGroup, double[] data , TableParseHandler handler) throws FitsException, IOException {
        if (handler!=null) {
            try {
                handler.start();
                handler.startTable(0);
                handler.header(dataGroup);
                for (int i = 0; (i < data.length); i++) {
                    handler.data(new Object[] {i, data[i]});
                }
            } catch (Exception e) {
                throw new IOException(e.toString(),e);
            } finally {
                handler.endTable(0);
                handler.end();
                FitsFactory.setLongStringsEnabled(false);
            }
        }
        else {
            DataType[] dd= dataGroup.getDataDefinitions();;
            dataGroup.setInitCapacity(data.length);
            for (int i = 0; (i < data.length); i++) {
                DataObject aRow = new DataObject(dataGroup);
                aRow.setDataElement(dd[0], i);
                aRow.setDataElement(dd[1], data[i]);
                dataGroup.add(aRow);
            }
        }
        dataGroup.trimToSize();
    }


    private static void logTableReadError(String fitsFilename, int tableIdx, String reason) {
        logger.error("Unable to get table from fits file: " + fitsFilename +
                ", HDU#: " + tableIdx + ", reason: "+reason);
    }

    //This function is loosely based on the packagedType function from the FitsStarTable class (uk.ac.starlink.fits package)
    private static Class<?> getClassType(Object base, boolean isScaled, String colFormat) {
        if (base == null) {
            if (isScaled) return Double.class;
            return getClassByTform(colFormat);
        } else {
            Class<?> cls = base.getClass().getComponentType();
            if (cls != null && Array.getLength(base) == 1) {
                if (isScaled) {
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
     * @param tableIdx the hdu index to read
     * @return DataGroup converted from the hdu
     * @throws IOException thrown if error reading table entry
     * @throws FitsException call to convertHDUToDataType may throw FitsException
     */

    private static DataGroup doReadFitsTable(BasicHDU<?> hdu,
                                             String fitsFilename,
                                             int tableIdx) throws IOException, FitsException {

        if (!(hdu instanceof TableHDU<?> hduTable)) {
            logTableReadError(fitsFilename,tableIdx,"HDU is not a table hdu");
            return null;
        }

        FitsFactory.setLongStringsEnabled(false);
        try {
            var fitsTableReadInfo= doReadFitsTableHeader(hduTable,fitsFilename);
            var dataGroup= fitsTableReadInfo.dataGroup();
            var evaluator= fitsTableReadInfo.evaluator();
            int totalRows = hduTable.getNRows();
            DataType[] dataDefinitions= dataGroup.getDataDefinitions();
            hduTable.getKernel();
            for (int row = 0; row < totalRows; row++){
                addRowToDG(dataGroup, dataDefinitions, row, hduTable, evaluator);
            }
            dataGroup.trimToSize();
            return dataGroup;
        } finally {
            FitsFactory.setLongStringsEnabled(false);
        }
    }

    private static void doIngestFitsTable(TableParseHandler handler,
                                          TableHDU<?> hduTable,
                                          String fitsFilename,
                                          int tableIdx,
                                          boolean spectrumHint) throws IOException, FitsException {

        try {
            FitsFactory.setLongStringsEnabled(false);
            var fitsTableReadInfo= doReadFitsTableHeader(hduTable,fitsFilename);
            var dataGroup= fitsTableReadInfo.dataGroup();
            var evaluator= fitsTableReadInfo.evaluator();
            int totalRows = hduTable.getNRows();
            int nCol = hduTable.getNCols();
            SpectrumMetaInspector.searchForSpectrum(dataGroup,hduTable, spectrumHint);
            dataGroup.trimToSize();

            handler.start();
            handler.startTable(0);
            handler.header(dataGroup);

            // makeRowExtractor() resolves getValAsObject()'s reflection-based dispatch once per
            // column instead of once per cell (nRow x nCol times).
            RowExtractor[] extractors = new RowExtractor[nCol];
            for (int icol = 0; icol < nCol; icol++) {
                extractors[icol] = makeRowExtractor(hduTable.getColumn(icol), evaluator[icol]);
            }

            for (int row = 0; row < totalRows; row++) {
                try {
                    Object[] outRow = new Object[nCol];
                    for (int icol = 0; icol < nCol; icol++) {
                        outRow[icol] = extractors[icol].get(row);
                    }
                    handler.data(outRow);
                } catch (Exception e) {
                    logger.error("Unable to read table row:" + row + "   msg:" + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new IOException(e.toString(),e);
        } finally {
            handler.endTable(0);
            handler.end();
            FitsFactory.setLongStringsEnabled(false);
        }
    }




    private static FitsTableReadInfo doReadFitsTableHeader(TableHDU<?> hduTable, String title) throws IOException, FitsException {

        int colCount = hduTable.getNCols();
        Class<?>[] bases = new Class[colCount];
        String[] colNames = new String[colCount];

        int nRow = hduTable.getNRows();
        int nCol = hduTable.getNCols();

        EvalVal[] evaluator = new EvalVal[nCol];
        try {
            for (int icol = 0; icol < nCol; ++icol) {
                colNames[icol] = hduTable.getColumnName(icol);

                String tscal = hduTable.getColumnMeta(icol, "TSCAL");
                String tzero = hduTable.getColumnMeta(icol, "TZERO");
                double zeroval;
                double scale=1.0;
                long blank=0;
                boolean hasBlank=false;
                boolean isScaled = false;
                double zeros= 0;
                if (tscal != null) {
                    zeroval = Double.parseDouble(tscal);
                    scale = zeroval;
                }
                if (tzero != null) {
                    zeroval = Double.parseDouble(tzero);
                    zeros = zeroval;
                }
                if (scale != 1.0 || zeros != 0.0) {
                    isScaled = true;
                }

                String blankKey = "TNULL" + (icol + 1);
                if (hduTable.getHeader().containsKey(blankKey)) {
                    blank = hduTable.getHeader().getLongValue(blankKey); //hduTable.getBlankValue();
                    hasBlank= true;
                }

                Object entry = null;
                try {
                    for (int irow = 0; entry == null && irow < nRow; ++irow) {
                        entry = hduTable.getElement(irow, icol);
                    }
                } catch (Exception e) {
                    throw new IOException("Error reading table entry");
                }

                bases[icol] =
                        hduTable.getNRows()==0 ? getClassByTform(hduTable.getColumnFormat(icol)) :
                                getClassType(entry, isScaled, hduTable.getColumnFormat(icol));
                evaluator[icol] = new EvalVal(blank, isScaled, hasBlank, scale, zeros);
            }
        }
        catch (NumberFormatException e) {
            logger.error("Number format exception reading column meta: " + e.getMessage());
        }
        //creating DataType list ... column info
        ArrayList<DataType> dataTypes = new ArrayList<>();

        for (int colIdx = 0; colIdx < colCount; colIdx++) {
            DataType dt = convertHDUToDataType(colNames, bases, hduTable, colIdx);
            dataTypes.add(dt);
        }

        DataGroup dataGroup = new DataGroup(title, dataTypes);
        // creating DataGroup rows.
        dataGroup.setInitCapacity(nRow);

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

        for (Map.Entry<String, String> entry : headerParams.entrySet()) {
            String n = entry.getKey();
            String v = entry.getValue();
            dataGroup.getTableMeta().addKeyword(n, v); //should keywords be in order as they appear in the Table Header?
        }
        return   new FitsTableReadInfo(dataGroup,evaluator);
    }

    record EvalVal(long blank, boolean scaled, boolean hasBlank, double scale, double zero) {
        public Number evalValue(Number val) {
            if (hasBlank && val.doubleValue() == blank) return null;
            if (!scaled) return val;
            return val.doubleValue() * scale + zero;
        }
    }

    @FunctionalInterface
    private interface RowExtractor {
        Object get(int row);
    }

    /**
     * Resolves how to pull a single row's value out of a whole-column array returned by
     * {@link TableHDU#getColumn}, once per column rather than once per cell.
     * scale/blank handling below mirrors getValAsObject().
     */
    private static RowExtractor makeRowExtractor(Object col, EvalVal ev) {
        return switch (col) {
            case double[]  a -> row -> ev.evalValue(a[row]);
            case float[]   a -> row -> ev.evalValue(a[row]);
            case int[]     a -> row -> ev.evalValue(a[row]);
            case long[]    a -> row -> ev.evalValue(a[row]);
            case short[]   a -> row -> ev.evalValue((int) a[row]);
            case byte[]    a -> row -> ev.evalValue((int) a[row]);
            case boolean[] a -> row -> a[row];
            case String[]  a -> row -> isEmpty(a[row]) ? null : a[row];
            case Object[]  a -> row -> ArrayFuncs.flatten(a[row]);     // vector-valued column (repeat > 1)
            default -> throw new IllegalStateException("Unrecognized FITS column type: " + col.getClass());
        };
    }

    //This function is loosely based on the packageValue function from the FitsStarTable class in the uk.ac.starlink.fits package
    private static Object getValAsObject(Object elem, int icol, EvalVal[] evaluator) throws FitsException {
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
                return switch (cls) {
                    case "byte" -> evaluator[icol].evalValue((int)((byte[])(elem))[0]);
                    case "short" -> evaluator[icol].evalValue((int)((short[])(elem))[0]);
                    case "int" -> evaluator[icol].evalValue(((int[])(elem))[0]);
                    case "long" -> evaluator[icol].evalValue(((long[])(elem))[0]);
                    case "float" -> evaluator[icol].evalValue(((float[])(elem))[0]);
                    case "double" -> evaluator[icol].evalValue(((double[])(elem))[0]);
                    case "boolean" -> ((boolean[]) (elem))[0];
                    case "class java.lang.String" -> isEmpty(((String[])(elem))[0]) ? null : ((String[])(elem))[0];
                    default -> throw new FitsException( "Unrecognized class type in FITS table file entry: " + cls);
                };
            }
            else {
                return ArrayFuncs.flatten(elem);
            }
    }






    private static void addRowToDG(DataGroup dataGroup, DataType[] dataDefinitions, int rowIdx, TableHDU<?> hduTable,
                                   EvalVal[] evaluator) throws FitsException {
        DataObject aRow = new DataObject(dataGroup);
        try {
            Object[] rowData= hduTable.getRow(rowIdx);
            for (int dtIdx = 0; dtIdx < dataGroup.getDataDefinitions().length; dtIdx++) {
                //so cast the val object to an array of its type by calling the getValAsObject function
                Object unpackedVal = getValAsObject(rowData[dtIdx], dtIdx, evaluator);
                aRow.setDataElement(dataDefinitions[dtIdx], unpackedVal);
            }
            dataGroup.add(aRow);
        } catch (Exception e) {
            logger.error("Unable to read table row:" + rowIdx + "   msg:" + e.getMessage());
        }
    }

    private static Class<?> formatClass(Class<?> c) throws FitsException {
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
     * see <a href="http://archive.stsci.edu/fits/fits_standard/node69.html#SECTION001232060000000000000">...</a>
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