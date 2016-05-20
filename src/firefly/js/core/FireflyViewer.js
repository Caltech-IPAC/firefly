
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {pickBy, filter} from 'lodash';

import {flux, firefly} from '../Firefly.js';
import {getMenu, isAppReady, dispatchSetMenu, dispatchOnAppReady} from '../core/AppDataCntlr.js';
import {LO_VIEW, LO_MODE, getLayouInfo, SHOW_DROPDOWN, dispatchSetLayoutMode} from '../core/LayoutCntlr.js';
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
        var {title, isReady, menu={}, appTitle, appIcon, altAppIcon,
                searchPanels, views, expanded,  standard, hasTables, hasImages, hasXyPlots,
                dropdownVisible, dropdownView} = this.state;
        const searches = getDropDownNames();
        standard = LO_VIEW.get(standard);
        expanded = LO_VIEW.get(expanded);

        if (!isReady || !this.mounted) {
            return (<div style={{top: 0}} className='loading-mask'/>);
        } else {
            return (
                <div id='App' className='rootStyle'>
                    <header>
                        <BannerSection {...{menu, appTitle, appIcon, altAppIcon}}/>
                        <DropDownContainer
                            key='dropdown'
                            visible={dropdownVisible}
                            selected={dropdownView}
                            {...{searches, searchPanels} } />
                    </header>
                    <main>
                        <DynamicResults {...{title, expanded, views, standard, hasTables, hasImages, hasXyPlots}}/>
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
    title: 'Firefly',
    views: 'tri_view'
};

function onReady({menu, views}) {
    views = LO_VIEW.get(views) || LO_VIEW.none;
    if (menu) {
        dispatchSetMenu({menuItems: menu});
    }
    if (views.has(LO_VIEW.images) ) {
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
    if (LO_VIEW.get(views)) {
        return <TriViewResults key='triview' {...props} />;
    }
}
DynamicResults.propTypes = {
    title: PropTypes.string,
    expanded: PropTypes.object,
    standard: PropTypes.object,
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
        var {title, expanded, standard, views, hasTables, hasImages, hasXyPlots} = this.props;
        var hasData = [];
        var addTables, addImages, addXy;
        standard = standard || views;
        views = LO_VIEW.get(views) || LO_VIEW.none;

        if (views.has(LO_VIEW.images)) {
            hasData.push(hasImages);
            addImages = hasImages;
        }
        if (views.has(LO_VIEW.xyPlots)) {
            hasData.push(hasXyPlots);
            addXy = hasXyPlots;
        }
        if (views.has(LO_VIEW.tables)) {
            hasData.push(hasTables);
            addTables = hasTables;
        }

        var content = {};
        var count = filter(hasData).length;
        if (addImages) {
            expanded = count === 1 ? LO_VIEW.images : expanded;
            content.imagePlot = (<TriViewImageSection key='res-tri-img'
                                                      showCoverage={true} showFits={true}
                                                      showImageMetaData={true}
                                                      closeable={count>1}
                                                      imageExpandedMode={expanded===LO_VIEW.images} />);
        }
        if (addXy) {
            expanded = count ===1 ? LO_VIEW.xyPlots : expanded;
            content.xyPlot = (<ChartsContainer key='res-xyplots'
                                              closeable={count>1}
                                              expandedMode={expanded===LO_VIEW.xyPlots}/>);
        }
        if (addTables) {
            expanded = count ===1 ? LO_VIEW.tables : expanded;
            content.tables = (<TablesContainer key='res-tables'
                                               mode='both'
                                               closeable={count>1}
                                               expandedMode={expanded===LO_VIEW.tables}/>);
        }
        const searchDesc = (addImages && addXy && addTables) ?
                            (<div>
                                <div style={ {display: 'inline-block', float: 'right'} }>
                                    <button type='button' className='button-std'
                                            onClick={() => dispatchSetLayoutMode(LO_MODE.standard, LO_VIEW.get('tables | images | xyPlots'))}>tri-view</button>
                                    <button type='button' className='button-std'
                                            onClick={() => dispatchSetLayoutMode(LO_MODE.standard, LO_VIEW.get('tables | images'))}>img-tbl</button>
                                    <button type='button' className='button-std'
                                            onClick={() => dispatchSetLayoutMode(LO_MODE.standard, LO_VIEW.get('images | xyPlots'))}>img-xy</button>
                                    <button type='button' className='button-std'
                                            onClick={() => dispatchSetLayoutMode(LO_MODE.standard, LO_VIEW.get('tables | xyPlots'))}>xy-tbl</button>
                                </div>
                            </div>)
                            : <div/>;


        if (count === 0) return <div/>;
        else {
            return (
                <ResultsPanel key='results'
                              title={title}
                              searchDesc ={searchDesc}
                              expanded={expanded}
                              standard={standard}
                              visToolbar={<VisToolbar key='res-vis-tb'/>}
                    { ...content}
                />
            );
        }
    }
}
TriViewResults.propTypes = {
    title: PropTypes.string,
    views: PropTypes.string,
    expanded: PropTypes.object,
    standard: PropTypes.object,
    hasTables: PropTypes.bool,
    hasXyPlots: PropTypes.bool,
    hasImages: PropTypes.bool
};

