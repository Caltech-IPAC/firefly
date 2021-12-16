/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';

/**
 * @typedef RequestType
 * @summary web plot request type
 * @description can be 'SERVICE', 'FILE', 'URL', 'ALL_SKY', 'HiPS', 'BLANK', 'PROCESSOR', 'RAWDATASET_PROCESSOR',
 * 'TRY_FILE_THEN_URL', 'WORKSPACE'
 *
 * @prop SERVICE
 * @prop FILE
 * @prop URL
 * @prop ALL_SKY
 * @prop HiPS
 * @prop BLANK
 * @prop PROCESSOR
 * @prop RAWDATASET_PROCESSOR
 * @prop TRY_FILE_THEN_URL
 * @prop WORKSPACE
 * @type {Enum}
 * @public
 * @global
 */
export const RequestType= new Enum([ 'SERVICE', 'FILE', 'URL', 'ALL_SKY', 'HiPS',
                                     'BLANK', 'PROCESSOR', 'RAWDATASET_PROCESSOR',
                                      'TRY_FILE_THEN_URL', 'WORKSPACE'],
                                     { ignoreCase: true });
