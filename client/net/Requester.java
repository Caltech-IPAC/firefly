package edu.caltech.ipac.client.net;

import edu.caltech.ipac.client.ClientLog;
import edu.caltech.ipac.gui.BackgroundStatusDialog;
import edu.caltech.ipac.gui.BaseSaveAction;
import edu.caltech.ipac.gui.DialogSupport;
import edu.caltech.ipac.gui.ExtensionFilter;
import edu.caltech.ipac.gui.IconFactory;
import edu.caltech.ipac.gui.MenuBarState;
import edu.caltech.ipac.gui.OptionPaneWrap;
import edu.caltech.ipac.gui.SwingSupport;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.action.ClassProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 * @version $Id: Requester.java,v 1.10 2012/12/11 21:00:48 roby Exp $
 *
 * This class provides the control when the use does a 
 * foreground network request.  It shows a working dialog, provides for
 * canceling, starts the network request, and shows errors when request fail.
 *
 * WARNING!!!! this is probably to most complex class I have ever written.
 * I have not yet put much documentation into it.
 * 
 * The main reason that it is so complex is because it provides 
 * a way to cancel: you must make an operation appear to be happening 
 * in the foreground while really doing it in the background.  What really
 * happens in the foreground is a dialog waiting for the user to push the
 * cancel button.
 * 
 */
public class Requester {

    private static final ClassProperties _prop= new ClassProperties(
                                                               Requester.class);

    private final static String UNABLE_EST_1 =  _prop.getError("unableEst1");
    private final static String UNABLE_EST_2=   _prop.getError("unableEst2");
    private final static String ABORT_TITLE =   _prop.getTitle("abort");
    private final static String NET_PROBLEM =   _prop.getTitle("netProblem");
    private final static String SERVER_PROBLEM= _prop.getTitle("serverProblem");
    private static final Icon FALLBACK_NORMAL_ICON = getFallbackAnimation(false);
    private static final Icon FALLBACK_SMALL_ICON = getFallbackAnimation(true);

    private static Icon _defNormalIcon = FALLBACK_NORMAL_ICON;
    private static Icon _defSmallIcon  = FALLBACK_SMALL_ICON;

    private static final int DOWN_QUESTION_INTERVAL= 5*60*1000; // 5 minutes

    private final ThreadedService   _action;
    private final Component         _component;
    private JProgressBar            _progress     = null;
    private BackgroundStatusDialog  _backDialog   = null;
    private JDialog                 _foreDialog   = null;
//    private IconMovie               _im           = null;
    private FailedRequestException  _failException= null;
//    private final JLabel            _iconLabel= new JLabel();
    private JLabel                  _textLabel;

    private static long             _lastDownProcessQuestionTime= 0;
    private static Map<JFrame,BackgroundStatusDialog> _backgroundDialogs=
                     Collections.synchronizedMap(new HashMap<JFrame,BackgroundStatusDialog>());
    private boolean _moreRequestComming= false;
    private BackgroundStatusDialog.StatusDisplay _statusDisplay;
    private String _errorTitle= null;

    static {
        IconFactory factory= IconFactory.getInstance();
        factory.addResource(Requester.class);
    }

    Requester(ThreadedService action, Component c) {
        _action= action;
        _component= c;
    }

  //====================================================================
  //--------------------------------------------------------------------
  //====================================================================

    /**
     * Override the normal animation (running cheetah) used during a network call.
     *
     * @param small the small animated icon, used during background operation
     * @param normal the normal sized animated icon, used blocking network operations
     */
    public static void setDefaultAnimatedIcons(Icon small, Icon normal) {
        _defNormalIcon = small;
        _defSmallIcon  = normal;
    }


    void setMoreRequestComming(boolean moreRequestComming) {
        _moreRequestComming= moreRequestComming;
    }

    void startStandardRequest(boolean showProgress) throws FailedRequestException {
        doPreRequestCheck();
        String options[]= { _prop.getName("abortButton") };
        JLabel iconLabel= new JLabel();
        _textLabel= new JLabel(makeLabelDesc(_action));

        JPanel combine= new JPanel();
        iconLabel.setIcon(getAnimation(false));
        combine.add(iconLabel);
        combine.add(_textLabel);

        Component c= showProgress ? makeIconProgressBox(iconLabel) : combine;
        JOptionPane pane = new JOptionPane(c, JOptionPane.PLAIN_MESSAGE, 0, null, options );
        _action.startThread();
        _foreDialog = pane.createDialog(SwingSupport.getJFrame(_component),
                                        _action.getOperationDesc() + ABORT_TITLE );
        DialogSupport.addWindow(_foreDialog);
        DialogSupport.setWaitCursor(true);
        DialogSupport.setWaitCursor(_foreDialog,true);
        //DialogSupport.setFade(true);


        MenuBarState.disable(_component);
           // ------------ Block -----------------
        if (!_action.serviceCompleted()) _foreDialog.setVisible(true);  // block
           // ------------ Block -----------------
        MenuBarState.restore(_component);


        _foreDialog.dispose();
        //DialogSupport.setFade(false);
        DialogSupport.setWaitCursor(false); // - remove clock
        DialogSupport.removeWindow(_foreDialog);
        doPostRequestCheck(false);
    }


    void endStandardRequest() {
        if (_foreDialog!=null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { _foreDialog.setVisible(false); }
            });
        }
    }

     /**
      * A background request is usually called from an object that itself is
      * running in a separate thread.  The background request also runs
      * in yet another thread because we want to be able to make it time out.
      * So we put the calling thread in a sleep (for time out purposes) and
      * to the request in another thread.
      * @throws FailedRequestException if something goes wrong on the request
      */
     @Deprecated
     void doStandardSilentRequest() throws FailedRequestException {

         ClientLog.warning("This method is Deprecated, I don't think we use it anymore",
                           "If I see this messsage I am WRONG!!!!!");

        doPreRequestCheck();
        _action.startThread();
           // ------------ Block -----------------------------
        try { Thread.sleep(5000); // the thread blocks here
        }  catch (InterruptedException e) {
            System.out.println("Requester.doBackgroundRequest: interrupted");
        }
           // ------------ Block -----------------------------
        doPostRequestCheck(true);
    }

    void startBackgroundRequest()
                                        throws FailedRequestException {
        doPreRequestCheck();
        _backDialog= makeBackgroundDialog();
//        _backDialog.setVisible(true);
        _progress= _statusDisplay.getProgressBar();
        _textLabel= _statusDisplay.getLabel();
        _action.startThread();
    }

    void endBackgroundRequest() {
        try {
           closeBackgroundDialogAndTest();
        } catch (FailedRequestException e) {
            // do nothing - already handled
        }
   }




    void doCallerThreadedRequest() throws FailedRequestException {
        doPreRequestCheckFromThread();

        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                _backDialog= makeBackgroundDialog();
                _progress= _statusDisplay.getProgressBar();
                _textLabel= _statusDisplay.getLabel();
            }} );

        _action.run();
        closeBackgroundDialogAndTest();
    }

    void doCallerThreadedSlientRequest() throws FailedRequestException {
        doPreRequestCheckFromThread();
        _action.run();
        doPostRequestCheck(false);
    }

    void updateFeedback() {
        UpdateFeedback feedback= new UpdateFeedback(_action,_progress,_textLabel);
        if (EventQueue.isDispatchThread()) {
            feedback.showFeedback();
        }
        else {
            SwingUtilities.invokeLater(feedback);
        }
    }

    void setErrorTitle(String s) { _errorTitle= s; }

  //====================================================================
  //--------------------------------------------------------------------
  //====================================================================



    private void doPreRequestCheckFromThread() throws FailedRequestException {
        _failException= null;
        try {
            SwingUtilities.invokeAndWait( new Runnable() {
                              public void run() {
                                    try { doPreRequestCheck();
                                    } catch (FailedRequestException fre) {
                                         _failException= fre;
                                    }
                              }} );
        } catch (Exception e) {
              ClientLog.warning("Got a unexpected exception", e.toString());
        }
        if (_failException != null) throw _failException;
    }

    /**
     * If the network is not up then determine if we should ask the user is he
     * wants to try the network request anyway.  If we determine that we are
     * not going to try the request then throw a FailedRequestException.
     * @throws FailedRequestException if something goes wrong on the request
     */
    private void doPreRequestCheck() throws FailedRequestException {
        NetworkManager netMan=NetworkManager.getInstance();
        if(netMan.getNetworkStatus()==NetworkManager.NET_DOWN) {
            boolean proceed= _action.getProceedIfNetworkDown();

            if(_action.getShowNetworkDownError()) {
                proceed= askTryAnywayQuestion();
            }
            if(!proceed) {
                throw new FailedRequestException(
                               FailedRequestException.NETWORK_DOWN);
            }
        }
    }

    /**
     * Tell the user the network is and and ask if he wants to try his
     * request anyway.  If we have already ask this question recently and
     * the user said yes then down ask again just return yes answer.
     * @return the user answer, true= try anyway, false= don't try
     */
    private boolean askTryAnywayQuestion() {
        int  answer = JOptionPane.YES_OPTION;
        long nowTime= new Date().getTime();
        long intervalSinceNetDownErr= nowTime-_lastDownProcessQuestionTime;

        if(intervalSinceNetDownErr>DOWN_QUESTION_INTERVAL) {
            String msg= _prop.getError("continue1") +
                        _action.getOperationDesc()+
                        _prop.getError("continue2");
            answer= OptionPaneWrap.confirmDialog(_component, msg,
                                                 _prop.getTitle("networkDown"),
                                                 JOptionPane.YES_NO_OPTION);
            _lastDownProcessQuestionTime= (answer==JOptionPane.YES_OPTION) ?
                                          nowTime : 0;
        }
        return (answer==JOptionPane.YES_OPTION);
    }


    private void doPostRequestCheck(boolean wasTimeout)
                                               throws FailedRequestException {
        NetworkManager netMan= NetworkManager.getInstance();
        Exception e= _action.getException();
        if ( _action.serviceCompleted() && e != null)  { // there was an error
            if (_action.getShowUserErrors()) showError();
            netMan.evaluateNetwork();
            throwException(e);
        } else if (_action.working() ) { // if it is still going
            _action.shutdown();
            if (wasTimeout) {
                 throw new FailedRequestException(
                                    FailedRequestException.SERVER_TIMEOUT);
            }
            if (e != null) {
                 if (_action.getShowUserErrors()) showError();
                 netMan.evaluateNetwork();
                 throwException(e);
            }
        }
        if (_action.interrupted()) { // if interrpted then it was canceled
                 throw new FailedRequestException(
                                FailedRequestException.USER_CANCELED);
        }
        // if we get to the end of this method everything ran successfully
    }

    private void throwException(Exception e) throws FailedRequestException {
        if (e instanceof FailedRequestException) {
            throw (FailedRequestException)e;
        }
        else {
            throw new FailedRequestException(
                           FailedRequestException.NETWORK_DOWN,null, e );
        }
    }

    /**
     * Get the Icon array for the working animation.
     * @param small true if need to return a small animated gif otherwise return the normal size
     * @return animated gif
     */
    private static Icon getFallbackAnimation(boolean small) {
        IconFactory f= IconFactory.getInstance();
        return small ? f.getIcon("resources/cheetah_running-small.gif") :
                       f.getIcon("resources/cheetah_running.gif");
    }

    private Icon getAnimation(boolean small) {
        Icon icon;
        if (small) icon= _action.isUserOverrideIcons() ? _action.getOverrideSmallIcon()  : _defSmallIcon;
        else       icon= _action.isUserOverrideIcons() ? _action.getOverrideNormalIcon() : _defNormalIcon;
        return icon;
    }



    private void showError() {
        String options[]= { _prop.getName("ok"), _prop.getName("details") };
        NetworkManager netMan= NetworkManager.getInstance();
        String errString;
        Exception exception= _action.getException();
        FailedRequestException fre;
        boolean doHTML= false;
        if (exception instanceof FailedRequestException) {
            fre= (FailedRequestException)exception;
            if (!fre.getUserShouldSeeHint()) return;
            errString=    _action.getPrimaryErrorStr(fre);
            doHTML=       fre.isHtmlMessage();
        }
        else {
            errString= UNABLE_EST_1 + _action.getOperationDesc() +
                       UNABLE_EST_2;
            fre= new FailedRequestException(errString, null, exception);
        }
        JComponent jt;
        MenuBarState.disable(_component);
        /*
        * use a JLabel when doing html, otherwise use a JTextArea- it will
        * do multiline and use the default font nicely.
        */
        if (doHTML) {
            errString= cleanExtraHtmlTagsOut(errString);
            try {
                jt= new JLabel( "<html>" + errString);
            } catch (ClassCastException cce){// this is because of a swing bug
                jt= new JLabel( "Error in network request");
                ClientLog.message("The following string produced a " +
                                  "Class Cast Exception in Swing:",
                                  errString);
            }
        }
        else {
            jt= new JTextArea(errString);
            ((JTextArea)jt).setEditable(false);
            jt.setBackground(null);
        }
        JOptionPane pane = new JOptionPane(jt,
                                           JOptionPane.WARNING_MESSAGE, 0, null, options );
        String title;
        if (_errorTitle!=null) {
            title= _errorTitle;
        }
        else {
            title= (netMan.getNetworkStatus() == NetworkManager.NET_DOWN) ?
                   NET_PROBLEM : SERVER_PROBLEM;
        }
        JDialog dialog= pane.createDialog(_component, title);
        dialog.pack();
        Object answer = options[1];
        while (options[1].equals(answer)) {
            dialog.setVisible(true);
            answer = pane.getValue();
            if (options[1].equals(answer)) showDetailMessage(fre);
        }
        MenuBarState.restore(_component);
        dialog.dispose();
    }

    private void showDetailMessage(FailedRequestException e) {
        String detailString= getDetailString(e);
        JLabel label= new JLabel();
        Box box= Box.createHorizontalBox();
        box.add(label);
        JScrollPane p= new JScrollPane(box);
        p.setPreferredSize(new Dimension( 650, 400) );
        label.setText( "<html><font color=black>" + detailString +"</font>");
        label.setMaximumSize( new Dimension( 600, 5000) );
        //OptionPaneWrap.showInfo( _component, p, _prop.getTitle("details"));
       //=================

        String ops[]    = {_prop.getName("ok"), _prop.getName("save")};
        int decision= 1;
        while (decision==1) {
            decision= OptionPaneWrap.optionDialog( _component, p,
                                                   _prop.getTitle("details"),
                                                   JOptionPane.DEFAULT_OPTION,
                                                   JOptionPane.INFORMATION_MESSAGE,
                                                   null, ops, ops[0] );
            if (decision==1) saveException(e);
        }
    }


    private String getDetailString(Exception e) {
        String s= cleanExtraHtmlTagsOut(e.toString());
        s= s.replaceAll("<", "&lt;");
        s= s.replaceAll(">", "&gt;");
        s= s.replaceAll("\n", "<br>");
        return s;

    }

    private void saveException(final FailedRequestException e) {
        new BaseSaveAction(_prop.makeBase("saveException"),_component) {
            protected void doSave(File f) throws Exception {
                BufferedWriter out= null;
                try {
                    out= new BufferedWriter(new FileWriter(f));
                    out.write(e.toString());
                    out.write('\n');
                } finally {
                    if (out!=null) out.close();
                }

            }
            protected FileFilter[] getFilterArray() {
                String[] extension={FileUtil.TXT };
                return new FileFilter[] {
                             new ExtensionFilter(extension, "Text Files", true)
                                 };
            }
            protected String getDirProperty() { return null; }
            protected File modifyFile(File f) {
                return FileUtil.modifyFile(f, FileUtil.TXT);
            }

        }.actionPerformed(new ActionEvent(this,0,"save"));
    }

    private BackgroundStatusDialog makeBackgroundDialog() {
        // a background dialog will always have a progess bar
        JFrame f= SwingSupport.getJFrame(_component);
        BackgroundStatusDialog dialog;
        if (!_backgroundDialogs.containsKey(f)) {
            dialog= new BackgroundStatusDialog(f, _action.getOperationDesc() + ABORT_TITLE, getAnimation(true));
            _backgroundDialogs.put(f,dialog);
        }
        dialog= _backgroundDialogs.get(f);
        String msg= makeLabelDesc(_action);
        _statusDisplay= dialog.findOrAddDisplay(_action, msg);
        updateFeedback();
        return dialog;
    }


    private void closeBackgroundDialogAndTest() throws FailedRequestException{
        _failException= null;
//        if (_im != null) _im.endMovie();
        try {
             SwingUtilities.invokeAndWait(
                   new Runnable() {
                        public void run() {
                           if(!_moreRequestComming) {
                               _backDialog.removeDisplay(_statusDisplay);
                               if (!_backDialog.getJDialog().isVisible()) {
                                   JFrame f= SwingSupport.getJFrame(_component);
                                   _backgroundDialogs.remove(f);
                               }
                           }
                           else {
                               _statusDisplay.releaseDisplay();
                           }
//                           _backDialog.getJDialog().dispose();
                           try {
                                doPostRequestCheck(false);
                           } catch (FailedRequestException e) {
                                _failException= e;
                           }
             }} );
        } catch (Exception e) {
             System.out.println("Got unexpected exception in " +
                                "Requester.closeBackgroundDialogAndTest");
             System.out.println(e);
        }
        if (_failException != null) throw _failException;
    }

    private Component makeIconProgressBox(JLabel iconLabel) {
        _textLabel.setText( makeLabelDesc(_action));
        JComponent box= new JPanel(new BorderLayout());
        JComponent upDown= new JPanel(new BorderLayout());
        iconLabel.setOpaque(true);
        iconLabel.setBorder(new EmptyBorder(0,2,0,5));
        _textLabel.setBorder(new EmptyBorder(0,2,0,5));
        box.add(iconLabel, BorderLayout.WEST);
        upDown.add(_textLabel, BorderLayout.NORTH);
        box.add(upDown, BorderLayout.CENTER);
        _progress= new JProgressBar();
        box.add(_progress, BorderLayout.SOUTH);
        _progress.setIndeterminate(true);
        _progress.setStringPainted(true);
        Dimension d= new Dimension(200, 20);
        _progress.setPreferredSize(d);
        _progress.setMinimumSize(d);
        _progress.setBorder(new EmptyBorder(5,1,1,1));
        updateFeedback();
        return box;
    }

    /**
     * this method exist because the <title> html tag makes JLabel throw 
     * an exception
     * @param s the html string
     * @return  the string remove of things JLabel does not support
     */
    private String cleanExtraHtmlTagsOut(String s) {
        String htmlToRemove[]= {"<title>", "</title>", "<TITLE>", "</TITle>"};
        s= " " + s;

        for(String html : htmlToRemove) {
            for(int idx= s.indexOf(html); (idx != -1);
                idx= s.indexOf(html)) {
                s= s.substring(0,idx) + s.substring(idx+html.length());
            }
        }

        s= stripTag(s, "<script" , "</script");
        s= stripTag(s, "<SCRIPT" , "</SCRIPT");
        s= stripTag(s, "<img alt" , ">");
        s= stripTag(s, "<IMG ALT" , ">");


        s= s.substring(1);
        //System.out.println("returning: " + s);
        return s;
    }

    private String stripTag(String s, String begin, String end) {
        int endIdx;
        for(int idx= s.indexOf(begin); (idx != -1); idx= s.indexOf(begin)) {
            endIdx= s.indexOf(end, idx);
            if (endIdx>-1) {
                s= s.substring(0,idx) + s.substring(endIdx+end.length());
            }
            else {
                s= s.substring(0,idx);
            }
        }
        return s;
    }


    private static String makeLabelDesc(ThreadedService service) {
        String str;
        String proc= "";
        if (service.getProcessingDesc()!=null &&
            service.getProcessingDesc().length()>0) {
            proc= "<br>" +service.getProcessingDesc();
        }
        str= "<html>" + service.getOperationDesc() + proc;
//        str= "<html>" + _action.getOperationDesc() + proc + "<br><br>" +
//             ABORT_INFO;
        return str;
    }

//==================================================================
//------------------ Private inner classes -------------------------
//==================================================================

    /**
     * This class annimates a array of gifs using a separate thread.
     */
//    private class IconMovie implements Runnable {
//        private final int SHORT_SLEEP_TIME= 100;  // .1 sec
//        private Thread     _thread;
//        private boolean    _go= true;
//        private JDialog    _dialog;
//
//        public IconMovie(JDialog dialog) {
//           _dialog  = dialog;
//           _thread= new Thread(this, "IconMovie");
//           _thread.setPriority( Thread.MIN_PRIORITY);
//           _thread.start();
//        }
//
//        public void run() {
//           _go= true;
//           UpdateIcon ui= new UpdateIcon(_iconLabel);
//           int i= 0;
//           int sleepTime;
//           while (_go) {
//               Thread.yield();
//               ui.setIcon(_icons[i++] );
//               SwingUtilities.invokeLater( ui );
//               if (i == _icons.length) {
//                  i= 0;
//                  sleepTime= SHORT_SLEEP_TIME;
//               }
//               else {
//                  sleepTime= SHORT_SLEEP_TIME;
//               }
//               try { Thread.sleep(sleepTime); }
//               catch (InterruptedException e) { _go= false;}
//           }
//        }
//
//        public void endMovie() {
//           SwingUtilities.invokeLater( new Runnable() {
//                public void run() { _dialog.setVisible(false); }
//           } );
//           _go=false;
//           _thread.interrupt();
//        }
//    }

    /**
     * A class to set the icon in a label or JOptionPane.  It is made to be
     * used with SwingUtilities.invokeLater()
     */
//    private class UpdateIcon implements Runnable {
//        Icon        _icon;
//        JComponent  _c;
//        UpdateIcon(JComponent c) { _c= c; }
//        void setIcon(Icon icon)  { _icon= icon; }
//        public void run() {
//              if (_c instanceof JLabel)
//                    ((JLabel)_c).setIcon(_icon);
//              else if (_c instanceof JOptionPane)
//                    ((JOptionPane)_c).setIcon(_icon);
//              else {
//                    Assert.tst(false);
//              }
//        }
//    }

    /**
     * A class to update the information on the feedbakc dialogs.
     * It is made to be used with SwingUtilities.invokeLater()
     */
    private static class UpdateFeedback implements Runnable {
        private long    _min;
        private long    _max;
        private long    _current;
        private String _pText;
        private String _lText;
        private final JProgressBar _progress;
        private final JLabel _label;

        public  UpdateFeedback(ThreadedService service,
                               JProgressBar progress,
                               JLabel       label) {
            _progress= progress;
            _label= label;
            _lText  = makeLabelDesc(service);
            _min    = service.getProgressMin();
            _max    = service.getProgressMax();
            _current= service.getProgressCurrent();
            _pText  = service.getProgressDescription();
        }
        public void run() { showFeedback();  }

        public void showFeedback() {
            if(_progress!=null) {
                if (_current <_min) {
                    _progress.setIndeterminate(true);
                }
                else {
                    _progress.setIndeterminate(false);
                    _progress.setValue((int)_current);
                    _progress.setMinimum((int)_min);
                    _progress.setMaximum((int)_max);
                }
                _progress.setString(_pText);
            }
            if (_label!=null)  _label.setText(_lText);

        }
    }




//==================================================================
//================= End inner classes ==============================
//==================================================================


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
