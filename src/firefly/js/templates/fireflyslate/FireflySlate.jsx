/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get, pickBy, isEmpty} from 'lodash';

import {flux, firefly} from '../../Firefly.js';
import {isDefined} from '../../util/WebUtil.js';
import {getMenu, isAppReady, dispatchSetMenu, dispatchOnAppReady} from '../../core/AppDataCntlr.js';
import {getLayouInfo, dispatchSetLayoutMode, getGridView, getGridViewColumns,
            SHOW_DROPDOWN, LO_MODE, LO_VIEW} from '../../core/LayoutCntlr.js';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {startLayoutManager} from './FireflySlateManager.js';
import {Menu} from '../../ui/Menu.jsx';
import {Banner} from '../../ui/Banner.jsx';
import {visRoot} from '../../visualize/ImagePlotCntlr.js';
import {DropDownContainer} from '../../ui/DropDownContainer.jsx';
import {GridLayoutPanel} from './GridLayoutPanel.jsx';
import {ImageExpandedMode} from '../../visualize/iv/ImageExpandedMode.jsx';
import {VisHeader} from '../../visualize/ui/VisHeader.jsx';
import {getActionFromUrl} from '../../core/History.js';
import {getExpandedChartProps} from '../../charts/ChartsCntlr.js';
import {VisToolbar} from '../../visualize/ui/VisToolbar.jsx';
import {getMultiViewRoot, findViewerWithItemId, PLOT2D} from '../../visualize/MultiViewCntlr.js';

import FFTOOLS_ICO from 'html/images/fftools-logo-offset-small-75x75.png';
import {warningDivId} from '../../ui/LostConnection';
import {startTTFeatureWatchers} from '../common/ttFeatureWatchers.js';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';



/**
 * This FireflySlate is a generic layout application with some configurable behaviors.
 * The application is separated into these major parts:  banner, menu, searches, and results.
 * The props below allow you to alter their default behaviors.
 *
 * <b>Props</b>
 * <li><b>title</b>:  This title will appears at center top of the results area. Defaults to 'FFTools'. </li>
 * <li><b>menu</b>:  menu is an array of menu items {label, action, icon, desc, type}.  Leave type blank for dropdown.  If type='COMMAND', it will fire the action without triggering dropdown.</li>
 * <li><b>appTitle</b>:  The title of the FireflyViewer.  It will appears at top left of the banner. Defaults to 'Firefly'. </li>
 * <li><b>appIcon</b>:  A url string to the icon to appear on the banner. </li>
 * <li><b>footer</b>:   A react elements to place on the footer when the menu drop down. </li>
 * <li><b>dropdownPanels</b>:  An array of additional react elements which are mapped to a menu item's action. </li>
 *
 */
export class FireflySlate extends PureComponent {

    constructor(props) {
        super(props);
        this.state = this.getNextState();
        startTTFeatureWatchers();
        this.stopLayoutManager= startLayoutManager(this.props.renderTreeId, {renderTreeId:this.props.renderTreeId});

    }

    getNextState() {
        const {renderTreeId}= this.props;
        const menu = getMenu();
        const layoutInfo = getLayouInfo();
        const gridView= getGridView(layoutInfo, renderTreeId);
        const gridColumns= getGridViewColumns(layoutInfo, renderTreeId);
        // const gridView= getGridView(layoutInfo);
        // const gridColumns= getGridViewColumns(layoutInfo);
        const isReady = isAppReady();

        return Object.assign({gridView:undefined}, this.props,
            {gridColumns, gridView, menu, isReady, ...layoutInfo});
    }

    componentDidMount() {
        dispatchOnAppReady((state) => {
            onReady({state, menu: this.props.menu});
        });
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
        this.stopLayoutManager && this.stopLayoutManager();
    }

    storeUpdate() {
        this.setState(this.getNextState());
    }

    render() {
        const {isReady, mode, gridView= [], gridColumns=1,  menu={}, appTitle, appIcon, altAppIcon, dropDown,
                dropdownPanels, footer, style, showViewsSwitch,
                leftButtons, centerButtons, rightButtons} = this.state;
        const {expanded} = mode || {};
        const {visible, view} = dropDown || {};
        if (isDefined(this.props.showBgMonitor)) menu.showBgMonitor= this.props.showBgMonitor;


        if (!isReady) {
            return (<div style={{top: 0}} className='loading-mask'/>);
        } else {
            return (
                    <div id='App' className='rootStyle' style={style}>
                        <header>
                            <BannerSection {...{menu, appTitle, appIcon, altAppIcon}}/>
                            <div id={warningDivId} data-decor='full' className='warning-div center'/>
                            <RenderTreeIdCtx.Provider value={{renderTreeId : this.props.renderTreeId}}>
                                <DropDownContainer
                                    key='dropdown'
                                    footer={footer}
                                    visible={!!visible}
                                    selected={view}
                                    {...{dropdownPanels} } />
                            </RenderTreeIdCtx.Provider>
                        </header>
                        <main style={{height:'100%'}}>
                            <RenderTreeIdCtx.Provider value={{renderTreeId : this.props.renderTreeId}}>
                                {mainView({expanded, gridView, gridColumns})}
                            </RenderTreeIdCtx.Provider>
                        </main>
                    </div>
            );
        }
    }
}


const closeExpanded= () => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);


/**
 *
 * @param props
 * @param props.expanded
 * @param props.gridView
 * @param props.leftButtons
 * @param props.centerButtons
 * @param props.rightButtons
 * @return {*}
 */
function mainView({expanded,gridView, gridColumns, leftButtons, centerButtons, rightButtons}) {
    const vr= visRoot();

    if (expanded && expanded!==LO_VIEW.none && vr.apiToolsView) return <div></div>;

    switch (expanded) {
        case LO_VIEW.images:
            return <ImageExpandedMode closeFunc={closeExpanded}/>;
            break;
        case LO_VIEW.xyPlots:
            const chartId = get(getExpandedChartProps(), 'chartId');
            const chartViewerId= chartId ? findViewerWithItemId(getMultiViewRoot(), chartId, PLOT2D) : null;
            return <ChartsContainer closeable={true} expandedMode={true} viewerId={chartViewerId}/>;
            break;
        case LO_VIEW.tables:
            return <TablesContainer mode='both' closeable={true} expandedMode={true} />;
            break;
        default:
            return isEmpty(gridView) ? showEmptyMessage()  : normalMainView({gridView, gridColumns});
    }
}


function normalMainView({gridView, gridColumns, leftButtons, centerButtons, rightButtons}) {

    return (
        <div key='normalMainView' style={{flex: '1 1 auto', display: 'flex', flexDirection:'column', justifyContent: 'flex-start'}}>
            <VisToolbar style={{paddingBottom:6, borderBottom: '1px solid rgba(0,0,0,.2)'}}/>
            <GridLayoutPanel gridView={gridView||[]} gridColumns={gridColumns}/>
        </div>
    );
}


function showEmptyMessage() {
    return (
        <div style={{flex: '1 1 auto', display:'flex', justifyContent:'center', alignItems:'center'}}>
            <div style={{fontSize: '50pt'}}>
            Firefly Ready
            </div>
        </div>

    );
}

/*
<div style={{display: 'inline-flex', justifyContent: 'space-between', flex: '1 1 auto' }}>
    <div>
        {leftButtons && leftButtons.map( (el) => el()) }
    </div>
    <div>
        {centerButtons && centerButtons.map( (el) => el()) }
    </div>
    <div style={{display: 'inline-flex'}}>
        {rightButtons && rightButtons.map( (el) => el()) }
        <div style={{width: 20}}/>
    </div>
</div>
*/




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
};

FireflySlate.defaultProps = {
    appTitle: 'Firefly',
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
    const {menu, appIcon=FFTOOLS_ICO, ...rest} = pickBy(props);
    return (
        <Banner key='banner'
            menu={<Menu menu={menu} /> }
            visPreview={<VisHeader showHeader={false}/> }
            readout={<VisHeader showPreview={false}/> }
            appIcon={appIcon}
            {...rest}
        />
    );
}


