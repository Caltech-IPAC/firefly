package edu.caltech.ipac.firefly.ui.table;

import edu.caltech.ipac.firefly.ui.creator.ResultUIComponent;

/**
 * Date: Feb 20, 2009
 *
 * @author loi
 * @version $Id: TablePreview.java,v 1.12 2010/10/07 23:41:22 roby Exp $
 */
public interface TablePreview extends ResultUIComponent {

    void setPreviewVisible(boolean v);
    void setID(String id);
    String getID();

}
