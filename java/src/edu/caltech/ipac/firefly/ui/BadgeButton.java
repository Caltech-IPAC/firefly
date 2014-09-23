package edu.caltech.ipac.firefly.ui;
/**
 * User: roby
 * Date: 5/6/14
 * Time: 11:46 AM
 */


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

/**
* @author Trey Roby
*/
public class BadgeButton {
    private final FlowPanel panel= new FlowPanel();
    private Badge badge=  null;


    public BadgeButton(Image image) { this(image,null,true);  }

    public BadgeButton(Image image, String styleName, boolean backgroundIsDark) {
        init(styleName, backgroundIsDark);
        setIcon(image);
    }

    public BadgeButton(String text) { this(text,null);  }

    public BadgeButton(String text, String styleName) {
        init(styleName,false);
        setText(text);
    }

    private void init(String styleName, boolean backgroundIsDark) {
        panel.setStyleName("firefly-v2-MenuItem");
        if (backgroundIsDark) panel.addStyleName("firefly-v2-MenuItem-dark");
        else                  panel.addStyleName("firefly-v2-MenuItem-light");
        if (styleName!=null) panel.addStyleName(styleName);
    }

    public void setIcon(Image image) {
        panel.clear();
        panel.add(image);
        if (getHasBadge()) panel.add(badge.badgeHTML);
    }



    public void setText(String text) {
        panel.clear();
        Label l= new Label(text);
        l.setStyleName("menuItemText");
        panel.add(l);
        if (getHasBadge()) panel.add(badge.badgeHTML);
    }
    public Widget getWidget() { return panel; }

    public boolean isEnabled() {
        return !panel.getStyleName().contains("firefly-MenuItem-v2-disabled");
    }

    public void setEnabled(boolean enabled) {
        if (enabled)  panel.removeStyleName("firefly-MenuItem-v2-disabled");
        else          panel.addStyleName("firefly-MenuItem-v2-disabled");
    }

    public void setAttention(boolean attention) {
        if (attention)  panel.addStyleName("firefly-MenuItem-v2-attention");
        else            panel.removeStyleName("firefly-MenuItem-v2-attention");
    }

    public void setTitle(String tip) { panel.setTitle(tip); }

    public void setBadgeCount(int cnt) {
        if (cnt==0 && getHasBadge()) {
            this.panel.remove(this.badge.badgeHTML);
            badge.badgeHTML= null;
        }

        if (cnt>0) {
            if (badge==null) badge= new Badge();
            if (badge.badgeHTML==null) {
                this.badge.badgeHTML= new HTML(cnt+"");
                panel.add(this.badge.badgeHTML);
                badge.badgeHTML.setStyleName("firefly-v2-badge");
            }
            else {
                badge.badgeHTML.setHTML(cnt+"");
            }
            badge.badgeHTML.removeStyleName("firefly-v2-badge-1-digit");
            badge.badgeHTML.removeStyleName("firefly-v2-badge-2-digit");
            if (cnt>9) {
                badge.badgeHTML.addStyleName("firefly-v2-badge-2-digit");
            }
            else {
                badge.badgeHTML.addStyleName("firefly-v2-badge-1-digit");
            }
            updateOffsets();
        }
    }

    private void updateOffsets() {
        if (getHasBadge()) {
            if (badge.useDiffOffset) {
                GwtUtil.setStyle(badge.badgeHTML, "top", badge.offsetY+"px");
            }
            else {
                GwtUtil.setStyle(badge.badgeHTML, "top", "");
            }
        }

    }

    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return panel.addDomHandler( handler, ClickEvent.getType());
    }

    public static Widget makeBadge(int cnt) {
        Widget badge= new HTML(cnt+"");
        badge.setStyleName("firefly-v2-badge");
        if (cnt>9) {
            badge.addStyleName("firefly-v2-badge-2-digit");
        }
        else {
            badge.addStyleName("firefly-v2-badge-1-digit");

        }
        return badge;
    }

    private boolean getHasBadge() {
        return badge!=null && badge.badgeHTML!=null;
    }

    public void setBadgeYOffset(int offsetY) {
        if (badge==null) badge= new Badge();
        badge.useDiffOffset= true;
        badge.offsetY= offsetY;
        updateOffsets();
    }

    private static class Badge {
        HTML badgeHTML= null;
        boolean useDiffOffset=false;
        int offsetY= 0;

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
