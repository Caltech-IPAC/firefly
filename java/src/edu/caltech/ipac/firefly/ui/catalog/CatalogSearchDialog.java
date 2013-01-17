package edu.caltech.ipac.firefly.ui.catalog;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.*;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.firefly.util.WebClassProperties;

import java.util.List;


/**
 * @author Trey Roby
 */
public class CatalogSearchDialog extends BaseDialog {
    private static final WebClassProperties _prop= new WebClassProperties(CatalogSearchDialog.class);
    private CatalogPanel panel;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public CatalogSearchDialog(Widget parent, String projectId) {
        super(parent, ButtonType.OK_CANCEL, _prop.getTitle(), "basics.catalog");
        setHideAlgorythm(HideType.AFTER_COMPLETE);
        panel = new CatalogPanel(getDialogWidget(), projectId,true);
        setWidget(panel);
        setDefaultContentSize(880, 415);
        setContentMinWidth(480);
        setContentMinHeight(350);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    @Override
    protected void onVisible() {
        setAutoLocate(true);
    }

    @Override
    public void setVisible(boolean v) {
//        if (v) {
//            setWidget(panel);
//        }
        super.setVisible(v, PopupPane.Align.CENTER);
    }


    @Override
    public void inputCompleteAsync(final AsyncCallback<String> cb) {
        if (panel.isAsyncCallRequired()) {
            panel.getFieldValuesAsync(new AsyncCallback<List<Param>>() {
                public void onFailure(Throwable caught) {
                    cb.onFailure(caught);
                }

                public void onSuccess(List<Param> params) {
                    performSearch(params, cb);

                }
            });
        }
        else {
            performSearch(panel.getFieldValues(), cb);
        }

    }

    private void performSearch(List<Param> params, final AsyncCallback<String> cb) {
        CatalogRequest req = new CatalogRequest(CatalogRequest.RequestType.GATOR_QUERY);
        req.setParams(params);
        req.setUse(CatalogRequest.Use.CATALOG_OVERLAY);
        Widget w= getDialogWidget();
        int cX= w.getAbsoluteLeft()+ w.getOffsetWidth()/2;
        int cY= w.getAbsoluteTop()+ w.getOffsetHeight()/2;
        IrsaCatalogTask.getCatalog(this.getDialogWidget(),req,new CatalogSearchResponse(){
            public void showNoRowsReturned() {
                PopupUtil.showError(_prop.getTitle("noRowsReturned"),
                            _prop.getError("noRowsReturned"));
            }

            public void status(RequestStatus requestStatus) {
                cb.onSuccess("ok");
            }
        },cX,cY, panel.getTitle());
    }


    @Override
    protected boolean validateInput() throws ValidationException {
        return panel.validatePanel();
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
