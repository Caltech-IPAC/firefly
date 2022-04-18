/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import nom.tam.fits.Header;

import java.util.List;

/**
 * @author Trey Roby
 * Date: Sep 11, 2009
 */
public record WebPlotInitializer(PlotState plotState, CoordinateSys imageCoordSys,
                                 Header[] headerAry, Header[] zeroHeaderAry, int dataWidth,
                                 int dataHeight, WebFitsData[] fitsData,
                                 String desc, String dataDesc, List<RelatedData> relatedData) { }

