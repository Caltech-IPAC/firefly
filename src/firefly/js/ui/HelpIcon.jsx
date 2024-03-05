/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Link} from '@mui/joy';
import React from 'react';
import {string, object, oneOf, any} from 'prop-types';
import {flux} from '../core/ReduxFlux.js';
import {ToolbarButton} from './ToolbarButton.jsx';
import {HELP_LOAD} from '../core/AppDataCntlr.js';
import HelpOutline from '@mui/icons-material/HelpOutline';

const onClick = (element,ev,helpId) => {
    ev.stopPropagation();
    flux.process({ type: HELP_LOAD, payload: {helpId} });
};

export const HelpIcon= ({helpId, size='small', style={}, sx, component}) => (
    <ToolbarButton {...{style, sx, icon:<HelpOutline/>, onClick: (e,ev)=> onClick(e,ev,helpId), component }}/>
);

export const HelpText= ({helpId, text, size='small', sx}) => (
    <Link {...{sx, size,icon:<HelpOutline/>, onClick: (ev)=> onClick(undefined,ev,helpId),}}>
        {text}
    </Link>
);

HelpIcon.propTypes = {
    helpId: string,
    size:   oneOf(['small', 'large']),
    style: object,
    sx: object,
    component: any,
};

export default HelpIcon;