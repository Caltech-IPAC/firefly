package edu.caltech.ipac.firefly.ui.catalog;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.data.Param;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: balandra
 * Date: Feb 18, 2010
 */
public class CatalogQueryDialog extends BaseDialog {
    private CatddPanel panel;



//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================
    public CatalogQueryDialog (Widget parent, CatColumnInfo info, List<Param> params, String columns, String requiredColumns, String constraints, boolean defSelect) {
        super(parent, ButtonType.OK_CANCEL, "Catalog Columns", "basics.catalog.columns");
        Button b = this.getButton(BaseDialog.ButtonID.CANCEL);
        b.setText("Close");
        try{
            panel = new CatddPanel(info, params, columns, requiredColumns, constraints, defSelect);
        } catch (Exception e){
            PopupUtil.showError("Catalog Error", e.getMessage());
        }
        setWidget(panel);
        setDefaultContentSize(610,600);
        setContentMinWidth(500);
        setContentMinHeight(300);

    }

//======================================================================
//----------------------- Static Methods -------------------------------
//======================================================================

    public static void showCatalogDialog(Widget parent, CatColumnInfo info,  List<Param> params, String columns, String reqColumns, String constraints, boolean defSelect) {
        new CatalogQueryDialog(parent, info, params, columns, reqColumns, constraints, defSelect).setVisible(true);
    }
    public static void showCatalogDialog(Widget parent, CatColumnInfo info,  List<Param> params, String columns, String reqColumns, String constraints) {
        new CatalogQueryDialog(parent, info, params, columns, reqColumns, constraints, false).setVisible(true);
    }
    public static void showCatalogDialog(Widget parent, CatColumnInfo info,  List<Param> params, String columns, String constraints, boolean defSelect) {
        new CatalogQueryDialog(parent, info, params, columns, "", constraints, defSelect).setVisible(true);
    }
    public static void showCatalogDialog(Widget parent, CatColumnInfo info,  List<Param> params, String columns, String constraints) {
        new CatalogQueryDialog(parent, info, params, columns, "", constraints, false).setVisible(true);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    @Override
    public void setVisible(boolean v) {
        super.setVisible(v, PopupPane.Align.CENTER);
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    protected void inputComplete() {
        panel.setColumns();
        panel.setConstraints();
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
