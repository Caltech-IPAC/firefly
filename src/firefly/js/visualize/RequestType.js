/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';

/**
 * @typedef {Object} RequestType
 * @summary web plot request type
 * @description can be 'SERVICE', 'FILE', 'URL', 'ALL_SKY', 'HiPS', 'PROCESSOR', 'TRY_FILE_THEN_URL', 'WORKSPACE', ''S3_AS_LOCAL'
 *
 * @prop SERVICE
 * @prop FILE
 * @prop URI
 * @prop ALL_SKY
 * @prop HiPS
 * @prop PROCESSOR
 * @prop TRY_FILE_THEN_URL
 * @prop WORKSPACE
 * @prop S3_AS_LOCAL
 * @prop get
 * @public
 * @global
 */

/** @type RequestType */
export const RequestType= new Enum([ 'SERVICE', 'FILE', 'URI', 'ALL_SKY', 'HiPS',
        'PROCESSOR', 'TRY_FILE_THEN_URL', 'WORKSPACE'],
                                     { ignoreCase: true });