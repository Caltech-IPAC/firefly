package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.input.FileUploadField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PrintableOverlay;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.draw.Drawer;
import edu.caltech.ipac.firefly.visualize.draw.RegionConnection;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.firefly.visualize.draw.TabularDrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;
import edu.caltech.ipac.firefly.visualize.task.VisTask;
import edu.caltech.ipac.firefly.visualize.task.rpc.RegionData;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.ValidationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Trey Roby
 */
public class DS9RegionLoadDialog extends BaseDialog {

    private static final WebClassProperties _prop= new WebClassProperties(DS9RegionLoadDialog.class);

    private final VerticalPanel _topPanel= new VerticalPanel();
    private FileUploadField _uploadField;
    private static int cnt= 1;
    private static Map<String,RegionDrawing> regMap= new HashMap<String, RegionDrawing>(13);


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public DS9RegionLoadDialog() {
        super(RootPanel.get(), ButtonType.OK_CANCEL_HELP, _prop.getTitle(), "visualization.RegionLoad");

//        Button applyB= getButton(ButtonID.OK);
//        applyB.setText(_prop.getName("load"));

        createContents();
        setWidget(_topPanel);
        _topPanel.setPixelSize(400, 100);
        setHideAlgorythm(HideType.AFTER_COMPLETE);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    @Override
    public void setVisible(boolean v) {
        super.setVisible(v, PopupPane.Align.TOP_LEFT, 200, 45);
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private void createContents() {
        SimpleInputField field = SimpleInputField.createByProp(_prop.makeBase("upload"));
        _topPanel.add(field);
        _uploadField= (FileUploadField)field.getField();
        HTML help= new HTML("Supported regions: text, circle, box, polygon, line, annulus, text");
        _topPanel.add(help);


    }

    @Override
    public void inputCompleteAsync(final AsyncCallback<String> cb) {
        _uploadField.submit(new AsyncCallback<String>() {
            public void onFailure(Throwable caught) { }

            public void onSuccess(String fileKey) {
                new VisTask().getDS9Region(fileKey,new AsyncCallback<RegionData>() {
                    public void onFailure(Throwable caught) {
                        PopupUtil.showInfo("failed");
                    }

                    public void onSuccess(RegionData result) {
                        loadRegion(result.getTitle(),
                                   result.getRegionTextData(),
                                   result.getRegionParseErrors());
                        cb.onSuccess("ok");
                    }
                });
            }
        });
    }

    private void loadRegion(String title, String regText, String regErr) {
        TabularDrawingManager drawMan;
        List<String> retStrList= StringUtils.parseStringList(regText);
        List<String> errStrList= StringUtils.parseStringList(regErr);
        List<Region> regList= new ArrayList<Region>(retStrList.size());
        for(String s : retStrList) {
            Region r= Region.parse(s);
            if (r!=null)  regList.add(r);
        }
        if (regList.size()>0) {
            RegionConnection rc= new RegionConnection(title, regList);
            String id= "RegionOverlay" + (cnt++);
            RegionPrintable printable= new RegionPrintable(regList);
            drawMan= new TabularDrawingManager(id, rc, printable);
            drawMan.setCanDoRegion(false); // we actually can do region but we want to do it manually
            regMap.put(id,new RegionDrawing(id,drawMan));
            for(MiniPlotWidget mpw : AllPlots.getInstance().getAll()) {
                drawMan.addPlotView(mpw.getPlotView());
            }
            AlertLayerPopup.setAlert(true);
        }
        if (regList.size()==0 || errStrList.size()>0) {
            StringBuilder sb= new StringBuilder(20);
            if (regList.size()==0) {
                sb.append("<b>No regions loaded.</b>");
                if (errStrList.size()>0) sb.append("<br><br>");
            }
            if (errStrList.size()>0) {
                sb.append("<span style=\"text-decoration: underline;\">The following "+
                                  (errStrList.size()>1 ? "errors" : "error") +
                                   " occurred parsing the file</span><br><br>");
                sb.append(makeErrorList(errStrList));
            }
            PopupUtil.showError("Region", sb.toString());
        }

    }

    private static String makeErrorList(List<String> errStrList) {
        StringBuilder sb= new StringBuilder(200);
        for(String s : errStrList) {
            sb.append(s).append("<br>");
        }
        if (errStrList.size()>0) {
           sb.delete(sb.length()-4, sb.length());
        }
        return sb.toString();
    }

    private static void removeRegion(String id) {
        RegionDrawing rd= regMap.get(id);
        if (rd!=null) rd.freeResources();
    }

    private static class RegionDrawing {
        private final String id;
        private TabularDrawingManager drawMan;

        private RegionDrawing(String id, TabularDrawingManager drawMan) {
            this.id = id;
            this.drawMan = drawMan;
            if (!WebLayerItem.hasUICreator(id)) {
                WebLayerItem.addUICreator(id, new RegionUICreator());
            }
        }

        public void freeResources() {
            List<MiniPlotWidget> mpwList = AllPlots.getInstance().getAll();
            for (MiniPlotWidget mpw : mpwList) drawMan.removePlotView(mpw.getPlotView());
            drawMan = null;
        }

    }


    @Override
    protected boolean validateInput() throws ValidationException {
        return true;
    }

// =====================================================================
// -------------------- Inner Classes ----------------------------------
// =====================================================================

    private static class RegionUICreator extends WebLayerItem.UICreator {

        private RegionUICreator() { super(false,true); }

        public void delete(WebLayerItem item) { removeRegion(item.getID()); }
    }


    private static class RegionPrintable implements PrintableOverlay {

        List<Region> regList;

        private RegionPrintable(List<Region> regList) {
            this.regList = regList;
        }

        public void addPrintableLayer(List<StaticDrawInfo> drawInfoList, WebPlot plot, Drawer drawer, WebLayerItem item) {
            StaticDrawInfo info= new StaticDrawInfo();
            info.setDrawType(StaticDrawInfo.DrawType.REGION);
            for(Region r : regList) {
                if (r.getOptions().isInclude()) {
                    info.addRegion(r);
                }
            }
            drawInfoList.add(info);
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

