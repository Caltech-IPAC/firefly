package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.visualize.ScreenPt;

import java.util.ArrayList;
import java.util.List;
/**
 * User: roby
 * Date: Oct 1, 2008
 * Time: 11:21:49 AM
 */


/**
 * @author Trey Roby
 */
public class HtmlGwtCanvas implements AdvancedGraphics {

    private final List<CanvasLabelShape> _labelList= new ArrayList<CanvasLabelShape>(20);
    private final CanvasPanel panel;
    private final CanvasElement cElement;
    private final Context2d ctx;
    private Shadow nextDrawShadow= null;
    private ScreenPt nextDrawTranslation= null;

 //======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public HtmlGwtCanvas() {
        panel= new CanvasPanel();
        cElement= panel.getCanvas().getCanvasElement();
        ctx= cElement.getContext2d();
    }

    public Widget getWidget() { return panel; }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    public void setTranslationPerm(ScreenPt pt) {
        setTranslation(pt);
    }

    public void setTranslationForNextDraw(ScreenPt pt) {
        nextDrawTranslation= pt;
    }

    public void clearTranslation() {
        setTranslation(new ScreenPt(0,0));
    }

    public void setShadowPerm(Shadow s) {
        setShadow(s);
    }



    public void setShadowForNextDraw(Shadow s) {
        nextDrawShadow= s;
    }

    public void clearShadow() {
        ctx.setShadowColor("transparent");
        ctx.setShadowBlur(0);
        ctx.setShadowOffsetX(0);
        ctx.setShadowOffsetX(0);
        nextDrawShadow= null;
    }

    public static boolean isSupported() { return CanvasPanel.isSupported();  }

    public void drawLine(String color,
                         int sx,
                         int sy,
                         int ex,
                         int ey) {
        drawLine(color, DEF_WIDTH, sx, sy, ex, ey);
    }

    public void drawLine(String color,
                         int lineWidth,
                         int sx,
                         int sy,
                         int ex,
                         int ey) {
        ctx.save();
        checkMods();
        ctx.setLineWidth(lineWidth);
        ctx.setStrokeStyle(makeColor(color));
        ctx.beginPath();
        ctx.moveTo(sx, sy);
        ctx.lineTo(ex, ey);
//        ctx.closePath();
        ctx.stroke();
        ctx.restore();

    }

    public void drawCircle(String color, int lineWidth, int x, int y, int radius) {
        ctx.save();
        checkMods();
        ctx.setLineWidth(lineWidth);
        ctx.setStrokeStyle(makeColor(color));
        ctx.beginPath();
        ctx.arc(x,y,radius,0,2*Math.PI);
//        ctx.closePath();
        ctx.stroke();
        ctx.restore();
    }

    public void drawRec(String color,
                        int lineWidth,
                        int x,
                        int y,
                        int width,
                        int height) {
        ctx.save();
        checkMods();
        ctx.setLineWidth(lineWidth);
        ctx.setStrokeStyle(makeColor(color));
        ctx.beginPath();
        ctx.moveTo(x,y);
        ctx.lineTo(x+width,y);
        ctx.lineTo(x+width,y+height);
        ctx.lineTo(x,y+height);
        ctx.lineTo(x,y);
//        ctx.closePath();
        ctx.stroke();
        ctx.restore();
    }

    public void drawPath(String color,
                         int lineWidth,
                         List<ScreenPt> pts,
                         boolean close) {
        ctx.save();
        checkMods();
        ctx.setLineWidth(lineWidth);
        ctx.setStrokeStyle(makeColor(color));
        ctx.beginPath();

        boolean first= true;
        for(ScreenPt pt : pts) {
            if (first) {
                first=  false;
                ctx.moveTo(pt.getX(),pt.getY());
            }
            else {
                ctx.lineTo(pt.getX(),pt.getY());
            }
        }
        if (close) ctx.closePath();
        ctx.stroke();
        ctx.restore();
    }

    public void fillRec(String color,
                        int x,
                        int y,
                        int width,
                        int height) {
        ctx.save();
        checkMods();
        ctx.setLineWidth(1);
        ctx.setFillStyle(makeColor(color));
        ctx.fillRect(x, y, width, height);
        ctx.restore();
    }

    private void checkMods() {
        if (nextDrawShadow!=null) {
            setShadow(nextDrawShadow);
            nextDrawShadow= null;
        }

        if (nextDrawTranslation!=null) {
            setTranslation(nextDrawTranslation);
            nextDrawTranslation= null;
        }
    }


    public void drawText(String color,
                         String fontFamily,
                         String size,
                         String fontWeight,
                         String fontStyle,
                         int x,
                         int y,
                         String text) {
        HTML label= DrawUtil.makeDrawLabel(color, fontFamily, size, fontWeight, fontStyle, text);
        CanvasLabelShape labelShape= new CanvasLabelShape(label);
        _labelList.add(labelShape);
        panel.addLabel(label,x,y);
    }

    public void drawText(String color, String size, int x, int y, String text) {
        drawText(color, "inherit", size, "normal",  "normal", x, y, text);
    }




    public void clear() {
        for(CanvasLabelShape label : _labelList) {
            panel.removeLabel(label.getLabel());
        }
        ctx.clearRect(0,0,cElement.getWidth(),cElement.getHeight());
        _labelList.clear();
    }

    public void paint() { }

    public void setDrawingAreaSize(int width, int height) {
        Canvas c= panel.getCanvas();
        if(cElement.getWidth()!=width || cElement.getHeight()!=height ||
           c.getCoordinateSpaceWidth()!=width || c.getCoordinateSpaceHeight()!=height) {

            panel.setPixelSize(width,height);
            cElement.setWidth(width);
            cElement.setHeight(height);

        }
    }



//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    public static CssColor makeColor(String c)  {
        if (GwtUtil.isHexColor(c))  {
            c= "#" + c;
        }
        return CssColor.make(c);
    }


    private void setShadow(Shadow s) {
        ctx.setShadowBlur(s.getBlur());
        ctx.setShadowOffsetX(s.getOffX());
        ctx.setShadowOffsetY(s.getOffY());
        ctx.setShadowColor(s.getColor());
    }

    private void setTranslation(ScreenPt pt) {
        ctx.translate(pt.getIX(), pt.getIY());
    }



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
