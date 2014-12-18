package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 2/29/12
 * Time: 10:12 AM
 */


/**
 * @author Trey Roby
 */
public interface AsyncDataLoader {

    public void requestLoad(LoadCallback cb);
    public void disableLoad();
    public boolean isDataAvailable();
    public void markStale();

}

