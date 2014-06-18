package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 2/20/14
 * Time: 9:23 AM
 */


import edu.caltech.ipac.firefly.server.ServerContext;
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
        String sID= ServerContext.getRequestOwner().getSessionId();
        EventMatchCriteria criteria= EventMatchCriteria.makeSessionCriteria(sID,winId);
        ServerEventManager.addEventQueueForClient(cometResponse, criteria);
    }




    @Override
    public void cometTerminated(CometServletResponse cometResponse, boolean serverInitiated) {

//        Logger.briefInfo("cometTerminated, session: "+ServerContext.getRequestOwner().getSessionId());
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
