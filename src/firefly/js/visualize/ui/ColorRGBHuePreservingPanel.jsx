import {Stack, Typography} from '@mui/joy';
import React, {useEffect} from 'react';
import {debounce} from 'lodash';
import {replot3ColorHuePreserving} from './ColorDialog.jsx';
import {getTypeMinField, renderAsinH, ZscaleCheckbox} from './ColorBandPanel.jsx';
import {getFieldGroupResults, validateFieldGroup} from '../../fieldGroup/FieldGroupUtils';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {RangeSlider} from '../../ui/RangeSlider.jsx';
import {FieldGroupCollapsible} from '../../ui/panel/CollapsiblePanel.jsx';
import {useWatcher} from 'firefly/ui/SimpleComponent.jsx';
import ImagePlotCntlr from 'firefly/visualize/ImagePlotCntlr.js';
import {dispatchForceFieldGroupReducer} from 'firefly/fieldGroup/FieldGroupCntlr.js';

const isDebug = () => window.firefly?.debugStretch ?? false;

export const ColorRGBHuePreservingPanel= ({rgbFields,groupKey}) => {

    const doReplot= debounce(getReplotFunc(groupKey), 600);

    useEffect(() => {
        dispatchForceFieldGroupReducer(groupKey);
    },[]);

    useWatcher([ImagePlotCntlr.ANY_REPLOT, ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW],
        (action) => {
            dispatchForceFieldGroupReducer(groupKey, action);
        }
    );
    if (!rgbFields) return <div/>;
    const {zscale} = rgbFields;
    const zscaleValue = zscale?.value;
    const renderRange = (isZscale) => {
        if (isZscale) {
            return null;
        } else {
            return (
                <FieldGroupCollapsible  header='Pedestals (black point values)'
                                        initialState= {{ value:'closed' }}
                                        fieldKey='rangeParameters'>
                    <Stack spacing={1}>
                        <ValidationField sx={{'& .MuiInput-root':{ 'paddingInlineEnd': 0, }}}
                                         endDecorator={getTypeMinField('lowerWhichRed')}
                                         fieldKey='lowerRangeRed'
                        />
                        <ValidationField sx={{'& .MuiInput-root':{ 'paddingInlineEnd': 0, }}}
                                         endDecorator={getTypeMinField('lowerWhichGreen')}
                                         fieldKey='lowerRangeGreen'
                        />
                        <ValidationField
                                         sx={{'& .MuiInput-root':{ 'paddingInlineEnd': 0, }}}
                                         endDecorator={getTypeMinField('lowerWhichBlue')}
                                         fieldKey='lowerRangeBlue'
                        />
                        {isDebug() && <div style={{whiteSpace: 'no-wrap'}}>
                            <ValidationField fieldKey='stretch'/>
                        </div>}
                    </Stack>
                </FieldGroupCollapsible>
            );
        }
    };

    const asinH = renderAsinH(rgbFields, renderRange(zscaleValue), doReplot, true);
    const scale = renderScalingCoefficients(rgbFields, doReplot);
    return (
        <Stack spacing={2} sx={{mt:1, minWidth:360}}>
            <Typography level='body-sm'>
                Brightness-independent color-preserving asinh stretch;<br/>
                images must be free of background artifacts
            </Typography>
            {scale}
            <ZscaleCheckbox/>
            {asinH}
        </Stack>
    );
};

const scaleMarks = [
    {label:'-1', value: .1},
    {label: '-0.699', value: .2},
    {label:'-0.301', value: .5},
    {label:'0', value: 1},
    {label:'0.301', value: 2},
    {label:'0.699', value: 5},
    {label:'1', value: 10}
];

function renderScalingCoefficients(fields, replot) {

    return (
        <FieldGroupCollapsible  header='Scaling coefficients'
                                initialState= {{ value:'open' }}
                                fieldKey='bandScaling'>
            {['Red', 'Green', 'Blue'].map((c) => {
                const fieldKey = `k${c}`;
                const value = fields?.[fieldKey]?.value ?? 1;
                const displayVal = Math.pow(10, value).toFixed(2);
                const label = `${c}: ${displayVal}`;

                return (<RangeSlider
                    key={c}
                    fieldKey={fieldKey}
                    marks={scaleMarks}
                    step={0.01}
                    min={-1}
                    max={1}
                    slideValue={value}
                    label={label}
                    style={{marginTop: 10, marginBottom: 20, marginRight: 15}}
                    decimalDig={2}
                    onValueChange={replot}
                />);
            })}
        </FieldGroupCollapsible>
    );
}

function getReplotFunc(groupKey) {
    return () => {
        validateFieldGroup(groupKey).then((valid) => {
            if (valid) {
                const request = getFieldGroupResults(groupKey);
                replot3ColorHuePreserving(request);
            }
        });
    };
}