/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {flux} from '../Firefly.js';
import {HELP_LOAD} from '../core/AppDataCntlr.js';

export function HelpText({helpId, linkText, linkStyle}) {


    const onClick = () => flux.process({ type: HELP_LOAD, payload: {helpId} });

    return (
        <div>
            <a href="#" className={linkStyle}
                 onClick={onClick}
                 >{linkText}</a>
        </div>);
}

HelpText.propTypes = {
    helpId: PropTypes.string,
    linkText: PropTypes.string,
    linkStyle: PropTypes.object
};
