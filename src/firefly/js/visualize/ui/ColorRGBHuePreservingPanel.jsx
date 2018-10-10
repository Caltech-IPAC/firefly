import React, {PureComponent} from 'react';
import {debounce, get} from 'lodash';
import {replot3ColorHuePreserving} from './ColorDialog.jsx';
import {getTypeMinField, getZscaleCheckbox, renderAsinH} from './ColorBandPanel.jsx';
import {getFieldGroupResults, validateFieldGroup} from '../../fieldGroup/FieldGroupUtils';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {RangeSlider} from '../../ui/RangeSlider.jsx';
import {FieldGroupCollapsible} from '../../ui/panel/CollapsiblePanel.jsx';


export class ColorRGBHuePreservingPanel extends PureComponent {

    constructor(props) {
        super(props);
        this.doReplot= debounce(getReplotFunc(props.groupKey), 600);
    }

    render() {
        const {rgbFields} = this.props;
        const textPadding = {paddingBottom:3};
        const LABEL_WIDTH= 105;
        const renderRange = (isZscale) => {
            if (isZscale) {
                return null;
            } else {
                return (
                    <div>
                        <div style={{whiteSpace: 'no-wrap'}}>
                            <ValidationField wrapperStyle={textPadding} inline={true}
                                             labelWidth={LABEL_WIDTH}
                                             fieldKey='lowerRangeRed'
                            />
                            {getTypeMinField('lowerWhichRed')}
                        </div>
                        <div style={{whiteSpace: 'no-wrap'}}>
                            <ValidationField wrapperStyle={textPadding} inline={true}
                                             labelWidth={LABEL_WIDTH}
                                             fieldKey='lowerRangeGreen'
                            />
                            {getTypeMinField('lowerWhichGreen')}
                        </div>
                        <div style={{whiteSpace: 'no-wrap'}}>
                            <ValidationField wrapperStyle={textPadding} inline={true}
                                             labelWidth={LABEL_WIDTH}
                                             fieldKey='lowerRangeBlue'
                            />
                            {getTypeMinField('lowerWhichBlue')}
                        </div>
                        <div style={{whiteSpace: 'no-wrap'}}>
                            <ValidationField wrapperStyle={textPadding} inline={true}
                                             labelWidth={LABEL_WIDTH}
                                             fieldKey='stretch'/>
                        </div>
                    </div>
                );
            }
        };

        const asinH = renderAsinH(rgbFields, renderRange, this.doReplot, {});
        const scale = renderScalingCoefficients(rgbFields, this.doReplot);
        return (
            <div style={{minWidth: 360, padding: 5, position: 'relative'}}>
                <div style={{paddingBottom: 8, opacity: .7, textAlign: 'center'}}>
                    Brightness-independent color-preserving asinh stretch;<br/>
                    images must be free of background artifacts
                </div>
                {asinH}
                {scale}
                <div style={{position:'absolute', bottom:5, left:5, right:5}}>
                    {getZscaleCheckbox()}
                </div>
            </div>
        );
    }
}

const scaleMarks = {
 '-1': '.1', '-0.699': '.2', '-0.301': '.5', '0': '1', '0.301': '2', '0.699': '5', '1': '10'
};

function renderScalingCoefficients(fields, replot) {

    return (
        <div style={{paddingBottom: 40}}>
            <FieldGroupCollapsible  header='Band scaling coefficients'
                                    initialState= {{ value:'closed' }}
                                    fieldKey='bandScaling'>
                {['Red', 'Green', 'Blue'].map((c) => {
                    const fieldKey = `k${c}`;
                    const value = get(fields, [fieldKey, 'value'], 1);
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
                        labelWidth={100}
                        wrapperStyle={{marginTop: 10, marginBottom: 20, marginRight: 15}}
                        decimalDig={2}
                        onValueChange={replot}
                    />);
                })}
            </FieldGroupCollapsible>
        </div>
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