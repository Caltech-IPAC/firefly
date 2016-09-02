/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {pickBy} from 'lodash';

import {flux, firefly} from '../../Firefly.js';
import {getMenu, isAppReady, dispatchSetMenu, dispatchOnAppReady} from '../../core/AppDataCntlr.js';
import {LO_VIEW, getLayouInfo, SHOW_DROPDOWN} from '../../core/LayoutCntlr.js';
import {lcManager} from './lcManager.js';
import {LcResult} from './LcResult.jsx';
import {Menu} from '../../ui/Menu.jsx';
import {Banner} from '../../ui/Banner.jsx';
import {DropDownContainer} from '../../ui/DropDownContainer.jsx';
import {VisHeader} from '../../visualize/ui/VisHeader.jsx';
import {getActionFromUrl} from '../../core/History.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';

import {FormPanel} from './../../ui/FormPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import Validate from '../../util/Validate.js';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import * as TblUtil from '../../tables/TableUtil.js';

// import {deepDiff} from '../util/WebUtil.js';

/**
 * This is a light curve viewer.
 */
export class LcViewer extends Component {

    constructor(props) {
        super(props);
        this.state = this.getNextState();
        dispatchAddSaga(lcManager,{views: props.views});
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
                        <LcResult {...{views}}/>
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
    style: PropTypes.object,
};
LcViewer.defaultProps = {
    appTitle: 'Light Curve'
};

function onReady({menu, views}) {
    views = LO_VIEW.get(views) || LO_VIEW.none;
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
            visPreview={<VisHeader/> }
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
                width='640px' height='100px'
                groupKey='TBL_BY_URL_PANEL'
                onSubmit={(request) => onSearchSubmit(request)}
                onCancel={hideSearchPanel}>
                <FieldGroup groupKey='TBL_BY_URL_PANEL' validatorFunc={null} keepState={true}>
                    <FileUpload
                        wrapperStyle = {{margin: '5px 0'}}
                        fieldKey = 'fileUpload'
                        groupKey='TBL_BY_URL_PANEL'
                        initialState= {{
                                tooltip: 'Select a file to upload',
                                label: 'Upload File:'
                            }}
                    />
                    <ValidationField fieldKey='tbl_index'
                                     groupKey='TBL_BY_URL_PANEL'
                                     initialState= {{
                                            value: 0,
                                            size: 4,
                                            validator: Validate.intRange.bind(null, 0, 100000),
                                            label : 'Table Index:',
                                            labelWidth : 60
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
    name: 'LCUpload',
};


function hideSearchPanel() {
    dispatchHideDropDown();
}

function onSearchSubmit(request) {
    if (request.fileUpload) {
        const treq = TblUtil.makeFileRequest('Raw Table', request.fileUpload, null, {tbl_id:'raw_table', ...request});
        dispatchTableSearch(treq);
    }
}
