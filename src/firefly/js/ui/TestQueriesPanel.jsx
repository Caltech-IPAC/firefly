/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// THIS PANEL IS TEMPORARY, ONLY TO TEST various searches
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {
    makeAreaDef, makeCheckboxDef, makeCircleDef, makeEnumDef, makeFloatDef, makeIntDef, makePolygonDef, makeTargetDef,
    makeUnknownDef
} from './dynamic/DynamicDef.js';
import {
    convertRequest, DynamicFieldGroupPanel, DynamicForm, DynCompleteButton,
    DynLayoutPanelTypes
} from './dynamic/DynamicUISearchPanel.jsx';
import {EmbeddedPositionSearchPanel} from './dynamic/EmbeddedPositionSearchPanel.jsx';

import {FormPanel} from './FormPanel.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {ServerParams} from '../data/ServerParams.js';
import {showInfoPopup} from './PopupUtil.jsx';

import {dispatchHideDropDown} from '../core/LayoutCntlr.js';

import FieldGroupUtils  from '../fieldGroup/FieldGroupUtils.js';
import {dispatchTableSearch} from '../tables/TablesCntlr.js';
import {FieldGroupTabs, Tab} from './panel/TabPanel.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {makeWorldPt, parseWorldPt} from '../visualize/Point.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {getDS9Region} from '../rpc/PlotServicesJson.js';
import {RegionFactory} from '../visualize/region/RegionFactory.js';

import {showUploadDialog} from 'firefly/ui/FileUploadDropdown';
import {DATA_LINK_TABLES, IMAGES, MOC_TABLES, REGIONS, SPECTRUM_TABLES, TABLES, UWS} from 'firefly/ui/FileUploadUtil';

const dynamic1Params= [
    makeTargetDef(
        {hipsUrl:'ivo://CDS/P/DSS2/color', centerPt:makeWorldPt(10,10), hipsFOVInDeg:10, raKey:'ra', decKey:'dec'}),
    makeAreaDef({key:'sizeOfSearch', minValue:1, maxValue:10, initValue:2, desc:'Area to Search'}),
    makeIntDef({key:'int1', minValue:10, maxValue:1000, desc:'field #1', units: 'um', tooltip:'tooltip for field1',initValue:25 }),
    makeFloatDef({key:'float2', minValue:.1, maxValue:8.88, precision:3, initValue:3, desc:'float #3', tooltip:'tooltip for field1'}),
    makeEnumDef({key:'enum3', tooltip:'tip for enum 3', initValue:'joe', desc:'Choose',
        enumValues: [
            {label:'Samuel', value:'sam'},
            {label:'Joe', value:'joe'},
            {label:'Mary', value:'mary'},
            {label:'Jane', value:'jane'},
            {label:'All', value:'all'}
        ]}),
    makeEnumDef({key:'enum5', tooltip:'for thing', initValue:'paper', desc:'Choose thing',
        enumValues: [
            {label:'Rock', value:'rock'},
            {label:'Paper', value:'paper'},
            {label:'Scissors', value:'scissors'},
            {label:'Dynamite', value:'dynamite'},
            {label:'Other', value:'other'},
        ]}),
    makeCheckboxDef({key:'cb1', desc:'check box one', initValue:true}),
    makeCheckboxDef({key:'cb2', desc:'check box two', initValue:false}),
    makeCheckboxDef({key:'cb3', desc:'check box three', initValue:true}),
    makeCheckboxDef({key:'cb4', desc:'check box found', initValue:true}),
    makePolygonDef({key:'somePoints', desc:'Area to Search', tooltip:'the area to search'})
];

const dynamic2Params= [
    makeEnumDef({key:'enum3', tooltip:'tip for enum 3', initValue:'joe', desc:'Choose', units:'name',
        enumValues: [
            {label:'Samuel', value:'sam'},
            {label:'Joe', value:'joe'},
            {label:'Mary', value:'mary'},
            {label:'Jane', value:'jane'},
            {label:'All', value:'all'}
        ]}),
    makeEnumDef({key:'enum5', tooltip:'for thing', initValue:'paper', desc:'Choose thing',
        enumValues: [
            {label:'Rock', value:'rock'},
            {label:'Paper', value:'paper'},
            {label:'Scissors', value:'scissors'},
            {label:'Dynamite', value:'dynamite'},
            {label:'Other', value:'other'},
        ]}),
    makeUnknownDef({key:'u6', tooltip:'a string of some sort', initValue:'stuff', desc:'Enter stuff'}),
    makeCheckboxDef({key:'cb1', desc:'check box one', initValue:true}),
    makeCheckboxDef({key:'cb2', desc:'check box two', initValue:false}),
    makeCheckboxDef({key:'cb3', desc:'check box three', initValue:true}),
];

const dynamic3Params= [
    makeCircleDef({key:'someArea', desc:'Area to Search', tooltip:'the area to search',
        targetKey:'TargetPoint', sizeKey:'coneRadius', minValue:.01, maxValue:4, initValue:.2,
        centerPt:makeWorldPt(10,10), hipsFOVInDeg:30, hipsUrl:'ivo://CDS/P/DSS2/color' }),
    makePolygonDef({key:'somePoints', desc:'Area to Search', tooltip:'the area to search'}),
    makeIntDef({key:'int1', minValue:10, maxValue:1000, desc:'field #1', units: 'um', tooltip:'tooltip for field1',initValue:25 }),
    makeFloatDef({key:'float2', minValue:.1, maxValue:8.88, precision:3, initValue:3, desc:'float #3', tooltip:'tooltip for field1'}),
    makeCheckboxDef({key:'cb1', desc:'check box one', initValue:true}),
    makeCheckboxDef({key:'cb2', desc:'check box two', initValue:false}),
    makeCheckboxDef({key:'cb3', desc:'check box three', initValue:true}),
];

export class TestQueriesPanel extends PureComponent {

    constructor(props) {
        super(props);
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = FieldGroupUtils.bindToStore('TEST_CAT_PANEL', (fields) => {
            if (this.iAmMounted) this.setState(fields);
        });
    }

    render() {
        //const fields = this.state;
        const {fields}=this.state || {};

        return (
            <div style={{padding: 10, height: '100%',width:'100%'}}>
                <FormPanel
                    groupKey='TEST_CAT_PANEL'
                    onSuccess={onSearchSubmit}>
                    <FieldGroup groupKey='TEST_CAT_PANEL' keepState={true}
                                style={{height:'100%', display:'flex', flexDirection:'column'}}>
                        <FieldGroupTabs initialState={{ value:'2massImage' }} fieldKey='Tabs' style={{flexGrow:1}} >
                            <Tab name='2Mass Search' id='2massImage' sx={{p:1}}>
                                <div>{render2MassSearch(fields)}</div>
                            </Tab>
                            <Tab name='Inset Example' id='inset1'>
                                {makeInsetSearchExample()}
                            </Tab>
                            <Tab name='Dynamic 2' id='dynamic2'>
                                {makeDynamic2()}
                            </Tab>
                            <Tab name='Dyn Search Panel' id='dsp'>
                                {makeDynamicForm()}
                            </Tab>
                            <Tab name='Dynamic 3' id='dynamic3'>
                                {makeDynamic3()}
                            </Tab>
                            <Tab name='Uploads' id='upload'>
                                {uploadButtons()}
                            </Tab>
                        </FieldGroupTabs>

                    </FieldGroup>
                </FormPanel>
            </div>
        );

    }
}


TestQueriesPanel.propTypes = {
    name: PropTypes.oneOf(['TestSearches'])
};

TestQueriesPanel.defaultProps = {
    name: 'TestSearches'
};

function makeInsetSearchExample() {
    return (
        <div style={{width:'100%', height:'100%', display:'flex', flexDirection:'column'}}>
            <FieldGroup groupKey='InsetUI' keepState={true}
                        style={ { width:'100%', height:'100%', display: 'flex', flexDirection: 'column', alignItems: 'center' } }>
                <EmbeddedPositionSearchPanel {...{
                    slotProps: {
                        formPanel: {
                            onSuccess: (request) => {
                                showInfoPopup(`done: ${JSON.stringify(request)}`, 'information');
                                return false;
                            },
                            onError: () => showInfoPopup('Fix errors and search again', 'Error'),
                            help_id: 'muy-help-id',
                            slotProps: {
                                input: {p: 0, mb: 1, border: 'none'}
                            },
                        },
                        hipsTargetView: {
                            hipsFOVInDeg: 30, hipsUrl: 'ivo://CDS/P/DSS2/color', toolbarHelpId: 'exampleHelp',
                        },
                        sizeInput: {min: 5 / 3600, max: 1.5, initValue: .5}
                    },
                }}>
                    <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center',}}>
                        <>
                            <div>here i am</div>
                        </>
                    </div>
                </EmbeddedPositionSearchPanel>
            </FieldGroup>
        </div>
    );
}

function makeDynamic2() {
    return (
        <div style={{width: '100%', height: '100%', display: 'flex', flexDirection: 'column'}}>
            <DynamicFieldGroupPanel
                groupKey={'simpledyngroup'}
                DynLayoutPanel={DynLayoutPanelTypes.Simple}
                fieldDefAry={dynamic2Params}
                style={{margin:3, width:'100%'}}/>
            <DynCompleteButton groupKey={'simpledyngroup'}
                               style={{margin: 3}}
                               fieldDefAry={dynamic3Params}
                               onSuccess={(request) => showDymResult(request)}/>
        </div>
    );
}

function makeDynamic3() {
    return (
        <div style={{width:'100%', height:'100%', display:'flex', flexDirection:'column'}}>
            <DynamicFieldGroupPanel
                groupKey={'simpledyngroup3'}
                DynLayoutPanel={DynLayoutPanelTypes.Inset}
                fieldDefAry={dynamic3Params}
                style={{margin:3, width:'100%', display:'flex', flexGrow:1}}/>
            <DynCompleteButton groupKey={'simpledyngroup3'}
                               style={{margin: 3}}
                               fieldDefAry={dynamic3Params}
                               onSuccess={(request) => showDymResult(request)}/>
        </div>
    );
}

function uploadButtons() {
    return (
        <div style={{padding:10}}>

            <button type='button' className='button std hl' onClick={() => showUploadDialog([TABLES],true,'FileUploadAnalysis_Only_Table',true)}>
                <b>Tables Only Upload</b>
            </button>

            <button type='button' className='button std hl' onClick={() => showUploadDialog([MOC_TABLES,DATA_LINK_TABLES,TABLES,SPECTRUM_TABLES],true,'FileUploadAnalysis_All_Tables',true)}>
                <b>All Tables Upload</b>
            </button>
            <br/><br/>
            <button type='button' className='button std hl' onClick={() => showUploadDialog([REGIONS],true,'FileUploadAnalysis_Region',true)}>
                <b>Region Upload</b>
            </button>

            <button type='button' className='button std hl' onClick={() => showUploadDialog([IMAGES],true,'FileUploadAnalysis_Image',true)}>
                <b>Image Upload</b>
            </button>
            <br/><br/>
            <button type='button' className='button std hl' onClick={() => showUploadDialog([DATA_LINK_TABLES],true,'FileUploadAnalysis_DataLink_Only',true)}>
                <b>DataLink Tables Only</b>
            </button>
            <button type='button' className='button std hl' onClick={() => showUploadDialog([MOC_TABLES],true,'FileUploadAnalysis_MocFits_Only',true)}>
                <b>MOC FITS Only</b>
            </button>
            <br/><br/>
            <button type='button' className='button std hl' onClick={() => showUploadDialog([MOC_TABLES,DATA_LINK_TABLES,TABLES,SPECTRUM_TABLES,REGIONS,IMAGES,UWS],true,'FileUploadAnalysis_Everything',true)}>
                <b>Accept Everything (Default)</b>
            </button>
            <br/>


            <br/>
        </div>
    );
}


function makeDynamicForm() {

    return (
        <DynamicForm groupKey='dform'
                     fieldDefAry={dynamic1Params}
                     style={{width: 'calc(100% - 5px)', display: 'flex', flexDirection: 'column'}}
                     DynLayoutPanel={DynLayoutPanelTypes.Grid}
                     onSubmit={(r) => {
                         showDymResult(r);
                         console.log(r);
                     }}
        />

    );
}



function render2MassSearch(fields) {


    return (
        <div>
            <div style={{padding:'5px 0 5px 0'}}>
                <TargetPanel/>
            </div>
            <div style={{padding:5, display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center' }}>
                <RadioGroupInputField
                    fieldKey='ds'
                    orientation='vertical'
                    initialState={{
                        tooltip: 'Spatial Type',
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
                    orientation='horizontal'
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
        </div>
    );

}



function showDymResult(convertedRequest) {
    console.log(convertedRequest);
    const result= (
        <div style={{padding:'5px'}}>
            {Object.entries(convertedRequest).map( ([k,v]) => ( <div key={k}> {k} = {v+''} </div> )) }
        </div>
    );
    showInfoPopup(result, 'Results' );


}


function onSearchSubmit(request) {
    console.log(request);
    const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
    const usesTarget= ['wiseImage', '2massImage','atlasImage', 'loadRegion'];
    if (!wp && usesTarget.includes(request.Tabs)) {
        showInfoPopup('Target is required');
        return;
    }
    else if (request.Tabs === 'wiseImage') {
        doWise(request);
    }
    else if (request.Tabs === '2massImage') {
        do2Mass(request);
    }else if (request.Tabs === 'atlasImage') {
        doAtlas(request);
    }
    else if (request.Tabs === 'loadRegion') {
        doRegionLoad(request);
    }
    else if (request.Tabs === 'dynamic1') {
        showDymResult(convertRequest(request, dynamic1Params));
    }
    else if (request.Tabs === 'dynamic2') {
        showDymResult( convertRequest(request,dynamic2Params ));
    }
}







const schemaParams = {
    'allwise-multiband': {ImageSet: 'allwise-multiband', ProductLevel: '3a', title: 'AllWISE'},
    'allsky_4band-1b': {ImageSet: 'allsky-4band', ProductLevel: '1b', title: 'AllSky - Single'},
    'allsky_4band-3a': {ImageSet: 'allsky-4band', ProductLevel: '3a', title: 'AllSky - Atlas'},
    'cryo_3band-1b': {ImageSet: 'cryo_3band', ProductLevel: '1b', title: '3-Band Single'},
    'cryo_3band-1b-3a': {ImageSet: 'cryo_3band', ProductLevel: '3a', title: '3-Band Atlas'},
    'postcryo-1b': {ImageSet: 'postcryo', ProductLevel: '1b', title: 'Post-Cryo'},
    'neowiser-1b': {ImageSet: 'neowiser', ProductLevel: '1b', title: 'NeoWISER'}
};


function doWise(request) {
    console.log('wise', request);
    var tgtName = '';
    const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
    if (wp.getObjName()) tgtName = ', ' + wp.getObjName();
    const params = Object.assign(schemaParams[request.wiseDataSet],
        {
            [ServerParams.USER_TARGET_WORLD_PT]: request[ServerParams.USER_TARGET_WORLD_PT],
            mission: 'wise',
            intersect: request.intersect,
            mcenter: (request.intersect === 'CENTER' || request.intersect === 'COVERS') ? request.mcenter : 'all',
            size: request.size,
            subsize: request.subsize,
            band: request.wisebands
        });
    const reqParams = makeTblRequest('ibe_processor', `${schemaParams[request.wiseDataSet].title}${tgtName}`, params);
    dispatchTableSearch(reqParams);
}

function do2Mass(request) {
    console.log('wmass', request);
    const reqParams = makeTblRequest('ibe_processor', '2MASS-' + request[ServerParams.USER_TARGET_WORLD_PT],
        {
            [ServerParams.USER_TARGET_WORLD_PT]: request[ServerParams.USER_TARGET_WORLD_PT],
            mission: 'twomass',
            ds: request.ds,
            band: request.band
        });
    dispatchTableSearch(reqParams);
}

function doAtlas(request) {
    console.log('atlas', request);
    const reqParams = makeTblRequest('ibe_processor', 'ATLAS-' + request[ServerParams.USER_TARGET_WORLD_PT],
        {
            [ServerParams.USER_TARGET_WORLD_PT]: request[ServerParams.USER_TARGET_WORLD_PT],
            mission: 'atlas',
            //mcenter:true, returned 1 image
            // TODO Not yet fully working, see AtlasRequestList
            ds:request.ds1,
            band: request.band,
            subsize:0.05,
            sizeUnit:'deg',
            mcenter:true
            // filter:''
        });
    dispatchTableSearch(reqParams);
}


function doRegionLoad(request) {
    getDS9Region(request.fileUpload)
        .then((result) => {
            //console.log(result);

            if (result.RegionData) {
                var rgAry = RegionFactory.parseRegionJson(result.RegionData);  // todo: region drawing
            }
        });
}

