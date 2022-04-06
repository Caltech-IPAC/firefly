/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isEmpty} from 'lodash';
import React, {memo, useEffect} from 'react';
import PropTypes from 'prop-types';
import shallowequal from 'shallowequal';
import {flux} from '../../core/ReduxFlux.js';
import {getActionFromUrl} from '../../core/History.js';
import {getMenu, isAppReady, dispatchSetMenu,
    dispatchOnAppReady, dispatchNotifyRemoteAppReady} from '../../core/AppDataCntlr.js';
import {getLayouInfo, dispatchSetLayoutMode, getGridView, getGridViewColumns,
            LO_MODE, LO_VIEW} from '../../core/LayoutCntlr.js';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {getExpandedChartProps} from '../../charts/ChartsCntlr.js';
import {visRoot} from '../../visualize/ImagePlotCntlr.js';
import {getMultiViewRoot, findViewerWithItemId, PLOT2D} from '../../visualize/MultiViewCntlr.js';
import {ImageExpandedMode} from '../../visualize/iv/ImageExpandedMode.jsx';
import {startTTFeatureWatchers} from '../common/ttFeatureWatchers.js';
import {Menu} from '../../ui/Menu.jsx';
import {Banner} from '../../ui/Banner.jsx';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {warningDivId} from '../../ui/LostConnection';
import {AppInitLoadingMessage} from '../../ui/AppInitLoadingMessage.jsx';
import {DropDownContainer} from '../../ui/DropDownContainer.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {startLayoutManager} from './FireflySlateManager.js';
import {GridLayoutPanel} from './GridLayoutPanel.jsx';

import FFTOOLS_ICO from 'html/images/fftools-logo-offset-small-75x75.png';


function getNextState(prevS, renderTreeId) {
    const layoutInfo= getLayouInfo();
    const menu = getMenu();
    const gridView= getGridView(layoutInfo, renderTreeId);
    const gridColumns= getGridViewColumns(layoutInfo, renderTreeId);
    const isReady = isAppReady();
    return (prevS &&
        shallowequal(prevS.layoutInfo,layoutInfo) &&
        menu===prevS.menu &&
        gridView===prevS.gridView &&
        gridColumns===prevS.gridColumns &&
        isReady===prevS.isReady ) ? prevS : { menu, ...layoutInfo, gridView, gridColumns, isReady};
}

/**
 * This FireflySlate is a generic layout application with some configurable behaviors.
 * The application is separated into these major parts:  banner, menu, searches, and results.
 * The props below allow you to alter their default behaviors.
 *
 * <b>Props</b>
 * <li><b>title</b>:  This title will appears at center top of the results area. Defaults to 'FFTools'. </li>
 * <li><b>menu</b>:  menu is an array of menu items {label, action, icon, desc, type}.  Leave type blank for dropdown.
 * If type='COMMAND', it will fire the action without triggering dropdown.</li>
 * <li><b>appTitle</b>:  The title of the FireflyViewer.  It will appears at top left of the banner. Defaults to 'Firefly'. </li>
 * <li><b>appIcon</b>:  A url string to the icon to appear on the banner. </li>
 * <li><b>footer</b>:   A react elements to place on the footer when the menu drop down. </li>
 * <li><b>dropdownPanels</b>:  An array of additional react elements which are mapped to a menu item's action. </li>
 * @param props
 *
 */
export const FireflySlate= memo(( {initLoadingMessage, appTitle= 'Firefly', appIcon=FFTOOLS_ICO, altAppIcon,
                                      footer, style, renderTreeId, menu:menuItems, showBgMonitor=false}) => {
    const state= useStoreConnector( (prevState) => getNextState(prevState,renderTreeId));
    const {isReady, mode={}, gridView= [], gridColumns=1, menu={}, dropDown={}, layoutInfo, initLoadCompleted, dropdownPanels} = state;
    const {expanded} = mode;
    const {visible, view, initArgs} = dropDown;

    useEffect(() => {
        startTTFeatureWatchers();
        const stopLayoutManager= startLayoutManager(renderTreeId, {renderTreeId, groupIgnoreFilter:'DPC'});
        dispatchOnAppReady(() => onReady(menuItems,layoutInfo));
        return () => void (stopLayoutManager());
    },[]);

    if (!isReady) return (<div style={{top: 0}} className='loading-mask'/>);
    if (showBgMonitor) menu.showBgMonitor= showBgMonitor;
    return (
        <RenderTreeIdCtx.Provider value={{renderTreeId}}>
            <div id='App' className='rootStyle' style={style}>
                <header>
                    <BannerSection {...{menu, appTitle, appIcon, altAppIcon}}/>
                    <div id={warningDivId} data-decor='full' className='warning-div center'/>
                    <DropDownContainer key='dropdown' footer={footer} visible={!!visible}
                        selected={view} initArgs={initArgs} {...{dropdownPanels} } />
                </header>
                <main style={{height:'100%'}}>
                    {mainView({expanded, gridView, gridColumns, initLoadingMessage, initLoadCompleted})}
                </main>
            </div>
        </RenderTreeIdCtx.Provider>
    );
});

/**
 * menu is an array of menu items {label, action, icon, desc, type}.
 * dropdownPanels is an array of additional react elements which are mapped to a menu item's action.
 * @type {{title: *, menu: *, appTitle: *, appIcon: *, altAppIcon: *, dropdownPanels: *}}
 */
FireflySlate.propTypes = {
    title: PropTypes.string,
    menu: PropTypes.arrayOf(PropTypes.object),
    appTitle: PropTypes.string,
    appIcon: PropTypes.string,
    altAppIcon: PropTypes.string,
    footer: PropTypes.element,
    dropdownPanels: PropTypes.arrayOf(PropTypes.element),
    style: PropTypes.object,
    showViewsSwitch: PropTypes.bool,
    leftButtons: PropTypes.arrayOf( PropTypes.func ),
    centerButtons: PropTypes.arrayOf( PropTypes.func ),
    rightButtons: PropTypes.arrayOf( PropTypes.func ),
    renderTreeId: PropTypes.string,
    showBgMonitor: PropTypes.bool,
    initLoadingMessage: PropTypes.string,
};


const closeExpanded= () => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);

/**
 *
 * @param props
 * @param props.expanded
 * @param props.gridView
 * @return {*}
 */
function mainView({expanded,gridView, gridColumns, initLoadingMessage, initLoadCompleted}) {
    if (expanded && expanded!==LO_VIEW.none && visRoot().apiToolsView) return <div/>;

    switch (expanded) {
        case LO_VIEW.images:
            return <ImageExpandedMode closeFunc={closeExpanded}/>;
        case LO_VIEW.xyPlots:
            const chartId = getExpandedChartProps()?.chartId;
            const chartViewerId= chartId && findViewerWithItemId(getMultiViewRoot(), chartId, PLOT2D);
            return <ChartsContainer closeable={true} expandedMode={true} viewerId={chartViewerId}/>;
        case LO_VIEW.tables:
            return <TablesContainer mode='both' closeable={true} expandedMode={true} />;
        default:
            if (isEmpty(gridView)) {
                return (initLoadingMessage && !initLoadCompleted) ?
                    <AppInitLoadingMessage message={initLoadingMessage}/> : <EmptyMessage/>;
            }
            else {
                return <NormalMainView {...{gridView, gridColumns, initLoadingMessage, initLoadCompleted}} />;
            }
    }
}


const NormalMainView= ({gridView, gridColumns, initLoadingMessage, initLoadCompleted}) => (
        <div key='normalMainView' style={{flex: '1 1 auto', display: 'flex', flexDirection:'column', justifyContent: 'flex-start'}}>
            <GridLayoutPanel {...{gridView:gridView||[], gridColumns, initLoadingMessage, initLoadCompleted}}/>
        </div> );

const EmptyMessage= () => (
        <div style={{flex: '1 1 auto', display:'flex', justifyContent:'center', alignItems:'center'}}>
            <div style={{fontSize: '50pt'}}>
            Firefly Ready
            </div>
        </div> );

const BannerSection= ({menu, appIcon, ...rest}) => (
    <Banner {...{key:'banner', appIcon, enableVersionDialog:true, ...rest,
        menu: <Menu menu={menu}/>,
    }}/> );

function onReady(menuItems, layoutInfo={}) {
    if (menuItems) dispatchSetMenu({menuItems});
    const {hasImages, hasTables, hasXyPlots} = layoutInfo;
    if (!(hasImages || hasTables || hasXyPlots)) {
        const goto = getActionFromUrl();
        if (goto) flux.process(goto);
    }
    dispatchNotifyRemoteAppReady();
}
