/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useRef} from 'react';
import PropTypes from 'prop-types';

export const SimpleCanvas= memo(({drawIt, width, height, id, backgroundColor}) => {
    const {current:canvasRef} = useRef({canvas:undefined});
    const setUpCanvas=(c) => {
        canvasRef.canvas= c;
        drawIt?.(c);
    };
    return ( <canvas width={width+''} height={height+''} id={id} style={{backgroundColor}} ref={setUpCanvas}/> );
});

SimpleCanvas.propTypes= {
    drawIt : PropTypes.func.isRequired,
    width : PropTypes.number.isRequired,
    height : PropTypes.number.isRequired,
    id : PropTypes.string,
    backgroundColor: PropTypes.string
};


