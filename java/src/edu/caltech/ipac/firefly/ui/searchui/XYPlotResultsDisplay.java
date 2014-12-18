package edu.caltech.ipac.firefly.ui.searchui;

import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.ui.BaseLayoutElement;
import edu.caltech.ipac.firefly.ui.previews.XYPlotter;

/**
 * Date: 7/1/14
 *
 * @author loi
 * @version $Id: $
 */
public class XYPlotResultsDisplay extends BaseLayoutElement {

    private final XYPlotter xy= new XYPlotter(Application.getInstance().getEventHub());

    public XYPlotResultsDisplay() {
        setContent(xy.getWidget());
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void hide() {
        super.show();
    }



//====================================================================
//
//====================================================================

}
