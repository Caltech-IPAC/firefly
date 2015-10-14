/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.task;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Collections;
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
    private final Object _task;


    public PlotFileTaskHelper(WebPlotRequest request1,
                              WebPlotRequest request2,
                              WebPlotRequest request3,
                              boolean threeColor,
                              boolean removeOldPlot,
                              boolean addToHistory,
                              AsyncCallback<WebPlot> notify,
                              MiniPlotWidget mpw,
                              Object task) {
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
        if (e!=null && e.getCause() != null) {
            extra = e.getCause().toString();

        }
//        Window.alert("Plot Failed: Server Error: "+  extra);
        _mpw.processError(null, "Server Error", "Plot Failed: Server Error: " + extra, null);
        if (_notify != null) _notify.onFailure(null);
        WebEvent<List<WebPlot>> ev = new WebEvent<List<WebPlot>>(_task, Name.PLOT_REQUEST_COMPLETED,
                                                                 Collections.<WebPlot>emptyList());
        _mpw.getPlotView().fireEvent(ev);
    }


    public MiniPlotWidget getMiniPlotWidget() { return _mpw; }



    public WebPlotRequest getRequest() {
        WebPlotRequest req = null;
        if (_request1 != null) req = _request1;
        else if (_request2 != null) req = _request2;
        else if (_request3 != null) req = _request3;
        return req;
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



    public boolean isThreeColor() { return _threeColor; }


    public void handleSuccess(WebPlotResult result) {
        long start = System.currentTimeMillis();
        List<WebPlot> successList= new ArrayList<WebPlot>(10);
        WebPlotView pv= _mpw.getPlotView();
        try {
            pv.setContainsMultiImageFits(false);
            if (_removeOldPlot) {
                _mpw.setFlipBarVisible(false);
                pv.clearAllPlots();
            }

            if (result.isSuccess()) {
                WebPlot plot;
                WebPlot firstPlot = null;
                boolean maySetFrame = pv.size() == 0;
                CreatorResults cr= (CreatorResults)result.getResult(WebPlotResult.PLOT_CREATE);

                for (WebPlotInitializer wpInit : cr) {
                    plot = new WebPlot(wpInit);
                    if (getRequest().isMinimalReadout()) plot.setAttribute(WebPlot.MINIMAL_READOUT,true);
                    if (firstPlot == null) firstPlot = plot;
                    if (_continueOnSuccess) {
                        successList.add(plot);
                        if (_addToHistory) PlotRequestHistory.instance().add(getRequest());
                        addAttributes(plot);
                        pv.addPlot(plot, false);
                    } else {
                        killAfterSuccess(plot);
                    }
                }
                if (_continueOnSuccess) {
                    pv.setPrimaryPlot(firstPlot);
                    pv.setContainsMultiImageFits(_removeOldPlot && isMultiImageFits(cr));
                    pv.setContainsMultipleCubes(_removeOldPlot && isMultiCube(cr));
                    _mpw.postPlotTask(getPostPlotTitle(firstPlot), firstPlot, _notify);

                    if (maySetFrame) {
                        _mpw.setFlipBarVisible(cr.size() > 1);
                        pv.setPrimaryPlot(firstPlot);
                    }
                    _mpw.forcePlotPrefUpdate();
                }
            } else {
                showFailure(result);
            }
        } catch (Exception e) {
            _mpw.processError(null, e.getMessage(), "WebPlot exception: " + e, e);
            GWT.log("WebPlot exception: " + e, e);
        }
        WebEvent<List<WebPlot>> ev = new WebEvent<List<WebPlot>>(_task, Name.PLOT_REQUEST_COMPLETED, successList);
        pv.fireEvent(ev);
//        GWT.log("plot task time: " + (System.currentTimeMillis()-start));

    }


    private boolean isMultiImageFits(CreatorResults cr) {
        boolean retval= true;
        for (WebPlotInitializer wpInit : cr) {
            if (!wpInit.getPlotState().isMultiImageFile()) {
                retval= false;
                break;
            }
            if (!retval) break;
        }
        return retval;
    }



    private boolean isMultiCube(CreatorResults cr) {
        boolean retval= false;
        for (WebPlotInitializer wpInit : cr) {
            for(Band band : wpInit.getPlotState().getBands()) {
                if (wpInit.getPlotState().getCubeCnt(band)>1) {
                    retval= true;
                    break;
                }
            }
        }
        return retval;
    }



    private String getPostPlotTitle(WebPlot plot) {
        WebPlotRequest r= getRequest();
        String title = r.getTitle();
        WebPlotRequest.TitleOptions titleOps= getRequest().getTitleOptions();
        String preTitle= (r.getPreTitle()!=null) ? r.getPreTitle()+": ": "";
        if (titleOps== WebPlotRequest.TitleOptions.FILE_NAME) {
            title= computeFileNameBaseTitle(r,plot.getPlotState(), plot.getFirstBand(),preTitle);
        }
        else if (StringUtils.isEmpty(title) ||
                titleOps== WebPlotRequest.TitleOptions.PLOT_DESC ||
                titleOps== WebPlotRequest.TitleOptions.PLOT_DESC_PLUS ||
                titleOps== WebPlotRequest.TitleOptions.SERVICE_OBS_DATE ) {
            title = preTitle + plot.getPlotDesc();
        }

        String postTitle= (r.getPostTitle()!=null) ? ": "+r.getPostTitle() : "";
        title= title + postTitle;

        return title;

    }

    private static String computeFileNameBaseTitle(WebPlotRequest r,PlotState state, Band band, String preTitle) {
        String retval= "";
        RequestType rt= r.getRequestType();
        if (rt== RequestType.FILE || rt==RequestType.TRY_FILE_THEN_URL) {
            if (state.getUploadFileName(band)!=null) {
                retval= preTitle + computeTitleFromFile(state.getUploadFileName(band));
            }
            else {
                retval= preTitle + computeTitleFromFile(r.getFileName());
            }
        }
        else if (r.getRequestType()== RequestType.URL) {
            retval= computeTitleFromURL(r.getURL(),r,preTitle);
        }
        return retval;
    }


    private static String computeTitleFromURL(String urlStr, WebPlotRequest r, String preTitle) {
        String retval= "";
        int qIdx=urlStr.indexOf("?");
        if (!StringUtils.isEmpty(urlStr)) {
            if (qIdx>-1 && urlStr.length()>qIdx+1) {
                String prepend= r.getTitleFilenameModePfx()==null ? "from " : r.getTitleFilenameModePfx()+ " ";
                prepend+= preTitle;
                try {
                    String workStr= urlStr.substring(qIdx+1);
                    int fLoc= workStr.toLowerCase().indexOf(".fit");
                    if (fLoc>-1) {
                        workStr= workStr.substring(0,fLoc);
                        if (workStr.lastIndexOf('=')>0)  {
                            workStr= workStr.substring(workStr.lastIndexOf("="));
                            retval= prepend +StringUtils.stripFilePath(workStr);
                        }
                        else {
                            retval= prepend +StringUtils.stripFilePath(workStr);
                        }
                    }
                    else {
                        fLoc= urlStr.toLowerCase().indexOf(".fit");
                        if (fLoc>-1) {
                            workStr= urlStr.substring(0,fLoc);
                            retval= prepend +StringUtils.stripFilePath(workStr);
                        }
                        else {
                            retval= prepend+ workStr;
                        }


                    }
                } catch (Exception e) {
                    retval= prepend+ computeTitleFromFile(urlStr);
                }
            }
            else {
                retval= preTitle+computeTitleFromFile(urlStr);
            }
        }
        return retval;
    }

    private static String computeTitleFromFile(String fileStr) {
        String retval= "";
        if (!StringUtils.isEmpty(fileStr)) {
            String fName= StringUtils.stripFilePath(fileStr);
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
    }


    private void addAttributes(WebPlot plot) {
        WebPlotRequest req = getRequest();
        Circle c = req.getRequestArea();
        ActiveTarget.PosEntry posEntry= ActiveTarget.getInstance().getActive();
        if (req.getOverlayPosition()!=null) {
            ActiveTarget.PosEntry entry = new ActiveTarget.PosEntry(req.getOverlayPosition(),true);
            plot.setAttribute(WebPlot.FIXED_TARGET, entry);
            if (posEntry==null) {
                ActiveTarget.getInstance().setActive(null,req.getOverlayPosition(),null,true);
            }
            if (c != null) {
                plot.setAttribute(WebPlot.REQUESTED_SIZE, c.getRadius());  // says radius but really size
            }

        }
        else if (c != null) {
            ActiveTarget.PosEntry entry = new ActiveTarget.PosEntry(c.getCenter(), true);
            plot.setAttribute(WebPlot.FIXED_TARGET, entry);
            if (c.getCenter() != null) {
                plot.setAttribute(WebPlot.REQUESTED_SIZE, c.getRadius());  // says radius but really size
                if (posEntry==null) {
                    ActiveTarget.getInstance().setActive(null,c.getCenter(),null,true);
                }
            }
        }
        else if (posEntry!=null) {
            plot.setAttribute(WebPlot.FIXED_TARGET, posEntry);
        }
        else {
            int dw = plot.getImageDataWidth();
            int dh = plot.getImageDataHeight();
            ImagePt ip= new ImagePt(dw/2,dh/2);
            WorldPt wp= plot.getWorldCoords(ip);
            if (wp!=null) {
                ActiveTarget.PosEntry entry = new ActiveTarget.PosEntry(wp, true);
                plot.setAttribute(WebPlot.FIXED_TARGET, entry);
                if (posEntry==null) { // if there is no active, then set this best guess
                    ActiveTarget.getInstance().setActive(null,wp,null,true);
                }
            }

        }
        if (req.getUniqueKey() != null) {
            plot.setAttribute(WebPlot.UNIQUE_KEY, req.getUniqueKey());
        }

    }


}

