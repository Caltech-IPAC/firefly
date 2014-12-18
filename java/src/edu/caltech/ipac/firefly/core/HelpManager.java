package edu.caltech.ipac.firefly.core;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.util.StringUtils;

/**
 * Date: Oct 2, 2009
 *
 * @author loi
 * @version $Id: HelpManager.java,v 1.32 2012/11/14 18:40:22 loi Exp $
 */
public class HelpManager {
    static String HELP_BASE_URL = Application.getInstance().getProperties().getProperty("help.base.url");
    private String appHelpName= Application.getInstance().getAppName();

    public void showHelp() {
        showHelpAt(null);
    }

    public void showHelpAt(String helpId) {
        showHelpAt(helpId, appHelpName);
    }

    public void showHelpAt(String helpId, String appName) {
        String url = HELP_BASE_URL;
        url = url.endsWith("/") ? url : url + "/";

        if (!StringUtils.isEmpty(appName)) {
            url += appName + "/";
        }
        if (!StringUtils.isEmpty(helpId)) {
            url += "#id=" + helpId;
        }
        GwtUtil.open(url, "Online Help");
    }

    public void setAppHelpName(String name) {
        appHelpName= name;
    }

    public static Widget makeHelpIcon(String helpId) {
        return makeHelpIcon(helpId, true);
    }

    public static Widget makeHelpIcon(String helpId, boolean isDark) {
        return new HelpIcon(helpId, makeHelpImage(isDark));
    }

    public static Image makeHelpImage() {
        return makeHelpImage(true);
    }

    public static Image makeHelpImage(boolean isDark) {
        Image img= new Image(IconCreator.Creator.getInstance().getHelpSmall());
        DOM.setStyleAttribute(img.getElement(), "cursor", "pointer");
        return img;
    }

    public static class HelpIcon extends Composite {
        private String helpId;

        public HelpIcon() {
            this(null, null);
        }

        public HelpIcon(String helpId, Image img) {
            this.helpId = helpId;
            if (img == null) {
                img = makeHelpImage();
            }

            initWidget(img);

            img.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    Application.getInstance().getHelpManager().showHelpAt(getHelpId());
                }
            });
        }

        public String getHelpId() {
            return helpId;
        }

        public void setHelpId(String helpId) {
            this.helpId = helpId;
        }
    }

}
