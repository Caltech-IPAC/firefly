/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.WorldPt;
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
import uk.ac.starlink.fits.FitsStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    public static void main(String[] args)
    {
        String fits_filename = "/Users/ejoliet/devspace/branch/dev/firefly/test-array.fits";//args[0];

        Fits fits= null;
        try {
            fits = new Fits(fits_filename);
            BasicHDU<?>[] hdus = FitsReadUtil.readHDUs(fits);
            StarTable table = getStarTable(hdus[1], fits_filename, 1);
            DataGroup data = FITSTableReader.convertFitsToDataGroup(fits_filename, null, FITSTableReader.DEFAULT, 1);

            int columnCount = table.getColumnCount();
            for (int a= 0; a<columnCount;a++) {
                ColumnInfo columnInfo = table.getColumnInfo(a);
                int[] shape = columnInfo.getShape();
                System.out.println(columnInfo.getName() + " shape length " + shape.length);
            }
        } catch (FitsException | IOException | NullPointerException e) {
            e.printStackTrace();
        }
        finally {
            FitsReadUtil.closeFits(fits);
        }
        System.exit(0);
    }


    public static DataGroup convertFitsToDataGroup(String fits_filename, Map<String,String> metaInfo,
                                                   String strategy, int table_idx) throws FitsException, IOException {
        return convertFitsToDataGroup(fits_filename,null,null,strategy,metaInfo,table_idx);
    }

    /**
     * Convert a table from a FITS file to DataGroup based on table index
     * @param fits_filename the file name
     * @param dataCols: The names of the columns which will be copied to the data section of the DataGroups.
     *                If dataCols = null, get all the columns into the data group.
     * @param headerCols: The names of the columns which will be copied to the header section of the DataGroups.
     *                If headerCols = null, get none of the columns into the header of the data group.
     * @param strategy The strategy used to deal with the repeat count  of the data in the given dataCols columns.
     * @param table_idx table index, i.e. HDU number in FITS
     * @return the data group
     */
    public static DataGroup convertFitsToDataGroup(String fits_filename,
                                                         String[] dataCols,
                                                         String[] headerCols,
                                                         String strategy,
                                                         Map<String,String> metaInfo,
                                                         int table_idx) throws FitsException, IOException {


        Fits fits= null;
        try {
            // disable long string for HeaderCard creation while collecting table with table_idx from StarTableFactory to work around
            // the exception error sent from nom.tam.fits.
            FitsFactory.useThreadLocalSettings(true);
            DataGroup dg;
            fits = new Fits(fits_filename);
            BasicHDU<?>[] hdus = FitsReadUtil.readHDUs(fits);

            if (table_idx >= hdus.length) {
                throw new FitsException( "table index of " +table_idx+" exceeds the number of HDUS " + hdus.length);
            }
            BasicHDU<?> hdu= hdus[table_idx];

            if (table_idx==0) { //FITS tables are not at 0, if at zero try to read it as an image first
                dg= getFitsImageAsTable(hdu,metaInfo);
                if (dg==null && hdus.length>1) {
                    FitsFactory.setLongStringsEnabled(false);
                    StarTable table = getStarTable(hdus[1], fits_filename, table_idx); // this is really a hack, maybe it should not be here.
                    dg= convertStarTableToDataGroup(table, dataCols, headerCols, strategy);
                }
            }
            else { // if >0 then try to read it as a FITS table first.
                FitsFactory.setLongStringsEnabled(false);
                StarTable table = getStarTable(hdu, fits_filename, table_idx);
                FitsFactory.setLongStringsEnabled(true);
                dg=  (table!=null) ?
                        convertStarTableToDataGroup(table, dataCols, headerCols, strategy) :
                        getFitsImageAsTable(hdu,metaInfo);
            }
            String dataTypeHint= metaInfo !=null ? metaInfo.getOrDefault(MetaConst.DATA_TYPE_HINT,"").toLowerCase() : "";
            SpectrumMetaInspector.searchForSpectrum(dg,hdus[table_idx], dataTypeHint.equals("spectrum"));
            return dg;
        } catch (FitsException|IOException e) {
            logTableReadError(fits_filename,table_idx,e.getMessage());
            throw e;
        } finally {
            FitsFactory.useThreadLocalSettings(false);
            FitsReadUtil.closeFits(fits);
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

    private static String makeKeyforHDUTab(FitsReadUtil.ExtractionResults result) {
        return result.extName()!=null ? result.extName() : "HDU#"+result.hduNum();
    }
    private static String makeMetaEntryForHDUs(List<FitsReadUtil.ExtractionResults> results) {
        StringBuilder str= new StringBuilder();
        for(FitsReadUtil.ExtractionResults r : results) {
           if (str.length()>0) str.append(";");
           str.append(makeKeyforHDUTab(r)).append("=").append(r.hduNum());
        }
        return str.toString();
    }


    private static void insertZaxisSpectrumMeta(DataGroup dataGroup,
                                                List<FitsReadUtil.ExtractionResults> results,
                                                String wavelengthColName,
                                                String fluxColName ) {

        // guess an error
        String errCol= null;
        for(FitsReadUtil.ExtractionResults result: results) {
            String extName= result.extName();
            if (extName!=null && extName.toLowerCase().startsWith("err")) {
                errCol= makeKeyforHDUTab(result);
                break;
            }
        }
        SpectrumMetaInspector.createSpectrumMeta(dataGroup,wavelengthColName,fluxColName,errCol);
    }

    private static void insertZaxisGenericChartMeta(DataGroup dataGroup,
                                                    List<FitsReadUtil.ExtractionResults> results,
                                                    String xColName ) {
        String defYCol= "";
        for(FitsReadUtil.ExtractionResults result : results) {
            if (result.refHDU()) defYCol= makeKeyforHDUTab(result);
        }
        TableMeta meta= dataGroup.getTableMeta();
        meta.addKeyword(MetaConst.DEFAULT_CHART_X_COL, xColName);
        meta.addKeyword(MetaConst.DEFAULT_CHART_Y_COL, defYCol);
    }




    public static DataGroup getCubeZaxisAsTable(ImagePt pt, WorldPt wpt, String filename, int refHduNum,
                                            boolean allMatchingHDUs, int drillSize, double[] wlAry,
                                                String wlUnit, Map<Integer,String> fluxUnit)
            throws IOException, FitsException {
        File f= ServerContext.convertToFile(filename);
        List<FitsReadUtil.ExtractionResults> results= FitsReadUtil.getAllZAxisAryFromRelatedCubes(
                pt, f, refHduNum, allMatchingHDUs, drillSize );
        ArrayList<DataType> dataTypes = new ArrayList<>();
        int len= results.get(0).aryData().length;
        dataTypes.add(new DataType("plane","Plane", Integer.class));
        if (wlAry!=null) {
            DataType wlDt= new DataType("wavelength",Double.class, "Wavelength", wlUnit, null, null);
            dataTypes.add(wlDt);
        }
        String refKey= null;
        for(FitsReadUtil.ExtractionResults result : results) {
            String desc= result.extName()!=null ? result.extName() : "HDU# "+result.hduNum();
            String key= makeKeyforHDUTab(result);
            String u= fluxUnit.get(result.hduNum());
            DataType dt = new DataType(key,Double.class, desc, u, null, null);
            if (result.refHDU()) refKey= key;

            dataTypes.add(dt);
        }
        DataGroup dataGroup = new DataGroup("Cube Z-Axis", dataTypes);
        for (int i = 0; (i < len); i++) {
            DataObject aRow = new DataObject(dataGroup);
            aRow.setDataElement("plane",i+1);
            if (wlAry!=null) aRow.setDataElement("wavelength",rnd(wlAry[i],7));
            for(FitsReadUtil.ExtractionResults result : results) {
                aRow.setDataElement(makeKeyforHDUTab(result),result.aryData()[i]);
            }
            dataGroup.add(aRow);
        }
        TableMeta meta= dataGroup.getTableMeta();
        meta.addKeyword(MetaConst.FITS_IM_PT, pt.toString());
        if (wpt!=null) meta.addKeyword(MetaConst.FITS_WORLD_PT, wpt.toString());
        meta.addKeyword(MetaConst.FITS_IMAGE_HDU, makeMetaEntryForHDUs(results));
        meta.addKeyword(MetaConst.FITS_FILE_PATH, ServerContext.replaceWithPrefix(f));
        meta.addKeyword(MetaConst.FITS_EXTRACTION_TYPE, "z-axis");
        if (wlAry!=null && wlAry.length>0) {
            insertZaxisSpectrumMeta(dataGroup, results, "wavelength", refKey);
        }
        else {
            insertZaxisGenericChartMeta(dataGroup, results, wlAry!=null?"wavelength":"plane");
        }
        return dataGroup;
    }

    private static double minValue(ImagePt pt1,ImagePt pt2) {
        return usingXAxis(pt1,pt2) ? Math.min(pt1.getX(),pt2.getX()) : Math.min(pt1.getY(),pt2.getY());
    }

    private static boolean usingXAxis(ImagePt pt1,ImagePt pt2) {
        double deltaX = Math.abs(pt2.getX() - pt1.getX());
        double deltaY = Math.abs(pt2.getY() - pt1.getY());
        return (deltaX > deltaY);
    }


    public static DataGroup getPointsAsTable(ImagePt[] ptAry, WorldPt[] wptAry, String filename, int refHduNum, int plane,
                                             boolean allMatchingHDUs, int drillSize) throws IOException, FitsException {
        File f= ServerContext.convertToFile(filename);
        List<FitsReadUtil.ExtractionResults> results= FitsReadUtil.getAllPointsFromRelatedHDUs(
                ptAry, f, refHduNum, plane, allMatchingHDUs, drillSize );
        ArrayList<DataType> dataTypes = new ArrayList<>();
        int len= results.get(0).aryData().length;
        dataTypes.add(new DataType("x", Integer.class, "x", "pixel", null, null));
        dataTypes.add(new DataType("y", Integer.class, "y", "pixel", null, null));
        boolean hasWpt= wptAry!=null && wptAry.length==ptAry.length;
        if (hasWpt) {
            dataTypes.add(new DataType("ra",Double.class, "ra", "deg", null, null));
            dataTypes.add(new DataType("dec",Double.class, "dec","deg", null, null ));
        }
        String defYCol= "";
        for(FitsReadUtil.ExtractionResults result : results) {
            String desc= result.extName()!=null ? result.extName() : "HDU# "+result.hduNum();
            String key= makeKeyforHDUTab(result);
            String bunit= result.header().getStringValue("BUNIT");
            DataType dt = new DataType(key,Double.class, desc, bunit, null,null);
            if (result.refHDU()) defYCol= key;
            dataTypes.add(dt);
        }
        DataGroup dataGroup = new DataGroup("Cube Z-Axis", dataTypes);
        for (int i = 0; (i < len); i++) {
            DataObject aRow = new DataObject(dataGroup);
            aRow.setDataElement("x",(int)Math.rint(ptAry[i].getX()));
            aRow.setDataElement("y",(int)Math.rint(ptAry[i].getY()));
            if (hasWpt) {
                aRow.setDataElement("ra",rnd(wptAry[i].getX(),7));
                aRow.setDataElement("dec",rnd(wptAry[i].getY(),7));
            }
            for(FitsReadUtil.ExtractionResults result : results) {
                aRow.setDataElement(makeKeyforHDUTab(result),result.aryData()[i]);
            }
            dataGroup.add(aRow);
        }
        TableMeta meta= dataGroup.getTableMeta();
        meta.addKeyword(MetaConst.FITS_IMAGE_HDU, makeMetaEntryForHDUs(results));
        if (plane>0) meta.addKeyword(MetaConst.FITS_IMAGE_HDU_CUBE_PLANE, plane+"");
        if (hasWpt) meta.addKeyword(MetaConst.CENTER_COLUMN, "ra;dec;J2000");
        meta.addKeyword(MetaConst.IMAGE_COLUMN, "x;y");
        meta.addKeyword(MetaConst.CATALOG_OVERLAY_TYPE, hasWpt ? "TRUE" : "IMAGE_PTS");
        meta.addKeyword(MetaConst.FITS_FILE_PATH, ServerContext.replaceWithPrefix(f));
        meta.addKeyword(MetaConst.FITS_EXTRACTION_TYPE, "points");
        meta.addKeyword(MetaConst.DEFAULT_CHART_X_COL, "x");
        meta.addKeyword(MetaConst.DEFAULT_CHART_Y_COL, defYCol);
        dataGroup.trimToSize();
        return dataGroup;
    }


    private static double rnd(double d, int decimalPlaces) {
        double factor= Math.pow(10,decimalPlaces);
        return Math.round(d*factor)/factor;
    }


    public static DataGroup getLineSelectAsTable(ImagePt[] ptAry, WorldPt[] wptAry, String filename, int refHduNum, int plane,
                                                boolean allMatchingHDUs, int drillSize) throws IOException, FitsException {
        File f= ServerContext.convertToFile(filename);
        List<FitsReadUtil.ExtractionResults> results= FitsReadUtil.getAllPointsFromRelatedHDUs(
                ptAry, f, refHduNum, plane, allMatchingHDUs, drillSize );
        ArrayList<DataType> dataTypes = new ArrayList<>();
        int len= results.get(0).aryData().length;
        boolean hasWpt= wptAry!=null && wptAry.length==ptAry.length;
        if (hasWpt) {
            dataTypes.add(new DataType("offset", Double.class, "offset", "arcsec", null, null));
            dataTypes.add(new DataType("ra",Double.class, "ra", "deg", null, null));
            dataTypes.add(new DataType("dec",Double.class, "dec","deg", null, null ));
        }
        dataTypes.add(new DataType("pixOffset",Double.class, "pixOffset", "pixel",null, null ));
        dataTypes.add(new DataType("x", Integer.class, "x", "pixel", null, null));
        dataTypes.add(new DataType("y", Integer.class, "y", "pixel", null, null));
        String defYCol= "";
        for(FitsReadUtil.ExtractionResults result : results) {
            String desc= result.extName()!=null ? result.extName() : "HDU# "+result.hduNum();
            String key= makeKeyforHDUTab(result);
            String bunit= result.header().getStringValue("BUNIT");
            DataType dt = new DataType(key,Double.class, desc, bunit, null,null);
            if (result.refHDU()) defYCol= key;
            dataTypes.add(dt);
        }
        DataGroup dataGroup = new DataGroup("Cube Z-Axis", dataTypes);
        for (int i = 0; (i < len); i++) {
            DataObject aRow = new DataObject(dataGroup);
            aRow.setDataElement("pixOffset",rnd(VisUtil.computeDistance(ptAry[0],ptAry[i]), 1));
            aRow.setDataElement("x",(int)Math.rint(ptAry[i].getX()));
            aRow.setDataElement("y",(int)Math.rint(ptAry[i].getY()));
            if (hasWpt) {
                aRow.setDataElement("offset",rnd(VisUtil.computeDistance(wptAry[0],wptAry[i])*3600,3));
                aRow.setDataElement("ra",rnd(wptAry[i].getX(),7));
                aRow.setDataElement("dec",rnd(wptAry[i].getY(),7));
            }
            for(FitsReadUtil.ExtractionResults result : results) {
                aRow.setDataElement(makeKeyforHDUTab(result),result.aryData()[i]);
            }
            dataGroup.add(aRow);
        }
        TableMeta meta= dataGroup.getTableMeta();
        meta.addKeyword(MetaConst.FITS_IM_PT, ptAry[0].toString());
        meta.addKeyword(MetaConst.FITS_IM_PT2, ptAry[ptAry.length-1].toString());
        meta.addKeyword(MetaConst.FITS_IMAGE_HDU, makeMetaEntryForHDUs(results));
        if (plane>0) meta.addKeyword(MetaConst.FITS_IMAGE_HDU_CUBE_PLANE, plane+"");
        meta.addKeyword(MetaConst.FITS_FILE_PATH, ServerContext.replaceWithPrefix(f));
        meta.addKeyword(MetaConst.FITS_EXTRACTION_TYPE, "line");
        meta.addKeyword(MetaConst.DEFAULT_CHART_X_COL, wptAry!=null ? "offset" : "pixOffset");
        meta.addKeyword(MetaConst.DEFAULT_CHART_Y_COL, defYCol);
        meta.addKeyword(MetaConst.IMAGE_COLUMN, "x;y");
        meta.addKeyword(MetaConst.CATALOG_OVERLAY_TYPE, hasWpt ? "TRUE" : "IMAGE_PTS");

        dataGroup.trimToSize();
        return dataGroup;
    }





    private static DataGroup getFitsImageAsTable(BasicHDU<?> hdu,
                                                 Map<String,String> metaInfo) throws FitsException, IOException {

        // todo try to read it as an image
        String[] colNames= new String[]{"index"};
        String[] colUnits= null;
        if (metaInfo!=null) {
            String colNameStr = metaInfo.get(MetaConst.IMAGE_AS_TABLE_COL_NAMES);
            if (colNameStr != null && colNameStr.length() > 1) colNames =colNameStr.split(",");
            String colUnitsStr = metaInfo.get(MetaConst.IMAGE_AS_TABLE_UNITS);
            if (colUnitsStr != null && colUnitsStr.length() > 1) colUnits =colUnitsStr.split(",");
        }

        if ((hdu instanceof ImageHDU) || (hdu instanceof CompressedImageHDU || hdu instanceof UndefinedHDU)) {
            Header header = hdu.getHeader();
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
            if (colUnits!=null) idxDT.setUnits(colUnits[0]);
            dataTypes.add(idxDT);

            if (is1dImage(hdu)) {
                double[] data = FitsReadUtil.getImageHDUDataInDoubleArray(hdu);
                if (data == null) return null;

                //------------- here -------------------

                String cName = (colNames.length > 1) ? colNames[1] : "data";
                DataType dataDT = new DataType(cName, cName, Double.class);
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
            } else if (naxis == 2 && naxis2 > 0) {
                double[][] data = null;
                if (naxis2 > 30) return null; // right now we only support 30 columns, this could be a parameter
                if ((hdu instanceof ImageHDU) || (hdu instanceof CompressedImageHDU)) {
                    ImageHDU imageHDU = (hdu instanceof CompressedImageHDU) ?
                            ((CompressedImageHDU) hdu).asImageHDU() : (ImageHDU) hdu;
                    ImageData imageDataObj = imageHDU.getData();
                    data = (double[][]) ArrayFuncs.convertArray(imageDataObj.getData(), Double.TYPE, true);
                } else if (hdu instanceof UndefinedHDU) {
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

    /**
     * Get a StarTable from a FITS file by giving table index, i.e. HDU number in FITS
     * @param hdu hdu to use for reading
     * @param fitsFilename fits filename string
     * @param tableIdx the hdu index to read
     * @return one star table or none
     * @throws IOException when the file can't be read
     */
    private static StarTable getStarTable(BasicHDU<?> hdu, String fitsFilename, int tableIdx) throws FitsException, IOException {

        if (!(hdu instanceof TableHDU)) {
            logTableReadError(fitsFilename,tableIdx,"HDU is not a table hdu");
            return null;
        }
        Header h= hdu.getHeader();

        String zValue= h.containsKey("ZIMAGE") ? h.getStringValue("ZIMAGE").toUpperCase() : "";
        if (zValue.equals("T") || zValue.equals("TRUE")) {
            logTableReadError(fitsFilename,tableIdx,"HDU is an compressed image stored at a table");
            return null;
        }

        FitsStarTable starTable= new FitsStarTable((TableHDU<?>)hdu);

        Iterator<HeaderCard> i= h.iterator();
        for(HeaderCard card= i.next();i.hasNext(); card= i.next()) {
            starTable.setParameter( new DescribedValue(
                    new DefaultValueInfo(card.getKey(), String.class), card.getValue()) );
        }
        return starTable;
    }

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
    private static DataGroup convertStarTableToDataGroup(StarTable table,
                                                        String[] inclCols,
                                                        String[] inclHeaders,
                                                        String strategy)
            throws FitsException, IllegalArgumentException, IOException {

        if (table==null) return null;
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
        dataGroup.setInitCapacity((int)table.getRowCount());
        for (long row = 0; row < table.getRowCount(); row++){
            addRowToDataGroup(table, dataGroup, colIdxMap, row, strategy);
        }

        // setting DataGroup meta info
        for(int colIdx = 0; colIdx < dataTypes.size(); colIdx++) {
            DataType dt = dataTypes.get(colIdx);
            String format = getParam(table, "TDISP" + (colIdx + 1));
            convertFormat(format, dt);
        }

        List<String> hdList = inclHeaders == null ? null : Arrays.asList(inclHeaders);
        List<?> params = table.getParameters();
        if (params != null) {
            for (Object p : params) {
                if (p instanceof DescribedValue) {
                    DescribedValue dv = (DescribedValue) p;
                    String n = dv.getInfo().getName();
                    String v = dv.getValueAsString(Integer.MAX_VALUE);
                    if (hdList == null || hdList.contains(n)) {
                        dataGroup.getTableMeta().addKeyword(n, v);
                    }
                }
            }
        }
        dataGroup.trimToSize();
        return dataGroup;
    }

    private static void addRowToDataGroup(StarTable table, DataGroup dataGroup, LinkedHashMap<ColumnInfo, Integer> colIdxMap, long rowIdx, String strategy) {
        DataObject aRow = new DataObject(dataGroup);
        ColumnInfo[] colAry = colIdxMap.keySet().toArray(new ColumnInfo[0]);
        try {
            for (int dtIdx = 0; dtIdx < dataGroup.getDataDefinitions().length; dtIdx++) {
                DataType dt = dataGroup.getDataDefinitions()[dtIdx];
                ColumnInfo colInfo = colAry[dtIdx];
                int colIdx = colIdxMap.get(colInfo);
                Object data =  table.getCell(rowIdx, colIdx);
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
     * @param table the star tabl3
     * @param colIdx index
     * @param strategy strategy string
     * @return dataType
     */
    private static DataType convertToDataType(StarTable table, int colIdx, String strategy)
        throws FitsException{

        ColumnInfo colInfo = table.getColumnInfo(colIdx);
        String colName = colInfo.getName()!=null ? colInfo.getName() : "column-"+colIdx;
        String classType = DefaultValueInfo.formatClass(colInfo.getContentClass());
        String unit = colInfo.getUnitString();
        String desc = colInfo.getDescription();
        desc = desc == null ? getParam(table, "TDOC" + (colIdx+1)) : desc; // this is for LSST.. not sure it applies to others.

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
        } else if ((classType.contains("double")) || (classType.contains("Double"))) {
            java_class = Double.class;
        } else if ((classType.contains("char")) || (classType.contains("String") || classType.contains("Character"))) {
            java_class = String.class;
        } else {
            throw new FitsException(
                    "Unrecognized format character in FITS table file: " + classType);
        }

        if (colInfo.isArray()) {
            // FitsStarTable from starlink has an issue, TDIM values are not trimed before parsing into integer, hence Shape always int[]{-1}
            // Using here our own getter to overcome this problem (TOPCAT is showing the same problem in the UI)
            int[] shape = getShape(table, colIdx);
            String arraySize = Arrays.stream(shape)
                    .mapToObj(d -> d > 0 ? d+"" : "*")
                    .collect(Collectors.joining("x"));
            dataType.setArraySize(arraySize);
        }

        dataType.setDataType(java_class);
        dataType.setUnits(unit);
        dataType.setDesc(desc);
        dataType.setUCD(colInfo.getUCD());
        dataType.setUType(colInfo.getUtype());

        return dataType;
    }

    private static int[] getShape(StarTable table, int colIdx) {

        DescribedValue td = table.getParameterByName("TDIM" + (colIdx+1));
        String tdim = td == null ? null : td.getValue().toString();
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
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
        return new int[]{-1};
    }

    private static String getParam(StarTable table, String key) {
        DescribedValue p = table.getParameterByName(key);
        return p == null ? null : p.getValueAsString(Integer.MAX_VALUE);
    }

    /**
     * converts FITS table keyword TDISPn into firefly's precision/width attributes
     * see http://archive.stsci.edu/fits/fits_standard/node69.html#SECTION001232060000000000000
     * @param format a format string taken from TDISPn
     * @param dt     the column this format belongs to
     */
    private static void convertFormat(String format, DataType dt) {
        if (!StringUtils.isEmpty(format)) {
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

    /**
     * Fill a dataGroup with a row of data from a table.
     * Each dataObj contains data from all the columns at a repeat.
     * Convert the data type of each element from the star types to IPAC types if needed.
     * If strategy = EXPAND_BESET_FIT or EXPAND_REPEAT, fill in null or the last data value when no data is available in the column.
     *
     * @return dataGroup
     */
    private static DataGroup fillDataGroup(StarTable table,
                                           List<DataType> dataTypeList,
                                           int maxRepeat,
                                           long row,
                                           String[] dataCols,
                                           String strategy,
                                           DataGroup dataGroup) throws IOException{

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
            objNew = obj; // Q: Need to cast to Integer?
        }

        return objNew;
    }

    /**
     * Concatenate the element(s) in the obj and the divider "," to a String attributeValue.
     * Convert the data type of each element from the star types to IPAC types if needed.
     * @return attributeValue String
     */
    private static String getAttributeValue(Object obj,
                                            boolean isArray,
                                            String classType,
                                            String originalType) {
        String attributeValue;
        if (isArray){
            int attributeNum = Math.min(Array.getLength(obj), maxNumAttributeValues);
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

}