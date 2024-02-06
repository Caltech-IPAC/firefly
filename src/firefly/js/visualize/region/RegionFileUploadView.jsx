/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Stack} from '@mui/joy';
/**
 * This Object contains the specifications of the DS9 region
 */
import React, {useState} from 'react';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {dispatchCreateRegionLayer, dispatchCreateFootprintLayer} from '../DrawLayerCntlr.js';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {dispatchHideDialog} from '../../core/ComponentCntlr.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {createNewRegionLayerId, getRegionLayerTitle} from '../../drawingLayers/RegionPlot.js';
import {relocatable} from '../../drawingLayers/FootprintLocatable.js';
import {createNewFootprintLayerId, getFootprintLayerTitle} from '../../drawingLayers/FootprintTool.js';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';

const popupId = 'RegionFilePopup';
const rgUploadGroupKey = 'RegionUploadGroup';
const rgUploadFieldKey = 'regionFileUpload';
const relocatableKey = 'relocatableRegion';


export function showRegionFileUploadPanel(divElement, popTitle='Load DS9 Region File') {
    const popup = (<PopupPanel title={popTitle}>
                    <RegionUpload />
                </PopupPanel>);

    DialogRootContainer.defineDialog(popupId, popup);
    dispatchShowDialog(popupId);
}


function regionFileNameFromDisplay(file) {
    const dispVal = file ? FieldGroupUtils.getGroupFields(rgUploadGroupKey)?.[rgUploadFieldKey]?.displayValue : '';
    return dispVal ? dispVal.split(/(\\|\/)/g).pop() : '';
}

/**
 * get region json from server, convert json into Region objects and make RegionPlot DrawLayer
 * @param request
 * @param setUpload
 * @param setMessage
 */
function uploadAndProcessRegion(request, setUpload, setMessage) {
    const [FieldKeyErr] = ['no region file uploaded yet'];

     const regionFile = request?.[rgUploadFieldKey];

     const setStatus = (file, regionAry, message)  => {
            setUpload?.(!!file || !!regionAry);
            setMessage?.(message);

            if (file || regionAry) {
                const relocateBy = request?.[relocatableKey];
                const fileName = regionFileNameFromDisplay(file);
                const absFileName = !fileName ? '' : (fileName.includes('.') ? fileName.slice(0, fileName.lastIndexOf('.')) : fileName);

                let   drawLayerId, title;

                if (!relocateBy) {   // regular region file, non relocatable
                    drawLayerId = createNewRegionLayerId();
                    title = getRegionLayerTitle(absFileName);

                    dispatchCreateRegionLayer(drawLayerId, title, file, regionAry);
                } else {
                    drawLayerId = createNewFootprintLayerId();
                    title = getFootprintLayerTitle(absFileName);
                    dispatchCreateFootprintLayer(drawLayerId, title, {footprint: file, relocateBy, fromFile: absFileName},
                                                 [], true);
                }
                dispatchHideDialog(popupId);
            }
    };


    if (!regionFile) {
        setStatus(null, null, FieldKeyErr);
        //for testing
        /*
        var regionAry = [
            'global color=green dashlist=8 3 width=1 font="helvetica 10 normal normal" select=1 highlite=1 dash=0 fixed=0 edit=1 move=1 delete=1 include=1 rotate=0 source=1',
            'J2000;point    202.41   47.262 # color=pink text="pt circle 1" point=circle',
            'line(202.414796 47.281999 202.423758 47.247072) # color=orange width=10 select=0 text={line 2}',
            'text 30p 40p # color=pink text={text test 3} edit=0 highlite=0 font="BRAGGADOCIO 10 normal roman"',
            'text 202.437691 47.234228{more text testing 4} # color=purple move=0',
            'point    202.43    47.270 # point=x 15 color=green text="pt x 5" rotate=0',
            '#point    202.45    47.262 # color=red 10p text="pt boxcircle 6" delete=0 point=boxcircle 10',
            'boxcircle point    202.45    47.262 # color=red text="pt boxcircle 6" delete=0',
            'physical;circle   80 140  20       # color=red offsetx=25  offsety=2 width=5 edit=0 text="circle 7"',
            'annulus   13h30m16.41s +47d14m43.9s  30p 40p 50p       # color=green text="hello" include=0 text="annulus 8"',
            'physical;point 200 140 # color=yellow point=cross 20 text="pt cross 9" offsetx=10',
            'physical;point 13h29m52.73s, +47d11m40.9s # color=purple point=diamond 15 text="pt diamond 10"',
            '#circle(202.55556, 47.188286 , 20p)  # color=blue text="text 11"',
            'box(202.55556, 47.188286 ,20p,40p, 30p, 50p, 0)  # color=red width=5 text="boxann 11"',
            'box(202.52556,47.226286,0.0240,0.006,0)  # color=green width=5 text="box 11-2"',
            'image;box(100, 150, 50p, 20p, 2r) # color=magenta width=6 text="slanted box 12"',
            'image;box(190.564796, 47.281999, 50p, 20p, 0) # color=red width=6 offsety=-15 text="box 12-1"',
            'j2000;box(47.247072i, 180.445347i, 50p, 20p, 0) # color=blue width=6 text="box 12-3"',
            'physical;-box point 12 12 # text="pt box 13"',
            'j2000;polygon(202.564796, 47.281999,202.553758, 47.247072, 202.445347, 47.244296, 202.479687, 47.264027, 202.492153, 47.290841) # color=blue text="polygon 14" offsety=10 width=10 font="helvetica 16 bold"',
            'point    202.45   47.2879 # color=cyan text="pt arrow 15" delete=0 point=arrow 20',
            'image;ellipse 100 100 20p 40p 30p 60p 40p 80p 20 # color=green text={ellipseannulus 2}',
            'J2000;ellipse 202.55556, 47.188286 20p 40p 0i # color=#48f text={ellipse 1} width=10',
            'image;box 130 100 20p 40p 30p 50p 70p 100p 30 # color=red text={slanted box annulus 3}'
            ];
        setStatus(null, regionAry);
        */

    } else {
        setStatus(regionFile, null);
    }
}

/**
 * popup panel for upload region file
 */
const RegionUpload= () => {

    const [upload, setUpload]= useState(() => true);
    const [message, setMessage]= useState(() => '');
    return (
        <Box sx={{p: 1, width: 380, resize:'horizontal', overflow:'hidden'}}>
            <FieldGroup groupKey={rgUploadGroupKey}>
                <Stack spacing={2}>
                    <FileUpload
                        sx={{my:1/2, '& .ff-FileUpload-upload':{width:1, maxWidth:1}}}
                        fieldKey={rgUploadFieldKey}
                        tooltip='Select a region file to upload' label='Upload File'
                    />
                    <CheckboxGroupInputField
                        fieldKey={relocatableKey}
                        options={[{label: 'treat as relocatable', value: relocatable.origin.key}]}
                        tooltip='treat the regions movable'
                        initialState={{ value: '' }}
                    />

                    <div style={{color: 'red', height: 15}}>
                        {!upload && ( (message && `*${message}`) || '*region error')}
                    </div>

                    <Stack {...{direction:'row', alignItems:'center', justifyContent:'space-between'}}>
                        <CompleteButton
                            dialogId={popupId}
                            onSuccess={(request) => uploadAndProcessRegion(request, setUpload, setMessage)}
                            closeOnValid={false}
                            text='Draw'/>
                        <HelpIcon helpId={'visualize.ds9.upload'}/>
                    </Stack>

                </Stack>
            </FieldGroup>
        </Box>
    );
};

