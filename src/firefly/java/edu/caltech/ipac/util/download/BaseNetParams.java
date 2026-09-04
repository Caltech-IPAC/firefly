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
    public String id;
    public boolean notify= true;

    public BaseNetParams(String statusKey, String id) {
        _statusKey= statusKey;
        this.id = id;
    }

    public abstract String getUniqueString();

    @Override
    public int hashCode() { return toString().hashCode(); }

    @Override
    public boolean equals(Object o) {
        if (this==o) {
            return true;
        }
        else if (o instanceof BaseNetParams other) {
            return toString().equals(other.toString());
        }
        return false;
    }

    @Override
    public String toString() { return getUniqueString(); }

    public String getStatusKey() { return _statusKey; }
    public void setStatusKey(String statusKey) { _statusKey= statusKey; }
    public String getId() { return id; }
    public void setId(String plotId) { id = plotId; }
    public void setNotify(boolean notify) { this.notify= notify; }
    public boolean getNotify() { return notify; }

}

