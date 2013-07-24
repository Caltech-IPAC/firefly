package edu.caltech.ipac.firefly.ui.creator.drawing;
/**
 * User: roby
 * Date: 3/23/12
 * Time: 1:26 PM
 */


import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.firefly.data.form.PositionFieldDef;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.eventworker.ActiveTargetCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseEventWorker;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.visualize.ActiveTarget;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.conv.CoordUtil;
import edu.caltech.ipac.firefly.visualize.draw.AutoColor;
import edu.caltech.ipac.firefly.visualize.draw.DataConnection;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
import edu.caltech.ipac.firefly.visualize.draw.SimpleDataConnection;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author Trey Roby
*/
public class ActiveTargetLayer extends BaseEventWorker<DataConnection> implements DrawingLayerProvider {

    private final ActiveTargetCreator.TargetType _type;
    private List<String> _raCol= new ArrayList<String>(2);
    private List<String> _decCol= new ArrayList<String>(2);
    private String _prefKey= null; // future use
    private ActiveTargetCreator.InputFormat _inFormat= ActiveTargetCreator.InputFormat.DECIMAL;

    public ActiveTargetLayer() {
        this(ActiveTargetCreator.TargetType.QueryCenter);
    }

    public ActiveTargetLayer(List<String> raCol, List<String> decCol, ActiveTargetCreator.TargetType type) {
        super(CommonParams.ACTIVE_TARGET);
        if (type== ActiveTargetCreator.TargetType.PlotFixedTarget) {
            setupPlotSpecific();
        }
        else {
            setEventsByName(Arrays.asList(TablePreviewEventHub.ON_TABLE_SHOW, TablePreviewEventHub.ON_ROWHIGHLIGHT_CHANGE));
        }
        _type= type;
        _raCol = raCol;
        _decCol = decCol;
    }
    public ActiveTargetLayer(String raCol, String decCol, ActiveTargetCreator.TargetType type) {
        this(Arrays.asList(raCol), Arrays.asList(decCol), type);
    }

    public ActiveTargetLayer(ActiveTargetCreator.TargetType type) {
        super(CommonParams.ACTIVE_TARGET);
        _type= type;
        _raCol = null;
        _decCol = null;
        if (type== ActiveTargetCreator.TargetType.PlotFixedTarget) {
            setupPlotSpecific();
        }
        else {
            setEventsByName(Arrays.asList(TablePreviewEventHub.ON_TABLE_SHOW, TablePreviewEventHub.ON_ROWHIGHLIGHT_CHANGE));
        }
    }


    private void setupPlotSpecific() {
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                Vis.init(new Vis.InitComplete() {
                    public void done() {
                        handleResults(new PerPlotTargetDisplay());
                    }
                });
            }
        });
    }





    private WorldPt findWorldPt(TableData.Row hrow) {
        WorldPt wp= null;
        String raCol;
        String decCol;
        if (hrow != null && _raCol.size()>0 && _decCol.size()>0)  {
            TableData.Row<String> row = hrow;
            for(int i=0; (i<_raCol.size() && wp==null); i++) {
                if (i<_decCol.size()) {
                    raCol= _raCol.get(i);
                    decCol= _decCol.get(i);
                    if (StringUtils.isEmpty(raCol) && StringUtils.isEmpty(decCol) ) {
                        wp= null;
                    }
                    else {
                        String raS = row.getValue(raCol);
                        String decS = row.getValue(decCol);
                        if (!StringUtils.isAnyEmpty(raS,decS)) {
                            if (_inFormat== ActiveTargetCreator.InputFormat.DECIMAL) {
                                wp= makeWorldPtFromDecimal(raS,decS);
                            }
                            else if (_inFormat== ActiveTargetCreator.InputFormat.HMS) {
                                wp= makeWorldPtFromHMS(raS,decS);
                            }
                            else {
                                wp= makeWorldPtFromDecimal(raS,decS);
                                if (wp==null) wp= makeWorldPtFromHMS(raS,decS);
                            }
                        }
                    }
                }
            }
        }
        return wp;
    }



    private WorldPt makeWorldPtFromDecimal(String raStr, String decStr) {
        WorldPt wp= null;
        try {
            double ra = Double.parseDouble(raStr);
            double dec = Double.parseDouble(decStr);
            if (!Double.isNaN(ra) && !Double.isNaN(dec)) {
                wp= new WorldPt(ra,dec, CoordinateSys.EQ_J2000);
            }
        } catch (NumberFormatException e) {
            wp= null;
        }
        return wp;
    }

    private WorldPt makeWorldPtFromHMS(String raStr, String decStr) {
        WorldPt wp;
        try {
            double ra= CoordUtil.convertStringToLon(raStr, true);
            double dec= CoordUtil.convertStringToLat(decStr, true);
            wp= new WorldPt(ra,dec, CoordinateSys.EQ_J2000);
        } catch (CoordException e) {
            wp= null;
        }
        return wp;
    }


    public void setEnablingPreferenceKey(String pref) { _prefKey= pref; }

    public String getEnablingPreferenceKey() { return _prefKey; }

    public void setInputFormat(ActiveTargetCreator.InputFormat f) {_inFormat= f;}
    public ActiveTargetCreator.InputFormat getInputFormat() {return _inFormat;}


    public void handleEvent(WebEvent ev) {
        if (!(ev.getSource() instanceof TablePanel)) return;
        TablePanel table = (TablePanel) ev.getSource();
        List<String> qSources= getQuerySources();
        if (qSources==null || (!qSources.contains(table.getName()) && !qSources.contains(CommonParams.ALL))) {
            return;
        }
        Map<String,String> params= new HashMap<String,String>(3);

        if (_type== ActiveTargetCreator.TargetType.TableRow){
            TableData.Row<String> hrows = table.getTable().getHighlightedRow();
            WorldPt wp= findWorldPt(hrows);
            if (wp!=null) {
                params.put(CommonParams.WORLD_PT, wp.toString());
            }
        }


        activate(table, params);
    }


    public void activate(Object source, Map<String, String> params) {
        if (_type== ActiveTargetCreator.TargetType.QueryCenter) {
            ActiveTarget at= ActiveTarget.getInstance();
            if (at!=null) {
                ActiveTarget.PosEntry entry= at.getActive();
                showResults(entry.getName(), entry.getPt());
            }
        }
        else if (_type== ActiveTargetCreator.TargetType.TableRow){
            String wpStr= params.get(CommonParams.WORLD_PT);
            WorldPt wp= WorldPt.parse(wpStr);
            if (wp!=null) showResults(null,wp);
        }
        else if (_type== ActiveTargetCreator.TargetType.TableRowByPlot){
            String keys= getParam(CommonParams.UNIQUE_KEY_COLUMNS);
            if (keys!=null && source instanceof TablePanel) {
                List<String> keyList= StringUtils.asList(keys, ",");
                showDynamicResults((TablePanel)source,keyList.toArray(new String[keyList.size()]));
            }
        }
//        else if (_type== ActiveTargetCreator.TargetType.PlotFixedTarget){
//            Vis.init(new Vis.InitComplete() {
//                public void done() {
//                    handleResults(new PerPlotTargetDisplay());
//                }
//            });
//        }
        else {
            WebAssert.fail("don't know this type: " + _type);
        }
    }

    private void showResults(final String name, final WorldPt wp) {
        Vis.init(new Vis.InitComplete() {
            public void done() {
                handleResults(new ActiveTargetDisplay(name, wp));
            }
        });

    }

    private void showDynamicResults(final TablePanel table, final String keyColumns[]) {
        Vis.init(new Vis.InitComplete() {
            public void done() {
                handleResults(new DynamicActiveTargetDisplay(table,keyColumns));
            }
        });

    }

    private static class ActiveTargetDisplay extends SimpleDataConnection {

        private final List<DrawObj> list= new ArrayList<DrawObj>(1);

        ActiveTargetDisplay(String name, WorldPt wp) {
            super("Query Object: " + (StringUtils.isEmpty(name) ? PositionFieldDef.formatPosForTextField(wp) : name),
                  "The center of your query", AutoColor.HIGHLIGHTED_PT);
            PointDataObj obj= new PointDataObj(wp);
            obj.setSymbol(DrawSymbol.CIRCLE);
            list.add(obj);
        }

        @Override
        public List<DrawObj> getData(boolean rebuild, WebPlot plot) {
            return list;
        }

        @Override
        public boolean isPriorityLayer() { return true; }
    }

    private static class PerPlotTargetDisplay extends SimpleDataConnection {

        PerPlotTargetDisplay() {
            super("Query Object",
                  "The center of your query", AutoColor.HIGHLIGHTED_PT);
        }



        @Override
        public List<DrawObj> getData(boolean rebuild, WebPlot plot) {
            List<DrawObj> retval= Collections.emptyList();
            if (plot!=null && plot.containsAttributeKey(WebPlot.FIXED_TARGET)) {
                ActiveTarget.PosEntry entry= (ActiveTarget.PosEntry)plot.getAttribute(WebPlot.FIXED_TARGET);
                if (entry!=null)  {
                    WorldPt wp= entry.getPt();
                    if (wp!=null) {
                        PointDataObj obj= new PointDataObj(wp);
                        obj.setSymbol(DrawSymbol.CIRCLE);
                        retval= Arrays.asList((DrawObj)obj);
                    }
                }
            }
            return retval;
        }

        @Override
        public boolean getHasPerPlotData() { return true; }

        @Override
        public String getTitle(WebPlot plot) {
            String retval= "Query Object";
            if (plot!=null && plot.containsAttributeKey(WebPlot.FIXED_TARGET)) {
                ActiveTarget.PosEntry entry= (ActiveTarget.PosEntry)plot.getAttribute(WebPlot.FIXED_TARGET);
                if (entry!=null)  {
                    WorldPt wp= entry.getPt();
                    if (wp!=null) {
                        String name= null;
                        if (wp instanceof ResolvedWorldPt) {
                            name= ((ResolvedWorldPt)wp).getObjName();
                        }
                        String posStr= (name!=null) ? name :PositionFieldDef.formatPosForTextField(wp);
                        retval= posStr;
                    }
                }
            }
            return retval;
        }

        @Override
        public boolean isPriorityLayer() { return true; }
    }


    private class DynamicActiveTargetDisplay extends SimpleDataConnection {

        private List<DrawObj> list= new ArrayList<DrawObj>(1);
        private final TablePanel table;
        private final String keyColumns[];

        DynamicActiveTargetDisplay(TablePanel table,
                                   String  keyColumns[]) {
            super("moving object", "moving object", AutoColor.HIGHLIGHTED_PT);
            this.table= table;
            this.keyColumns= keyColumns;
        }

        @Override
        public List<DrawObj> getData(boolean rebuild, WebPlot plot) {

            list= new ArrayList<DrawObj>(3);

            if (plot.containsAttributeKey(WebPlot.UNIQUE_KEY)) {
                String key= (String)plot.getAttribute(WebPlot.UNIQUE_KEY);

                TableData<TableData.Row> tableData= table.getDataset().getModel();
                for(TableData.Row row : tableData.getRows()) {
                    if (key.equals(makeKey(row))) {
                        WorldPt wp= findWorldPt(row);
                        PointDataObj obj= new PointDataObj(wp);
                        obj.setSymbol(DrawSymbol.CIRCLE);
                        list.add(obj);
                        break;
                    }
                }
            }

            return list;
        }

        private String makeKey(TableData.Row row) {
            StringBuilder sb= new StringBuilder(10*keyColumns.length);
            for(String c : keyColumns) {
                sb.append(row.getValue(c));
            }
            return sb.toString();
        }

//        @Override
//        public boolean getHasVeryLittleData() { return true; }

        @Override
        public boolean getHasPerPlotData() { return true; }

        @Override
        public boolean getSupportsMouse() { return false; }
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
