/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.firefly.visualize.draw.Drawer;
import edu.caltech.ipac.firefly.visualize.draw.DrawingDef;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
import edu.caltech.ipac.util.ComparisonUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: roby
 * Date: Sep 28, 2009
 * Time: 11:52:42 AM
 */


/**
 * @author Trey Roby
 */
public class MagnifiedView extends Composite {

    private AbsolutePanel _magView = new AbsolutePanel();
    private final int _size;
    private final WebPlotView _pv;
    private final Image _ne = new Image();
    private final Image _nw = new Image();
    private final Image _se = new Image();
    private final Image _sw = new Image();
    private final DefaultDrawable _drawable = new DefaultDrawable();
    private Drawer _drawer = null;
    private boolean _freeze= false;

    private TileDrawer.ImageReturn _savImages = null;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public MagnifiedView(WebPlotView pv, int size) {

        _size = size;
        _pv = pv;
        this.initWidget(_magView);
        _magView.setStyleName("NO-STYLE");
        _magView.setSize(_size + "px", _size + "px");
        DOM.setStyleAttribute(_magView.getElement(), "overflow", "hidden");

        _pv.addPersistentMouseInfo(new WebPlotView.MouseInfo(new MagMouse(), "Show Magnifier"));

        _magView.add(_ne);
        _magView.add(_nw);
        _magView.add(_se);
        _magView.add(_sw);
        _magView.add(_drawable.getDrawingPanelContainer(), 0, 0);

        _drawable.setPixelSize(_size, _size);

        _drawer = new Drawer(_pv, _drawable, false);
//        _drawer.setDefaultColor(DrawingDef.COLOR_DRAW_1);
        _drawer.setDrawingDef(new DrawingDef(DrawingDef.COLOR_DRAW_1));


        _ne.setVisible(false);
        _nw.setVisible(false);
        _se.setVisible(false);
        _sw.setVisible(false);
    }

    public void setFreezeView(boolean f) { _freeze= f;}
    public void update(ScreenPt spt) {if (_freeze) showMag(spt);}

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void showMag(ScreenPt spt) {

        WebPlot plot = _pv.getPrimaryPlot();

        if (plot == null) return;
        if (plot.isBlankImage()) return;


//        if (VisUtil.isLargePlot(plot.getZoomFact(), plot.getScreenWidth(), plot.getScreenHeight())) {
        if (plot.getZoomFact() > 6) {
            clear();
            return;
        }

        int mouseX = spt.getIX();
        int mouseY = spt.getIY();
        int x = mouseX - _size / 2;
        int y = mouseY - _size / 2;

        int screenW = plot.getScreenWidth();
        int screenH = plot.getScreenHeight();


        final int sizeOffX;

        if (x < 0) {
            x = 0;
            sizeOffX = mouseX;
        } else if (x > screenW - _size) {
            x = screenW - _size;
            sizeOffX = (_size / 2) + ((_size / 2) - (screenW - mouseX));
        } else {
            sizeOffX = _size / 2;
        }


        final int sizeOffY;
        if (y < 0) {
            y = 0;
            sizeOffY = mouseY;
        } else if (y > screenH - _size) {
            y = screenH - _size;
            sizeOffY = (_size / 2) + ((_size / 2) - (screenH - mouseY));
        } else {
            sizeOffY = _size / 2;
        }


//        GwtUtil.showDebugMsg("sizeOffX= " + sizeOffX);

        if (plot == null) return;

        TileDrawer.ImageReturn images = plot.getTileDrawer().getImagesAt(new ScreenPt(x, y), _size);
        if (images == null) return;

        List<PlotImages.ImageURL> serverTiles = images.getServerTiles();
        List<TileDrawer.ImageTileData> imageTiles = images.getImageTiles();
        int tsize = imageTiles.size();


        if (tsize == 1) {
            _ne.setVisible(true);
            _nw.setVisible(false);
            _se.setVisible(false);
            _sw.setVisible(false);
            if (tilesChanged(images)) {
                TileDrawer.ImageTileData iu = imageTiles.get(0);
                _ne.setUrl(TileDrawer.createImageUrl(plot, serverTiles.get(0)));
                _ne.setPixelSize(iu.getWidth() * 2, iu.getHeight() * 2);
            }


            _magView.setWidgetPosition(_ne, -2 * images.getX() - sizeOffX, -2 * images.getY() - sizeOffY);
            redrawGraphics(sizeOffX, sizeOffY);

//            _magView.setWidgetPosition(_ne,0,0);
        } else if (tsize == 2) {
            TileDrawer.ImageTileData t1UI = imageTiles.get(0);
            TileDrawer.ImageTileData t2UI = imageTiles.get(1);

            if (t1UI.getX() < t2UI.getX()) {  // tiles are horizontal
//                GwtUtil.showDebugMsg("tsize= 2H");
                _ne.setVisible(true);
                _nw.setVisible(true);
                _se.setVisible(false);
                _sw.setVisible(false);

                if (tilesChanged(images)) {
                    _ne.setUrl(TileDrawer.createImageUrl(plot, serverTiles.get(0)));
                    _nw.setUrl(TileDrawer.createImageUrl(plot, serverTiles.get(1)));
                    _ne.setPixelSize(t1UI.getWidth() * 2, t1UI.getHeight() * 2);
                    _nw.setPixelSize(t2UI.getWidth() * 2, t2UI.getHeight() * 2);
                }

                _magView.setWidgetPosition(_ne, -2 * (images.getX()) - sizeOffX, -2 * (images.getY()) - sizeOffY);
                _magView.setWidgetPosition(_nw, -2 * (images.getX()) - sizeOffX + t1UI.getWidth() * 2, -2 * (images.getY()) - sizeOffY);

            } else { // tiles are vertical

//                    GwtUtil.showDebugMsg("tsize= 2V");
                _ne.setVisible(true);
                _nw.setVisible(false);
                _se.setVisible(true);
                _sw.setVisible(false);

                if (tilesChanged(images)) {
                    _ne.setUrl(TileDrawer.createImageUrl(plot, serverTiles.get(0)));
                    _se.setUrl(TileDrawer.createImageUrl(plot, serverTiles.get(1)));
                    _ne.setPixelSize(t1UI.getWidth() * 2, t1UI.getHeight() * 2);
                    _se.setPixelSize(t2UI.getWidth() * 2, t2UI.getHeight() * 2);
                }

                _magView.setWidgetPosition(_ne, -2 * (images.getX()) - sizeOffX, -2 * (images.getY()) - sizeOffY);
                _magView.setWidgetPosition(_se, -2 * (images.getX()) - sizeOffX, -2 * (images.getY()) - sizeOffY + t1UI.getHeight() * 2);


            }
            redrawGraphics(sizeOffX, sizeOffY);

        } else if (tsize == 4) {
//            GwtUtil.showDebugMsg("tsize= 4" );
            TileDrawer.ImageTileData tNE = imageTiles.get(0);
            TileDrawer.ImageTileData tSE = imageTiles.get(1);
            TileDrawer.ImageTileData tNW = imageTiles.get(2);
            TileDrawer.ImageTileData tSW = imageTiles.get(3);


            _ne.setVisible(true);
            _nw.setVisible(true);
            _se.setVisible(true);
            _sw.setVisible(true);

            if (tilesChanged(images)) {
                _ne.setUrl(TileDrawer.createImageUrl(plot, serverTiles.get(0)));
                _se.setUrl(TileDrawer.createImageUrl(plot, serverTiles.get(1)));
                _nw.setUrl(TileDrawer.createImageUrl(plot, serverTiles.get(2)));
                _sw.setUrl(TileDrawer.createImageUrl(plot, serverTiles.get(3)));

                _ne.setPixelSize(tNE.getWidth() * 2, tNE.getHeight() * 2);
                _se.setPixelSize(tSE.getWidth() * 2, tSE.getHeight() * 2);
                _nw.setPixelSize(tNW.getWidth() * 2, tNW.getHeight() * 2);
                _sw.setPixelSize(tSW.getWidth() * 2, tSW.getHeight() * 2);
            }

            _magView.setWidgetPosition(_ne, -2 * (images.getX()) - sizeOffX, -2 * (images.getY()) - sizeOffY);
            _magView.setWidgetPosition(_se, -2 * (images.getX()) - sizeOffX, -2 * (images.getY()) - sizeOffY + tNE.getHeight() * 2);
            _magView.setWidgetPosition(_nw, -2 * (images.getX()) - sizeOffX + tNE.getWidth() * 2, -2 * (images.getY()) - sizeOffY);
            _magView.setWidgetPosition(_sw, -2 * (images.getX()) - sizeOffX + tNE.getWidth() * 2,
                                       -2 * (images.getY()) - sizeOffY + tNE.getHeight() * 2);
            redrawGraphics(sizeOffX, sizeOffY);

        } else {
            // if tile is not 1,2, or 4 the I am in a zoom down case and it will fix itself, so ignore
        }
        _savImages = images;

    }

    private boolean tilesChanged(TileDrawer.ImageReturn images) {
        boolean retval = true;
        if (_savImages != null) {
            boolean equals = ComparisonUtil.equals(images.getServerTiles(), _savImages.getServerTiles());
            retval = !equals;
        }
//        GwtUtil.showDebugMsg("tsize= "+ images.getTiles().size() + "change= "+ (retval));
        return retval;
    }

    private void clear() {
        _ne.setVisible(false);
        _nw.setVisible(false);
        _se.setVisible(false);
        _sw.setVisible(false);
        clearGraphics();
    }

    private void redrawGraphics(int x, int y) {
        PointDataObj drawObj = new PointDataObj(new ScreenPt(x, y), DrawSymbol.EMP_CROSS);
        _drawer.setData(Arrays.asList(new DrawObj[]{drawObj}));
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                _drawer.redraw();
            }
        });
    }

    private void clearGraphics() {
        _drawer.setData(new ArrayList<DrawObj>(0));
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                _drawer.redraw();
            }
        });
    }

// =====================================================================
// -------------------- Inner Classes --------------------------------
// =====================================================================

    private class MagMouse extends WebPlotView.DefMouseAll {

        @Override
        public void onMouseMove(WebPlotView pv, ScreenPt spt, MouseMoveEvent ev) {
            if (!_freeze) showMag(spt);
        }

        @Override
        public void onMouseOver(WebPlotView pv, ScreenPt spt) {
            if (!_freeze) showMag(spt);
        }

        @Override
        public void onMouseOut(WebPlotView pv) {
            if (!_freeze) clear();
        }

        @Override
        public void onTouchStart(WebPlotView pv, ScreenPt spt, TouchStartEvent ev) {
            showMag(spt);
        }

        @Override
        public void onTouchMove(WebPlotView pv, ScreenPt spt, TouchMoveEvent ev) {
            showMag(spt);
        }
    }

}

