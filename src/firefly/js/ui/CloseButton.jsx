/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Button, Tooltip} from '@mui/joy';
import React from 'react';
import PropTypes from 'prop-types';

import BACK_ARROW from 'images/icons-2014/16x16_Backward.png';
// import BACK_ARROW from 'images/backButton-start.png';

export function CloseButton({text='Close', tip='Close', style={}, onClick}) {
    return (
        <Tooltip title={tip} style={style}>
            <Button {...{color:'neutral', size:'sm', variant:'solid', onClick, sx:{whiteSpace:'nowrap',pl:0},
                    startDecorator:(<img src={BACK_ARROW}/>) }}>
                {text}
            </Button>
        </Tooltip>
    );
}


CloseButton.propTypes= {
    text : PropTypes.string,
    tip : PropTypes.string,
    style : PropTypes.object,
    onClick : PropTypes.func
};



