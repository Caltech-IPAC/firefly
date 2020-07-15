/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {flux} from '../core/ReduxFlux.js';
import {HELP_LOAD} from '../core/AppDataCntlr.js';

export function HelpText({helpId, style, linkText, linkStyle}) {


    const onClick = () => flux.process({ type: HELP_LOAD, payload: {helpId} });

    return (
        <div style={style}>
            <a href="#" className={linkStyle}
                 onClick={onClick}
                 >{linkText}</a>
        </div>);
}

HelpText.propTypes = {
    helpId: PropTypes.string,
    style: PropTypes.object,
    linkText: PropTypes.string,
    linkStyle: PropTypes.object
};
