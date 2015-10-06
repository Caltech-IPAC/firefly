/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 11:16:27 AM
 */

/*jshint esnext:true*/
import Enum from "enum";
const RequestType= new Enum([ "SERVICE",
                              "FILE",
                              "URL",
                              "ALL_SKY",
                              "BLANK",
                              "PROCESSOR",
                              "RAWDATASET_PROCESSOR",
                              "TRY_FILE_THEN_URL"]);

export default RequestType;
