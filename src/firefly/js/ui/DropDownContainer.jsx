/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import shallowequal from 'shallowequal';
import {get, pick} from 'lodash';

import {getDropDownInfo} from '../core/LayoutCntlr.js';
import {flux, getVersion} from '../Firefly.js';
import {SearchPanel} from '../ui/SearchPanel.jsx';
import {TestQueriesPanel} from '../ui/TestQueriesPanel.jsx';
import {ImageSelectDropdown} from '../ui/ImageSelectDropdown.jsx';
import {ChartSelectDropdown} from '../ui/ChartSelectDropdown.jsx';
import {CatalogSelectViewPanel} from '../visualize/ui/CatalogSelectViewPanel.jsx';
import {getAlerts} from '../core/AppDataCntlr.js';

import './DropDownContainer.css';
// import {deepDiff} from '../util/WebUtil.js';


const dropDownMap = {
    AnyDataSetSearch: <SearchPanel />,
    TestSearches: <TestQueriesPanel />,
    ImageSelectDropDownCmd: <ImageSelectDropdown />,
    ChartSelectDropDownCmd: <ChartSelectDropdown />,
    IrsaCatalogDropDown: <CatalogSelectViewPanel/>
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
            const key = get(el, 'props.name');
            if (key) dropDownMap[key] = el;
        });

        if (props.searchPanels) {
            props.searchPanels.forEach( (el) => {
                const key = get(el, 'props.name');
                if (key) dropDownMap[key] = el;
            } );
        }

        this.state = {
                visible: props.visible,
                selected: props.selected,
                searches: props.searches
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
        return !shallowequal(pick(nState, check), pick(this.state, check));
   }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }

    storeUpdate() {
        if (this.iAmMounted) {
            const {visible, view} = getDropDownInfo();
            if (visible !== this.state.visible || view !== this.state.selected) {
                this.setState({visible, selected: view});
            }
        }
    }

    render() {
        const {footer, alerts} = this.props;
        var { visible, selected }= this.state;
        var view = dropDownMap[selected];

        if (!visible) return <div/>;
        return (
            <div>
                <div className='DD-ToolBar'>
                    {alerts || <Alerts />}
                    <div style={{flexGrow: 1}}>
                        <div className='DD-ToolBar__content'>
                            {view}
                        </div>
                    </div>
                    <div id='footer' className='DD-ToolBar__footer'>
                        {footer}
                        <div className='DD-ToolBar__version'>
                            {getVersion()}
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
    searches: PropTypes.arrayOf(PropTypes.string),
    searchPanels: PropTypes.arrayOf(PropTypes.element),
    footer: PropTypes.node,
    alerts: PropTypes.node
};
DropDownContainer.defaultProps = {
    visible: false
};

export class Alerts extends Component {

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

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
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