package edu.caltech.ipac.client.net;

import edu.caltech.ipac.gui.BaseUserDialog;
import edu.caltech.ipac.gui.ButtonScenario;
import edu.caltech.ipac.gui.CompletedListener;
import edu.caltech.ipac.gui.DialogAction;
import edu.caltech.ipac.gui.DialogEvent;
import edu.caltech.ipac.gui.IntTextField;
import edu.caltech.ipac.gui.LabeledTextFields;
import edu.caltech.ipac.gui.OKButtonScenario;
import edu.caltech.ipac.gui.RadioBox;
import edu.caltech.ipac.gui.UserDialog;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.action.ActionConst;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.util.action.RadioAction;
import edu.caltech.ipac.util.action.RadioEvent;
import edu.caltech.ipac.util.action.RadioListener;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

/**
 * 
 * @author Trey Roby
 */
public class ProxySetupDialog implements UserDialog, 
                                         RadioListener, 
                                         CompletedListener {

  private final static ClassProperties _prop   = 
                           new ClassProperties(ProxySetupDialog.class);
  private final static String DIALOG_TITLE= _prop.getTitle();
  private final static String USE_PROXY_PROP= 
                                    _prop.makeBase("useProxy.RadioValue");

  private BaseUserDialog    _dialog;
  private JTextField        _standardHost;
  private JTextField        _secureHost;
  private JTextField        _proxyUserName;
  private JPasswordField    _proxyUserPwd;
  private IntTextField      _standardPort;
  private IntTextField      _securePort;
  private RadioAction       _useProxyAction;
  private LabeledTextFields _proxyFields;

  public ProxySetupDialog(JFrame f) {
     OKButtonScenario buttons  = makeButtons( SwingConstants.HORIZONTAL, 
                                              "ProxySetupDialog" );
     buttons.addCompletedListener(this);
     _dialog = makeDialog(f, true, buttons);
     addStuff();
  }

  private void addStuff() {
     JPanel main= new JPanel( new BorderLayout() );
     _dialog.getContentPane().add(main, BorderLayout.CENTER);

     RadioBox useProxy= new RadioBox(_prop.makeBase("useProxy") );
     _useProxyAction= new RadioAction(_prop.makeBase("useProxy") );
     useProxy.addRadioAction(_useProxyAction);
     useProxy.addRadioListener(this);

     _proxyFields  = LabeledTextFields.createByProp(_prop.makeBase("proxy"));
     _standardHost = _proxyFields.addTextField("host", 30);
     _standardPort = _proxyFields.addIntTextField(10, "port");
     _secureHost   = _proxyFields.addTextField("secureHost", 30);
     _securePort   = _proxyFields.addIntTextField(10, "securePort");
     _proxyUserName= _proxyFields.addTextField("proxyUserName", 30);
     _proxyUserPwd = new JPasswordField(30);
     _proxyFields.addComponent(_proxyUserPwd, "proxyUserPwd");


     Box innerBox= Box.createHorizontalBox();
     innerBox.add( Box.createGlue());
     innerBox.add( useProxy);
     innerBox.add( Box.createGlue());

     Box outerBox= Box.createVerticalBox();
     outerBox.add(Box.createVerticalStrut(10) );
     outerBox.add(innerBox );
     outerBox.add(Box.createVerticalStrut(10) );

     main.add(outerBox, BorderLayout.NORTH);
     main.add(_proxyFields, BorderLayout.CENTER);

  }

   public static void setupProxyFromSavedProperties() {
      String command= AppProperties.getPreference( USE_PROXY_PROP);
      String s;
      if (command.equals("proxy")) {
        s= AppProperties.getPreference("net.http.proxyHost",null);
        if (s!=null) System.setProperty( "http.proxyHost",  s);

        s= AppProperties.getPreference("net.http.proxyPort",null);
        if (s!=null) System.setProperty( "http.proxyPort",  s);

        s= AppProperties.getPreference("net.https.proxyHost",null);
        if (s!=null) System.setProperty( "https.proxyHost",  s);

        s= AppProperties.getPreference("net.https.proxyPort",null);
        if (s!=null) System.setProperty( "https.proxyPort",  s);

        s= AppProperties.getPreference("net.http.proxyUser",null);
        if (s!=null) System.setProperty( "http.proxyUser",  s);

        s= AppProperties.getPreference("net.http.proxyPassword",null);
        if (s!=null) System.setProperty( "http.proxyPassword",  s);
     }
     else {
        System.setProperty( "http.proxyHost",     "");
        System.setProperty( "http.proxyPort",     "");
        System.setProperty( "https.proxyHost",    "");
        System.setProperty( "https.proxyPort",    "");
        System.setProperty( "http.proxyUser",     "");
        System.setProperty( "http.proxyPassword", "");
     }
   }



  //-------------------------------------------------------------------
  //============== Method from RadioListener Interface ===============
  //-------------------------------------------------------------------
   public void radioChange(RadioEvent ev) {
        adjustFields(ev.getCommand());
   }



  // ================================================================
  // -------------------- Methods from Interfaces -------------------
  // ================================================================
  public ButtonScenario  getButtonScenario() {
     return _dialog.getButtonScenario();
  }
  public DialogAction getAction() { return null; }
  public void setVisible(boolean visible) {
     if (visible) init();
     _dialog.setVisible(visible);
  }

  public JDialog getJDialog()             { return _dialog.getJDialog(); }

  public void inputCompleted(DialogEvent e) {
     String useProxy= _useProxyAction.getRadioValue();
     AppProperties.setPreference( USE_PROXY_PROP, useProxy);
     if (useProxy.equals("proxy")) {
        AppProperties.setPreference( "net.http.proxyHost",  
                                     _standardHost.getText() );
        if (_standardPort.getValue() == ActionConst.INT_NULL) {
            AppProperties.setPreference( "net.http.proxyPort",  "");
        }
        else {
            AppProperties.setPreference( "net.http.proxyPort",  
                                         _standardPort.getValue()+"" );
        }
        AppProperties.setPreference( "net.https.proxyHost", 
                                     _secureHost.getText() );
        if (_securePort.getValue() == ActionConst.INT_NULL) {
            AppProperties.setPreference( "net.https.proxyPort",  "");
        }
        else {
            AppProperties.setPreference( "net.https.proxyPort",  
                                         _securePort.getValue()+"" );
        }

        AppProperties.setPreference( "net.http.proxyUser",  
                                              _proxyUserName.getText() );
        AppProperties.setPreference( "net.http.proxyPwd",
                                     new String(_proxyUserPwd.getPassword()));


     }
     //"ftp.proxyHost"
     //"ftp.proxyPort"

     setupProxyFromSavedProperties();

     NetworkManager.getInstance().evaluateNetwork();
  }

  // ================================================================
  // -------------------- Private / Protected Methods ----------------
  // ================================================================

 
   private void init() {
      String command= AppProperties.getPreference( USE_PROXY_PROP);
      _useProxyAction.setRadioValue(command);
      adjustFields(command);
      if (command.equals("proxy")) {
         _standardHost.setText( 
                   AppProperties.getPreference("net.http.proxyHost","") );
         _secureHost.setText( 
                   AppProperties.getPreference("net.https.proxyHost","") );
         _proxyUserName.setText( 
                   AppProperties.getPreference("net.http.proxyUser","") );
         _proxyUserPwd.setText( 
                   AppProperties.getPreference("net.http.proxyPassword","") );


         String s= AppProperties.getPreference("net.http.proxyPort",null);
         if (s==null) {
             _standardPort.setValue(ActionConst.INT_NULL);
         }
         else {
             _standardPort.setValue( 
                     AppProperties.getIntPreference("net.http.proxyPort",0) );
         }

         s= AppProperties.getPreference("net.https.proxyPort",null);
         if (s==null) {
             _securePort.setValue(ActionConst.INT_NULL);
         }
         else {
             _securePort.setValue( 
                     AppProperties.getIntPreference("net.https.proxyPort",0) );
         }
      }



   }

   private void adjustFields(String command) {
       if ( command.equals("proxy")){
           enableFields(true);
       }
       else if ( command.equals("direct")){
           enableFields(false);
       }
       else {
           Assert.tst(false);
       }
   }

   private void enableFields(boolean enable) {
       _proxyFields.setFieldEnabled( _standardHost,  enable);
       _proxyFields.setFieldEnabled( _standardPort,  enable);
       _proxyFields.setFieldEnabled( _secureHost,    enable);
       _proxyFields.setFieldEnabled( _securePort,    enable);
       _proxyFields.setFieldEnabled( _proxyUserName, enable);
       _proxyFields.setFieldEnabled( _proxyUserPwd,  enable);
   }


  // ================================================================
  // -------------------- Factory Methods ---------------------------
  // ================================================================
  protected OKButtonScenario makeButtons(int direction, String helpID) {
      return new OKButtonScenario( direction, helpID);
  }
  protected BaseUserDialog makeDialog(JFrame         f, 
                                      boolean        modal, 
                                      ButtonScenario b) {
      return new BaseUserDialog(f, DIALOG_TITLE, modal, b);
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
