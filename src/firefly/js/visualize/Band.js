/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';

/**
 * A Band in a plot
 * must be enum 'RED', 'GREEN', 'BLUE', 'NO_BAND'
 * @public
 * @global
 */
export const Band= new Enum({'RED':0, 'GREEN':1, 'BLUE':2, 'NO_BAND':0}, {ignoreCase:true});

export const allBandAry= [Band.RED,Band.GREEN,Band.BLUE];
