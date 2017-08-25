/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import {isEmpty} from 'lodash';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {visRoot, dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {primePlot} from '../PlotViewUtil.js';
import {getMultiViewRoot,getExpandedViewerItemIds,dispatchReplaceViewerItems,
                             EXPANDED_MODE_RESERVED, IMAGE} from '../MultiViewCntlr.js';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';



export function showExpandedOptionsPopup(plotViewAry) {
    const popup= (
        <PopupPanel title={'Choose which'} >
            <ExpandedOptionsPanel plotViewAry={plotViewAry}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('ExpandedOptionsPopup', popup);
    dispatchShowDialog('ExpandedOptionsPopup');
}


function ExpandedOptionsPanel ({plotViewAry}) {
    var loadedPv= plotViewAry.filter( (pv) => primePlot(pv)?true:false );
    var options= loadedPv.map( (pv) => ({label: primePlot(pv).title, value:pv.plotId}));
    const expandedIds= getExpandedViewerItemIds(getMultiViewRoot());
    var enabledStr= loadedPv.reduce( (s,pv) => {
        if (!expandedIds.includes(pv.plotId)) return s;
        return s ? `${pv.plotId},${s}` : pv.plotId;
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
        const plotIdAry= request.optionCheckBox.split(',');
        if (!isEmpty(plotIdAry)) {
            if (!plotIdAry.includes(visRoot().activePlotId)) {
                dispatchChangeActivePlotView(plotIdAry[0]);
            }
            dispatchReplaceViewerItems(EXPANDED_MODE_RESERVED, plotIdAry, IMAGE);
        }
    }
}


