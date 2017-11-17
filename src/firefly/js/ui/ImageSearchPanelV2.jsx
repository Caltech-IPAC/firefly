/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get, includes, isNil} from 'lodash';
import {FormPanel} from './FormPanel.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {SizeInputFields} from './SizeInputField.jsx';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {ServerParams} from '../data/ServerParams.js';
import {showInfoPopup} from './PopupUtil.jsx';
import {Tabs, Tab} from './panel/TabPanel.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {FileUpload} from './FileUpload.jsx';
import {ValidationField} from './ValidationField.jsx';
import FieldGroupUtils, {getFieldGroupResults, getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {parseWorldPt} from '../visualize/Point.js';
import WebPlotRequest, {WPConst} from '../visualize/WebPlotRequest.js';
import {dispatchPlotImage} from '../visualize/ImagePlotCntlr.js';
import {getImageMasterData} from '../visualize/ui/AllImageSearchConfig.js';
import {ImageSelect} from './ImageSelect.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {logError} from '../util/WebUtil.js';

import './ImageSearchPanelV2.css';


const FG_KEYS = {
    image_type: 'ImageSearchPanel_imageType',
    single: 'ImageSearchPanel_single',
    red: 'ImageSearchPanel_red',
    green: 'ImageSearchPanel_green',
    blue: 'ImageSearchPanel_blue'
};


export class ImageSearchPanelV2 extends PureComponent {

    constructor(props) {
        super(props);
        this.state= {   imageMasterData: undefined,
                        showError: false
                    };
    }

    componentWillUnmount() {
        if (this.removeListeners) this.removeListeners.forEach( (rl) => rl());
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListeners = Object.values(FG_KEYS)
            .map( (key) => FieldGroupUtils.bindToStore(key, (fields) => {
                if (this.iAmMounted && fields) this.setState({[key]: fields});
            }));
        const cont = this;
        getImageMasterData()
            .then( (imageMasterData) => {
                cont.iAmMounted && cont.setState({imageMasterData});
            }).catch( (e) => {
                cont.iAmMounted && cont.setState({showError:true});
            });
    }

    render() {
        let {multiSelect=true, title='Image Search'}= this.props;
        const {imageMasterData, showError= false}= this.state;
        const isThreeColor = getFieldVal(FG_KEYS.image_type, 'imageType') === 'threeColor';
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
                    <FormPanel  inputStyle = {{width: 700, backgroundColor: 'transparent', padding: 'none', border: 'none'}}
                                groupKey = {Object.values(FG_KEYS)} includeUnmounted={true}
                                params = {{hideOnInvalid: false}}
                                onSubmit = {(request) => onSearchSubmit(request, imageMasterData)}
                                onError = {(request) => searchFailed(request)}
                                onCancel = {hideSearchPanel}>
                        <ImageType/>
                        { isThreeColor && <ThreeColor {...{imageMasterData, multiSelect}}/>}
                        {!isThreeColor && <SingleChannel {...{groupKey: FG_KEYS.single, imageMasterData, multiSelect}}/>}
                    </FormPanel>
                </div>
            );
        } else {
            return <div className='ImageSearch__mask'> <div className='loading-mask'/> </div>;
        }
    }
}

ImageSearchPanelV2.propTypes = {
    title: PropTypes.string,
    multiSelect: PropTypes.bool
};


function SingleChannel({groupKey, imageMasterData, multiSelect}) {
    return (
        <div style={{width:'100%'}}>
            <FieldGroup groupKey={groupKey} reducerFunc={mainReducer} keepState={true}>
                <ImageSource {...{groupKey, imageMasterData, multiSelect}}/>
            </FieldGroup>
        </div>
    );
}

function ThreeColor({imageMasterData, multiSelect}) {

    return (
        <div style={{marginTop: 5}}>
            <Tabs componentKey='ImageSearchPanelV2' resizable={false} useFlex={true}
                  borderless={true} contentStyle={{backgroundColor: 'rgb(202, 202, 202)', paddingBottom: 2}}
                  headerStyle={{display:'inline-flex', justifyContent:'center'}}>
                <Tab key='ImageSearchRed' name='red' label={<div style={{width:40, color:'red'}}>Red</div>}>
                    <SingleChannel {...{groupKey: FG_KEYS.red, imageMasterData, multiSelect}}/>
                </Tab>
                <Tab key='ImageSearchRed' name='green' label={<div style={{width:40, color:'green'}}>Green</div>}>
                    <SingleChannel {...{groupKey: FG_KEYS.green, imageMasterData, multiSelect}}/>
                </Tab>
                <Tab key='ImageSearchRed' name='blue' label={<div style={{width:40, color:'blue'}}>Blue</div>}>
                    <SingleChannel {...{groupKey: FG_KEYS.blue, imageMasterData, multiSelect}}/>
                </Tab>
            </Tabs>
        </div>
    );
}

function ImageType({}) {
    return (
        <FieldGroup className='ImageSearch__section' groupKey={FG_KEYS.image_type} keepState={true}>
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

function ImageSource({groupKey, imageMasterData, multiSelect}) {
    const isThreeColor = getFieldVal(FG_KEYS.image_type, 'imageType') === 'threeColor';
    const options = [   {label: 'IRSA', value: 'irsa'},
                        {label: 'Upload', value: 'upload'},
                        {label: 'URL', value: 'url'}];
    isThreeColor && (options.push({label: 'None', value: 'none'}));
    const defaultValue = isThreeColor ? 'none' : 'irsa';
    const imageSource = getFieldVal(groupKey, 'imageSource', defaultValue);

    return (
        <div>
            <div className='ImageSearch__section'>
                <div className='ImageSearch__section--title'>2. Select image source</div>
                <RadioGroupInputField
                    initialState= {{ defaultValue, options,
                    tooltip: 'Please select the image source'}}
                    fieldKey='imageSource'/>
            </div>
            {imageSource === 'url'    && <SelectUrl {...{groupKey, imageMasterData, multiSelect}}/>}
            {imageSource === 'irsa'   && <SelectIrsa {...{groupKey, imageMasterData, multiSelect}}/>}
            {imageSource === 'upload' && <SelectUpload {...{groupKey, imageMasterData, multiSelect}}/>}
        </div>
    );
}

function SelectIrsa({groupKey,  imageMasterData, multiSelect}) {
    const title = '4. Select Data Set';
    const style = {width: '100%', height: 300};
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
            <ImageSelect key='ImageSelect'  {...{groupKey, title, style, addChangeListener, imageMasterData, multiSelect}} />
        </div>
    );
}

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
    // put reducing logic here is any

    // call all listeners for
    inFields = Object.values(changeListeners).reduce( (p, l) => l(p, action), inFields);
    return inFields;
};


function hideSearchPanel() {
    dispatchHideDropDown();
}


function onSearchSubmit(request, imageMasterData) {
    console.log(request);
    const validInfo= validateInput(request);
    if (!validInfo.valid)  {
        showInfoPopup(validInfo.message);
        return false;
    }
    doImageSearch(request, imageMasterData);
}

function searchFailed(request) {
    const validInfo= validateInput(request);
    showInfoPopup(!validInfo.valid ? validInfo.message :'One or more fields are not valid');
}


//-------------------------------------------------------------------------
// ----------- validation and successful results Code - todo: needs edit to work with new UI
//-------------------------------------------------------------------------


function hasValidInput(request) {
    if (!request) return {valid: false, message: 'Target is required'};

    if(request.imageSource==='upload'){
        if(!includes(request.fileUpload, 'fits') ){
            return ({valid:false, message:'FITS file is required'});
        }
    } else if (request.imageSource==='url')  {
        if (isNil(request.txURL)){
            return ({valid:false, message:'invalid URL'});
        }
    } else {
        const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
        if (!wp) {
            return ({valid: false, message: 'Target is required'});
        }
        if (!Object.keys(request).filter((k) => k.startsWith('IMAGES_')).some((k) => request[k].length > 0)) { // are any checked
            return ({valid: false, message: 'You must select at least one image'});
        }
    }
    return {valid:true, message: 'success'};
}

function validateInput(allFields) {
    const isThreeColor = getFieldVal(FG_KEYS.image_type, 'imageType') === 'threeColor';

    if (isThreeColor) {
                const resps = [FG_KEYS.red, FG_KEYS.green, FG_KEYS.blue].map((band) => hasValidInput(get(allFields, band)));
                const validResp = resps.find((r) => r.valid);      // only one need to be valid
                if (validResp) {
            return validResp;
                } else {
            return {valid:false, message: 'At least one color must contains valid inputs'};
                }
    } else {
        return hasValidInput(get(allFields, [FG_KEYS.single]));
    }
}

//-------------------------------------------------------------------------
// ----------- END: validation and successful results Code
//-------------------------------------------------------------------------


//------------------------------------------------------------------
//------ The code below should work in the final product
//------------------------------------------------------------------

function doImageSearch(allFields, imageMasterData) {
    const isThreeColor = getFieldVal(FG_KEYS.image_type, 'imageType') === 'threeColor';

    if (isThreeColor){
        const redReq = get(allFields, FG_KEYS.red);
        const greenReq = get(allFields, FG_KEYS.green);
        const blueReq = get(allFields, FG_KEYS.blue);

        const wpSet = [];
        [redReq, greenReq, blueReq].forEach( (req) => {
            const wprs = makeWebPlotRequests(req, imageMasterData);
            if (wprs.length === 1) {
                wpSet.push(wprs[0]);
            } else { wpSet.push(undefined); }
        });
        dispatchPlotImage({threeColor:true, wpRequest: wpSet});

    } else {
        const wprs = makeWebPlotRequests(get(allFields, FG_KEYS.single), imageMasterData);
        wprs.forEach( (r) => dispatchPlotImage({wpRequest:r}));
    }
}

var outputMessage = (errMsg) => errMsg&&showInfoPopup(errMsg, 'Load Selected Image Error');
function getAllGroupFields(...keys) {
    return keys.reduce((prev, fg) => (Object.assign(prev, {[fg]: FieldGroupUtils.getGroupFields(fg)})), {});
}


/**
 * Creates one or more WebPlotRequest from the given parameters
 * @param request   the request parameters from the input fields
 * @param imageMasterData   the full imageMasterData when IRSA is selected
 * @returns {WebPlotRequest[]}
 */
function makeWebPlotRequests(request, imageMasterData){

    if (!request || request.imageSource === 'none') {
        return [];
    } else if (request.imageSource === 'upload') {
        return [makeFitsWebRequest(request)];

    } else if (request.imageSource === 'url') {
        return [makeURLWebRequest(request)];
    } else {
        const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
        const radius= request.conesize;

        const imageIdList= [];
        Object.keys(request).filter( (k) => k.startsWith('IMAGES_')).forEach( (p) => {
            const list= request[p].split(',');
            list.forEach( (e) => imageIdList.push(e));
        });
        const paramAry= imageMasterData.filter( (d) => imageIdList.includes(d.imageId));
        console.log(paramAry);
        return paramAry.map( (d) => makeWPRequest(wp, radius, d.plotRequestParams));
    }
}


//=========utility?====================

const rootId='conceptPid-';
let rootIdCnt=1;
function makeWPRequest(wp, radius, params) {
    const inReq= Object.assign( {
        [WPConst.PLOT_ID] : rootId+rootIdCnt,
        [WPConst.WORLD_PT] : wp.toString(),
        [WPConst.SIZE_IN_DEG] : radius+'',
        [WPConst.PLOT_GROUP_ID] : 'multiImageGroup',
        [WPConst.GROUP_LOCKED] : 'true',
    }, params);

    rootIdCnt++;
    return WebPlotRequest.makeFromObj(inReq);
}

function makeURLWebRequest(request) {

    var url = get(request, 'txURL');
    const params = {
        [WPConst.PLOT_ID] : rootId+rootIdCnt,
        [WPConst.URLKEY] : url,
        [WPConst.PLOT_GROUP_ID] : 'multiImageGroup',
        [WPConst.GROUP_LOCKED] : 'true'
    };
    rootIdCnt++;

    return  WebPlotRequest.makeFromObj(params);
}

function makeFitsWebRequest(request, color='') {

    var fits = get(request, 'fileUpload');

    const params = {
        [WPConst.PLOT_ID] : rootId+rootIdCnt,
        [WPConst.FILE] : fits,
        [WPConst.PLOT_GROUP_ID] : 'multiImageGroup',
        [WPConst.GROUP_LOCKED] : 'true'
       // [WPConst.TITLE_OPTIONS] : TitleOptions.FILE_NAME
    };
    rootIdCnt++;
    return  WebPlotRequest.makeFromObj(params);


}
