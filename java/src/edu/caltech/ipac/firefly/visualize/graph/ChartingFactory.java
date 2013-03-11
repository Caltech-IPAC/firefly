package edu.caltech.ipac.firefly.visualize.graph;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;
import com.googlecode.gchart.client.GChartCanvasFactory;
import com.googlecode.gchart.client.GChartCanvasLite;
import com.google.gwt.user.client.Element;
//import edu.caltech.ipac.firefly.visualize.draw.Graphics;
//import edu.caltech.ipac.firefly.visualize.draw.JSGraphics;



/**
 * User: roby
 * Date: Mar 5, 2009
 * Time: 9:32:34 AM
 */


/**
 * @author Trey Roby
 */
public class ChartingFactory implements GChartCanvasFactory {

   private static final ChartingFactory _instance= new ChartingFactory();

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    private ChartingFactory() {
    }

// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

    public static ChartingFactory getInstance() { return _instance;  }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public GChartCanvasLite create() {
//        GwtUtil.showDebugMsg("create");
        GChartCanvasLite canvas;
        if (Canvas.isSupported()) {
            canvas= new GChartGraphicsCanvasImpl();
        } else {
            canvas = new GChartLightCanvasImpl();
        }
        return canvas;
    }



// =====================================================================
// -------------------- Inner classes --------------------------------
// =====================================================================

    public static class GChartLightCanvasImpl extends GWTCanvas  implements GChartCanvasLite {

        public void setFillStyle(String cssColor) {
            super.setFillStyle(new Color(cssColor));
        }

        public void setStrokeStyle(String cssColor) {
            super.setStrokeStyle(new Color(cssColor));
        }
    }


    public static class GChartGraphicsCanvasImpl extends Widget implements GChartCanvasLite {

        Canvas canvas;
       	Context2d canvasContext;

       	public GChartGraphicsCanvasImpl() {
       		canvas = Canvas.createIfSupported();
       		canvasContext = canvas.getContext2d();
       	}
       	@Override
       	public Element getElement() {
       		return canvas.getElement();
       	}
       	public void setStrokeStyle(String cssColor) {
       		// Sharp angles of default MITER can overwrite adjacent pie slices
       		canvasContext.setLineJoin(Context2d.LineJoin.ROUND);
       		canvasContext.setStrokeStyle(cssColor);
       	}
       	public void setFillStyle(String cssColor) {
       		canvasContext.setFillStyle(cssColor);
       	}
       	public void arc(double x, double y, double radius, double startAngle, double endAngle, boolean antiClockwise) {
       		canvasContext.arc(x, y, radius, startAngle, endAngle, antiClockwise);
       	}
       	public void beginPath() {
       		canvasContext.beginPath();
       	}
       	public void clear() {
       		canvasContext.clearRect(0, 0, canvas.getCoordinateSpaceWidth(), canvas.getCoordinateSpaceHeight());
       	}
       	public void closePath() {
       		canvasContext.closePath();
       	}
       	public void fill() {
       		canvasContext.fill();
       	}
       	public void lineTo(double x, double y) {
       		canvasContext.lineTo(x, y);
       	}
       	public void moveTo(double x, double y) {
       		canvasContext.moveTo(x, y);
       	}
       	public void resize(int width, int height) {
       		canvas.setCoordinateSpaceWidth(width);
       		canvas.setCoordinateSpaceHeight(height);
       	}
       	public void setLineWidth(double width) {
       		canvasContext.setLineWidth(width);
       	}
       	public void stroke() {
       		canvasContext.stroke();
       	}
    }

    /*
    public static class GChartJSGraphicsCanvasImpl extends Composite implements GChartCanvasLite {

        private int _originalX;
        private int _originalY;
        private int _x;
        private int _y;
        private String _id;
        private JSGraphics _graphics= null;
        private String _strokeColor= "red";
        private String _fillColor= "blue";
         private int _lineWidth= 1;

        public GChartJSGraphicsCanvasImpl () {

            _graphics= new JSGraphics();
            _graphics.getWidget().setStyleName("GChartJSGraphicsCanvasImpl");
            initWidget(_graphics.getWidget());
        }

        private Graphics getGraphics() {
            _graphics.init();
            return _graphics;
        }


        public void arc(double x, double y, double radius, double startAngle, double endAngle, boolean antiClockwise) {
            int size= (int)(radius*2F);
            ((JSGraphics)getGraphics()).fillArc(_fillColor,
                                                (int)x,(int)y,size,size,
                                                (float)startAngle,(float)endAngle);
        }

        public void beginPath() {
            _originalX=_x= 0;
            _originalY=_y= 0;
        }

        public void clear() {
            if (_graphics!=null) {
                getGraphics().clear();
            }
        }

        public void closePath() {
            getGraphics().drawLine(_strokeColor,false,_x,_y,_originalX,_originalY);
            _x= _originalX;
            _y= _originalY;
        }

        public void fill() {
        }

        public void lineTo(double x, double y) {
            getGraphics().drawLine(_strokeColor,false,_x,_y,(int)x,(int)y);
            _x= (int)x;
            _y= (int)y;
        }

        public void moveTo(double x, double y) {
            _originalX=_x= (int)y;
            _originalY=_y= (int)y;
        }

        public void setBackgroundColor(String cssColor) {
        }

        public void resize(int width, int height) {
            _graphics.getWidget().setSize(width+"px", height+"px");
        }

        public void setFillStyle(String cssColor) {
            _fillColor= cssColor;
        }

        public void setLineWidth(double width) {
            _lineWidth= (int)width;
        }

        public void setStrokeStyle(String cssColor) {
            _strokeColor= cssColor;
        }

        public void stroke() {
            getGraphics().paint();
        }
    }
    */
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
