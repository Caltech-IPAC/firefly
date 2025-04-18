import {FormHelperText, Link, Stack, Typography} from '@mui/joy';
import {isArray} from 'lodash';
import React, {useContext} from 'react';
import {getAppOptions} from 'firefly/core/AppDataCntlr.js';
import {FieldGroupCtx} from './FieldGroup';
import {positionValidateSoft} from './PositionFieldDef';
import {ingestNewTargetValue} from './TargetPanel';

const defExampleEntries= {
    row1: ['m81', 'ngc 18', '12.34 34.89', '46.53 -0.251 gal'],
    row2: [ '19h17m32s 11d58m02s equ j2000','12.3 8.5 b1950','J140258.51+542318.3']
    };


function formatExample(row, fieldKey, setFld) {
    return (
        <Stack {...{direction:'row', spacing:1.5}}>
            {row?.map( (s) => {
                const {valid}= positionValidateSoft(s);
                if (s && valid && fieldKey) {
                    return (
                        <Link key={s}
                              onClick={(ev) => ingestNewTargetValue(s,(params) => setFld(fieldKey,params))}>
                            {s}
                        </Link>
                    );
                }
                else {
                    return <span key={s} >{s}</span>;
                }
            })}
        </Stack>
    )
}


function stripQuotes(s) {
    if (s.startsWith(`'`) && s.endsWith(`'`)) return s.substring(1, s.length-1);
    if (s.startsWith('"') && s.endsWith('"')) return s.substring(1, s.length-1);
    return s;
}


const ConfigExamples = ({targetPanelExampleRow1, targetPanelExampleRow2, fieldKey}) => {
    const {setFld}= useContext(FieldGroupCtx);
    const tpR1= !targetPanelExampleRow1 || isArray(targetPanelExampleRow1) ? targetPanelExampleRow1 : [targetPanelExampleRow1];
    const tpR2= !targetPanelExampleRow2 || isArray(targetPanelExampleRow2) ? targetPanelExampleRow2 : [targetPanelExampleRow2];
    const row1Op= tpR1 ?? getAppOptions()?.targetPanelExampleRow1 ?? defExampleEntries.row1;
    const row2Op= tpR2 ?? getAppOptions()?.targetPanelExampleRow2 ?? defExampleEntries.row2;

    const r1= row1Op.map( (s) => stripQuotes(s));
    const r2= row2Op.map( (s) => stripQuotes(s));

    return (
        <Stack {...{lineHeight : '1.2em', fontSize:'smaller', direction:'column', spacing:1/2, alignItems:'center'}}>
            {formatExample(r1, fieldKey, setFld)}
            {formatExample(r2, fieldKey, setFld)}
        </Stack>
        );
};

export function TargetFeedback ({showHelp, feedback, sx, targetPanelExampleRow1, targetPanelExampleRow2,
                                    examples,fieldKey}) {
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
                <Typography level='body-sm' sx={{pr:1}}> Examples: </Typography>
                <Typography level='body-sm' component='div'>
                    {examples ?? <ConfigExamples {...{targetPanelExampleRow1, targetPanelExampleRow2, fieldKey}}/>}
                </Typography>
            </FormHelperText>
    );
}
