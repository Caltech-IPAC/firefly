package edu.caltech.ipac.vamp.ui.previews;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.table.RowDetailPreview;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.vamp.searches.SearchAvmInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User: tlau
 * Date: May 5, 2010
 * Time: 4:16:47 PM
 * @version $Id: SpectralPreview.java,v 1.5 2012/05/25 21:48:47 tatianag Exp $
 */
public class SpectralPreview extends RowDetailPreview {
    private static final String REQ_AVM_ID = "avm_id";
    private static final String REQ_PUBLISHER_ID = "publisher_id";
    private static final String sql ="select distinct avm_spectral.color color, " +
            "avm_spectral.band, avm_spectral.bandpass, avm_spectral.wavelength " +
            "from avm_spectral,avm_meta where " +
            "avm_meta.avm_id = ? and avm_meta.publisher_id = ? and avm_spectral.avm_meta_id=avm_meta.avm_meta_id";

    private SplitLayoutPanel display = new SplitLayoutPanel();

    //Foreign keys: avm_id, publisher_id
    protected String _reqAvmId;
    protected String _reqPublisherId;
    protected String _cReqAvmId;
    protected String _cReqPublisherId;

    public SpectralPreview(String name) {
        this(name, REQ_AVM_ID, REQ_PUBLISHER_ID);
    }

    public SpectralPreview(String name, String reqAvmId, String reqPublisherId) {
        super(name);
        _reqAvmId = reqAvmId;
        _reqPublisherId = reqPublisherId;

        Widget tableDisplay = super.getDisplay();
        //display.addNorth(tabPanel, 200);
        display.add(tableDisplay);
        display.addStyleName("avm-details-panel");
        display.setSize("100%", "100%");
    }

    @Override
    public void clear() {
        _cReqAvmId = "";
        _cReqPublisherId = "";
    }

    protected void handleAvmTableLoad(TablePanel table, TableData.Row selRow) {
        if (selRow != null) {
            final String reqAvmIdF = String.valueOf(selRow.getValue(_reqAvmId));
            final String reqPublisherIdF = String.valueOf(selRow.getValue(_reqPublisherId));

            if (reqAvmIdF.equals(_cReqAvmId) && reqPublisherIdF.equals(_cReqPublisherId)) {
                return;  // request for the same info.. skip
            } else {
                _cReqAvmId = reqAvmIdF;
                _cReqPublisherId = reqPublisherIdF;
            }

            ServerTask<RawDataSet> requestIDTask = new ServerTask<RawDataSet>(getView(), "Get AVM Info", true) {
                public void doTask(AsyncCallback<RawDataSet> passAlong) {
                    SearchAvmInfo.Req request = new SearchAvmInfo.Req(_cReqAvmId, _cReqPublisherId, sql);
                    SearchServices.App.getInstance().getRawDataSet(request, passAlong);
                }

                public void onSuccess(RawDataSet result) {
                    DataSet dataset = DataSetParser.parse(result);
                    Map<String, String> info = new LinkedHashMap<String, String>();

                    if (dataset.getTotalRows() > 0) {
                        int i=0;
                        for (Object o: dataset.getModel().getRows()) {
                            TableData.Row row = (TableData.Row)o;
                            String num = "#"+Integer.toString(++i)+" ";
                            for (TableDataView.Column c : dataset.getColumns()) {
                                if (c.isVisible()) {
                                    info.put(num+c.getTitle(), String.valueOf(
                                            row.getValue(c.getName())));
                                }
                            }
                        }
                    }
                    loadTable(info);
                }

                public void onFailure(Throwable caught) {
                    showTable(false);
                    PopupUtil.showSevereError(caught);
                }

            };
            requestIDTask.start();
        }
    }

    @Override
    protected void doTableLoad(TablePanel table, TableData.Row selRow) {
        handleAvmTableLoad(table, selRow);
    }

    @Override
    public Widget getDisplay() {
        return display;
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
