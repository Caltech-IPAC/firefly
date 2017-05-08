/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {flux} from '../Firefly.js';
import {HELP_LOAD} from '../core/AppDataCntlr.js';

export function HelpText({helpId, linkText, linkStyle}) {


    var onClick = () => {
        flux.process({
            type: HELP_LOAD,
            payload: {helpId}
        });
    };

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


export default HelpText;