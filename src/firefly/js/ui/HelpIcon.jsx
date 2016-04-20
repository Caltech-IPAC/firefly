/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {flux} from '../Firefly.js';
import {HELP_LOAD} from '../core/AppDataCntlr.js';

import largeHelp from 'html/images/icons-2014/Help.png';
import smallHelp from 'html/images/icons-2014/Help-16x16.png';

import './HelpIcon.css';

export function HelpIcon({helpId, size='small'}) {
    var imgSrc = (size === 'small') ? smallHelp : largeHelp;

    var onClick = () => {
        flux.process({
            type: HELP_LOAD,
            payload: {helpId}
        });
    };

    return (
        <div>
            <img className={'helpicon'}
                 onClick={onClick}
                 src={imgSrc}/>
        </div>);
}

HelpIcon.propTypes = {
    helpId: PropTypes.string,
    size:   PropTypes.oneOf(['small', 'large'])
};


export default HelpIcon;