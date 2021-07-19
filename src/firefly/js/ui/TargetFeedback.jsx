import React from 'react';
import {getAppOptions} from 'firefly/core/AppDataCntlr.js';

const topDivStyle= {
    paddingTop: 5,
    position: 'relative',
    height : 50,
    textAlign : 'center'
};

const defExampleEntries= {
    /* eslint-disable-next-line quotes */
    row1: [`'m81'`, `'ngc 18'`, `'12.34 34.89'`, `'46.53 -0.251 gal'`],
    /* eslint-disable-next-line quotes */
    row2: [ `'19h17m32s 11d58m02s equ j2000'`,`'12.3 8.5 b1950'`,`'J140258.51+542318.3'`]
    };

const defaultExamples= () => {
    const row1Op= getAppOptions()?.targetPanelExampleRow1 ?? defExampleEntries.row1;
    const row2Op= getAppOptions()?.targetPanelExampleRow2 ?? defExampleEntries.row2;
    return (
        <div style={{ display : 'inline-block', lineHeight : '1.2em'}}>
            {row1Op?.map( (s,idx) => <span key={s} style={{paddingLeft: (idx===0) ? 5 : 15}}>{s}</span> )}
            <br/>
            {row2Op?.map( (s,idx) => <span key={s} style={{paddingLeft: (idx===0) ? 5 : 15}}>{s}</span> )}
        </div>
        );
};

export function TargetFeedback ({showHelp, feedback, style={}, examples}) {
    const topStyle= {...topDivStyle, ...style};
    if (!showHelp) return (<div style={topStyle}> <span dangerouslySetInnerHTML={{ __html : feedback }}/> </div>);
    return (
        <div style={topStyle}>
            <div>
                <div style={{display : 'inline-block', verticalAlign: 'top'}}>
                    <i>Examples: </i>
                </div>
                {examples ?? defaultExamples()}
            </div>
        </div>
    );
}
