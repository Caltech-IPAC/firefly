package edu.caltech.ipac.hydra.ui.wise;

import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.previews.ThreeColorPreviewData;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotRelatedPanel;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.firefly.visualize.ui.NeedsHub;
import edu.caltech.ipac.firefly.visualize.ui.PlotTypeUI;
import edu.caltech.ipac.util.dd.ValidationException;

import java.util.Arrays;
import java.util.List;
/**
 * User: roby
 * Date: Aug 11, 2010
 * Time: 12:34:49 PM
 */


/**
 * @author Trey Roby
 */
public class WisePlotTypeUI extends PlotTypeUI implements NeedsHub,
                                                          PlotRelatedPanel,
                                                          ThreeColorPreviewData.CanUpdatePreview {

    private static final WebClassProperties _prop = new WebClassProperties(WisePlotTypeUI.class);

    public static final String BAND1 = "band1";
    public static final String BAND2 = "band2";
    public static final String BAND3 = "band3";
    public static final String BAND4 = "band4";
    public static final String NOT_USED = "notused";
    private List<String> _limitTableParam;
    private List<String> _argHeaders;
    private float _zoomLevel = Float.NaN;
    private TablePanel _table = null;
    private ThreeColorPreviewData _prevData;


    private final SimpleInputField _redOp = SimpleInputField.createByProp(_prop.makeBase("band.red"), new SimpleInputField.Config("100px"));
    private final SimpleInputField _greenOp = SimpleInputField.createByProp(_prop.makeBase("band.green"), new SimpleInputField.Config("100px"));
    private final SimpleInputField _blueOp = SimpleInputField.createByProp(_prop.makeBase("band.blue"), new SimpleInputField.Config("100px"));

    public WisePlotTypeUI(List<String> limitTableParam, List<String> argHeaders) {
        super(false, false, false, true);
        _limitTableParam = limitTableParam;
        _argHeaders = argHeaders;
    }

    public void setZoomLevel(float zl) { _zoomLevel = zl; }

    public void addTab(TabPane tabs) {
        VerticalPanel panel = new VerticalPanel();
        panel.add(_redOp);
        panel.add(_greenOp);
        panel.add(_blueOp);
        panel.setSpacing(5);
        tabs.addTab(panel, _prop.getTitle());
    }

    public WebPlotRequest createRequest() { return null; }

    @Override
    public WebPlotRequest[] createThreeColorRequest() { return createRequestForRow(); }

    protected boolean validateInput() throws ValidationException {

        if (_table == null) {
            throw new ValidationException("Cannot find TablePanel");
        }
        if (!isUsed(_redOp.getValue()) &&
                !isUsed(_greenOp.getValue()) &&
                !isUsed(_blueOp.getValue())) {
            throw new ValidationException(_prop.getError("chooseOne"));
        }
        return true;
    }

    private boolean isUsed(String v) { return (!(NOT_USED.equals(v))); }

    private Param getBandParam(String v) {
        Param retval = null;
        if (BAND1.equals(v)) {
            retval = new Param("band", "1");
        } else if (BAND2.equals(v)) {
            retval = new Param("band", "2");
        } else if (BAND3.equals(v)) {
            retval = new Param("band", "3");
        } else if (BAND4.equals(v)) {
            retval = new Param("band", "4");
        }
        return retval;
    }

    public void updateSizeArea() { }

    public String getDesc() { return _prop.getTitle("fileOnServer"); }

    public int getHeight() { return 150; }

    public void bind(TablePreviewEventHub hub) {
        WebEventListener wel = new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (ev.getSource() instanceof TablePanel) {
                    _table = (TablePanel) ev.getSource();
                }
            }
        };
        hub.getEventManager().addListener(TablePreviewEventHub.ON_ROWHIGHLIGHT_CHANGE, wel);
        hub.getEventManager().addListener(TablePreviewEventHub.ON_TABLE_SHOW, wel);
    }

    public void setPreviewData(ThreeColorPreviewData prevData) { _prevData = prevData; }

    public WebPlotRequest[] createRequestForRow() {

        TableData.Row<String> row = _table.getTable().getHighlightedRow();

        WebPlotRequest red = setParamsAndMakeRequest(_redOp.getValue(), Band.RED, row);
        WebPlotRequest green = setParamsAndMakeRequest(_greenOp.getValue(), Band.GREEN, row);
        WebPlotRequest blue = setParamsAndMakeRequest(_blueOp.getValue(), Band.BLUE, row);

        return new WebPlotRequest[]{red, green, blue};
    }


    private WebPlotRequest setParamsAndMakeRequest(String opValue,
                                                   Band band,
                                                   TableData.Row<String> row) {
        Param param;
        WebPlotRequest req = null;
        if (isUsed(opValue)) {
            param = getBandParam(opValue);
            req = makeServerRequest(_table, param, row);
            _prevData.setBandParams(band, Arrays.asList(param));
        } else {
            _prevData.setBandParams(band, null);
        }
        return req;
    }


    public WebPlotRequest makeServerRequest(TablePanel table,
                                            Param wiseBandParam,
                                            TableData.Row<String> row) {
        ServerRequest sr = new ServerRequest("WiseFileRetrieve");
        sr.setParam(wiseBandParam);

        String cname;
        for (TableDataView.Column c : table.getDataset().getColumns()) {
            cname = c.getName();
            if (_limitTableParam == null ||
                    _limitTableParam.size() == 0 ||
                    _limitTableParam.contains(cname)) {
                sr.setSafeParam(cname, row.getValue(cname));
            }
        }


        if (_argHeaders != null && _argHeaders.size() > 0) {
            TableMeta meta = table.getDataset().getMeta();
            for (String key : _argHeaders) {
                if (meta.contains(key)) {
                    sr.setSafeParam(key, meta.getAttribute(key));
                }
            }
        }
        WebPlotRequest req = WebPlotRequest.makeProcessorRequest(sr, "");
        req.setContinueOnFail(true);
        if (Float.isNaN(_zoomLevel)) req.setZoomType(ZoomType.SMART);
        else req.setInitialZoomLevel(_zoomLevel);
        return req;
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
