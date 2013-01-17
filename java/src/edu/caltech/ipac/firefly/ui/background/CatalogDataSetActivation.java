package edu.caltech.ipac.firefly.ui.background;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.background.BackgroundActivation;
import edu.caltech.ipac.firefly.core.background.BackgroundReport;
import edu.caltech.ipac.firefly.core.background.BackgroundSearchReport;
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




    public CatalogDataSetActivation() {
    }

    public Widget buildActivationUI(MonitorItem monItem, int idx, boolean markAlreadyActivated) {

       String text=  "Show catalog: "+monItem.getTitle();
       String tip =  "Show the catalog: " + monItem.getTitle();
       return UIBackgroundUtil.buildActivationUI(text, tip, monItem, idx,
                                                  this,markAlreadyActivated);
    }

    public void activate(final MonitorItem monItem, int idx) {
        monItem.setActivated(0,true);
        BackgroundReport r= monItem.getReport();
        if (r instanceof BackgroundSearchReport) {
            BackgroundSearchReport pbr= (BackgroundSearchReport)r;
            final TableServerRequest req = pbr.getServerRequest();
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

    public boolean getImmediately() { return false; }

    private void newRawDataSet(String title, RawDataSet rawDataSet, TableServerRequest req) {
        DataSet ds= DataSetParser.parse(rawDataSet);
        if (ds.getTotalRows()>0) {
            NewTableResults data= new NewTableResults(req, WidgetFactory.BASIC_TABLE, title);
            WebEvent<NewTableResults> ev= new WebEvent<NewTableResults>(this, Name.NEW_TABLE_RETRIEVED,
                                                                        data);
            WebEventManager.getAppEvManager().fireEvent(ev);
        }
        else {
            PopupUtil.showError("No Rows returned", "The search did not find any data");
        }
    }

    public String getWaitingMsg() { return "Retrieving Catalog..."; }
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
