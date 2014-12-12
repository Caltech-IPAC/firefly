package edu.caltech.ipac.client.net;

import edu.caltech.ipac.gui.IconFactory;
import edu.caltech.ipac.util.action.ClassProperties;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Show the network status icon in a label.  Update it as the network status
 * changes.
 *
 * @author Trey Roby
 */
public class NetStatusDisplay {
    private final static String NET_UP    = "resources/network.gif";
    private final static String NET_DOWN  = "resources/network_down.gif";

    private final static ClassProperties _prop= new ClassProperties(
                                                     NetStatusDisplay.class);
    private final static String NETUP      = _prop.getName("netUp");
    private final static String NETUP_TIP  = _prop.getTip ("netUp");
    private final static String NETDOWN    = _prop.getName("netDown");
    private final static String NETDOWN_TIP= _prop.getName("netDown");

    private Icon   _netUpIcon  = null;
    private Icon   _netDownIcon= null;
    private JLabel _netLabel   = new JLabel();

    public NetStatusDisplay() {
        IconFactory factory= IconFactory.getInstance();
        factory.addResource(getClass());
        _netUpIcon=  factory.getIcon(NET_UP);
        _netDownIcon=factory.getIcon(NET_DOWN);
        addNetListener();
        changeNetDisplay(NetworkManager.getInstance().getNetworkStatus());
    }

    public JComponent getComponent() { return _netLabel; }

    protected void addNetListener() {
       NetworkManager netMan= NetworkManager.getInstance();
       netMan.addPropertyChangeListener( new PropertyChangeListener() {
           public void propertyChange(PropertyChangeEvent ev) {
               if (ev.getPropertyName().equals("networkStatus")) {
                   changeNetDisplay( ((Integer)ev.getNewValue()).intValue());
               }
           }
       } );
    }


    /**
     * Show if the network is available or not.  Because this method may be
     * call in a separate thread if uses an involkLater method
     */
    protected void changeNetDisplay(int netMode) {
        if (netMode == NetworkManager.NET_UP) {
             SwingUtilities.invokeLater(new Runnable() {
                  public void run() { 
                         _netLabel.setText(NETUP); 
                         _netLabel.setToolTipText(NETUP_TIP);
                         _netLabel.setIcon(_netUpIcon);
                  } });
        }
        else if (netMode == NetworkManager.NET_DOWN) {
             SwingUtilities.invokeLater(new Runnable() {
                  public void run() { 
                         _netLabel.setText(NETDOWN); 
                         _netLabel.setToolTipText(NETDOWN_TIP);
                         _netLabel.setIcon(_netDownIcon);
                  } });
        }
        else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    _netLabel.setText("");
                    _netLabel.setToolTipText("");
                    _netLabel.setIcon(null);
                } });
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
