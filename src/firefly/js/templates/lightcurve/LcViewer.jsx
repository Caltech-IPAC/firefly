/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {get, pickBy} from 'lodash';

import {flux, firefly} from '../../Firefly.js';
import {getMenu, isAppReady, dispatchSetMenu, dispatchOnAppReady} from '../../core/AppDataCntlr.js';
import {LO_VIEW, getLayouInfo, SHOW_DROPDOWN} from '../../core/LayoutCntlr.js';
import {lcManager, PERIODOGRAM, PHASE_FOLDED, RAW_TABLE, PEAK_TABLE} from './LcManager.js';
import {listenerPanel} from './LcPhaseFoldingPanel.jsx';
import {LcResult} from './LcResult.jsx';
import {Menu} from '../../ui/Menu.jsx';
import {Banner} from '../../ui/Banner.jsx';
import {DropDownContainer} from '../../ui/DropDownContainer.jsx';
import {VisHeader} from '../../visualize/ui/VisHeader.jsx';
import {getActionFromUrl} from '../../core/History.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';

import {FormPanel} from './../../ui/FormPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {dispatchInitFieldGroup} from '../../fieldGroup/FieldGroupCntlr.js';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {loadXYPlot} from '../../charts/dataTypes/XYColsCDT.js';
import {syncChartViewer} from '../../visualize/saga/ChartsSync.js';
import * as TblUtil from '../../tables/TableUtil.js';

// import {deepDiff} from '../util/WebUtil.js';

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
        this.removeListener && this.removeListener();
    }

    storeUpdate() {
        this.setState(this.getNextState());
    }

    render() {
        var {isReady, menu={}, appTitle, appIcon, altAppIcon, dropDown,
                dropdownPanels=[], views, footer, style} = this.state;
        const {visible, view} = dropDown || {};
        
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
                        <LcResult/>
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
    appTitle: 'Light Curve'
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


/**
 *  A temporary upload panel for use during development phase.  This should be removed or replaced with something else.
 */
export const UploadPanel = () => {

    return (
        <div style={{padding: 10}}>
            <FormPanel
                width='640px' height='200px'
                groupKey='LC_FORM'
                onSubmit={(request) => onSearchSubmit(request)}
                onCancel={dispatchHideDropDown}>
                <FieldGroup groupKey='LC_FORM' validatorFunc={null} keepState={true}>
                    <FileUpload
                        wrapperStyle = {{margin: '5px 0'}}
                        fieldKey = {RAW_TABLE}
                        groupKey='LC_FORM'
                        initialState= {{
                                tooltip: 'Select a file to upload',
                                label: 'Raw Table:'
                            }}
                    />
                    <FileUpload
                        wrapperStyle = {{margin: '5px 0'}}
                        fieldKey = {PEAK_TABLE}
                        groupKey='LC_FORM'
                        initialState= {{
                                tooltip: 'Select a file to upload',
                                label: 'Peak Table:'
                            }}
                    />
                    <FileUpload
                        wrapperStyle = {{margin: '5px 0'}}
                        fieldKey = {PHASE_FOLDED}
                        groupKey='LC_FORM'
                        initialState= {{
                                tooltip: 'Select a file to upload',
                                label: 'Phase Folded:'
                            }}
                    />
                    <FileUpload
                        wrapperStyle = {{margin: '5px 0'}}
                        fieldKey = {PERIODOGRAM}
                        groupKey='LC_FORM'
                        initialState= {{
                                tooltip: 'Select a file to upload',
                                label: 'Periodogram:'
                            }}
                    />

                </FieldGroup>
            </FormPanel>
        </div>
    );
};
UploadPanel.propTypes = {
    name: PropTypes.oneOf(['LCUpload'])
};

UploadPanel.defaultProps = {
    name: 'LCUpload'
};


function onSearchSubmit(request) {
    var treq, xyPlotParams;
    if ( get(request, RAW_TABLE) ){
        treq = TblUtil.makeFileRequest('Raw Table', request[RAW_TABLE], null, {tbl_id:RAW_TABLE});
        treq.tblType='notACatalog';
        xyPlotParams = {x: {columnOrExpr: 'mjd'}, y: {columnOrExpr: 'w1mpro_ep', options:'grid,flip'}};
    } else if ( get(request, PHASE_FOLDED) ) {
        treq = TblUtil.makeFileRequest('Phase Folded', request[PHASE_FOLDED], null, {tbl_id:PHASE_FOLDED});
        treq.tblType='notACatalog';
        xyPlotParams = {x: {columnOrExpr: 'phase'}, y: {columnOrExpr: 'w1mpro_ep',  options:'grid,flip'}};
    } else if ( get(request, PERIODOGRAM) ) {
        treq = TblUtil.makeFileRequest('Periodogram', request[PERIODOGRAM], null, {tbl_id:PERIODOGRAM});
        treq.tblType='notACatalog';
        xyPlotParams = {x: {columnOrExpr: 'PERIOD', options: 'log'}, y: {columnOrExpr: 'POWER'}};
    } else if ( get(request, PEAK_TABLE) ) {
        treq = TblUtil.makeFileRequest('Peak Table', request[PEAK_TABLE], null, {tbl_id:PEAK_TABLE});
        treq.tblType='notACatalog';
        xyPlotParams = {x: {columnOrExpr: 'Period', options: 'log'}, y: {columnOrExpr: 'Power'}};
    }
    if (treq !== null) {
        dispatchTableSearch(treq, {removable: false});
        dispatchHideDropDown();
        dispatchInitFieldGroup('LC_FORM');
    }
    if (xyPlotParams) {
        loadXYPlot({chartId:treq.tbl_id, tblId:treq.tbl_id, xyPlotParams});
    }
}
