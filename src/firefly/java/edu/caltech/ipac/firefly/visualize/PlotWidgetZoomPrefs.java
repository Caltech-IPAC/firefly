/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 9/23/11
 * Time: 1:45 PM
 */


import edu.caltech.ipac.firefly.util.BrowserCache;
import edu.caltech.ipac.util.ComparisonUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class PlotWidgetZoomPrefs {

    private static final String USER_ZOOM_PREF = "-userZoomPref";
    private static final Map<String,String> _tmpCache= new HashMap<String, String>(40);
    private final MiniPlotWidget _mpw;
    private String _key= null;
    private ZoomPref _pref= null;
    private boolean _permCache;


    public PlotWidgetZoomPrefs(MiniPlotWidget mpw, boolean perm) {
        _mpw= mpw;
        _permCache= perm;
    }

    public void setKey(String key) {
        if (!ComparisonUtil.equals(key,_key)) {
            _key= key;
            _pref= null;
        }
    }

    public String getKey() { return _key; }

    public boolean isPrefsAvailable() {
        update();
        return _pref!=null;
    }



    public float getZoomLevel() {
        update();
        checkAvailable();
        return _pref._zoomLevel;
    }


    public void saveState() {
        if (_key==null) return;
        Vis.init(new Vis.InitComplete() {
            public void done() {
                WebPlot plot= _mpw.getCurrentPlot();
                if (plot!=null) {
                    PlotState state= plot.getPlotState();
                    _pref= new ZoomPref(state.getZoomLevel());
                    put(_key+ USER_ZOOM_PREF, _pref.toString());
                }
            }
        });
    }

    private void update() {
        if (_pref==null && _key!=null) {
            String prefStr= get(_key+ USER_ZOOM_PREF);
            if (prefStr!=null) {
                _pref= ZoomPref.parse(prefStr);
            }
        }
    }

    private void checkAvailable() {
        if (_pref==null) {
            throw new IllegalArgumentException("Preferences are not available and could not be revalidated");
        }
    }

    private void put(String key, String value) {
        if (_permCache) BrowserCache.put(key,value);
        else            _tmpCache.put(key,value);
    }

    private String get(String key) {
        if (_permCache) return BrowserCache.get(key);
        else            return _tmpCache.get(key);
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private static class ZoomPref {
        private float _zoomLevel;


        public ZoomPref(float zoomLevel) {
            _zoomLevel= zoomLevel;
        }

        public String toString() {
            return _zoomLevel+"";
        }

        public static ZoomPref parse(String inStr) {
            float zoomLevel= 1.0F;
            try {
                zoomLevel= Float.parseFloat(inStr);
            } catch (NumberFormatException e) {
                 // do nothing
            }
            return new ZoomPref(zoomLevel);
        }

    }


}

