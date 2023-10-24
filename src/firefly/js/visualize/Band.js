/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import Enum from 'enum';

/**
 * @typedef {Object} Band
 * A Band in an image plot
 * must be enum 'RED', 'GREEN', 'BLUE', 'NO_BAND'
 * @prop RED
 * @prop GREEN
 * @prop BLUE
 * @prop NO_BAND
 * @type {Enum}
 * @public
 * @global
 */

/** @type {Band} */
export const Band= new Enum({'RED':0, 'GREEN':1, 'BLUE':2, 'NO_BAND':0}, {ignoreCase:true});

/** @type {Array.<Band>} */
export const allBandAry= [Band.RED,Band.GREEN,Band.BLUE];
