/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PureComponent} from 'react';
import PropTypes from 'prop-types';
import shallowequal from 'shallowequal';
import {get, pick, isEmpty} from 'lodash';

import {getDropDownInfo} from '../core/LayoutCntlr.js';
import {flux} from '../core/ReduxFlux.js';
import {getVersion} from '../Firefly.js';
import {SearchPanel} from '../ui/SearchPanel.jsx';
import {ImageSearchDropDown} from '../visualize/ui/ImageSearchPanelV2.jsx';
import {TestSearchPanel} from '../ui/TestSearchPanel.jsx';
import {TapSearchPanel} from './tap/TableSelectViewPanel.jsx';
import {TestQueriesPanel} from '../ui/TestQueriesPanel.jsx';
import {ChartSelectDropdown} from '../ui/ChartSelectDropdown.jsx';
import {CatalogSelectViewPanel} from '../visualize/ui/CatalogSelectViewPanel.jsx';
import {LSSTCatalogSelectViewPanel} from '../visualize/ui/LSSTCatalogSelectViewPanel.jsx';
import {FileUploadDropdown} from '../ui/FileUploadDropdown.jsx';
import {WorkspaceDropdown} from '../ui/WorkspaceDropdown.jsx';
import {getAlerts} from '../core/AppDataCntlr.js';
import {showInfoPopup} from '../ui/PopupUtil.jsx';

import './DropDownContainer.css';

const flexGrowWithMax = {width: '100%', maxWidth: 1200};

export const dropDownMap = {
    Search: {view: <SearchPanel />},
    TestSearch: {view: <TestSearchPanel />},
    TestSearches: {view: <TestQueriesPanel />},
    TAPSearch: {view: <TapSearchPanel />, layout: {width: '100%'}},
    ImageSearchPanelV2: {view: <ImageSearchDropDown />, layout: flexGrowWithMax},
    ImageSelectDropDownCmd: {view: <ImageSearchDropDown />, layout: flexGrowWithMax},
    ImageSelectDropDownSlateCmd: {view: <ImageSearchDropDown gridSupport={true} />, layout: flexGrowWithMax},
    ChartSelectDropDownCmd: {view: <ChartSelectDropdown />},
    IrsaCatalogDropDown: {view: <CatalogSelectViewPanel />},
    LsstCatalogDropDown: {view: <LSSTCatalogSelectViewPanel />},
    FileUploadDropDownCmd: {view: <FileUploadDropdown />},
    WorkspaceDropDownCmd: {view: <WorkspaceDropdown />},
};




/**
 * The container for items appearing in the drop down panel.
 * This container mimic a card layout in which it will accept multiple cards.
 * However, only one selected card will be displayed at a time.
 * Items in this container must have a 'name' property.  It will be used to
 * compare to the selected card.
 */
export class DropDownContainer extends Component {
    constructor(props) {
        super(props);

        React.Children.forEach(this.props.children, (el) => {
            const {name:key, layout} = get(el, 'props', {});
            if (key) dropDownMap[key] = {view: el, layout};
        });

        if (props.dropdownPanels) {
            props.dropdownPanels.forEach( (el) => {
                const {name:key, layout} = get(el, 'props', {});
                if (key) dropDownMap[key] = {view: el, layout};
            } );
        }

        this.state = {
                visible: props.visible,
                selected: props.selected
            };
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
        this.iAmMounted = true;
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        this.removeListener && this.removeListener();
    }
    
    shouldComponentUpdate(nProps, nState) {
        const check = ['visible','selected'];
        return !shallowequal(pick(nState, check), pick(this.state, check)) || this.props.initArgs!==nProps.initArgs;
   }

    storeUpdate() {
        if (this.iAmMounted) {
            const {visible, view} = getDropDownInfo();
            if (visible !== this.state.visible || view !== this.state.selected) {
                this.setState({visible, selected: view});
            }
        }
    }

    render() {
        const {footer, alerts, initArgs={}} = this.props;
        const { visible, selected }= this.state;
        const {view, layout={}} = dropDownMap[selected] || {};

        if (!visible) return <div/>;

        const contentWrapStyle = isEmpty(layout) ? {} : {display: 'flex', width: '100%', justifyContent: 'center'};
        const contentStyle = {flexGrow: 1, ...layout};

        return (
            <div>
                <div className='DD-ToolBar'>
                    {alerts || <Alerts />}
                    <div style={{flexGrow: 1, ...contentWrapStyle}}>
                        <div style={contentStyle} className='DD-ToolBar__content'>
                            {view && React.cloneElement(view, { initArgs } ) }
                        </div>
                    </div>
                    <div id='footer' className='DD-ToolBar__footer'>
                        {footer}
                        <div className='DD-ToolBar__version'>
                            <VersionInfo versionInfo={getVersion()}/>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

DropDownContainer.propTypes = {
    visible: PropTypes.bool,
    selected: PropTypes.string,
    dropdownPanels: PropTypes.arrayOf(PropTypes.element),
    footer: PropTypes.node,
    alerts: PropTypes.node,
    initArgs: PropTypes.object
};
DropDownContainer.defaultProps = {
    visible: false
};

function VersionInfo({versionInfo={}}) {
    const {BuildMajor, BuildMinor, BuildRev, BuildType, BuildDate} = versionInfo;
    const showFullInfo = () => showInfoPopup(versionInfoFull(versionInfo), 'Version Information');

    let version = `v${BuildMajor}.${Number(BuildMinor)===-1?'Next':BuildMinor}`;
    version += BuildRev !== '0' ? `.${BuildRev}` : '';
    version += BuildType === 'Final' ? '' : `_${BuildType}`;
    const builtOn = ` Built On: ${BuildDate}`;
    return (
        <div onClick={showFullInfo}>{version + builtOn}</div>
    );

}

function versionInfoFull({BuildMajor, BuildMinor, BuildRev, BuildNumber, BuildType, BuildTime, BuildTag, BuildCommit, BuildCommitFirefly}) {
    let version = `v${BuildMajor}.${Number(BuildMinor)===-1?'Next':BuildMinor}`;
    version += BuildRev !== '0' ? `.${BuildRev}` : '';
    return (
        <div className='DD-Version'>
            <div className='DD-Version__item'>
                <div className='DD-Version__key'>Version</div>
                <div className='DD-Version__value'>{version}</div>
            </div>
            <div className='DD-Version__item'>
                <div className='DD-Version__key'>Type</div>
                <div className='DD-Version__value'>{BuildType}</div>
            </div>
            <div className='DD-Version__item'>
                <div className='DD-Version__key'>Build Number</div>
                <div className='DD-Version__value'>{BuildNumber}</div>
            </div>
            <div className='DD-Version__item'>
                <div className='DD-Version__key'>Built On</div>
                <div className='DD-Version__value'>{BuildTime}</div>
            </div>
            <div className='DD-Version__item'>
                <div className='DD-Version__key'>Git commit</div>
                <div className='DD-Version__value'>{BuildCommit}</div>
            </div>
            { BuildCommitFirefly &&
                <div className='DD-Version__item'>
                    <div className='DD-Version__key'>Git commit(Firefly)</div>
                    <div className='DD-Version__value'>{BuildCommitFirefly}</div>
                </div>
            }
        </div>
    );

}

export class Alerts extends PureComponent {

    constructor(props) {
        super(props);
        this.state = Object.assign({}, props);
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
        this.iAmMounted = true;
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        this.removeListener && this.removeListener();
    }

    storeUpdate() {
        if (this.iAmMounted) {
            this.setState(getAlerts());
        }
    }

    render() {
        const {msg, style} = this.state;
        if (msg) {
            /* eslint-disable react/no-danger */
            return (
                <div className='alerts__msg' style={style}>
                    <div dangerouslySetInnerHTML={{__html: msg}} />
                </div>
            );
        } else return <div/>;
    }
}

Alerts.propTypes = {
    msg: PropTypes.string,
    style: PropTypes.object
};
