package edu.caltech.ipac.firefly.ui.background;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.background.BackgroundActivation;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.data.NewTableResults;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
/**
 * User: roby
 * Date: Aug 23, 2010
 * Time: 1:57:12 PM
 */


/**
* @author Trey Roby
*/
public class CatalogDataSetActivation implements BackgroundActivation {



    public CatalogDataSetActivation() { }

    public Widget buildActivationUI(MonitorItem monItem, int idx, boolean markAlreadyActivated) {

       String text=  "Show catalog";
       String tip =  "Show the catalog: " + monItem.getTitle();
       return UIBackgroundUtil.buildActivationUI(text, tip, monItem, idx,
                                                  this,markAlreadyActivated);
    }

    public void activate(MonitorItem monItem, int idx, boolean byAutoActivation) {
        if (byAutoActivation) {
            askAndLoad(monItem,idx);
        }
        else {
            doActivation(monItem,idx);
        }
    }

    private void doActivation(final MonitorItem monItem, int idx) {
        monItem.setActivated(true);
        BackgroundStatus bgStat= monItem.getStatus();
        if (bgStat.getBackgroundType()== BackgroundStatus.BgType.SEARCH) {
            final TableServerRequest req = (TableServerRequest) bgStat.getServerRequest().cloneRequest();
            SearchServices.App.getInstance().getRawDataSet(req, new AsyncCallback<RawDataSet>(){
                public void onFailure(Throwable caught) {
                    PopupUtil.showError("No Rows returned", "The search did not find any data");
                }

                public void onSuccess(RawDataSet result) {
                    newRawDataSet(monItem.getTitle(), result, req);
                }
            });
        }
    }

    private void askAndLoad(final MonitorItem monItem, final int idx) {
        BackgroundStatus bgStat= monItem.getStatus();
        if (bgStat.getBackgroundType()== BackgroundStatus.BgType.SEARCH) {
            final TableServerRequest req = (TableServerRequest) bgStat.getServerRequest().cloneRequest();
            SearchServices.App.getInstance().getRawDataSet(req, new AsyncCallback<RawDataSet>(){
                public void onFailure(Throwable caught) {
                    PopupUtil.showError("No Rows returned", "Your "+ monItem.getReportDesc()+
                            " catalog search did not find any data");
                }

                public void onSuccess(RawDataSet result) {
                    confirmLoad(result,req, monItem, idx);
                }
            });
        }
    }

    public void confirmLoad(RawDataSet result,
                            final TableServerRequest req,
                            final MonitorItem monItem,
                            final int idx) {
        DataSet ds= DataSetParser.parse(result);
        if (ds.getTotalRows()>0) {
            String msg= "Your "+ monItem.getTitle()+ " catalog search returned " +
                    ds.getTotalRows() + " rows. <br><br>" + "Load catalog now?";
            PopupUtil.showConfirmMsg(
                    "Background Search Complete", msg,
                    new ClickHandler() {
                        public void onClick(ClickEvent event) {
                            NewTableResults data= new NewTableResults(req,
                                                                      WidgetFactory.TABLE,
                                                                      monItem.getTitle());
                            WebEvent<NewTableResults> ev= new WebEvent<NewTableResults>(this,
                                                                                        Name.NEW_TABLE_RETRIEVED,
                                                                                        data);
                            WebEventManager.getAppEvManager().fireEvent(ev);
                            monItem.setActivated(0,true);
                        }
                    });

        }
        else {
            PopupUtil.showError("No Rows returned", "The search did not find any data");
        }

    }

    private void newRawDataSet(String title, RawDataSet rawDataSet, TableServerRequest req) {
        DataSet ds= DataSetParser.parse(rawDataSet);
        if (ds.getTotalRows()>0) {
            NewTableResults data= new NewTableResults(req, WidgetFactory.TABLE, title);
            WebEvent<NewTableResults> ev= new WebEvent<NewTableResults>(this, Name.NEW_TABLE_RETRIEVED,
                                                                        data);
            WebEventManager.getAppEvManager().fireEvent(ev);
        }
        else {
            PopupUtil.showError("No Rows returned", "The search did not find any data");
        }
    }

}


