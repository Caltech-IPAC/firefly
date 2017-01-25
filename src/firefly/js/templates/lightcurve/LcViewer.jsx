/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {pickBy, get} from 'lodash';
import {flux, firefly} from '../../Firefly.js';
import {getMenu, isAppReady, dispatchSetMenu, dispatchOnAppReady} from '../../core/AppDataCntlr.js';
import {getLayouInfo, SHOW_DROPDOWN} from '../../core/LayoutCntlr.js';
import {lcManager, LC, removeTablesFromGroup} from './LcManager.js';
import {listenerPanel} from './LcPhaseFoldingPanel.jsx';
import {LcResult} from './LcResult.jsx';
import {LcPeriod} from './LcPeriod.jsx';
import {Menu} from '../../ui/Menu.jsx';
import {Banner} from '../../ui/Banner.jsx';
import {DropDownContainer} from '../../ui/DropDownContainer.jsx';
import {VisHeader} from '../../visualize/ui/VisHeader.jsx';
import {getActionFromUrl} from '../../core/History.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';
import {FormPanel} from './../../ui/FormPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {loadXYPlot} from '../../charts/dataTypes/XYColsCDT.js';
import {syncChartViewer} from '../../visualize/saga/ChartsSync.js';
import {makeFileRequest} from '../../tables/TableUtil.js';
import {sortInfoString} from '../../tables/SortInfo.js';


const vFileKey = LC.FG_FILE_FINDER;
/**
 * This is a light curve viewer.
 */
export class LcViewer extends Component {

    constructor(props) {
        super(props);
        this.state = this.getNextState();
        dispatchAddSaga(lcManager);
        dispatchAddSaga(listenerPanel);
        dispatchAddSaga(syncChartViewer);
    }

    getNextState() {
        const menu = getMenu();
        const layoutInfo = getLayouInfo();
        const isReady = isAppReady();

        return Object.assign({}, this.props,
            {menu, isReady, ...layoutInfo});
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    componentDidMount() {
        this.iAmMounted = true;
        dispatchOnAppReady((state) => {
            onReady({state, menu: this.props.menu});
        });
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }

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
        var {isReady, menu={}, appTitle, appIcon, altAppIcon, dropDown,
                dropdownPanels=[], footer, style, displayMode, missionEntries} = this.state;
        const {visible, view} = dropDown || {};
        const periodProps = {displayMode, timeColName: get(missionEntries, [LC.META_TIME_CNAME]),
                                          fluxColName: get(missionEntries, [LC.META_FLUX_CNAME]),
                                          validTimeColumns: get(missionEntries, [LC.META_TIME_NAMES]),
                                          validFluxColumns: get(missionEntries, [LC.META_FLUX_NAMES])};

        dropdownPanels.push(<UploadPanel/>);

        if (!isReady) {
            return (<div style={{top: 0}} className='loading-mask'/>);
        } else {
            return (
                <div id='App' className='rootStyle' style={style}>
                    <header>
                        <BannerSection {...{menu, appTitle, appIcon, altAppIcon}}/>
                        <DropDownContainer
                            key='dropdown'
                            footer={footer}
                            visible={!!visible}
                            selected={view}
                            {...{dropdownPanels} } />
                    </header>
                    <main>
                        {displayMode&&displayMode.startsWith('period') ? <LcPeriod {...periodProps}/> : <LcResult/>}
                    </main>
                </div>
            );
        }
    }
}

/**
 * menu is an array of menu items {label, action, icon, desc, type}.
 * dropdownPanels is an array of additional react elements which are mapped to a menu item's action.
 * @type {{title: *, menu: *, appTitle: *, appIcon: *, altAppIcon: *, dropdownPanels: *, views: *}}
 */
LcViewer.propTypes = {
    title: PropTypes.string,
    menu: PropTypes.arrayOf(PropTypes.object),
    appTitle: PropTypes.string,
    appIcon: PropTypes.string,
    altAppIcon: PropTypes.string,
    footer: PropTypes.element,
    dropdownPanels: PropTypes.arrayOf(PropTypes.element),
    style: PropTypes.object
};

LcViewer.defaultProps = {
    appTitle: 'Time Series Viewer'
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


/**
 *  A generic upload panel.
 * @param {Object} props react component's props
 */
export function UploadPanel(props) {
    const wrapperStyle = {margin: '5px 0'};
    return (
        <div style={{padding: 10}}>
            <FormPanel
                groupKey={vFileKey}
                onSubmit={(request) => onSearchSubmit(request)}
                onCancel={dispatchHideDropDown}>
                <FieldGroup groupKey={vFileKey} validatorFunc={null} keepState={true}>
                    <FileUpload
                        wrapperStyle = {wrapperStyle}
                        fieldKey = 'rawTblSource'
                        initialState= {{
                                tooltip: 'Select a Light Curves Table file to upload',
                                label: 'Raw Light Curves Table:'
                            }}
                    />
                    <ValidationField fieldKey={LC.META_TIME_CNAME}
                                     wrapperStyle = {wrapperStyle}
                                     placeholder = 'mjd'
                                     initialState= {{
                                          tooltip: 'Enter the name of the time column',
                                          label : 'Time Column Name:',
                                          labelWidth : 120
                                      }} />
                    <ValidationField fieldKey={LC.META_FLUX_CNAME}
                                     wrapperStyle = {wrapperStyle}
                                     placeholder = 'w1mpro_ep'
                                     initialState= {{
                                          tooltip: 'Enter the name of the flux column',
                                          label : 'Flux Column Name:',
                                          labelWidth : 120
                                      }} />

                </FieldGroup>
            </FormPanel>
        </div>
    );
}

UploadPanel.propTypes = {
    name: PropTypes.oneOf(['LCUpload'])
};

UploadPanel.defaultProps = {
    name: 'LCUpload'
};

function onSearchSubmit(request) {
    if ( request.rawTblSource ){
        removeTablesFromGroup();
        removeTablesFromGroup(LC.PERIODOGRAM_GROUP);

        const {timeCName= LC.DEF_TIME_CNAME, fluxCName= LC.DEF_FLUX_CNAME} = request;
        const options = {
            tbl_id: LC.RAW_TABLE,
            tblType: 'notACatalog',
            sortInfo: sortInfoString(timeCName),
            META_INFO: {timeCName, fluxCName}
        };
        const treq = makeFileRequest('Raw Table', request.rawTblSource, null, options);
        dispatchTableSearch(treq, {removable: true});

        const xyPlotParams = {x: {columnOrExpr: timeCName}, y: {columnOrExpr: fluxCName, options:'grid,flip'}};
        loadXYPlot({chartId:treq.tbl_id, tblId:treq.tbl_id, xyPlotParams});
        dispatchHideDropDown();
    } 
}
