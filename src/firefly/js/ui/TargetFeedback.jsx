import React from 'react';

const topDivStyle= {
    paddingTop: 5,
    position: 'relative',
    height : 50,
    textAlign : 'center'
};

const titleStyle= {
    display : 'inline-block',
    verticalAlign: 'top'
};

const makeSpan= (w) => <span style={{paddingLeft: w}}/>;

const defaultExamples = (
    <div style={{ display : 'inline-block', lineHeight : '1.2em'}}>
        {makeSpan(5)} 'm81' {makeSpan(15)} 'ngc 18' {makeSpan(15)}  '12.34 34.89'  {makeSpan(15)} '46.53 -0.251 gal'
        <br/>
        {makeSpan(5)}'19h17m32s 11d58m02s equ j2000' {makeSpan(5)}  '12.3 8.5 b1950' {makeSpan(5)} 'J140258.51+542318.3'
    </div>);

export function TargetFeedback ({showHelp, feedback, style={}, examples={}}) {
    const topStyle= {...topDivStyle, ...style};
    if (!showHelp) return <div style={topStyle}> <span dangerouslySetInnerHTML={{ __html : feedback }}/> </div>
    return (
        <div style={topStyle}>
            <div>
                <div style={titleStyle}>
                    <i>Examples: </i>
                </div>
                {{...defaultExamples, ...examples}}
            </div>
        </div>
    );
}
