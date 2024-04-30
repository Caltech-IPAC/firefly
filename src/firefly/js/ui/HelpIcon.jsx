/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Link} from '@mui/joy';
import React from 'react';
import PropTypes, {any, object, string} from 'prop-types';
import {flux} from '../core/ReduxFlux.js';
import {ToolbarButton} from './ToolbarButton.jsx';
import {HELP_LOAD} from '../core/AppDataCntlr.js';
import HelpOutline from '@mui/icons-material/HelpOutline';
import {useColorMode} from 'firefly/ui/FireflyRoot.jsx';

const onClick = (ev,helpId, isDarkMode,element) => {
    ev.stopPropagation();
    flux.process({ type: HELP_LOAD, payload: {helpId, isDarkMode} });
};

export const HelpIcon= ({helpId, component, ...props}) => {
    const {isDarkMode} = useColorMode();
    return (
        <ToolbarButton icon={<HelpOutline/>} component={component } {...props}
                       onClick={(e,ev)=> onClick(ev, helpId, isDarkMode, e)}/>
    );
};

HelpIcon.propTypes = {
    helpId: string,
    style: object,
    component: any,
    ...ToolbarButton.propTypes
};


export const HelpText= ({helpId, text, size='small', sx}) => {
    const {isDarkMode} = useColorMode();
    return (
        <Link {...{sx, size,icon:<HelpOutline/>, onClick: (ev)=> onClick(ev, helpId, isDarkMode),}}>
            {text}
        </Link>
    );
};


export function HelpLink({helpId, linkText, ...props}) {

    const {isDarkMode} = useColorMode();
    return (
        <Link onClick={(ev) => onClick(ev, helpId, isDarkMode)} {...props}>
            {linkText}
        </Link>
    );
}

HelpLink.propTypes = {
    helpId: PropTypes.string,
    linkText: PropTypes.string,
    ...Link.propTypes
};


export default HelpIcon;

