package edu.caltech.ipac.firefly.ui.table;

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
public class ImageResultsDisplay extends BaseLayoutElement {


    private final CoveragePreview covPrev;

    public ImageResultsDisplay() {


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


//====================================================================
//
//====================================================================

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED visSTATES UCC 2312-2313)
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
