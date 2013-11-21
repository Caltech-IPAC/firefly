package edu.caltech.ipac.heritage.ui;

import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.ui.previews.PreviewData;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotRelatedPanel;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.heritage.commands.HeritageRequestCmd;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.data.entity.download.HeritageFileRequest;
import edu.caltech.ipac.heritage.searches.HeritageSearch;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Apr 13, 2010
 * Time: 11:40:59 AM
 */


/**
 * @author Trey Roby
 */
public class HeritagePreviewData implements PreviewData {

    public static final String COL_TO_PREVIEW_KEY ="topreview";
    public static final String INVENTORY_BASE_PREFIX_KEY = "INVENTORY_BASE_PREFIX";

    private static final WebClassProperties _prop= new WebClassProperties(HeritagePreviewData.class);
    private static final String BCD_TITLE_BASE=  _prop.getTitle("bcd");
    private static final String PBCD_TITLE_BASE= _prop.getTitle("pbcd");
    private static final String SM_TITLE_BASE= _prop.getTitle("sm");


    private final String [] knownSpectraEndings = {"spect.tbl", "tune.tbl", "bksub.tbl", "sed.tbl"};

    public Info createRequestForRow(TableData.Row<String> row, Map<String, String> meta, List<String> columns) {

        WebPlotRequest request= null;
        Type ptype= null;
        DataType dType= DataType.parse(meta.get(HeritageSearch.DATA_TYPE));

        if (row!=null) {

            // creating request for LEGACY data - can be local or URL accessed
            if  (dType.equals(DataType.LEGACY)) {
                String colToPreview = meta.get(COL_TO_PREVIEW_KEY);
                String filename;
                if (colToPreview == null) {
                    filename= row.getValue("fname");
                    ptype= determinePlotType(filename);
                } else {
                    filename= row.getValue(colToPreview);
                    if (filename.endsWith(".tbl")) ptype = Type.SPECTRUM;
                    else ptype= determinePlotType(filename);
                }
                if (ptype != null) {
                    String title=getBaseName(filename);
                    String inventoryBase = meta.get(INVENTORY_BASE_PREFIX_KEY);
                    if (inventoryBase == null) {
                        ServerRequest fileRequest = new HeritageFileRequest(dType, filename, false);
                        fileRequest.setParam("file_name", title);
                        switch (ptype) {
                            case FITS:
                                request= WebPlotRequest.makeProcessorRequest(fileRequest, fileRequest.getRequestId());
                                break;
                            case SPECTRUM:
                                request = WebPlotRequest.makeProcessorRequest(fileRequest, "Table: "+fileRequest.toString());
                                break;
                            default:
                                request= null;
                                break;
                        }
                    } else {
                        switch (ptype) {
                            case FITS:
                                request= WebPlotRequest.makeURLPlotRequest(inventoryBase+"/"+filename);
                                break;
                            case SPECTRUM:
                                request = WebPlotRequest.makeTblURLPlotRequest(inventoryBase+"/"+filename);
                                break;
                            default:
                                request= null;
                                break;
                        }
                    }
                    if (request != null) {
                        request.setZoomType(ZoomType.SMART);
                        request.setTitle(getTitleBase(dType) + title);
                    }
                }
            } else if (dType.equals(DataType.SM)) {
                String filename= row.getValue("fname");
                ptype= determinePlotType(filename);
                if (ptype != null && ptype.equals(Type.FITS)) {
                    WebAppProperties props = Application.getInstance().getProperties();
                    String downloadBase = props.getProperty("download.sm.base");
                    if (downloadBase.contains("://")) {
                        request= WebPlotRequest.makeURLPlotRequest(downloadBase+"/"+filename);
                    } else {
                        ServerRequest fileRequest = new HeritageFileRequest(dType, filename, false);
                        fileRequest.setParam("file_name", getBaseName(filename));
                        request= WebPlotRequest.makeProcessorRequest(fileRequest, fileRequest.getRequestId());
                    }
                    request.setZoomType(ZoomType.TO_WIDTH);
                    request.setTitle(getTitleBase(dType));
                }

            } else if (dType.equals(DataType.IRS_ENHANCED)) {
                String fileName = row.getValue("heritagefilename");
                String reqkey = row.getValue("reqkey");
                String object = row.getValue("object");
                //String bandpass = (String)row.getValue("bandpass");
                //String title=StringUtils.isEmpty(bandpass) ? getBaseName(fileName) : bandpass;
                String title= (StringUtils.isEmpty(object) ? "" : object+", ")+"AORKEY="+reqkey;
                //String urlPath = "http://web.ipac.caltech.edu/staff/bob/enhanced/SAMPLES/SPITZER_S5_27584256_01_merge.tbl";
                ServerRequest fileRequest = new HeritageFileRequest(DataType.IRS_ENHANCED, fileName, false);
                fileRequest.setParam("file_name", getBaseName(fileName));
                request = WebPlotRequest.makeProcessorRequest(fileRequest, "IRS Enhanced Product Spectrum");
                request.setTitle(getTitleBase(dType)+title);
                ptype = Type.SPECTRUM;
            } else {
                Object externalname=row.getValue("externalname");

                if (dType == DataType.MOS) {
                    ptype = Type.FITS;
                    externalname = "bcd-"+row.getValue("bcdid")+".fits";
                }
                // all other spitzer data are local, need to get file info from server

                if (!StringUtils.isEmpty(externalname)) {

                    String filename = externalname.toString();
                    ptype= determinePlotType(filename);

                    if (ptype != null) {
                        Object idV;
                        String title= "";
                        boolean irs = false;

                        switch (dType) {
                            case BCD :
                                idV= row.getValue("bcdid");
                                title= row.getValue("wavelength");
                                irs=  isIRS(columns,row);
                                break;
                            case MOS :
                                idV= row.getValue("bcdid");
                                title= row.getValue("wavelength");
                                irs=  isIRS(columns,row);
                                break;
                            case PBCD:
                                idV= row.getValue("pbcdid");
                                title= row.getValue("wavelength");
                                irs=  isIRS(columns,row);
                                break;
                            default:
                                idV= null;
                                break;
                        }

                        ServerRequest fileRequest = new HeritageFileRequest(dType, idV.toString(), irs);
                        fileRequest.setParam("file_name", getBaseName(filename));

                        if (ptype!=null) {
                            switch (ptype) {
                                case FITS:
                                    request= WebPlotRequest.makeProcessorRequest(fileRequest, fileRequest.getRequestId());
                                    request.setZoomType(ZoomType.TO_WIDTH);
                                    request.setTitle(getTitleBase(dType) + " "+title);
                                    if (irs && isIrsSL(columns,row)) {
                                        RangeValues rv= new RangeValues( RangeValues.PERCENTAGE, 0,
                                                RangeValues.PERCENTAGE, 100,
                                                RangeValues.STRETCH_EQUAL);
                                        request.setInitialRangeValues(rv);
                                    } else if (dType == DataType.MOS) {
                                        request.setInitialColorTable(1);
                                        RangeValues rv = new RangeValues(RangeValues.PERCENTAGE, 1, RangeValues.PERCENTAGE, 99, RangeValues.STRETCH_LOG);
                                        request.setInitialRangeValues(rv);

                                    }
                                    break;
                                case SPECTRUM:
                                    request = WebPlotRequest.makeProcessorRequest(fileRequest, "Table: "+fileRequest.toString());
                                    request.setTitle(getTitleBase(dType) + " " + title);
                                    break;
                                default:
                                    request= null;
                                    break;
                            }

                        }
                    }
                }
            }
        }
        return (request!=null) ? new Info(ptype,request) : null;
    }


    public String getTabTitle() { return _prop.getTitle(); }
    public String getTip() { return _prop.getTip(); }

    public boolean getHasPreviewData(String id, List<String> colNames, Map<String, String> metaAttributes) {
        DataType dType= DataType.parse(metaAttributes.get(HeritageSearch.DATA_TYPE));
        return (dType==DataType.BCD || dType==DataType.PBCD ||
                (dType==DataType.LEGACY && metaAttributes.containsKey(COL_TO_PREVIEW_KEY)) ||
                dType==DataType.IRS_ENHANCED || dType==DataType.SM || dType==DataType.MOS
        );
    }


    private Type determinePlotType(String extname) {
        Type ptype = null;
        if (extname.endsWith("fits")) { ptype = Type.FITS; }
        else if (extname.endsWith("tbl")) {
            for (String kse : knownSpectraEndings) {
                if (extname.endsWith(kse)) {
                    ptype = Type.SPECTRUM;
                    break;
                }
            }
        }
        return ptype;
    }


    private String getTitleBase(DataType dType) {
        String base;
        if (dType==DataType.BCD || dType==DataType.MOS) {
            base= BCD_TITLE_BASE;
        }
        else if (dType==DataType.PBCD) {
            base= PBCD_TITLE_BASE;
        }
        else if (dType==DataType.SM) {
            base= SM_TITLE_BASE;
        } else {
            base= "";
        }
        return base;
    }


    private boolean isIRS(List<String> columns,TableData.Row row) {
        int wavelengthIdx= columns.indexOf("wavelength");
        int fileTypeIdx= columns.indexOf("filetype");
        String wavelength= (String)row.getValue(wavelengthIdx);
        String fileType= fileTypeIdx>=0 ? (String)row.getValue(fileTypeIdx) : "image";
        wavelength= wavelength!=null ? wavelength.toLowerCase() : "";
        fileType= fileType!=null ? fileType.toLowerCase() : "";
        return fileType.equals("image") &&
                (wavelength.startsWith("irs sl") || wavelength.startsWith("irs ll") ||
                    wavelength.startsWith("irs sh") || wavelength.startsWith("irs lh"));
    }


    private boolean isIrsSL(List<String> columns, TableData.Row row) {
        int wavelengthIdx= columns.indexOf("wavelength");
        int fileTypeIdx= columns.indexOf("filetype");
        String wavelength= (String)row.getValue(wavelengthIdx);
        String fileType= (String)row.getValue(fileTypeIdx);
        wavelength= wavelength!=null ? wavelength.toLowerCase() : "";
        fileType= fileType!=null ? fileType.toLowerCase() : "";
        return fileType.equals("image") && wavelength.startsWith("irs sl");
    }

    private String getBaseName(String filePath) {
        String basename = "none";
        if (!StringUtils.isEmpty(filePath)) {
            String[] parts = filePath.split("/");
            if (parts.length > 0) {
                basename =  parts[parts.length-1];
            }
        }
        return basename;
    }

    public void prePlot(MiniPlotWidget mpw, Map<String, String> metaAttributes) {
        mpw.setPreferenceColorKey("SHA-Preview-color");
    }

    public void postPlot(MiniPlotWidget mpw, WebPlot plot) {
        WebPlotRequest req= plot.getPlotState().getWebPlotRequest(Band.NO_BAND);
        if (req.getBooleanParam(HeritageFileRequest.IRS_IMAGE)) {
            plot.setAttribute(WebPlot.READOUT_ATTR, new IRSMouseReadoutHandler());
            plot.setAttribute(WebPlot.DISABLE_ROTATE_REASON,
                              "FITS image with IRS slit data can't be rotated");
        }
    }





    public boolean isThreeColor() { return false; }
    public String getGroup() { return null; }
    public int getMinWidth() { return 0; }
    public int getMinHeight() { return 0; }
    public PlotRelatedPanel[] getExtraPanels() { return null; }
    public List<String> getEventWorkerList() { return Arrays.asList(HeritageRequestCmd.ACTIVE_TARGET_ID); }
    public boolean getSaveImageCorners() { return true; }

    public boolean getPlotFailShowPrevious() { return false;   }
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
