package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.visualize.ui.color.ColorChooserPanel;
//import org.agilar.gwt.colorpicker.ColorChooserPanel;


/**
 * User: roby
 * Date: Jan 26, 2009
 * Time: 3:45:42 PM
 */



/**
 * @author Trey Roby
 */
public class ColorPickerDialog extends BaseDialog {

    private static WebClassProperties _prop= new WebClassProperties(ColorPickerDialog.class);

    private String _color;
    private ColorChoice _colorChoice;
    private final ColorChooserPanel _colorPanel= new ColorChooserPanel();
//    private final ColorPicker _cp= new ColorPicker("#ff0000", false, true);


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public ColorPickerDialog(Widget parent,
                            String title,
                             String defColor,
                             ColorChoice colorChoice) {
        super(parent, ButtonType.OK_CANCEL, title, "visualization.fitsViewer");
        _colorChoice= colorChoice;
        _color= defColor;
        createContents();
    }

//======================================================================
//----------------------- Static Methods -------------------------------
//======================================================================

    public static void chooseColor(Widget parent,
                                   String title,
                                   String defColor,
                                   ColorChoice colorChoice) {
        new ColorPickerDialog(parent,title,defColor,colorChoice).setVisible(true);
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    @Override
    public void setVisible(boolean v) {
        if (v) {
//            _color= null;
//        _cp.setColor(_color);
//        setWidget(_cp);
            setWidget(_colorPanel);
            _colorPanel.setColor(_color);
//            _colorPanel.setWidth("200px");
//            _colorPanel.setHeight("200px");
        }
        super.setVisible(v);
    }

//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void createContents() {
    }

    @Override
    protected void inputComplete() {
        _color= _colorPanel.getColor();
        if (_colorChoice!=null) _colorChoice.choice(_color);
    }

    @Override
    protected void inputCanceled() {
        if (_colorChoice!=null) _colorChoice.choice(null);
    }

    @Override
    protected boolean validateInput() throws ValidationException {
       return true;
    }


// =====================================================================
// -------------------- Inner Interface --------------------------------
// =====================================================================

    public interface ColorChoice {
        public void choice(String color);
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
