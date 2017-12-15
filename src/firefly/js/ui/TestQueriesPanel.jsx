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

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../Firefly.js';
import {get} from 'lodash';

import {FormPanel} from './FormPanel.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {ValidationField} from '../ui/ValidationField.jsx';
import {IbeSpacialType} from './IbeSpacialType.jsx';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {InputGroup} from '../ui/InputGroup.jsx';
import {ServerParams} from '../data/ServerParams.js';
import {showInfoPopup} from './PopupUtil.jsx';
import HiPSGrid  from '../drawingLayers/HiPSGrid.js';

import Validate from '../util/Validate.js';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';

import {getFieldVal, getFieldGroupState} from '../fieldGroup/FieldGroupUtils.js';
import {dispatchTableSearch} from '../tables/TablesCntlr.js';
import {FieldGroupTabs, Tab} from './panel/TabPanel.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {FileUpload} from '../ui/FileUpload.jsx';
import {parseWorldPt} from '../visualize/Point.js';
import {makeTblRequest, makeIrsaCatalogRequest} from '../tables/TableRequestUtil.js';
import {getDrawLayerByType} from '../visualize/PlotViewUtil.js';
import {dispatchAddViewerItems,getAViewFromMultiView,getMultiViewRoot, DEFAULT_FITS_VIEWER_ID, IMAGE} from '../visualize/MultiViewCntlr.js';
import WebPlotRequest from '../visualize/WebPlotRequest.js';
import {dispatchPlotImage, dispatchPlotHiPS} from '../visualize/ImagePlotCntlr.js';
import {getDS9Region} from '../rpc/PlotServicesJson.js';
import {RegionFactory} from '../visualize/region/RegionFactory.js';
import {onHiPSSurveys, isLoadingHiPSSurverys, HiPSDataType} from '../visualize/HiPSCntlr.js';
import {showHiPSSurveyList, HiPSSurvey} from './HiPSViewer.jsx';
import {getTblById, getCellValue} from '../tables/TableUtil.js';

const options = [
    {label: 'AllWISE Source Catalog', value: 'allwise_p3as_psd', proj: 'WISE'},
    {label: '2MASS All-Sky Point Source Catalog (PSC)', value: 'fp_psc', proj: '2MASS'},
    {label: 'IRAS Point Source Catalog v2.1 (PSC)', value: 'iraspsc', proj: 'IRAS'}
];



const hipsSURVEYS = [
    {
        url: 'http://alasky.u-strasbg.fr/2MASS/Color',
        label: '2MASS colored'
    },
    {
        url: 'http://alasky.u-strasbg.fr/DSS/DSSColor',
        label: 'DSS colored'
    },
    {
        url: 'http://alasky.u-strasbg.fr/DSS/DSS2Merged',
        label: 'DSS2 Red (F+R)'
    },
    {
        url: 'http://alasky.u-strasbg.fr/Fermi/Color',
        label: 'Fermi color'
    },
    {
        url: 'http://alasky.u-strasbg.fr/FinkbeinerHalpha',
        label: 'Halpha'
    },
    {
        url: 'http://alasky.u-strasbg.fr/GALEX/GR6-02-Color',
        label: 'GALEX Allsky Imaging Survey colored'
    },
    {
        url: 'http://alasky.u-strasbg.fr/IRISColor',
        label: 'IRIS colored'
    },
    {
        url: 'http://alasky.u-strasbg.fr/MellingerRGB',
        label: 'Mellinger colored'
    },
    {
        url: 'http://alasky.u-strasbg.fr/SDSS/DR9/color',
        label: 'SDSS9 colored'
    },
    {
        url: 'http://alasky.u-strasbg.fr/SpitzerI1I2I4color',
        label: 'IRAC color I1,I2,I4 - (GLIMPSE, SAGE, SAGE-SMC, SINGS)'
    },
    {
        url: 'http://alasky.u-strasbg.fr/VTSS/Ha',
        label: 'VTSS-Ha'
    },
    {
        url: 'http://saada.u-strasbg.fr/xmmallsky',
        label: 'XMM-Newton stacked EPIC images (no phot. normalization)'
    },
    {
        url: 'http://saada.u-strasbg.fr/xmmallsky/',
        label: 'XMM PN colored'
    },
    {
        url: 'http://alasky.u-strasbg.fr/AllWISE/RGB-W4-W2-W1/',
        label: 'AllWISE color'
    },
    {
        url: 'http://www.spitzer.caltech.edu/glimpse360/aladin/data',
        label: 'GLIMPSE360'
    }
];


export const HiPSData = [HiPSDataType.image, HiPSDataType.cube];
const HiPSId = 'hips';

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
        this.removeListener = flux.addListener(()=>this.storeUpdate());
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const {fields} = getFieldGroupState('TEST_CAT_PANEL') || {};
            if (fields !== get(this.state, 'fields')) {
                this.setState({fields});
            }
            if (getFieldVal('TEST_CAT_PANEL', 'Tabs') === HiPSId) {
                const isUpdatingHips = isLoadingHiPSSurverys(HiPSId);

                if (isUpdatingHips !== get(this.state, 'isUpdatingHips', false)) {
                    this.setState({isUpdatingHips});
                }
            }
        }
    }

    render() {
        //const fields = this.state;
        const {fields, isUpdatingHips}=this.state || {};

        const onTabSelect = (idx, id, name) => {
            if (id === HiPSId) {
                onHiPSSurveys(HiPSData, id);
            }
        };

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
                        <FieldGroupTabs initialState={{ value:'catalog' }} fieldKey='Tabs' onTabSelect={onTabSelect}>
                            <Tab name='Test Catalog' id='catalog'>{renderCatalogTab()}</Tab>
                            <Tab name='Wise Search' id='wiseImage'>
                                <div>{renderWiseSearch(fields)}</div>
                            </Tab>
                            <Tab name='2Mass Search' id='2massImage'>
                                <div>{render2MassSearch(fields)}</div>
                            </Tab>
                            <Tab name='Atlas Search' id='atlasImage'>
                                <div>{renderAtlasSearch(fields)}</div>
                            </Tab>
                            <Tab name='HiPS' id={HiPSId}>
                                <div style={{width: '100%', height: 400}}>{showHiPSSurveyList(HiPSId, isUpdatingHips)}</div>
                            </Tab>
                            <Tab name='Periodogram' id='periodogram'>
                                 <div>{renderPeriodogram(fields)}</div>
                            </Tab>
                        </FieldGroupTabs>

                    </FieldGroup>
                </FormPanel>
            </div>
        );

    }
}


/*
 <Tab name='Images' id='images'>{renderImagesTab()}</Tab>
<Tab name='LSST DAX Dummy' id='lsstDummy'>
    <div>{renderLSSTDummy(fields)}</div>
</Tab>
 <Tab name='Load Region' id='loadRegion'>
 <div>{renderLoadRegion(fields)}</div>
 </Tab>
 <Tab name='Compute Periodogram' id='periodogram'>
 <div>{renderPeriodogram(fields)}</div>
 </Tab>
*/






TestQueriesPanel.propTypes = {
    name: PropTypes.oneOf(['TestSearches']),
};

TestQueriesPanel.defaultProps = {
    name: 'TestSearches',
};


function hideSearchPanel() {
    dispatchHideDropDown();
}


function renderHiPS(fields) {

    const options= hipsSURVEYS.map( (s,idx) => ({label:s.label, value:idx+''}));

    return (
        <div>
            <div>
                HiPS Plot
            </div>

            <RadioGroupInputField
                fieldKey='hipsSurvey'
                inline={true}
                alignment='vertical'
                initialState={{
                    tooltip: 'Choose survey',
                    value: options[0].value
                }}
                options={options}
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
                'table_name': 'periodogram',
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

function renderLSSTDummy() {

    return (
        <div style={{padding:5}}>

            <button type='button' className='button std hl' onClick={() => lsstSearchSubmit(true)}>
                <b>Test LSST DD search processor</b>
            </button>
            <br/>
            <button type='button' className='button std hl' onClick={() => lsstSearchSubmit(false)}>
                <b> Test LSST Catalog processor</b>
            </button>
            <br/>
        </div>
    );
}
function lsstSearchSubmit(isDD) {

    var tReq;
    if (isDD) { //Meta Search
        tReq = makeTblRequest('LSSTMetaSearch', 'RunDeepForcedSource', {
          'table_name':  'RunDeepForcedSource',

       });
    } else {//Catalog Search
        tReq = makeTblRequest('LSSTCataLogSearch', 'RunDeepForcedSource', {
            'table_name': 'RunDeepForcedSource',
            'table_path': '/hydra/cm/firefly_test_data/DAXTestData/',
            'meta_table':  'RunDeepForcedSource',
            'SearchMethod':'cone'
        });
    }

    console.log('tReq ' +tReq);
    dispatchTableSearch(tReq);
}

function renderCatalogTab() {
    return (
        <div style={{padding:5}}>

            <InputGroup labelWidth={110}>
                <ListBoxInputField initialState={{
                                          tooltip: 'Select Catalog',
                                          label : 'Select Catalog:'
                                      }}
                                   options={options }
                                   multiple={false}
                                   fieldKey='catalog'
                />


                <ValidationField fieldKey='radius'
                                 initialState={{
                                          fieldKey: 'radius',
                                          value: '300',
                                          validator: Validate.floatRange.bind(null, 0, 2000, 3,'field 3'),
                                          tooltip: 'radius',
                                          label : 'radius:',
                                          labelWidth : 100
                                      }}/>
            </InputGroup>
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


function renderLoadRegion(fields) {
    return (
        <div style={{padding:5, display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center' }}>
            <FileUpload
                wrapperStyle={{margin: '5px 0'}}
                fieldKey='fileUpload'
                initialState={{
                        tooltip: 'Select a region file to upload',
                        label: 'Upload File:'}}
            />
        </div>
    );

}

function renderCatalogFromSource(fields) {
    return (
        <div style={{padding:5, display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center' }}>
            <FileUpload
                wrapperStyle={{margin: '5px 0'}}
                fieldKey='urlSource'
                initialState={{
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
    const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
    if (!wp && request.Tabs !== 'hips') {
        showInfoPopup('Target is required');
        return;
    }
    if (request.Tabs === 'catalog') {
        doCatalog(request);
    }
    else if (request.Tabs === 'images') {
        doImages(request);
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
    else if (request.Tabs === 'lsstDummy') {
        doLSSTDummyLoad(request);
    }
    else if (request.Tabs === 'hips') {
        doHiPSLoad(request);
    }
    else {
        console.log('request no supported');
    }
}


function doHiPSLoad(request) {

    dispatchHideDropDown();

    const tableModel = getTblById(HiPSSurvey + HiPSId);
    if (!tableModel) {
        return;
    }

    const {highlightedRow=0} = tableModel;
    const rootUrl= getCellValue(tableModel, highlightedRow, 'url');
    const wpRequest= WebPlotRequest.makeHiPSRequest(rootUrl, null);
    wpRequest.setPlotGroupId(DEFAULT_FITS_VIEWER_ID);
    wpRequest.setPlotId('aHiPSid');
    wpRequest.setHipsSurveysId(HiPSId);

    const wp = parseWorldPt(request.UserTargetWorldPt);
    if (wp) wpRequest.setOverlayPosition(wp);

    // dispatchAddViewerItems(DEFAULT_FITS_VIEWER_ID, ['aHiPSid'], IMAGE);

    dispatchPlotHiPS({
        plotId: 'aHiPSid',
        wpRequest,
        viewerId: DEFAULT_FITS_VIEWER_ID
    });


    // const dl = getDrawLayerByType(dlRoot(), HiPSGrid.TYPE_ID);
    // if (!dl) dispatchCreateDrawLayer(HiPSGrid.TYPE_ID);
    // dispatchAttachLayerToPlot(HiPSGrid.TYPE_ID, 'aHiPSid', false, false);
}



function doCatalog(request) {
    var tReq = makeIrsaCatalogRequest(
        request.catalog,
        options.find((op) => request.catalog === op.value).proj,
        request.catalog,
        {
            [ServerParams.USER_TARGET_WORLD_PT]: request[ServerParams.USER_TARGET_WORLD_PT],
            SearchMethod: 'Cone',
            radius: request.radius,
        });
    dispatchTableSearch(tReq);
}


function doImages(request) {
    var wp = parseWorldPt(request.UserTargetWorldPt);

    // -example call to 2mass
    //var wpr1= WebPlotRequest.makePlotServiceReq(ServiceType.TWOMASS, wp,'h',.1 );
    //var wpr2= WebPlotRequest.makePlotServiceReq(ServiceType.TWOMASS, wp,'k',.1 );


    // -example call to wise
    var wpr1 = WebPlotRequest.makeWiseRequest(wp, '1b', '1', .4);
    var wpr2 = WebPlotRequest.makeWiseRequest(wp, '1b', '2', .4);
    var wpr3 = WebPlotRequest.makeWiseRequest(wp, '1b', '3', .4);
    var wpr4 = WebPlotRequest.makeWiseRequest(wp, '1b', '4', .4);


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
    wpr1.setGroupLocked(true);
    wpr2.setGroupLocked(true);
    wpr3.setGroupLocked(true);
    wpr4.setGroupLocked(true);


    wpr1.setInitialZoomLevel(parseFloat(request.zoom));
    wpr2.setInitialZoomLevel(parseFloat(request.zoom));
    wpr3.setInitialZoomLevel(parseFloat(request.zoom));
    wpr4.setInitialZoomLevel(parseFloat(request.zoom));

    wpr2.setInitialColorTable(4);


    //=========== 3 color
    var cWpr1 = WebPlotRequest.makeWiseRequest(wp, '3a', '1', .4);
    var cWpr2 = WebPlotRequest.makeWiseRequest(wp, '3a', '2', .4);
    var cWpr3 = WebPlotRequest.makeWiseRequest(wp, '3a', '3', .4);
    cWpr1.setPlotGroupId('test-group');
    cWpr2.setPlotGroupId('test-group');
    cWpr3.setPlotGroupId('test-group');

    cWpr1.setInitialZoomLevel(parseFloat(request.zoom));
    cWpr2.setInitialZoomLevel(parseFloat(request.zoom));
    cWpr3.setInitialZoomLevel(parseFloat(request.zoom));


    //wpr1.setAnnotationOps(AnnotationOps.TITLE_BAR);
    dispatchPlotImage({plotId: 'TestImage1', wpRequest: wpr1});
    dispatchPlotImage({plotId: 'TestImage2', wpRequest: wpr2});
    dispatchPlotImage({plotId: 'TestImage3', wpRequest: wpr3});
    dispatchPlotImage({plotId: 'TestImage4', wpRequest: wpr4});

    dispatchPlotImage({plotId: 'TestImage3Color', wpRequest: [cWpr1, cWpr2, cWpr3]});

    var viewer = getAViewFromMultiView(getMultiViewRoot(), IMAGE);
    dispatchAddViewerItems(viewer.viewerId, ['TestImage1', 'TestImage2', 'TestImage3', 'TestImage4', 'TestImage3Color'], IMAGE);
    dispatchHideDropDown();

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

function doLSSTDummyLoad(request) {
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

function doRegionLoad(request) {
    getDS9Region(request.fileUpload)
        .then((result) => {
            //console.log(result);

            if (result.RegionData) {
                var rgAry = RegionFactory.parseRegionJson(result.RegionData);  // todo: region drawing
            }
        });
}

