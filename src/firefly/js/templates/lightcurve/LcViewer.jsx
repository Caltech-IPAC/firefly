/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useEffect} from 'react';
import PropTypes from 'prop-types';
import {isEmpty, capitalize} from 'lodash';
import shallowequal from 'shallowequal';
import {flux} from '../../core/ReduxFlux.js';
import {getMenu, isAppReady, dispatchSetMenu,
    dispatchOnAppReady, dispatchNotifyRemoteAppReady} from '../../core/AppDataCntlr.js';
import {dispatchHideDropDown, getLayouInfo, SHOW_DROPDOWN} from '../../core/LayoutCntlr.js';
import {lcManager, LC} from './LcManager.js';
import {LcResult} from './LcResult.jsx';
import {LcPeriodPlotly} from './LcPeriodPlotly.jsx';
import {Menu} from '../../ui/Menu.jsx';
import {Banner} from '../../ui/Banner.jsx';
import {DropDownContainer} from '../../ui/DropDownContainer.jsx';
import {getActionFromUrl} from '../../core/History.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';
import {FormPanel} from './../../ui/FormPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {HelpIcon} from './../../ui/HelpIcon.jsx';
import {getAllConverterIds, getConverter, getMissionName, DL_DATA_TAG} from './LcConverterFactory.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {HelpText} from './../../ui/HelpText.jsx';
import {WorkspaceUpload} from '../../ui/WorkspaceViewer.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {getWorkspaceConfig, initWorkspace} from '../../visualize/WorkspaceCntlr.js';
import {ServerParams} from '../../data/ServerParams.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {warningDivId} from '../../ui/LostConnection';
import {startTTFeatureWatchers} from '../common/ttFeatureWatchers.js';
import {makeSearchOnce} from '../../util/WebUtil.js';
import {upload} from '../../rpc/CoreServices.js';


const vFileKey = LC.FG_FILE_FINDER;
const DEFAULT_TITLE = 'Time Series Tool';
const topStyle = {fontSize:'18pt', display:'flex', flexDirection:'column', alignItems:'flex-end'};
const subStyle = {fontSize: '11pt', fontWeight: 100};

/**
 * light curve viewer
 */
export const LcViewer = memo((props) => {

    useEffect(() => {
        startTTFeatureWatchers();
        dispatchAddSaga(lcManager);
        getWorkspaceConfig() && initWorkspace();
        dispatchOnAppReady((state) => onReady({state, menu:props.menu}));
    }, []);

    const [storeState]= useStoreConnector((oldState={}) => {
        const fileLocation = getFieldVal(vFileKey, 'uploadContainer', 'isLocal');
        const newState= {fileLocation, menu:getMenu(), isReady:isAppReady(), ...getLayouInfo()};
        return shallowequal(oldState,newState) ? oldState : newState;
    });

    const {isReady, menu={}, appTitle, altAppIcon, dropDown,
        dropdownPanels=[], footer, style, displayMode, missionEntries, fileLocation, error} = storeState;
    const {appIcon, additionalTitleStyle= {}, bannerLeftStyle, bannerMiddleStyle} = props;
    const additionalTitleStylePlus= {marginRight: 20, alignSelf: 'flex-start',...additionalTitleStyle}
    const bannerMiddleStylePlus= {flexDirection: 'row', ...bannerMiddleStyle};

    if (!isReady) return (<div style={{top: 0}} className='loading-mask'/>);

    const periodProps = {
        displayMode,
        timeColName: missionEntries?.[LC.META_TIME_CNAME],
        fluxColName: missionEntries?.[LC.META_FLUX_CNAME]
    };

    const MainView = () => {
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

    const subTitleStr= displayMode ? (displayMode.startsWith('period')) ? '(Period Finder)' : '(Viewer)' : '';
    const title = (
        <div style={topStyle}>
            <div>{appTitle || DEFAULT_TITLE}</div>
            <div style={subStyle}>{subTitleStr}</div>
        </div>);

    const {visible, view, initArgs} = dropDown || {};
    return (
        <div id='App' className='rootStyle' style={style}>
            <header>
                <BannerSection {...{menu, appTitle : title, appIcon, altAppIcon,
                    additionalTitleStyle:additionalTitleStylePlus, bannerLeftStyle,
                    bannerMiddleStyle:bannerMiddleStylePlus, showTitleOnBanner:true}}/>
                <div id={warningDivId} data-decor='full' className='warning-div center'/>
                <DropDownContainer key='dropdown' footer={footer} visible={!!visible}
                                   selected={view} initArgs={initArgs}
                                   dropdownPanels={[...dropdownPanels, <UploadPanel {...{fileLocation}}/>]} />
            </header>
            <main>
                <MainView/>
            </main>
        </div>
    );
});

/**
 * menu is an array of menu items {label, action, icon, desc, type}.
 * dropdownPanels is an array of additional react elements which are mapped to a menu item's action.
 * @type {{title: *, menu: *, appTitle: *, appIcon: *, altAppIcon: *, additionalTitleStyle: *, dropdownPanels: *, views: *}}
 */
LcViewer.propTypes = {
    title: PropTypes.string,
    menu: PropTypes.arrayOf(PropTypes.object),
    appTitle: PropTypes.string,
    additionalTitleStyle: PropTypes.object,
    appIcon: PropTypes.string,
    altAppIcon: PropTypes.string,
    footer: PropTypes.element,
    dropdownPanels: PropTypes.arrayOf(PropTypes.element),
    style: PropTypes.object
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


const BannerSection= ({menu,...rest}) => (
        <Banner key='banner' menu={<Menu menu={menu} /> }
            {...rest}
        />
    );

const labelW = 150;

const executeOnce= makeSearchOnce(); // setup options to immediately execute the search the first time
const validateAutoSearch= (initArgs) => !isEmpty(initArgs);

async function callAutoSearch(initArgs) {
    const {cacheKey}= await upload(initArgs.url);
    onSearchSubmit({ mission: initArgs.dataset, rawTblSource: cacheKey});
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


export const UploadPanel = ({initArgs}) =>{
    const [{missionOptions,fileLocation}]= useStoreConnector(getUploadPanelState );

    useEffect( () => {
        executeOnce( () => validateAutoSearch(initArgs), () => callAutoSearch(initArgs));
    },[initArgs]);

    const wrapperStyle = {color: 'inherit', margin: '5px 0'};
    const instruction = 'Plot time series data, view associated images, find period, and phase fold.';
    const options = missionOptions.map((id) => ({label: getMissionName(id) || capitalize(id), value: id}) );

    const showUploadLocation = () => {
        const options = [
            {'id': 0, label: 'Local File', 'value': 'isLocal'},
            {'id': 1, label: 'Workspace', 'value': 'isWs'}
        ];
        return (
            <div>
                <RadioGroupInputField
                    fieldKey={'uploadContainer'}
                    initialState={{value: options[0].value, label: 'Choose Upload from:',
                        labelWidth: (labelW-4)}}
                    alignment={'horizontal'}
                    options={options}
                />
            </div>
        );
    };

    const showFileUploadButton = () => (
            <div style={{padding:5, display:'flex', alignItems:'center', minWidth: 450, height: 50}}>
                <div style={{display:'flex', flexDirection:'column', width: labelW}}>
                    <div> {'Upload time series table:'} </div>
                    <HelpText helpId={'loadingTSV'} linkText={'(See requirements)'}/>
                </div>
                { fileLocation === 'isLocal' ?
                        <FileUpload wrapperStyle={wrapperStyle} fieldKey='rawTblSource'
                            initialState={{ tooltip: 'Select a Time Series Table file to upload',
                                label: ''}}
                        />
                     :
                        <WorkspaceUpload preloadWsFile={false} wrapperStyle={wrapperStyle} fieldKey='rawTblSource'
                            initialState={{ tooltip: 'Select a Time Series Table file from workspace to upload'}}
                        />
                }
            </div>
        );

    return (
        <div style={{padding: 10, marginBottom:15}}>
            <div style={{margin: '0px 5px 5px'}}>{instruction}</div>
            <FormPanel
                groupKey={vFileKey} onSubmit={(request) => onSearchSubmit(request)} onCancel={dispatchHideDropDown}
                submitText={'Upload'} help_id={'loadingTSV'}>
                <FieldGroup groupKey={vFileKey} keepState={true}>
                    <div style={{padding:5 }}>
                        <div style={{padding:5 }}>
                            {showUploadLocation()}
                        </div>
                        {showFileUploadButton()}
                        <div style={{padding:5,display:'flex', alignItems:'center' }}>
                            <div style={{display:'flex', flexDirection:'column', width: labelW}}>
                                <div>{'Choose mission'}</div>
                                <div>{'to view associated images:'}</div>
                            </div>
                            <ListBoxInputField fieldKey='mission' wrapperStyle={wrapperStyle} options={options}
                                               initialState={{
                                                   value: 'wise',
                                                   tooltip: 'Choose mission to view associated images',
                                                   label : '',
                                                   labelWidth : 0
                                               }} />
                        </div>
                    </div>
                </FieldGroup>
            </FormPanel>
        </div>
    );
};


UploadPanel.propTypes = {
    name: PropTypes.oneOf(['LCUpload']),
    initArgs: PropTypes.object
};

UploadPanel.defaultProps = {
    name: 'LCUpload'
};

function onSearchSubmit(request) {
    if (!request?.rawTblSource) return;
    const converter = getConverter(request.mission);
    if (!converter) return;
    const fields = FieldGroupUtils.getGroupFields(vFileKey);
    const displayName = fields.rawTblSource.displayValue && fields.rawTblSource.displayValue.split('\\');
    const uploadedFileName = displayName ? displayName[displayName.length-1] : 'Loaded Table';
    const treq = converter.rawTableRequest(converter, request.rawTblSource,uploadedFileName);

    if (request.uploadContainer===ServerParams.IS_WS) treq[ServerParams.SOURCE_FROM]= ServerParams.IS_WS;
    dispatchTableSearch(treq, {removable: true});
    dispatchHideDropDown();
}