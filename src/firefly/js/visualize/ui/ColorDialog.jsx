/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Stack} from '@mui/joy';
import React, {memo, useState} from 'react';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {CompleteButton} from '../../ui/CompleteButton.jsx';
import {Band} from '../Band.js';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {RadioGroupInputFieldView} from '../../ui/RadioGroupInputFieldView.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {ColorBandPanel} from './ColorBandPanel.jsx';
import {ColorRGBHuePreservingPanel} from './ColorRGBHuePreservingPanel.jsx';
import {dispatchStretchChange, visRoot} from '../ImagePlotCntlr.js';
import {primePlot, getActivePlotView, isThreeColor} from '../PlotViewUtil.js';
import { RangeValues, ZSCALE, STRETCH_ASINH}from '../RangeValues.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {isHiPS, isImage} from '../WebPlot';
import {useStoreConnector} from '../../ui/SimpleComponent';
import {FieldGroupTabs, Tab} from '../../ui/panel/TabPanel.jsx';

import {RED_PANEL,
    GREEN_PANEL,
    BLUE_PANEL,
    NO_BAND_PANEL,
    RGB_HUEPRESERVE_PANEL,
    colorPanelChange, rgbHuePreserveChange} from './ColorPanelReducer.js';


const colorPanelReducer= colorPanelChange(Band.NO_BAND);
const colorPanelRedReducer= colorPanelChange(Band.RED);
const colorPanelGreenReducer= colorPanelChange(Band.GREEN);
const colorPanelBlueReducer= colorPanelChange(Band.BLUE);
const rgbHuePreserveReducer= rgbHuePreserveChange([Band.RED, Band.GREEN, Band.BLUE]);

export function showColorDialog(element) {
    const content= <PopupPanel title={'Modify Color Stretch'} > <ColorDialog /> </PopupPanel>;
    DialogRootContainer.defineDialog('ColorStretchDialog', content, element);
    dispatchShowDialog('ColorStretchDialog');
}


function getStoreUpdate(oldS) {
    const plot= primePlot(visRoot());
    const fields= FieldGroupUtils.getGroupFields(NO_BAND_PANEL);
    const rFields= FieldGroupUtils.getGroupFields(RED_PANEL);
    const gFields= FieldGroupUtils.getGroupFields(GREEN_PANEL);
    const bFields= FieldGroupUtils.getGroupFields(BLUE_PANEL);
    const rgbFields= FieldGroupUtils.getGroupFields(RGB_HUEPRESERVE_PANEL);
    const newState= {plot, fields, rFields, gFields, bFields, rgbFields};
    if (!oldS) return newState;

    if ( plot?.plotState!==oldS.plot?.plotState || fields!==oldS.fields ||
        rFields!==oldS.rFields || gFields!==oldS.gFields || bFields!==oldS.bFields ||
        rgbFields!==oldS.rgbFields) {
        return newState;
    }
    return oldS;
}

export const ColorDialog= memo(() => {
    const {plot,fields,rFields,gFields,bFields,rgbFields} = useStoreConnector(getStoreUpdate);
    const [huePreserving, setHuePreserving]= useState(plot?.plotState.getRangeValues().rgbPreserveHue);
    if (!plot) return false;

    if (isImage(plot)) {
        return isThreeColor(plot) ?
            renderThreeColorView(plot,rFields,gFields,bFields,rgbFields,huePreserving,setHuePreserving) :
            renderStandardView(plot,fields);
    }
    else if (isHiPS(plot)) {
        return (
            <div style={{ fontSize: '12pt', padding: 10, width: 350, textAlign: 'center', margin: '30px 0 30px 0' }}>
                Cannot modify stretch for HiPS Image
            </div>
        );
    }
});

function renderThreeColorView(plot, rFields, gFields, bFields, rgbFields, isHuePreservingSelected, setHuePreserving) {
    const {plotState} = plot;
    const canBeHuePreserving = plotState.isBandUsed(Band.RED) && plotState.isBandUsed(Band.GREEN) && plotState.isBandUsed(Band.BLUE);
    const threeColorStretchMode = canBeHuePreserving &&
        <RadioGroupInputFieldView
            options={[
                {label: 'Per-band stretch', value: 'perBand'},
                {label: 'Hue preserving stretch', value: 'huePreserving'}

            ]}
            value={isHuePreservingSelected ? 'huePreserving' : 'perBand'}
            onChange={(ev) => setHuePreserving(ev.target.value==='huePreserving')}
        />;

    return (
        <Box sx={{m:1}}>
            {threeColorStretchMode}
            {Boolean(isHuePreservingSelected) && renderHuePreservingThreeColorView(plot, rgbFields)}
            {!isHuePreservingSelected && renderStandardThreeColorView(plot, rFields, gFields, bFields)}
        </Box>
    );
}

function renderHuePreservingThreeColorView(plot,rgbFields) {
    const groupKey = RGB_HUEPRESERVE_PANEL;
    return (
        <Box sx={{m:1}}>
            <FieldGroup groupKey={groupKey} keepState={false} reducerFunc={rgbHuePreserveReducer} >
                <ColorRGBHuePreservingPanel {...{plot, rgbFields, groupKey}}/>
                <Stack {...{direction: 'row', alignItems: 'center', justifyContent: 'space-between'}}>
                    <CompleteButton
                        closeOnValid={false} sx={{pt:.25, pb:1, pl:1}} text='Refresh' dialogId='ColorStretchDialog'
                        onFail={invalidMessage} onSuccess={(request)=>replot3ColorHuePreserving(request)}
                    />
                    <Box sx={{ textAlign:'right', py:.25, px:1}}>
                        <HelpIcon helpId='visualization.stretches'/>
                    </Box>
                </Stack>
            </FieldGroup>
        </Box>

    );
}

function renderStandardThreeColorView(plot,rFields,gFields,bFields) {
    const {plotState}= plot;
    const usedBands = plotState? plotState.usedBands:null;
    return (
        <Box sx={{pt:.5}}>
            <FieldGroup groupKey={'colorDialogTabs'} keepState={false}>
                <FieldGroupTabs initialState= {{ value:'red' }} fieldKey='colorTabs'>
                    {plotState.isBandUsed(Band.RED) &&
                    <Tab name='Red' id='red'>
                        <FieldGroup groupKey={RED_PANEL} keepState={true} reducerFunc={colorPanelRedReducer}>
                            <ColorBandPanel groupKey={RED_PANEL} band={Band.RED} fields={rFields}
                                            plot={plot} key={Band.RED.key}/>
                        </FieldGroup>
                    </Tab>
                    }

                    {plotState.isBandUsed(Band.GREEN) &&
                    <Tab name='Green' id='green'>
                        <FieldGroup groupKey={GREEN_PANEL} keepState={true} reducerFunc={colorPanelGreenReducer} >
                            <ColorBandPanel groupKey={GREEN_PANEL} band={Band.GREEN} fields={gFields}
                                            plot={plot} key={Band.GREEN.key}/>
                        </FieldGroup>
                    </Tab>
                    }

                    {plotState.isBandUsed(Band.BLUE) &&
                    <Tab name='Blue' id='blue'>
                        <FieldGroup groupKey={BLUE_PANEL} keepState={true}  reducerFunc={colorPanelBlueReducer} >
                            <ColorBandPanel groupKey={BLUE_PANEL} band={Band.BLUE} fields={bFields}
                                            plot={plot} key={Band.BLUE.key}/>
                        </FieldGroup>
                    </Tab>
                    }
                </FieldGroupTabs>
                <Stack direction='row' justifyContent='space-between' alignItems='center'>
                    <CompleteButton
                        text='Refresh' dialogId='ColorStretchDialog' includeUnmounted={true}
                        groupKey={['colorDialogTabs',RED_PANEL,GREEN_PANEL,BLUE_PANEL]}
                        closeOnValid={false} sx={{pt:.25, pb:1, pl:1}}
                        onSuccess={replot(usedBands)} onFail={invalidMessage}
                    />
                    <HelpIcon helpId='visualization.modifyColorStretch3C'/>
                </Stack>
            </FieldGroup>
        </Box>

    );

}


function renderStandardView(plot,fields) {


    return (
        <FieldGroup groupKey={NO_BAND_PANEL} keepState={true}  reducerFunc={colorPanelReducer} >
            <ColorBandPanel groupKey={NO_BAND_PANEL} band={Band.NO_BAND} fields={fields} plot={plot}/>
            <Stack direction='row' justifyContent='space-between' alignItems='center'>
                <CompleteButton
                    text='Refresh' dialogId='ColorStretchDialog'
                    closeOnValid={false} sx={{pt:.25, pb:1, pl:1}}
                    onSuccess={replot()} onFail={invalidMessage}
                />
                <HelpIcon helpId='visualization.modifyColorStretchSingleBand'/>
            </Stack>
        </FieldGroup>
       );
}

function replot(usedBands=null) {

    return (request)=> {

        const defReq= request.redPanel || request.greenPanel || request.bluePanel;
        if (request.colorDialogTabs) {

            replot3Color(
                request.redPanel||defReq, request.greenPanel||defReq, request.bluePanel||defReq,
                request.colorDialogTabs.colorTabs, usedBands);
        } else {
            replotStandard(request);
        }
    };
}


function invalidMessage() {
    showInfoPopup('One or more fields are not valid', 'Invalid Data');
}


function replotStandard(request) {
    // console.log(request);
    const serRv=  makeSerializedRv(request);
    const stretchData= [{ band: Band.NO_BAND.key, rv:  serRv, bandVisible: true }];
    const pv= getActivePlotView(visRoot());
    if (pv) dispatchStretchChange({plotId:pv.plotId,stretchData});
}

export function replot3ColorHuePreserving(request) {
    // console.log(request);
    const useZ= Boolean(request.zscale);
    const stretchData = [[Band.RED.key, 'lowerWhichRed','lowerRangeRed','kRed'],
        [Band.GREEN.key, 'lowerWhichGreen','lowerRangeGreen','kGreen'],
        [Band.BLUE.key, 'lowerWhichBlue', 'lowerRangeBlue','kBlue']].map(([band, lowerWhich, lowerRange,  scalingK]) => {
        const scalingKVal = Math.pow(10, parseFloat(request[scalingK]));
        const rv= RangeValues.makeRV( {
            lowerWhich: useZ ? ZSCALE : request[lowerWhich],
            lowerValue: request[lowerRange],
            asinhQValue: request.asinhQ,
            algorithm: STRETCH_ASINH,
            rgbPreserveHue: 1,
            asinhStretch: Number.NaN, //request.stretch,
            scalingK: scalingKVal,
        });
        return {band, rv, bandVisible: true};
    });
    const pv= getActivePlotView(visRoot());
    if (pv) dispatchStretchChange({plotId:pv.plotId,stretchData});
}


/**
 *
 * @param redReq
 * @param greenReq
 * @param blueReq
 * @param activeTab - which tab is active, might be used in future
 *  @param usedBands
 */
function replot3Color(redReq,greenReq,blueReq,activeTab, usedBands) {

    const stretchData= [];

    for (let i=0; i<usedBands.length; i++){
        switch (usedBands[i].key.toLowerCase()) {
            case 'red':

                stretchData.push({band: Band.RED.key, rv: makeSerializedRv(redReq), bandVisible: true});

                break;
            case 'green':
                stretchData.push({band: Band.GREEN.key, rv: makeSerializedRv(greenReq), bandVisible: true});

                break;
            case 'blue':
                stretchData.push({band: Band.BLUE.key, rv: makeSerializedRv(blueReq), bandVisible: true});

                break;
        }
    }

    const pv= getActivePlotView(visRoot());
    if (pv) dispatchStretchChange({plotId:pv.plotId,stretchData});
}


export function makeSerializedRv(request) {
    const useZ= Boolean(request.zscale);

    const rv= RangeValues.makeRV( {
            lowerWhich: useZ ? ZSCALE : request.lowerWhich,
            upperWhich: useZ ? ZSCALE : request.upperWhich,
            lowerValue: request.lowerRange,
            upperValue: request.upperRange,
            asinhQValue: request.asinhQ,
            gammaValue: request.gamma,
            algorithm: request.algorithm,
            zscaleContrast: request.zscaleContrast,
            zscaleSamples: request.zscaleSamples,
            zscaleSamplesPerLine: request.zscaleSamplesPerLine,
            rgbPreserveHue: 0
       });

    return RangeValues.serializeRV(rv);
}
