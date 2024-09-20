/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Stack, Typography} from '@mui/joy';
import React from 'react';
import {string,object} from 'prop-types';
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

    const {threeColorVisible=false}=  getViewer(getMultiViewRoot(), viewerId)?.customData?.[dataId] ?? {};

    const initRedVal= getKey(threeOp,Band.RED) ?? 'NONE';
    const initGreenVal= getKey(threeOp,Band.GREEN) ?? 'NONE';
    const initBlueVal= getKey(threeOp,Band.BLUE) ?? 'NONE';

    const red= useFieldGroupValue(Band.RED.key, 'WHICH_BANDS')[0]();
    const green= useFieldGroupValue(Band.GREEN.key, 'WHICH_BANDS')[0]();
    const blue= useFieldGroupValue(Band.BLUE.key, 'WHICH_BANDS')[0]();

    const dups= hasDuplicateBands(red??initRedVal, green??initGreenVal, blue??initBlueVal);

    return (
        <FieldGroup groupKey='WHICH_BANDS'
                    sx={{display:'flex', px:2, flexDirection:'column', alignItems:'center', minWidth:'22rem' }}>
            <Stack {...{spacing:2, alignItems:'flex-start', width:1}}>
                <ListBoxInputField initialState= {{ value: initRedVal, tooltip: 'Select Red band', label : 'Red' }}
                                   orientation='vertical'
                                   slotProps={{ label: {sx: {color:'red'}} }}
                                   options={options} fieldKey={Band.RED.key} />

                <ListBoxInputField initialState= {{value: initGreenVal, tooltip: 'Select Green band', label : 'Green' }}
                                   orientation='vertical'
                                   slotProps={{ label: {sx: {color:'green'}} }}
                                   options={options} fieldKey={Band.GREEN.key} />

                <ListBoxInputField initialState= {{value: initBlueVal, tooltip: 'Select Blue band', label : 'Blue' }}
                                   orientation='vertical'
                                   slotProps={{ label: {sx: {color:'blue'}} }}
                                   options={options} fieldKey={Band.BLUE.key} />

                {dups && <Typography color='warning'>Duplicate Bands</Typography>}
                <Stack {...{direction:'row', alignSelf:'stretch', alignItems: 'center', justifyContent:'space-between', py:2}}>
                    <Stack {...{direction:'row',  spacing:2}}>
                        <CompleteButton
                            text={`${threeColorVisible?'Update':'Show'} Three Color`}
                            onSuccess={(request) => update3Color(request,bandData, viewerId, dataId)}
                            closeOnValid={true}
                            dialogId={POPUP_ID} />

                        {threeColorVisible &&
                            <CompleteButton
                                primary={false} text='Hide Three Color'
                                onSuccess={() => hideThreeColor(viewerId, dataId)}
                                closeOnValid={true} dialogId={POPUP_ID} />}
                    </Stack>
                    <HelpIcon helpId={'visualization.imageview3color'} />
                </Stack>
            </Stack>
        </FieldGroup>
    );
}


ColorBandChooserPanel.propTypes= {
    viewerId: string.isRequired,
    bandData: object.isRequired,
    dataId : string.isRequired
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

    dispatchUpdateCustom(viewerId,{...v.customData, [dataId]:{...v.customData[dataId], ...entry, threeColorVisible:true}});
    dispatchHideDialog(POPUP_ID);
}
