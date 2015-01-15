/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;
/**
 * User: roby
 * Date: Apr 21, 2010
 * Time: 4:16:42 PM
 */


/**
 * @author Trey Roby
 */
public abstract class LoadStatus {

    boolean success;

    public LoadStatus() {

    }

    public void setSuccess(boolean success)  {  this.success= success; }
    public boolean isSuccess(boolean success)  {  return success; }

    public abstract void onComplete();

}

