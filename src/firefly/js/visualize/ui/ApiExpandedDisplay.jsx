/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Stack} from '@mui/joy';
import React, {memo} from 'react';
import {func,string} from 'prop-types';
import {ImageExpandedMode} from '../iv/ImageExpandedMode.jsx';

export const ApiExpandedDisplay= memo( ({closeFunc, viewerId}) => (
        <Stack {...{width:1, height:1, flexWrap:'nowrap', alignItems:'stretch'}}>
            <Stack {...{flex: '1 1 auto', direction:'row'}}>
                <ImageExpandedMode   {...{key:'results-plots-expanded', closeFunc, viewerId}}/>
            </Stack>
        </Stack>
    ));

ApiExpandedDisplay.propTypes= {
    closeFunc: func,
    viewerId: string,
};
