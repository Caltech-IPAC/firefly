package edu.caltech.ipac.firefly.visualize.task;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
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
                CreatorResults cr= (CreatorResults)result.getResult(WebPlotResult.PLOT_CREATE);
                WebPlotView pv= _mpw.getPlotView();

                if (pv.isContainsMultiImageFits() &&_cropMultiAll) {

                    pv.clearAllPlots();
                    List<WebPlot> cropPlotList= new ArrayList<WebPlot>(cr.size());
                    for(WebPlotInitializer wpInit : cr) {
                        WebPlot cropPlot= new WebPlot(wpInit);
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

                    WebPlot cropPlot= new WebPlot(wpInit);
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
        pserv.crop(_oldPrimaryPlot.getPlotState(),_pt1,_pt2,_cropMultiAll,passAlong);
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
