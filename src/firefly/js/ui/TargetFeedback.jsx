import {FormHelperText, Typography} from '@mui/joy';
import {isArray} from 'lodash';
import React from 'react';
import {getAppOptions} from 'firefly/core/AppDataCntlr.js';

const defExampleEntries= {
    /* eslint-disable-next-line quotes */
    row1: [`'m81'`, `'ngc 18'`, `'12.34 34.89'`, `'46.53 -0.251 gal'`],
    /* eslint-disable-next-line quotes */
    row2: [ `'19h17m32s 11d58m02s equ j2000'`,`'12.3 8.5 b1950'`,`'J140258.51+542318.3'`]
    };

const defaultExamples= (targetPanelExampleRow1, targetPanelExampleRow2) => {
    const tpR1= !targetPanelExampleRow1 || isArray(targetPanelExampleRow1) ? targetPanelExampleRow1 : [targetPanelExampleRow1];
    const tpR2= !targetPanelExampleRow2 || isArray(targetPanelExampleRow2) ? targetPanelExampleRow2 : [targetPanelExampleRow2];
    const row1Op= tpR1 ?? getAppOptions()?.targetPanelExampleRow1 ?? defExampleEntries.row1;
    const row2Op= tpR2 ?? getAppOptions()?.targetPanelExampleRow2 ?? defExampleEntries.row2;
    return (
        <div style={{ display : 'inline-block', lineHeight : '1.2em', fontSize:'smaller'}}>
            {row1Op?.map( (s,idx) => <span key={s} style={{paddingLeft: (idx===0) ? 5 : 15}}>{s}</span> )}
            <br/>
            {row2Op?.map( (s,idx) => <span key={s} style={{paddingLeft: (idx===0) ? 5 : 15}}>{s}</span> )}
        </div>
        );
};

export function TargetFeedback ({showHelp, feedback, sx, targetPanelExampleRow1, targetPanelExampleRow2, examples}) {
    const minHeight= '3rem';
    if (!showHelp) {
        return (
            <FormHelperText sx={{textAlign:'center', minHeight, ...sx}}>
                <Typography component='div'>
                    <span dangerouslySetInnerHTML={{ __html : feedback }}/>
                </Typography>
            </FormHelperText>
        );
    }
    return (
            <FormHelperText sx={{textAlign:'center', minHeight, ...sx}}>
                <Typography level='body-sm'>
                    <i>Examples: </i>
                </Typography>
                <Typography level='body-sm' component='div'>
                    {examples ?? defaultExamples(targetPanelExampleRow1, targetPanelExampleRow2)}
                </Typography>
            </FormHelperText>
    );
}
