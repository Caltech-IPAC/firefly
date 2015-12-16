/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import TargetPanel from '../../ui/TargetPanel.jsx';
import InputGroup from '../../ui/InputGroup.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import ValidationField from '../../ui/ValidationField.jsx';
import Validate from '../../util/Validate.js';
import FieldGroup from '../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {showExampleDialog} from '../../ui/ExampleDialog.jsx';

import WebPlotRequest, {ServiceType} from '../WebPlotRequest.js';
import ImagePlotCntlr from '../ImagePlotCntlr.js';
import DrawingLayerCntlr from '../DrawingLayerCntlr.js';
import PlotViewUtils from '../PlotViewUtil.js';
import AppDataCntlr from '../../core/AppDataCntlr.js';
import {makeWorldPt, parseWorldPt} from '../Point.js';
import ImageViewer from './ImageViewer.jsx';
import ZoomUtil from '../ZoomUtil.js';
import {showFitsDownloadDialog} from '../../ui/FitsDownloadDialog.jsx';
import SelectArea from '../../drawingLayers/SelectArea.js';
import {flux} from '../../Firefly.js';



/**
 *
 * @param {object} inFields
 * @param {object} action
 * @return {object}
 */
var ipReducer= function(inFields, action) {
    if (!inFields)  {
        return {
            zoom: {
                fieldKey: 'zoom',
                value: '3',
                validator: Validate.floatRange.bind(null, .1, 10, 'my zoom field'),
                tooltip: 'this is a tip for zoom',
                label: 'Zoom:'
            }
        };
    }
    else {
        return inFields;
    }
};


function showResults(success, request) {
    console.log(request);
    //====temp
    //var wp= makeWorldPt(1,1);
    var wp= parseWorldPt(request.UserTargetWorldPt);
    var wpr1= WebPlotRequest.makePlotServiceReq(ServiceType.TWOMASS, wp,'h',.1 );
    var wpr2= WebPlotRequest.makePlotServiceReq(ServiceType.TWOMASS, wp,'k',.1 );
    //var wpr2= WebPlotRequest.makeDSSRequest(wp,'poss2ukstu_red',.1 );
    wpr1.setPlotGroupId('2massGroup');
    wpr2.setPlotGroupId('2massGroup');
    wpr1.setInitialZoomLevel(parseFloat(request.zoom));
    wpr2.setInitialZoomLevel(parseFloat(request.zoom));
    wpr2.setInitialColorTable(4);
    ImagePlotCntlr.dispatchPlotImage('TestImage1', wpr1);
    ImagePlotCntlr.dispatchPlotImage('TestImage2', wpr2);
    //====temp
}

function resultsFail(request) {
    showResults(false,request);
}

function resultsSuccess(request) {
    showResults(true,request);
}


function selectArea() {
    var s= AppDataCntlr.getCommandState('SelectAreaCmd');
    var plotView= PlotViewUtils.getActivePlotView();
    if (!plotView) return;

    var plotIdAry= PlotViewUtils.getPlotViewIdListInGroup(plotView);
    if (!s) {
        s= {selectOn:true};
    }
    else {
        s= {selectOn:!s.selectOn};
    }

    AppDataCntlr.dispatchChangeCommandState('SelectAreaCmd',s);

    if (s.selectOn) {

        SelectArea.dispatchInitSelectArea();
        DrawingLayerCntlr.dispatchAttachLayerToPlot(SelectArea.LAYER_ID,plotIdAry);
    }
    else {
        DrawingLayerCntlr.dispatchDetachLayerFromPlot(SelectArea.LAYER_ID,plotIdAry);
    }



}

function zoom(zType) {
    console.log(zType);
    switch (zType) {
        case 'up':
            ImagePlotCntlr.dispatchZoom('TestImage1',ZoomUtil.UserZoomTypes.UP);
            break;
        case 'down':
            ImagePlotCntlr.dispatchZoom('TestImage1',ZoomUtil.UserZoomTypes.DOWN);
            break;
        case 'fit':
            ImagePlotCntlr.dispatchZoom('TestImage1',ZoomUtil.UserZoomTypes.FIT);
            break;
        case 'fill':
            ImagePlotCntlr.dispatchZoom('TestImage1',ZoomUtil.UserZoomTypes.FILL);
            break;
        case '1x':
            ImagePlotCntlr.dispatchZoom('TestImage1',ZoomUtil.UserZoomTypes.ONE);
            break;

    }
}

function showExDialog() {
    console.log('showing example dialog');
    showExampleDialog();
}


function showFitsDialog() {
     console.log('showing Fits Download  dialog');
     showFitsDownloadDialog();
 }

function TestImagePanelView({selectOn}) {
    var s = AppDataCntlr.getCommandState('SelectAreaCmd');
    var selectText = (selectOn) ? 'Turn Select Off' : 'Turn Select On';
    return (
        <div>
            <div style={{display:'inline-block', verticalAlign:'top'}}>
                <FieldGroup groupKey='TEST_IMAGE_PANEL' reducerFunc={ipReducer} keepState={true}>
                    <TargetPanel groupKey='TEST_IMAGE_PANEL'/>
                    <ValidationField fieldKey={'zoom'}
                                     groupKey='TEST_IMAGE_PANEL'/>
                    <div style={{height:10}}/>
                    <CompleteButton groupKey='TEST_IMAGE_PANEL'
                                    onSuccess={resultsSuccess}
                                    onFail={resultsFail}
                    />
                    <div style={{height:50}}/>
                    <button type='button' onClick={() => zoom('down')}>Zoom Down</button>
                    <button type='button' onClick={() => zoom('up')}>Zoom Up</button>
                    <button type='button' onClick={() => zoom('fit')}>Zoom Fit</button>
                    <button type='button' onClick={() => zoom('fill')}>Zoom Fill</button>
                    <button type='button' onClick={() => zoom('1x')}>Zoom 1x</button>
                    <button type='button' onClick={() => selectArea()}>{selectText}</button>
                    <br/>
                    <button type='button' onClick={showExDialog}>Example Dialog</button>
                    <br/>
                    <button type='button' onClick={showFitsDialog}>Fits Download Dialog</button>
                </FieldGroup>
            </div>
            <div style={{display:'inline-block', width:400,height:400,marginLeft:10}}>
                <ImageViewer plotId='TestImage1'/>
            </div>
            <div style={{display:'inline-block', width:400,height:400,marginLeft:10}}>
                <ImageViewer plotId='TestImage2'/>
            </div>
        </div>
    );
}

var TestImagePanel= React.createClass({


    // code that connects to store
    // code that connects to store
    // code that connects to store

    componentWillUnmount() {
        if (this.unbinder) this.unbinder();
    },

    componentDidMount() {
       this.unbinder= flux.addListener( () => {
           if (this.isMounted) {
               var s= AppDataCntlr.getCommandState('SelectAreaCmd');
               this.setState({selectOn:(s && s.selectOn) });
            }
        });
    },

    render() {
        var selectOn= (this.state && this.state.selectOn) ? this.state.selectOn : false;
        return (<TestImagePanelView selectOn={selectOn}/>);
    }

    // end code that connects to store
});


export default TestImagePanel;
