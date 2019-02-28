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
import {get} from 'lodash';

import {FormPanel} from './FormPanel.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {ValidationField} from '../ui/ValidationField.jsx';
import {IbeSpacialType} from './IbeSpacialType.jsx';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {InputGroup} from '../ui/InputGroup.jsx';
import {ServerParams} from '../data/ServerParams.js';
import {showInfoPopup} from './PopupUtil.jsx';

import Validate from '../util/Validate.js';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';

import FieldGroupUtils  from '../fieldGroup/FieldGroupUtils.js';
import {dispatchTableSearch} from '../tables/TablesCntlr.js';
import {FieldGroupTabs, Tab} from './panel/TabPanel.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {FileUpload} from '../ui/FileUpload.jsx';
import {parseWorldPt} from '../visualize/Point.js';
import {makeTblRequest, makeIrsaCatalogRequest} from '../tables/TableRequestUtil.js';
import {dispatchAddViewerItems,getAViewFromMultiView,getMultiViewRoot, DEFAULT_FITS_VIEWER_ID, IMAGE} from '../visualize/MultiViewCntlr.js';
import WebPlotRequest from '../visualize/WebPlotRequest.js';
import {dispatchPlotImage, dispatchPlotHiPS} from '../visualize/ImagePlotCntlr.js';
import {getDS9Region} from '../rpc/PlotServicesJson.js';
import {RegionFactory} from '../visualize/region/RegionFactory.js';
import {HiPSId, URL_COL} from '../visualize/HiPSListUtil.js';
import {HiPSPopupMsg, HiPSSurveyListSelection, getTblModelOnPanel,
        getHiPSSourcesChecked, sourcesPerChecked} from './HiPSSurveyListDisplay.jsx';
import {getCellValue} from '../tables/TableUtil.js';
import {NaifidPanel} from "./NaifidPanel";


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
                        <FieldGroupTabs initialState={{ value:'wiseImage' }} fieldKey='Tabs'>
                            <Tab name='Wise Search' id='wiseImage'>
                                <div>{renderWiseSearch(fields)}</div>
                            </Tab>
                            <Tab name='2Mass Search' id='2massImage'>
                                <div>{render2MassSearch(fields)}</div>
                            </Tab>
                            <Tab name='Atlas Search' id='atlasImage'>
                                <div>{renderAtlasSearch(fields)}</div>
                            </Tab>
                            <Tab name='Periodogram' id='periodogram'>
                                 <div>{renderPeriodogram(fields)}</div>
                            </Tab>
                            <Tab name='NAIF-ID' id='naifid'>
                                <div>{renderNaifid(fields)}</div>
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


function hideSearchPanel() {
    dispatchHideDropDown();
}


function renderNaifid(fields){
    return(
        <div style={{height:100, margin:5}}>
           <NaifidPanel fieldKey='mTargetName' labelWidth={110}
                        initialState={{value: 'ceres', size: 4, label: 'Moving Target Name:'}}
           />

        </div>
    );
}


function renderPeriodogram(fields) {

    /**
     *
     * @param opt
     */
    function lightCurveSubmit(opt) {
        console.log('periodogram...');
        let tReq;
        const ds = get(fields, 'period.value', 1);
        //var tReq = makeTblRequest('PhaseFoldedProcessor', 'Phase folded', { period: '1', 'table_name':'folded_table','original_table':});
        if (opt === 0) {
            tReq = makeTblRequest('LightCurveProcessor', 'Periodogram', {
                'original_table':'http://web.ipac.caltech.edu/staff/ejoliet/demo/OneTarget-27-AllWISE-MEP-m82-2targets-10arsecs.tbl',
                'x':'mjd',
                'y':'w1mpro_ep',
                'table_name': 'periodogram'
                // The following are optional
                //'pmin':0,
                //'pmax':200,
                //'step_method':'fixedf',
                //'step_size': 10,
                //'alg': 'ls', //There are three algorithms: ls (Lomb-Scargle), bls (Box-fitting Least Squares), and plav (Plavchan 2008). The default algorithm is Lomb-Scargle.
                //'peaks' : 50
                //'result_table': 'http://web.ipac.caltech.edu/staff/ejoliet/demo/vo-nexsci-result-sample.xml'
            },
                {inclCols : '"Power","Period"'});
        } else if (opt === 1) {
            tReq = makeTblRequest('PhaseFoldedProcessor', 'Phase folded', {
                'period_days': ds,
                'table_name': 'folded_table',
                'x':'mjd',
                'y':'w1mpro_ep',
                'original_table': 'http://web.ipac.caltech.edu/staff/ejoliet/demo/OneTarget-27-AllWISE-MEP-m82-2targets-10arsecs.tbl'
            });
        } else if (opt === 2){
            tReq = makeTblRequest('LightCurveProcessor', 'Peaks', {
                'original_table':'http://web.ipac.caltech.edu/staff/ejoliet/demo/OneTarget-27-AllWISE-MEP-m82-2targets-10arsecs.tbl',
                'x':'mjd',
                'y':'w1mpro_ep',
                'table_name': 'peak_table',
                //'pmin':0,
                //'pmax':200,
                //'step_method':'fixedf',
                //'step_size': 10,
                //'alg': 'ls', //There are three algorithms: ls (Lomb-Scargle), bls (Box-fitting Least Squares), and plav (Plavchan 2008). The default algorithm is Lomb-Scargle.
                'peaks' : 57
                //'result_table': 'http://web.ipac.caltech.edu/staff/ejoliet/demo/vo-nexsci-result-sample.xml'
            }, {inclCols : '"Peak", "Period", "Power", "SDE"'});
        }

        console.log(ds);
        console.log('tReq ' +tReq);
        dispatchTableSearch(tReq);
    }

    return (
        <div style={{padding:5}}>

            <button type='button' className='button std hl' onClick={() => lightCurveSubmit(0)}>
                <b>Compute Periodogram</b>
            </button>
            <br/>
            <button type='button' className='button std hl' onClick={() => lightCurveSubmit(2)}>
                <b> Get peaks table</b>
            </button>
            <br/>
            <ValidationField fieldKey='period'
                             initialState={{
                                          fieldKey: 'period',
                                          value: '1.0',
                                          tooltip: 'period',
                                          label : 'period:',
                                          labelWidth : 100
                                      }}/>
            <button type='button' className='button std hl' onClick={() => lightCurveSubmit(1)}>
                <b>Phase folded (period value not used yet)</b>
            </button>
        </div>
    );
}



const wiseBandMap = {
    'allwise-multiband': ['1', '2', '3', '4'],
    'allsky_4band-1b': ['1', '2', '3', '4'],
    'allsky_4band-3a': ['1', '2', '3', '4'],
    'cryo_3band-1b': ['1', '2', '3'],
    'cryo_3band-1b-3a': ['1', '2', '3'],
    'postcryo-1b': ['1', '2']
};


function renderWiseSearch(fields) {
    const ds = get(fields, 'wiseDataSet.value', 'allwise-multiband');
    const options = wiseBandMap[ds].map((w) => ({label: 'W' + w, value: w}));
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
                        initialState={{
                                    value: '1,2,3,4',   // workaround for _all_ for now
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

function renderAtlasSearch(fields) {

// See value of band and instruments here as SEIP example:
// https://irsadev.ipac.caltech.edu/IBE?table=spitzer.seip_science&POS=56.86909,24.10531
    return (
        <div style={{padding:5, display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center' }}>
            <CheckboxGroupInputField
                fieldKey='ds1'
                alignment='vertical'
                initialState={{
                    tooltip: 'Spacial Type',
                    value: 'spitzer.seip_science'
                }}
                options={[
                    {label: 'MSX', value: 'msx.msx_images'},
                    {label: 'SEIP', value: 'spitzer.seip_science'}
                ]}
            />
            <CheckboxGroupInputField
                fieldKey='band'
                alignment='horizontal'
                initialState={{
                    tooltip: 'Return Band',
                    value: 'IRAC1'
                }}
                options={[
                    {label : 'IRAC 2.4', value: 'IRAC1'},
                    {label : 'IRAC 3.6', value: 'IRAC2'},
                    {label : 'IRAC 5.8', value: 'IRAC3'},
                    {label : 'IRAC 8', value: 'IRAC4'},
                    {label : 'MIPS 24', value: 'MIPS24'},
                    {label : 'E', value: 'E'},
                    {label : 'A', value: 'A'},
                    {label : 'C', value: 'C'},
                    {label : 'D', value: 'D'}
                ]}
            />
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





function onSearchSubmit(request) {
    console.log(request);
    const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
    if (!wp && request.Tabs !== 'hips') {
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
    else {
        console.log('request no supported');
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

