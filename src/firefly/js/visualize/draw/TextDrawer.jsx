/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import CanvasWrapper from './CanvasWrapper.jsx';




function generateText(textDrawAry) {
    return textDrawAry.map( (t,idx) => {
        return  ( <div style={t.style} key={idx}>{t.text}</div>);
    });
}





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

