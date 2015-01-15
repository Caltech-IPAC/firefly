/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core.background;

/**
 * Date: Sep 3, 2010
 *
 * @author loi
 * @version $Id: Backgroundable.java,v 1.1 2010/09/14 17:58:07 loi Exp $
 */
public interface Backgroundable extends CanCancel {
    BackgroundStatus getBgStatus();
    void backgrounded();
    boolean canBackground();
}
