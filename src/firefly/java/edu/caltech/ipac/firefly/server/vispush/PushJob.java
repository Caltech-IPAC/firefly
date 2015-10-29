/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.vispush;
/**
 * User: roby
 * Date: 1/27/15
 * Time: 1:04 PM
 */


import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.RangeValues;

/**
 * @author Trey Roby
 */
public class PushJob {


    private static void fireEvent(String data, Name evName) {
        ServerEvent sevt = new ServerEvent(evName,
                new ServerEvent.EventTarget(ServerEvent.Scope.CHANNEL),
                ServerEvent.DataType.STRING, data,
                ServerContext.getRequestOwner().getEventConnID());
        ServerEventManager.fireEvent(sevt);

    }

    public static boolean pushFits(WebPlotRequest wpr) {
        fireEvent(wpr.toString(), Name.PUSH_WEB_PLOT_REQUEST);
        return true;
    }

    public static boolean pushExtension(String sreqId,
                                        String plotId,
                                        String extType,
                                        String title,
                                        String image,
                                        String toolTip) {
        ServerRequest r = new ServerRequest(sreqId);
        r.setParam(ServerParams.EXT_TYPE, extType);
        r.setParam(ServerParams.TITLE, title);
        r.setParam(ServerParams.PLOT_ID, plotId);
        if (image != null) r.setParam(ServerParams.IMAGE, image);
        if (toolTip != null) r.setParam(ServerParams.IMAGE, toolTip);
        fireEvent(r.toString(), Name.PUSH_FITS_COMMAND_EXT);
        return true;
    }

    public static boolean pushPan(String plotId,
                                  String xStr,
                                  String yStr) {
        ServerRequest r = new ServerRequest(plotId);
        r.setParam(ServerParams.SCROLL_X, xStr);
        r.setParam(ServerParams.SCROLL_Y, yStr);
        fireEvent(r.toString(), Name.PUSH_PAN);
        return true;
    }

    public static boolean pushZoom(String plotId, String zFactStr) {
        ServerRequest r = new ServerRequest(plotId);
        r.setParam(ServerParams.ZOOM_FACTOR, zFactStr);
        fireEvent(r.toString(), Name.PUSH_ZOOM);
        return true;
    }

    public static boolean pushRangeValues(String plotId, RangeValues rv) {
        ServerRequest r = new ServerRequest(plotId);
        r.setParam(ServerParams.RANGE_VALUES, rv.toString());
        fireEvent(r.toString(), Name.PUSH_RANGE_VALUES);
        return true;
    }


    public static boolean pushTable(ServerRequest req) {
        fireEvent(req.toString(), Name.PUSH_TABLE_FILE);
        return true;
    }

    public static boolean pushXYPlot(ServerRequest r) {
        fireEvent(r.toString(), Name.PUSH_XYPLOT_FILE);
        return true;
    }



    //================================
    //========== Region Stuff
    //================================

    public static boolean pushRegionFile(String fileName, String id, String plotIdAry) {
        ServerRequest r = new ServerRequest(id);
        r.setParam(ServerParams.FILE, fileName);
        if (plotIdAry!=null) r.setParam(ServerParams.PLOT_ID, plotIdAry);
        fireEvent(r.toString(), Name.PUSH_REGION_FILE);
        return true;
    }

    public static boolean pushRemoveRegionFile(String id, String plotIdAry) {
        ServerRequest r = new ServerRequest(id);
        if (plotIdAry!=null) r.setParam(ServerParams.PLOT_ID, plotIdAry);
        fireEvent(r.toString(), Name.PUSH_REMOVE_REGION_FILE);
        return true;
    }

    public static boolean pushRemoveRegionData(String id, String data) {
        ServerRequest r = new ServerRequest(id);
        r.setParam(ServerParams.DS9_REGION_DATA, data);
        fireEvent(r.toString(), Name.REMOVE_REGION_DATA);
        return true;
    }


    public static boolean pushRegionData(String title, String id, String data, String plotIdAry) {
        ServerRequest r = new ServerRequest(id);
        r.setParam(ServerParams.TITLE, title);
        r.setParam(ServerParams.DS9_REGION_DATA, data);
        if (plotIdAry!=null) r.setParam(ServerParams.PLOT_ID, plotIdAry);
        fireEvent(r.toString(), Name.PUSH_REGION_DATA);
        return true;
    }

    public static boolean pushAddMask(String maskId,
                                      int bitNumber,
                                      int imageNumber,
                                      String color,
                                      String bitDesc,
                                      String fileKey,
                                      String plotIdStr) {
        ServerRequest r = new ServerRequest(maskId);
        r.setParam(ServerParams.BIT_NUMBER,bitNumber+"");
        r.setParam(ServerParams.IMAGE_NUMBER,imageNumber+"");
        r.setParam(ServerParams.COLOR,color);
        r.setParam(ServerParams.BIT_DESC,bitDesc);
        r.setParam(ServerParams.FILE,fileKey);
        r.setParam(ServerParams.PLOT_ID,plotIdStr);
        fireEvent(r.toString(), Name.PUSH_ADD_MASK);
        return true;
    }


    public static boolean pushRemoveMask(String maskId) {
        ServerRequest r = new ServerRequest(maskId);
        fireEvent(r.toString(), Name.PUSH_REMOVE_MASK);
        return true;
    }

    public static boolean isBrowserClientActive(String ipString) {
        String channel= ServerContext.getRequestOwner().getEventChannel();
        return ServerEventManager.getActiveQueueChannelCnt(channel)>1;
    }

    //================================
    //========== End Region Stuff
    //================================

//    public static boolean isURLAcitve(String urlStr) {
//        fireEvent(urlStr, Name.REPORT_ALIVE);
//
//        WaitForResponse worker = new WaitForResponse(channel, urlStr);
//        Thread thread = new Thread(worker, "Response Wait");
//
//        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
//            public void uncaughtException(Thread t, Throwable e) {
//                // do something
//            }
//        });
//
//        thread.setDaemon(true);
//        thread.start();
//
//        boolean found;
//        try {
//            thread.join(4000);
//            worker.quit();
//            found = worker.isFound();
//        } catch (InterruptedException e) {
//            found = false;
//        }
//        return found;
//    }



//    private static class WaitForResponse implements Runnable {
//
//        private volatile Thread thread;
//        private volatile ServerEventQueue queue;
//        private volatile boolean threadIsDone = false;
//        private volatile String channel;
//        private volatile String url;
//        private volatile boolean found= false;
//
//
//        public WaitForResponse(String channel, String url) {
//            this.channel = channel;
//            this.url = url;
//        }
//
//
//        public void run() {
//            thread = Thread.currentThread();
//            queue = new ServerEventQueue("NONE", channel, new ServerEventQueue.EventConnector() {
//                @Override
//                public void send(String message) throws Exception {
//                    ServerEvent ev= ServerEventQueue.parseJsonEvent(message);
//                    if (ev.getName().equals(Name.RESPONDED_ALIVE)) {
//                        if (ev.getData().equals(WaitForResponse.this.url)) {
//                            found= true;
//                            thread.interrupt();
//                        }
//                    }
//                }
//
//                @Override
//                public boolean isOpen() { return true; }
//
//                @Override
//                public void close() { }
//            });
//            ServerEventManager.addEventQueue(queue);
//
//            try {
//                TimeUnit.SECONDS.sleep(300);
//            } catch (InterruptedException e) {
//                // do nothing
//            }
//            ServerEventManager.removeEventQueue(queue);
//        }
//
//        public boolean isFound() {
//            return found;
//        }
//
//        public void quit() {
//            threadIsDone = true;
//            this.thread.interrupt();
//        }


}
