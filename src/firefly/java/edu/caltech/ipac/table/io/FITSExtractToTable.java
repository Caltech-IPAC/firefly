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
import java.util.Objects;

import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * @author Trey Roby
 */
public class FITSExtractToTable {

    private static String makeKeyByHDU(FitsExtract.ExtractionResults result, String prefix, int plane) {
        var planeStr= plane==0 ? "" : "/"+plane;
        return result.extName()!=null ?
                makeKey(prefix,result.extName()+planeStr) : makeKey(prefix,"HDU#"+result.hduNum()+planeStr);
    }

    private static String makeKeyByHDU(FitsExtract.ExtractionResults result) { return makeKeyByHDU(result,null,0); }

    private static String makeMetaEntryForHDUs(List<FitsExtract.ExtractionResults> results) {
        StringBuilder str= new StringBuilder();
        for(FitsExtract.ExtractionResults r : results) {
           if (!str.isEmpty()) str.append(";");
           str.append(makeKeyByHDU(r)).append("=").append(r.hduNum());
        }
        return str.toString();
    }

    private static Number getFirstNonNaN(List<Number> valueList) {
        for(Number v: valueList) { // find first  non nan entry
            if (!Double.isNaN(v.doubleValue())) return v;
        }
        return valueList.getFirst();
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

    private static String makePrefix(int extractTotal, String title) {
        return extractTotal==1 ? "" : title;
    }

    private static String makeKey(String prefix, String keyName) {
        return isEmpty(prefix) ? keyName : keyName+"("+prefix+")";
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
                errCol= makeKeyByHDU(result);
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
            if (result.refHDU()) defYCol= makeKeyByHDU(result);
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
        int len= results.getFirst().aryData().size();
        dataTypes.add(new DataType("plane","Plane", Integer.class));
        if (wlAry!=null) {
            DataType wlDt= new DataType("wavelength",Double.class, "Wavelength", wlUnit, null, null);
            dataTypes.add(wlDt);
        }
        String refKey= null;
        for(FitsExtract.ExtractionResults result : results) {
            String desc= result.extName()!=null ? result.extName() : "HDU# "+result.hduNum();
            desc= addSize(desc,ptSize,ptSize,ct);
            String key= makeKeyByHDU(result);
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
                aRow.setDataElement(makeKeyByHDU(result),result.aryData().get(i));
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

    private static Object getWithEnsuredType(Object obj, Class<?> type) {
        if (obj instanceof Number n) {
            if (type==Float.class) return n.floatValue();
            if (type==Double.class) return n.doubleValue();
            if (type==int.class) return n.intValue();
            if (type==long.class) return n.longValue();
        }
        return obj;
    }

    public record DataExtractParams(String title, ImagePt[] ptAry,
                                    String filename,
                                    int plane, int refHduNum, double[] wlAry, String wlUnit) {}

    public record TitledExtractionResult(String title, ImagePt[] ptAry, double[] wlAry, String wlUnit,
                                         List<FitsExtract.ExtractionResults> extractionResults,
                                         File file, int refHduNum, int plane) {}

    public static DataGroup getDataSelectAsTable(List<DataExtractParams> extractParamsList, WorldPt[] wptAry,
                                                 boolean allMatchingHDUs, int ptSizeX, int ptSizeY,
                                                 FitsExtract.CombineType ct, boolean isLine)
            throws IOException, FitsException {

        List<TitledExtractionResult> allExtractions=extractParamsList.stream().map( p -> {
                    try {
                        File f= ServerContext.convertToFile(p.filename);
                        List<FitsExtract.ExtractionResults> extractionResults= FitsExtract.getAllPointsFromRelatedHDUs(
                                p.ptAry, f, p.refHduNum, p.plane, allMatchingHDUs, ptSizeX, ptSizeY, ct);
                        return new TitledExtractionResult(p.title, p.ptAry, p.wlAry, p.wlUnit,
                                extractionResults, f, p.refHduNum, p.plane);
                    } catch (IOException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        boolean hasWpt= wptAry!=null && wptAry.length==extractParamsList.getFirst().ptAry.length;
        int totalRows= allExtractions.getFirst().extractionResults.getFirst().aryData().size();

        List<DataType> dataTypes = initDataColumns(allExtractions,hasWpt,ptSizeX,ptSizeY,ct,isLine);
        DataGroup dataGroup = new DataGroup("line extract", dataTypes);
        for (int i = 0; (i < totalRows); i++) {
            DataObject aRow = new DataObject(dataGroup);
            if (hasWpt) {
                if (isLine) aRow.setDataElement("offset",rnd(VisUtil.computeDistance(wptAry[0],wptAry[i])*3600,3));
                aRow.setDataElement("ra",rnd(wptAry[i].getX(),7));
                aRow.setDataElement("dec",rnd(wptAry[i].getY(),7));
            }
            for(var titleResult : allExtractions) {
                String title= makePrefix(allExtractions.size(),titleResult.title);
                var ptAry= titleResult.ptAry;
                int x= (int)Math.rint(ptAry[i].getX());
                int y= (int)Math.rint(ptAry[i].getY());
                if (isLine) aRow.setDataElement(makeKey(title,"pixOffset"),rnd(VisUtil.computeDistance(ptAry[0],ptAry[i]), 1));
                aRow.setDataElement(makeKey(title,"x"),x);
                aRow.setDataElement(makeKey(title,"y"),y);
                if (titleResult.wlAry!=null) aRow.setDataElement(makeKey(title,"wavelength"),rnd(titleResult.wlAry[i],6));
                for(FitsExtract.ExtractionResults result : titleResult.extractionResults) {
                    String key= makeKeyByHDU(result,title,titleResult.plane);
                    aRow.setDataElement(
                            key,
                            getWithEnsuredType(
                                    result.aryData().get(i),
                                    dataGroup.getDataDefintion(key).getDataType()
                            )
                    );
                }
            }
            dataGroup.add(aRow);
        }

        TableMeta meta= dataGroup.getTableMeta();
        var baseEx= allExtractions.getFirst();
        var ptAry= baseEx.ptAry;
        var baseTitle= makePrefix(allExtractions.size(),allExtractions.getFirst().title);

        String defYCol= "";
        var baseX= makeKey(baseTitle,"x");
        var baseY= makeKey(baseTitle,"y");
        var defXCol= !isLine ? baseX : wptAry!=null ? "offset" : "pixOffset"; //todo fix
        if (allExtractions.getFirst().extractionResults.getFirst().refHDU()) {
            var firstResult= allExtractions.getFirst();
            defYCol= makeKeyByHDU(firstResult.extractionResults.getFirst(), baseTitle, firstResult.plane);
        };


        var plane= extractParamsList.getFirst().plane;
        meta.addKeyword(MetaConst.FITS_IMAGE_HDU, makeMetaEntryForHDUs(baseEx.extractionResults));
        if (plane>0) meta.addKeyword(MetaConst.FITS_IMAGE_HDU_CUBE_PLANE, plane+"");
        if (hasWpt) meta.addKeyword(MetaConst.CENTER_COLUMN, "ra;dec;J2000");
        meta.addKeyword(MetaConst.FITS_FILE_PATH, ServerContext.replaceWithPrefix(baseEx.file));
        meta.addKeyword(MetaConst.FITS_EXTRACTION_TYPE, isLine ? "line" : "points");
        meta.addKeyword(MetaConst.DEFAULT_CHART_X_COL, defXCol);
        meta.addKeyword(MetaConst.DEFAULT_CHART_Y_COL, defYCol);
        meta.addKeyword(MetaConst.IMAGE_COLUMN, baseX+";"+baseY);
        meta.addKeyword(MetaConst.CATALOG_OVERLAY_TYPE, hasWpt ? "TRUE" : "IMAGE_PTS");
        if (isLine) {
            meta.addKeyword(MetaConst.FITS_IM_PT, ptAry[0].toString());
            meta.addKeyword(MetaConst.FITS_IM_PT2, ptAry[ptAry.length-1].toString());
        }

        dataGroup.trimToSize();
        return dataGroup;
    }


    public static List<DataType> initDataColumns(List<TitledExtractionResult> allExtractions,
                                                 boolean hasWpt,
                                                 int ptSizeX,
                                                 int ptSizeY,
                                                 FitsExtract.CombineType ct,
                                                 boolean isLine) {

        ArrayList<DataType> dataTypes = new ArrayList<>();
        if (hasWpt) {
            if (isLine) dataTypes.add(new DataType("offset", Double.class, "offset", "arcsec", null, null));
            dataTypes.add(new DataType("ra",Double.class, "ra", "deg", null, null));
            dataTypes.add(new DataType("dec",Double.class, "dec","deg", null, null ));
        }
        for(var titleResult : allExtractions) {
            String title= makePrefix(allExtractions.size(),titleResult.title);
            if (isLine) dataTypes.add(new DataType(makeKey(title,"pixOffset"),Double.class, makeKey(title,"pixOffset"), "pixel",null, null ));
            dataTypes.add(new DataType(makeKey(title,"x"), Integer.class, makeKey(title,"x"), "pixel", null, null));
            dataTypes.add(new DataType(makeKey(title,"y"), Integer.class, makeKey(title,"y"), "pixel", null, null));
            if (titleResult.wlAry!=null) {
                dataTypes.add(new DataType(makeKey(title,"wavelength"), Double.class, makeKey(title,"wavelength"), titleResult.wlUnit, null, null));
            }
            for(FitsExtract.ExtractionResults result : titleResult.extractionResults) {
                var plane= titleResult.plane+1;
                var planeStr= plane==1 ? "" : "/"+plane;
                String desc= result.extName()!=null
                        ? makeKey(title,result.extName()+planeStr)
                        : makeKey(title,"HDU# "+result.hduNum()+planeStr);
                String key= makeKeyByHDU(result,title,titleResult.plane);
                String bunit= FitsReadUtil.getBUnit(result.header());
                Class<?> dataType= getDataType(result.aryData());
                FitsExtract.CombineType activeCt= ct;
                if (titleResult.refHduNum!=result.hduNum() && (dataType==Long.class || dataType==Integer.class)) {
                    activeCt= FitsExtract.CombineType.OR;
                }
                desc= addSize(desc,ptSizeX,ptSizeY,activeCt);
                DataType dt = new DataType(key,dataType, desc, bunit, null,null);
                dataTypes.add(dt);
            }
        }
        return dataTypes;
    }

}