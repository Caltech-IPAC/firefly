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

import React, {Component, PropTypes} from 'react';
import {get} from 'lodash';

import FormPanel from './FormPanel.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {ValidationField} from '../ui/ValidationField.jsx';
import {IbeSpacialType} from './IbeSpacialType.jsx';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {InputGroup} from '../ui/InputGroup.jsx';
import {ServerParams} from '../data/ServerParams.js';

import Validate from '../util/Validate.js';
import {dispatchHideDropDownUi} from '../core/LayoutCntlr.js';

import {TableRequest} from '../tables/TableRequest.js';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {dispatchSetupTblTracking} from '../visualize/TableStatsCntlr.js';
import {dispatchTableSearch} from '../tables/TablesCntlr.js';
import {FieldGroupTabs, Tab} from './panel/TabPanel.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {FileUpload} from '../ui/FileUpload.jsx';
import {parseWorldPt} from '../visualize/Point.js';
import * as TblUtil from '../tables/TableUtil.js';
import {dispatchAddImages,getAViewFromMultiView,getMultiViewRoot} from '../visualize/MultiViewCntlr.js';
import WebPlotRequest from '../visualize/WebPlotRequest.js';
import {dispatchPlotImage} from '../visualize/ImagePlotCntlr.js';
import {getDS9Region} from '../rpc/PlotServicesJson.js';

const options= [
    {label: 'AllWISE Source Catalog', value:'wise_allwise_p3as_psd', proj:'WISE'},
    {label: '2MASS All-Sky Point Source Catalog (PSC)', value:'fp_psc', proj:'2MASS'},
    {label: 'IRAS Point Source Catalog v2.1 (PSC)', value:'iraspsc', proj: 'IRAS'}
];


export class TestQueriesPanel extends Component {

    constructor(props) {
        super(props);
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted= false;
    }

    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= FieldGroupUtils.bindToStore('TEST_CAT_PANEL', (fields) => {
            if (this.iAmMounted) this.setState(fields);
        });
    }

    render() {
        const fields= this.state;
        return (
            <div style={{padding: 10}}>
                <FormPanel
                    width='640px' height='500px'
                    groupKey='TEST_CAT_PANEL'
                    onSubmit={(request) => onSearchSubmit(request)}
                    onCancel={hideSearchPanel}>
                    <FieldGroup groupKey='TEST_CAT_PANEL' validatorFunc={null} keepState={true}>
                        <div style={{padding:'5px 0 5px 0'}}>
                            <TargetPanel/>
                        </div>
                        <FieldGroupTabs initialState= {{ value:'catalog' }} fieldKey='Tabs'>
                            <Tab name='Test Catalog' id='catalog'>{renderCatalogTab()}</Tab>
                            <Tab name='Images' id='images'>{renderImagesTab()}</Tab>
                            <Tab name='Wise Search' id='wiseImage'><div>{renderWiseSearch(fields)}</div></Tab>
                            <Tab name='2Mass Search' id='2massImage'><div>{render2MassSearch(fields)}</div></Tab>
                            <Tab name='Load Region' id='loadRegion'><div>{renderLoadRegion(fields)}</div></Tab>

                        </FieldGroupTabs>

                    </FieldGroup>
                </FormPanel>
            </div>
        );

    }


}




TestQueriesPanel.propTypes = {
    name: PropTypes.oneOf(['TestSearches']),
};

TestQueriesPanel.defaultProps = {
    name: 'TestSearches',
};


function hideSearchPanel() {
    dispatchHideDropDownUi();
}


function renderCatalogTab() {
    return (
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
        );
}



const wiseBandMap= {
    'allwise-multiband' : ['1','2','3','4'],
    'allsky_4band-1b' :['1','2','3','4'],
    'allsky_4band-3a' :['1','2','3','4'],
    'cryo_3band-1b' :['1','2','3'],
    'cryo_3band-1b-3a' :['1','2','3'],
    'postcryo-1b' :['1','2']
};


function renderWiseSearch(fields) {
    const ds= get(fields,'wiseDataSet.value', 'allwise-multiband');
    const options= wiseBandMap[ds].map( (w) =>  ({label:'W'+w, value:w }));
    return (
        <div style={{padding:5, display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center' }}>
            <IbeSpacialType groupKey='TEST_CAT_PANEL'/>
            <div style={{padding:5, display:'flex', flexDirection:'row', flexWrap:'no-wrap', alignItems:'center' }}>
                <div style={{display:'inline-block', paddingTop:10}}>
                    <RadioGroupInputField
                        fieldKey='wiseDataSet'
                        inline={true}
                        alignment='vertical'
                        initialState={{
                        tooltip: 'Choose Wise Data set',
                        value: 'allwise-multiband'
                    }}
                        options={[
                        {label: 'AllWISE (multi-band) Atlas', value: 'allwise-multiband'},
                        {label: 'AllSky (4 band) Single Exposure', value: 'allsky_4band-1b'},
                        {label: 'AllSky (4 band) Atlas', value: 'allsky_4band-3a'},
                        {label: '3-Band Cryo Single Exposure', value: 'cryo_3band-1b'},
                        {label: '3-Band Cryo Atlas', value: 'cryo_3band-1b-3a'},
                        {label: 'Post-Cryo (2 band) Single Exposure', value: 'postcryo-1b'},
                    ]}
                    />
                </div>
                <div style={{display:'inline-block', paddingLeft:50}}>
                    <CheckboxGroupInputField
                        fieldKey='wiseBands'
                        initialState= {{
                                    value: '_all_',
                                    tooltip: 'Please select some boxes',
                                    label : 'Bands:' }}
                        options={options}
                        alignment='horizontal'
                        labelWidth={35}
                    />

                </div>
            </div>
        </div>

    );
}


function render2MassSearch(fields) {


    return (
        <div style={{padding:5, display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center' }}>
            <RadioGroupInputField
                fieldKey='ds'
                alignment='vertical'
                initialState={{
                        tooltip: 'Spacial Type',
                        value: 'Cone'
                    }}
                options={[
                      {label: '2MASS All Sky', value: 'asky'},
                      {label: '2MASS Full Survey', value: 'askyw'},
                      {label: '2MASS 6X Catalog Images', value: 'sx'},
                      {label: '2MASS Full 6X Images', value: 'sxw'},
                      {label: '2MASS Calibration Images', value: 'cal'}
                    ]}
            />
            <RadioGroupInputField
                fieldKey='band'
                alignment='horizontal'
                initialState={{
                        tooltip: 'Return Band',
                        value: 'A'
                    }}
                options={[
                       {label : 'All 2MASS Bands', value: 'A'},
                       {label : '2MASS J-Band', value: 'J'},
                       {label : '2MASS H-Band', value: 'H'},
                       {label : '2MASS Ks-Band', value: 'K'}
                    ]}
            />
        </div>
    );
    
}



function renderLoadRegion(fields) {
    return (
        <div style={{padding:5, display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center' }}>
            <FileUpload
                wrapperStyle = {{margin: '5px 0'}}
                fieldKey = 'fileUpload'
                initialState= {{
                        tooltip: 'Select a region file to upload',
                        label: 'Upload File:'}}
            />
        </div>
    );

}




function renderImagesTab() {
    return (
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
    );

}


function onSearchSubmit(request) {
    console.log(request);
    if (request.Tabs==='catalog') {
        doCatalog(request);
    }
    else if (request.Tabs==='images') {
        doImages(request);
    }
    else if (request.Tabs==='wiseImage') {
        doWise(request);
    }
    else if (request.Tabs==='2massImage') {
        do2Mass(request);
    }
    else if (request.Tabs==='loadRegion') {
        doRegionLoad(request);
    }
    else {
        console.log('request no supported');
    }
}

function doCatalog(request) {
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
    dispatchTableSearch(tReq);
}


function doImages(request) {
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
    
    var viewer= getAViewFromMultiView(getMultiViewRoot());
    dispatchAddImages(viewer.viewerId, ['TestImage1', 'TestImage2', 'TestImage3', 'TestImage4', 'TestImage3Color']);
    dispatchHideDropDownUi();

}


const schemaParams= {
    'allwise-multiband':  {ImageSet:'allwise-multiband', ProductLevel:'3a' } ,
    'allsky_4band-1b':  {ImageSet:'allsky-4band', ProductLevel:'1b' },
    'allsky_4band-3a':  {ImageSet:'allsky-4band', ProductLevel:'3a' },
    'cryo_3band-1b':  {ImageSet:'cryo_3band', ProductLevel:'1b' },
    'cryo_3band-1b-3a': {ImageSet:'cryo_3band', ProductLevel:'3a' },
    'postcryo-1b':  {ImageSet:'postcryo', ProductLevel:'1b'  },
    'neowiser-1b':  {ImageSet:'neowiser', ProductLevel:'1b'  },
};


function doWise(request) {
    console.log('wise',request);
    const reqParams= Object.assign({
        [ServerParams.USER_TARGET_WORLD_PT] : request[ServerParams.USER_TARGET_WORLD_PT],
        mission: 'wise',
        id: 'ibe_processor',
        title: 'WISE-' + request[ServerParams.USER_TARGET_WORLD_PT],
        intersect: request.intersect,
        mcenter:  (request.intersect==='CENTER' || request.intersect==='COVERS') ? request.mcenter : 'all',
        size: request.size,
        subsize: request.subsize,
        band : request.wisebands
    }, schemaParams[request.wiseDataSet]);
    
    dispatchTableSearch(TableRequest.newInstance(reqParams));
}

function do2Mass(request) {
    console.log('wmass',request);
    const reqParams= {
        [ServerParams.USER_TARGET_WORLD_PT] : request[ServerParams.USER_TARGET_WORLD_PT],
        mission: 'twomass',
        id: 'ibe_processor',
        title: '2MASS-' + request[ServerParams.USER_TARGET_WORLD_PT],
        ds : request.ds,
        band : request.band
    };

    dispatchTableSearch(TableRequest.newInstance(reqParams));
}



function doRegionLoad(request) {
    getDS9Region(request.fileUpload)
        .then( (result) => {
            console.log(result);

        });
    console.log('load region');
    
}

