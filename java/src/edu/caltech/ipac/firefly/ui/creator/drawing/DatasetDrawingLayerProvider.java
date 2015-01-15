/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator.drawing;
/*
 * User: roby
 * Date: 2/21/12
 * Time: 10:13 AM
 */


import com.google.gwt.core.client.Scheduler;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.eventworker.AbstractDatasetQueryWorker;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.draw.DataConnection;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class DatasetDrawingLayerProvider extends AbstractDatasetQueryWorker<DataConnection> implements DrawingLayerProvider {

    public enum Type {Point, Track, MatchedPoint, Box}

    private DrawSymbol _symbol = DrawSymbol.X;
    private String _color = null;
    private String _matchColor = null;
    private boolean _selection = true;
    private Type _type = Type.Point;
    private List<String> _keyList = null;
    private int decimationFactor = 1;
    private ProviderDataConnection _dc;
    private String _lastActiveTableName = null;
    private String _prefKey = null;
    private TableMeta.LonLatColumns[] _fallbackCornerCols = null;
    private TableMeta.LonLatColumns _fallbackCenterCol= null;


    public void setSymbol(DrawSymbol symbol) {
        _symbol = symbol;
    }

    public void setColor(String color) {
        _color = color;
    }

    public void setMatchColor(String color) {
        _matchColor = color;
    }

    public void setDrawingType(Type type) {
        _type = type;
    }

    public void setUniqueKeyColumns(List<String> keyList) {
        _keyList = keyList;
    }

    public void setSelection(boolean s) {
        _selection = s;
    }

    public void setDecimationFactor(int f) {
        decimationFactor = f;
    }


    public void setEnablingPreferenceKey(String pref) {
        _prefKey = pref;
    }

    public String getEnablingPreferenceKey() {
        return _prefKey;
    }


    public void enableDefaultColumns() {
        initFallbackCol("ra", "dec",
                        "ra1","dec1",
                        "ra2","dec2",
                        "ra3","dec3",
                        "ra4","dec4");

    }

    public void initFallbackCol(String cRA,
                                String cDec) {
        initFallbackCol(cRA,cDec,null,null,null,null,null,null,null,null);
    }


    public void initFallbackCol(String cRA,
                                String cDec,
                                String ra1,
                                String dec1,
                                String ra2,
                                String dec2,
                                String ra3,
                                String dec3,
                                String ra4,
                                String dec4 ) {
        if ( ra1!=null && dec1!=null && ra2!=null && dec2!=null &&
                ra3!=null && dec3!=null && ra4!=null && dec4!=null ) {
            _fallbackCornerCols = new TableMeta.LonLatColumns[] {
                    new TableMeta.LonLatColumns(ra1, dec1, CoordinateSys.EQ_J2000),
                    new TableMeta.LonLatColumns(ra2, dec2, CoordinateSys.EQ_J2000),
                    new TableMeta.LonLatColumns(ra3, dec3, CoordinateSys.EQ_J2000),
                    new TableMeta.LonLatColumns(ra4, dec4, CoordinateSys.EQ_J2000)
            };
        }

        if ( cRA!=null && cDec!=null) {
            _fallbackCenterCol= new TableMeta.LonLatColumns(cRA, cDec, CoordinateSys.EQ_J2000);
        }
    }



    private void updateVisibility(EventHub hub, boolean forceAdd) {
        if (hub.getActiveTable() == null) {
            return;
        }
        String activeTableName = hub.getActiveTable().getName();

        if (ComparisonUtil.equals(activeTableName, "GatorQuery") || 
                ComparisonUtil.equals(activeTableName, CommonParams.USER_CATALOG_FROM_FILE)) {
            return;
        }


        List<String> qSources = getQuerySources();
        if (forceAdd) _lastActiveTableName = null;

        if (activeTableName != null && !activeTableName.equals(_lastActiveTableName)) {
            if (qSources.contains(_lastActiveTableName) && !qSources.contains(activeTableName)) { // remove last active, if necessary
                hub.getEventManager().fireEvent(new WebEvent<DataConnection>(
                        this, EventHub.DRAWING_LAYER_REMOVE, _dc));
            } else if (qSources.contains(activeTableName) && !qSources.contains(_lastActiveTableName)) { // add current, if necessary
                hub.getEventManager().fireEvent(new WebEvent<DataConnection>(
                        this, EventHub.DRAWING_LAYER_ADD, _dc));
            }

        }
        _lastActiveTableName = activeTableName;

    }

    public void activate(final Object source, final Map<String, String> params) {

        if (getDelayTime() > 0) {
            Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
                public boolean execute() {
                    doActivate(source, params);
                    return false;
                }
            }, getDelayTime());
        } else {
            doActivate(source, params);
        }
    }

    private void doActivate(Object source, Map<String, String> params) {
        callServerWithParams(params);
        setLastEvent(new WebEvent<TableServerRequest>(this, Name.BYPASS_EVENT, getLastRequest()));
    }

    public void bind(final EventHub hub) {
        super.bind(hub);
        _dc = getDataConnection(null);
        hub.getEventManager().fireEvent(new WebEvent<DataConnection>(
                this, EventHub.DRAWING_LAYER_INIT, getDataConnection(null)));


        hub.getEventManager().addListener(EventHub.ON_TAB_SELECTED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                updateVisibility(hub, false);
            }
        });

        hub.getEventManager().addListener(TablePanel.ON_VIEW_CHANGE, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                updateVisibility(hub, true);
            }
        });

//        hub.getEventManager().addListener(EventHub.ON_ROWHIGHLIGHT_CHANGE, new WebEventListener() {
//            public void eventNotify(WebEvent ev) { updateVisibility(hub);  }
//        });

    }

    @Override
    public DataConnection convertResult(DataSet dataset) {
        if (_dc == null) _dc = getDataConnection(null);
        _dc.updateData(dataset);
        return _dc;
    }

    private ProviderDataConnection getDataConnection(DataSet dataset) {
        if (_type == Type.Point || _type==Type.Box) {
            CoverageLayer d= new CoverageLayer(this, dataset, getDesc(), _symbol, _color, _type==Type.Box);
            d.setFallbackCenterCols(_fallbackCenterCol);
            d.setFallbackCornerCols(_fallbackCornerCols);
            return d;
        } else if (_type == Type.Track) {
            return new TrackDisplay(this, dataset, getDesc(), _symbol, _color, _matchColor, _keyList,
                                    true, _selection, decimationFactor, true);
        } else if (_type == Type.MatchedPoint) {
            return new TrackDisplay(this, dataset, getDesc(), _symbol, _color, _matchColor, _keyList,
                                    false, _selection, decimationFactor, true);
        } else {
            WebAssert.argTst(false, "I don't know type: " + _type);
        }
        return null;
    }


}

