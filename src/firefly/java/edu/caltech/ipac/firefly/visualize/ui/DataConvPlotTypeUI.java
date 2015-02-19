/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.ui;
/**
 * User: roby
 * Date: 9/8/14
 * Time: 12:46 PM
 */


import java.util.List;

/**
 * @author Trey Roby
 */
public abstract class DataConvPlotTypeUI extends PlotTypeUI {


    public DataConvPlotTypeUI(boolean usesTarget,
                              boolean usesRadius,
                              boolean handlesSubmit,
                              boolean threeColor) {
        super(usesTarget,usesRadius,handlesSubmit,threeColor);
    }


    public abstract List<String> getThreeColorIDs();

    public boolean getProducesIDsOnly() { return true; }

    public void reinitUI(String id) {}

}

