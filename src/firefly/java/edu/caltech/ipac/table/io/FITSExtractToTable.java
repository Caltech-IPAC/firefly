package edu.caltech.ipac.table.io;
/**
 * User: roby
 * Date: 10/13/22
 * Time: 4:18 PM
 */


import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.plotdata.FitsExtract;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class FITSExtractToTable {


    private static String makeKeyforHDUTab(FitsExtract.ExtractionResults result) {
        return result.extName()!=null ? result.extName() : "HDU#"+result.hduNum();
    }

    private static String makeMetaEntryForHDUs(List<FitsExtract.ExtractionResults> results) {
        StringBuilder str= new StringBuilder();
        for(FitsExtract.ExtractionResults r : results) {
           if (str.length()>0) str.append(";");
           str.append(makeKeyforHDUTab(r)).append("=").append(r.hduNum());
        }
        return str.toString();
    }

    private static Number getFirstNonNaN(List<Number> valueList) {
        for(Number v: valueList) { // find first  non nan entry
            if (!Double.isNaN(v.doubleValue())) return v;
        }
        return valueList.get(0);
    }

    private static Class<?> getDataType(List<Number> valueList) {
        return getFirstNonNaN(valueList).getClass();
    }

    private static String addSize(String desc, int ptSizeX, int ptSizeY, FitsExtract.CombineType ct) {
        if (ptSizeX<2 && ptSizeY < 2) return desc;
        return desc+ " ("+ptSizeX+"x"+ptSizeY+","+ct.toString()+")";
    }

    private static double rnd(double d, int decimalPlaces) {
        double factor= Math.pow(10,decimalPlaces);
        return Math.round(d*factor)/factor;
    }

    private static void insertZaxisSpectrumMeta(DataGroup dataGroup,
                                                List<FitsExtract.ExtractionResults> results,
                                                String wavelengthColName,
                                                String fluxColName ) {

        // guess an error
        String errCol= null;
        for(FitsExtract.ExtractionResults result: results) {
            String extName= result.extName();
            if (extName!=null && extName.toLowerCase().startsWith("err")) {
                errCol= makeKeyforHDUTab(result);
                break;
            }
        }
        SpectrumMetaInspector.createSpectrumMeta(dataGroup,wavelengthColName,fluxColName,errCol);
    }

    private static void insertZaxisGenericChartMeta(DataGroup dataGroup,
                                                    List<FitsExtract.ExtractionResults> results,
                                                    String xColName ) {
        String defYCol= "";
        for(FitsExtract.ExtractionResults result : results) {
            if (result.refHDU()) defYCol= makeKeyforHDUTab(result);
        }
        TableMeta meta= dataGroup.getTableMeta();
        meta.addKeyword(MetaConst.DEFAULT_CHART_X_COL, xColName);
        meta.addKeyword(MetaConst.DEFAULT_CHART_Y_COL, defYCol);
    }

    public static DataGroup getCubeZaxisAsTable(ImagePt pt, WorldPt wpt, String filename, int refHduNum,
                                                boolean allMatchingHDUs, int ptSize, FitsExtract.CombineType ct,
                                                double[] wlAry, String wlUnit, Map<Integer,String> fluxUnit)
            throws IOException, FitsException {
        File f= ServerContext.convertToFile(filename);
        List<FitsExtract.ExtractionResults> results= FitsExtract.getAllZAxisAryFromRelatedCubes(
                pt, f, refHduNum, allMatchingHDUs, ptSize, ct);
        ArrayList<DataType> dataTypes = new ArrayList<>();
        int len= results.get(0).aryData().size();
        dataTypes.add(new DataType("plane","Plane", Integer.class));
        if (wlAry!=null) {
            DataType wlDt= new DataType("wavelength",Double.class, "Wavelength", wlUnit, null, null);
            dataTypes.add(wlDt);
        }
        String refKey= null;
        for(FitsExtract.ExtractionResults result : results) {
            String desc= result.extName()!=null ? result.extName() : "HDU# "+result.hduNum();
            desc= addSize(desc,ptSize,ptSize,ct);
            String key= makeKeyforHDUTab(result);
            String u= fluxUnit.get(result.hduNum());
            Class<?> dataType= getDataType(result.aryData());
            DataType dt = new DataType(key,dataType, desc, u, null, null);
            if (result.refHDU()) refKey= key;

            dataTypes.add(dt);
        }
        DataGroup dataGroup = new DataGroup("Cube Z-Axis", dataTypes);
        for (int i = 0; (i < len); i++) {
            DataObject aRow = new DataObject(dataGroup);
            aRow.setDataElement("plane",i+1);
            if (wlAry!=null) aRow.setDataElement("wavelength",rnd(wlAry[i],7));
            for(FitsExtract.ExtractionResults result : results) {
                aRow.setDataElement(makeKeyforHDUTab(result),result.aryData().get(i));
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

    public static DataGroup getPointsAsTable(ImagePt[] ptAry, WorldPt[] wptAry, String filename, int refHduNum, int plane,
                                             boolean allMatchingHDUs, int ptSizeX, int ptSizeY, FitsExtract.CombineType ct,
                                             double[] wlAry, String wlUnit)
            throws IOException, FitsException {
        File f= ServerContext.convertToFile(filename);
        List<FitsExtract.ExtractionResults> results= FitsExtract.getAllPointsFromRelatedHDUs(
                ptAry, f, refHduNum, plane, allMatchingHDUs, ptSizeX, ptSizeY, ct );
        ArrayList<DataType> dataTypes = new ArrayList<>();
        int len= results.get(0).aryData().size();
        dataTypes.add(new DataType("x", Integer.class, "x", "pixel", null, null));
        dataTypes.add(new DataType("y", Integer.class, "y", "pixel", null, null));
        boolean hasWpt= wptAry!=null && wptAry.length==ptAry.length;
        if (hasWpt) {
            dataTypes.add(new DataType("ra",Double.class, "ra", "deg", null, null));
            dataTypes.add(new DataType("dec",Double.class, "dec","deg", null, null ));
        }
        if (wlAry!=null) {
            dataTypes.add(new DataType("wavelength", Double.class, "wavelength", wlUnit, null, null));
        }
        String defYCol= "";
        for(FitsExtract.ExtractionResults result : results) {
            String desc= result.extName()!=null ? result.extName() : "HDU# "+result.hduNum();
            desc= addSize(desc,ptSizeX,ptSizeY,ct);
            String key= makeKeyforHDUTab(result);
            String bunit= FitsReadUtil.getBUnit(result.header());
            Class<?> dataType= getDataType(result.aryData());
            DataType dt = new DataType(key,dataType, desc, bunit, null,null);
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
            if (wlAry!=null) {
                aRow.setDataElement("wavelength",rnd(wlAry[i],6));
            }
            for(FitsExtract.ExtractionResults result : results) {
                aRow.setDataElement(makeKeyforHDUTab(result),result.aryData().get(i));
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


    public static DataGroup getLineSelectAsTable(ImagePt[] ptAry, WorldPt[] wptAry, String filename, int refHduNum, int plane,
                                                 boolean allMatchingHDUs, int ptSizeX, int ptSizeY, FitsExtract.CombineType ct,
                                                 double[] wlAry, String wlUnit )
            throws IOException, FitsException {
        File f= ServerContext.convertToFile(filename);
        List<FitsExtract.ExtractionResults> results= FitsExtract.getAllPointsFromRelatedHDUs(
                ptAry, f, refHduNum, plane, allMatchingHDUs, ptSizeX, ptSizeY, ct);
        ArrayList<DataType> dataTypes = new ArrayList<>();
        int len= results.get(0).aryData().size();
        boolean hasWpt= wptAry!=null && wptAry.length==ptAry.length;
        if (hasWpt) {
            dataTypes.add(new DataType("offset", Double.class, "offset", "arcsec", null, null));
            dataTypes.add(new DataType("ra",Double.class, "ra", "deg", null, null));
            dataTypes.add(new DataType("dec",Double.class, "dec","deg", null, null ));
        }
        dataTypes.add(new DataType("pixOffset",Double.class, "pixOffset", "pixel",null, null ));
        dataTypes.add(new DataType("x", Integer.class, "x", "pixel", null, null));
        dataTypes.add(new DataType("y", Integer.class, "y", "pixel", null, null));
        if (wlAry!=null) {
            dataTypes.add(new DataType("wavelength", Double.class, "wavelength", wlUnit, null, null));
        }
        String defYCol= "";
        for(FitsExtract.ExtractionResults result : results) {
            String desc= result.extName()!=null ? result.extName() : "HDU# "+result.hduNum();
            String key= makeKeyforHDUTab(result);
            String bunit= FitsReadUtil.getBUnit(result.header());
            Class<?> dataType= getDataType(result.aryData());
            FitsExtract.CombineType activeCt= ct;
            if (refHduNum!=result.hduNum() && (dataType==Long.class || dataType==Integer.class)) {
                activeCt= FitsExtract.CombineType.OR;
            }
            desc= addSize(desc,ptSizeX,ptSizeY,activeCt);
            DataType dt = new DataType(key,dataType, desc, bunit, null,null);
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
            if (wlAry!=null) {
                aRow.setDataElement("wavelength",rnd(wlAry[i],6));
            }
            for(FitsExtract.ExtractionResults result : results) {
                aRow.setDataElement(makeKeyforHDUTab(result),result.aryData().get(i));
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
}
