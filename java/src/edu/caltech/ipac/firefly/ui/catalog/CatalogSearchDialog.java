/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
