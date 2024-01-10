/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {string, object, oneOf} from 'prop-types';
import {flux} from '../core/ReduxFlux.js';
import {ToolbarButton} from './ToolbarButton.jsx';
import {HELP_LOAD} from '../core/AppDataCntlr.js';

import largeHelp from 'html/images/icons-2014/Help.png';
import smallHelp from 'html/images/icons-2014/Help-16x16.png';

export function HelpIcon({helpId, size='small', style={}, sx}) {
    const onClick = (element,ev) => {
        ev.stopPropagation();
        flux.process({ type: HELP_LOAD, payload: {helpId} });
    };

    return <ToolbarButton {...{style, sx, icon:size==='small' ? smallHelp : largeHelp, onClick }}/> ;
}

HelpIcon.propTypes = {
    helpId: string,
    size:   oneOf(['small', 'large']),
    style: object,
    sx: object
};

export default HelpIcon;