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
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.RangeValues;

/**
 * @author Trey Roby
 */
public class PlotWidgetColorPrefs {

    private static final String USER_PLOT_PREF= "-userColorPref";
    private final MiniPlotWidget _mpw;
    private String _key= null;
    private ColorPref _pref= null;


    public PlotWidgetColorPrefs(MiniPlotWidget mpw) { _mpw= mpw; }

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



//    public float getZoomLevel() {
//        update();
//        checkAvailable();
//        return _pref._zoomLevel;
//    _zoomLevel}

    public RangeValues getRangeValues(Band band) {
        update();
        checkAvailable();
        RangeValues retval= null;
        switch (band) {
            case RED: retval=     _pref._red;    break;
            case GREEN: retval=   _pref._green;  break;
            case BLUE: retval=    _pref._blue;   break;
            case NO_BAND: retval= _pref._noband; break;
        }
        return retval;
    }


    public int getColorTableId() {
        update();
        checkAvailable();
        return _pref._colorTableID;

    }

    public void saveState() {
        if (_key==null) return;
        Vis.init(new Vis.InitComplete() {
            public void done() {
                WebPlot plot= _mpw.getCurrentPlot();
                if (plot!=null) {
                    PlotState state= plot.getPlotState();
                    if (state.isThreeColor()) {
                        _pref= new ColorPref(state.getColorTableId(),
                                           state.getRangeValues(Band.RED),
                                           state.getRangeValues(Band.GREEN),
                                           state.getRangeValues(Band.BLUE),
                                           null);
                    }
                    else {
                        _pref= new ColorPref(state.getColorTableId(),
                                           state.getRangeValues(Band.NO_BAND));

                    }
                    BrowserCache.put(_key+USER_PLOT_PREF, _pref.toString());
                }
            }
        });
    }

    private void update() {
        if (_key!=null) {
            String prefStr= BrowserCache.get(_key+USER_PLOT_PREF);
            if (prefStr!=null) _pref= ColorPref.parse(prefStr);

        }
    }

    private void checkAvailable() {
        if (_pref==null) {
            throw new IllegalArgumentException("Preferences are not available and could not be revalidated");
        }
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private static class ColorPref {
        private static final int ID_IDX     = 0;
        private static final int RED_IDX    = 1;
        private static final int GREEN_IDX  = 2;
        private static final int BLUE_IDX   = 3;
        private static final int NO_BAND_IDX= 4;
        private int  _colorTableID;
//        private float _zoomLevel;
        private RangeValues _red;
        private RangeValues _blue;
        private RangeValues _green;
        private RangeValues _noband;

        public ColorPref(int colorTableID,
                         RangeValues noband) {
            this(colorTableID,null,null,null,noband);
        }

        public ColorPref(int colorTableID,
                         RangeValues red,
                         RangeValues blue,
                         RangeValues green,
                         RangeValues noband) {
            _colorTableID= colorTableID;
//            _zoomLevel= zoomLevel;
            _red= red;
            _green= green;
            _blue= blue;
            _noband= noband;
        }

        public String toString() {

            String rStr= _red==null ?    "" : _red.serialize();
            String gStr= _green==null ?  "" : _green.serialize();
            String bStr= _blue==null ?   "" : _blue.serialize();
            String nStr= _noband==null ? "" : _noband.serialize();

            return _colorTableID +";"+ rStr +";"+ gStr +";"+ bStr +";"+ nStr;
        }

        public static ColorPref parse(String inStr) {
            if (StringUtils.isEmpty(inStr)) return null;
            String s[]= inStr.split(";", 5);
            if (s.length!=5) return null;
            ColorPref retval;
            try {
                RangeValues red   = null;
                RangeValues blue  = null;
                RangeValues green = null;
                RangeValues noband= null;
                int colorTableID= Integer.parseInt(s[0]);
//                float zoomLevel= Float.parseFloat(s[1]);
                if (s[RED_IDX].length()>0)     red   = RangeValues.parse(s[RED_IDX]);
                if (s[GREEN_IDX].length()>0)   green = RangeValues.parse(s[GREEN_IDX]);
                if (s[BLUE_IDX].length()>0)    blue  = RangeValues.parse(s[BLUE_IDX]);
                if (s[NO_BAND_IDX].length()>0) noband= RangeValues.parse(s[NO_BAND_IDX]);

                retval= new ColorPref(colorTableID,red,green,blue,noband);
            } catch (NumberFormatException e) {
                retval= null;
            }
            return retval;
        }

    }


}

