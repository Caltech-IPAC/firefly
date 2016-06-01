/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import lodash from 'lodash';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {CompleteButton} from '../../ui/CompleteButton.jsx';
import {Band} from '../Band.js';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {dispatchInitFieldGroup} from '../../fieldGroup/FieldGroupCntlr.js';
import {ColorBandPanel} from './ColorBandPanel.jsx';
import ImagePlotCntlr, {dispatchStretchChange, visRoot} from '../ImagePlotCntlr.js';
import {primePlot, getActivePlotView} from '../PlotViewUtil.js';
import {flux} from '../../Firefly.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {FieldGroupTabs, Tab} from '../../ui/panel/TabPanel.jsx';

import {RED_PANEL,
        GREEN_PANEL,
        BLUE_PANEL,
        NO_BAND_PANEL,
        colorPanelChange} from './ColorPanelReducer.js';


import { RangeValues, ZSCALE,STRETCH_ASINH, STRETCH_POWERLAW_GAMMA}from '../RangeValues.js';


export function showColorDialog() {
    const content= (
        <PopupPanel title={'Modify Color Stretch'} >
            <ColorDialog />
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('ColorStretchDialog', content);
    const watchActions= [ImagePlotCntlr.ANY_REPLOT, ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW];
    dispatchInitFieldGroup( NO_BAND_PANEL, true, null, colorPanelChange(Band.NO_BAND), watchActions);
    dispatchInitFieldGroup( RED_PANEL, true, null, colorPanelChange(Band.RED), watchActions);
    dispatchInitFieldGroup( GREEN_PANEL, true, null, colorPanelChange(Band.GREEN), watchActions);
    dispatchInitFieldGroup( BLUE_PANEL, true, null, colorPanelChange(Band.BLUE), watchActions);
    dispatchShowDialog('ColorStretchDialog');
}

class ColorDialog extends Component {

    constructor(props) {
        super(props);
        const plot= primePlot(visRoot());
        const fields= FieldGroupUtils.getGroupFields(NO_BAND_PANEL);
        const rFields= FieldGroupUtils.getGroupFields(RED_PANEL);
        const gFields= FieldGroupUtils.getGroupFields(GREEN_PANEL);
        const bFields= FieldGroupUtils.getGroupFields(BLUE_PANEL);
        this.state= {plot, fields, rFields, gFields, bFields};
    }
    
    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted= false;
    }

    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= flux.addListener(() => this.storeUpdate());

    }

    storeUpdate() {
        var {state}= this;
        const plot= primePlot(visRoot());

        const fields= FieldGroupUtils.getGroupFields(NO_BAND_PANEL);
        const rFields= FieldGroupUtils.getGroupFields(RED_PANEL);
        const gFields= FieldGroupUtils.getGroupFields(GREEN_PANEL);
        const bFields= FieldGroupUtils.getGroupFields(BLUE_PANEL);


        if (plot!=state.plot || fields!=state.fields ||
            rFields!=state.rFields || gFields!=state.gFields || bFields!=state.bFields) {
            this.setState({plot, fields, rFields, gFields, bFields});
        }
    }


    render() {
        const {plot,fields, rFields,gFields,bFields}= this.state;
        if (!plot) return false;


        if (plot.plotState.isThreeColor()) {
            return renderThreeColorView(plot,rFields,gFields,bFields);
        }
        else {
            return renderStandardView(plot,fields);
        }
    }
}

function renderThreeColorView(plot,rFields,gFields,bFields) {
    const {plotState}= plot;

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
                                            plot={plot}key={Band.GREEN.key}/>
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
                    onSuccess={replot}
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
                    onSuccess={replot}

                    onFail={invalidMessage}
                    text='Refresh'
                    dialogId='ColorStretchDialog'

                />
            </FieldGroup>
        </div>
       );
}

function replot(request) {
    if (request.colorDialogTabs) {
        replot3Color(
            request.redPanel,request.greenPanel,
            request.bluePanel,
            request.colorDialogTabs.colorTabs);
    }
    else {
        replotStandard(request);
    }

}


function invalidMessage() {
    showInfoPopup('One or more fields are not valid', 'Invalid Data');
}

function replotStandard(request) {
    // console.log(request);

    var serRv=  makeSerializedRv(request);
    const stretchData= [{ band : Band.NO_BAND.key, rv :  serRv, bandVisible: true }];
    const pv= getActivePlotView(visRoot());
    if (pv) dispatchStretchChange({plotId:pv.plotId,stretchData});
}

/**
 *
 * @param redReq
 * @param greenReq
 * @param blueReq
 * @param activeTab - which tab is active, might be used in future
 */
function replot3Color(redReq,greenReq,blueReq,activeTab) {
    // console.log('activeTab',activeTab);
    // console.log('red',redReq);
    // console.log('green',greenReq);
    // console.log('blue',blueReq);
    const stretchData= [];

    if (redReq)   stretchData.push({band : Band.RED.key, rv : makeSerializedRv(redReq) , bandVisible: true });
    if (greenReq) stretchData.push({band : Band.GREEN.key, rv : makeSerializedRv(greenReq) , bandVisible: true });
    if (blueReq)  stretchData.push({band : Band.BLUE.key, rv : makeSerializedRv(blueReq) , bandVisible: true });

    const pv= getActivePlotView(visRoot());
    if (pv) dispatchStretchChange({plotId:pv.plotId,stretchData});
}


function makeSerializedRv(request) {
    const useZ= Boolean(request.zscale);

    const rv= RangeValues.make( useZ ? ZSCALE : request.lowerWhich,
        request.lowerRange,
        useZ ? ZSCALE :request.upperWhich,
        request.upperRange,
        request.beta,
        request.gamma, request.algorithm, request.zscaleContrast,
        request.zscaleSamples, request.zscaleSamplesPerLine);

    return RangeValues.serializeRV(rv);
}
