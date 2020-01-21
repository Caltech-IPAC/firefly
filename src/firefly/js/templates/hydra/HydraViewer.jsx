/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {pickBy} from 'lodash';

import {flux, firefly} from '../../Firefly.js';
import {dispatchSetMenu, dispatchOnAppReady, getMenu, isAppReady, getSearchInfo} from '../../core/AppDataCntlr.js';
import {getLayouInfo, SHOW_DROPDOWN, LO_VIEW} from '../../core/LayoutCntlr.js';
import {hydraManager} from './HydraManager';
import {Menu} from '../../ui/Menu.jsx';
import {Banner} from '../../ui/Banner.jsx';
import {DropDownContainer} from '../../ui/DropDownContainer.jsx';
import {VisHeader} from '../../visualize/ui/VisHeader.jsx';
import {getActionFromUrl} from '../../core/History.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';

import {ImageExpandedMode} from '../../visualize/iv/ImageExpandedMode.jsx';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {warningDivId} from '../../ui/LostConnection';
import {startTTFeatureWatchers} from '../common/ttFeatureWatchers.js';
import {dispatchSetLayoutMode, LO_MODE} from '../../core/LayoutCntlr';


export {META_VIEWER_ID as IMAGE_DATA_VIEWER_ID } from '../../visualize/MultiViewCntlr';

/**
 * This is a viewer.
 */
export class HydraViewer extends PureComponent {

    constructor(props) {
        super(props);
        this.state = this.getNextState();

        dispatchAddSaga(hydraManager);
        startTTFeatureWatchers();
    }

    getNextState() {
        const menu = getMenu();
        const layoutInfo = getLayouInfo();
        const searchInfo = getSearchInfo();
        const isReady = isAppReady();

        return Object.assign({}, this.props,
            {menu, isReady, searchInfo, layoutInfo});
    }

    componentDidMount() {
        dispatchOnAppReady((state) => {
            onReady({state, menu: this.props.menu});
        });
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.isUnmounted = true;
        this.removeListener && this.removeListener();
    }

    storeUpdate() {
        if (!this.isUnmounted) {
            this.setState(this.getNextState());
        }
    }

    render() {
        const {appTitle, appIcon, altAppIcon, footer, dropdownPanels=[], style} = this.props;
        const {menu, layoutInfo={}} = this.state;
        const {dropDown} = layoutInfo;
        const {visible, view} = dropDown || {};

        return (
            <div id='App' className='rootStyle' style={style}>
                <header>
                    <BannerSection {...{menu, appTitle, appIcon, altAppIcon}}/>
                    <div id={warningDivId} data-decor='full' className='warning-div center'/>
                    <DropDownContainer
                        key='dropdown'
                        footer={footer}
                        visible={!!visible}
                        selected={view}
                        {...{dropdownPanels} } />
                </header>
                <main style={{position: 'relative', padding: 0}}>
                    <div style={{display: 'flex', position: 'relative', flexGrow: 1}}>
                        <ResultSection layoutInfo={layoutInfo}/>
                    </div>
                </main>
            </div>
        );
    }
}

/**
 * menu is an array of menu items {label, action, icon, desc, type}.
 * dropdownPanels is an array of additional react elements which are mapped to a menu item's action.
 * @type {{title: *, menu: *, appTitle: *, appIcon: *, altAppIcon: *, dropdownPanels: *, views: *}}
 */
HydraViewer.propTypes = {
    title: PropTypes.string,
    menu: PropTypes.arrayOf(PropTypes.object),
    appTitle: PropTypes.string,
    appIcon: PropTypes.string,
    altAppIcon: PropTypes.string,
    footer: PropTypes.element,
    dropdownPanels: PropTypes.arrayOf(PropTypes.element),
    style: PropTypes.object,
    hasCoverage: PropTypes.bool,
    hasDataProduct: PropTypes.bool
};

HydraViewer.defaultProps = {
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

function ResultSection({layoutInfo}) {
    const {currentSearch, images} = layoutInfo;
    const {expanded=LO_VIEW.none} = layoutInfo.mode || {};

    const {allSearchItems} = getSearchInfo();
    if (!allSearchItems) return <div/>;
    const {renderStandardView:StandardView, renderExpandedView} = allSearchItems[currentSearch] || {};
    const ExpandedImpl= renderExpandedView || showExpandedView;
    const standard = StandardView ? <StandardView {...{layoutInfo}}/> : <div/>;

    return expanded === LO_VIEW.none ? standard : <ExpandedImpl {...{layoutInfo, expanded, images}}/>;
}

function showExpandedView ({expanded}) {

    let view;
    if (expanded === LO_VIEW.tables) {
        view = (<TablesContainer mode='both'
                         closeable={true}
                         expandedMode={expanded===LO_VIEW.tables}
                         tableOptions={{help_id:'main1TSV.table'}}/>
                );
    } else if (expanded === LO_VIEW.xyPlots) {
        view = (<ChartsContainer closeable={true}
                                 tbl_group='main'
                                 addDefaultChart={true}
                         expandedMode={expanded===LO_VIEW.xyPlots}/>
                );
    } else {
        view = (
            <ImageExpandedMode
                key='results-plots-expanded'
                closeFunc={closeExpanded}/>
        );
    }
    return (
        <div style={{ display: 'flex', flexGrow: 1, overflow: 'hidden'}}>
            {view}
        </div>
    );
}

function closeExpanded() {
    dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
}
