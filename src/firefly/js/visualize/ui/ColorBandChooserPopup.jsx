/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {omit, isArray} from 'lodash';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {useFieldGroupValue} from '../../ui/SimpleComponent.jsx';
import {getMultiViewRoot,dispatchUpdateCustom, getViewer} from '../MultiViewCntlr.js';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {Band, allBandAry} from '../Band.js';
import {HelpIcon} from '../../ui/HelpIcon';


const POPUP_ID= 'ColorBandChooserPopup';

export function showColorBandChooserPopup(viewerId,bandData, dataId) {
    const popup= (
        <PopupPanel title={'Choose Color Bands'} >
            <ColorBandChooserPanel viewerId={viewerId} bandData={bandData}
                                   dataId={dataId}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(POPUP_ID, popup);
    dispatchShowDialog(POPUP_ID);
}


const getKey= (threeOp, band) =>
    Object.keys(threeOp).find( (k) =>
        isArray(threeOp[k].color) ? threeOp[k].color.includes(band) : threeOp[k].color===band );

const hasDuplicateBands= (red,green,blue) => {
    if (red && red!=='NONE' && (red===green || red===blue) ) return true;
    if (green && green!=='NONE' && (green===red || green===blue) ) return true;
    if (blue && blue!=='NONE' && (blue===red || blue===green) ) return true;
    return false;
};

function ColorBandChooserPanel ({viewerId, bandData, dataId}) {

    const threeOp= omit(bandData, ['threeColorVisible']);

    const options= Object.keys(threeOp).map( (k) => ({value:k, label:threeOp[k].title}));
    options.push({value:'NONE', label:'Disable'});

    const {threeColorVisible}=  getViewer(getMultiViewRoot(), viewerId)?.customData[dataId];

    const initRedVal= getKey(threeOp,Band.RED) ?? 'NONE';
    const initGreenVal= getKey(threeOp,Band.GREEN) ?? 'NONE';
    const initBlueVal= getKey(threeOp,Band.BLUE) ?? 'NONE';

    const [getRed]= useFieldGroupValue(Band.RED.key, 'WHICH_BANDS');
    const [getGreen]= useFieldGroupValue(Band.GREEN.key, 'WHICH_BANDS');
    const [getBlue]= useFieldGroupValue(Band.BLUE.key, 'WHICH_BANDS');

    const dups= hasDuplicateBands(getRed()??initRedVal, getGreen()??initGreenVal, getBlue()??initBlueVal);

    return (
        <FieldGroup groupKey={'WHICH_BANDS'} keepState={false} style={{display:'flex', flexDirection:'column', alignItems:'center' }}>
            <div style={{padding:'10px 5px 5px 5px'}}>
                <div style={{display:'flex', flexDirection:'column', justifyContent:'space-between', height:85}}>
                    <ListBoxInputField labelWidth={40}
                        initialState= {{ value: initRedVal, tooltip: 'Select Red band', label : 'Red:' }}
                                        options={options} fieldKey={Band.RED.key} />

                    <ListBoxInputField labelWidth={40}
                        initialState= {{value: initGreenVal, tooltip: 'Select Green band', label : 'Green:' }}
                                        options={options} fieldKey={Band.GREEN.key} />

                    <ListBoxInputField labelWidth={40}
                                       initialState= {{value: initBlueVal, tooltip: 'Select Blue band', label : 'Blue:' }}
                                        options={options} fieldKey={Band.BLUE.key} />

                    <div style={{color:'red', height: 12,  display:'flex', flexDirection:'column', justifyContent:'flex-end'}}>
                        {dups && <div>Duplicate Bands</div>}
                    </div>
                </div>
            </div>
            <div style={{display:'flex', justifyContent:'space-around', margin: '7px 5px 10px 3px' }}>
                <CompleteButton
                    style={{padding : '12px 0 5px 5px'}}
                    text={`${threeColorVisible?'Update':'Show'} Three Color`}
                    onSuccess={(request) => update3Color(request,bandData, viewerId, dataId)}
                    closeOnValid={true}
                    dialogId={POPUP_ID} />

                {threeColorVisible && <CompleteButton
                    style={{padding : '12px 0 5px 5px'}} text={'Hide Three Color'}
                    onSuccess={() => hideThreeColor(viewerId, dataId)}
                    closeOnValid={true} dialogId={POPUP_ID} />}
            </div>
            <div>
                <HelpIcon helpId={'visualization.imageview3color'} />
            </div>

        </FieldGroup>
    );
}


ColorBandChooserPanel.propTypes= {
    viewerId: PropTypes.string.isRequired,
    bandData: PropTypes.object.isRequired,
    dataId : PropTypes.string.isRequired
};


function update3Color(request, bandData, viewerId, dataId) {
    const {errStr,valid}= validate(request);
    if (valid) loadThreeColor(request, bandData, viewerId, dataId);
    else showInfoPopup(errStr,'Error');
}

function validate(request) {
    if (Object.keys(request).every(  (k) => request[k]==='NONE')) {
        return { valid:false, errStr: 'You must enable a least one color band' };
    }
    return {valid:true};
}

function hideThreeColor(viewerId, dataId) {
    const v= getViewer(getMultiViewRoot(), viewerId);
    dispatchUpdateCustom(viewerId,{...v.customData, [dataId]:{...v.customData[dataId], threeColorVisible:false}});
}


function loadThreeColor(request, bandData, viewerId, dataId) {
    const v= getViewer(getMultiViewRoot(), viewerId);
    if (!v) return;

    const entry= Object.keys(bandData).reduce( (obj,k) => {
        obj[k]= (k==='threeColorVisible') ? true : { color: null, title: bandData[k].title };
        return obj;
    },{});

    allBandAry.forEach( (b) => {
        if (request[b]==='NONE') return;
        if (!entry[request[b]].color) entry[request[b]].color = b;
        else if (isArray(entry[request[b]].color)) entry[request[b]].color= [...entry[request[b]].color,b];
        else entry[request[b]].color= [entry[request[b]].color,b];
    });

    dispatchUpdateCustom(viewerId,{...v.customData, [dataId]:{...v.customData[dataId], ...entry}});
    dispatchHideDialog(POPUP_ID);
}
