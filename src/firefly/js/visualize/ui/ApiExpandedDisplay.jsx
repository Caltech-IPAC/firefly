/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {ImageExpandedMode} from '../iv/ImageExpandedMode.jsx';

export const ApiExpandedDisplay= memo( ({closeFunc=undefined, viewerId}) => {

    return (
        <div style={{width:'100%', height:'100%', display:'flex', flexWrap:'nowrap',
            alignItems:'stretch', flexDirection:'column'}}>
            <div style={{flex: '1 1 auto', display:'flex'}}>
                <ImageExpandedMode   {...{key:'results-plots-expanded', closeFunc, viewerId}}/>
            </div>
        </div>
    );
});

ApiExpandedDisplay.propTypes= {
    forceExpandedMode : PropTypes.bool,
    closeFunc: PropTypes.func,
    viewerId: PropTypes.string,
};
