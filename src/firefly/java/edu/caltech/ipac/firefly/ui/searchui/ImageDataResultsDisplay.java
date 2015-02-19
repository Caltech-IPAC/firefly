/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.searchui;

import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.ui.BaseLayoutElement;
import edu.caltech.ipac.firefly.ui.previews.MultiDataViewer;

/**
 * Date: 7/1/14
 *
 * @author loi
 * @version $Id: $
 */
public class ImageDataResultsDisplay extends BaseLayoutElement {

    private MultiDataViewer viewer= new MultiDataViewer();

    public ImageDataResultsDisplay() {

        setContent(viewer.getWidget());
        viewer.bind(Application.getInstance().getEventHub());
    }

    @Override
    public void show() {
        super.show();
        viewer.onShow();
    }

    @Override
    public void hide() {
        super.hide();
        viewer.onHide();
    }

    @Override
    public boolean hasContent() {
        return viewer.hasContent();
    }


    //====================================================================
//
//====================================================================

}
