/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get, includes, isNil} from 'lodash';
import {FormPanel} from '../../ui/FormPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {SizeInputFields} from '../../ui/SizeInputField.jsx';
import {TargetPanel} from '../../ui/TargetPanel.jsx';
import {ServerParams} from '../../data/ServerParams.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {Tabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import FieldGroupUtils, {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {parseWorldPt} from '../../visualize/Point.js';
import WebPlotRequest, {WPConst} from '../../visualize/WebPlotRequest.js';
import {dispatchPlotImage, visRoot} from '../../visualize/ImagePlotCntlr.js';
import {getImageMasterData} from '../../visualize/ui/AllImageSearchConfig.js';
import {ImageSelect} from '../../ui/ImageSelect.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {NewPlotMode, findViewerWithItemId, getMultiViewRoot, getViewer, getAViewFromMultiView, IMAGE} from '../MultiViewCntlr.js';
import {getPlotViewById} from '../PlotViewUtil.js';


import './ImageSearchPanelV2.css';


const FG_KEYS = {
    main: 'ImageSearchPanel_imageType',
    single: 'ImageSearchPanel_single',
    red: 'ImageSearchPanel_red',
    green: 'ImageSearchPanel_green',
    blue: 'ImageSearchPanel_blue'
};


var imageMasterData;        // latest imageMasterData retrieved from server

/**
 * @typedef {Object} ContextInfo
 * @property {string} plotId    The plotId to replace.  undefined to ADD.
 * @property {string} viewerId  The viewerId to add to.
 * @property {string} multiSelect ImageSelect mode to render in.
 */
/**
 * returns the context information
 * @returns {ContextInfo}
 */
function getContexInfo() {
    const mvroot = getMultiViewRoot();
    let plotId = get(visRoot(), 'activePlotId');
    let viewerId = plotId && findViewerWithItemId(mvroot, plotId, IMAGE);
    let viewer = viewerId && getViewer(mvroot, viewerId);

    if (!viewer || canNotUpdatePlot(viewer)) {
        // viewer does not exists or cannot be updated, find another one that can.
        viewer = getAViewFromMultiView(mvroot, IMAGE);
        viewerId =  viewer && viewer.viewerId;
    }
    if (canAddNewPlot(viewer)) {
        // don't replace if add is allowed
        plotId = undefined;
    }
    const multiSelect = !plotId;  // when replace, set to single select mode
    return {plotId, viewerId, multiSelect};
}

/*-----------------------------------------------------------------------------------------*/
/* search panel used in drop-down                                                          */
/*-----------------------------------------------------------------------------------------*/
export function ImageSearchDropDown({gridSupport}) {
    const {plotId, viewerId, multiSelect} = getContexInfo();
    return (
        <FormPanel  inputStyle = {{width: 700, backgroundColor: 'transparent', padding: 'none', border: 'none'}}
                    groupKey = {Object.values(FG_KEYS)} includeUnmounted={true}
                    params = {{hideOnInvalid: false}}
                    onSubmit = {(request) => onSearchSubmit({request, plotId, viewerId, gridSupport})}
                    onError = {searchFailed}
                    onCancel = {dispatchHideDropDown}>
            <ImageSearchPanelV2 {...{multiSelect}}/>
            {gridSupport && <GridSupport/>}
        </FormPanel>
    );
}
function GridSupport() {
    return (
        <FieldGroup className='ImageSearch__section' groupKey={FG_KEYS.main} keepState={true}>
            <div className='ImageSearch__section--title' style={{width: 138}}>Add to new grid cell</div>
            <CheckboxGroupInputField
                fieldKey='createNewCell'
                options={[{label: '', value: 'newCell'}]}
                labelWidth = {0}
            />
        </FieldGroup>
    );
}

/*-----------------------------------------------------------------------------------------*/
/* search panel used in pop-up                                                             */
/*-----------------------------------------------------------------------------------------*/
const popupId = 'ImageSelectPopup';
export function showImageSelPanel(popTitle) {
    const {plotId, viewerId, multiSelect} = getContexInfo();
    const onSubmit = (request) => {
        onSearchSubmit({request, plotId, viewerId}) && dispatchHideDialog(popupId);
    };

    const popup = (
        <PopupPanel title={popTitle}>
            <FormPanel  inputStyle = {{width: 700, backgroundColor: 'transparent', padding: 'none', border: 'none'}}
                        style = {{padding: '0 5px'}}
                        groupKey = {Object.values(FG_KEYS)}
                        includeUnmounted={true}
                        submitText='Load'
                        onSubmit = {onSubmit}
                        onError = {searchFailed}
                        onCancel = {() => dispatchHideDialog(popupId)}>
                <ImageSearchPanelV2 {...{title:'', multiSelect}}/>
            </FormPanel>
        </PopupPanel>
    );

    DialogRootContainer.defineDialog(popupId, popup);
    dispatchShowDialog(popupId);
}
/*-----------------------------------------------------------------------------------------*/


/**
 *
 */
export class ImageSearchPanelV2 extends PureComponent {

    constructor(props) {
        super(props);
        this.state= {   imageMasterData: undefined,
                        showError: false
                    };
    }

    componentWillUnmount() {
        if (this.removeListeners) this.removeListeners.forEach( (rl) => rl());
        this.iAmMounted= false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListeners = Object.values(FG_KEYS)
            .map( (key) => FieldGroupUtils.bindToStore(key, (fields) => {
                if (this.iAmMounted && fields) this.setState({[key]: fields});
            }));
        getImageMasterData()
            .then( (newImageMasterData) => {
                if (this.iAmMounted) {
                    imageMasterData = newImageMasterData;
                    this.setState({imageMasterData});
                }
            }).catch( (e) => {
                console.error(e);
                this.iAmMounted && this.setState({showError:true});
            });
    }

    render() {
        const {archiveName='Archive', title='Image Search'}= this.props;
        let {multiSelect=true} = this.props;
        const {imageMasterData, showError= false}= this.state;
        const isThreeColor = getFieldVal(FG_KEYS.main, 'imageType') === 'threeColor';
        multiSelect = !isThreeColor && multiSelect;

        if (showError) {
            return (
                <div style={{width:500}}>
                    <div style={{ padding: '50px 0 50px 0', textAlign: 'center', fontSize: '20pt' }}>
                        Error loading image search meta data
                    </div>
                </div>
            );
        } else if (imageMasterData) {
            return (
                <div>
                    <div className='ImageSearch__title'>{title}</div>
                    <ImageType/>
                    { isThreeColor && <ThreeColor {...{imageMasterData, multiSelect, archiveName}}/>}
                    {!isThreeColor && <SingleChannel {...{groupKey: FG_KEYS.single, imageMasterData, multiSelect, archiveName}}/>}
                </div>
            );
        } else {
            return <div className='ImageSearch__mask'> <div className='loading-mask'/> </div>;
        }
    }
}

ImageSearchPanelV2.propTypes = {
    title:       PropTypes.string,
    archiveName: PropTypes.string,
    multiSelect: PropTypes.bool,
};

// eslint-disable-next-line
function SingleChannel({groupKey, imageMasterData, multiSelect, archiveName}) {
    return (
        <div style={{width:'100%'}}>
            <FieldGroup groupKey={groupKey} reducerFunc={mainReducer} keepState={true}>
                <ImageSource {...{groupKey, imageMasterData, multiSelect, archiveName}}/>
            </FieldGroup>
        </div>
    );
}

// eslint-disable-next-line
function ThreeColor({imageMasterData, multiSelect, archiveName}) {

    return (
        <div style={{marginTop: 5}}>
            <Tabs componentKey='ImageSearchPanelV2' resizable={false} useFlex={true}
                            borderless={true} contentStyle={{backgroundColor: 'rgb(202, 202, 202)', paddingBottom: 2}}
                            headerStyle={{display:'inline-flex', justifyContent:'center'}}>
                <Tab key='ImageSearchRed' name='red' label={<div style={{width:40, color:'red'}}>Red</div>}>
                    <SingleChannel {...{groupKey: FG_KEYS.red, imageMasterData, multiSelect, archiveName}}/>
                </Tab>
                <Tab key='ImageSearchGreen' name='green' label={<div style={{width:40, color:'green'}}>Green</div>}>
                    <SingleChannel {...{groupKey: FG_KEYS.green, imageMasterData, multiSelect, archiveName}}/>
                </Tab>
                <Tab key='ImageSearchBlue' name='blue' label={<div style={{width:40, color:'blue'}}>Blue</div>}>
                    <SingleChannel {...{groupKey: FG_KEYS.blue, imageMasterData, multiSelect, archiveName}}/>
                </Tab>
            </Tabs>
        </div>
    );
}

function ImageType({}) {
    return (
        <FieldGroup className='ImageSearch__section' groupKey={FG_KEYS.main} keepState={true}>
            <div className='ImageSearch__section--title'>1. Choose image type</div>
            <RadioGroupInputField
                initialState= {{ defaultValue: 'singleChannel',
                             tooltip: 'Please select the image type'}}
                options={[  {label: 'Single channel image', value: 'singleChannel'},
                        {label: '3-color', value: 'threeColor'}]}
                fieldKey='imageType'
            />
        </FieldGroup>
    );
}

// eslint-disable-next-line
function ImageSource({groupKey, imageMasterData, multiSelect, archiveName='Archive'}) {
    const isThreeColor = getFieldVal(FG_KEYS.main, 'imageType') === 'threeColor';
    const options = [   {label: archiveName, value: 'archive'},
                        {label: 'Upload', value: 'upload'},
                        {label: 'URL', value: 'url'}];
    isThreeColor && (options.push({label: 'None', value: 'none'}));
    const defaultValue = isThreeColor ? 'none' : 'archive';
    const imageSource = getFieldVal(groupKey, 'imageSource', defaultValue);

    return (
        <div>
            <div className='ImageSearch__section'>
                <div className='ImageSearch__section--title'>2. Select image source</div>
                <RadioGroupInputField
                    initialState = {{ defaultValue, options, tooltip: 'Please select the image source'}}
                    defaultValue ={defaultValue}
                    options = {options}
                    fieldKey = 'imageSource'/>
            </div>
            {imageSource === 'url'    && <SelectUrl {...{groupKey, imageMasterData, multiSelect}}/>}
            {imageSource === 'archive'   && <SelectArchive {...{groupKey, imageMasterData, multiSelect}}/>}
            {imageSource === 'upload' && <SelectUpload {...{groupKey, imageMasterData, multiSelect}}/>}
        </div>
    );
}

// eslint-disable-next-line
function SelectArchive({groupKey,  imageMasterData, multiSelect}) {
    const title = '4. Select Data Set';
    const style = {width: '100%', height: 350};
    const targetStyle = {height: 40};
    const sizeStyle = {margin: '-5px 0 0 36px'};

    return (
        <div>
            <div className='ImageSearch__section'>
                <div className='ImageSearch__section--title'>3. Set spatial constraints</div>
                <div>
                    <TargetPanel labelWidth={100} feedbackStyle={targetStyle}/>
                    <SizeInputFields fieldKey='conesize' showFeedback={true}
                                     feedbackStyle={sizeStyle}
                                     initialState={{
                                             unit: 'arcsec',
                                             labelWidth : 0,
                                             nullAllowed: false,
                                             value: (500/3600)+'',
                                             min: 1 / 3600,
                                             max: 1,
                                         }}
                                     label={'Choose Radius'}
                    />
                </div>
            </div>
            <ImageSelect key={`ImageSelect_${groupKey}`} {...{groupKey, title, style, addChangeListener, imageMasterData, multiSelect}} />
        </div>
    );
}

// eslint-disable-next-line
function SelectUpload({groupKey,  imageMasterData, multiSelect}) {
    return (
        <div className='ImageSearch__section' style={{height: 35, alignItems: 'center'}}>
            <div className='ImageSearch__section--title'>3. Select Image</div>
            <FileUpload
                fieldKey='fileUpload'
                initialState= {{
                            tooltip: 'Select a file to upload' }}
            />
        </div>
    );
}

// eslint-disable-next-line
function SelectUrl({groupKey,  imageMasterData, multiSelect}) {
    return (
        <div className='ImageSearch__section' style={{height: 35, alignItems: 'center'}}>
            <div className='ImageSearch__section--title'>3. Enter URL</div>
            <ValidationField
                labelWidth={150}
                style={{width: 475}}
                fieldKey='txURL'
            />
        </div>
    );
}


// map of key/listener
const changeListeners = {};
function addChangeListener(key, changeListener) {
    changeListeners[key] = changeListener;
}
function mainReducer(inFields, action) {
    // put reducing logic here if any

    // call all listeners for
    inFields = Object.values(changeListeners).reduce( (p, l) => l(p, action), inFields);
    return inFields;
};


function onSearchSubmit({request, gridSupport, plotId, plotGroupId, viewerId}) {
    const validInfo= validateInput(request);
    if (!validInfo.valid)  {
        showInfoPopup(validInfo.message);
        return false;
    }
    doImageSearch({imageMasterData, request, gridSupport, plotId, plotGroupId, viewerId});
    return true;
}

function searchFailed(request) {
    const validInfo= validateInput(request);
    showInfoPopup(!validInfo.valid ? validInfo.message :'One or more fields are not valid');
}


//-------------------------------------------------------------------------
// ----------- validation and successful results Code
//-------------------------------------------------------------------------
function getValidatedInfo(request, isThreeColor) {

    if (!request) {
        return (isThreeColor) ? {valid:true, message: 'success'} : {valid: false, message: 'Target is required'};
    }

    switch (request.imageSource) {
        case 'upload' :
            if(!includes(request.fileUpload, 'fits') ){
                return ({valid:false, message:'FITS file is required'});
            }
            break;
        case 'url' :
            if (isNil(request.txURL)){
                return ({valid:false, message:'invalid URL'});
            }
            break;
        case 'archive' :
            const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
            if (!wp) {
                return ({valid: false, message: 'Target is required'});
            }
            if (!Object.keys(request).filter((k) => k.startsWith('IMAGES_')).some((k) => request[k].length > 0)) { // are any checked
                return ({valid: false, message: 'You must select at least one image'});
            }
            break;
    }
    return {valid:true, message: 'success'};
}

function validateInput(allFields) {
    const isThreeColor = getFieldVal(FG_KEYS.main, 'imageType') === 'threeColor';

    if (isThreeColor) {
        const resps = [FG_KEYS.red, FG_KEYS.green, FG_KEYS.blue].map((band) => {
                        const req = get(allFields, band);
                        if (get(req, 'imageSource', 'none') === 'none') {
                            return undefined;
                        } else {
                            return getValidatedInfo(req, true);
                        }
                    });

        const errMsg = ['Red', 'Green', 'Blue'].map( (b, idx) => get(resps, [idx, 'valid'], true) ? '' : b + ': ' + resps[idx].message)
                                               .filter((msg) => msg);
        if (errMsg.length === 1) {
            return {valid: false, message: errMsg[0]};
        } else if (errMsg.length > 1) {
            const msg = errMsg.map((s, idx) => <li key={idx}>{s}</li>);
            return {valid: false, message: <ol style={{margin:'unset', paddingLeft:15}}>{msg}</ol>};
        } else if (!resps.some((r) => get(r, 'valid'))) {
            return {valid: false, message: 'At least one of the 3 colors must be selected'};
        } else {
            return {valid: true, message: 'success'};
        }
    } else {
        return getValidatedInfo(get(allFields, [FG_KEYS.single]));
    }
}

//-------------------------------------------------------------------------
// ----------- END: validation and successful results Code
//-------------------------------------------------------------------------


//------------------------------------------------------------------
//------ The code below should work in the final product
//------------------------------------------------------------------
function doImageSearch({ imageMasterData, request, plotId, plotGroupId, viewerId}) {

    if (plotId) {
        plotGroupId = getPlotViewById(visRoot(), plotId).plotGroupId;
        viewerId = findViewerWithItemId(getMultiViewRoot(), plotId, IMAGE);
    } else if (viewerId) {
        //IRSA-142 LZ 4/07/17
        //When the image from different groups, the wcsMatch does not work since the match only matches the image within the same group.
        //If the group id exists, add the image into the same group
        const viewer = getViewer(getMultiViewRoot(), viewerId);
        if (viewer && viewer.itemIdAry[0]) {
            const pv = getPlotViewById(visRoot(), viewer.itemIdAry[0]);
            if (pv) {
                plotGroupId = pv.plotGroupId;
            }
        }
    }

    if (get(request, [FG_KEYS.main, 'createNewCell']) === 'newCell')  {
        viewerId = newCellViewerId();
    }

    const isThreeColor = getFieldVal(FG_KEYS.main, 'imageType') === 'threeColor';
    if (isThreeColor){
        const redReq = get(request, FG_KEYS.red);
        const greenReq = get(request, FG_KEYS.green);
        const blueReq = get(request, FG_KEYS.blue);

        const wpSet = [];
        [redReq, greenReq, blueReq].forEach( (req) => {
            const wprs = makeWebPlotRequests(req, imageMasterData, plotId, plotGroupId);
            if (wprs.length === 1) {
                wpSet.push(wprs[0]);
            } else { wpSet.push(undefined); }
        });
        dispatchPlotImage({threeColor:true, wpRequest: wpSet, viewerId});

    } else {
        const wprs = makeWebPlotRequests(get(request, FG_KEYS.single), imageMasterData, plotId, plotGroupId);
        wprs.forEach( (r) => dispatchPlotImage({wpRequest:r, viewerId}));
    }
}


/**
 * Creates one or more WebPlotRequest from the given parameters
 * @param request
 * @param imageMasterData
 * @param plotId
 * @param plotGroupId
 * @returns {WebPlotRequest[]}
 */
function makeWebPlotRequests(request, imageMasterData, plotId, plotGroupId){

    if (get(request, 'imageSource', 'none') === 'none') {
        return [];
    } else if (request.imageSource === 'upload') {
        return [makeFitsWebRequest(request, plotId, plotGroupId)];

    } else if (request.imageSource === 'url') {
        return [makeURLWebRequest(request, plotId, plotGroupId)];
    } else {
        const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
        const radius= request.conesize;

        const imageIdList= [];
        Object.keys(request).filter( (k) => k.startsWith('IMAGES_')).forEach( (p) => {
            const list= request[p].split(',');
            list.forEach( (e) => imageIdList.push(e));
        });
        const paramAry= imageMasterData.filter( (d) => imageIdList.includes(d.imageId));
        return paramAry.map( (d) => makeWPRequest(wp, radius, d.plotRequestParams, plotId, plotGroupId));
    }
}


//=========utility?====================

const canAddNewPlot = (viewer) => (!viewer.viewerId.includes('RESERVED') && (viewer.canReceiveNewPlots === NewPlotMode.create_replace.key));
const canNotUpdatePlot = (viewer) => (viewer.viewerId.includes('RESERVED') || (viewer.canReceiveNewPlots === NewPlotMode.none.key));

// this is used by gridSupport
const newCellViewerId = (() => {
    let newCellCnt=0;
    return () => {
        return `autoCreatedCell--${++newCellCnt}`;
    };
})();

const nextPlotId = (() => {
    let rootIdCnt=0;
    return () => {
        return `imageSearchPid-${++rootIdCnt}`;
    };
})();

function makeWPRequest(wp, radius, params, plotId=nextPlotId(), plotGroupId='multiImageGroup', type) {
    const inReq= Object.assign( {
        [WPConst.PLOT_ID] : plotId,
        [WPConst.WORLD_PT] : wp.toString(),
        [WPConst.SIZE_IN_DEG] : radius+'',
        [WPConst.PLOT_GROUP_ID] : plotGroupId,
        [WPConst.GROUP_LOCKED] : 'true',
        [WPConst.TYPE] : type
    }, params);

    return WebPlotRequest.makeFromObj(inReq);
}

function makeURLWebRequest(request, plotId=nextPlotId(), plotGroupId='multiImageGroup', type) {

    var url = get(request, 'txURL');
    const params = {
        [WPConst.PLOT_ID] : plotId,
        [WPConst.URLKEY] : url,
        [WPConst.PLOT_GROUP_ID] : plotGroupId,
        [WPConst.GROUP_LOCKED] : 'true',
        [WPConst.TYPE] : type
    };
    return  WebPlotRequest.makeFromObj(params);
}

function makeFitsWebRequest(request, plotId=nextPlotId(), plotGroupId='multiImageGroup', type) {

    var fits = get(request, 'fileUpload');

    const params = {
        [WPConst.PLOT_ID] : plotId,
        [WPConst.FILE] : fits,
        [WPConst.PLOT_GROUP_ID] : plotGroupId,
        [WPConst.GROUP_LOCKED] : 'true',
        [WPConst.TYPE] : type
    };
    return  WebPlotRequest.makeFromObj(params);
}
