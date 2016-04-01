/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import './LinkButton.css';

const labelStyle= {
    display: 'inline-block',
    lineHeight: '30px',
    fontSize: '12pt',
    color: 'white',
    verticalAlign:'baseline'
};

const messageStyle={
    fontSize: '10pt',
    lineHeight: '14pt'
}

/**
 *
 * @param text
 * @param tip
 * @param onClick
 * @param style
 * @return react object
 */
export function LinkButton({text, tip='$text',style={}, onClick}) {
    var s= Object.assign({cursor:'pointer', verticalAlign:'bottom'},style);
    return (
        <div style={s} title={tip} onClick={onClick}>

            <div style={labelStyle} title={tip}>
                <href>{text}</href>
            </div>

        </div>
    );
}


//CloseButton.contextTypes= {
//};


LinkButton.propTypes= {
    text : PropTypes.string,
    style : PropTypes.object,
    tip : PropTypes.string,
    onClick : PropTypes.func
};



