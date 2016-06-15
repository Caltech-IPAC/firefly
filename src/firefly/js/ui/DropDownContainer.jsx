/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import shallowequal from 'shallowequal';
import {get, pick} from 'lodash';

import {getDropDownInfo} from '../core/LayoutCntlr.js';
import {flux, getVersion} from '../Firefly.js';
import {SearchPanel} from '../ui/SearchPanel.jsx';
import {TestQueriesPanel} from '../ui/TestQueriesPanel.jsx';
import {ImageSelectDropdown} from '../ui/ImageSelectDropdown.jsx';
import {CatalogSelectViewPanel} from '../visualize/ui/CatalogSelectViewPanel.jsx';

import './DropDownContainer.css';
// import {deepDiff} from '../util/WebUtil.js';


const dropDownMap = {
    AnyDataSetSearch: <SearchPanel />,
    TestSearches: <TestQueriesPanel />,
    ImageSelectDropDownCmd: <ImageSelectDropdown />,
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
    }

    componentWillUnmount() {
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
        const {visible, view} = getDropDownInfo();
        if (visible!==this.state.visible || view!==this.state.selected) {
            this.setState({visible, selected: view});
        }
    }

    render() {
        const {footer} = this.props;
        var { visible, selected }= this.state;
        var view = dropDownMap[selected];

        if (!visible) return <div/>;
        return (
            <div className='DD-ToolBar'>
                <div className='DD-ToolBar__content'>
                    {view}
                </div>
                <div id='footer' className='DD-ToolBar__footer'>
                    {footer}
                    <div className='DD-ToolBar__version'>
                        {getVersion()}
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
    footer: PropTypes.node
};
DropDownContainer.defaultProps = {
    visible: false
};

const Alerts = (props) => {
    return (
        <div id="region-alerts" aria-hidden="true" style="width: 100%; height: 100%; display: none;">
            <div align="left" style="width: 100%; height: 100%;"></div>
        </div>
    );
};
