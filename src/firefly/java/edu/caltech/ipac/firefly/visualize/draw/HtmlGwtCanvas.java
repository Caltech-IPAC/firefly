/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
import java.util.logging.Level;


/**
 * @author Trey Roby
 */
public class HtmlGwtCanvas implements AdvancedGraphics {

    private final List<CanvasLabelShape> _labelList= new ArrayList<CanvasLabelShape>(20);
    private final CanvasPanel panel;
    private HtmlGwtCanvas labelPanel;
    private final CanvasElement cElement;
    private final Context2d ctx;
    private Shadow nextDrawShadow= null;
    private ScreenPt nextDrawTranslation= null;
    //Rotation angle - see context2d setRotate
	private double nextDrawRotation= 0;

 //======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public HtmlGwtCanvas() {
        panel= new CanvasPanel();
        labelPanel= this;
        cElement= panel.getCanvas().getCanvasElement();
        ctx= cElement.getContext2d();
    }

    public Widget getWidget() { return panel; }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void setLabelPanel(HtmlGwtCanvas p) {
        labelPanel= p;
    }

    public void setTranslationPerm(ScreenPt pt) {
        setTranslation(pt);
    }

    public void setTranslationForNextDraw(ScreenPt pt) {
        nextDrawTranslation= pt;
    }

    /* (non-Javadoc)
     * @see edu.caltech.ipac.firefly.visualize.draw.AdvancedGraphics#setRotationForNextDraw(double)
     */
    public void setRotationForNextDraw(double radAngle) {
		this.nextDrawRotation = radAngle;
		
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
//        ctx.save();
//        checkMods();
//        ctx.setLineWidth(lineWidth);
//        ctx.setStrokeStyle(makeColor(color));
//        ctx.beginPath();
//        ctx.moveTo(x,y);
//        ctx.lineTo(x+width,y);
//        ctx.lineTo(x+width,y+height);
//        ctx.lineTo(x,y+height);
//        ctx.lineTo(x,y);
//        ctx.stroke();
//        ctx.restore();


        ctx.save();
        checkMods();
        ctx.setLineWidth(lineWidth);
        ctx.setStrokeStyle(makeColor(color));
        ctx.strokeRect(x,y,width,height);
//        ctx.stroke();
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

    public void beginPath(String color, int lineWidth) {
        ctx.save();
        checkMods();
        ctx.setLineWidth(lineWidth);
        ctx.setStrokeStyle(makeColor(color));
        ctx.beginPath();
    }

    public void pathMoveTo(int x,int y) {
        ctx.moveTo(x,y);

    }

    public void pathLineTo(int x,int y) {
        ctx.lineTo(x,y);
    }

    public void rect(int x, int y, int width, int height) {
    	ctx.rect(x,y,width,height);
    }

    public void arc(int x,int y, double radius, double startAngle, double endAngle) {
        ctx.arc(x,y,radius,startAngle,endAngle);
    }

    public void drawPath() {
    	ctx.stroke();
        ctx.restore();
    }

    public void drawPath(String color,
                         int lineWidth,
                         List<PathType> ptList) {
        ctx.save();
        checkMods();
        ctx.setLineWidth(lineWidth);
        ctx.setStrokeStyle(makeColor(color));
        ctx.beginPath();

        boolean first= true;
        for(PathType pT : ptList) {
            if (!pT.isDraw() || first) {
                ctx.moveTo(pT.getX(),pT.getY());
                first=  false;
            }
            else {
                ctx.lineTo(pT.getX(),pT.getY());
            }
        }
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
        if (nextDrawRotation!=0) {
        	GwtUtil.logToServer(Level.INFO, ""
    				+ "checkMods() with rotation "+nextDrawRotation);
        	setRotation(nextDrawRotation);
            nextDrawRotation= 0;
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
        labelPanel._labelList.add(labelShape);
        labelPanel.panel.addLabel(label,x,y);
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


    public void copyAsImage(AdvancedGraphics g) {
        Canvas sourceCanvas= g.getCanvasPanel().getCanvas();
        int w= sourceCanvas.getCoordinateSpaceWidth();
        int h= sourceCanvas.getCoordinateSpaceHeight();
        ctx.drawImage(sourceCanvas.getCanvasElement(), 0,0, w,h, 0,0, w,h  );
    }

    public CanvasPanel getCanvasPanel() {
        return panel;
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
        if (s!=null) {
            ctx.setShadowBlur(s.getBlur());
            ctx.setShadowOffsetX(s.getOffX());
            ctx.setShadowOffsetY(s.getOffY());
            ctx.setShadowColor(s.getColor());
        }
        else {
            ctx.setShadowColor("transparent");
        }
    }

    private void setTranslation(ScreenPt pt) {
        ctx.translate(pt.getIX(), pt.getIY());
    }
    /**
     * Set rotation to rotation angle
     * @param ang angle to rotate in radians
     */
    private void setRotation(double ang) {
        ctx.rotate(ang);
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

// =====================================================================
// -------------------- Native Methods --------------------------------
// =====================================================================


}
