/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useState, useEffect, useRef} from 'react';
import shallowequal from 'shallowequal';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {CompleteButton} from '../../ui/CompleteButton.jsx';
import {Band} from '../Band.js';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {RadioGroupInputFieldView} from '../../ui/RadioGroupInputFieldView.jsx';
import FieldGroupUtils, {getFieldGroupResults, validateFieldGroup} from '../../fieldGroup/FieldGroupUtils.js';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {dispatchInitFieldGroup} from '../../fieldGroup/FieldGroupCntlr.js';
import {ColorBandPanel} from './ColorBandPanel.jsx';
import {ColorRGBHuePreservingPanel} from './ColorRGBHuePreservingPanel.jsx';
import ImagePlotCntlr, {dispatchStretchChange, visRoot} from '../ImagePlotCntlr.js';
import {primePlot, getActivePlotView, isThreeColor} from '../PlotViewUtil.js';
import { RangeValues, ZSCALE, STRETCH_ASINH}from '../RangeValues.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {hasLocalRawData, isHiPS, isImage} from '../WebPlot';
import {useStoreConnector} from '../../ui/SimpleComponent';
import {FieldGroupTabs, Tab} from '../../ui/panel/TabPanel.jsx';

import {RED_PANEL,
    GREEN_PANEL,
    BLUE_PANEL,
    NO_BAND_PANEL,
    RGB_HUEPRESERVE_PANEL,
    colorPanelChange, rgbHuePreserveChange} from './ColorPanelReducer.js';
import {debounce} from 'lodash';



export function showColorDialog(element) {
    const content= (
        <PopupPanel title={'Modify Color Stretch'} >
            <ColorDialog />
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('ColorStretchDialog', content, element);
    const watchActions= [ImagePlotCntlr.ANY_REPLOT, ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW];
    dispatchInitFieldGroup( NO_BAND_PANEL, true, null, colorPanelChange(Band.NO_BAND), watchActions);
    dispatchInitFieldGroup( RED_PANEL, true, null, colorPanelChange(Band.RED), watchActions);
    dispatchInitFieldGroup( GREEN_PANEL, true, null, colorPanelChange(Band.GREEN), watchActions);
    dispatchInitFieldGroup( BLUE_PANEL, true, null, colorPanelChange(Band.BLUE), watchActions);
    dispatchInitFieldGroup( RGB_HUEPRESERVE_PANEL, true, null, rgbHuePreserveChange([Band.RED, Band.GREEN, Band.BLUE]), watchActions);
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

const doReplotStandard= debounce((request) => replotStandard(request), 600);

export const ColorDialog= memo(() => {
    const [{plot,fields,rFields,gFields,bFields,rgbFields}]= useStoreConnector(getStoreUpdate);
    const [huePreserving, setHuePreserving]= useState(plot?.plotState.getRangeValues().rgbPreserveHue);
    const localRawData= hasLocalRawData(plot);
    const {current:lastResultsRef} = useRef({lastResults:undefined, init:true});
    const threeColor= isThreeColor(plot);
    useEffect( () => {
        if (!localRawData) return;
        if (threeColor) return;
        const results = getFieldGroupResults(NO_BAND_PANEL);
        const {upperRange, lowerRange}= results;
        if (upperRange && lowerRange && !lastResultsRef.init && !shallowequal(results, lastResultsRef.lastResults)) {
            lastResultsRef.lastResults = results;
            validateFieldGroup(NO_BAND_PANEL).then((valid) => {
                if (valid) {
                    doReplotStandard(results);
                }
            });
        }
        lastResultsRef.init= false;
    });

    if (!plot) return false;


    if (isImage(plot)) {
        return threeColor ?
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
            wrapperStyle={{padding: 5}}
            value={isHuePreservingSelected ? 'huePreserving' : 'perBand'}
            onChange={(ev) => setHuePreserving(ev.target.value==='huePreserving')}
        />;

    return (
        <div>
            {threeColorStretchMode}
            {Boolean(isHuePreservingSelected) && renderHuePreservingThreeColorView(plot, rgbFields)}
            {!isHuePreservingSelected && renderStandardThreeColorView(plot, rFields, gFields, bFields)}
        </div>
    );
}

function renderHuePreservingThreeColorView(plot,rgbFields) {
    const groupKey = RGB_HUEPRESERVE_PANEL;
    return (
        <div style={{paddingTop:4}}>
            <FieldGroup groupKey={groupKey} keepState={false}>
                <ColorRGBHuePreservingPanel {...{plot, rgbFields, groupKey}}/>
                <div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
                    <CompleteButton
                        closeOnValid={false}
                        style={{padding: '2px 0 7px 10px'}}
                        onSuccess={(request)=>replot3ColorHuePreserving(request)}
                        onFail={invalidMessage}
                        text='Refresh'
                        dialogId='ColorStretchDialog'
                    />
                    <div style={{ textAlign:'right', padding: '2px 10px'}}>
                        <HelpIcon helpId='visualization.stretches'/>
                    </div>
                </div>
            </FieldGroup>
        </div>

    );
}

function renderStandardThreeColorView(plot,rFields,gFields,bFields) {
    const {plotState}= plot;
    const usedBands = plotState? plotState.usedBands:null;
    return (
        <div style={{paddingTop:4}}>
            <FieldGroup groupKey={'colorDialogTabs'} keepState={false}>
                <FieldGroupTabs initialState= {{ value:'red' }} fieldKey='colorTabs'>
                    {plotState.isBandUsed(Band.RED) &&
                    <Tab name='Red' id='red'>
                        <FieldGroup groupKey={RED_PANEL} keepState={true} >
                            <ColorBandPanel groupKey={RED_PANEL} band={Band.RED} fields={rFields}
                                            plot={plot} key={Band.RED.key}/>
                        </FieldGroup>
                    </Tab>
                    }

                    {plotState.isBandUsed(Band.GREEN) &&
                    <Tab name='Green' id='green'>
                        <FieldGroup groupKey={GREEN_PANEL} keepState={true} >
                            <ColorBandPanel groupKey={GREEN_PANEL} band={Band.GREEN} fields={gFields}
                                            plot={plot} key={Band.GREEN.key}/>
                        </FieldGroup>
                    </Tab>
                    }

                    {plotState.isBandUsed(Band.BLUE) &&
                    <Tab name='Blue' id='blue'>
                        <FieldGroup groupKey={BLUE_PANEL} keepState={true} >
                            <ColorBandPanel groupKey={BLUE_PANEL} band={Band.BLUE} fields={bFields}
                                            plot={plot} key={Band.BLUE.key}/>
                        </FieldGroup>
                    </Tab>
                    }
                </FieldGroupTabs>
                <CompleteButton
                    groupKey={['colorDialogTabs',RED_PANEL,GREEN_PANEL,BLUE_PANEL]}
                    closeOnValid={false}
                    style={{padding: '2px 0 7px 10px'}}
                    onSuccess={replot(usedBands)}
                    onFail={invalidMessage}
                    text='Refresh'
                    dialogId='ColorStretchDialog'
                    includeUnmounted={true}
                />
            </FieldGroup>
        </div>

    );

}


function renderStandardView(plot,fields) {


    return (
        <div>
            <FieldGroup groupKey={NO_BAND_PANEL} keepState={true} >
                <ColorBandPanel groupKey={NO_BAND_PANEL} band={Band.NO_BAND} fields={fields} plot={plot}/>
                <CompleteButton
                    closeOnValid={false}
                    style={{padding: '2px 0 7px 10px'}}
                    onSuccess={replot()}
                    onFail={invalidMessage}
                    text='Refresh'
                    dialogId='ColorStretchDialog'

                />
            </FieldGroup>
        </div>
       );
}

function replot(usedBands=null) {

    return (request)=> {

        if (request.colorDialogTabs) {

            replot3Color(
                request.redPanel, request.greenPanel,
                request.bluePanel,
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
