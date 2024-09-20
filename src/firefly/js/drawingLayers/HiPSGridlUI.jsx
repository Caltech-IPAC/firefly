
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Checkbox, Stack} from '@mui/joy';
import React from 'react';
import PropTypes from 'prop-types';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {ListBoxInputFieldView} from '../ui/ListBoxInputField.jsx';
import {dispatchModifyCustomField} from '../visualize/DrawLayerCntlr.js';
import {getMaxDisplayableHiPSGridLevel, getHiPSNorderlevel} from '../visualize/HiPSUtil.js';
import {primePlot} from '../visualize/PlotViewUtil.js';


const options= [ {label: 'Auto', value: 'auto'},
    {label: 'Grid Match Image Depth', value: 'match'},
    {label: 'Grid Level Lock', value: 'lock'}
];


export const getUIComponent = (drawLayer,pv) => <HiPSGridUI drawLayer={drawLayer} pv={pv}/>;

function HiPSGridUI({drawLayer,pv}) {

    const {gridType}= drawLayer;
    return (
        <Stack spacing={1} pl={1}>

            <Checkbox {...{checked:drawLayer.showLabels, label:'Show Labels', size:'sm',
                onChange:() =>
                    dispatchModifyCustomField(drawLayer.drawLayerId, {showLabels:!drawLayer.showLabels},pv.plotId)
            }} />
            <Stack {...{direction:'row', alignItems: 'flex-end'}}>
                <RadioGroupInputFieldView options={options}  value={gridType}
                                          alignment={'vertical'}
                                          onChange={(ev) => changeHipsPref(drawLayer,pv,ev.target.value)} />
                {gridType==='lock' && showLevelOp(drawLayer,pv) }
            </Stack>
        </Stack>
    );
}


function showLevelOp(drawLayer, pv) {
    const plot= primePlot(pv);
    if (!plot) return null;
    const norder= getMaxDisplayableHiPSGridLevel(plot);
    const {gridLockLevel}= drawLayer;
    const value= Math.min(norder, Number(gridLockLevel));
    const lockOp= [];
    for(let i= 1; i<=norder;i++) lockOp.push({label:i+'', value:i});

    return (
        <ListBoxInputFieldView
            value={value}
            onChange={(ev, newValue) => changeLockLevelPref(drawLayer,pv,newValue)}
            label=' '
            tooltip={ 'Choose HiPS grid level'}
            options={lockOp}
            multiple={false}
        />
    );
}


function changeHipsPref(drawLayer,pv,value) {
    const plot= primePlot(pv);
    const gridLockLevel= drawLayer.gridLockLevel || getHiPSNorderlevel(plot).norder;
    dispatchModifyCustomField(drawLayer.drawLayerId, {gridType:value,gridLockLevel},pv.plotId);
}

function changeLockLevelPref(drawLayer,pv,value) {
    dispatchModifyCustomField(drawLayer.drawLayerId, {gridType:'lock', gridLockLevel:value},pv.plotId);
}

HiPSGridUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};

