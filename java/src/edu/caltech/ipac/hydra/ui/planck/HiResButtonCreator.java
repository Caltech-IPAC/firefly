package edu.caltech.ipac.hydra.ui.planck;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import edu.caltech.ipac.firefly.core.BaseCallback;
import edu.caltech.ipac.firefly.data.NewTabInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseTableButtonSetter;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorkerCreator;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.PropertyChangeEvent;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.StringUtils;

import java.util.Map;
/**
 */
public class HiResButtonCreator implements EventWorkerCreator {
    public static final String ID = "PlanckHiRes";

    public EventWorker create(Map<String, String> params) {
        HiResButtonSetter worker = new HiResButtonSetter();
        worker.setQuerySources(StringUtils.asList(params.get(EventWorker.QUERY_SOURCE), ","));
        if (params.containsKey(EventWorker.ID)) worker.setID(params.get(EventWorker.ID));

        return worker;
    }

    public static class HiResButtonSetter extends BaseTableButtonSetter {
        private TableDataView dataset;
        private TablePanel tablePanel;
        private BaseDialog dialog;

        public HiResButtonSetter() {
            super(ID);
        }

        protected FocusWidget makeButton(final TablePanel table) {
            final Button button = GwtUtil.makeButton("HiRes", "Generate High Res Image", new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    if (dialog == null) {
                        dialog= new BaseDialog(table, ButtonType.OK_CANCEL,"Input Dialog",true,null) {
                            protected void inputComplete() {
                                dialog.setVisible(false);
                                generateHiRes();
                            }
                            protected void inputCanceled() {
                                dialog.setVisible(false);
                            }
                        };
                        HTML content = new HTML("Put your options here...<br><br>");
                        content.setHTML(content.getHTML() + "<br>" + "Selected data from table:");
                        for (int i : table.getDataset().getSelected()) {
                            TableData.Row row = table.getDataModel().getCurrentData().getModel().getRow(i);
                            content.setHTML(content.getHTML() + "<br>" + i + " - " + StringUtils.toString(row.getValues().values()));
                        }
                        content.setSize("600px", "400px");
                        dialog.setWidget(content);
                    }
                    dialog.show();
                }
            });
            dataset = table.getDataset();

            button.setEnabled(checkSelection());
            dataset.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent pce) {
                    button.setEnabled(checkSelection());
                }
            });

            return button;
        }

        private void generateHiRes() {
            NewTabInfo newTabInfo = new NewTabInfo("HiRes");
            MiniPlotWidget mpw = makeImagePlot(newTabInfo);
            newTabInfo.setDisplay(mpw);
            WebEventManager.getAppEvManager().fireEvent(new WebEvent(this, Name.NEW_TABLE_RETRIEVED, newTabInfo));
        }

        private MiniPlotWidget makeImagePlot(final NewTabInfo newTabInfo) {
            final MiniPlotWidget mpw= new MiniPlotWidget(newTabInfo.getName());
            GwtUtil.setStyles(mpw, "fontFamily", "tahoma,arial,helvetica,sans-serif",
                    "fontSize", "11px");
            mpw.setRemoveOldPlot(true);
            mpw.setMinSize(50, 50);
            mpw.setAutoTearDown(false);
            mpw.setLockImage(false);
            mpw.addStyleName("standard-border");
            mpw.getOps(new MiniPlotWidget.OpsAsync() {
                public void ops(PlotWidgetOps widgetOps) {
                    ServerRequest req = new ServerRequest("your_processor_id");
                    // add all of the params here.. so it can be sent to server.
                    WebPlotRequest wpr = WebPlotRequest.makeProcessorRequest(req, "HiRes image");
                    widgetOps.plot(wpr, false, new BaseCallback<WebPlot>() {
                        public void doSuccess(WebPlot result) {
                            newTabInfo.ready();
                        }
                    });
                }
            });
            return mpw;
        }

        private boolean checkSelection() {
            return dataset != null && dataset.getSelectionInfo().getSelectedCount() > 0;
        }
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
