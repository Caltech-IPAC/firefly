/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataSource;
import edu.caltech.ipac.astro.ibe.datasource.PtfIbeDataSource;
import edu.caltech.ipac.astro.ibe.datasource.TwoMassIbeDataSource;
import edu.caltech.ipac.astro.ibe.datasource.WiseIbeDataSource;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author tatianag
 *         $Id: $
 */
public class IBEUtils {

    public static IBE getIBE(String mission, Map<String,String> paramMap) throws IOException, DataAccessException {
        IbeDataSource ibeDataSource;
        if (StringUtils.isEmpty(mission)) {
            throw new DataAccessException("Unspecified mission");
        }
        if (mission.equals(WiseIbeDataSource.WISE)) {
            ibeDataSource = new WiseIbeDataSource();
        } else if (mission.equals(PtfIbeDataSource.PTF)) {
            ibeDataSource = new PtfIbeDataSource();
        } else if (mission.equals(TwoMassIbeDataSource.TWOMASS)) {
            ibeDataSource = new TwoMassIbeDataSource();
        } else {
            throw new DataAccessException("Unsupported mission: "+mission);
        }
        ibeDataSource.initialize(paramMap);
        return new IBE(ibeDataSource);
    }



    public static Map<String,String> getParamMap(List<Param> params) {
        HashMap<String,String> paramMap = new HashMap<String,String>();
        for (Param p : params) {
            paramMap.put(p.getName(), p.getValue());
        }
        return paramMap;
    }


    public static SortInfo getSortInfo(IbeDataSource source) {
        if (source instanceof WiseIbeDataSource) {
            String productLevel = ((WiseIbeDataSource)source).getDataProduct().plevel();
            if (productLevel.startsWith("1")) {
                return new SortInfo("scan_id","frame_num","band");
            } else if (productLevel.startsWith("3")) {
                return new SortInfo("coadd_id","band");
            }
        } else if (source instanceof PtfIbeDataSource) {
            // TODO PTF
            return null;
        }
        return null;
    }

    public static Map<String,String> getMissionSpecificTableMeta(IbeDataSource source) {
        Map<String,String> attribs = new HashMap<String,String>();
        String [] colsToHide = null;
        String relatedCols = null;
        Map<String,String> sortByCols = new HashMap<String,String>(2);
        if (source instanceof WiseIbeDataSource) {
            WiseIbeDataSource.DataProduct dp = ((WiseIbeDataSource)source).getDataProduct();
            String productLevel = dp.plevel();
            attribs.put("ProductLevel", productLevel);
            String imageSet = dp.imageset();
            attribs.put("ImageSet", imageSet);
            if (productLevel.startsWith("1")) {
                // level 1
                sortByCols.put("scan_id", "scan_id,frame_num,band");
                sortByCols.put("frame_num", "frame_num,scan_id,band");
                colsToHide = new String[]{"magzp", "magzpunc", "modeint", "scangrp",
                        "utanneal", "exptime", "debgain", "febgain", "qual_scan",
                        "date_imgprep", "date_obs", "qa_status"};
                relatedCols = "scan_id,frame_num";
            } else {
                // level 3
                sortByCols.put("coadd_id", "coadd_id,band");
                colsToHide = new String[]{"magzp", "magzpunc", "qual_coadd",
                        "date_imgprep", "qa_status"};
                relatedCols = "coadd_id";
            }
        } else if (source instanceof TwoMassIbeDataSource) {
            TwoMassIbeDataSource.DS ds = ((TwoMassIbeDataSource)source).getDS();
            attribs.put("ds", ds.getName());
            colsToHide = new String[]{"cntr", "bin",
                    "copy_flag", "iordate", "strip_id", "daynum",
                    "scandir", "pixnam", "icsversn", "schedver", "telname"};

            relatedCols = "coadd_key";

        } else if (source instanceof PtfIbeDataSource) {
            // TODO: PTF
        }

        for (String sortCol : sortByCols.keySet()) {
            attribs.put(DataSetParser.makeAttribKey(DataSetParser.SORT_BY_TAG, sortCol), sortByCols.get(sortCol));
        }

        if (colsToHide != null) {
            for (String c : colsToHide) {
                attribs.put(DataSetParser.makeAttribKey(DataSetParser.VISI_TAG, c), DataSetParser.VISI_HIDE);
            }
        }

        if (relatedCols != null) {
            attribs.put("col.related", relatedCols);
        }
        return attribs;
    }
}
