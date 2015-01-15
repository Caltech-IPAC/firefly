/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.fuse;
/**
 * User: roby
 * Date: 9/8/14
 * Time: 4:00 PM
 */


import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.data.fuse.config.SelectedRowData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class ServerRequestBuilder {



    private int colorTableID= 0;
    private RangeValues rv= null;
    private List<String> colToUse;
    private List<String> headerParams;


    public WebPlotRequest makeServerRequest(String reqKey,
                                            String title,
                                            SelectedRowData selRowData,
                                            List<Param> extraParams) {

        TableData.Row<String>row= selRowData.getSelectedRow();
        TableMeta meta= selRowData.getTableMeta();
        List<String> columns= selRowData.getSelectedRow().getColumnNames();
        ServerRequest sr= new ServerRequest(reqKey);
        if (extraParams!=null) {
            for (Param p : extraParams) sr.setParam(p);
        }

        for(String cname : columns) {
            if (getColumnsToUse().size()==0 ||
                    (getColumnsToUse().size()==1 && getColumnsToUse().contains("ALL")) ||
                    getColumnsToUse().contains(cname)) {
                sr.setSafeParam(cname, row.getValue(cname));
            }
        }


        List<String> headerParams= getHeaderParams();
        if (headerParams!=null) {
            boolean allHeaders= headerParams.size()==1 || "ALL".equalsIgnoreCase(headerParams.get(0));

            if (allHeaders) {
                for(Map.Entry<String,String> entry : meta.getAttributes().entrySet())  {
                    sr.setSafeParam(entry.getKey(),entry.getValue());
                }
            }
            else {
                for(String key : getHeaderParams()) {
                    if (meta.contains(key)) {
                        sr.setSafeParam(key, meta.getAttribute(key));
                    }
                }
            }
        }

        WebPlotRequest wpReq= WebPlotRequest.makeProcessorRequest(sr,title);
        wpReq.setZoomType(ZoomType.FULL_SCREEN);
        wpReq.setInitialColorTable(getColorTableID());
        wpReq.setTitle(title);
        if (getRangeValues()!=null) {
            wpReq.setInitialRangeValues(getRangeValues());
        }
        return wpReq;
    }


    public void setColumnsToUse(List<String> colToUse) {
        this.colToUse= (colToUse==null) ? Collections.<String>emptyList() : colToUse;
    }
    public List<String> getColumnsToUse() { return colToUse; }


    public void setHeaderParams(List<String> headerParams) {
        this.headerParams= (headerParams==null) ? Collections.<String>emptyList() : headerParams;
    }

    public List<String> getHeaderParams() { return headerParams; }

    public void setRangeValues(RangeValues rv) { this.rv= rv; }
    public RangeValues getRangeValues() { return rv; }

    public void setColorTableID(int id) {
        if (id==Integer.MAX_VALUE || id<0) id= 0;
        colorTableID= id;
    }
    public int getColorTableID() { return colorTableID; }


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

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
