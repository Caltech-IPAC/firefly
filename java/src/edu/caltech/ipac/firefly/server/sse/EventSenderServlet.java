/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 2/20/14
 * Time: 9:23 AM
 */


import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import net.zschech.gwt.comet.server.CometServlet;
import net.zschech.gwt.comet.server.CometServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * @author Trey Roby
 */
public class EventSenderServlet extends CometServlet {

    @Override
    protected void doComet(CometServletResponse cometResponse) throws ServletException, IOException {
        String winId= cometResponse.getRequest().getParameter("winId");
        String sID= ServerContext.getRequestOwner().getUserKey();
        EventMatchCriteria criteria= EventMatchCriteria.makeSessionCriteria(sID,winId);
        ServerEventManager.addEventQueueForClient(cometResponse, criteria);
        Logger.briefInfo("doComet, request owner: " + ServerContext.getRequestOwner().getUserKey()+ ", winId= "+winId);
    }


    @Override
    public void cometTerminated(CometServletResponse cometResponse, boolean serverInitiated) {
        String winId= cometResponse.getRequest().getParameter("winId");
        Logger.briefInfo("cometTerminated, request owner: "+ServerContext.getRequestOwner().getUserKey()+ ", winId= "+winId);
    }


//    static int initCnt=  0;
//    static int messNum = 0;
//    private static boolean sessionActive;
//    private static boolean oneTest= false;
//    private Map<CometSession,EventSender> activeSenders= new HashMap<CometSession, EventSender>(13);
//static List<String> testerList= new ArrayList<String>(10);

//    public static class CTest implements Runnable {
//
//        private String sID;
//        private String winId;
//        private static  int createCnt= 0;
//        private int id= createCnt++;
//
//
//        public CTest(String sID, String winId) {
//            this.sID = sID;
//            this.winId= winId;
//
//            Thread thread= new Thread(this);
//            thread.setDaemon(true);
//            thread.start();
//        }
//
//        public void run() {
//            doCometTEST();
//        }
//        protected void doCometTEST() {
//            initCnt++;
//            boolean keepSending= true;
//            try {
//                TimeUnit.SECONDS.sleep(10);
//            } catch (InterruptedException e) {
//            }
//            try {
//                while(keepSending) {
//    //            String s= ServerContext.getRequestOwner().getSessionId();
//                    String mess= "message #"+initCnt+",mess num="+ messNum +", tester id="+id;
//                    ServerSentEvent ev= new ServerSentEvent(Name.APP_ONLOAD, new EventTarget.Session(sID,winId), new EventData(mess));
//                    ServerEventManager.fireEvent(ev);
//                    messNum++;
//                    try {
//                        TimeUnit.SECONDS.sleep(10);
//                    } catch (InterruptedException e) {
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
//    }

}

