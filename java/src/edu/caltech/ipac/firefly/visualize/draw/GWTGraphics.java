package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;
/**
 * User: roby
 * Date: Oct 1, 2008
 * Time: 11:21:49 AM
 */



/**
 * @author Trey Roby
 */
public class GWTGraphics implements Graphics {

    private final GWTGraphicsGroup _group;
    private final GWTCanvas _surfaceW;
    private final GWTCanvasPanel _canvasPanel;

 //======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public GWTGraphics() {

        _canvasPanel= new GWTCanvasPanel();
        _surfaceW= _canvasPanel.getCanvas();
        _group= new GWTGraphicsGroup(_canvasPanel);
    }

    public Widget getWidget() { return _canvasPanel; }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    public Shape drawLine(String color,
                         boolean front,
                         int sx,
                         int sy,
                         int ex,
                         int ey) {
        return drawLine(color,front, DEF_WIDTH,sx,sy,ex,ey);
    }

    public Shape drawLine(String color,
                         boolean front,
                         int lineWidth,
                         int sx,
                         int sy,
                         int ex,
                         int ey) {
        return _group.drawLine(color,front,lineWidth,sx,sy,ex,ey);

    }

    public Shape drawText(String color, String size, int x, int y, String text) {
        return _group.drawText(text,size,color,x,y);
    }

    public Shape drawText(String color,
                          String fontFamily,
                          String size,
                          String fontWeight,
                          String fontStyle,
                          int x,
                          int y,
                          String text) {

        return _group.drawText(color,fontFamily,size,fontWeight,fontStyle,x,y,text);
    }

    public Shape drawCircle(String color, boolean front, int lineWidth, int x, int y, int radius) {
        return _group.drawCircle(color,front,lineWidth,x,y,radius);
    }

    public Shape drawRec(String color,
                        boolean front,
                        int lineWidth,
                        int x,
                        int y,
                        int width,
                        int height) {


        return _group.drawRec(color,front,lineWidth,x,y,width,height);
    }


    public Shape fillRec(String color,
                         boolean front,
                         int x,
                         int y,
                         int width,
                         int height) {

        return _group.fillRec(color,front,x,y,width,height);
    }



    public void deleteShapes(Shapes shapes) {
        _group.deleteShapes(shapes);
    }

    public void clear() {
        _group.clear();
        _surfaceW.clear();
    }

    public void paint() { }
    public boolean getDrawingAreaChangeClear() { return true;}

    public void setDrawingAreaSize(int width, int height) {
        if(_surfaceW.getOffsetWidth()!=width ||
           _surfaceW.getOffsetHeight()!=height ||
           _surfaceW.getCoordWidth()!=width ||
           _surfaceW.getCoordHeight()!=height) {

            _surfaceW.setPixelSize(width,height);
            _surfaceW.setCoordSize(width, height);
            _canvasPanel.setPixelSize(width,height);
            _group.redrawAll();
        }
    }

    public boolean getSupportsPartialDraws() { return true;}
    public boolean getSupportsShapeChange() { return false; }



//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

// =====================================================================
// -------------------- Native Methods --------------------------------
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
