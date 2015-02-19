/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.draw.AutoColor;
import edu.caltech.ipac.firefly.visualize.draw.DataConnection;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
import edu.caltech.ipac.firefly.visualize.draw.SimpleDataConnection;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Date: Aug 4, 2010
 *
 * @author loi
 * @version $Id: RowTargetCreator.java,v 1.10 2012/05/09 23:24:01 roby Exp $
 */
public class RowTargetCreator implements EventWorkerCreator {

    public static final String ROW_TARGET= "RowTarget";

    public EventWorker create(Map<String, String> params) {

        RowTargetWorker worker = new RowTargetWorker();
        worker.setParams(params);
        worker.setEventsByName(Arrays.asList(EventHub.ON_ROWHIGHLIGHT_CHANGE));
        worker.setQuerySources(StringUtils.asList(worker.getParam(EventWorker.QUERY_SOURCE), ","));
        if (params.containsKey(EventWorker.ID)) worker.setID(params.get(EventWorker.ID));

        return worker;
    }


    static class RowTargetWorker extends BaseEventWorker<DataConnection> {

        public RowTargetWorker() {
            super(ROW_TARGET);
        }

        @Override
        protected boolean useEvent(WebEvent ev) {
            boolean retval= false;
            if (ev.getSource() instanceof TablePanel) {
                TablePanel t= (TablePanel) ev.getSource();
                retval= getQuerySources().contains(t.getName());
            }
            return retval;
        }

        public void handleEvent(WebEvent ev) {
            if (!(ev.getSource() instanceof TablePanel)) return;
            final TablePanel table = (TablePanel) ev.getSource();
            if (!this.getQuerySources().contains(table.getName())){
                return;
            }

            Vis.init(new Vis.InitComplete() {
                public void done() {
                    handleResults(new RowTargetDisplay(table));
                }
            });
        }
    }

    private static class RowTargetDisplay extends SimpleDataConnection {

        private final List<DrawObj> list= new ArrayList<DrawObj>(1);

        RowTargetDisplay(TablePanel table) {
            super("Selected Row Center", "The RA,DEC of the selected Row", AutoColor.PT_4);


            TableData.Row row = table.getTable().getHighlightedRow();

            TableMeta.LonLatColumns llc=  table.getDataset().getMeta().getCenterCoordColumns();
            if (row!=null && llc!=null) {
                try {
                    String raS = String.valueOf(row.getValue(llc.getLonCol()));
                    double ra = Double.parseDouble(raS);
                    String decS = String.valueOf(row.getValue(llc.getLatCol()));
                    double dec = Double.parseDouble(decS);
                    WorldPt wp= new WorldPt(ra,dec,llc.getCoordinateSys());
                    PointDataObj obj= new PointDataObj(wp, DrawSymbol.DIAMOND);
                    list.add(obj);
                } catch(NumberFormatException ex) {
                    // skip bad data
                }
            }

        }

        @Override
        public List<DrawObj> getData(boolean rebuild, WebPlot plot) {
            return list;
        }

        @Override
        public boolean getSupportsMouse() { return false; }

    }
}
