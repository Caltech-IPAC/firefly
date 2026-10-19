package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.servlets.HiPSRetrieve;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.FITSExtractToTable;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.plotdata.FitsExtract;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Extract data from an image and make a table
 */
@SearchProcessorImpl(id = ExtractFromImage.ID, params=
        {
                @ParamDoc(name="extractionType", desc="should be one of z-axis, line, or points"),
                @ParamDoc(name="pt", desc="image point"),
                @ParamDoc(name=ServerParams.WPT, desc="world point"),
                @ParamDoc(name="ptAry[i]", desc="for point selection"),
                @ParamDoc(name="wptAry", desc="for point selection, added to the created table"),
                @ParamDoc(name="wlAry[i]", desc="an array of wavelength to add to the table, added to the created table"),
                @ParamDoc(name="wlUnit[i]", desc="wavelength units. if defined"),
                @ParamDoc(name="filename[i]", desc="filename on the server"),
                @ParamDoc(name="hipsTileUrl", desc="z-axis only, url of a HiPS FITS cube tile, used instead of filename"),
                @ParamDoc(name=ServerParams.HIPS_TILE_URL_ARY, desc="points or line, url of the HiPS FITS tile of each point, used with ptAry, wptAry and plane instead of the indexed parameters"),
                @ParamDoc(name="refHDUNum[i]", desc="hdu number to extract"),
                @ParamDoc(name="extractionSizeX", desc="number of the x size of the extract"),
                @ParamDoc(name="extractionSizeY", desc="number of the y size of the extract"),
                @ParamDoc(name=ServerParams.COMBINE_OP, desc="how to combine point"),
                @ParamDoc(name="secondaryCombine", desc="auto or same"),
                @ParamDoc(name="allMatchingHDUs", desc="extract every HDU that matches the refHDU")
        })
public class ExtractFromImage extends EmbeddedDbProcessor {

    public static final String ID = "ExtractFromImage";
    public static final String EXTRACTION_TYPE = "extractionType";
    public static final String FILENAME = "filename";
    public static final String HIPS_TILE_URL = "hipsTileUrl";
    public static final String REF_HDU_NUM = "refHDUNum";
    public static final String EXTRACTION_SIZE_X = "extractionSizeX";
    public static final String EXTRACTION_SIZE_Y = "extractionSizeY";
    public static final String ALL_MATCHING_HDUS = "allMatchingHDUs";

    @Override
    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {

        String extType = req.getParam(EXTRACTION_TYPE, "");
        int ptSizeX = req.getIntParam(EXTRACTION_SIZE_X, 1);
        int ptSizeY = req.getIntParam(EXTRACTION_SIZE_Y, 1);
        FitsExtract.CombineType ct= Enum.valueOf(FitsExtract.CombineType.class,req.getParam(ServerParams.COMBINE_OP,"AVG"));
        FitsExtract.SecondaryHduCombine secondCombine= Enum.valueOf(FitsExtract.SecondaryHduCombine.class,req.getParam("secondaryCombine","AUTO"));
        boolean allMatchingHDUs = req.getBooleanParam(ALL_MATCHING_HDUS, true);
        var isLine= extType.equals("line");
        var ptOrLine= extType.equals("points") || isLine;
        var hipsTiles= req.getParam(ServerParams.HIPS_TILE_URL_ARY)!=null;
        WorldPt[] wptAry = ptOrLine ? SrvParam.getWorldPtAryFromJson(req.getParam(ServerParams.WPT_ARY)) : null;
        try {
            if (extType.equals("z-axis") || isEmpty(extType)) {
                String hipsTileUrl = req.getParam(HIPS_TILE_URL);
                File fitsFile = hipsTileUrl!=null
                        ? HiPSRetrieve.getHiPSFitsTileFile(hipsTileUrl)
                        : ServerContext.convertToFile(req.getParam(FILENAME));
                int refHduNum = hipsTileUrl!=null ? FitsExtract.FIRST_IMAGE_HDU : req.getIntParam(REF_HDU_NUM, FitsExtract.NOT_SPECIFIED);
                ImagePt pt = ImagePt.parse(req.getParam(ServerParams.PT));
                WorldPt wpt = WorldPt.parse(req.getParam(ServerParams.WPT));
                String wlUnit= req.getParam(ServerParams.WL_UNIT);
                double[] wlAry= SrvParam.getDoubleAryFromJson(req.getParam(ServerParams.WL_ARY));
                checkZAxisParams(pt, fitsFile, refHduNum);
                DataGroup dg= FITSExtractToTable.getCubeZaxisAsTable(pt, wpt, fitsFile, refHduNum, allMatchingHDUs,
                        ptSizeX, ct, secondCombine, wlAry,wlUnit);
                if (hipsTileUrl!=null) dg.getTableMeta().addKeyword(MetaConst.HIPS_TILE_URL, hipsTileUrl);
                return dg;
            }
            else if (ptOrLine && hipsTiles) {
                String[] tileUrlAry= SrvParam.getStringAryFromJson(req.getParam(ServerParams.HIPS_TILE_URL_ARY));
                return FITSExtractToTable.getHiPSPointsAsTable(
                        tileUrlAry,
                        getHiPSTileFiles(tileUrlAry),
                        SrvParam.getImagePtAryFromJson(req.getParam(ServerParams.PTARY)),
                        wptAry,
                        FitsExtract.FIRST_IMAGE_HDU,
                        req.getIntParam(ServerParams.PLANE, 0),
                        allMatchingHDUs, ptSizeX, ptSizeY, ct, secondCombine, isLine);
            }
            else if (ptOrLine) {
                var extractParamsList = getDataExtractParams(req);
                return FITSExtractToTable.getDataSelectAsTable(extractParamsList, wptAry,
                        allMatchingHDUs, ptSizeX, ptSizeY, ct, secondCombine, isLine);
            }
        } catch (IOException | FitsException e) {
            throw new IllegalArgumentException("Could not make a table from extracted data");
        }
        throw new IllegalArgumentException("extractionType must be z-axis, line, or points");
    }

    /**
     * retrieve the HiPS FITS tiles, each url is retrieved once
     * @param tileUrlAry the url of the tile for each point
     * @return the file for each url, null for a tile that could not be retrieved
     */
    private static File[] getHiPSTileFiles(String[] tileUrlAry) {
        if (tileUrlAry==null) throw new IllegalArgumentException(ServerParams.HIPS_TILE_URL_ARY+" is required");
        Map<String,File> fileByUrl= new HashMap<>();
        File[] fileAry= new File[tileUrlAry.length];
        for(int i=0; i<tileUrlAry.length; i++) {
            fileAry[i]= fileByUrl.computeIfAbsent(tileUrlAry[i], HiPSRetrieve::getHiPSFitsTileFile);
        }
        return fileAry;
    }

    private static List<FITSExtractToTable.DataExtractParams> getDataExtractParams(TableServerRequest req) {
        var eList = new ArrayList<FITSExtractToTable.DataExtractParams>();
        String[] titles= SrvParam.getStringAryFromJson( req.getParam("titleAry"));
        if (titles==null) titles= new String[] {""};
        for (int i=0; i<titles.length; i++) {
            String filename = req.getParam(FILENAME+i);
            int refHduNum = req.getIntParam(REF_HDU_NUM+i, FitsExtract.NOT_SPECIFIED);
            int plane = req.getIntParam(ServerParams.PLANE+i, -1);
            double[] wlAry= SrvParam.getDoubleAryFromJson(req.getParam(ServerParams.WL_ARY+i));
            String wlUnit= req.getParam(ServerParams.WL_UNIT+i);
            ImagePt[] ptAry= SrvParam.getImagePtAryFromJson(req.getParam(ServerParams.PTARY+i));
            checkPointParams(ptAry, plane, filename, refHduNum);
            eList.add(new FITSExtractToTable.DataExtractParams(titles[i],ptAry,filename,plane,refHduNum,wlAry,wlUnit));
        }
        return eList;
    }

    private static void checkZAxisParams(ImagePt pt, File fitsFile, int refHduNum) {
        if (pt==null) throw new IllegalArgumentException("Point is require for z-axis extraction");
        if (fitsFile==null) throw new IllegalArgumentException("filename or a retrievable hipsTileUrl is require for z-axis extraction");
        if (refHduNum==FitsExtract.NOT_SPECIFIED || (refHduNum<0 && refHduNum!=FitsExtract.FIRST_IMAGE_HDU)) {
            throw new IllegalArgumentException("refHduNum is require for z-axis extraction");
        }
    }

    private static void checkPointParams(ImagePt[] ptAry, int plane, String filename, int refHduNum) {
        if (ptAry==null || ptAry.length==0) throw new IllegalArgumentException("ptAry is require for points extraction");
        if (filename==null) throw new IllegalArgumentException("filename is require for points extraction");
        if (refHduNum==FitsExtract.NOT_SPECIFIED || refHduNum<0) {
            throw new IllegalArgumentException("refHduNum is require for points extraction");
        }
        if (plane<0) throw new IllegalArgumentException("plane is require for points extraction");
    }

}