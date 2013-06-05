package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.util.WebUtil;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.draw.AutoColor;
import edu.caltech.ipac.firefly.visualize.draw.DirectionArrowDataObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.Drawer;
import edu.caltech.ipac.firefly.visualize.draw.ImageCoordsBoxObj;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * User: roby
 * Date: Sep 28, 2009
 * Time: 11:52:42 AM
 */


/**
 * @author Trey Roby
 */
public class ThumbnailView extends Composite {


    private final AbsolutePanel _panel= new AbsolutePanel();
    private final Image _thumbnailImage= new Image();
    private final DefaultDrawable _drawable= new DefaultDrawable();
    private final WebPlotView _pv;
    private Drawer _drawer= null;
    private WebPlot _lastPlot= null;
    private VerticalPanel tnWrapper= new VerticalPanel();
    private boolean _parentShowing= true;
    private boolean _needsUpdate= false;
    DirectionArrowDataObj _dataN= null;
    DirectionArrowDataObj _dataE= null;
    ImageCoordsBoxObj _scrollBox;
    private boolean _mouseDown= false;
    private final int maxSize;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public ThumbnailView(WebPlotView pv, int size) {

        _pv= pv;
        maxSize= size;
        pv.addListener(Name.PRIMARY_PLOT_CHANGE,
                  new WebEventListener() {
                      public void eventNotify(WebEvent ev) {
                          if (_parentShowing) updateThumbnail();
                          else _needsUpdate= true;
                      }
                  });
        pv.addListener(Name.REPLOT,
                  new WebEventListener() {
                      public void eventNotify(WebEvent ev) {
                          ReplotDetails details= (ReplotDetails)ev.getData();
                          if (details.getReplotReason()== ReplotDetails.Reason.IMAGE_RELOADED) {
                              if (_parentShowing) updateThumbnail();
                              else _needsUpdate= true;
                          }
                      }
                  });

        pv.addScrollHandler(new ScrollHandler() {
            public void onScroll(ScrollEvent event) {
                if (_parentShowing) updateThumbnail();
                else _needsUpdate= true;
            }
        });


        Widget drawingWidget= _drawable.getDrawingPanelContainer();

        drawingWidget.addDomHandler(new MouseDownHandler() {
            public void onMouseDown(MouseDownEvent ev) { beginMove(); }
        }, MouseDownEvent.getType());

        drawingWidget.addDomHandler(new MouseMoveHandler() {
            public void onMouseMove(MouseMoveEvent ev) { move(ev.getX(), ev.getY()); }
        }, MouseMoveEvent.getType());

        drawingWidget.addDomHandler(new MouseUpHandler() {
            public void onMouseUp(MouseUpEvent ev) { endMove(); }
        }, MouseUpEvent.getType());

        drawingWidget.addDomHandler(new TouchStartHandler() {
            public void onTouchStart(TouchStartEvent ev) { beginMove(); }
        }, TouchStartEvent.getType());

        drawingWidget.addDomHandler(new TouchMoveHandler() {
            public void onTouchMove(TouchMoveEvent ev) {
                Touch t= ev.getTouches().get(0);
                int x= t.getClientX() - _panel.getAbsoluteLeft();
                int y= t.getClientY() - _panel.getAbsoluteTop();
                move(x,y);
            }
        }, TouchMoveEvent.getType());

        drawingWidget.addDomHandler(new TouchEndHandler() {
            public void onTouchEnd(TouchEndEvent ev) { endMove(); }
        }, TouchEndEvent.getType());



        _panel.clear();
        tnWrapper.add(_thumbnailImage);
        _panel.add(tnWrapper);
        initWidget(_panel);
        _panel.add(drawingWidget,0,0);
        _drawable.setPixelSize(size,size);
    }

    void setParentShowingHint(boolean showing) {
        _parentShowing= showing;
        if (showing && _needsUpdate) updateThumbnail();
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void beginMove() {
        _mouseDown= true;

    }

    private void move(int x, int y) {
        int w= _thumbnailImage.getWidth();
        int h= _thumbnailImage.getHeight();
        if (_mouseDown) {
            thumbnailMove(x, y, w, h,true);
        }
        else {
            thumbnailMove(x, y, w, h,false);
        }

    }

    private void endMove() {
        _mouseDown= false;

    }

    private void updateThumbnail() {
        if (_drawer==null) _drawer= new Drawer(_pv, _drawable, true);
        _needsUpdate= false;
        WebPlot plot= _pv.getPrimaryPlot();
        if (plot==null) return;
        PlotImages pi= plot.getTileDrawer().getImages();
        if (pi==null) return;
        PlotImages.ThumbURL tn= pi.getThumbnail();
        if (tn==null) return;
        String url= createImageUrl(tn.getURL());
        _thumbnailImage.setUrl(url);

        int w= tn.getWidth();
        int h= tn.getHeight();
        draw(w,h);
        _drawable.setPixelSize(w,h);
        _panel.setWidgetPosition(_drawable.getDrawingPanelContainer(), 0,0);
        _panel.setPixelSize(maxSize,maxSize);
    }

    private float getThumbZoomFact(WebPlot plot, int thumbW, int thumbH) {
        float tSize= Math.max(thumbW, thumbH);
        float iSize= Math.max( plot.getImageWidth(), plot.getImageHeight());
        return tSize / iSize;
    }

    private void draw(int width, int height) {


        WebPlot plot= _pv.getPrimaryPlot();
        try {
            int arrowLength= (width+height)/4;
            _lastPlot= plot;
            float thumbZoomFact= getThumbZoomFact(plot,width,height);
            double iWidth= plot.getImageWidth();
            double iHeight= plot.getImageHeight();
            double ix= iWidth/2;
            double iy= iHeight/2;
            WorldPt wptC= plot.getWorldCoords(new ImageWorkSpacePt(ix,iy));
            double cdelt1 = plot.getImagePixelScaleInDeg();
            WorldPt wpt2= new WorldPt(wptC.getLon(), wptC.getLat() + (Math.abs(cdelt1)/thumbZoomFact)*(arrowLength/2));
            WorldPt wptE2= new WorldPt(wptC.getLon()+(Math.abs(cdelt1)/thumbZoomFact)*(arrowLength/2), wptC.getLat());
            WorldPt wptE1= wptC;
            WorldPt wpt1= wptC;

            ScreenPt spt1= plot.getScreenCoords(wpt1, thumbZoomFact);
            ScreenPt spt2= plot.getScreenCoords(wpt2, thumbZoomFact);

            ScreenPt sptE1= plot.getScreenCoords(wptE1, thumbZoomFact);
            ScreenPt sptE2= plot.getScreenCoords(wptE2, thumbZoomFact);

            _dataN= new DirectionArrowDataObj(spt1, spt2,"N");
            _dataE= new DirectionArrowDataObj(sptE1, sptE2,"E");

            _drawer.setDefaultColor(AutoColor.DRAW_1);

                 // the following line keeps the thumbnail from appearing very large if something goes wrong
//            int tw= Math.max(_thumbnailImage.getWidth(),maxSize);
//            int th= Math.max(_thumbnailImage.getHeight(),maxSize);
            tnWrapper.setPixelSize(maxSize,maxSize);

            _drawable.setPixelSize(_thumbnailImage.getWidth(),_thumbnailImage.getHeight());
            updateScrollBox(width,height);
            redrawGraphics();
        } catch (ProjectionException e) {
            clearGraphics();
        }
    }

    private void redrawGraphics() {
        _drawer.setData(Arrays.asList(new DrawObj[] {_dataN,_dataE, _scrollBox} ));
        DeferredCommand.addCommand(new Command() {
            public void execute() { _drawer.redraw(); }
        });
    }

    private void clearGraphics() {
        _drawer.setData(new ArrayList<DrawObj>(0));
        DeferredCommand.addCommand(new Command() {
            public void execute() { _drawer.redraw(); }
        });
    }

    private void updateScrollBox(int thumbWidth, int thumbHeight) {
        WebPlot plot= _pv.getPrimaryPlot();

        float fact= getThumbZoomFact(plot,thumbWidth,thumbHeight) / plot.getZoomFact();

        float zfact= plot.getZoomFact();
        int offX= (int)(plot.getOffsetX()*zfact);
        int offY= (int)(plot.getOffsetY()*zfact);


        int tsX= (int)((_pv.getScrollX()-offX)*fact);
        int tsY= (int)((_pv.getScrollY()-offY)*fact);
        int tsWidth= (int)(_pv.getScrollWidth()*fact);
        int tsHeight= (int)(_pv.getScrollHeight()*fact);

        _scrollBox= new ImageCoordsBoxObj(new ScreenPt(tsX,tsY),
                                                    tsWidth,tsHeight);
        _scrollBox.setStyle(ImageCoordsBoxObj.Style.LIGHT);
        _scrollBox.setColor(AutoColor.DRAW_2);
    }


    private void thumbnailMove(int thumbX, int thumbY, int thumbWidth, int thumbHeight, boolean moveImage) {
        WebPlot plot= _pv.getPrimaryPlot();

        float fact= getThumbZoomFact(plot,thumbWidth,thumbHeight);

        ImageWorkSpacePt ipt= plot.getImageWorkSpaceCoords(new ScreenPt(thumbX,thumbY),fact);
        ScreenPt spt= plot.getScreenCoords(ipt);
        if (moveImage) plot.getPlotView().centerOnPoint(ipt);
        _pv.fireMouseMove(spt);
    }

    private String createImageUrl(String imageURL) {
        WebPlot plot= _pv.getPrimaryPlot();
        Param[] params= new Param[] {
                new Param("file", imageURL),
                new Param("type", "thumbnail"),
                new Param("state", plot.getPlotState().toString()),
//                new Param("ctx", plot.getPlotState().getContextString()),
        };
        return WebUtil.encodeUrl(GWT.getModuleBaseURL()+ "sticky/FireFly_ImageDownload", params);
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

