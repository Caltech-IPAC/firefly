package edu.caltech.ipac.firefly.ui.previews;

import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;

import java.util.ArrayList;
import java.util.List;
/**
 * User: roby
 * Date: Apr 20, 2010
 * Time: 12:13:04 PM
 */


/**
 * @author Trey Roby
 */
public class XmlDataSourceCoverageData implements DataSourceCoverageData {

    private List<String> _eventWorkerList = new ArrayList<String>(1);
    private final ZoomType _smartType;
    private String _group= null;
    private String _title = "Coverage";
    private boolean _enableDetails= true;
    private boolean _useBlankPlot= false;

    public XmlDataSourceCoverageData(ZoomType smartType) {
        smartType= WebPlotRequest.isSmartZoom(smartType) ? smartType : ZoomType.SMART;
        _smartType = smartType;
    }

    public String getTitle() {
        return _title;
    }

    public String setTitle(String title) {
        return _title = title;
    }


    public String getTip() {
        return "Shows the coverage of the table";
    }

    public ZoomType getSmartZoomHint() { return _smartType; }

    public List<String> getEventWorkerList() {
        return _eventWorkerList;
    }


    public void setEventWorkerList(List<String> l) {
        _eventWorkerList = l;
    }

    public void setGroup(String group) { _group= group; }
    public String getGroup() { return _group; }

    public String getCoverageBaseTitle() {
        return " ";
    }

    public void setEnableDetails(boolean enable) { _enableDetails= enable; }
    public boolean getEnableDetails() { return _enableDetails; }

    public boolean getUseBlankPlot() { return _useBlankPlot;  }
    public void setUseBlankPlot(boolean useBlankPlot) { _useBlankPlot= useBlankPlot;  }
}

