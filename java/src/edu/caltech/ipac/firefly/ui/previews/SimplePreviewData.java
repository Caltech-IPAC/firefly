package edu.caltech.ipac.firefly.ui.previews;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.visualize.Band;
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
public class SimplePreviewData extends AbstractPreviewData {

    private String _reqKey;
    private List<Param> _extraParams;
    private List<Param> _noPreviewParams = null;

    public SimplePreviewData(String reqKey, List<Param> extraParams) {
        _reqKey= reqKey;
        _extraParams = extraParams;
        setThreeColor(false);
        setPlotFailShowPrevious(false);
    }

    public void setNoPreviewCondition(List<Param>  noPreviewParams) {
        _noPreviewParams = noPreviewParams;
    }

    public boolean noRowPreview(TableData.Row<String> row) {
        if (_noPreviewParams == null) return false;
        for (Param p : _noPreviewParams) {
           String val = row.getValue(p.getName());
           if (val != null && val.equals(p.getValue())) {
                return true;
           }
        }
        return false;
    }


    public Info createRequestForRow(TableData.Row<String> row, Map<String, String> meta, List<String> columns) {

        if (noRowPreview(row)) return null;

        Map<Band,WebPlotRequest> reqMap= null;
        CommonParams.DataSource dataSource= getDataSource(meta);
        String dataColumn= getDataColumn();
        if (dataColumn==null) dataColumn= meta.get(CommonParams.PREVIEW_COLUMN_HEADER);

        Type ptype= null;

        if (row!=null) {

            ptype= determinePlotType(row);
            switch (ptype) {
                case FITS:
                    reqMap= new HashMap<Band,WebPlotRequest>(1);
                    String filename=row.getValue(dataColumn);
                    WebPlotRequest request= null;
                    float zl= computeZoomLevel(meta);
                    if (dataSource== CommonParams.DataSource.URL && filename!=null) {
                        request=  WebPlotRequest.makeURLPlotRequest(filename);
                        request.setZoomType(Float.isNaN(zl) ? ZoomType.TO_WIDTH: ZoomType.STANDARD);
                    }
                    else if (dataSource== CommonParams.DataSource.FILE && filename!=null){
                        request=  WebPlotRequest.makeFilePlotRequest(filename);
                        request.setZoomType(Float.isNaN(zl) ? ZoomType.TO_WIDTH: ZoomType.STANDARD);
                    }
                    else if (dataSource== CommonParams.DataSource.REQUEST) {
                        request= makeServerRequest(row,meta,columns);
                    }
                    if (request!=null) {
                        if (!Float.isNaN(zl)) request.setInitialZoomLevel(zl);
                        reqMap.put(Band.NO_BAND,request);
                        request.setTitle( getTitle());
                        if (getUseScrollBars())  request.setShowScrollBars(true);
                        if (getImageSelection()) request.setAllowImageSelection(true);
                        if (isRotateNorthUp())   request.setRotateNorthSuggestion(true);

                        request.setParams(getExtraPlotRequestParams());
                    }
                    break;
                case SPECTRUM:
                    reqMap= getSpectrumReq(dataSource,row);
                    break;
                default:
                    reqMap= null;
                    break;
            }
        }
        return new Info(ptype,reqMap,false);
    }

    private CommonParams.DataSource getDataSource(Map<String,String> meta) {
        CommonParams.DataSource dataSource= getDataSource();
        if (dataSource==null) {
            String dsStr= meta.get(CommonParams.PREVIEW_SOURCE_HEADER);
            dataSource= CommonParams.DataSource.REQUEST;
            if (dsStr!=null) {
                try {
                    dataSource= Enum.valueOf(CommonParams.DataSource.class,dsStr);
                } catch (Exception e) {
                    dataSource= CommonParams.DataSource.REQUEST;
                }
            }
        }
        return dataSource;
    }

    private Map<Band,WebPlotRequest> getSpectrumReq(CommonParams.DataSource resType, TableData.Row<String> row) {
        String filename=row.getValue(CommonParams.PREVIEW_COLUMN_HEADER);
        Map<Band,WebPlotRequest> reqMap= new HashMap<Band,WebPlotRequest>(2);
        WebPlotRequest request= null;
        if (resType== CommonParams.DataSource.URL && filename!=null) {
            request= WebPlotRequest.makeTblURLPlotRequest(filename);
        }
        else if (resType== CommonParams.DataSource.FILE && filename!=null){
            request= WebPlotRequest.makeTblFilePlotRequest(filename);
        }
        if (request!=null) {
            reqMap.put(Band.NO_BAND,request);
            request.setTitle(getTitle());
        }
        return reqMap;
    }


    public WebPlotRequest makeServerRequest(TableData.Row<String>row,
                                            Map<String,String> meta,
                                            List<String> columns) {
        ServerRequest sr= new ServerRequest(_reqKey);
        if (_extraParams!=null) {
            for (Param p : _extraParams) sr.setParam(p);
        }

        for(String cname : columns) {
            if (getLimitTableParams().size()==0 ||
               (getLimitTableParams().size()==1 && getLimitTableParams().contains("ALL")) ||
                getLimitTableParams().contains(cname)) {
                sr.setSafeParam(cname, row.getValue(cname));
            }
        }


        List<String> headerParams= getHeaderParams();
        boolean allHeaders= headerParams.size()==1 || "ALL".equalsIgnoreCase(headerParams.get(0));

        if (allHeaders) {
            for(Map.Entry<String,String> entry : meta.entrySet())  {
                sr.setSafeParam(entry.getKey(),entry.getValue());
            }
        }
        else {
            for(String key : getHeaderParams()) {
                if (meta.containsKey(key)) {
                    sr.setSafeParam(key, meta.get(key));
                }
            }
        }

        WebPlotRequest wpReq= WebPlotRequest.makeProcessorRequest(sr,getTitle());
        wpReq.setZoomType(Float.isNaN(computeZoomLevel(meta)) ? ZoomType.TO_WIDTH : ZoomType.STANDARD);
        if (getColorTableID()>0) {
            wpReq.setInitialColorTable(getColorTableID());
        }
        if (getRangeValues()!=null) {
            wpReq.setInitialRangeValues(getRangeValues());
        }
        return wpReq;
    }


    /**
     * Look at several factors
     * <ol>
     *    <li>the previews has the table in its ID list or it is set for CommonParams.ALL_TABLES</li>
     *    <li>if that is true then either:</li>
     *    <ul>
     *        <li>the center coordinate columns are set </li>
     *        <li>the CommonParams.DATA_SOURCE &&  the CommonParams.DATA_COLUMN is set and the
     *            data column is in the list of columns in the column list</li>
     *        <li>the meta data contains the key CommonParams.HAS_PREVIEW_DATA</li>
     *    </ul>
     * </ol>
     * @param id  table id
     * @param colNames  col names
     * @param metaAttributes  meta
     * @return true if preview data are available for the given table
     */
    public boolean getHasPreviewData(String id, List<String> colNames, Map<String, String> metaAttributes) {
        boolean retval= false;
        List<String> sourceList= getSourceList();
        if (metaAttributes!=null && (sourceList.contains(id) || sourceList.contains(CommonParams.ALL_TABLES))) {
            retval= TableMeta.getCenterCoordColumns(metaAttributes)!=null;
            if (!retval) {
                if (getDataColumn()!=null && getDataSource()!=null) {
                    retval= colNames.contains(getDataColumn());
                }
            }
            if (!retval) {
                retval= metaAttributes.containsKey(CommonParams.HAS_PREVIEW_DATA);
            }
        }
        return retval;
    }

    public String getTabTitle() {
        return getTitle();
    }

    public String getTip() {
        return "";
    }

    private Type determinePlotType(TableData.Row<String> row) {

        return Type.FITS;
    }






}
