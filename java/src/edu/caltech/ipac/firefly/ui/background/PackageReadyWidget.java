package edu.caltech.ipac.firefly.ui.background;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.core.background.PackageProgress;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.PropFile;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.util.StringUtils;
/**
 * User: roby
 * Date: Dec 17, 2009
 * Time: 10:20:41 AM
 */


/**
 * @author Trey Roby
 */
public class PackageReadyWidget extends Composite {

    public enum FileDownloadStatus {NONE, WORKING, DONE}

    interface PFile extends PropFile { @Source("PackageReadyWidget.prop") TextResource get(); }
    private static WebClassProperties _prop= new WebClassProperties(PackageReadyWidget.class, (PFile) GWT.create(PFile.class));

    private static final String DOWNLOAD_NOW_TXT= _prop.getName("downloadNow");
    private static final String DOWNLOAD_PART_TXT= _prop.getName("downloadNow.part");
    private static final String DOWNLOAD_NOW_TIP= _prop.getTip("downloadNow");
    private static final String ZIP_ROOT_TXT= _prop.getName("zip.root");

    private static final String PROCESSING_ICON= "images/blue-downloading-15x15.png";
    private final Image _icon= new Image(PROCESSING_ICON);

    private final MonitorItem _monItem;
    private final int _idx;

    PackageReadyWidget(MonitorItem monItem, int idx, boolean markAlreadyActivated) {
        _monItem= monItem;
        _idx= idx;
        BackgroundStatus bgStat= monItem.getStatus();
        PackageProgress bundle= bgStat.getPartProgress(idx);
        FlexTable fp= new FlexTable();
        HTMLTable.CellFormatter formatter= fp.getCellFormatter();

        String desc= bgStat.getPackageCount()==1 ? "" :  ZIP_ROOT_TXT + (idx+1);
        fp.setWidget(0,0,makeDownloadNowButton(bundle.getURL(),desc,idx));
        formatter.setWidth(0,0,"100px");

        fp.setWidget(0,4,_icon);
        setShowRetrivedIcon(markAlreadyActivated ? FileDownloadStatus.DONE : FileDownloadStatus.NONE);
        formatter.setWidth(0,4,"20px");
        formatter.setHorizontalAlignment(0,4, HasHorizontalAlignment.ALIGN_RIGHT);
        Label dSize= new Label(StringUtils.getSizeAsString(bundle.getFinalCompressedBytes(),true));
        fp.setWidget(0,2,dSize);
        formatter.setWidth(0,2,"65px");
        formatter.setHorizontalAlignment(0,2, HasHorizontalAlignment.ALIGN_RIGHT);
        initWidget(fp);
    }


    private Widget makeDownloadNowButton(String url, String txt, int idx) {
        String btxt= StringUtils.isEmpty(txt) ? DOWNLOAD_NOW_TXT  : DOWNLOAD_PART_TXT  +  " " + txt;

        return GwtUtil.makeLinkButton(btxt, DOWNLOAD_NOW_TIP,
                                      new DownloadHandler(url,idx));
    }


    private void getZipFile(String url, int idx) {
//        disableIndicators();
        getZipFile(url);
        String sAry[]= url.split("\\?");
        if (sAry.length==2)  new DownloadProgress(sAry[1]);
        else assert false; //sAry.length not equals to 2
    }

    public static void getZipFile(String url) {
        Frame f= Application.getInstance().getNullFrame();
        f.setUrl(url);
    }

    private void setShowRetrivedIcon(FileDownloadStatus status) {
        switch (status) {
            case DONE:
                _icon.setVisible(true);
                _icon.setUrl(UIBackgroundUtil.RETRIEVED_ICON);
                break;
            case NONE:
                _icon.setVisible(false);
                break;
            case WORKING:
                _icon.setVisible(true);
                _icon.setUrl(PROCESSING_ICON);
                break;
            default: assert false;
                break;
        }
    }


// =====================================================================
// -------------------- Inner Classes ----------------------------------
// =====================================================================

    private class DownloadHandler implements ClickHandler {
        private String _url;
        private int    _idx;
        public DownloadHandler (String url, int idx) {
            _idx= idx;
            _url= url;
        }

        public void onClick(ClickEvent ev) {
            getZipFile(_url,_idx);
        }
    }


    /**
     * @author Trey Roby
     */
    private class DownloadProgress extends Timer {
        private static final int DELAY= 1100; // every 1.1 seconds
        private String _fileKey;

        public DownloadProgress(String fileKey) {
            _fileKey= fileKey;
            run();
        }

        private void unknown(boolean fail) {
            setShowRetrivedIcon(FileDownloadStatus.NONE);
            if (fail) {
                fail();
            }
            else {
                this.schedule(DELAY);
            }
        }

        private void fail() {
            setShowRetrivedIcon(FileDownloadStatus.NONE);
        }

        private void downloading() {
            setShowRetrivedIcon(FileDownloadStatus.WORKING);
            this.schedule(DELAY);
        }

        private void done() {
            setShowRetrivedIcon(FileDownloadStatus.DONE);
            _monItem.setActivated(_idx,true);
//            _manager.adjustHidingOnFinishedDownloads();
        }

        public void run() {
            check();
        }

        public void check() {
            SearchServicesAsync dserv= SearchServices.App.getInstance();
            dserv.getDownloadProgress(_fileKey, new AsyncCallback<SearchServices.DownloadProgress>() {
                public void onFailure(Throwable caught) {unknown(true);}
                public void onSuccess(SearchServices.DownloadProgress progress) {
                    switch (progress) {
                        case DONE:
                            done();
                            break;
                        case STARTING:
                            unknown(false);
                            break;
                        case UNKNOWN:
                            unknown(false);
                            break;
                        case WORKING:
                            downloading();
                            break;
                        case FAIL:
                            fail();
                            break;
                    }
                }
            });
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
