/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;
/**
 * User: roby
 * Date: 2/10/11
 * Time: 2:18 PM
 */


/**
 * @author Trey Roby
 */
public abstract class BaseNetParams implements  NetParams {

    public String _statusKey;
    public String _plotId;

    public BaseNetParams(String statusKey, String plotId) {
        _statusKey= statusKey;
        _plotId= plotId;
    }

    public abstract String getUniqueString();

    @Override
    public int hashCode() { return toString().hashCode(); }

    @Override
    public boolean equals(Object o) {
        boolean retval= false;
        if (this==o) {
            retval= true;
        }
        else if (o!=null && o instanceof BaseNetParams) {
            BaseNetParams other= (BaseNetParams)o;
            retval= toString().equals(other.toString());
        }
        return retval;
    }

    @Override
    public String toString() { return getUniqueString(); }

    public String getStatusKey() { return _statusKey; }
    public void setStatusKey(String statusKey) { _statusKey= statusKey; }
    public String getPlotId() { return _plotId; }
    public void setPlotId(String plotId) { _plotId= plotId; }

}

