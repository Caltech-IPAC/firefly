package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
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
        worker.setEventsByName(Arrays.asList(TablePreviewEventHub.ON_ROWHIGHLIGHT_CHANGE));
        worker.setQuerySources(StringUtils.asList(worker.getParam(EventWorker.QUERY_SOURCE), ","));
        if (params.containsKey(EventWorker.ID)) worker.setID(params.get(EventWorker.ID));

        return worker;
    }


    static class RowTargetWorker extends BaseEventWorker<DataConnection> {

        public RowTargetWorker() {
            super(ROW_TARGET);
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
                    PointDataObj obj= new PointDataObj(wp);
                    obj.setSymbol(DrawSymbol.DIAMOND);
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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
