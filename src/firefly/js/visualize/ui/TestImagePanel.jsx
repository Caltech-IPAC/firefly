/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react/addons';
import TargetPanel from '../../ui/TargetPanel.jsx';
import InputGroup from '../../ui/InputGroup.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import ValidationField from '../../ui/ValidationField.jsx';
import Validate from '../../util/Validate.js';
import FieldGroup from '../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';

import WebPlotRequest, {ServiceType} from '../WebPlotRequest.js';
import ImagePlotCntlr from '../ImagePlotCntlr.js';
import {makeWorldPt, parseWorldPt} from '../Point.js';
import ImageViewer from './ImageViewer.jsx';




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






var TestImagePanel = React.createClass({
    showResults(success, request) {
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
    },

    resultsFail(request) {
        this.showResults(false,request);
    },

    resultsSuccess(request) {
        this.showResults(true,request);
    },

    render() {
        return (
            <div>
                <div style={{display:'inline-block', verticalAlign:'top'}}>
                    <FieldGroup groupKey='TEST_IMAGE_PANEL' reducerFunc={ipReducer} validatorFunc={null} keepState={true}>
                        <TargetPanel groupKey='TEST_IMAGE_PANEL' />
                        <ValidationField fieldKey={'zoom'}
                                         groupKey='TEST_IMAGE_PANEL'/>
                        <div style={{height:10}}/>
                        <CompleteButton groupKey='TEST_IMAGE_PANEL'
                                        onSuccess={this.resultsSuccess}
                                        onFail={this.resultsFail}
                        />
                    </FieldGroup>
                </div>
                <div style={{display:'inline-block', width:400,height:400,border:'1px solid black', marginLeft:10}}>
                    <ImageViewer plotId='TestImage1'/>
                </div>
                <div style={{display:'inline-block', width:400,height:400,border:'1px solid black', marginLeft:10}}>
                    <ImageViewer plotId='TestImage2'/>
                </div>
            </div>
        );

    }
});

export default TestImagePanel;
