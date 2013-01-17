package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.SplitPanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;

/**
 * Date: Feb 24, 2009
 *
 * @author loi
 * @version $Id: TablePreviewCombo.java,v 1.13 2012/01/11 00:30:16 loi Exp $
 */
public class TablePreviewCombo extends SplitPanel {

    private TablePanel table;
    private PreviewTabPane preview;

    public TablePreviewCombo(String name, Loader<TableDataView> loader) {
        this(new SelectableTablePanel(name, loader), new PreviewTabPane());
    }

    public TablePreviewCombo(TablePanel table, PreviewTabPane preview) {
        this.table = table;
        this.preview = preview;
        table.init();
        preview.bind(table);

        if (preview != null) {
            setSecondaryWidget(preview);
            setLayout(Layout.LEFT);
            setSplitPosition("67%");
        }

        setMainWidget(table);
        setScrollMode(table, "visible");
        table.getEventManager().addListener(SelectableTablePanel.ON_VIEW_CHANGE, new WebEventListener(){
                    public void eventNotify(WebEvent ev) {
                        String vn = String.valueOf(ev.getData());
                        if (vn.equals(TextView.NAME.getName())) {
                            setSplitPosition("100%");
                        } else {
                            setSplitPosition("67%");
                            }
                    }
                });
    }

//    @Override
    public void setHeight(final String height) {
        super.setHeight(height);
        if (table.isInit()) {
            if (height.toLowerCase().indexOf("px") >= 0) {
                int h = Integer.parseInt(height.replaceAll("\\D", "")) -10;
                table.setHeight(h+"px");
                preview.setHeight(h+"px");
            } else {
                table.setHeight(height);
                preview.setHeight(height);
            }
        } else {
            DeferredCommand.addCommand(new IncrementalCommand(){
                public boolean execute() {
                    if(table.isInit()) {
                        if (height.toLowerCase().indexOf("px") >= 0) {
                            int h = Integer.parseInt(height.replaceAll("\\D", "")) -10;
                            table.setHeight(h+"px");
                            preview.setHeight(h+"px");
                        } else {
                            table.setHeight(height);
                            preview.setHeight(height);
                        }
                        return false;
                    }
                    return true;
                }
            });
        }
    }

    public TablePanel getTable() {
        return table;
    }

    public PreviewTabPane getPreview() {
        return preview;
    }


//    class ResizeWrapper extends ResizableWrapper<PreviewTabPane> {
//        public ResizeWrapper(PreviewTabPane preview) {
//            super(preview);
//        }
//
//        @Override
//        public void onResize(int width, int height) {
//            height = height - GwtUtil.getOffsetValueY(previewWrapper);
//            width = width - GwtUtil.getOffsetValueY(previewWrapper);
//            this.getWrappedWidget().onResize(width, height);
//        }
//    }
//
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
