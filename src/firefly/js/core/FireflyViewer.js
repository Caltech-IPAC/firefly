/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {pickBy, get, filter} from 'lodash';

import {flux, firefly} from '../Firefly.js';
import {getMenu, isAppReady, dispatchSetMenu, dispatchOnAppReady} from '../core/AppDataCntlr.js';
import {LO_EXPANDED, getLayouInfo, SHOW_DROPDOWN} from '../core/LayoutCntlr.js';
import {Menu, getDropDownNames} from '../ui/Menu.jsx';
import Banner from '../ui/Banner.jsx';
import {DropDownContainer} from '../ui/DropDownContainer.jsx';
import {ResultsPanel} from '../ui/ResultsPanel.jsx';
import {TablesContainer} from '../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../visualize/ChartsContainer.jsx';
import {VisHeader} from '../visualize/ui/VisHeader.jsx';
import {VisToolbar} from '../visualize/ui/VisToolbar.jsx';
import {getActionFromUrl} from '../core/History.js';
import {TriViewImageSection, launchImageMetaDataSega} from '../visualize/ui/TriViewImageSection.jsx';
import {dispatchAddViewer} from '../visualize/MultiViewCntlr.js';
// import {deepDiff} from '../util/WebUtil.js';

const TriView_Types = ['tables', 'images', 'xyPlots', 'tri_view', 'image_xyplot', 'image_table', 'xyplot_table'];
const Image_Support = ['tri_view', 'image_xyplot', 'image_table', 'images'];
const XyPlot_Support = ['tri_view', 'image_xyplot', 'xyplot_table', 'xyPlots'];
const Table_Support = ['tri_view', 'image_table', 'xyplot_table', 'tables'];

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
 * <li><b>views</b>:  The type of result view.  Choices are 'tri_view', 'image_xyplot', 'image_table', 'xyplot_table', 'tables', 'images', or 'xyPlots'.  Default is 'tri_view'.</li>
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
        var {title, isReady, menu={}, appTitle, appIcon, altAppIcon,
                searchPanels, views, expanded, hasTables, hasImages, hasXyPlots,
                dropdownVisible, dropdownView} = this.state;
        const searches = getDropDownNames();

        if (!isReady || !this.mounted) {
            return (<div style={{top: 0}} className='loading-mask'/>);
        } else {
            return (
                <div id='App'>
                    <header>
                        <BannerSection {...{menu, appTitle, appIcon, altAppIcon}}/>
                        <DropDownContainer
                            key='dropdown'
                            visible={dropdownVisible}
                            selected={dropdownView}
                            {...{searches, searchPanels} } />
                    </header>
                    <main>
                        <DynamicResults {...{title, expanded, views, hasTables, hasImages, hasXyPlots}}/>
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
    views: PropTypes.oneOf(TriView_Types)
};

FireflyViewer.defaultProps = {
    title: 'Firefly',
    views: 'tri_view'
};

function onReady({menu, views}) {
    if (menu) {
        dispatchSetMenu({menuItems: menu});
    }
    if (Image_Support.includes(views) ) {
        dispatchAddViewer('triViewImages', true, true);
        launchImageMetaDataSega();
    }
    const home = getDropDownNames()[0];
    const goto = getActionFromUrl() || (home && {type: SHOW_DROPDOWN, payload: {view: home}});
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
    if (TriView_Types.includes(views)) {
        return <TriViewResults key='triview' {...props} />;
    }
}
DynamicResults.propTypes = {
    title: PropTypes.string,
    expanded: PropTypes.string,
    views: PropTypes.string,
    hasTables: PropTypes.bool,
    hasXyPlots: PropTypes.bool,
    hasImages: PropTypes.bool
};



/**
 * One view of the results this FireflyViewer can serve.
 * Must manually added to the DynamicResults component.
 */
export class TriViewResults extends Component {

    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }
    render() {
        var {title, expanded, views, hasTables, hasImages, hasXyPlots} = this.props;
        var hasData = [];
        var addTables, addImages, addXy;

        if (Image_Support.includes(views) ) {
            hasData.push(hasImages);
            addImages = hasImages;
        }
        if (XyPlot_Support.includes(views)) {
            hasData.push(hasXyPlots);
            addXy = hasXyPlots;
        }
        if (Table_Support.includes(views)) {
            hasData.push(hasTables);
            addTables = hasTables;
        }

        var content = {};
        var count = filter(hasData).length;
        if (addImages) {
            expanded = count === 1 ? LO_EXPANDED.images.view : expanded;
            content.imagePlot = (<TriViewImageSection key='res-tri-img'
                                                      showCoverage={true} showFits={true}
                                                      showImageMetaData={true}
                                                      closeable={count>1}
                                                      imageExpandedMode={expanded===LO_EXPANDED.images.view} />);
        }
        if (addXy) {
            expanded = count ===1 ? LO_EXPANDED.xyPlots.view : expanded;
            content.xyPlot = <ChartsContainer key='res-xyplots'
                                              closeable={count>1}
                                              expandedMode={expanded===LO_EXPANDED.xyPlots.view}/>;
        }
        if (addTables) {
            expanded = count ===1 ? LO_EXPANDED.tables.view : expanded;
            content.tables = <TablesContainer key='res-tables'
                                              closeable={count>1}
                                              expandedMode={expanded===LO_EXPANDED.tables.view}/>;
        }

        if (count === 0) return <div/>;
        else {
            return (
                <ResultsPanel key='results'
                              title={title}
                              expanded={expanded}
                              standard={views}
                              visToolbar={<VisToolbar key='res-vis-tb'/>}
                    { ...content}
                />
            );
        }
    }
}
TriViewResults.propTypes = {
    title: PropTypes.string,
    expanded: PropTypes.string,
    views: PropTypes.string,
    hasTables: PropTypes.bool,
    hasXyPlots: PropTypes.bool,
    hasImages: PropTypes.bool
};

