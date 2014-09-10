package edu.caltech.ipac.firefly.visualize.ui;

import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.WebClassProperties;

import java.util.List;
/**
 * User: roby
 * Date: Aug 11, 2010
 * Time: 12:34:49 PM
 */


/**
 * @author Trey Roby
 */
public class DatasetPlotTypeUI /*extends DataConvPlotTypeUI implements ThreeColorPreviewData.CanUpdatePreview*/ {

    private static final WebClassProperties _prop = new WebClassProperties(DatasetPlotTypeUI.class);

    public static final String BAND1 = "band1";
    public static final String BAND2 = "band2";
    public static final String BAND3 = "band3";
    public static final String BAND4 = "band4";
    public static final String NOT_USED = "notused";
    private List<String> _limitTableParam;
    private List<String> _argHeaders;
    private float _zoomLevel = Float.NaN;
    private TablePanel _table = null;
    /*
    private final DatasetInfoConverter info;


    private final ListBox _redOp = new ListBox();
    private final ListBox _greenOp =new ListBox();
    private final ListBox _blueOp = new ListBox();

    public DatasetPlotTypeUI(DatasetInfoConverter info) {
        super(false, false, false, true);
        this.info= info;
    }

    public void setZoomLevel(float zl) { _zoomLevel = zl; }

    public void addTab(TabPane tabs) {
        VerticalPanel panel = new VerticalPanel();
        Grid grid= new Grid(2,3);


        grid.setHTML(0,0, "Red Band: ");
        grid.setWidget(0,1, _redOp);

        grid.setHTML(1,0, "Green Band: ");
        grid.setWidget(1,1, _greenOp);

        grid.setHTML(2,0, "Blue Band: ");
        grid.setWidget(2,1, _blueOp);



        tabs.addTab(panel, _prop.getTitle());
    }

    public WebPlotRequest createRequest() { return null; }

    @Override
    public WebPlotRequest[] createThreeColorRequest() { return createRequestForRow(); }


    @Override
    public List<String> getThreeColorIDs() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected boolean validateInput() throws ValidationException {

        if (_table == null) {
            throw new ValidationException("Cannot find TablePanel");
        }
        if (!isUsed(getValue(_redOp)) &&
                !isUsed(getValue(_greenOp)) &&
                !isUsed(getValue(_blueOp))) {
            throw new ValidationException(_prop.getError("chooseOne"));
        }
        return true;
    }

    private String getValue(ListBox listBox) {
        return listBox.getValue(listBox.getSelectedIndex());
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

    public String getDesc() { return "Select Band"; }

    public int getHeight() { return 150; }

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

*/
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
