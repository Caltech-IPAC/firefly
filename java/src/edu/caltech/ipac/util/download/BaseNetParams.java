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

    public final String _statusKey;


    public BaseNetParams() {this(null);}

    public BaseNetParams(String statusKey) {_statusKey= statusKey;}

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

}

