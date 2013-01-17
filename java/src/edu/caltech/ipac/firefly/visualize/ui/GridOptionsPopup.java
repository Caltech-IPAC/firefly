package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.visualize.draw.WebGridLayer;

/**
 * User: roby
 * Date: Feb 26, 2009
 * Time: 1:50:08 PM
 */


/**
 * @author Trey Roby
 */
public class GridOptionsPopup {

    private static WebClassProperties _prop= new WebClassProperties(GridOptionsPopup.class);
    private final SimpleInputField _coordSys= SimpleInputField.createByProp(_prop.makeBase("coordSystem"));
    private final PopupPane _popup;
    private final WebGridLayer _gridLayer;
    private final Widget _parent;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public GridOptionsPopup(WebGridLayer gridLayer, Widget parent) {
        _parent= parent;
        _gridLayer= gridLayer;
        _popup= new PopupPane(_prop.getTitle(),null, PopupType.STANDARD,false,true);
        layout();

    }



//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void setVisible(boolean v) {
        if (v) {
            _popup.alignTo(_parent, PopupPane.Align.TOP_LEFT);
            _popup.show();
        }
        else {
            _popup.hide();
        }

    }

//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void layout() {

        _coordSys.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent ev) {
                String value= _coordSys.getValue();
                _gridLayer.setCoordSystem(value);
                _popup.hide();
            }
        });

        String csysStr= _gridLayer.getCoordSystem();
        _coordSys.setValue(csysStr);


        Widget color= GwtUtil.makeLinkButton(_prop.makeBase("color"),new ClickHandler() {
            public void onClick(ClickEvent ev) {
                changeColor();
                _popup.hide();
            }
        });


        Widget hide= GwtUtil.makeLinkButton(_prop.makeBase("hide"),new ClickHandler() {
            public void onClick(ClickEvent ev) {
                _gridLayer.setShowing(false);
                _popup.hide();
            }
        });

        VerticalPanel vbox= new VerticalPanel();
        vbox.setSpacing(5);
        vbox.add(_coordSys);
        vbox.add(GwtUtil.centerAlign(color));
        vbox.add(GwtUtil.centerAlign(hide));

        _popup.setWidget(vbox);
    }

    private void changeColor() {
        String color= _gridLayer.getGridColor();
        ColorPickerDialog.chooseColor(_parent,_prop.getTitle("colorChooser"),color,
                new ColorPickerDialog.ColorChoice() {
                    public void choice(String color) {
                        if (color!=null) _gridLayer.setGridColor(color);
                    }
        });

    }



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
