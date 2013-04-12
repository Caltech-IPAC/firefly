package edu.caltech.ipac.firefly.visualize.task;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.visualize.ActiveTarget;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotRequestHistory;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.Circle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * User: roby
 * Date: Apr 28, 2009
 * Time: 4:25:00 PM
 */


/**
 * @author Trey Roby
 */
public class PlotFileTaskHelper {
    private final WebPlotRequest _request1;
    private final WebPlotRequest _request2;
    private final WebPlotRequest _request3;
    private final boolean _threeColor;
    private final MiniPlotWidget _mpw;
    private final boolean _addToHistory;
    private final boolean _removeOldPlot;
    private final AsyncCallback<WebPlot> _notify;
    private boolean _continueOnSuccess = true;
    private String _plotCtxStr = null;
    private final PlotFileTask _task;


    public PlotFileTaskHelper(WebPlotRequest request1,
                              WebPlotRequest request2,
                              WebPlotRequest request3,
                              boolean threeColor,
                              boolean removeOldPlot,
                              boolean addToHistory,
                              AsyncCallback<WebPlot> notify,
                              MiniPlotWidget mpw,
                              PlotFileTask task) {
        _request1 = request1;
        _request2 = request2;
        _request3 = request3;
        _threeColor = threeColor;
        _mpw = mpw;
        _notify = notify;
        _removeOldPlot = removeOldPlot;
        _addToHistory = addToHistory;
        _task= task;
    }

    public void handleFailure(Throwable e) {
        if (_removeOldPlot) {
            _mpw.setFlipBarVisible(false);
            _mpw.getPlotView().clearAllPlots();
        }
        String extra = "";
        if (e.getCause() != null) {
            extra = e.getCause().toString();

        }
//        Window.alert("Plot Failed: Server Error: "+  extra);
        _mpw.processError(null, "Server Error", "Plot Failed: Server Error: " + extra, null);
        if (_notify != null) _notify.onFailure(null);
    }


    public MiniPlotWidget getMiniPlotWidget() { return _mpw; }

    public List<Param> makeParamList() {
        List<Param> paramList = new ArrayList<Param>(4);

        if (_threeColor) {
            if (_request1!=null) paramList.add(new Param(ServerParams.RED_REQUEST, _request1.toString()));
            if (_request2!=null) paramList.add(new Param(ServerParams.GREEN_REQUEST, _request2.toString()));
            if (_request3!=null) paramList.add(new Param(ServerParams.BLUE_REQUEST, _request3.toString()));
        } else {
            paramList.add(new Param(ServerParams.NOBAND_REQUEST, _request1.toString()));
        }
        return paramList;
    }

    public void handleSuccess(WebPlotResult result) {
        long start = System.currentTimeMillis();
        try {
            if (_removeOldPlot) {
                _mpw.setFlipBarVisible(false);
                _mpw.getPlotView().clearAllPlots();
            }
            if (result.isSuccess()) {
                WebPlot plot;
                WebPlot firstPlot = null;
                WebPlotView pv = _mpw.getPlotView();
                boolean maySetFrame = pv.size() == 0;
                CreatorResults creatorResults= (CreatorResults)result.getResult(WebPlotResult.PLOT_CREATE);
                for (WebPlotInitializer wpInit : creatorResults) {
                    plot = new WebPlot(wpInit);
                    if (firstPlot == null) firstPlot = plot;
                    if (_continueOnSuccess) {
                        if (_addToHistory) PlotRequestHistory.instance().add(getRequest());
                        addAttributes(plot);
                        pv.addPlot(plot, false);
                    } else {
                        killAfterSuccess(plot);
                    }
                }
                if (_continueOnSuccess) {
                    pv.setPrimaryPlot(firstPlot);

                    if (creatorResults.size() > 1) {
                        _mpw.postPlotTask(getRequest().getTitle(), pv.getPrimaryPlot(), _notify);
                    } else {
                        _mpw.postPlotTask(getPostPlotTitle(firstPlot), pv.getPrimaryPlot(), _notify);
                    }

                    if (maySetFrame) {
                        if (creatorResults.size() > 1) {
                            _mpw.setFlipBarVisible(true);
                            pv.setPrimaryPlot(pv.getPlot(0));
                        } else {
                            _mpw.setFlipBarVisible(false);
                        }

                    }
                    _mpw.forcePlotPrefUpdate();
                    List<WebPlotRequest> reqList = _threeColor ?
                                                   new ArrayList<WebPlotRequest>(Arrays.asList(_request1, _request2, _request3)) :
                                                   new ArrayList<WebPlotRequest>(Arrays.asList(_request1));

                    WebEvent<List<WebPlotRequest>> ev = new WebEvent<List<WebPlotRequest>>(_task, Name.PLOT_REQUEST_COMPLETED, reqList);
                    _mpw.getPlotView().getEventManager().fireEvent(ev);
                }
            } else {
                showFailure(result);
            }
        } catch (Exception e) {
            _mpw.processError(null, e.getMessage(), "WebPlot exception: " + e, e);
            GWT.log("WebPlot exception: " + e, e);
        }
//        GWT.log("plot task time: " + (System.currentTimeMillis()-start));

    }

    public WebPlotRequest getRequest() {
        WebPlotRequest req = null;
        if (_request1 != null) req = _request1;
        else if (_request2 != null) req = _request2;
        else if (_request3 != null) req = _request3;
        return req;
    }

    public boolean isThreeColor() {
        return _threeColor;
    }

    public WebPlotRequest getRequest(Band band) {
        WebPlotRequest retval;
        if (_threeColor) {
            WebAssert.argTst(band != Band.NO_BAND, "This is a 3 color request, band must be RED, GREEN, or BLUE");
            switch (band) {
                case RED:
                    retval = _request1;
                    break;
                case GREEN:
                    retval = _request2;
                    break;
                case BLUE:
                    retval = _request3;
                    break;
                default:
                    retval = null;
                    break;
            }
        } else {
            WebAssert.argTst(band == Band.NO_BAND, "This is not a 3 color request, band must be NO_BAND");
            retval = _request1;
        }
        return retval;
    }

    private String getPostPlotTitle(WebPlot plot) {
        String title = getRequest().getTitle();
        WebPlotRequest.TitleOptions titleOps= getRequest().getTitleOptions();
        if (titleOps== WebPlotRequest.TitleOptions.FILE_NAME) {
            PlotState state= plot.getPlotState();
            title= getFileName(state.getWebPlotRequest(state.firstBand()));
        }
        else if (StringUtils.isEmpty(title) ||
                titleOps== WebPlotRequest.TitleOptions.PLOT_DESC ||
                titleOps== WebPlotRequest.TitleOptions.PLOT_DESC_PLUS ||
                titleOps== WebPlotRequest.TitleOptions.PLOT_DESC_PLUS_DATE ) {
            title = plot.getPlotDesc();
        }
        return title;

    }

    private static String getFileName(WebPlotRequest r) {
        String retval= "";
        RequestType rt= r.getRequestType();
        String workStr= null;
        if (rt== RequestType.FILE || rt==RequestType.TRY_FILE_THEN_URL) {
            workStr= r.getFileName();
        }
        else if (r.getRequestType()== RequestType.URL) {
            workStr= r.getURL();
        }
        if (workStr!=null) {
            String fName= StringUtils.stripFilePath(workStr);
            retval= StringUtils.getFileBase(fName);
        }
        return retval;
    }

    private void killAfterSuccess(WebPlot plot) {
        VisTask.getInstance().deletePlot(plot);
    }


    private void showFailure(WebPlotResult result) {
        String bMsg = result.getBriefFailReason();
        String uMsg = result.getUserFailReason();
        String dMsg = result.getDetailFailReason();
        String title = _request1.getTitle() == null ? "" : _request1.getTitle() + ": ";
        _mpw.processError(null, bMsg,
                          title + " Plot Failed- " + uMsg,dMsg, null);
        if (_notify != null) _notify.onFailure(null);
    }


    /**
     * change the default cancel behavior so server can clean up
     */
    public void cancel() {
        _continueOnSuccess = false;

        if (_plotCtxStr != null) {
            VisTask.getInstance().deletePlot(_plotCtxStr);
        }
    }


    private void addAttributes(WebPlot plot) {
        WebPlotRequest req = getRequest();
        Circle c = req.getRequestArea();
        if (c != null && c.getCenter() != null) {
            ActiveTarget.PosEntry entry = new ActiveTarget.PosEntry(c.getCenter(), true);
            plot.setAttribute(WebPlot.FIXED_TARGET, entry);
            plot.setAttribute(WebPlot.REQUESTED_SIZE, c.getRadius());  // says radius but really siae
        }
        if (req.getUniqueKey() != null) {
            plot.setAttribute(WebPlot.UNIQUE_KEY, req.getUniqueKey());
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
