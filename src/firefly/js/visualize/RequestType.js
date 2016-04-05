/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
export const RequestType= new Enum([ 'SERVICE', 'FILE', 'URL', 'ALL_SKY',
                                     'BLANK', 'PROCESSOR', 'RAWDATASET_PROCESSOR',
                                      'TRY_FILE_THEN_URL']);
