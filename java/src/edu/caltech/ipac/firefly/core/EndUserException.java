/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;
/**
 * User: roby
 * Date: Nov 9, 2009
 * Time: 5:18:11 PM
 */


/**
 * @author Trey Roby
 */
public class EndUserException extends Exception {

    private final String _endUserMsg;
    private final String _moreDetail;

    public EndUserException(String endUserMsg, String moreDetail) {
        this(endUserMsg,moreDetail,null);

    }

    public EndUserException(String endUserMsg, String detailMsg, Throwable cause) {
        super(endUserMsg+": "+detailMsg, cause);
        _endUserMsg= endUserMsg;
        _moreDetail = detailMsg;
    }

    public final String getEndUserMsg() { return _endUserMsg; }
    public final String getMoreDetailMsg() { return _moreDetail; }

}


