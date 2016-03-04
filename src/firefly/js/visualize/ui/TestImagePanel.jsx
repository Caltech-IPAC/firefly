/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import TargetPanel from '../../ui/TargetPanel.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import ValidationField from '../../ui/ValidationField.jsx';
import Validate from '../../util/Validate.js';
import FieldGroup from '../../ui/FieldGroup.jsx';
import {showExampleDialog} from '../../ui/ExampleDialog.jsx';

import WebPlotRequest, {ServiceType, AnnotationOps} from '../WebPlotRequest.js';
import {visRoot,dispatchPlotImage} from '../ImagePlotCntlr.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {getDrawLayerByType, getActivePlotView, isDrawLayerAttached} from '../PlotViewUtil.js';
import AppDataCntlr from '../../core/AppDataCntlr.js';
import {parseWorldPt} from '../Point.js';
import {ImageViewer} from './../iv/ImageViewer.jsx';
import {UserZoomTypes} from '../ZoomUtil.js';
import SelectArea from '../../drawingLayers/SelectArea.js';
import DistanceTool from '../../drawingLayers/DistanceTool.js';
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
                value: '1',
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
    //var wpr1= WebPlotRequest.makePlotServiceReq(ServiceType.TWOMASS, wp,'h',.1 );
    //var wpr2= WebPlotRequest.makePlotServiceReq(ServiceType.TWOMASS, wp,'k',.1 );
    var wpr1= WebPlotRequest.makeWiseRequest(wp,'1b','1',.4 );
    var wpr2= WebPlotRequest.makeWiseRequest(wp,'1b','2',.4 );
    var wpr3= WebPlotRequest.makeWiseRequest(wp,'1b','3',.4 );
    var wpr4= WebPlotRequest.makeWiseRequest(wp,'1b','4',.4 );
    //var wpr2= WebPlotRequest.makeDSSRequest(wp,'poss2ukstu_red',.1 );
    wpr1.setPlotGroupId('test-group');
    wpr2.setPlotGroupId('test-group');
    wpr3.setPlotGroupId('test-group');
    wpr4.setPlotGroupId('test-group');




    wpr1.setInitialZoomLevel(parseFloat(request.zoom));
    wpr2.setInitialZoomLevel(parseFloat(request.zoom));
    wpr3.setInitialZoomLevel(parseFloat(request.zoom));
    wpr4.setInitialZoomLevel(parseFloat(request.zoom));

    wpr2.setInitialColorTable(4);


    //=========== 3 color
    var cWpr1= WebPlotRequest.makeWiseRequest(wp,'3a','1',.4 );
    var cWpr2= WebPlotRequest.makeWiseRequest(wp,'3a','2',.4 );
    var cWpr3= WebPlotRequest.makeWiseRequest(wp,'3a','3',.4 );
    cWpr1.setPlotGroupId('test-group');
    cWpr2.setPlotGroupId('test-group');
    cWpr3.setPlotGroupId('test-group');

    cWpr1.setInitialZoomLevel(parseFloat(request.zoom));
    cWpr2.setInitialZoomLevel(parseFloat(request.zoom));
    cWpr3.setInitialZoomLevel(parseFloat(request.zoom));




    //wpr1.setAnnotationOps(AnnotationOps.TITLE_BAR);
    dispatchPlotImage('TestImage1', wpr1);
    dispatchPlotImage('TestImage2', wpr2);
    dispatchPlotImage('TestImage3', wpr3);
    dispatchPlotImage('TestImage4', wpr4);

    dispatchPlotImage('TestImage3Color', [cWpr1,cWpr2,cWpr3],true);



    //====temp
}

function resultsFail(request) {
    showResults(false,request);
}

function resultsSuccess(request) {
    showResults(true,request);
}





function showExDialog() {
    console.log('showing example dialog');
    showExampleDialog();
}

function TestImagePanelView({selectOn,distOn}) {
    var s = AppDataCntlr.getCommandState('SelectAreaCmd');
    var selectText = (selectOn) ? 'Turn Select Off' : 'Turn Select On';
    var distText = (distOn) ? 'Turn Distance Tool Off' : 'Turn Distance Tool On';
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
                    <button type='button' onClick={showExDialog}>Example Dialog</button>
                    
                </FieldGroup>
            </div>
            <div style={{display:'inline-block', width:400,height:400,marginLeft:10}}>
                <ImageViewer plotId='TestImage3Color'/>
            </div>
            <div style={{display:'inline-block', width:400,height:400,marginLeft:10}}>
                <ImageViewer plotId='TestImage1'/>
            </div>
            <div style={{display:'inline-block', width:400,height:400,marginLeft:10}}>
                <ImageViewer plotId='TestImage2'/>
            </div>
            <div style={{display:'inline-block', width:400,height:400,marginLeft:10}}>
                <ImageViewer plotId='TestImage3'/>
            </div>
            <div style={{display:'inline-block', width:400,height:400,marginLeft:10}}>
                <ImageViewer plotId='TestImage4'/>
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
           var pv= getActivePlotView(visRoot());
           var selectOn= false;
           var distOn= false;
           if (pv) {
               var dlAry= getDlAry();
               const selectLayer= getDrawLayerByType(dlAry,SelectArea.TYPE_ID);
               selectOn=  isDrawLayerAttached(selectLayer,pv.plotId);
               const distLayer= getDrawLayerByType(dlAry,DistanceTool.TYPE_ID);
               distOn=  isDrawLayerAttached(distLayer,pv.plotId);
           }
           this.setState({selectOn,distOn});
        });
    },

    render() {
        var selectOn= (this.state && this.state.selectOn) ? this.state.selectOn : false;
        var distOn= (this.state && this.state.distOn) ? this.state.distOn : false;
        return (<TestImagePanelView selectOn={selectOn} distOn={distOn}/>);
    }

    // end code that connects to store
});


export default TestImagePanel;
