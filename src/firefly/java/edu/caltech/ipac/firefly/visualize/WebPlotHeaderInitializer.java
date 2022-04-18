package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.visualize.plot.RangeValues;
import nom.tam.fits.Header;

import java.util.Map;

/**
 * @author Trey Roby
 * Date: 2019-04-09
 */
public record WebPlotHeaderInitializer(String originalFitsFileStr, String workingFitsFileStr,
                                       String uploadFileNameStr, RangeValues rv,
                                       String dataDesc, boolean multiImageFile,
                                       boolean threeColor, WebPlotRequest request, Header[] zeroHeaderAry,
                                       Map<String,String> attributes) {}

