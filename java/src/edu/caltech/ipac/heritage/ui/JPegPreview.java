package edu.caltech.ipac.heritage.ui;

import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.MaskMessgeWidget;
import edu.caltech.ipac.firefly.ui.MaskPane;
import edu.caltech.ipac.firefly.ui.table.AbstractTablePreview;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.searches.HeritageSearch;
import edu.caltech.ipac.util.StringUtils;


/**
 * Date: Feb 20, 2009
 *
 * @author Trey
 * @version $Id: JPegPreview.java,v 1.12 2010/05/07 00:26:34 roby Exp $
 */
public class JPegPreview extends AbstractTablePreview {

    public enum CoverageType { X, BOX, HIDE}

    private static final WebClassProperties _prop= new WebClassProperties(JPegPreview.class);
    private final Image _jpegImage= new Image();
    private final ScrollPanel _scroll= new ScrollPanel(_jpegImage);
    private final MaskPane _nopreviewMaskPane=
            new MaskPane(_scroll, new MaskMessgeWidget(_prop.getName("noimage"),false));


    public JPegPreview(String name) {
        setName(name);
        setDisplay(_scroll);
        _jpegImage.addErrorHandler(new ErrorHandler() {
            public void onError(ErrorEvent ev) {
                setPreview(null);
            }
        });

        _jpegImage.addLoadHandler(new LoadHandler() {
            public void onLoad(LoadEvent ev) {
                GwtUtil.centerScrollPanel(_scroll);
            }
        });
    }

    @Override
    public void bind(EventHub hub) {
        super.bind(hub);
        WebEventListener wel =  new WebEventListener(){
            public void eventNotify(WebEvent ev) {

                TablePanel table = (TablePanel) ev.getSource();
                updateTabVisible(table);
                if (GwtUtil.isOnDisplay(getDisplay())) {
                    updatePbcdJpeg(table);
                }
            }
        };
        hub.getEventManager().addListener(EventHub.ON_ROWHIGHLIGHT_CHANGE, wel);
        hub.getEventManager().addListener(EventHub.ON_TABLE_SHOW, wel);
    }

    private void updateTabVisible(TablePanel table) {
        boolean show;
        TableMeta meta= table.getDataset().getMeta();
        DataType dType= DataType.parse(meta.getAttribute(HeritageSearch.DATA_TYPE));
        show= (dType==DataType.PBCD);

        getEventHub().setPreviewEnabled(this,show);

    }



    private void updatePbcdJpeg(TablePanel table) {
        TableData model= table.getDataset().getModel();
        TableData.Row row = table.getTable().getHighlightedRow();


        String previewURL = model.getAttribute("previewDownloadURL");

        if (row!=null && previewURL != null) {
            String url;
            Object pbcdID= row.getValue("pbcdid");
            Object prodID= row.getValue("productid");
            Object heritagefilename =  row.getValue("heritagefilename");

            if (!StringUtils.isEmpty(prodID)) {
                url= previewURL+prodID.toString();
                setPreview(url);

            } else if (!StringUtils.isEmpty(pbcdID)  &&
                    !StringUtils.isEmpty(heritagefilename)) {
                if (heritagefilename.toString().endsWith("maic.fits")) {
                    url= previewURL+pbcdID.toString();
                    setPreview(url);
                }
                else {
                    setPreview(null);
                }
            }
            else {
                setPreview(null);
            }

        }
    }


    public void setPreview(String url) {
        if (url!=null) {
            _jpegImage.setVisible(true);
            _jpegImage.setUrl(url);
            _nopreviewMaskPane.hide();
            GwtUtil.centerScrollPanel(_scroll);
        }
        else {
            _jpegImage.setVisible(false);
            _nopreviewMaskPane.show();
        }

    }

//    public void onResize(int width, int height) {
//        _scroll.setPixelSize(width,height);
//    }

    protected void updateDisplay(TablePanel table) {
        if (table!=null) {
            updatePbcdJpeg(table);
            GwtUtil.centerScrollPanel(_scroll);
        }
    }


}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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

