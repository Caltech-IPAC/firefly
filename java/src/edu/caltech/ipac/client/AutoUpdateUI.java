package edu.caltech.ipac.client;

import edu.caltech.ipac.gui.MenuBarState;
import edu.caltech.ipac.gui.OptionPaneWrap;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.Installer;
import edu.caltech.ipac.util.JarVersion;
import edu.caltech.ipac.util.JarExpander;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.util.software.SoftwarePackage;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JDialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Color;
import java.util.List;
import java.io.File;

class AutoUpdateUI {


    public static enum UpdateAction { UPDATE_NOW,  UPDATE_LATER, DONT_UPDATE};


    private static final ClassProperties _prop=
                              new ClassProperties(AutoUpdateUI.class);
    private static String _feedbackStr= _prop.getName("expanding");
    private static final Color EXPAND_COLOR= Color.BLUE.brighter();

   private final JFrame  _f;
   private final String _appName;
   private final JLabel _outLabel= new JLabel();;

   AutoUpdateUI(JFrame f, String appName) {
      _f      = f;
      _appName= appName;
      Font font= _outLabel.getFont();
      _outLabel.setFont(font.deriveFont(Font.PLAIN));
   }

   UpdateAction askUpdateQuestion(List<LocalPackageDesc> localPackages,
                                  boolean isRetry,
                                  boolean updatingJRE) {

      UpdateAction retval   = UpdateAction.UPDATE_NOW;
      String vProp          = isRetry ? "retry" : "newVersion";

      String newVersionTitle= format(_prop.getTitle(vProp));
      String newVersion     = format(_prop.getName( vProp));
      String announce       = format(getVersionDesc());

      if (updatingJRE) announce+= format(_prop.getName("jreWarn"));

      String yesNow  = format(_prop.getName("yesNow"));
      String yesLater= format(_prop.getName("yesLater"));
      String noNever = format(_prop.getName("noNever"));
      String details = format(_prop.getName("details"));
      String retryOps[]   = {yesNow, yesLater, noNever};
      String standardOps[]= {yesNow, yesLater, noNever, details};
      String ops[]= isRetry ? retryOps : standardOps;

      String out= "<html>" + newVersion + "<br><br>" + announce;


      int decision= 3;
      while (decision==3) {
           _outLabel.setText(out);
           decision= OptionPaneWrap.optionDialog( _f, _outLabel,
                                                  newVersionTitle,
                                                  JOptionPane.DEFAULT_OPTION,
                                                  JOptionPane.INFORMATION_MESSAGE, null,
                                                  ops, ops[0] );
          if (decision==3) showDetailsDialog(localPackages, updatingJRE);
      }
      switch (decision) {
           case 0:  retval= UpdateAction.UPDATE_NOW;   break;
           case 1:  retval= UpdateAction.UPDATE_LATER; break;
           case 2:  retval= UpdateAction.DONT_UPDATE;  break;
           default: Assert.tst(false);    break;
      }

      return retval;
   }


   void showFinalQuitInfo() {
      String quitTitle = format(_prop.getTitle("quitInstruction"));
      String quitInfo  = format(_prop.getName( "quitInstruction"));
      OptionPaneWrap.showInfo( _f, quitInfo, quitTitle);
   }

   UpdateAction showProgRunningQuitQuestion(boolean repeat) {
      UpdateAction retval    = UpdateAction.DONT_UPDATE;
      String quitTitle = format(_prop.getTitle("progRunning"));
      String vprop     = repeat ? "progRunning2" : "progRunning";
      String quitInfo  = format(_prop.getName( vprop));

      int answer= OptionPaneWrap.confirmDialog(_f, quitInfo, quitTitle,
                                               JOptionPane.OK_CANCEL_OPTION);
      switch (answer) {
           case JOptionPane.OK_OPTION    :  retval= UpdateAction.UPDATE_NOW;
               break;
           case JOptionPane.CANCEL_OPTION:  retval= UpdateAction.DONT_UPDATE;
               break;
           default : Assert.tst(true, "answer= " + answer);
               break;
      }

      return retval;
   }


   boolean askUserWantsDisabled() {
       int answer= OptionPaneWrap.confirmDialog(_f,
                                                _prop.getName("disableUpdate"),
                                                _prop.getTitle("disableUpdate"),
                                                JOptionPane.YES_NO_OPTION);
       return (answer==JOptionPane.YES_OPTION);

   }

   UpdateAction askInformOfMinorUpdate(List<LocalPackageDesc> localPackages) {
      UpdateAction retval= UpdateAction.UPDATE_NOW;

      String newVersionTitle= format(_prop.getTitle("minorUpdate"));
      String yesNow         = format(_prop.getName("yesNow"));
      String yesLater       = format(_prop.getName("yesLater"));
      String details        = format(_prop.getName("details"));
      String ops[]          = {yesNow, yesLater, details};

      StringBuffer    announce = new StringBuffer(200);
      announce.append(format(_prop.getName("minorUpdate")));
      announce.append("<br><br>");
      announce.append(getVersionDesc());

      MenuBarState.disable(_f);

      int decision= 2;
      while (decision==2) {
          _outLabel.setText(announce.toString());
          decision= OptionPaneWrap.optionDialog( _f, _outLabel,
                                                 newVersionTitle,
                                                 JOptionPane.DEFAULT_OPTION,
                                                 JOptionPane.INFORMATION_MESSAGE,
                                                 null, ops, ops[0] );
          if (decision==2) showDetailsDialog(localPackages, false);
      }
      switch (decision) {
           case 0:  retval= UpdateAction.UPDATE_NOW;   break;
           case 1:  retval= UpdateAction.UPDATE_LATER; break;
           default: Assert.tst(false);    break;
      }

      MenuBarState.restore(_f);

      return retval;
   }

   void warnUserOfNoAccess() {
      String title      = format(_prop.getTitle("noAccess"));
      String noAccessMsg= format(_prop.getName( "noAccess"));
      OptionPaneWrap.showError( _f, noAccessMsg, title);
   }




    ExpandFeedBack startExpandingMessage() {
        return new ExpandFeedBack(_f);
    }



    void tellUserVistaAccessProblem() {
        String title      = format(_prop.getTitle("noAccess"));
        String noAccessMsg= format(_prop.getName( "noAccess.vista"));

        String ok         = "OK";
        String ops[]= {ok};

        // use option dialog method so we will wait for the reply before returning
        OptionPaneWrap.optionDialog( _f, noAccessMsg,
                                               title,
                                               JOptionPane.OK_OPTION,
                                               JOptionPane.INFORMATION_MESSAGE,
                                               null, ops, ops[0] );
//        OptionPaneWrap.showError( _f, noAccessMsg, title);
    }



   private String getVersionDesc() {
      StringBuffer      announce = new StringBuffer(200);
      ApplicationVersion av=
                 ApplicationVersion.getInstalledApplicationVersion();

      ApplicationVersion newAv= new ApplicationVersion(av.getAppName(),
                                                       Installer.getAutoUpdateUpdateDirFile());

      makeVersion(announce, _appName, null, av.getMajorVersion(),
                  av.getMinorVersion(),
                  av.getRevision(),
                  newAv.getMajorVersion(),
                  newAv.getMinorVersion(),
                  newAv.getRevision() );
      return announce.toString();
   }




   private void showDetailsDialog(List<LocalPackageDesc> localPackages,
                                  boolean updatingJRE) {
       String            detailsTitle= format(_prop.getTitle("detailsDesc"));
       StringBuffer      announce    = new StringBuffer(200);
       Box               box         = Box.createHorizontalBox();


       announce.append(format(_prop.getName("detailsDesc")));
       announce.append("<ol>");
       for(LocalPackageDesc  localPackage: localPackages) {
           if (localPackage.isDownloaded()) {
               announce.append("<li>");
               announce.append(makeCriticaPackagesDesc(localPackage));
               //announce.append(localPackage.getUserDescription());
               announce.append("</li>");
           }
       }
       if (updatingJRE) {
           announce.append("<li>");
           announce.append(format(_prop.getName("jreDesc")));
           announce.append(System.getProperty("java.version"));
           announce.append("</li>");
       }
       announce.append("</ol>");
       _outLabel.setText(announce.toString());
       box.add(_outLabel);
       JScrollPane p= new JScrollPane(box);
       p.setPreferredSize(new Dimension( 400, 400) );
       _outLabel.setMaximumSize( new Dimension( 400, 5000) );
       OptionPaneWrap.showInfo( _f, p, detailsTitle);
   }



   private String makeCriticaPackagesDesc(LocalPackageDesc localPackage) {
      StringBuffer    announce = new StringBuffer(200);
      String          who      = localPackage.getUserDescription();
      SoftwarePackage serverPkg= localPackage.getServerPackage();
      JarVersion      jv       = localPackage.getJarVersion();

      int oldMaj= 0;
      int oldMin= 0;
      int oldRev= 0;
      if (jv != null) {
         oldMaj=  jv.getMajorVersion();
         oldMin=  jv.getMinorVersion();
         oldRev=  jv.getRevision();
      }

      String jarName= (localPackage.getJarFile() != null) ?
                      localPackage.getJarFile().getName() : null;
      if (jarName==null) {
          SoftwarePackage spkg= localPackage.getServerPackage();
          jarName= spkg.getPackageFile().getName();
      }

      makeVersion(announce, who, jarName, oldMaj, oldMin, oldRev,
                  serverPkg.getMajorVersion(),
                  serverPkg.getMinorVersion(),
                  serverPkg.getRevsion() );
      return announce.toString();
   }



   private void makeVersion(StringBuffer announce,
                            String       who,
                            String       jarName,
                            int          oldMaj,
                            int          oldMin,
                            int          oldRev,
                            int          newMaj,
                            int          newMin,
                            int          newRev) {
      //announce.append("<br>");
      announce.append("<b>");
      announce.append(who);
      announce.append("</b>");
      if (jarName!=null) {
         announce.append(" (");
         announce.append(jarName);
         announce.append(")");
      }
      announce.append("<br>" );
      if (oldMaj>0 || oldMin>0 || oldRev>0) {
            announce.append("Old version: ");
            announce.append(oldMaj);
            announce.append("."     );
            announce.append(oldMin);
            announce.append(" Rev " );
            announce.append(oldRev);
      }
      else {
            announce.append("New jar file");
      }
      announce.append("<br>" );
      if (newMaj>0 || newMin>0 || newRev>0) {
            announce.append("New version: ");
            announce.append(newMaj);
            announce.append("."   );
            announce.append(newMin);
            announce.append(" Rev ");
            announce.append(newRev);
      }
      else {
            announce.append("New version: <i>Patch to the same version</i>");
      }
      announce.append("<br>" );
   }
   private String format(String s) {
       return String.format(s, _appName);
   }

    public static class ExpandFeedBack {
        private JFrame _f;
        private JDialog _feedbackDialog;
        private JLabel _feedbackLabel;
        private JarExpander.ExpandStatus _feedbackStatus=
                                      new JarExpander.ExpandStatus() {
                                          public void completedFile(File f, int idx) {
                                              setFeedbackLabel(f.getName(),idx);
                                          }
                                      };
        public ExpandFeedBack(JFrame f) {
            _f= f;
            startExpandingMessage();
        }

        private void startExpandingMessage() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    _feedbackDialog= new JDialog(_f);
                    _feedbackLabel= new JLabel();
                    _feedbackLabel.setOpaque(true);
//                Color alphaColor= new Color(c.getRed(), c.getGreen(),
//                                            c.getBlue(),50);
                    _feedbackLabel.setBackground(EXPAND_COLOR);


                    _feedbackLabel.setFont(_feedbackLabel.getFont().deriveFont(22.0F));
                    setFeedbackLabel("Beginning Expandsion: initializing, opening",0);
                    _feedbackDialog.add(_feedbackLabel);
                    _feedbackDialog.setModal(true);
                    _feedbackDialog.setUndecorated(true);
                    _feedbackDialog.pack();
                    _feedbackDialog.setLocationRelativeTo(_f);
                    _feedbackDialog.setVisible(true);
                }
            });
        }

        public void endExpandingMessage() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    _feedbackDialog.setVisible(false);
                    _feedbackDialog.dispose();
                    _feedbackDialog= null;
                    _feedbackLabel= null;
                }
            });
        }

        public JarExpander.ExpandStatus getExpandingStatus() { return _feedbackStatus; }

        private void setFeedbackLabel(String fname,int cnt) {
            if (_feedbackLabel!=null) {
                _feedbackLabel.setText(String.format(_feedbackStr,cnt,fname));
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
