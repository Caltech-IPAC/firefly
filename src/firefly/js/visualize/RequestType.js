/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';

/**
 * @summary web plot request type
 * @description can be 'SERVICE', 'FILE', 'URL', 'ALL_SKY', 'HiPS', 'BLANK', 'PROCESSOR', 'RAWDATASET_PROCESSOR',
 * 'TRY_FILE_THEN_URL', 'WORKSPACE'
 * @public
 * @global
 */
export const RequestType= new Enum([ 'SERVICE', 'FILE', 'URL', 'ALL_SKY', 'HiPS',
                                     'BLANK', 'PROCESSOR', 'RAWDATASET_PROCESSOR',
                                      'TRY_FILE_THEN_URL', 'WORKSPACE'],
                                     { ignoreCase: true });
