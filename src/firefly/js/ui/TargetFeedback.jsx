import React from 'react';



const topDivStyle= {
    paddingTop: 5,
    position: 'relative',
    height : 50,
    textAlign : 'center'
};
const exDivStyle= {
    display : 'inline-block'
};
const titleStyle= {
    display : 'inline-block',
    verticalAlign: 'top'
};


export function TargetFeedback ({showHelp, feedback }) {
    let retval;
    if (showHelp) {
        const makeSpan= (w) => <span style={{paddingLeft: `${w}px`}}/>;
        retval= (
            <div  style={topDivStyle}>
                <div >
                    <div style={titleStyle}>
                        <i>Examples: </i>
                    </div>
                    <div style={exDivStyle}>
                        {makeSpan(5)} 'm81' {makeSpan(15)} 'ngc 13' {makeSpan(15)}  '12.34 34.89'  {makeSpan(15)} '46.53, -0.251 gal'
                        <br />
                        {makeSpan(5)}'19h17m32s 11d58m02s equ j2000' {makeSpan(5)}  '12.3, 8.5 b1950'
                    </div>
                </div>
            </div>
        );
    }
    else {
        retval= (
            <div style={topDivStyle}>

                <span dangerouslySetInnerHTML={{
                    __html : feedback
                }}/>
            </div>
        );
    }
    return retval;
}

