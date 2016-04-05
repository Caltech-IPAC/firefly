/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// THIS PANEL IS TEMPORARY, ONLY TO TEST CATALOGS UNTIL WE FINISH THE REAL PANEL
// This panel will do search on 3 of the most common IRSA catalogs
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

import React, {PropTypes} from 'react';

import FormPanel from './FormPanel.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {ValidationField} from '../ui/ValidationField.jsx';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {InputGroup} from '../ui/InputGroup.jsx';
import {ListBoxInputField} from '../ui/ListBoxInputField.jsx';
import {ServerParams} from '../data/ServerParams.js';

import Validate from '../util/Validate.js';
import {dispatchHideDropDownUi} from '../core/LayoutCntlr.js';

import {TableRequest} from '../tables/TableRequest.js';
import {dispatchSetupTblTracking} from '../visualize/TableStatsCntlr.js';
import {dispatchTableSearch} from '../tables/TablesCntlr.js';
import {FieldGroupTabs, Tab} from './panel/TabPanel.jsx';
import {parseWorldPt} from '../visualize/Point.js';
import * as TblUtil from '../tables/TableUtil.js';
import {dispatchAddImages,getAViewFromMultiView} from '../visualize/MultiViewCntlr.js';
import WebPlotRequest from '../visualize/WebPlotRequest.js';
import {dispatchPlotImage} from '../visualize/ImagePlotCntlr.js';

const options= [
    {label: 'AllWISE Source Catalog', value:'wise_allwise_p3as_psd', proj:'WISE'},
    {label: '2MASS All-Sky Point Source Catalog (PSC)', value:'fp_psc', proj:'2MASS'},
    {label: 'IRAS Point Source Catalog v2.1 (PSC)', value:'iraspsc', proj: 'IRAS'}
];

/**
 *
 * @param props
 * @return {XML}
 * @constructor
 */
export const TestCatalog = (props) => {
    const {resultId} = props;
    return (
        <div style={{padding: 10}}>
            <FormPanel
                width='640px' height='300px'
                groupKey='TEST_CAT_PANEL'
                onSubmit={(request) => onSearchSubmit(request, resultId)}
                onCancel={hideSearchPanel}>
                <FieldGroup groupKey='TEST_CAT_PANEL' validatorFunc={null} keepState={true}>
                    <div style={{padding:'5px 0 5px 0'}}>
                        <TargetPanel/>
                    </div>
                    <FieldGroupTabs initialState= {{ value:'catalog' }} fieldKey='Tabs'>
                        <Tab name='Test Catalog' id='catalog'>
                            <div style={{padding:5}}>

                                <InputGroup labelWidth={110}>
                                    <ListBoxInputField  initialState= {{
                                          tooltip: 'Select Catalog',
                                          label : 'Select Catalog:'
                                      }}
                                                        options={options }
                                                        multiple={false}
                                                        fieldKey='catalog'
                                    />


                                    <ValidationField fieldKey='radius'
                                                     initialState= {{
                                          fieldKey: 'radius',
                                          value: '300',
                                          validator: Validate.floatRange.bind(null, 0, 2000, 3,'field 3'),
                                          tooltip: 'radius',
                                          label : 'radius:',
                                          labelWidth : 100
                                      }} />
                                </InputGroup>
                            </div>
                        </Tab>
                        <Tab name='Images' id='images'>
                            <div style={{padding:'5px 0 10px 10px'}}>
                                <ValidationField fieldKey={'zoom'}
                                                 initialState={{
                                                        value: '1',
                                                        validator: Validate.floatRange.bind(null, .1, 10, 'my zoom field'),
                                                        tooltip: 'this is a tip for zoom',
                                                        label: 'Zoom:',
                                                        labelWidth : 100
                                                  }}
                                />
                            </div>
                        </Tab>

                    </FieldGroupTabs>

                </FieldGroup>
            </FormPanel>
        </div>
    );
};

TestCatalog.propTypes = {
    name: PropTypes.oneOf(['TestACatalog']),
    resultId: PropTypes.string
};

TestCatalog.defaultProps = {
    name: 'TestACatalog',
    resultId: TblUtil.uniqueTblUiGid()
};


function hideSearchPanel() {
    dispatchHideDropDownUi();
}




function onSearchSubmit(request, resultId) {
    console.log(request);
    if (request.Tabs==='catalog') {
        doCatalog(request,resultId);
    }
    else if (request.Tabs==='images') {
        doImages(request,resultId);
    }
    else {
        console.log('request no supported');

    }
}

function doCatalog(request, resultId) {
    const activeTblId = TblUtil.uniqueTblId();
    var tReq = TableRequest.newInstance({
        [ServerParams.USER_TARGET_WORLD_PT] : request[ServerParams.USER_TARGET_WORLD_PT],
        id:'GatorQuery',
        title : request.catalog,
        SearchMethod: 'Cone',
        catalog : request.catalog,
        RequestedDataSet :request.catalog,
        radius : request.radius,
        use : 'catalog_overlay',
        catalogProject : options.find( (op) => request.catalog===op.value).proj
    });
    const tbl_ui_id = TblUtil.uniqueTblUiId();
    dispatchSetupTblTracking(activeTblId);
    dispatchTableSearch(tReq, resultId, tbl_ui_id);
}


function doImages(request, resultsId) {
    var wp= parseWorldPt(request.UserTargetWorldPt);

    // -example call to 2mass
    //var wpr1= WebPlotRequest.makePlotServiceReq(ServiceType.TWOMASS, wp,'h',.1 );
    //var wpr2= WebPlotRequest.makePlotServiceReq(ServiceType.TWOMASS, wp,'k',.1 );



    // -example call to wise
    var wpr1= WebPlotRequest.makeWiseRequest(wp,'1b','1',.4 );
    var wpr2= WebPlotRequest.makeWiseRequest(wp,'1b','2',.4 );
    var wpr3= WebPlotRequest.makeWiseRequest(wp,'1b','3',.4 );
    var wpr4= WebPlotRequest.makeWiseRequest(wp,'1b','4',.4 );


    // -example call to IRIS
    // var wpr1= WebPlotRequest.makeIRISRequest(wp,'12',5);
    // var wpr2= WebPlotRequest.makeIRISRequest(wp,'25',5);
    // var wpr3= WebPlotRequest.makeIRISRequest(wp,'60',5);
    // var wpr4= WebPlotRequest.makeIRISRequest(wp,'100', 5);

    // -example call to ISSA
    // var wpr1= WebPlotRequest.makeISSARequest(wp,'12',5);
    // var wpr2= WebPlotRequest.makeISSARequest(wp,'25',5);
    // var wpr3= WebPlotRequest.makeISSARequest(wp,'60',5);
    // var wpr4= WebPlotRequest.makeISSARequest(wp,'100', 5);


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
    
    var viewer= getAViewFromMultiView();
    dispatchAddImages(viewer.viewerId, ['TestImage1', 'TestImage2', 'TestImage3', 'TestImage4', 'TestImage3Color']);
    dispatchHideDropDownUi();

}
