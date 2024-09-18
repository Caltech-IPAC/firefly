/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Stack, Typography} from '@mui/joy';
import React, {memo, useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {capitalize, isEmpty, isFunction} from 'lodash';
import shallowequal from 'shallowequal';
import {flux} from '../../core/ReduxFlux.js';

import {dispatchNotifyRemoteAppReady, dispatchOnAppReady, dispatchSetMenu} from '../../core/AppDataCntlr.js';
import {dispatchHideDropDown, getLayouInfo, SHOW_DROPDOWN} from '../../core/LayoutCntlr.js';
import {FileDropZone} from '../../visualize/ui/FileUploadViewPanel.jsx';
import {LC, lcManager} from './LcManager.js';
import {LcResult} from './LcResult.jsx';
import {LcPeriodPlotly} from './LcPeriodPlotly.jsx';
import {makeBannerTitle} from '../../ui/Banner.jsx';
import {getActionFromUrl} from '../../core/History.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';
import {FormPanel} from './../../ui/FormPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupUtils, {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {HelpIcon, HelpLink} from './../../ui/HelpIcon.jsx';
import {getAllConverterIds, getConverter, getMissionName} from './LcConverterFactory.js';
import {WorkspaceUpload} from '../../ui/WorkspaceViewer.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {getWorkspaceConfig, initWorkspace} from '../../visualize/WorkspaceCntlr.js';
import {ServerParams} from '../../data/ServerParams.js';
import {useFieldGroupValue, useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {startTTFeatureWatchers} from '../common/ttFeatureWatchers.js';
import {makeSearchOnce, setIf as setIfUndefined} from '../../util/WebUtil.js';
import {upload} from '../../rpc/CoreServices.js';
import App from 'firefly/ui/App.jsx';
import {cloneDeep} from 'lodash/lang.js';


const vFileKey = LC.FG_FILE_FINDER;
const DEFAULT_TITLE = 'Time Series Tool';

/**
 * light curve viewer
 */
export const LcViewer = memo(({menu, dropdownPanels=[], appTitle, slotProps, ...appProps}) => {

    useEffect(() => {
        startTTFeatureWatchers();
        dispatchAddSaga(lcManager);
        getWorkspaceConfig() && initWorkspace();
        dispatchOnAppReady((state) => onReady({state, menu}));
    }, []);

    const fileLocation= useStoreConnector(() => getFieldVal(vFileKey, 'uploadContainer', 'isLocal'));
    const error = useStoreConnector(() => getLayouInfo()?.error);
    const displayMode = useStoreConnector(() => getLayouInfo()?.displayMode);
    const missionEntries = useStoreConnector(() => getLayouInfo()?.missionEntries);

    const periodProps = {
        displayMode,
        timeColName: missionEntries?.[LC.META_TIME_CNAME],
        fluxColName: missionEntries?.[LC.META_FLUX_CNAME]
    };

    const subTitleStr= displayMode?.startsWith('period') ? '(Period Finder)' : '(Viewer)';
    const title = makeBannerTitle(appTitle || DEFAULT_TITLE, subTitleStr);

    const mSlotProps = cloneDeep(slotProps || {});
    setIfUndefined(mSlotProps,'drawer.allowMenuHide', false);
    setIfUndefined(mSlotProps,'banner.title', title);

    return (
        <App slotProps={mSlotProps}
             dropdownPanels={[...dropdownPanels, <UploadPanel {...{fileLocation, name:'LCUpload'}}/>]}
             appTitle={appTitle}  {...appProps}
        >
            <MainView {...{error, displayMode, periodProps}}/>
        </App>
    );
});

/**
 * menu is an array of menu items {label, action, icon, desc, type}.
 * dropdownPanels is an array of additional react elements which are mapped to a menu item's action.
 * @type {{title: *, menu: *, appTitle: *, appIcon: *, additionalTitleStyle: *, dropdownPanels: *, views: *}}
 */
LcViewer.propTypes = {
    title: PropTypes.string,
    menu: PropTypes.arrayOf(PropTypes.object),
    appTitle: PropTypes.string,
    additionalTitleStyle: PropTypes.object,
    appIcon: PropTypes.element,
    footer: PropTypes.element,
    dropdownPanels: PropTypes.arrayOf(PropTypes.element),
    style: PropTypes.object
};


const MainView = ({error, displayMode, periodProps}) => {
    if (error) {
        return (
            <div style={{display: 'flex', width: '100%', marginTop: 20, justifyContent: 'center', alignItems: 'baseline'}}>
                <div style={{display: 'inline-flex', border: '1px solid #a3aeb9', padding:20, fontSize:'150%'}}>
                    <div>{error}</div>
                    <div style={{marginLeft: 10}}>
                        <HelpIcon helpId={'loadingTSV'}/>
                    </div>
                </div>
            </div>
        );
    }
    return displayMode?.startsWith('period') ? <LcPeriodPlotly {...periodProps}/> : <LcResult/>;
};



function onReady({menu}) {
    if (menu) {
        dispatchSetMenu({menuItems: menu});
    }
    const {hasImages, hasTables, hasXyPlots} = getLayouInfo();
    if (!(hasImages || hasTables || hasXyPlots)) {
        const goto = getActionFromUrl() || {type: SHOW_DROPDOWN};
        if (goto) flux.process(goto);
    }
    dispatchNotifyRemoteAppReady();
}



const executeOnce= makeSearchOnce(); // setup options to immediately execute the search the first time
const validateAutoSearch= (initArgs) => !isEmpty(initArgs?.urlApi);

async function callAutoSearch(initArgs) {
    const {cacheKey}= await upload(initArgs?.urlApi?.url);
    onSearchSubmit({ mission: initArgs.urlApi?.dataset, rawTblSource: cacheKey});
}


/**
 *
 * @param oldState
 * @return {{missionOptions: string[], fileLocation: string}}
 */
function getUploadPanelState(oldState) {
    const {missionOptions= getAllConverterIds()} = getLayouInfo();
    const fileLocation = getFieldVal(vFileKey, 'uploadContainer', 'isLocal');
    if (shallowequal(oldState?.missionOptions,missionOptions) && oldState?.fileLocation===fileLocation) return oldState;
    return {missionOptions, fileLocation};
}


export const UploadPanel = ({initArgs,name= 'LCUpload'}) =>{
    const {missionOptions, fileLocation} = useStoreConnector(getUploadPanelState );
    const [,setUploadContainer]= useFieldGroupValue('uploadContainer', vFileKey);
    const externalDropEvent= initArgs?.searchParams?.dropEvent;

    const [dropEvent, setDropEvent] = useState(externalDropEvent);
    useEffect( () => {
        executeOnce( () => validateAutoSearch(initArgs), () => callAutoSearch(initArgs));
    },[initArgs]);

    const wrapperStyle = {color: 'inherit', mx:1/2};
    const instruction = 'Plot time series data, view associated images, find period, and phase fold.';
    const options = missionOptions.map((id) => ({label: getMissionName(id) || capitalize(id), value: id}) );

    const uploadLocationChoice = (
                <RadioGroupInputField {...{
                    fieldKey:'uploadContainer',
                    orientation:'horizontal',
                    initialState:{value: 'isLocal'},
                    options: [
                                {id: 0, label: 'Upload Local File', value: 'isLocal'},
                                {id: 1, label: 'Use Workspace', value: 'isWs'}
                            ]
                }} /> );

    const showFileUploadButton = (setDropEvent,dropEvent) => (
            <Stack {...{minWidth: 450}}>
                <Stack {...{direction:'row', spacing:2}}>
                    <Typography>Upload time series table</Typography>
                    <HelpLink helpId={'loadingTSV'} linkText={'(See requirements)'}/>
                </Stack>
                { fileLocation === 'isLocal' ?
                        <FileUpload fieldKey='rawTblSource'
                                    setDropEvent={setDropEvent} dropEvent={dropEvent} canDragDrop={true}
                            initialState={{ tooltip: 'Select a Time Series Table file to upload',
                                label: ''}}
                        />
                     :
                        <WorkspaceUpload preloadWsFile={false} wrapperStyle={wrapperStyle} fieldKey='rawTblSource'
                            initialState={{ tooltip: 'Select a Time Series Table file from workspace to upload'}}
                        />
                }
            </Stack>
        );

    return (
            <FormPanel groupKey={vFileKey}
                       onSuccess={onSearchSubmit}
                       onCancel={dispatchHideDropDown}
                       help_id={'loadingTSV'}
                       completeText='Upload'>
                <FileDropZone {...{
                    dropEvent, setDropEvent,
                    setLoadingOp: () => {
                        setUploadContainer('isLocal');
                        const newEv = {type: 'drop', dataTransfer: {files: Array.from(dropEvent.dataTransfer.files)}};
                        setDropEvent(newEv);
                    },
                }}>
                    <Stack {...{width:1, alignItems:'center'}}>
                        <Stack {...{ml:0, mt:4, spacing:3}}>
                            <Typography level='body-lg'>{instruction}</Typography>
                            <FieldGroup groupKey={vFileKey} keepState={true}>
                                <Stack {...{spacing:2}}>
                                    {uploadLocationChoice}
                                    {showFileUploadButton(setDropEvent,dropEvent)}
                                    <Stack>
                                        <Typography> Choose mission to view associated images </Typography>
                                        <ListBoxInputField fieldKey='mission' wrapperStyle={wrapperStyle} options={options}
                                                           initialState={{
                                                               value: 'wise',
                                                               tooltip: 'Choose mission to view associated images',
                                                               label : '',
                                                           }} />
                                    </Stack>
                                </Stack>
                            </FieldGroup>
                        </Stack>
                    </Stack>
                </FileDropZone>
            </FormPanel>
    );
};

UploadPanel.propTypes = {
    name: PropTypes.string,
    initArgs: PropTypes.object
};

function onSearchSubmit(request) {
    if (!request?.rawTblSource) return;
    const converter = getConverter(request.mission);
    if (!converter) return;
    const fields = FieldGroupUtils.getGroupFields(vFileKey);
    const displayName = fields.rawTblSource.displayValue && fields.rawTblSource.displayValue.split('\\');
    const uploadedFileName = displayName ? displayName[displayName.length-1] : 'Loaded Table';
    if (isFunction(request.rawTblSource)) {
        request.rawTblSource().then(
            (ul)  => loadFileAndHide(converter,ul.value,uploadedFileName,request.uploadContainer) );
    }
    else {
        loadFileAndHide(converter,request.rawTblSource,uploadedFileName,request.uploadContainer);
    }
    
}

function loadFileAndHide(converter,uploadPath,uploadedFileName, uploadContainer) {
    const treq = converter.rawTableRequest(converter, uploadPath,uploadedFileName);
    if (uploadContainer===ServerParams.IS_WS) treq[ServerParams.SOURCE_FROM]= ServerParams.IS_WS;
    dispatchTableSearch(treq, {removable: true});
    dispatchHideDropDown();
}