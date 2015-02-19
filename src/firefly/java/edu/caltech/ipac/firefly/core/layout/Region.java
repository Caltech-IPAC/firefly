/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core.layout;

import com.google.gwt.user.client.ui.Widget;

/**
 * Date: Jun 11, 2008
 *
 * @author loi
 * @version $Id: Region.java,v 1.11 2011/10/12 17:28:53 loi Exp $
 */
public interface Region {

    String getId();
    String getTitle();
    void setCollapsedTitle(String title);
    void setExpandedTitle(String title);
    Widget getDisplay();
    void setDisplay(Widget display);
    void show();
    void hide();
    boolean isCollapsible();
    void collapse();
    void expand();
    int getMinHeight();
    void setMinHeight(int minHeight);
    Widget getContent();
    void clear();
    void setInlineBlock(boolean inlineBlock);
}
