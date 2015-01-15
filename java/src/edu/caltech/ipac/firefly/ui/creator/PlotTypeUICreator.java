/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.visualize.ui.PlotTypeUI;

import java.util.Map;

/**
 * Date: Aug 4, 2010
 *
 * @author loi
 * @version $Id: PlotTypeUICreator.java,v 1.1 2010/08/13 19:00:37 roby Exp $
 */
public interface PlotTypeUICreator extends UICreator {
    public PlotTypeUI create(Map<String, String> params);
}
