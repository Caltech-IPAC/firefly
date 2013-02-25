package edu.caltech.ipac.heritage.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.CommonRequestCmd;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.util.dd.StringFieldDef;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.rpc.ResourceServices;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.input.TextBoxInputField;
import edu.caltech.ipac.firefly.ui.table.PagingDataSetLoader;
import edu.caltech.ipac.firefly.ui.table.TableGroupPreviewCombo;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.RowDetailPreview;

import java.util.List;

public class IpacTableViewerCmd extends CommonRequestCmd {
    public static final String COMMAND_NAME = "IpacTableViewer";

    public IpacTableViewerCmd(String title, String desc, boolean enabled) {
        super(COMMAND_NAME, title, desc, enabled);
    }



    protected void processRequest(final Request req, final AsyncCallback<String> callback) {


        TableServerRequest sreq = new TableServerRequest();
        PagingDataSetLoader ipacTableLoader = new PagingDataSetLoader(sreq) {
                protected void doLoadData(TableServerRequest request, AsyncCallback<RawDataSet> callback) {
                    String path1 = req.getParam("path1");
                    ResourceServices.App.getInstance().getIpacTable(path1, req, callback);
                }

                public void onLoad(TableDataView result) {
//                    setupColumns(result, SearchTask.BCD_LIST);
                }

                private void setupColumns(TableDataView source, List<String> limiter) {
                    for (DataSet.Column col : source.getColumns()) {
                        if (!limiter.contains(col.getName())) {
                            col.setVisible(true);
                        }
                    }
                    for (int i = limiter.size() - 1; i >= 0; i--) {
                        source.moveColumn(source.findColumn(limiter.get(i)), 0);
                    }
                }
            };


        TableGroupPreviewCombo p = new TableGroupPreviewCombo();
        TablePanel t1 = p.addTable("Data1", ipacTableLoader);
        TablePanel t2 = p.addTable("Data2", ipacTableLoader);
        p.setHeight("500px");
        p.getPreview().addView(new RowDetailPreview("Details"));

        t1.init();
        t2.init();
        setResults(p);
    }

    public Form createForm() {
        Form form = new Form();

        TextBoxInputField path1 = new TextBoxInputField(new StringFieldDef("path1"));
        form.add(path1);

        return form;
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
