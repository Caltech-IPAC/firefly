/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';

function generateText(textDrawAry) {
    return textDrawAry.map( (t,idx) => ( <div style={t.style} key={idx}>{t.text}</div>) );
}

/**
 * React component the displays absolute divs with text.  Used as labels for drawing
 * @param textDrawAry
 * @param width
 * @param height
 * @return {object}
 */
function TextDrawer({textDrawAry,width,height}) {
    var style= {position:'absolute',left:0,right:0,width,height};
    if (!textDrawAry || !textDrawAry.length) return (<div className='textDrawer' style={style}/>);

    return (
        <div className='textDrawer' style={style}>
            {generateText(textDrawAry)}
        </div>
    );
}

TextDrawer.propTypes= {
    textDrawAry : PropTypes.array.isRequired,
    width : PropTypes.number.isRequired,
    height : PropTypes.number.isRequired
};

export default TextDrawer;
