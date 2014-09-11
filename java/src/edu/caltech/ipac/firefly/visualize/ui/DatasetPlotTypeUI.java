package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.fuse.data.DatasetInfoConverter;
import edu.caltech.ipac.firefly.fuse.data.ImagePlotDefinition;
import edu.caltech.ipac.firefly.fuse.data.PlotData;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.previews.ThreeColorPreviewData;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
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
public class DatasetPlotTypeUI extends DataConvPlotTypeUI implements ThreeColorPreviewData.CanUpdatePreview {

    private static final WebClassProperties _prop = new WebClassProperties(DatasetPlotTypeUI.class);

    public static final String BAND1 = "band1";
    public static final String BAND2 = "band2";
    public static final String BAND3 = "band3";
    public static final String BAND4 = "band4";
    public static final String NOT_USED = "Band Not Used";
    private List<String> _limitTableParam;
    private List<String> _argHeaders;
    private float _zoomLevel = Float.NaN;
    private Grid grid= new Grid(3,2);

    private final DatasetInfoConverter info;


    private final ListBox redOp = new ListBox();
    private final ListBox greenOp =new ListBox();
    private final ListBox blueOp = new ListBox();

    public DatasetPlotTypeUI(DatasetInfoConverter info) {
        super(false, false, false, true);
        this.info= info;
    }

    public void setZoomLevel(float zl) { _zoomLevel = zl; }

    public void addTab(TabPane tabs) {
        reinitUI(null);
        Widget panel= GwtUtil.makePanel(true,true,grid);
        tabs.addTab(panel, _prop.getTitle());
    }

    public void reinitUI(String id) {
        grid.clear();
        redOp.clear();
        greenOp.clear();
        blueOp.clear();

        grid.setHTML(0, 0, "Red Band: ");
        grid.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        grid.setWidget(0, 1, redOp);

        grid.setHTML(1,0, "Green Band: ");
        grid.getCellFormatter().setHorizontalAlignment(1, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        grid.setWidget(1,1, greenOp);

        grid.setHTML(2,0, "Blue Band: ");
        grid.getCellFormatter().setHorizontalAlignment(2, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        grid.setWidget(2,1, blueOp);


        ImagePlotDefinition imd= info.getImagePlotDefinition();
        PlotData pd= info.getPlotData();
        List<String> allOpList;

        List<String> threeCIDList= imd.get3ColorViewerIDs();
        if (id!=null && threeCIDList.contains(id)) {
            allOpList= imd.getAllBandOptions(id);
        }
        else {
            allOpList= imd.getAllBandOptions(threeCIDList.get(0));
        }

        for(String itemID : allOpList) {
            redOp.addItem(pd.getTitleFromID(itemID));
            greenOp.addItem(pd.getTitleFromID(itemID));
            blueOp.addItem(pd.getTitleFromID(itemID));
        }
        redOp.addItem(NOT_USED);
        greenOp.addItem(NOT_USED);
        blueOp.addItem(NOT_USED);
        int notUsedIdx= redOp.getItemCount()-1;

        if (id!=null) {
            List<String> opIDList= info.getPlotData().get3ColorIDOfIDs(id);
            if (opIDList.size()>0) {
                redOp.setSelectedIndex(allOpList.indexOf(opIDList.get(0)));
            }
            else {
                redOp.setSelectedIndex(notUsedIdx);
            }
            if (opIDList.size()>1) {
                greenOp.setSelectedIndex(allOpList.indexOf(opIDList.get(1)));
            }
            else {
                greenOp.setSelectedIndex(notUsedIdx);

            }
            if (opIDList.size()>2) {
                blueOp.setSelectedIndex(allOpList.indexOf(opIDList.get(2)));
            }
            else {
                blueOp.setSelectedIndex(notUsedIdx);
            }
        }

        grid.setCellSpacing(5);
        GwtUtil.setStyle(grid,"paddingTop", "20px");

    }

    public WebPlotRequest createRequest() { return null; }

    @Override
    public WebPlotRequest[] createThreeColorRequest() { return null; }


    @Override
    public List<String> getThreeColorIDs() {
        return Arrays.asList(getIDValue(redOp), getIDValue(greenOp), getIDValue(blueOp));
    }

    private String getIDValue(ListBox box) {
        PlotData pd= info.getPlotData();
        String retval= null;
        int i= box.getSelectedIndex();
        if (i>-1) {
            if (i==box.getItemCount()-1) retval= null;
            else                         retval= pd.getIDFromTitle(box.getItemText(i));
        }
        return retval;
    }

    protected boolean validateInput() throws ValidationException {

        if (!isUsed(getValue(redOp)) &&
                !isUsed(getValue(greenOp)) &&
                !isUsed(getValue(blueOp))) {
            throw new ValidationException(_prop.getError("chooseOne"));
        }
        return true;
    }

    private String getValue(ListBox listBox) {
        return listBox.getValue(listBox.getSelectedIndex());
    }

    private boolean isUsed(String v) { return (!(NOT_USED.equals(v))); }

//    private Param getBandParam(String v) {
//        Param retval = null;
//        if (BAND1.equals(v)) {
//            retval = new Param("band", "1");
//        } else if (BAND2.equals(v)) {
//            retval = new Param("band", "2");
//        } else if (BAND3.equals(v)) {
//            retval = new Param("band", "3");
//        } else if (BAND4.equals(v)) {
//            retval = new Param("band", "4");
//        }
//        return retval;
//    }

    public void updateSizeArea() { }

    public String getDesc() { return "Select Band"; }

    public int getHeight() { return 150; }

    public void setPreviewData(ThreeColorPreviewData prevData) { }

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
