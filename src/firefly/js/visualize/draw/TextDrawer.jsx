/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';

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
    textDrawAry : React.PropTypes.array.isRequired,
    width : React.PropTypes.number.isRequired,
    height : React.PropTypes.number.isRequired
};

export default TextDrawer;
