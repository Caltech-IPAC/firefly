/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.searchui;

import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.ui.BaseLayoutElement;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.previews.CoveragePreview;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;

import java.util.HashMap;
import java.util.Map;

/**
 * Date: 7/1/14
 *
 * @author loi
 * @version $Id: $
 */
public class CoverageResultsDisplay extends BaseLayoutElement {


    private final CoveragePreview covPrev;

    public CoverageResultsDisplay() {


        Map<String,String> paramMap= new HashMap<String, String>(7);
        WidgetFactory widgetFactory= Application.getInstance().getWidgetFactory();
        paramMap.put(CommonParams.ENABLE_DEFAULT_COLUMNS, "true");
        paramMap.put(CommonParams.CATALOGS_AS_OVERLAYS, "false");
        paramMap.put("EVENT_WORKER_ID", "target");

        covPrev = (CoveragePreview)widgetFactory.createObserverUI(WidgetFactory.COVERAGE_VIEW,paramMap);
        covPrev.bind(Application.getInstance().getEventHub());

        setContent(covPrev.getDisplay());
        MiniPlotWidget mpw= covPrev.getMPW();
        mpw.addStyleName("standard-border");
        mpw.setMinSize(50, 50);
        mpw.setAutoTearDown(false);
        mpw.setSaveImageCornersAfterPlot(true);
        mpw.setTitleAreaAlwaysHidden(true);
        mpw.setInlineToolbar(true);
        mpw.setUseToolsButton(false);
    }

    @Override
    public void show() {
        super.show();    //To change body of overridden methods use File | Settings | File Templates.
        covPrev.onShow();
    }

    //====================================================================
//
//====================================================================

}
