package edu.caltech.ipac.heritage.ui;

import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.ui.previews.AbstractCoverageData;
import edu.caltech.ipac.firefly.ui.previews.TableCtx;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.heritage.commands.HeritageRequestCmd;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.searches.HeritageSearch;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.Arrays;
import java.util.List;
/**
 * User: roby
 * Date: Apr 19, 2010
 * Time: 9:23:17 AM
 * $ID: $
 */


/**
 * @author Trey Roby
 */
public class HeritageCoverageData extends AbstractCoverageData {

    private static final WebClassProperties _prop= new WebClassProperties(HeritageCoverageData.class);
//    private static final String TITLE_BASE= _prop.getTitle("base");
    private static final String TITLE_BASE= "";
    private static final String BCD_TITLE_BASE= TITLE_BASE + _prop.getTitle("bcd");
    private static final String PBCD_TITLE_BASE= TITLE_BASE + _prop.getTitle("pbcd");
    private static final String IRS_ENHANCED_TITLE_BASE= TITLE_BASE + _prop.getTitle("irsenhanced");
    private static final String LEGACY_TITLE_BASE= TITLE_BASE + _prop.getTitle("enhanced");
    private static final String SM_TITLE_BASE= TITLE_BASE + _prop.getTitle("supermosaic");
    private static final String SOURCE_LIST_TITLE_BASE= TITLE_BASE + _prop.getTitle("sourcelist"); 


    public String getTitle() { return _prop.getTitle(); }
    public String getTip() { return _prop.getTip(); }

    public List<String> getEventWorkerList() { return Arrays.asList(HeritageRequestCmd.ACTIVE_TARGET_ID); }

    public boolean getHasCoverageData(TableCtx table) {
        if (!table.hasData()) return false;
        DataType dType= DataType.parse(table.getMeta().get(HeritageSearch.DATA_TYPE));
        return (dType==DataType.BCD || dType==DataType.PBCD ||
                dType==DataType.IRS_ENHANCED || dType==DataType.LEGACY ||
                dType==DataType.SM || dType==DataType.SOURCE_LIST ||
                dType==DataType.MOS
        );
    }

    public String getCoverageBaseTitle(TableCtx table) {

        DataType dType= DataType.parse(table.getMeta().get(HeritageSearch.DATA_TYPE));
        String base;
        if (dType==DataType.BCD) {
            base= BCD_TITLE_BASE;
        }
        else if (dType==DataType.PBCD) {
            base= PBCD_TITLE_BASE;
        }
        else if (dType==DataType.IRS_ENHANCED) {
            base= IRS_ENHANCED_TITLE_BASE;
        }
        else if (dType==DataType.LEGACY) {
            base= LEGACY_TITLE_BASE;
        }
        else if (dType==DataType.SM) {
            base= SM_TITLE_BASE;
        }
        else if (dType==DataType.SOURCE_LIST) {
            base= SOURCE_LIST_TITLE_BASE;
        }
        else {
            base= TITLE_BASE;
        }
        return base;
    }


    @Override
    public List<WorldPt[]> modifyBox(WorldPt[] wpts, TableCtx table, TableData.Row<String> row) {
        List<WorldPt[]> retval;
        if (wpts.length == 4) {
            if (isIRS(table,row)) {
                retval= IRSSlits.expandIRSAperture(wpts);
            }
            else {
                retval= super.modifyBox(wpts,table,row);
            }
        }
        else {
            retval= super.modifyBox(wpts,table,row);
        }
        return retval;
    }

    private boolean isIRS(TableCtx table, TableData.Row<String> row) {
        WebAssert.argTst(row!=null, "row : " +row+" should not be null");
        int wavelengthIdx;
        int fileTypeIdx;
        try {
            wavelengthIdx= table.getColumns().indexOf("wavelength");
            if (wavelengthIdx < 0) {return false;}
            fileTypeIdx= table.getColumns().indexOf("filetype");
            if (fileTypeIdx < 0) {return false;}
        } catch (IllegalArgumentException e) {
            // irs enhanced data have no wavelength col
            return false;
        }
        String wavelength= row.getValue(wavelengthIdx);
        String fileType= row.getValue(fileTypeIdx);
        wavelength= wavelength!=null ? wavelength.toLowerCase() : "";
        fileType= fileType!=null ? fileType.toLowerCase() : "";
        return fileType.equals("image") &&
                (wavelength.startsWith("irs sl") ||
                        wavelength.startsWith("irs ll"));

    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
