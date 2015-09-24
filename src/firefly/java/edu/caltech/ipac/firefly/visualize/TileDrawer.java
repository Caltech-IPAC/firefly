/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.WebUtil;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Mar 30, 2009
 * Time: 8:53:04 AM
 */


/**
 * @author Trey Roby
 */
public class TileDrawer {

    private static final String TRANSPARENT = GWT.getModuleBaseURL()+"images/transparent-20x20.gif";
    private static final VisIconCreator _ic = VisIconCreator.Creator.getInstance();
    private static final String BACKGROUND_STYLE = "url(" + _ic.getImageWorkingBackground().getURL() + ") top left repeat";
    private AbsolutePanel _imageWidget;
    private Map<PlotImages.ImageURL, ImageTileData> _imageStateMap = null;
    private final WebPlot _plot;
    private boolean _firstReloadComplete = true;
    private boolean _firstLoad = true;
    private PlotImages _serverTiles;
    private float _imageZoomLevel = 1;
    private boolean _scaled= false;
    private final List<HandlerRegistration> _hregList = new ArrayList<HandlerRegistration>(80);
    private static final FourTileSort _ftSort = new FourTileSort();
    private WebPlotView _pv;
    private boolean asOverlay;
    private float opacity= 1.0F;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public TileDrawer(WebPlot plot, PlotImages images, boolean asOverlay) {
        _plot = plot;
        _serverTiles = images;
        this.asOverlay= asOverlay;
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void setOpacity(float opacity) {
        this.opacity= opacity;
    }

    public void setPlotView(WebPlotView pv) {
        this._pv= pv;
    }

    AbsolutePanel getWidget() {
        if (_imageWidget == null) {
            _imageWidget = new AbsolutePanel();
            if (!asOverlay) {
                GwtUtil.setStyle(_imageWidget, "background", BACKGROUND_STYLE);
            }
            refreshWidget(_serverTiles);
        }
        return _imageWidget;
    }

    public PlotImages getImages() {
        return _serverTiles;
    }

    void refreshWidget() {
        if (_scaled) return;
        refreshWidget(_serverTiles);
    }

    void refreshWidget(PlotImages serverTiles) {
        if (_imageWidget==null) return;

        _scaled= false;
        _serverTiles = serverTiles;
        _imageZoomLevel = _plot.getZoomFact();
        _plot.getPlotGroup().computeMinMax();


        for (HandlerRegistration r : _hregList) r.removeHandler();
        _hregList.clear();
        _firstReloadComplete = false;
        _imageWidget.clear();


        _imageStateMap = new HashMap<PlotImages.ImageURL, ImageTileData>();

        Image imw;
        for (PlotImages.ImageURL serverDefinedTile : _serverTiles) {
            imw = new Image();
            if (!asOverlay) GwtUtil.setStyle(imw, "background", BACKGROUND_STYLE);

            ImageTileData tile = new ImageTileData(imw, serverDefinedTile.getXoff(),
                                                        serverDefinedTile.getYoff(),
                                                        serverDefinedTile.getWidth(),
                                                        serverDefinedTile.getHeight());

            ViewPortPt addPt= _plot.getViewPortCoords(new ScreenPt(tile._x, tile._y));
            _imageWidget.add(imw, addPt.getIX(), addPt.getIY());
            imw.setPixelSize(tile._width, tile._height);
            _imageWidget.setWidgetPosition(imw, addPt.getIX(), addPt.getIY());
            _hregList.add(imw.addLoadHandler(new PlotLoadHandler(tile)));
            _imageStateMap.put(serverDefinedTile, tile);
            if (serverDefinedTile.isCreated()) {
                getTileImage(serverDefinedTile);
            } else if (_serverTiles.size()>1) {
                imw.setUrl(TRANSPARENT);
            }
        }
        recomputeWidgetSize();
        deferredDrawTiles();
    }


    private void deferredDrawTiles() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                if (_plot.isAlive()) {
                    drawTilesForArea(new ScreenPt(_pv.getScrollX(), _pv.getScrollY()) ,
                            _pv.getScrollWidth(), _pv.getScrollHeight() );
                }
            }
        });
    }

    public void drawTilesForArea(ScreenPt viewPortLocation, int width, int height) {
        if (_plot.isAlive() && _serverTiles!=null) {
            for (PlotImages.ImageURL serverTile : _serverTiles) {
                ImageTileData tile= _imageStateMap.get(serverTile);
                if (isTileVisible(serverTile, viewPortLocation, width, height)) {
                    ViewPortPt addPt= _plot.getViewPortCoords(new ScreenPt(tile._x, tile._y));
                    GwtUtil.setStyle(tile.image, "opacity", opacity+"");
                    tile.image.setVisible(true);
                    _imageWidget.setWidgetPosition(tile.image, addPt.getIX(), addPt.getIY());
                    if (!serverTile.isCreated()) {
                        serverTile.setCreated(true);
                        getTileImage(serverTile);
                    }
                } else {
                    tile.image.setVisible(false);
                }
            }
        }
    }

    public void getTileImage(PlotImages.ImageURL image) {

        ImageTileData tile = _imageStateMap.get(image);
        if (tile != null) {
            String url = createImageUrl(_plot, image);
            tile.image.setUrl(url);
        }
    }

    public ImageReturn getImagesAt(ScreenPt spt, int size) {

        if (_serverTiles==null) return null;

        ArrayList<PlotImages.ImageURL> tiles = new ArrayList<PlotImages.ImageURL>(4);

        for (PlotImages.ImageURL image : _serverTiles) {
            if (isTileVisible(image, spt, size, size)) {
                tiles.add(image);
            }
        }
        ImageReturn retval = null;
        if (tiles.size() > 0) {
            Collections.sort(tiles, _ftSort);
            ImageTileData data = _imageStateMap.get(tiles.get(0));

            int newX = spt.getIX() - data._x;
            int newY = spt.getIY() - data._y;
            retval = new ImageReturn(tiles, newX, newY);
        }
        return retval;

    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    /**
     * This Comparator is for the very specific case that you want to arrange 4 tiles in a specific order
     */
    private static class FourTileSort implements Comparator<PlotImages.ImageURL> {
        public int compare(PlotImages.ImageURL o1, PlotImages.ImageURL o2) {

            int x1 = o1.getXoff();
            int x2 = o2.getXoff();
            int y1 = o1.getYoff();
            int y2 = o2.getYoff();

            int retval;
            if (x1 == x2) {
                if (y1 == y2) retval = 0;
                else if (y1 < y2) retval = -1;
                else retval = 1;
            } else if (x1 < x2) retval = -1;
            else retval = 1;
            return retval;
        }
    }

    private boolean isTileVisible(PlotImages.ImageURL serverTile,
                                  ScreenPt viewPortLocation,
                                  int w,
                                  int h) {

        ImageTileData tile= _imageStateMap.get(serverTile);

        int tileX= tile._x;
        int tileY= tile._y;
        int x= viewPortLocation.getIX();
        int y= viewPortLocation.getIY();

        return (x + w > tileX &&
                y + h > tileY &&
                x < tileX  + tile._width &&
                y < tileY + tile._height);
    }


    public static String createImageUrl(WebPlot plot, PlotImages.ImageURL imageURL) {
        Param[] params = new Param[]{
                new Param("file", imageURL.getURL()),
                new Param("state", plot.getPlotState().toString()),
                new Param("type", "tile"),
                new Param("x", imageURL.getXoff() + ""),
                new Param("y", imageURL.getYoff() + ""),
                new Param("width", imageURL.getWidth() + ""),
                new Param("height", imageURL.getHeight() + "")
        };
        return WebUtil.encodeUrl(GWT.getModuleBaseURL() + "sticky/FireFly_ImageDownload", params);
    }

    public void scaleImagesIfMatch(float oldLevel, float newLevel, PlotImages oldImages) {
        if (_serverTiles ==oldImages) {
           scaleImages(oldLevel,newLevel);
        }
    }

    private void scaleImages(float oldLevel, float newLevel) {

        if (_serverTiles!=null) {
            boolean optimizeZoomDown = ((newLevel / _imageZoomLevel) < .5 && _serverTiles.size() > 5);
            if (optimizeZoomDown) {
                _imageWidget.clear();
                _serverTiles= null;
            } else {
                float scale = newLevel / oldLevel;
                Image iw;
                for (PlotImages.ImageURL image : _serverTiles) {

                    ImageTileData tile = _imageStateMap.get(image);
                    tile._x = (int) (tile._x * scale);
                    tile._y = (int) (tile._y * scale);
                    tile._width = (int) (tile._width * scale);
                    tile._height = (int) (tile._height * scale);



                    if (image.isCreated()) {
                        iw = tile.image;
                        iw.setPixelSize(tile._width, tile._height);
                        _imageWidget.setWidgetPosition(iw, tile._x, tile._y);
                    }
                }
            }
        }

        recomputeWidgetSize();
        _scaled= true;
    }

    void recomputeWidgetSize() {
        _imageWidget.setPixelSize(_plot.getScreenWidth()-1, _plot.getScreenHeight()-1);
    }


    private void onFirstLoadComplete() {
        WebPlotGroup plotGroup = _plot.getPlotGroup();
        plotGroup.computeMinMax();
        WebPlotGroup.fireReplotEvent(ReplotDetails.Reason.IMAGE_RELOADED, _plot);
        if (_pv != null && _pv.contains(_plot)) {
            if (_firstLoad) {
                _pv.reconfigure();
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    public void execute() {
                        _pv.smartCenter();
                    }
                });
                _firstLoad = false;
            } else {
                ImageWorkSpacePt pt = _pv.findCurrentCenterPoint();
                _pv.reconfigure();
                if (!AllPlots.getInstance().isWCSMatch())_pv.centerOnPoint(pt);
            }
        }
    }

// =====================================================================
// -------------------- Inner Classes--------------------------------
// =====================================================================


    private class PlotLoadHandler implements LoadHandler {

        private final ImageTileData _tile;

        PlotLoadHandler(ImageTileData widgetData) {
            _tile = widgetData;
        }

        public void onLoad(LoadEvent ev) {
            final Image imw = (Image) ev.getSource();
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                public void execute() {
                    if (_imageWidget.getWidgetIndex(imw) > -1) {
                        imw.setPixelSize(_tile._width, _tile._height);
                        ViewPortPt addPt= _plot.getViewPortCoords(new ScreenPt(_tile._x, _tile._y));
                        _imageWidget.setWidgetPosition(imw, addPt.getIX(), addPt.getIY());
                        if (!_firstReloadComplete) {
                            onFirstLoadComplete();
                            _firstReloadComplete = true;
                        }
                    }
                }
            });
        }
    }


    public static class ImageTileData {
        private Image image;
        private int _x;
        private int _y;
        private int _width;
        private int _height;

        ImageTileData(Image image, int x, int y, int width, int height) {
            this.image = image;
            _x = x;
            _y = y;
            _width = width;
            _height = height;
        }

        public int getX() { return _x; }
        public int getY() { return _y; }
        public int getWidth() { return _width; }
        public int getHeight() { return _height; }
    }


    public class ImageReturn {
        private final List<PlotImages.ImageURL> _serverTiles;
        private final List<ImageTileData> _imageTiles = new ArrayList<ImageTileData>(4);
        private final int _x;
        private final int _y;

        public ImageReturn(ArrayList<PlotImages.ImageURL> serverTiles, int x, int y) {
            _serverTiles = serverTiles;
            for (PlotImages.ImageURL iu : _serverTiles) {
                _imageTiles.add(_imageStateMap.get(iu));
            }
            _x = x;
            _y = y;
        }

        public List<PlotImages.ImageURL> getServerTiles() { return _serverTiles; }
        public List<ImageTileData> getImageTiles() { return _imageTiles; }
        public int getX() { return _x; }
        public int getY() { return _y; }


        public boolean equals(Object other) {
            boolean retval = false;
            if (other == this) {
                retval = true;
            } else if (other != null && other instanceof ImageReturn) {
                ImageReturn ret = (ImageReturn) other;
                if (ComparisonUtil.equals(_serverTiles, _serverTiles) &&
                        _x == ret._x && _y == ret._y) {
                    retval = true;
                }
            }
            return retval;
        }


    }


}

