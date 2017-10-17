/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get, pickBy} from 'lodash';

import {flux, firefly} from '../../Firefly.js';
import {getMenu, isAppReady, dispatchSetMenu, dispatchOnAppReady} from '../../core/AppDataCntlr.js';
import {LO_VIEW, getLayouInfo, SHOW_DROPDOWN} from '../../core/LayoutCntlr.js';
import {layoutManager} from './FireflyViewerManager.js';
import {Menu} from '../../ui/Menu.jsx';
import {Banner} from '../../ui/Banner.jsx';
import {DropDownContainer} from '../../ui/DropDownContainer.jsx';
import {TriViewPanel} from './TriViewPanel.jsx';
import {VisHeader} from '../../visualize/ui/VisHeader.jsx';
import {getActionFromUrl} from '../../core/History.js';
import {launchImageMetaDataSega} from '../../visualize/ui/TriViewImageSection.jsx';
import {syncChartViewer, addDefaultScatter} from '../../visualize/saga/ChartsSync.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';
import {watchCatalogs} from '../../visualize/saga/CatalogWatcher.js';
import {getImageMasterData} from '../../visualize/ui/AllImageSearchConfig.js';

import FFTOOLS_ICO from 'html/images/fftools-logo-offset-small-75x75.png';

// import {deepDiff} from '../util/WebUtil.js';


/**
 * This FireflyViewer is a generic application with some configurable behaviors.
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
 * <li><b>views</b>:  The type of result view.  Choices are 'images', 'tables', and 'xyPlots'.  They can be combined with ' | ', i.e.  'images | tables'</li>
 *
 */
export class FireflyViewer extends PureComponent {

    constructor(props) {
        super(props);
        getImageMasterData();
        const views = LO_VIEW.get(props.views) || LO_VIEW.none;
        this.state = this.getNextState();
        dispatchAddSaga(watchCatalogs);
        if (views.has(LO_VIEW.images) ) launchImageMetaDataSega();
        dispatchAddSaga(layoutManager,{views: props.views});
        dispatchAddSaga(syncChartViewer);
        dispatchAddSaga(addDefaultScatter);
    }

    getNextState() {
        const menu = getMenu();
        const layoutInfo = getLayouInfo();
        const isReady = isAppReady();

        return Object.assign({}, this.props,
            {menu, isReady, ...layoutInfo});
    }

    componentDidMount() {
        dispatchOnAppReady((state) => {
            onReady({state, menu: this.props.menu, views: this.props.views, options:this.props.options});
        });
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
    }

    storeUpdate() {
        this.setState(this.getNextState());
    }

    render() {
        var {isReady, menu={}, appTitle, appIcon, altAppIcon, dropDown, showUserInfo,
                dropdownPanels, views, footer, style, showViewsSwitch, leftButtons, centerButtons, rightButtons} = this.state;
        const {visible, view} = dropDown || {};

        if (!isReady) {
            return (<div style={{top: 0}} className='loading-mask'/>);
        } else {
            return (
                <div id='App' className='rootStyle' style={style}>
                    <header>
                        <BannerSection {...{menu, showUserInfo, appTitle, appIcon, altAppIcon}}/>
                        <DropDownContainer
                            key='dropdown'
                            footer={footer}
                            visible={!!visible}
                            selected={view}
                            {...{dropdownPanels} } />
                    </header>
                    <main>
                        <DynamicResults {...{views, showViewsSwitch, leftButtons, centerButtons, rightButtons}}/>
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
FireflyViewer.propTypes = {
    title: PropTypes.string,
    menu: PropTypes.arrayOf(PropTypes.object),
    appTitle: PropTypes.string,
    appIcon: PropTypes.string,
    altAppIcon: PropTypes.string,
    showUserInfo: PropTypes.bool,
    footer: PropTypes.element,
    dropdownPanels: PropTypes.arrayOf(PropTypes.element),
    views: PropTypes.string,     // combination of LO_VIEW separated by ' | '.  ie. 'images | tables'.
    style: PropTypes.object,
    showViewsSwitch: PropTypes.bool,
    leftButtons: PropTypes.arrayOf( PropTypes.func ),
    centerButtons: PropTypes.arrayOf( PropTypes.func ),
    rightButtons: PropTypes.arrayOf( PropTypes.func ),
    options: PropTypes.object,
};

FireflyViewer.defaultProps = {
    appTitle: 'Firefly',
    views: 'images | tables | xyPlots'
};

function onReady({menu, views, options={}}) {
    if (menu) {
        const {backgroundMonitor= true}= options;
        dispatchSetMenu({menuItems: menu, showBgMonitor:backgroundMonitor});
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


function DynamicResults(props) {
    var {views, ...rest} = props;
    if (LO_VIEW.get(views)) {
        return <TriViewPanel {...rest}/>;
    }
}
DynamicResults.propTypes = {
    views: PropTypes.oneOfType([
                    PropTypes.string,
                    PropTypes.object]),
    showViewsSwitch: PropTypes.bool,
    leftButtons: PropTypes.arrayOf( PropTypes.func ),
    centerButtons: PropTypes.arrayOf( PropTypes.func ),
    rightButtons: PropTypes.arrayOf( PropTypes.func )
};
DynamicResults.defaultProps = {
    showViewsSwitch: true
};

