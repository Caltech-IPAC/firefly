/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {pickBy, get, capitalize} from 'lodash';
import {flux, firefly} from '../../Firefly.js';
import {getMenu, isAppReady, dispatchSetMenu, dispatchOnAppReady} from '../../core/AppDataCntlr.js';
import {dispatchHideDropDown, getLayouInfo, SHOW_DROPDOWN} from '../../core/LayoutCntlr.js';
import {lcManager, LC} from './LcManager.js';
import {LcResult} from './LcResult.jsx';
import {LcPeriodPlotly} from './LcPeriodPlotly.jsx';
import {Menu} from '../../ui/Menu.jsx';
import {Banner} from '../../ui/Banner.jsx';
import {DropDownContainer} from '../../ui/DropDownContainer.jsx';
import {VisHeader} from '../../visualize/ui/VisHeader.jsx';
import {getActionFromUrl} from '../../core/History.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';
import {FormPanel} from './../../ui/FormPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {watchCatalogs} from '../../visualize/saga/CatalogWatcher.js';
import {HelpIcon} from './../../ui/HelpIcon.jsx';
import {getAllConverterIds, getConverter, getMissionName, DL_DATA_TAG} from './LcConverterFactory.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {HelpText} from './../../ui/HelpText.jsx';
import {dispatchAllowDataTag} from '../../core/background/BackgroundCntlr.js';
import {WorkspaceUpload} from '../../ui/WorkspaceViewer.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {getWorkspaceConfig, initWorkspace} from '../../visualize/WorkspaceCntlr.js';
import {ServerParams} from '../../data/ServerParams.js';
import {SimpleComponent} from '../../ui/SimpleComponent.jsx';
import {warningDivId} from '../../ui/LostConnection';


const vFileKey = LC.FG_FILE_FINDER;
const DEFAULT_TITLE = 'Time Series Tool';
/**
 * This is a light curve viewer.
 */
export class LcViewer extends PureComponent {

    constructor(props) {
        super(props);
        this.state = this.getNextState();
        dispatchAddSaga(watchCatalogs);
        dispatchAddSaga(lcManager);
        if (getWorkspaceConfig()) { initWorkspace();}
    }

    getNextState() {
        const menu = getMenu();
        const layoutInfo = getLayouInfo();
        const isReady = isAppReady();
        const fileLocation = getFieldVal(vFileKey, 'uploadContainer', 'isLocal');

        return Object.assign({}, this.props,
            {fileLocation, menu, isReady, ...layoutInfo});
    }

    componentDidMount() {
        this.iAmMounted = true;
        dispatchOnAppReady((state) => {
            onReady({state, menu: this.props.menu});
        });
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        this.removeListener && this.removeListener();
    }

    storeUpdate() {
        if (this.iAmMounted) {
            this.setState(this.getNextState());
        }
    }

    render() {
        var {isReady, menu={}, appTitle, appIcon, altAppIcon, additionalTitleStyle, dropDown,
            dropdownPanels=[], footer, style, displayMode, missionEntries, fileLocation, error} = this.state;
        const {visible, view} = dropDown || {};
        const periodProps = {
            displayMode, timeColName: get(missionEntries, [LC.META_TIME_CNAME]),
            fluxColName: get(missionEntries, [LC.META_FLUX_CNAME])
        };

        dropdownPanels.push(<UploadPanel {...{fileLocation}}/>);

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
            } else {
                if (displayMode && displayMode.startsWith('period')) {
                    return (
                        <LcPeriodPlotly {...periodProps}/>
                    );
                } else {
                    return (
                        <LcResult/>
                    );
                }
            }
        };

        let title = appTitle ? appTitle : DEFAULT_TITLE; // use default title when appTitle is undefined or ''
        if (displayMode && displayMode.startsWith('period')) {
            title += ': Period Finder';

        } else if(displayMode && !displayMode.startsWith('period')){
            title += ': Viewer';
        }
        if (!isReady) {
            return (<div style={{top: 0}} className='loading-mask'/>);
        } else {

            return (
                <div id='App' className='rootStyle' style={style}>
                    <header>
                        <BannerSection {...{menu, appTitle : title, appIcon, altAppIcon, additionalTitleStyle}}/>
                        <div id={warningDivId} data-decor='full' className='warning-div--center'/>
                        <DropDownContainer
                            key='dropdown'
                            footer={footer}
                            visible={!!visible}
                            selected={view}
                            {...{dropdownPanels} } />
                    </header>
                    <main>
                        <MainView/>
                    </main>
                </div>
            );
        }
    }
}

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
        if (goto) firefly.process(goto);
    }
    dispatchAllowDataTag([DL_DATA_TAG]);
}


function BannerSection(props) {
    const {menu, ...rest} = pickBy(props);
    return (
        <Banner key='banner'
                menu={<Menu menu={menu} /> }
                visPreview={<VisHeader showHeader={false}/> }
                readout={<VisHeader showPreview={false}/> }
            {...rest}
        />
    );
}

BannerSection.propTypes = {
    props: PropTypes.object
};

const labelW = 150;

/**
 *  A generic upload panel.
 * @param {Object} props react component's props
 */

export class UploadPanel extends SimpleComponent {
    getNextState(np) {
        const {missionOptions} = getLayouInfo();
        const fileLocation = getFieldVal(vFileKey, 'uploadContainer', 'isLocal');
        return {missionOptions, fileLocation};
    }

    render() {
        const wrapperStyle = {color: 'inherit', margin: '5px 0'};
        const {fileLocation, missionOptions=getAllConverterIds()} = this.state || {};

        const instruction = 'Plot time series data, view associated images, find period, and phase fold.';

        const options = missionOptions.map((id) => {
            return {label: getMissionName(id) || capitalize(id), value: id};
        });

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

        const showFileUploadButton = () => {
            return (
                <div style={{padding:5, display:'flex', alignItems:'center', minWidth: 450, height: 50}}>
                    <div style={{display:'flex', flexDirection:'column', width: labelW}}>
                        <div> {'Upload time series table:'} </div>
                        <HelpText helpId={'loadingTSV'} linkText={'(See requirements)'}/>
                    </div>
                    { fileLocation === 'isLocal' ? (
                            <FileUpload
                                wrapperStyle={wrapperStyle}
                                fieldKey='rawTblSource'
                                initialState={{ tooltip: 'Select a Time Series Table file to upload',
                                                label: ''}}
                            />
                        ) :
                        (
                            <WorkspaceUpload
                                preloadWsFile={false}
                                wrapperStyle={wrapperStyle}
                                fieldKey='rawTblSource'
                                initialState={{ tooltip: 'Select a Time Series Table file from workspace to upload'}}
                            />
                        )
                    }
                </div>
            );
        };

        return (
            <div style={{padding: 10}}>
                <div style={{margin: '0px 5px 5px'}}>{instruction}</div>
                <FormPanel
                    groupKey={vFileKey}
                    onSubmit={(request) => onSearchSubmit(request)}
                    onCancel={dispatchHideDropDown}
                    submitText={'Upload'}
                    help_id={'loadingTSV'}>
                    <FieldGroup groupKey={vFileKey} validatorFunc={null} keepState={true}>
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
                                <ListBoxInputField fieldKey='mission'
                                                   wrapperStyle={wrapperStyle}
                                                   initialState={{
                                                        value: 'wise',
                                                        tooltip: 'Choose mission to view associated images',
                                                        label : '',
                                                        labelWidth : 0
                                                    }}
                                                   options={options}
                                />
                            </div>
                        </div>
                    </FieldGroup>
                </FormPanel>
            </div>
        );
    }
}


UploadPanel.propTypes = {
    name: PropTypes.oneOf(['LCUpload']),
};

UploadPanel.defaultProps = {
    name: 'LCUpload'
};

function onSearchSubmit(request) {
    if (request.rawTblSource) {
        const converter = getConverter(request.mission);
        if (!converter) return;

        var fields = FieldGroupUtils.getGroupFields(vFileKey);
        const displayName = fields.rawTblSource.displayValue.split('\\');
        const uploadedFileName = displayName[displayName.length-1];
        const treq = converter.rawTableRequest(converter, request.rawTblSource,uploadedFileName);

        if (request.uploadContainer===ServerParams.IS_WS) treq[ServerParams.SOURCE_FROM]= ServerParams.IS_WS;


        dispatchTableSearch(treq, {removable: true});
        dispatchHideDropDown();
    }
}


