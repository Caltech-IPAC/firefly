/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.ui.table.TablePreview;

import java.util.Map;

/**
 * Date: Mar 25, 2010
 *
 * @author loi, Trey
 * @version $Id: ObsResultCreator.java,v 1.1 2010/04/20 21:15:27 roby Exp $
 */
public interface ObsResultCreator extends UICreator {

    TablePreview create(Map<String, String> params);

}
