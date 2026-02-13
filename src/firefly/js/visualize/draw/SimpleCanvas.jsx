/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useRef} from 'react';
import PropTypes from 'prop-types';

export const SimpleCanvas= memo(({drawIt: drawInit, width=0, height=0, id, backgroundColor}) => {
    "use no memo";
    const {current:canvasRef} = useRef({canvas:undefined});
    const setUpCanvas=(c) => {
        canvasRef.canvas= c;
        drawInit?.(c);
    };
    // console.log(canvasRef?.canvas);
    return ( <canvas width={width+''} height={height+''} id={id} style={{backgroundColor}} ref={setUpCanvas}/> );
});

SimpleCanvas.propTypes= {
    drawIt : PropTypes.func.isRequired,
    width : PropTypes.number.isRequired,
    height : PropTypes.number.isRequired,
    id : PropTypes.string,
    backgroundColor: PropTypes.string
};


