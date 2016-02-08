/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import './ToolbarButton.css';


import BUTTON_START from 'html/images/backButton-start.png';
import BUTTON_MIDDLE from 'html/images/backButton-middle.png';
import BUTTON_END from 'html/images/backButton-end.png';

const middleStyle= {
    display: 'inline-block',
    lineHeight: '30px',
    fontSize: '12pt',
    background: `url(${BUTTON_MIDDLE}) top left repeat-x`,
    color: 'white',
    verticalAlign:'baseline'
};

/**
 *
 * @param text
 * @param tip
 * @param onClick
 * @param style
 * @return react object
 */
export function CloseButton({text='Close', tip='Close',style={}, onClick}) {
    var s= Object.assign({cursor:'pointer', verticalAlign:'bottom'},style);
    return (
        <div style={s} title={tip} onClick={onClick}>
            <img style={{verticalAlign:'bottom'}} src={BUTTON_START} />
            <div style={middleStyle} title={tip}>{text}</div>
            <img style={{verticalAlign:'bottom'}} src={BUTTON_END} />
        </div>
    );
}


//CloseButton.contextTypes= {
//};


CloseButton.propTypes= {
    text : PropTypes.string,
    style : PropTypes.object,
    tip : PropTypes.string,
    onClick : PropTypes.func
};



