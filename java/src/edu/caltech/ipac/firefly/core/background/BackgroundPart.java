/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core.background;

/**
 * User: roby
 * Date: Aug 20, 2010
 * Time: 12:04:38 PM
 */


import java.io.Serializable;

/**
 * @author Trey Roby
 */
public interface BackgroundPart extends Serializable {


    public BackgroundState getState();

    public boolean hasFileKey();
    public String getFileKey();

}

