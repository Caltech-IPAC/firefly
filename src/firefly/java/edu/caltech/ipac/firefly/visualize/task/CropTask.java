/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.task;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.DataEntry.WebPlotResultAry;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.OverlayPlotView;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.ImagePt;

import java.util.ArrayList;
import java.util.List;
/**
 * User: roby
 * Date: Apr 28, 2009
 * Time: 4:49:37 PM
 */


/**
 * @author Trey Roby
*/
public class CropTask extends ServerTask<WebPlotResult> {

    public final static String CROPPED= "Cropped: ";
    private final List<WebPlot> _oldPlotList;
    private final WebPlot _oldPrimaryPlot;
    private final int _currentIdx;
    private final MiniPlotWidget _mpw;
    private final ImagePt _pt1;
    private final ImagePt _pt2;
    private final String _newTitle;
    private final boolean _cropMultiAll;

    public static void crop(MiniPlotWidget mpw,
                            String message,
                            String newTitle,
                            ImagePt pt1,
                            ImagePt pt2,
                            boolean cropMultiAll) {
        new CropTask(mpw,message, newTitle, pt1, pt2, cropMultiAll).start();
    }

    CropTask(MiniPlotWidget mpw,
             String message,
             String newTitle,
             ImagePt pt1,
             ImagePt pt2,
             boolean cropMultiAll) {
        super(mpw.getPanelToMask(), message, true);
        super.setMaskingDelaySec(1);
        _oldPrimaryPlot = mpw.getCurrentPlot();
        _currentIdx= mpw.getPlotView().indexOf(_oldPrimaryPlot);
        _oldPlotList= cropMultiAll ? mpw.getPlotView().getPlotList() : null;
        _mpw= mpw;
        _pt1= pt1;
        _pt2= pt2;
        _cropMultiAll= cropMultiAll;
        _newTitle= newTitle;
    }

    public void onFailure(Throwable e) {
        String extra= "";
        if (e.getCause()!=null) {
            extra= e.getCause().toString();

        }
        Window.alert("Crop Failed: Server Error: " + extra);
    }

    @Override
    public void onSuccess(WebPlotResult result) {
        _mpw.hideMouseReadout();
        try {
            if (result.isSuccess()) {

                WebPlotResultAry resultEntry= (WebPlotResultAry)result.getResult(WebPlotResult.RESULT_ARY);
                WebPlotResult resultAry[]= resultEntry.getArray();
                CreatorResults cr;
                List<OverlayPlotView> overlayPlotViews= _mpw.getPlotView().getOverlayPlotViewList();
                if (resultAry.length>1 && resultAry.length-1==overlayPlotViews.size()) {
                    for(int i=1; (i<resultAry.length); i++) {
                        if (resultAry[i].isSuccess()) {
                            cr= (CreatorResults)resultAry[i].getResult(WebPlotResult.PLOT_CREATE);
                            WebPlotInitializer wpInit= cr.getInitializers()[0];
                            WebPlot overlayPlot= new WebPlot(wpInit,true);
                            OverlayPlotView opv= overlayPlotViews.get(i-1);
                            opv.setMaskPlot(overlayPlot);
                        }
                    }
                }


                WebPlotView pv= _mpw.getPlotView();

                cr= (CreatorResults)resultAry[0].getResult(WebPlotResult.PLOT_CREATE);
                if (pv.isContainsMultiImageFits() &&_cropMultiAll) {

                    pv.clearAllPlots();
                    List<WebPlot> cropPlotList= new ArrayList<WebPlot>(cr.size());
                    for(WebPlotInitializer wpInit : cr) {
                        WebPlot cropPlot= new WebPlot(wpInit,false);
                        cropPlotList.add(cropPlot);
                    }
                    if (cropPlotList.size()==_oldPlotList.size()) {
                        for(int i=0; (i<_oldPlotList.size()); i++) {
                            WebPlot p= cropPlotList.get(i);
                            WebPlot oldP= _oldPlotList.get(i);
                            TaskUtils.copyImportantAttributes(oldP, p);
                            pv.addPlot(p,false);
                            _mpw.postPlotTask(_newTitle, p, null);
                        }
                        pv.setPrimaryPlot(cropPlotList.get(_currentIdx));
                    }
                    else {
                        WebAssert.tst(false, "crop plot list is a different size then the original list");
                        Window.alert("Plot Failed: "+ result.getUserFailReason());
                    }

                }
                else {
                    WebPlotInitializer wpInit= cr.getInitializers()[0];

                    WebPlot cropPlot= new WebPlot(wpInit,false);
                    if (_oldPrimaryPlot.getPlotState().isMultiImageFile(cropPlot.getPlotState().firstBand())) {
                        if (!StringUtils.isEmpty(_oldPrimaryPlot.getPlotDesc())) {
                            cropPlot.setPlotDesc("Crop: "+_oldPrimaryPlot.getPlotDesc());
                        }
                    }
                    TaskUtils.copyImportantAttributes(_oldPrimaryPlot, cropPlot);


                    int idx= pv.indexOf(pv.getPrimaryPlot());
                    _mpw.getOps().removeCurrentPlot();
                    _mpw.getPlotView().addPlot(cropPlot,idx,true);

                    _mpw.postPlotTask(_newTitle, cropPlot, null);
                }
                _mpw.forcePlotPrefUpdate();
                pv.fireEvent(new WebEvent<WebPlotView>(this,Name.CROP,pv));

            }
            else {
                Window.alert("Plot Failed: "+ result.getUserFailReason());
            }
        } catch (Exception e) {
            Window.alert("exception: "+ e.toString());
        }
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        _mpw.prePlotTask();
        PlotServiceAsync pserv= PlotService.App.getInstance();
        pserv.crop(getPlotStateAry(_oldPrimaryPlot, _mpw), _pt1, _pt2, _cropMultiAll, passAlong);
    }

    public static PlotState[] getPlotStateAry(WebPlot plot,MiniPlotWidget mpw) {
        List<PlotState> stateList= new ArrayList<PlotState>();
        stateList.add(plot.getPlotState());

        List<WebPlot> overlayPlots= mpw.getPlotView().getOverlayPlotList();
        if (overlayPlots!=null && overlayPlots.size()>0) {
            for (WebPlot p : overlayPlots) {
                stateList.add(p.getPlotState());
            }
        }
        return stateList.toArray(new PlotState[stateList.size()]);
    }
}

