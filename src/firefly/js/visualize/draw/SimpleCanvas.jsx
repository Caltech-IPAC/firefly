/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useCallback} from 'react';
import PropTypes from 'prop-types';

export const SimpleCanvas= ({drawIt: drawInit, width=0, height=0, id, backgroundColor}) => {
    const setUpCanvas=useCallback( (c) => drawInit?.(c), [drawInit]);
    return ( <canvas width={width+''} height={height+''} id={id} style={{backgroundColor}} ref={setUpCanvas}/> );
};

SimpleCanvas.propTypes= {
    drawIt : PropTypes.func.isRequired,
    width : PropTypes.number.isRequired,
    height : PropTypes.number.isRequired,
    id : PropTypes.string,
    backgroundColor: PropTypes.string
};


