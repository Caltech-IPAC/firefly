/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * User: roby
 * Date: Apr 2, 2009
 * Time: 9:18:47 AM
 */


import Enum from 'enum';

export const BackgroundState  = new Enum([
    /**
     * WAITING - Waiting to start request
     */
    'WAITING',
    /**
     * STARTING - Starting to package - used only on client side, before getting first status
     */
    'STARTING',
    /**
     * server is working on the packaging
     */
    'WORKING',
    /**
     * server is notifying of more data
     */
    'NEW_DATA',
    /**
     * USER_ABORTED - user aborted the package - should only be set on client side
     */
    'USER_ABORTED',
    /**
     * FAIL - server failed to package request
     */
    'FAIL',
    /**
     * SUCCESS - server successfully packaged request
     */
    'SUCCESS',
    /**
     * CANCELED - packaging was canceled, set by server when checking the canceled flag
     */
    'CANCELED',
    /**
     * UNKNOWN_PACKAGE_ID - a status used for a request with a unknown package id
     */
    'UNKNOWN_PACKAGE_ID'
]);



export default BackgroundState;
