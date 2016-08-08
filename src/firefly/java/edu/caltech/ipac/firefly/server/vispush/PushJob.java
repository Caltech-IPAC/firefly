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


    /**
     * Get the active count. If 0 keep trying for try time milliseconds.
     * @param channel the channel
     * @param tryTime  tryTime is the about of time that is will keep checking for. It
     *                 polls every 200 ms until success or until tryTime runs out.
     *                 This way it will return quite fast. tryTime is really the time until it gives up.
     *                 The polling is only looking at an array that is updated in another thread.
     * @return count
     */
    public static int getBrowserClientActiveCount(String channel, int tryTime) {
        if (channel==null) channel= ServerContext.getRequestOwner().getEventChannel();
        int cnt=  ServerEventManager.getActiveQueueChannelCnt(channel);
        long endTry= System.currentTimeMillis()+tryTime;

        try {
            while (tryTime>0 && cnt==0 && System.currentTimeMillis()<endTry) {
                Thread.sleep(200);
                cnt=  ServerEventManager.getActiveQueueChannelCnt(channel);
            }
        } catch (InterruptedException e) {
        }

        return cnt;
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
