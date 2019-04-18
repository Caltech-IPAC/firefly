/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';
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
 * @param p
 * @param p.text
 * @param p.tip
 * @param p.style
 * @param p.onClick
 * @return react object
 */
export function CloseButton({text='Close', tip='Close', style={}, onClick}) {
    const s= Object.assign({cursor:'pointer', verticalAlign:'baseline', whiteSpace:'nowrap'},style);
    return (
        <div style={s} title={tip} onClick={onClick}>
            <img style={{verticalAlign:'bottom'}} src={BUTTON_START} />
            <div style={middleStyle} title={tip}>{text}</div>
            <img style={{verticalAlign:'bottom'}} src={BUTTON_END} />
        </div>
    );
}

CloseButton.propTypes= {
    text : PropTypes.string,
    tip : PropTypes.string,
    style : PropTypes.object,
    onClick : PropTypes.func
};



