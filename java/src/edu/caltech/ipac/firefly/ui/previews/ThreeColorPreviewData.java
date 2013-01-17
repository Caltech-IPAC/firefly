package edu.caltech.ipac.firefly.ui.previews;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotRelatedPanel;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;

import java.util.HashMap;
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
public class ThreeColorPreviewData extends AbstractPreviewData {

    private Map<Band,String> _reqKeyMap;
    private Map<Band,List<Param>>  _paramsMap;
    private Map<Band,Boolean> _continueOnFailMap= new HashMap<Band, Boolean>(5);
    private final Band[] _bands;

    public ThreeColorPreviewData(Map<Band,String> reqKeyMap,
                                 Map<Band,List<Param>> paramsMap,
                                 Band[] bands) {

        _reqKeyMap= reqKeyMap;
        _paramsMap= paramsMap;
        _bands= bands;
        setThreeColor(true);
        setPlotFailShowPrevious(true);

    }


    public void setBandParams(Band band, List<Param> params) {
        if (band!=null) {
            _paramsMap.put(band,params);
        }
    }

    public void setContinueOnFail(Band band, boolean continueOnFail) {
        _continueOnFailMap.put(band, continueOnFail);
    }

    public boolean getContinueOnFail(Band band) {
        return _continueOnFailMap.containsKey(band) ? _continueOnFailMap.get(band) : false;
    }

    public Info createRequestForRow(TableData.Row<String> row, Map<String, String> meta, List<String> columns) {


        Map<Band,WebPlotRequest> reqMap= null;
        reqMap= new HashMap<Band,WebPlotRequest>(5);

        if (row!=null) {
            for(Band band : _bands) {
                if (_paramsMap.get(band)!=null) {
                    WebPlotRequest request= makeServerRequest(meta, columns, band, row);
                    request.setContinueOnFail(getContinueOnFail(band));
                    float zl= computeZoomLevel(meta);
                    if (!Float.isNaN(zl)) request.setInitialZoomLevel(zl);
                    reqMap.put(band,request);
                    request.setTitle( getTitle());
                    if (getUseScrollBars())  request.setShowScrollBars(true);
                    if (getImageSelection()) request.setAllowImageSelection(true);
                    request.setParams(getExtraPlotRequestParams());
                }
            }
        }
        return (reqMap.size()>0) ? new Info(Type.FITS,reqMap,true) : null;
    }



    public WebPlotRequest makeServerRequest(Map<String,String> meta,
                                            List<String> columns,
                                            Band band,
                                            TableData.Row<String>row) {
        ServerRequest sr= new ServerRequest(_reqKeyMap.get(band));
        if (_paramsMap.get(band)!=null) {
            for (Param p : _paramsMap.get(band)) sr.setParam(p);
        }


        for(String cname : columns) {
            if (getLimitTableParams().size()==0 ||
                getLimitTableParams().contains(cname)) {
                sr.setSafeParam(cname, row.getValue(cname));
            }
        }

        for(String key : this.getHeaderParams()) {
            if (meta.containsKey(key)) {
                sr.setSafeParam(key, meta.get(key));
            }
        }

        WebPlotRequest wpReq= WebPlotRequest.makeProcessorRequest(sr,getTitle());
        wpReq.setZoomType(Float.isNaN(computeZoomLevel(meta)) ? ZoomType.TO_WIDTH : ZoomType.STANDARD);
        if (getRangeValues()!=null) {
            wpReq.setInitialRangeValues(getRangeValues());
        }

        return wpReq;
    }



    public boolean getHasPreviewData(String id, List<String> colNames, Map<String, String> metaAttributes) {
        boolean retval= false;
        if (metaAttributes!=null && getSourceList().contains(id)) {
            retval= TableMeta.getCenterCoordColumns(metaAttributes)!=null;
        }
        return retval;
    }

    public String getTabTitle() { return "3 Color Preview"; }

    public String getTip() {
        return "";
    }

    private void addDataSetOverlay(DataSet ds) {

    }


    private class DataSetCallback implements AsyncCallback<DataSet> {
        public void onFailure(Throwable caught) {
        }

        public void onSuccess(DataSet result) {
            addDataSetOverlay(result);
        }
    }


    public void setExtraPanel(PlotRelatedPanel prp) {
        if (prp instanceof ThreeColorPreviewData.CanUpdatePreview) {
            ((ThreeColorPreviewData.CanUpdatePreview)prp).setPreviewData(this);
        }
        super.setExtraPanel(prp);
    }


    public static interface CanUpdatePreview {
        void setPreviewData(ThreeColorPreviewData prevData);
    }

}/*
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

