package edu.caltech.ipac.heritage.ui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.RowDetailPreview;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.searches.HeritageRequest;
import edu.caltech.ipac.heritage.searches.SearchAorInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Date: Feb 20, 2009
 *
 * @author loi
 * @version $Id: AorDetailPreview.java,v 1.36 2012/05/25 21:48:47 tatianag Exp $
 */
public class AorDetailPreview extends RowDetailPreview {

    private static final String REQ_KEY_COL = "reqkey";
    private static final String REQ_MODE_COL = "reqmode";
    private static final String PROG_ID_COL = "progid";

    private String reqKeyCol;
    private String reqModecol;
    private String progIdCol;
    private String cReqKey = "";
    private String cReqMode = "";
    private String cProgId = "";
    private Map<String, String> cInfo = null;

    HTML abstractDetails = new HTML();
    private SplitLayoutPanel display = new SplitLayoutPanel();
    private boolean isAbstractDisplayed = false;
    private ScrollPanel abstractView;

    public AorDetailPreview(String name) {
        this(name, REQ_KEY_COL, REQ_MODE_COL, PROG_ID_COL);
    }

    public AorDetailPreview(String name, String reqKeyCol, String reqModeCol, String progIdCol) {
        super(name);
        this.reqKeyCol = reqKeyCol;
        this.reqModecol = reqModeCol;
        this.progIdCol = progIdCol;

//        abstractDetails.setVisible(false);
        abstractDetails.addStyleName("aor-details-abstract");
        clearAbstract();

        Widget tableDisplay = super.getDisplay();
        abstractView = new ScrollPanel(abstractDetails);

        display.addNorth(abstractView, 200);
        display.add(tableDisplay);
        display.addStyleName("aor-details-panel");
        // force hiding abstract display: reset takes effect only if display is different from isAbstractDisplayed
        isAbstractDisplayed = true;
        resetAbstractDisplay(false);
        display.setSize("100%", "100%");
    }

    @Override
    public void clear() {
        //cReqKey = "";
        //cReqMode = "";
    }

    protected void handleAorTableLoad(TableData.Row selRow) {
        if (selRow != null) {
            final String reqkeyF = String.valueOf(selRow.getValue(reqKeyCol));
            final String reqmodeF = String.valueOf(selRow.getValue(reqModecol));

            resetAbstractDisplay(true);

            if (reqkeyF.equals(cReqKey) && reqmodeF.equals(cReqMode) && cInfo!=null) {
                loadTable(cInfo);
                return;
            } else {
                cReqKey = reqkeyF;
                cReqMode = reqmodeF;
                cInfo = null;
            }

            ServerTask<RawDataSet> requestIDTask = new ServerTask<RawDataSet>(getView(), "Get AOR Info", true) {
                public void doTask(AsyncCallback<RawDataSet> passAlong) {
                    SearchAorInfo.Req request = new SearchAorInfo.Req(cReqMode, Integer.parseInt(cReqKey));
                    SearchServices.App.getInstance().getRawDataSet(request, passAlong);
                }

                public void onSuccess(RawDataSet result) {
                    DataSet dataset = DataSetParser.parse(result);
                    cInfo = new LinkedHashMap<String, String>();
                    if (dataset.getTotalRows() > 0) {
                        for (TableDataView.Column c : dataset.getColumns()) {
                            if (c.isVisible()) {
                                cInfo.put(c.getTitle(), String.valueOf(
                                            dataset.getModel().getRow(0).getValue(c.getName())));
                            }
                        }
                    }
                    loadTable(cInfo);
//                    tableDisplay.setHeight("95%");
                }

                public void onFailure(Throwable caught) {
                    showTable(false);
                    clearAbstract();
                    cProgId = "";
                    PopupUtil.showSevereError(caught);
                }

            };
            requestIDTask.start();

            final String progIdF = String.valueOf(selRow.getValue(progIdCol));

            if (progIdF.equals(cProgId)) {
                return;
            } else {
                cProgId  = progIdF;
                clearAbstract();
            }


            final int progid;
            try {
                progid = Integer.parseInt(progIdF);
            } catch (Exception e) {
                return;
            }

            ServerTask<Map<String,String>> abstractTask = new ServerTask<Map<String,String>>() {
                public void doTask(AsyncCallback<Map<String,String>> passAlong) {
                    edu.caltech.ipac.heritage.rpc.SearchServices.App.getInstance().
                            getAbstractInfo(progid, passAlong);
                }

                public void onSuccess(Map<String,String> result) {
                    updateAbstract(result);
                }

                public void onFailure(Throwable caught) {
                    clearAbstract();
                    //PopupUtil.showSevereError(caught);
                }

            };
            abstractTask.start();

        }
    }

    @Override
    protected void doTableLoad(TablePanel table, TableData.Row selRow) {

        if (table.getDataModel().getRequest() instanceof HeritageRequest) {
            HeritageRequest req = (HeritageRequest) table.getDataModel().getRequest();
            if( (req.getDataType() == DataType.AOR) ) {
                handleAorTableLoad(selRow);
                return;
            }
        }
        resetAbstractDisplay(false);
        super.doTableLoad(table, selRow);
    }

    private void resetAbstractDisplay(boolean displayAbstract) {
        if (isAbstractDisplayed != displayAbstract) {
            if (displayAbstract) {
                GwtUtil.SplitPanel.showWidget(display, abstractView);
                isAbstractDisplayed = true;
                display.removeStyleName("aor-details-disabled-splitter");
            } else {
                GwtUtil.SplitPanel.hideWidget(display, abstractView);
                isAbstractDisplayed = false;
                display.addStyleName("aor-details-disabled-splitter");
            }
        }
    }

    private void updateAbstract(Map<String,String> map) {
        StringBuffer pd = new StringBuffer();
        pd.append("<b>").append(map.get("progtitle")).append("</b><br>");
        pd.append(makeEntry("Program Name/Id",map.get("progname")+"/"+cProgId));
        Object scienceCat = map.get("sciencecat");
        if (!isNull(scienceCat)) {
            pd.append(makeEntry("Category", map.get("sciencecat")));
        }
        pd.append(makeEntry("PI",map.get("pi")));
        Object abstr = map.get("abstract");
        if (!isNull(abstr)) {
            pd.append(map.get("abstract"));
        }
        abstractDetails.setHTML(pd.toString());
    }

    private boolean isNull(Object o) {
        return o == null || o.equals("null");
    }

    private String makeEntry(String key, Object val) {
        return "&nbsp;&nbsp;<font color='darkBlue'>" + key + ": </font>" + String.valueOf(val) + "<br>";
    }


    private void clearAbstract() {
        abstractDetails.setHTML("<b>Abstract is not available</b>");
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
