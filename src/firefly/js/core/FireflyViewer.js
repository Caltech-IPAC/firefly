/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {pickBy} from 'lodash';

import {flux, firefly} from '../Firefly.js';
import {getMenu, isAppReady, dispatchSetMenu, dispatchOnAppReady} from '../core/AppDataCntlr.js';
import {LO_VIEW, getLayouInfo, SHOW_DROPDOWN, layoutManager} from '../core/LayoutCntlr.js';
import {Menu, getDropDownNames} from '../ui/Menu.jsx';
import Banner from '../ui/Banner.jsx';
import {DropDownContainer} from '../ui/DropDownContainer.jsx';
import {TriViewPanel} from '../ui/TriViewPanel.jsx';
import {VisHeader} from '../visualize/ui/VisHeader.jsx';
import {getActionFromUrl} from '../core/History.js';
import {launchImageMetaDataSega, FITS_VIEWER_ID} from '../visualize/ui/TriViewImageSection.jsx';
import {dispatchAddViewer} from '../visualize/MultiViewCntlr.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';

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
 * <li><b>searchPanels</b>:  An array of additional react elements which are mapped to a menu item's action. </li>
 * <li><b>views</b>:  The type of result view.  Choices are 'images', 'tables', and 'xyPlots'.  They can be combined with ' | ', i.e.  'images | tables'</li>
 *
 */
export class FireflyViewer extends Component {

    constructor(props) {
        super(props);
        this.state = this.getNextState();
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
        this.mounted = true;
        dispatchOnAppReady((state) => {
            onReady({state, menu: this.props.menu, views: this.props.views});
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
                searchPanels, views} = this.state;
        const {visible, view} = dropDown || {};
        const searches = getDropDownNames();

        if (!isReady || !this.mounted) {
            return (<div style={{top: 0}} className='loading-mask'/>);
        } else {
            return (
                <div id='App' className='rootStyle'>
                    <header>
                        <BannerSection {...{menu, appTitle, appIcon, altAppIcon}}/>
                        <DropDownContainer
                            key='dropdown'
                            visible={!!visible}
                            selected={view}
                            {...{searches, searchPanels} } />
                    </header>
                    <main>
                        <DynamicResults views={views}/>
                    </main>
                </div>
            );
        }
    }
}

/**
 * menu is an array of menu items {label, action, icon, desc, type}.
 * searchPanels is an array of additional react elements which are mapped to a menu item's action.
 * @type {{title: *, menu: *, appTitle: *, appIcon: *, altAppIcon: *, searchPanels: *, views: *}}
 */
FireflyViewer.propTypes = {
    title: PropTypes.string,
    menu: PropTypes.arrayOf(PropTypes.object),
    appTitle: PropTypes.string,
    appIcon: PropTypes.string,
    altAppIcon: PropTypes.string,
    searchPanels: PropTypes.arrayOf(PropTypes.element),
    views: PropTypes.string     // combination of LO_VIEW separated by ' | '.  ie. 'images | tables'.
};

FireflyViewer.defaultProps = {
    views: 'tri_view'
};

function onReady({menu, views}) {
    views = LO_VIEW.get(views) || LO_VIEW.none;
    if (menu) {
        dispatchSetMenu({menuItems: menu});
    }
    if (views.has(LO_VIEW.images) ) {
        dispatchAddViewer(FITS_VIEWER_ID, true, true);
        dispatchAddSaga(layoutManager,{views});
        launchImageMetaDataSega();
    }
    const goto = getActionFromUrl() || {type: SHOW_DROPDOWN};
    if (goto) firefly.process(goto);
}

function BannerSection(props) {
    const {menu, ...rest} = pickBy(props);
    return (
        <Banner key='banner'
            menu={<Menu menu={menu} /> }
            visPreview={<VisHeader/> }
            {...rest}
        />
    );
}


function DynamicResults(props) {
    var {views} = props;
    if (LO_VIEW.get(views)) {
        return <TriViewPanel/>;
    }
}
DynamicResults.propTypes = {
    views: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.object])
};

