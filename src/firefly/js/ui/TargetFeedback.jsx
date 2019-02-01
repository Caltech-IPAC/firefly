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
const makeSpan= (w) => <span style={{paddingLeft: `${w}px`}}/>;
const defaultExamples = <div style={exDivStyle}>
    {makeSpan(5)} 'm81' {makeSpan(15)} 'ngc 18' {makeSpan(15)}  '12.34 34.89'  {makeSpan(15)} '46.53 -0.251 gal'
    <br />
    {makeSpan(5)}'19h17m32s 11d58m02s equ j2000' {makeSpan(5)}  '12.3 8.5 b1950'
</div>;

export function TargetFeedback ({showHelp, feedback, style={}, examples={}}) {
    let retval;
    examples =  Object.assign({},defaultExamples, examples);
    style = Object.assign({}, topDivStyle, style);
    if (showHelp) {

        retval= (
            <div  style={style}>
                <div >
                    <div style={titleStyle}>
                        <i>Examples: </i>
                    </div>
                    {examples}
                </div>
            </div>
        );
    }
    else {
        retval= (
            <div style={style}>

                <span dangerouslySetInnerHTML={{
                    __html : feedback
                }}/>
            </div>
        );
    }
    return retval;
}

