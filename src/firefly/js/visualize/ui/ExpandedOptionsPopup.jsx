/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import CheckboxGroupInputField from '../../ui/CheckboxGroupInputField.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import FieldGroup from '../../ui/FieldGroup.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {dispatchExpandedList} from '../ImagePlotCntlr.js';
import {primePlot} from '../PlotViewUtil.js';

import AppDataCntlr from '../../core/AppDataCntlr.js';


export function showExpandedOptionsPopup(plotViewAry) {
    const popup= (
        <PopupPanel title={'Choose which'} >
            <ExpandedOptionsPanel plotViewAry={plotViewAry}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('ExpandedOptionsPopup', popup);
    AppDataCntlr.showDialog('ExpandedOptionsPopup');
}


function ExpandedOptionsPanel ({plotViewAry}) {
    var loadedPv= plotViewAry.filter( (pv) => primePlot(pv)?true:false );
    var options= loadedPv.map( (pv) => ({label: primePlot(pv).title, value:pv.plotId}));
    var enabledStr= loadedPv.reduce( (s,pv) => {
        if (!pv.plotViewCtx.inExpandedList) return s;
        return s ? `${s},${pv.plotId}` : pv.plotId;
    },'');

    return (
        <FieldGroup groupKey={'WHICH_PLOTS'} keepState={false}>
            <div style={{padding:'10px 10px 5px 15px'}}>
                <CheckboxGroupInputField
                    alignment={'vertical'}
                    initialState= {{
                        value: enabledStr,
                        tooltip: 'Select which plot to display',
                        label : ''
                    }}
                    options={options}
                    fieldKey='optionCheckBox'
                />
            </div>
            <CompleteButton
                style={{padding : '12px 0 5px 5px'}}
                onSuccess={updateView}
                dialogId='ExpandedOptionsPopup' />

        </FieldGroup>
    );
}


ExpandedOptionsPanel.propTypes= {
    plotViewAry: PropTypes.array.isRequired
};

function updateView(request) {
    if (request.optionCheckBox) {
        dispatchExpandedList(request.optionCheckBox.split(','));
    }
}
