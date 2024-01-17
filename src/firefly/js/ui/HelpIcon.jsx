/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {string, object, oneOf, any} from 'prop-types';
import {flux} from '../core/ReduxFlux.js';
import {ToolbarButton} from './ToolbarButton.jsx';
import {HELP_LOAD} from '../core/AppDataCntlr.js';
import HelpOutline from '@mui/icons-material/HelpOutline';


export function HelpIcon({helpId, size='small', style={}, sx, component}) {
    const onClick = (element,ev) => {
        ev.stopPropagation();
        flux.process({ type: HELP_LOAD, payload: {helpId} });
    };

    return <ToolbarButton {...{style, sx, icon:<HelpOutline/>, onClick, component }}/> ;
}

HelpIcon.propTypes = {
    helpId: string,
    size:   oneOf(['small', 'large']),
    style: object,
    sx: object,
    component: any,
};

export default HelpIcon;