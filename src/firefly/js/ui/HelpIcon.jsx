/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {flux} from '../Firefly.js';
import appDataCntlr from '../core/AppDataCntlr.js';

import largeHelp from 'html/images/icons-2014/Help.png';
import smallHelp from 'html/images/icons-2014/Help-16x16.png';

import './HelpIcon.css';

function HelpIcon({helpId, size='small'}) {
    var imgSrc = (size === 'small') ? smallHelp : largeHelp;

    var onClick = () => {
        flux.process({
            type: appDataCntlr.HELP_LOAD,
            payload: {helpId: helpId}
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
    size:   function(props, propName, componentName) {
            if (props[propName] && !/^(small|large)$/.test(props[propName])){
                return new Error(`Invalid size, should be either small or large`);
            }
    }
};


export default HelpIcon;