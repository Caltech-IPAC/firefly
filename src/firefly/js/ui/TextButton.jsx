/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';

const labelStyle= {
    display: 'inline-block',
    lineHeight: '30px',
    fontSize: '10pt',
    fontStyle: 'italic',
    color: 'blue',
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
export function TextButton({text, tip='$text',style={}, onClick}) {
    var s= Object.assign({cursor:'pointer', verticalAlign:'bottom'},style);
    return (
        <div style={s} title={tip} onClick={onClick}>

            <div style={labelStyle} title={tip}>
                <u>{text}</u>
            </div>

        </div>
    );
}


//CloseButton.contextTypes= {
//};


TextButton.propTypes= {
    text : PropTypes.string,
    style : PropTypes.object,
    tip : PropTypes.string,
    onClick : PropTypes.func
};



