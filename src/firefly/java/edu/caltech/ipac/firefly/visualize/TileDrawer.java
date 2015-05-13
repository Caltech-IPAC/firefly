/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
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
    private static final long MAX_VALID_TIME = 1000 * 60 * 3; // three minutes

    private static final String TRANSPARENT = GWT.getModuleBaseURL()+"images/transparent-20x20.gif";
    private static final VisIconCreator _ic = VisIconCreator.Creator.getInstance();
    private static final String BACKGROUND_STYLE = "url(" + _ic.getImageWorkingBackground().getURL() + ") top left repeat";
    private AbsolutePanel _imageWidget;
    private boolean _allTilesCreated = false;
    private Map<PlotImages.ImageURL, ImageWidgetData> _imageStateMap = null;
    private Map<PlotImages.ImageURL, ImageWidgetData> _oldStateMap = null;
    private final WebPlot _plot;
    private boolean _firstReloadComplete = true;
    private boolean _firstLoad = true;
    private PlotImages _images;
    private float _imageZoomLevel = 1;
    private boolean _scaled= false;
    private final List<HandlerRegistration> _hregList = new ArrayList<HandlerRegistration>(80);
    private static final FourTileSort _ftSort = new FourTileSort();
    private List<PlotImages.ImageURL> _panelList = new ArrayList<PlotImages.ImageURL>(8);

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

    public TileDrawer(WebPlot plot, PlotImages images) {
        _plot = plot;
        _images = images;
    }


    AbsolutePanel getWidget() {
        if (_imageWidget == null) {
            _imageWidget = new AbsolutePanel();
            GwtUtil.setStyle(_imageWidget, "background", BACKGROUND_STYLE);
//            DOM.setStyleAttribute(_imageWidget.getElement(), "background","#e5e3df");
            DOM.setStyleAttribute(_imageWidget.getElement(), "background", "black");
            refreshWidget(_images, false);
        }
        return _imageWidget;
    }

    public PlotImages getImages() {
        return _images;
    }

    private void clearOldImages() {
        if (_oldStateMap != null) {
            Timer t= new Timer() {
                @Override
                public void run() {
                    if (_oldStateMap != null) {
                        for (ImageWidgetData imw : _oldStateMap.values()) {
                            _imageWidget.remove(imw.getImage());
                        }
                        _oldStateMap.clear();
                        _oldStateMap = null;
                    }
                }
            };
            t.schedule(4000);
        }

    }

    void refreshWidget() {
        if (_scaled) return;
        refreshWidget(_images,false);
    }

    void refreshWidget(PlotImages images, boolean overlay) {
        if (_imageWidget==null) return;
        _scaled= false;
        _images = images;
        _imageZoomLevel = _plot.getZoomFact();
        _allTilesCreated = false;
        _plot.getPlotGroup().computeMinMax();

        Image imw;

        for (HandlerRegistration r : _hregList) r.removeHandler();
        _hregList.clear();
//        clearOldImages();

        if (overlay) {
            _oldStateMap = _imageStateMap;
            _firstReloadComplete = true;
        } else {
            _firstReloadComplete = false;
            _imageWidget.clear();
        }
        _imageStateMap = new HashMap<PlotImages.ImageURL, ImageWidgetData>();


//        _lastValidation= System.currentTimeMillis();


        for (PlotImages.ImageURL image : _images) {
            imw = new Image();
            GwtUtil.setStyle(imw, "background", BACKGROUND_STYLE);

            ImageWidgetData widgetData = new ImageWidgetData(imw,
                                                             image.getXoff(),
                                                             image.getYoff(),
                                                             image.getWidth(),
                                                             image.getHeight());
            _imageWidget.add(imw, widgetData._x, widgetData._y);
            imw.setPixelSize(widgetData._width, widgetData._height);
            // ---- experimental
            float zfact = _plot.getZoomFact();
            int offX = (int) (_plot.getOffsetX() * zfact);
            int offY = (int) (_plot.getOffsetY() * zfact);
            _imageWidget.setWidgetPosition(imw, widgetData._x + offX, widgetData._y + offY);
            // ---- experimental
            _hregList.add(imw.addLoadHandler(new PlotLoadHandler(widgetData)));
//            _hregList.add(imw.addErrorHandler(_loadError));
            _imageStateMap.put(image, widgetData);
            if (image.isCreated()) {
                getTileImage(image);
            } else if (_images.size()>1) {
                imw.setUrl(TRANSPARENT);
            }
        }
        recomputeWidgetSize();
        deferredDrawTiles();
    }


    private void deferredDrawTiles() {
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                if (_plot.isAlive()) {
                    WebPlotView pv = _plot.getPlotView();
                    drawTilesForArea(pv.getScrollX() - 5, pv.getScrollY() - 5, pv.getOffsetWidth() + 5, pv.getOffsetHeight() + 5);
                }
            }
        });

    }


    public void drawTilesForArea(int x, int y, int width, int height) {
        if (!_allTilesCreated && _plot.isAlive()) {
            List<PlotImages.ImageURL> iList = new ArrayList<PlotImages.ImageURL>(8);
            boolean allCreated = true;

            ScreenPt wcsMargin= _plot.getPlotView().getWcsMargins();
            int mx= wcsMargin.getIX();
            int my= wcsMargin.getIY();
            x-=mx;
            y-=my;

            for (PlotImages.ImageURL image : _images) {
                if (!image.isCreated()) {
                    allCreated = false;
                    if (isTileVisible(image, x, y, width, height)) {
                        image.setCreated(true);
                        getTileImage(image);
                    } else {
                        image.setCreated(false);
                        ImageWidgetData widgetData = _imageStateMap.get(image);
                        widgetData.getImage().setUrl(TRANSPARENT);
                    }
                }
            }
            if (iList.size() > 0) {
                _panelList.addAll(iList);
                retrieveValidatedTiles();
            }
            _allTilesCreated = allCreated;
            if (_allTilesCreated) {
                clearOldImages();
            }
//            clearOldImages();
        }
    }


    public void retrieveValidatedTiles() {
//        _lastValidation= System.currentTimeMillis();
        List<PlotImages.ImageURL> iList = new ArrayList<PlotImages.ImageURL>(_panelList);
        _panelList.clear();
        for (PlotImages.ImageURL image : iList) {
            getTileImage(image);
        }
    }



    public void getTileImage(PlotImages.ImageURL image) {
        ImageWidgetData widgetData = _imageStateMap.get(image);
        if (widgetData != null) {
            String url = createImageUrl(_plot, image);
            widgetData.getImage().setUrl(url);
        }
    }

    public ImageReturn getImagesAt(ScreenPt spt, int size) {

        ImageReturn retval = null;
        ArrayList<PlotImages.ImageURL> tiles = new ArrayList<PlotImages.ImageURL>(4);

        for (PlotImages.ImageURL image : _images) {
            if (isTileVisible(image, spt.getIX(), spt.getIY(), size, size)) {
                tiles.add(image);
            }
        }
        if (tiles.size() > 0) {
            Collections.sort(tiles, _ftSort);
            ImageWidgetData data = _imageStateMap.get(tiles.get(0));

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

    private boolean isTileVisible(PlotImages.ImageURL image,
                                  int x,
                                  int y,
                                  int w,
                                  int h) {

        ImageWidgetData widgetData = _imageStateMap.get(image);
        int tileX = widgetData._x;
        int tileY = widgetData._y;
        float zfact = _plot.getZoomFact();
        float offX = _plot.getOffsetX() * zfact;
        float offY = _plot.getOffsetY() * zfact;
        return (x + w > tileX + offX &&
                y + h > tileY + offY &&
                x < tileX + offX + widgetData._width &&
                y < tileY + offY + widgetData._height);
    }


    public static String createImageUrl(WebPlot plot, PlotImages.ImageURL imageURL) {
        Param[] params = new Param[]{
                new Param("file", imageURL.getURL()),
//                new Param("ctx", plot.getPlotState().getContextString()),
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
        if (_images==oldImages) {
           scaleImages(oldLevel,newLevel);
        }
    }

    public void scaleImages(float oldLevel, float newLevel) {

        Image iw;
//        clearOldImages();
        boolean optimizeZoomDown = ((newLevel / _imageZoomLevel) < .5 && _images.size() > 5);

        if (optimizeZoomDown) {
            _imageWidget.clear();
        } else {
            float scale = newLevel / oldLevel;
            for (PlotImages.ImageURL image : _images) {

                ImageWidgetData wd = _imageStateMap.get(image);
                wd._x = (int) (wd._x * scale);
                wd._y = (int) (wd._y * scale);
                wd._width = (int) (wd._width * scale);
                wd._height = (int) (wd._height * scale);

                int offX = (int) (_plot.getOffsetX() * newLevel);
                int offY = (int) (_plot.getOffsetY() * newLevel);


                if (image.isCreated()) {
                    iw = wd.getImage();
                    iw.setPixelSize(wd._width, wd._height);
                    _imageWidget.setWidgetPosition(iw, wd._x + offX, wd._y + offY);
                }
            }
        }

        recomputeWidgetSize();
        _scaled= true;
    }

    void recomputeWidgetSize() {
        _imageWidget.setPixelSize(_plot.getScreenWidth(), _plot.getScreenHeight());
    }


    private void onFirstLoadComplete() {
        WebPlotGroup plotGroup = _plot.getPlotGroup();
        plotGroup.computeMinMax();
        WebPlotGroup.fireReplotEvent(ReplotDetails.Reason.IMAGE_RELOADED, _plot);
        final WebPlotView pv = plotGroup.getPlotView();
        if (pv != null) {
            if (_firstLoad) {
                pv.reconfigure();
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                        pv.smartCenter();
                    }
                });
                _firstLoad = false;
            } else {
                ImageWorkSpacePt pt = pv.findCurrentCenterPoint();
                pv.reconfigure();
                if (!AllPlots.getInstance().isWCSMatch())pv.centerOnPoint(pt);
            }
        }
    }

// =====================================================================
// -------------------- Inner Classes--------------------------------
// =====================================================================


    private class PlotLoadHandler implements LoadHandler {

        private final ImageWidgetData _widgetData;

        PlotLoadHandler(ImageWidgetData widgetData) {
            _widgetData = widgetData;
        }

        public void onLoad(LoadEvent ev) {
            final Image imw = (Image) ev.getSource();
            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    if (_imageWidget.getWidgetIndex(imw) > -1) {
                        imw.setPixelSize(_widgetData._width, _widgetData._height);

                        float zfact = _plot.getZoomFact();
                        int offX = (int) (_plot.getOffsetX() * zfact);
                        int offY = (int) (_plot.getOffsetY() * zfact);

                        _imageWidget.setWidgetPosition(imw, _widgetData._x + offX, _widgetData._y + offY);
                        if (!_firstReloadComplete) {
                            onFirstLoadComplete();
                            _firstReloadComplete = true;
                        }
                        clearOldImages();
                    }
                }
            });
        }
    }


    public static class ImageWidgetData {
        private Image _image;
        private int _x;
        private int _y;
        private int _width;
        private int _height;

        ImageWidgetData(Image image, int x, int y, int width, int height) {
            _image = image;
            _x = x;
            _y = y;
            _width = width;
            _height = height;
        }

        Image getImage() { return _image; }
        public int getX() { return _x; }
        public int getY() { return _y; }
        public int getWidth() { return _width; }
        public int getHeight() { return _height; }
    }


    public class ImageReturn {
        private final List<PlotImages.ImageURL> _serverTiles;
        private final List<ImageWidgetData> _imageTiles = new ArrayList<ImageWidgetData>(4);
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
        public List<ImageWidgetData> getImageTiles() { return _imageTiles; }
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

