package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 2/20/14
 * Time: 9:23 AM
 */


import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.event.Name;
import net.zschech.gwt.comet.server.CometServlet;
import net.zschech.gwt.comet.server.CometServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Trey Roby
 */
public class EventSenderServlet extends CometServlet {

    static int initCnt=  0;
    static int tmpCnt= 0;
    private static boolean sessionActive;
    private static boolean oneTest= false;

//    private Map<CometSession,EventSender> activeSenders= new HashMap<CometSession, EventSender>(13);



    @Override
    protected void doComet(CometServletResponse cometResponse) throws ServletException, IOException {
//        activeSenders.put(cometResponse.getSession(),eventSender);
        if (!oneTest) {
            oneTest= true;
            String sID= ServerContext.getRequestOwner().getSessionId();
            EventTarget tgt= new EventTarget.Session(sID);
            EventSender.addEventSender(cometResponse, tgt);
//            doCometTEST(sID);
            new CTest(sID);
        }

    }




    @Override
    public void cometTerminated(CometServletResponse cometResponse, boolean serverInitiated) {

        Logger.briefInfo("cometTerminated, session: "+ServerContext.getRequestOwner().getSessionId());
//        CometServletResponseImpl csrI= (CometServletResponseImpl)cometResponse;
//        CometSession session= csrI.getSessionImpl();
//
////        EventSender eventSender= activeSenders.get(cometResponse.getSession());
//        EventSender eventSender= activeSenders.get(session);
//        if (eventSender!=null) {
//            eventSender.shutdown();
////            activeSenders.remove(session);
//        }
    }

    public static class CTest implements Runnable {

        private String sID;

        public CTest(String sID) {
            this.sID = sID;
            Thread thread= new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }

        public void run() {
            doCometTEST();
        }
        protected void doCometTEST() {
            initCnt++;
            boolean keepSending= true;
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
            }
            try {
                while(keepSending) {
    //            String s= ServerContext.getRequestOwner().getSessionId();
                    String mess= "message #"+initCnt+"."+tmpCnt+"."+sID;
                    ServerSentEvent ev= new ServerSentEvent(Name.APP_ONLOAD, new EventTarget.Session(sID), new EventData(mess));
                    ServerEventManager.send(ev);
                    tmpCnt++;
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
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
