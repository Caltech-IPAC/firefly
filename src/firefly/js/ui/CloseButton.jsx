/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Button, Tooltip} from '@mui/joy';
import React from 'react';
import PropTypes from 'prop-types';

import ArrowBack from '@mui/icons-material/ArrowBackIosNew';

export function CloseButton({text='Close', tip='Close', style={}, onClick}) {
    return (
        <Tooltip title={tip} style={style}>
            <Button {...{color:'neutral', size:'sm', variant:'solid', height:'1rem', onClick, sx:{whiteSpace:'nowrap',pl:'0.5rem'},
                    startDecorator:<ArrowBack/>}}>
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



