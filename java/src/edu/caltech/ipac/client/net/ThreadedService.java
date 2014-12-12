package edu.caltech.ipac.client.net;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;

import javax.swing.Icon;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.EventQueue;
/**
 * @author Trey Roby
 * @version $Id: ThreadedService.java,v 1.10 2012/12/11 21:00:48 roby Exp $
 */
public abstract class ThreadedService implements Runnable, DownloadListener {
    /**
     * a ThreadedSevice that appears to run in * the foreground (blocking
     * the user from doing anything else) and provides the user with
     * feedback and a cancel button
     */
    public  static final int STANDARD          = 1000;

    /**
     * run in the background (doesnot block the user) and provides the user
     * with feedback and a cancel button.
     */
    public  static final int BACKGROUND        = 1002;



    private static final boolean _showException=
            AppProperties.getBooleanPreference("ThreadedService.showException",
                                                       false);

    private final static String OP_DESC=        "unknown service";

    /**
     * if an exception happend durring execute() it is caught and store here.
     */
    private Exception     _exception         = null;
    /**
     * if true, the service has completed either sucessfully or unsucessfully
     */
    private boolean       _completed         = false;
    /**
     * if true, there has been and interrupt request given to the service
     */
    private boolean       _interrupted       = false;
    /**
     * If the service is created from the thread- EventQueue.isDispatchThread()
     * this this varible holds the reference to the thread where the network
     * work is done. If the service is created from a background thread then
     * no extra thread is created and _thread is null.
     */
    private final Thread                  _thread;
    /**
     * STANDARD or BACKGROUND
     */
    private final int                     _type;
 //   private final Requester               _requester;
    /**
     * the Window is use for creating the GUI for a foreground service.  If
     * it is null it indicates that the user wants to do the service silently,
     * that is no indication to the user that the network request is going on.
     */
    private final Component               _component;
    private final ThreadedServiceListener _listener; //listener for BACKGROUND


    private boolean _showUserErrors          = true;
    private boolean _showNetworkDownError    = true;
    private boolean _proceedIfNetworkDown    = false;
    private boolean _fireListenersInAWTThread= false;
    private String  _operationDesc           = OP_DESC;
    private String  _processingDesc          = "";
    private Icon    _overrideSmallIcon       = null;
    private Icon    _overrideNormalIcon      = null;

    private long    _pBarMin=0;
    private long    _pBarMax=0;
    private long    _pBarCurrent=-1;
    private String  _pBarDesc="";

    /**
     * run the request in the thread of the caller.  This means that
     * the caller has already spun off a separate thread so we do not
     * make a new one.
     * This boolean is set if the user had request STANDARD and he is
     * calling from a thread other than the dispatch thread.
     * this is look a "sub-mode" of STANDARD
     */
    private boolean _callerThreaded          =false;

  //========================================================================
  //---------------------- Public Construtors ----------------------------------
  //========================================================================

    /**
     * Construtor for a threaded service that you wish to run silent in
     * without the user knowing. (in other words- run with no feedback).
     */
    public ThreadedService()         { this(STANDARD, null, null); }
    /**
     * Construtor for a threaded service.
     * @param c if the Component is not null then
     *               provide feedback and a cancel button to the user.
     *               If the window is null this threaded service will
     *               run silent
     */
    public ThreadedService(Component c) { this(STANDARD, null, c);    }



    /**
     * Construtor for a threaded service.
     *
     * @param type The type of service: <ul>
     *                 <li>STANDARD- a ThreadedSevice that appears to run in
     *                           the foreground (blocking the user from doing
     *                           anything else) and provides the user with
     *                           feedback and a cancel button 
     *                 <li> BACKGROUND- run in the background (does
     *                           not block the user) and provides the user 
     *                           with feedback and a cancel button.
     *                  </ul>
     * @param listener  a listener to know the status of the service.
     *                 This is only used when the mode is BACKGROUND since
     *                 the use is doing some asyncronous type of request and his
     *                 only notification is through the listeners.
     * @param c if the Component is not null then
     *          provide feedback and a cancel button to the user.
     *          If the window is null this threaded service will
     *          run slient. Silent is only support in STANDARD mode.
     *          Background requires a Window.
     */
    public ThreadedService(int                     type, 
                           ThreadedServiceListener listener,
                           Component               c) {
        Assert.argTst(type==STANDARD ||
                     (type==BACKGROUND && c!=null),
             "Type must be: STANDARD, or BACKGROUND\n"+
             "if type is BACKGROUND then c must not be null.");
        Assert.argTst((type==BACKGROUND || listener==null),
                      "a listener should only be passed if the " +
                      "type is BACKGROUND, the listener is not used with "+
                      "STANDARD");
        if (type==STANDARD && !EventQueue.isDispatchThread()) {
            _callerThreaded= true;
            _thread= null;
        }
        else {
            _thread= new Thread(this, "ThreadedService");
        }
        _type     = type;
        _component= c;
        _listener = listener;
        //_requester= new Requester(this,c);
    }

  //========================================================================
  //---------------------- Public final execute Methods --------------------
  //========================================================================

    /**
     * Start executing the service.
     * Generally we will run in a separate thread.
     * If the user has already spun off anther thread and we are in STANDARD
     * mode then we do the network request in that thread.
     * @throws FailedRequestException is something goes wrong
     */
    public final void execute() throws FailedRequestException {
        execute(false);
    }

    /**
     * start executing the service.  
     * Generally we will run in a separate thread.
     * If the user has already spun off anther thread and we are in STANDARD
     * mode then we do the network request in that thread.
     *
     * @param showProgressBar show the progress bar on the status dialog.
     * @throws FailedRequestException is something goes wrong
     */
    public final void execute(boolean showProgressBar)
                                       throws FailedRequestException { 
        if (_type == STANDARD && !_callerThreaded) {
            if (_component!=null) {
                //_requester.startStandardRequest(showProgressBar);
            }
            else {
                //_requester.doStandardSilentRequest();  // this is here for historical reasons, i don't think it is used
                                                       // i think this now would be a caller threaded service
            }
        }
        else if (_type == STANDARD && _callerThreaded) {
            if (_component!=null) {
               //_requester.doCallerThreadedRequest();
            }
            else {
               //_requester.doCallerThreadedSlientRequest(); 
            }
        }
        else if (_type == BACKGROUND) {
            if (_component!=null) {
                //_requester.startBackgroundRequest();
            }
            else {
                Assert.tst(false, "We should never be here");
            }
        }
        else {
            Assert.tst(false, "incorrect mode- this should never happen");
        }
    }


   //=======================================================================
   //----- Public Methods to control behavior ------------------------------
   //=======================================================================
    public final void setErrorTitle(String s) { 
            //_requester.setErrorTitle(s); 
    }
    

    /**
     * If this service fails then show the user a error message.
     * @return true to show the message, false to fail silently
     */
    public final boolean getShowUserErrors() { return _showUserErrors; }
    /**
     * If this service fails then show the user a error message.
     * @param show true to show the message, false to fail silently
     */
    public final void setShowUserErrors(boolean show) {_showUserErrors= show; }

    public final void setMoreRequestComming(boolean moreComming) {
        //_requester.setMoreRequestComming(moreComming);
    }


    /**
     * Show the network down error if the network is down.  This method is
     * used in combination with setProceedIfNetworkDown().  If the network is
     * down and this method returns a false then we do what
     * setProceedIfNetworkDown() returns.  You should never override
     * this method unless you know what you are doing.
     * @return true to show error message, false not to show error message
     */
    public final boolean getShowNetworkDownError() {
        return _showNetworkDownError;
    }
    /**
     * Show the network down error if the network is down.  This method is
     * used in combination with setProceedIfNetworkDown().  If the network is
     * down and this method returns a false then we do what
     * setProceedIfNetworkDown() returns.  You should never override
     * this method unless you know what you are doing.
     * @param show true to show error message, false not to show error message
     */
    public final void setShowNetworkDownError(boolean show) {
        _showNetworkDownError= show;
    }

    /**
     * This is the description of the service
     * @return a description
     */
    public String  getOperationDesc()         { return _operationDesc; }
    /**
     * This is the description of the service. Shown in the feedback dialog.
     * This should be set for every service in the constructor
     * @param desc a description of the service
     */
    public final void setOperationDesc(String desc) {
        _operationDesc= desc;
    }

    /**
     * Override the normal animation for this Threaded service
     *
     * @param smallIcon the small animated icon, used during background operation
     * @param normalIcon the normal sized animated icon, used blocking network operations
     */
    public final void setAnimatedIcons(Icon smallIcon, Icon normalIcon) {
        _overrideSmallIcon= smallIcon;
        _overrideNormalIcon= normalIcon;
    }

    public final boolean isUserOverrideIcons() { return _overrideSmallIcon!=null && _overrideNormalIcon!=null; }
    public final Icon getOverrideSmallIcon() { return _overrideSmallIcon; }
    public final Icon getOverrideNormalIcon() { return _overrideNormalIcon; }

    /**
     * This is what the service is doing when it is running.  It is gennerally
     * only needed in more complex services.
     * @return a description of what is happening
     */
    public final String getProcessingDesc()        { return _processingDesc; }

    /**
     * This is what the service is doing when it is running.  It is gennerally
     * only needed in more complex services. If you want to update user feedback
     * when a service dialog is up call this method
     * @param desc a description of what is happening
     */
    public final void setProcessingDesc(String desc) {
        _processingDesc= desc;
        //if (working() && _requester!=null) _requester.updateFeedback();
    }

    public final void setProgressFeedback(long    min,
                                          long    max,
                                          long    current,
                                          String desc) {
        _pBarMin    =min;
        _pBarMax    =max;
        _pBarCurrent=current;
        _pBarDesc   =desc;
        //if (working() && _requester!=null) _requester.updateFeedback();
    }

    public final void setProgressMin(long min){
        setProgressFeedback(min,_pBarMax,_pBarCurrent,_pBarDesc);
    }
    public final void setProgressMax(long max){
        setProgressFeedback(_pBarMin,max,_pBarCurrent,_pBarDesc);
    }

    public final void setProgressMinMax(long min, long max){
        setProgressFeedback(min,max,_pBarCurrent,_pBarDesc);
    }

    public final void setProgressCurrent(long current){
        setProgressFeedback(_pBarMin,_pBarMax,current,_pBarDesc);
    }
    public final void setProgressDesc(String desc){
        setProgressFeedback(_pBarMin,_pBarMax,_pBarCurrent,desc);
    }

    public final void incrementProgress(){ incrementProgress(null); }

    public final void incrementProgress(String newDesc){
        if (newDesc==null) newDesc= _pBarDesc;
        long newCurrent;
        if (_pBarCurrent<_pBarMin) newCurrent= _pBarMin;
        else                       newCurrent= _pBarCurrent+1;
        setProgressFeedback(_pBarMin, _pBarMax, newCurrent, newDesc);
    }


    public final long getProgressMin()            { return _pBarMin; }
    public final long getProgressMax()            { return _pBarMax; }
    public final long getProgressCurrent()        { return _pBarCurrent; }
    public final String getProgressDescription() { return _pBarDesc; }


    /**
     * If false ask the user if he want to proceed. If true then plow ahead.
     * If this is a silent service and returns false it will just stop.
     * Most of the time return false. Silent services generally return true
     * @return true to proceed even is the net is donw
     */
    public final boolean getProceedIfNetworkDown() {
        return _proceedIfNetworkDown;
    }

    /**
     * If false ask the user if he want to proceed. If true then plow ahead.
     * If this is a silent service and returns false it will just stop.
     * Most of the time return false. Silent services generally return true
     * @param proceed true to proceed even is the net is donw
     */
    public final void setProceedIfNetworkDown(boolean proceed) {
        _proceedIfNetworkDown= proceed;
    }

    /**
     * if true then listeners will be fired with an invokeLater
     * @param fireInAWT true to fire listener in AWT thread, false to fire them
     *         in current thread.
     */
    public final void setFireListenersInAWTThread(boolean fireInAWT) {
        _fireListenersInAWTThread= fireInAWT;
    }




   //=======================================================================
   //-------------------- Public Final methods -----------------------------
   //=======================================================================

    public final synchronized void shutdown() {
        //_thread.interrupt();  -- does not work as it should
        _interrupted= true;
        firePreFail();
    }

    //public final Requester getRequester() { return _requester; }
    public final int       getType()      { return _type;}
    public final boolean   interrupted()  { return _interrupted; }


   //=======================================================================
   //-------------------- Listener fire methods ---------------------------
   //=======================================================================

    /**
     * This method is intended to be used from a subclass of Threaded Service.
     * It would be call during a long network process that might have several
     * steps.  When the subclass needs to pass partial competed data to the
     * listener that can be vetoed.
     * @param ev the ThreadedServiceEvent
     * @throws FailedRequestException will stop the processing
     */
    protected final void fireVetoableUpdate(ThreadedServiceEvent ev)
                                               throws FailedRequestException {
        if (_listener != null) _listener.vetoableUpdate(ev);
    }

    /**
     * This method is intended to be used from a subclass of Threaded Service.
     * It would be call during a long network process that might have several
     * steps.  When the subclass needs to pass partial competed data to the
     * listener.
     * @param ev the ThreadedServiceEvent
     */
    protected final void fireUpdate(final ThreadedServiceEvent ev) {
        if (_listener != null) {
             if (_fireListenersInAWTThread) {
                 SwingUtilities.invokeLater(new Runnable() {
                     public void run() { fireUpdateNow(ev); }
                 });
             }
             else {
                 fireUpdateNow(ev);
             }
        }
    }


    private void firePreFail() {
        if (_listener != null) {
             if (_fireListenersInAWTThread) {
                 SwingUtilities.invokeLater( new Runnable() {
                       public void run() { firePreFailNow(); }
                  } );
             }
             else {
                 firePreFailNow();
             }
        }
    }
    private void fireFailed(final FailedRequestException ev) {
        if (_listener != null) {
             if (_fireListenersInAWTThread) {
                 SwingUtilities.invokeLater(new Runnable() {
                     public void run() { fireFailedNow(ev); }
                 });
             }
             else {
                 fireFailedNow(ev);
             }
        }
    }

    private void fireSuccess() {
        if (_listener != null) {
             if (_fireListenersInAWTThread) {
                 SwingUtilities.invokeLater( new Runnable() {
                       public void run() { fireSuccessNow(); }
                  } );
             }
             else {
                 fireSuccessNow();
             }
        }
    }

    // ===================================================================
    // --------------  Method from Runnable Interface --------------------
    // ===================================================================

    public final void run() {
        Thread.currentThread().setName(getOperationDesc());
        performService();
    }



 // ===================================================================
 // --------------  Method from VetoableDownloadListener Interface ----
 // ===================================================================

       /**
        *  Stop the download if the service has been interrupted.
        */
    public final void checkDataDownloading(DownloadEvent ev)
                                       throws VetoDownloadException {
          if (_interrupted) throw new VetoDownloadException("interrupted");
    }

 // ===================================================================
 // --------------  Methods from DownloadListener Interface ------------
 // ===================================================================
    public final void beginDownload(DownloadEvent ev) {
        //_requester.setStatusCount(ev.getMax());
        //_requester.updateStatus(ev.getCurrent(), ev.getMessage() );
        setProgressMinMax(0,ev.getMax());

    }

    public final void dataDownloading(DownloadEvent ev) {
        //_requester.updateStatus(ev.getCurrent(), ev.getMessage() );
        setProgressFeedback(0,ev.getMax(),ev.getCurrent(),ev.getMessage());
    }

    public final void downloadCompleted(DownloadEvent ev) {}
    public final void downloadAborted(DownloadEvent ev) {}

 // ===================================================================
 // ---------------------- Package Methods ----------------------------
 // ------------- Should only be called from Requester ----------------
 // ===================================================================
    final Exception getException()            { return _exception; }

    final boolean serviceCompleted() {  return !_interrupted && _completed; }
    final boolean working()          {  return !_interrupted && !_completed; }



    final void startThread() {
        if (_thread != null) {
            _thread.start();
        }
        else {
            run();
        }
    }

    /**
     * Given the exception get an Error string. I should probably do away
     * with this method altogether and just put it in Requester.
     * @param e the exception
     * @return the error String
     */
    String getPrimaryErrorStr(FailedRequestException e)  {
        String retval;
        if (e.isHtmlMessage()) {
            retval= getOperationDesc() + ": " + e.getUserMessage();
        }
        else {
            retval= e.getUserMessage();
        }
        return retval;
    }

 // ===================================================================
 // ---------------------- Protected Methods ----------------------------
 // ===================================================================
    protected final Thread    getThread() { return _thread; }

    protected abstract void doService() throws Exception;

 // ===================================================================
 // ---------------------- Private Methods ----------------------------
 // ===================================================================


    private void performService() {
        try {
            doService();
            _completed= true;
            if (_interrupted)  {
                         // note- the firePreFail() is called from interrupt()
                fireFailed( new FailedRequestException(
                               FailedRequestException.USER_CANCELED) );
            }
            else {
                fireSuccess();
            }
        } catch (Exception e) {
            _exception= e;
            _completed= true;
            firePreFail();
            fireFailed( new FailedRequestException(
                           FailedRequestException.NETWORK_DOWN, null, e) );
            if (_showException) e.printStackTrace(System.out);
        }

        switch (_type) {
            case STANDARD         : if (!_callerThreaded) wrapUpStandard();
                                      // note- callerThreaded==true does not
                                      // need to wrapup
                                    break;
            case BACKGROUND       : wrapUpBackground();           break;
            default:         Assert.tst(false, "incorrect type"); break;
        } // end switch
    }

    //private void wrapUpStandard() { _requester.endStandardRequest(); }
    private void wrapUpStandard() { }

    private void wrapUpBackground() {
         //if (_component!=null) _requester.endBackgroundRequest();
    }

    private void fireUpdateNow(ThreadedServiceEvent ev) {
        if (_listener != null) _listener.update(ev);
    }
    private void firePreFailNow() {
        if (_listener != null) _listener.preFail();
    }
    private void fireFailedNow(FailedRequestException e) {
        if (_listener != null) _listener.failed(e);
    }
    private void fireSuccessNow() {
        if (_listener != null) _listener.success();
    }


}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
