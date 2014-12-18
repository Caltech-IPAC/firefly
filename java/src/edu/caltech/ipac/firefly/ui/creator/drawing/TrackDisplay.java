package edu.caltech.ipac.firefly.ui.creator.drawing;
/**
 * User: roby
 * Date: 3/23/12
 * Time: 1:33 PM
 */


import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.MovingTargetContext;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.draw.AutoColor;
import edu.caltech.ipac.firefly.visualize.draw.DrawConnector;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.firefly.visualize.draw.LineDrawConnector;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author Trey Roby
*/
class TrackDisplay extends ProviderDataConnection {

    private DataSet dataset;
    private final DrawSymbol symbol;
    private List<DrawObj> defaultList = new ArrayList<DrawObj>(1);
    private final List<String> keyList;
    private final DrawConnector _drawConnector = new LineDrawConnector();
    private final WebEventManager _evManager = new WebEventManager();
    private final boolean connectTrack;
    private final boolean supportsHighlight;
    private final boolean buildDataOnce;
    private       boolean firstTime;
    private final int     decimationFactor;
    private final String matchColor;
    private final DrawSymbol matchSymbol;
    private final Map<WebPlot, List<DrawObj>> dataMap= new HashMap<WebPlot, List<DrawObj>>(40);
    private final boolean markPlotWithMovingTarget;

    TrackDisplay(DatasetDrawingLayerProvider provider,
                 DataSet dataset,
                 String title,
                 DrawSymbol symbol,
                 String color,
                 String matchColor,
                 List<String> keyList,
                 boolean connectTrack,
                 boolean supportsHighlight,
                 int decimationFactor,
                 boolean markPlotWithMovingTarget) {
        super(provider,title, null, color == null ? AutoColor.PT_1 : color);
        this.dataset = dataset;
        this.symbol = symbol;
        this.keyList = keyList;
        this.matchColor= matchColor;
        this.connectTrack= connectTrack;
        this.supportsHighlight = supportsHighlight;
        this.decimationFactor= decimationFactor;
        this.markPlotWithMovingTarget= markPlotWithMovingTarget;
        buildDataOnce= (keyList==null);
        matchSymbol= buildDataOnce ? null : DrawSymbol.SQUARE;
        updateData(dataset);
    }


    @Override
    public void updateData(DataSet dataset) {
        this.dataset = dataset;
        firstTime= true;
        dataMap.clear();
    }

    @Override
    public List<DrawObj> getData(boolean rebuild, WebPlot plot) {

        if (dataset==null) return null;
        if (plot==null && defaultList!=null && defaultList.size()>0) return defaultList;

        if (buildDataOnce && !firstTime) return defaultList;

        if (dataMap.containsKey(plot)) {
            return dataMap.get(plot);
        }

        firstTime= false;

        List<DrawObj> list = new ArrayList<DrawObj>(100);

        try {
            TableMeta.LonLatColumns llc = dataset.getMeta().getCenterCoordColumns();
            if (llc != null) {
                int raIdx = dataset.getModel().getColumnIndex(llc.getLonCol());
                int decIdx = dataset.getModel().getColumnIndex(llc.getLatCol());
                for (Object o : dataset.getModel().getRows()) {
                    try {
                        TableData.Row<String> row = (TableData.Row) o;
                        PointDataObj obj= getObj(plot, row, raIdx, decIdx, llc.getCoordinateSys());
                        list.add(obj);
                    } catch (NumberFormatException e) {
                        // ignore and more on
                    }
                }
            }

        } catch (IllegalArgumentException e) {

        }

        if (plot!=null) {
            dataMap.put(plot,list);
            purgeOld();
        }
        defaultList= list;

        return list;
    }

    private PointDataObj getObj(WebPlot plot, TableData.Row<String> row, int raIdx, int decIdx, CoordinateSys csys) {
        String raStr = row.getValue(raIdx);
        String decStr = row.getValue(decIdx);
        double ra = Double.parseDouble(raStr);
        double dec = Double.parseDouble(decStr);
        WorldPt wp = new WorldPt(ra, dec, csys);
        PointDataObj obj;
        if (isCurrent(plot, row)) {
            obj = new PointDataObj(wp, matchSymbol);
            if (matchColor!=null) obj.setColor(matchColor);
            if (markPlotWithMovingTarget) {
                MovingTargetContext mtcNew= new MovingTargetContext(wp,null);
                plot.setAttribute(WebPlot.MOVING_TARGET_CTX_ATTR, mtcNew);
            }
        } else {
            obj = new PointDataObj(wp, symbol);
        }
        return obj;
    }


    public List<DrawObj> getHighlightData(WebPlot p) {
        PointDataObj retval= null;
        TableMeta.LonLatColumns llc = dataset.getMeta().getCenterCoordColumns();
        if (llc != null) {
            int raIdx = dataset.getModel().getColumnIndex(llc.getLonCol());
            int decIdx = dataset.getModel().getColumnIndex(llc.getLatCol());
            int rowIdx= dataset.getHighlighted();

            TableData.Row<String> row= rowIdx>-1 ? dataset.getModel().getRow(rowIdx): null;
            try {
                retval= getObj(p, row, raIdx, decIdx, llc.getCoordinateSys());
                retval.setHighlighted(true);
            } catch (NumberFormatException e) {
                // ignore and more on
            }
        }
        return (retval!=null) ? Arrays.asList((DrawObj)retval) : null;
    }



    private void purgeOld() {
        if (dataMap.size()>40) {
            List<WebPlot> removeList= new ArrayList<WebPlot> (dataMap.size()/2);
            for(WebPlot p : dataMap.keySet()) {
                if (!p.isAlive())  removeList.add(p);
            }
            for(WebPlot p : removeList) dataMap.remove(p);
        }
    }

    private List<DrawObj> makeUnique(List<DrawObj> inList, boolean usingMatchPoint) {
        ArrayList<DrawObj> retList= new ArrayList<DrawObj>(inList.size());
        PointDataObj testObj;

        if (usingMatchPoint && matchSymbol!= null) { // put match point in first
            for(DrawObj obj : inList) {
                testObj= (PointDataObj)obj;
                if (testObj.getSymbol()==matchSymbol && !containsPt(retList,(WorldPt)testObj.getPos())) {
                    retList.add(obj);
                }
            }
        }

        for(DrawObj obj : inList) { // put in others
            testObj= (PointDataObj)obj;
            if (testObj.getSymbol()!=matchSymbol && !containsPt(retList,(WorldPt)testObj.getPos())) {
                retList.add(obj);
            }

        }
        return retList;

    }

    private boolean containsPt(List<DrawObj> inList, WorldPt pt) {
        boolean  retval= false;
        PointDataObj testObj;
        WorldPt testPt;
        for(DrawObj obj : inList) {
            testObj= (PointDataObj)obj;
            testPt= (WorldPt)testObj.getPos();
            if (testPt.equals(pt)) {
                retval= true;
                break;
            }
        }
        return retval;
    }

    private boolean isCurrent(WebPlot plot, TableData.Row row) {
        boolean retval = false;
        if (plot != null && plot.containsAttributeKey(WebPlot.UNIQUE_KEY)) {
            String key = (String) plot.getAttribute(WebPlot.UNIQUE_KEY);
            retval = key.equals(makeKey(row));
        }
        return retval;
    }

    private String makeKey(TableData.Row row) {
        StringBuilder sb = new StringBuilder(10 * keyList.size());
        for (String c : keyList) {
            sb.append(row.getValue(c));
        }
        return sb.toString();
    }



    @Override
    public DrawConnector getDrawConnector() {
        return connectTrack ? _drawConnector : null;
    }

    @Override
    public boolean getSupportsMouse() {
        return true;
    }

    @Override
    public boolean getHasPerPlotData() { return true; }

    @Override
    public WebEventManager getEventManager() {
        return _evManager;
    }


    @Override
    public void setHighlightedIdx(int idx) {
        dataset.highlight(idx);
        _evManager.fireEvent(new WebEvent<Integer>(this, TablePanel.ON_ROWHIGHLIGHT_CHANGE, idx));
    }

    @Override
    public boolean getSupportsHighlight() { return supportsHighlight; }
}

