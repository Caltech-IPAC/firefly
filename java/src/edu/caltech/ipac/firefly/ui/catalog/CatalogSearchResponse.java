package edu.caltech.ipac.firefly.ui.catalog;


/**
 * User: roby
 * Date: May 13, 2010
 * Time: 12:35:30 PM
 */

public interface CatalogSearchResponse {
    public enum RequestStatus { BACKGROUNDING, SUCCESS, FAILED}
    public void showNoRowsReturned();
    public void status(RequestStatus requestStatus);
}
