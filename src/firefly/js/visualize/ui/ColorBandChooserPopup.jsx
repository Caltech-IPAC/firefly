/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {omit,countBy} from 'lodash';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {InputGroup} from '../../ui/InputGroup.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {INFO_POPUP, showInfoPopup} from '../../ui/PopupUtil.jsx';
import {getMultiViewRoot,dispatchUpdateCustom, getViewer} from '../MultiViewCntlr.js';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../../core/ComponentCntlr.js';
import {Band, allBandAry} from '../Band.js';



export function showColorBandChooserPopup(viewerId,bandData, dataId) {
    const popup= (
        <PopupPanel title={'Choose Color Bands'} >
            <ColorBandChooserPanel viewerId={viewerId} bandData={bandData}
                                   dataId={dataId}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('ColorBandChooserPopup', popup);
    dispatchShowDialog('ColorBandChooserPopup');
}


function ColorBandChooserPanel ({viewerId, bandData, dataId}) {


    const threeOp= omit(bandData, ['threeColorVisible']);

    const options= Object.keys(threeOp).map( (k) => ({value:k, label:threeOp[k].title}));
    options.push({value:'NONE', label:'Disable'});

    const {threeColorVisible}=  getViewer(getMultiViewRoot(), viewerId)?.customData[dataId];

    let redVal= Object.keys(threeOp).find( (k) => Boolean(threeOp[k].color===Band.RED));
    let greenVal= Object.keys(threeOp).find( (k) => Boolean(threeOp[k].color===Band.GREEN));
    let blueVal= Object.keys(threeOp).find( (k) => Boolean(threeOp[k].color===Band.BLUE));
    if (!redVal) redVal= 'NONE';
    if (!greenVal) greenVal= 'NONE';
    if (!blueVal) blueVal= 'NONE';

    return (
        <FieldGroup groupKey={'WHICH_BANDS'} keepState={false} style={{display:'flex', flexDirection:'column', alignItems:'center' }}>
            <div style={{padding:'10px 5px 5px 5px'}}>
                <div style={{display:'flex', flexDirection:'column', justifyContent:'space-around', height:60}}>
                    <ListBoxInputField labelWidth={40}
                        initialState= {{ value: redVal, tooltip: 'Select Red band', label : 'Red:' }}
                                        options={options} fieldKey={Band.RED.key} />

                    <ListBoxInputField labelWidth={40}
                        initialState= {{value: greenVal, tooltip: 'Select Green band', label : 'Green:' }}
                                        options={options} fieldKey={Band.GREEN.key} />

                    <ListBoxInputField labelWidth={40}
                                       initialState= {{value: blueVal, tooltip: 'Select Blue band', label : 'Blue:' }}
                                        options={options} fieldKey={Band.BLUE.key} />

                </div>
            </div>
            <div style={{display:'flex', justifyContent:'space-around', margin: '7px 5px 10px 3px' }}>
                <CompleteButton
                    style={{padding : '12px 0 5px 5px'}}
                    text={`${threeColorVisible?'Update':'Show'} Three Color`}
                    onSuccess={(request) => update3Color(request,bandData, viewerId, dataId)}
                    closeOnValid={true}
                    dialogId='ColorBandChooserPopup' />

                {threeColorVisible && <CompleteButton
                    style={{padding : '12px 0 5px 5px'}} text={'Hide Three Color'}
                    onSuccess={(request) => hideThreeColor(viewerId, dataId)}
                    closeOnValid={true} dialogId='ColorBandChooserPopup' />}
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
    if (valid) {
        loadThreeColor(request, bandData, viewerId, dataId);
    }
    else {
        showInfoPopup(errStr,'Error');
    }
}

function validate(request) {
    const keys= Object.keys(request);
    if (keys.every(  (k) => request[k]==='NONE')) {
        return {
            valid:false,
            errStr: 'You must enable a least one color band'
        };
    }
    const cnt= countBy(request);
    if (Object.keys(cnt).some( (k) => k!=='NONE' && cnt[k]>1 )) {
        return {
            valid:false,
            errStr: 'A color can be assign to only one band'
        };

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
        if (k==='threeColorVisible') {
            obj[k]=true;
        }
        else {
            obj[k]= {
                color: null,
                title: bandData[k].title
            };
        }
        return obj;
    },{});

    allBandAry.forEach( (b) => {
        if (request[b]!=='NONE') entry[request[b]].color = b;
    });

    const newCustom= Object.assign({}, v.customData[dataId], entry);
    dispatchUpdateCustom(viewerId,Object.assign({}, v.customData, {[dataId]:newCustom}));
    dispatchHideDialog('ColorBandChooserPopup');
}

