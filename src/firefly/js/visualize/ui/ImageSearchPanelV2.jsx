/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Sheet, Stack, Typography} from '@mui/joy';
import {get, includes, isNil, isString} from 'lodash';
import PropTypes, {bool, object, shape, string} from 'prop-types';
import React, {useContext, useEffect, useState} from 'react';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {dispatchHideDialog, dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import {MetaConst} from '../../data/MetaConst';
import {ServerParams} from '../../data/ServerParams.js';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {makeFileRequest} from '../../tables/TableRequestUtil';
import {dispatchTableSearch} from '../../tables/TablesCntlr';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {FieldGroup, FieldGroupCtx} from '../../ui/FieldGroup.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {FormPanel} from '../../ui/FormPanel.jsx';
import {getHipsUrl, HiPSImageSelect, makeHiPSWebPlotRequest} from '../../ui/HiPSImageSelect.jsx';
import {ImageSelect} from '../../ui/ImageSelect.jsx';
import {StatefulTabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {useFieldGroupValue, useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {SizeInputFields} from '../../ui/SizeInputField.jsx';
import {DEF_TARGET_PANEL_KEY, TargetPanel} from '../../ui/TargetPanel.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {WorkspaceUpload} from '../../ui/WorkspaceViewer.jsx';
import {isDefined} from '../../util/WebUtil';
import {dispatchPlotHiPS, dispatchPlotImage, visRoot} from '../ImagePlotCntlr.js';
import {parseWorldPt} from '../Point.js';
import {getImageMasterData} from './AllImageSearchConfig.js';
import WebPlotRequest, {WPConst} from '../../visualize/WebPlotRequest.js';
import CoordinateSys from '../CoordSys';
import {useForImageSearch} from '../HiPSListUtil.js';
import {
    findViewerWithItemId, getAViewFromMultiView, getMultiViewRoot, getViewer, IMAGE, NewPlotMode
} from '../MultiViewCntlr.js';
import {PlotAttribute} from '../PlotAttribute';
import {getPlotViewById} from '../PlotViewUtil.js';
import VisUtil from '../VisUtil';
import {getWorkspaceConfig} from '../WorkspaceCntlr.js';
import {FG_KEYS, FD_KEYS} from './UIConst';

var imageMasterData;        // latest imageMasterData retrieved from server
const scrollDivId = 'ImageSearchScroll';

// set 20 as the maximum plot
function* getHiPSPlotId() {
    let index = 0;

    while (true) {
        ++index;
        yield 'aHiPSId' + index;
    }
}

export const genHiPSPlotId = getHiPSPlotId();

/**
 * @typedef {Object} ContextInfo
 * @property {string} plotId    The plotId to replace.  undefined to ADD.
 * @property {string} viewerId  The viewerId to add to.
 * @property {string} multiSelect ImageSelect mode to render in.
 */

/**
 * returns the context information
 * @param [renderTreeId]
 * @param [presetViewerId] if defined, then use as viewerId
 * @returns {ContextInfo}
 */
function getContexInfo(renderTreeId, presetViewerId) {
    const mvroot = getMultiViewRoot();
    let plotId = visRoot()?.activePlotId;
    let viewerId= presetViewerId ? presetViewerId : (plotId && findViewerWithItemId(mvroot, plotId, IMAGE));
    let viewer = getViewer(mvroot, viewerId);

    if (viewer && renderTreeId && viewer.renderTreeId && renderTreeId!==viewer.renderTreeId) {
        viewer = getAViewFromMultiView(mvroot, IMAGE, renderTreeId);
        viewerId= viewer.viewerId;
    }

    if (!viewer || viewer.reservedContainer || canNotUpdatePlot(viewer)) {
        // viewer does not exists or cannot be updated, find another one that can.
        viewer = getAViewFromMultiView(mvroot, IMAGE, renderTreeId);
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

function ImageSearchPanel({resizable=true, onSubmit, gridSupport = false, multiSelect, submitText,
                              onCancel=dispatchHideDropDown, noScroll, initArgs}) {
    const archiveName =  get(getAppOptions(), 'ImageSearch.archiveName');
    const resize = {resize: 'both', overflow: 'hidden', paddingBottom: 5};
    const dim = {height: 600, width: 725, minHeight: 600, minWidth: 725};
    const style = resizable ? {...dim, ...resize} : {width: '100%'};

    const imageType = useStoreConnector(() => getFieldVal(FG_KEYS.main, FD_KEYS.type));

    return (
        <div style={style}>
            <FormPanel  inputStyle = {{display: 'flex', flexDirection: 'column', backgroundColor: 'transparent', padding: 'none', border: 'none'}}
                        submitBarStyle = {{flexShrink: 0, padding: '0 4px 3px'}}
                        groupKey = {Object.values(FG_KEYS)} includeUnmounted={true}
                        groupsToUse={() => getGroupsToValidate(imageType)}
                        params = {{hideOnInvalid: false}}
                        submitText={submitText}
                        onSubmit = {onSubmit}
                        onError = {searchFailed}
                        onCancel = {onCancel}
                        help_id = {'basics.searching'}>
                <ImageSearchPanelV2 {...{multiSelect, archiveName, noScroll, initArgs}}/>
                {gridSupport && <GridSupport/>}
            </FormPanel>
        </div>
    );
}



export function ImageSearchDropDown({gridSupport, resizable=false, initArgs}) {
    const {renderTreeId} = useContext(RenderTreeIdCtx);
    const {viewerId:presetViewerId}= initArgs?.searchParams ?? {viewerId:undefined};
    const {plotId, viewerId, multiSelect} = getContexInfo(renderTreeId, presetViewerId);
    const onSubmit = (request) => onSearchSubmit({request, plotId, viewerId, gridSupport, renderTreeId});
    return <ImageSearchPanel {...{resizable, gridSupport, onSubmit, multiSelect, onCancel:dispatchHideDropDown, initArgs}}/>;
}

function GridSupport() {
    return (
        <Sheet {...{variant:'outlined', sx:{py:1}}}>
            <FieldGroup groupKey={FG_KEYS.main} keepState={true} sx={{m:1}} >
                <CheckboxGroupInputField fieldKey='createNewCell'
                    options={[{label: 'Add to new grid cell', value: 'newCell'}]}
                />
            </FieldGroup>
        </Sheet>
    );
}

/*-----------------------------------------------------------------------------------------*/
/* search panel used in pop-up                                                             */
/*-----------------------------------------------------------------------------------------*/
const popupId = 'ImageSelectPopup';
export function showImageSelPanel(popTitle, renderTreeId) {
    const {plotId, viewerId, multiSelect} = getContexInfo(renderTreeId);

    const onSubmit = (request) => {
        onSearchSubmit({request, plotId, viewerId, renderTreeId}) && dispatchHideDialog(popupId);
    };
    const onCancel = () => dispatchHideDialog(popupId);

    const popup = (
        <PopupPanel title={popTitle}>
            <ImageSearchPanel {...{resizable:true, gridSupport:false, submitText: 'Load', onSubmit, onCancel, multiSelect, noScroll:true}}/>
        </PopupPanel>
    );

    DialogRootContainer.defineDialog(popupId, popup);
    dispatchShowDialog(popupId);
}
/*-----------------------------------------------------------------------------------------*/

function isImageType(imageType=getFieldVal(FG_KEYS.main, FD_KEYS.type)) {
    const isThreeColorImgType = imageType === 'threeColor';
    const isHipsImgType = imageType === 'hipsImage';
    const isSingleChannelImgType = !isThreeColorImgType && !isHipsImgType;
    return {isThreeColorImgType, isHipsImgType, isSingleChannelImgType};
}

function getGroupsToValidate(imageType) {
    const {isThreeColorImgType, isHipsImgType, isSingleChannelImgType} = isImageType(imageType);

    if (isThreeColorImgType) return [FG_KEYS.main,FG_KEYS.red,FG_KEYS.green,FG_KEYS.blue,FG_KEYS.targetSelect];
    else if (isHipsImgType) return [FG_KEYS.main,FG_KEYS.hips,FG_KEYS.targetSelect];
    else if (isSingleChannelImgType) return [FG_KEYS.main,FG_KEYS.single,FG_KEYS.targetSelect];
    else return Object.values(FG_KEYS);
}

function ImageSearchPanelV2 ({archiveName='Search', title='Image Search', multiSelect=true, noScroll, initArgs}) {

    const [, setImageMasterData] = useState(() => imageMasterData);
    const [showError=false, setShowError] = useState();

    useEffect( ()=> {
        getImageMasterData().then( (newImageMasterData) => {
                imageMasterData = newImageMasterData;
                setImageMasterData(imageMasterData);
            }).catch( (e) => {
                console.error(e);
                setShowError(true);
            });
    }, []);

    const {wp,type}= initArgs?.searchParams ?? {};
    const [, setTarget]= useFieldGroupValue(DEF_TARGET_PANEL_KEY,FG_KEYS.targetSelect);
    const [, setType]= useFieldGroupValue(FD_KEYS.type,FG_KEYS.main);
    useEffect(() => {
        if (!wp) return;
        setTarget(wp);
    }, [wp]);
    useEffect(() => {
        (type) && setType(type);
    }, [type]);

    const imageType = useStoreConnector(() => getFieldVal(FG_KEYS.main, FD_KEYS.type));
    const {isThreeColorImgType, isHipsImgType, isSingleChannelImgType} = isImageType(imageType);

    multiSelect = !isThreeColorImgType && multiSelect;

    const pStyle = noScroll ? {} : {overflow: 'auto', height: 1};
    
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
            <div className='flex-full' style={{position: 'relative'}}>
                <Typography {...{level:'h4'}}>{title}</Typography>
                <div className='flex-full' style={pStyle} id={scrollDivId}>
                    <ImageType/>
                    {isThreeColorImgType &&
                    <ThreeColor {...{imageMasterData:imageMasterData.filter( (md) => (!md.dataType || md.dataType==='image')), multiSelect, archiveName, noScroll}}/>}
                    {isSingleChannelImgType && <SingleChannel {...{groupKey: FG_KEYS.single,
                        imageMasterData, multiSelect, archiveName, noScroll}}/>}
                    {isHipsImgType && <HiPSImage {...{imageMasterData, groupKey: FG_KEYS.hips,  archiveName, initArgs}}/>}
                </div>
            </div>
        );
    } else {
        return (
            <Box sx={{position: 'absolute', left: 0, top: 0, width: 1, height: 1}}>
                <div className='loading-mask'/>
            </Box>
        );
    }
}

ImageSearchPanelV2.propTypes = {
    title:       string,
    archiveName: string,
    multiSelect: bool,
    noScroll: bool,
    initArgs: shape({
        searchParams: object,
        urlApi: object,
    }),
};

function SingleChannel({groupKey, imageMasterData, multiSelect, archiveName, noScroll}) {
    return (
        <div className='flex-full'>
            <FieldGroup className='flex-full' groupKey={groupKey} reducerFunc={mainReducer} keepState={true}>
                <ImageSource {...{groupKey, imageMasterData, multiSelect, archiveName, noScroll}}/>
            </FieldGroup>
        </div>
    );
}

function ThreeColor({imageMasterData, multiSelect, archiveName, noScroll}) {
    return (
        <div className='flex-full' style={{marginTop: 5}}>
            <StatefulTabs componentKey='ImageSearchPanelV2' resizable={false} useFlex={true} borderless={true}
                  style={{flexGrow: 1, overflow: 'unset'}}
                  contentStyle={{backgroundColor: 'rgb(202, 202, 202)', paddingBottom: 2, overflow: 'unset'}}
                  headerStyle={{display:'inline-flex', marginLeft: 185}}>
                <Tab key='ImageSearchRed' name='red' label={<div style={{width:40, color:'red'}}>Red</div>}>
                    <SingleChannel {...{key: FG_KEYS.red, groupKey: FG_KEYS.red, imageMasterData, multiSelect, archiveName, noScroll}}/>
                </Tab>
                <Tab key='ImageSearchGreen' name='green' label={<div style={{width:40, color:'green'}}>Green</div>}>
                    <SingleChannel {...{key: FG_KEYS.green, groupKey: FG_KEYS.green, imageMasterData, multiSelect, archiveName, noScroll}}/>
                </Tab>
                <Tab key='ImageSearchBlue' name='blue' label={<div style={{width:40, color:'blue'}}>Blue</div>}>
                    <SingleChannel {...{key: FG_KEYS.blue, groupKey: FG_KEYS.blue, imageMasterData, multiSelect, archiveName, noScroll}}/>
                </Tab>
            </StatefulTabs>
        </div>
    );
}

function HiPSImage({groupKey, archiveName, imageMasterData, initArgs}) {
    return (
        <div className='flex-full' style={{flexGrow: 1}}>
            <FieldGroup className='flex-full' groupKey={groupKey} reducerFunc={mainReducer} keepState={true}>
                <ImageSource {...{groupKey, archiveName, imageMasterData, initArgs}}/>
            </FieldGroup>
        </div>
    );
}
HiPSImage.propTypes = {
    groupKey: PropTypes.string,
    archiveName: PropTypes.string,
    initArgs: PropTypes.object,
    imageMasterData: PropTypes.object,
};

function ImageType({}) {
    const options = [  {label: 'View FITS Images', value: 'singleChannel'},
                     {label: 'Create 3-Color Composite', value: 'threeColor'}];

    if (useForImageSearch()) {
        options.push({label: 'View HiPS Images', value: 'hipsImage'});
    }
    return (
        <Sheet {...{variant:'outlined', sx:{mx:0,py:1}}}>
            <FieldGroup groupKey={FG_KEYS.main} keepState={true}>
                <Stack {...{direction:'row', justifyContent:'flex-start', alignItems:'center', spacing:2}}>
                    <Typography {...{px:1, width:200, color:'primary', level:'title-md'}}>1. Choose Image Type</Typography>
                    <RadioGroupInputField
                        initialState= {{ value: 'singleChannel',
                            tooltip: 'Please select the image type'}}
                        options={ options }
                        orientation='horizontal'
                        fieldKey={ FD_KEYS.type }
                    />

                </Stack>
            </FieldGroup>
        </Sheet>

    );
}


function ImageSource({groupKey, imageMasterData, multiSelect, archiveName='Archive', noScroll, initArgs}) {

    const imageType = useStoreConnector(() => getFieldVal(FG_KEYS.main, FD_KEYS.type));
    const {isThreeColorImgType, isHipsImgType} = isImageType(imageType);

    const [getDefaultImages, ] = useFieldGroupValue('defaultImages', FG_KEYS.rgb);
    const defaultValue = isThreeColorImgType && !getDefaultImages() ? 'none' : 'archive'; // if 3 color, set 'archive' if defaultImages are defined
    const imageSource  = useStoreConnector(() => getFieldVal(groupKey, FD_KEYS.source, defaultValue));

    const options = [   {label: archiveName, value: 'archive'},
                        {label: 'URL', value: 'url'}];

    if (!isHipsImgType) {   // additional sources for non-Hips options
        options.splice(1, 0, {label: 'Use my image', value: 'upload'});

        if (getWorkspaceConfig()) {
            options.push({label: 'Workspace', value: ServerParams.IS_WS});
        }
    }

    isThreeColorImgType && (options.push({label: 'None', value: 'none'}));

    return (
        <Stack flexGrow={1}>
            <Sheet {...{variant:'outlined', sx:{position:'static', mx:0,py:1,mt:1/2}}}>
                <Stack {...{direction:'row', justifyContent:'flex-start', alignItems:'center', spacing:2}}>
                    <Typography {...{px:1, width:200, color:'primary', level:'title-md'}}>2. Select Image Source</Typography>
                    <RadioGroupInputField
                        initialState = {{ defaultValue, options, tooltip: 'Please select the image source'}}
                        defaultValue ={defaultValue}
                        options = {options}
                        orientation='horizontal'
                        fieldKey = { FD_KEYS.source }/>
                </Stack>
            </Sheet>
            {imageSource === 'url'  && (isHipsImgType ? <SelectArchive {...{groupKey, imageMasterData, multiSelect, isHipsImgType, noScroll, initArgs}}/> : <SelectUrl />)}
            {imageSource === 'archive'  && <SelectArchive {...{groupKey, imageMasterData, multiSelect, isHipsImgType, noScroll,initArgs}}/>}
            {imageSource === 'upload' && <SelectUpload />}
            {imageSource === ServerParams.IS_WS && <SelectWorkspace />}
        </Stack>
    );
}

function SelectArchive({groupKey,  imageMasterData, multiSelect, isHipsImgType, noScroll, initArgs}) {
    const {radius:initRadius}= initArgs?.searchParams ?? {};
    const {setFld}= useContext(FieldGroupCtx);
    useEffect(() => {
        initRadius && setFld('sizeFov', {value:initRadius*2, unit : 'deg' });
    }, [initRadius]);

    const title = '4. Select Data Set';
    const targetStyle = {height: 40, width: 450};
    const isHips = isHipsImgType;
    const sizeLabel = isHips ? 'Field of view (optional):' : 'Cutout size (leave blank for full size):';
    const sizeKey = isHips ? 'sizeFov' : 'conesize';
    const minSize = isHips ? 9/3600 : 1/3600;
    const maxSize = isHips ? 180 : 1;
    const sizeVal = isHips ?  '' : (500/3600) + '';
    const initUnit = isHips ? 'deg' : 'arcsec';

    return (
        <Sheet className='flex-full' sx={{position:'static',mt:1/2}}>
            <Sheet {...{position:'static', variant:'outlined', sx:{py:1, flexGrow:0}}}>
                <Stack direction={'row'} >
                    <Typography {...{px:1, width:200, color:'primary', level:'title-md'}}>3. Select Target</Typography>
                    <FieldGroup groupKey={FG_KEYS.targetSelect} keepState={true}>
                        <Stack spacing={2} direction='column'>
                            <TargetPanel labelWidth={isHips ? 150 : 100} feedbackStyle={targetStyle}
                                         label={isHips ? 'Coordinates or Object Name (optional):' : 'Coordinates or Object Name:'}
                                         nullAllowed={true}/>
                            <SizeInputFields fieldKey={sizeKey} showFeedback={true}
                                             feedbackStyle={{marginLeft: 185}}
                                             initialState={{
                                                 unit: initUnit,
                                                 labelWidth: 0,
                                                 nullAllowed: true,
                                                 value: initRadius? initRadius*2 : sizeVal,
                                                 min: minSize,
                                                 max: maxSize
                                             }}
                                             label={sizeLabel}
                                             key={`sizeInput_${groupKey}`}/>
                        </Stack>
                    </FieldGroup>
                </Stack>
            </Sheet>
            {isHips ?
                <HiPSImageSelect groupKey={groupKey} /> :
                <Sheet {...{variant:'outlined', sx:{position:'static', mt:1/2,py:1}}}>
                    <Stack>
                        <Typography {...{px:1, color:'primary', level:'title-md'}}>4. Select Data Set</Typography>
                        <ImageSelect key={`ImageSelect_${groupKey}`} {...{groupKey, title, addChangeListener, imageMasterData, multiSelect, scrollDivId: !noScroll && scrollDivId}} />
                    </Stack>
                </Sheet>
            }
        </Sheet>
    );
}


function SelectUpload() {
    return (
        <Sheet variant='outlined' sx={{mt: 1/2, py:1, width:'100%', display:'flex', flexDirection:'column', flex:'1 1 auto'}}>
            <Stack direction='row' spacing={2}>
                <Typography {...{px:1, width:200, color:'primary', level:'title-md'}}>3. Select Image</Typography>
                <FileUpload fieldKey='fileUpload' initialState= {{tooltip: 'Select a image to upload' }} />
            </Stack>
        </Sheet>
    );
}

function SelectWorkspace() {
    return (
        <div className='ImageSearch__section' style={{alignItems: 'center'}}>
            <Typography {...{px:1, color:'primary', level:'title-md'}}>3. Select Image</Typography>
            <WorkspaceUpload
                preloadWsFile={false}
                fieldKey='wsFilepath'
                initialState= {{tooltip: 'Select an image from workspace to upload' }}
            />
        </div>
    );
}

function SelectUrl() {
    return (
        <Sheet variant='outlined' sx={{mt:1/2, py:1, width:'100%', display:'flex', flexDirection:'column', flex:'1 1 auto'}}>
            <Stack direction='row' spacing={2}>
                <Typography {...{px:1, color:'primary', level:'title-md'}}>3. Enter URL</Typography>
                <ValidationField labelWidth={150} sx={{width: .8}} fieldKey='txURL' />
            </Stack>
        </Sheet>
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
}

function onSearchSubmit({request, gridSupport, plotId, plotGroupId, viewerId, renderTreeId}) {
    const commonTargetSelectReq = request?.[FG_KEYS.targetSelect];
    if (commonTargetSelectReq) {
        // copy common target selection request data to each individual request if image source is archive
        [FG_KEYS.single, FG_KEYS.red, FG_KEYS.green, FG_KEYS.blue, FG_KEYS.hips].forEach((k) => {
            if (request?.[k]?.imageSource === 'archive') request[k] = {...request[k], ...commonTargetSelectReq};
        });
    }

    const validInfo= validateInput(request);
    if (!validInfo.valid)  {
        showInfoPopup(validInfo.message);
        return false;
    }
    return doImageSearch({imageMasterData, request, gridSupport, plotId, plotGroupId, viewerId, renderTreeId});
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
        case ServerParams.IS_WS :
            if(!includes(request.wsFilepath, 'fits') ){
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

function getHipsValidateInfo(request) {
    if (request.imageSource === 'url') {
        if (isNil(request.txURL) || (!request.txURL.trim())){
            return ({valid:false, message:'invalid URL'});
        }
    } else if (!getHipsUrl()) {
        return ({valid: false, message: 'No HiPS source selected'});
    }
    return {valid:true, message: 'success'};
}

function validateInput(allFields) {
    if (isImageType()?.isHipsImgType) {
        return getHipsValidateInfo(get(allFields, FG_KEYS.hips));
    } else if (isImageType()?.isThreeColorImgType) {
        const resps = [FG_KEYS.red, FG_KEYS.green, FG_KEYS.blue].map((band) => {
                        const req = get(allFields, band);
                        if (get(req, FD_KEYS.source, 'none') === 'none') {
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
function doImageSearch({ imageMasterData, request, plotId, plotGroupId, viewerId, renderTreeId}) {

    const pvOptions= {};
    if (plotId) {
        viewerId = findViewerWithItemId(getMultiViewRoot(), plotId, IMAGE);
        const pv = getPlotViewById(visRoot(), plotId);
        if (pv) {
            plotGroupId = pv.plotGroupId;
            pvOptions.displayFixedTarget= pv.plotViewCtx.displayFixedTarget;
            pvOptions.userCanDeletePlots= pv.plotViewCtx.userCanDeletePlots;
        }
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

    // hips
    if (isImageType()?.isHipsImgType) {
        if (!plotId) {
            plotId = genHiPSPlotId.next().value;
        }
        const wpRequest = makeHiPSWebPlotRequest(get(request, FG_KEYS.hips), plotId, plotGroupId);
        if (wpRequest) {
            dispatchPlotHiPS({ plotId, viewerId, wpRequest, pvOptions});
            return true;
        }
    } else if  (isImageType()?.isThreeColorImgType){       // three color
        const redReq = get(request, FG_KEYS.red);
        const greenReq = get(request, FG_KEYS.green);
        const blueReq = get(request, FG_KEYS.blue);

        const wpSet = [];
        [redReq, greenReq, blueReq].forEach( (req) => {
            const wprs = makeRequests(req, imageMasterData, plotId, plotGroupId);
            if (wprs.length === 1) {
                wpSet.push(wprs[0].request);
            } else { wpSet.push(undefined); }
        });
        dispatchPlotImage({threeColor:true, wpRequest: wpSet, viewerId, renderTreeId, pvOptions});
        return true;
    } else {                       // single channel
        const reqAry = makeRequests(get(request, FG_KEYS.single), imageMasterData, plotId, plotGroupId);
        let retVal= false;
        reqAry.forEach( (r) => {
            if (r.dataType==='image' || r.dataType==='cube') {
                dispatchPlotImage({wpRequest:r.request, viewerId,renderTreeId,pvOptions});
                retVal= true;
            }
            else if (r.dataType==='table') {
                dispatchTableSearch(r.request, {backgroundable:true, showFilters:true, removable:true });
                retVal= true;
            }
        });
        return retVal;
    }
    return false;
}


/**
 * Creates one or more WebPlotRequest from the given parameters
 * @param request
 * @param imageMasterData
 * @param plotId
 * @param plotGroupId
 * @returns {Array.<{dataType:String,request:Object}>}
 */
function makeRequests(request, imageMasterData, plotId, plotGroupId){

    if (get(request, FD_KEYS.source, 'none') === 'none') {
        return [];

    } else if (request.imageSource === 'upload') {
        const fileName = get(request, 'fileUpload');
        return [{dataType:'image', request: addStdParams(WebPlotRequest.makeFilePlotRequest(fileName), plotId, plotGroupId)}];

    } else if (request.imageSource === ServerParams.IS_WS) {
        const fileName = get(request, 'wsFilepath');
        return [{dataType:'image', request: addStdParams(WebPlotRequest.makeWorkspaceRequest(fileName),plotId, plotGroupId)}];

    } else if (request.imageSource === 'url') {
        const url = get(request, 'txURL');
        return [{dataType:'image', request: addStdParams(WebPlotRequest.makeURLPlotRequest(url), plotId, plotGroupId)}];

    } else {
        const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
        const radius= request.conesize;

        const imageIdList= [];
        Object.keys(request).filter( (k) => k.startsWith('IMAGES_')).forEach( (p) => {
            const list= request[p].split(',');
            list.forEach( (e) => e && imageIdList.push(e));
        });
        const paramAry= imageMasterData.filter( (d) => imageIdList.includes(d.imageId) && (!d.dataType || d.dataType==='image' || d.dataType==='cube') );
        const wpList= paramAry.map( (d) => ({dataType:'image', request:makeWPRequest(wp, d.dataType === 'cube'?undefined:radius, d, plotId, plotGroupId)}));
        const tParamAry= imageMasterData.filter( (d) => imageIdList.includes(d.imageId) && (d.dataType==='table') );
        const tableList= tParamAry.map( (d) => ({dataType:'table', request:makeTableRequest(wp, radius, d, plotId, plotGroupId)}));
        return [...wpList, ...tableList];
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

function resolveHRefVal(valObs, href='') {
    if (!href || !isString(href))  return href;
    const vars = href.match(/\${[\w -.]+}/g);
    if (!vars) return href;
    let replaceHref = href;
    vars.forEach((v) => {
        const [,keyName] = v.match(/\${([\w -.]+)}/) || [];
        if (isDefined(valObs[keyName])) {
            replaceHref = replaceHref.replace(v, valObs[keyName]);
        }
    });
    return replaceHref;
}

function resolveHRefValWithParams(wp,radius, maxRangeDeg, href='') {
    const rNum= Number(radius);
    const sizeArcMin= rNum ? radius*60 : '';
    const sizeArcSec= rNum ? radius*3600 : '';
    const j2Wp= VisUtil.convert(wp, CoordinateSys.EQ_J2000);
    const galWp= VisUtil.convert(wp, CoordinateSys.GALACTIC);

    return resolveHRefVal(
        {
            ra:j2Wp.x,
            dec:j2Wp.y,
            galLon:galWp.x,
            galLat:galWp.y,
            size:radius,
            sizeDeg:radius,
            sizeArcMin,
            sizeArcSec,

            sizeOrMax:radius || maxRangeDeg,
            sizeDegOrMax:radius || maxRangeDeg,
            sizeArcMinOrMax:sizeArcMin || Number(maxRangeDeg) * 60,
            sizeArcSecOrMax:sizeArcSec || Number(maxRangeDeg) * 3600,

            sizeOrZero:radius || 0,
            sizeDegOrZero:radius || 0,
            sizeArcMinOrZero:sizeArcMin || 0,
            sizeArcSecOrZero:sizeArcSec || 0,
        },
        href);
}


function makeWPRequest(wp, radius, data, plotId, plotGroupId) {
    const {plotRequestParams:{URL,...params}, helpUrl, projectTypeDesc, waveType, wavelength, maxRangeDeg}= data;

    params.URL=  URL && resolveHRefValWithParams(wp, radius, maxRangeDeg, URL);
    const inReq= {
        [WPConst.WORLD_PT] : wp.toString(),
        [WPConst.SIZE_IN_DEG] : radius+'',
        attributes: {
            [PlotAttribute.DATA_HELP_URL]: helpUrl,
            [PlotAttribute.PROJ_TYPE_DESC]: projectTypeDesc,
            [PlotAttribute.WAVE_TYPE]: waveType,
            [PlotAttribute.WAVE_LENGTH_UM]: parseFloat(wavelength),
        },
        ...params};

    return addStdParams(WebPlotRequest.makeFromObj(inReq), plotId, plotGroupId);
}

function makeTableRequest(wp, radius, data) {
    const {tableRequestParams:{URL,...params},title }= data;
    const computedURL= resolveHRefValWithParams(wp,radius, URL);
    return makeFileRequest(title,computedURL,undefined,
        {
            META_INFO: {
                [MetaConst.CATALOG_OVERLAY_TYPE]:false,
                ...params
            }
        });
}

function addStdParams(wpreq, plotId = nextPlotId(), plotGroupId = 'multiImageGroup') {
    wpreq.setPlotId(plotId);
    wpreq.setPlotGroupId(plotGroupId);
    wpreq.setGroupLocked(true);
    return wpreq;
}