import React, {PureComponent} from 'react';
import {debounce} from 'lodash';
import {replot3ColorHuePreserving} from './ColorDialog.jsx';
import {getTypeMinField, getZscaleCheckbox, renderAsinH} from './ColorBandPanel.jsx';
import {getFieldGroupResults} from '../../fieldGroup/FieldGroupUtils';
import {ValidationField} from '../../ui/ValidationField.jsx';


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

        const asinH = renderAsinH(rgbFields, renderRange, this.doReplot);
        return (
            <div style={{minWidth:360, padding:5, position: 'relative'}}>
                {asinH}
                <div style={{position:'absolute', bottom:5, left:5, right:5}}>
                    {getZscaleCheckbox()}
                </div>
            </div>
        );
    }
}

function getReplotFunc(groupKey) {

    return () => {
        const request = getFieldGroupResults(groupKey);
        replot3ColorHuePreserving(request);
    };
}