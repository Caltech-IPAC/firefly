/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {get} from 'lodash';

import {getDropDownInfo} from '../core/LayoutCntlr.js';
import {flux} from '../Firefly.js';

import './DropDownContainer.css';

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
        this.state = {
                visible: props.visible,
                selected: props.selected
            };
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }

    storeUpdate() {
        const {visible, view} = getDropDownInfo();
        this.setState({visible, selected: view});
    }

    render() {
        var { visible, selected }= this.state;
        var view;
        React.Children.forEach(this.props.children, (child) => {
            if (selected === get(child, 'props.name')) {
                view = child;
            }
        });
        return (
            visible &&
            <div className='DD-ToolBar'>
                <div className='DD-ToolBar__content'>
                    {view}
                </div>

                <Footers />
            </div>
        );
    }
}

DropDownContainer.propTypes = {
    visible: PropTypes.bool,
    selected: PropTypes.string
};
DropDownContainer.defaultProps = {
    visible: false
};




 const Footers = (props) => {
    return (
        <div id='footer' className='DD-ToolBar__footer'>
            <div className='DD-ToolBar__footer--links'>
                <ul>
                    <li><a href='https://irsasupport.ipac.caltech.edu/' target='helpdesk'>Contact</a></li>
                    <li><a href='http://irsa.ipac.caltech.edu/privacy.html' target='privacy'>Privacy Policy</a></li>
                    <li><a href='http://irsa.ipac.caltech.edu/ack.html' target='ack'>Acknowledge IRSA</a></li>
                </ul>
            </div>
            <div className='DD-ToolBar__footer--icons'>
                <a href='http://www.ipac.caltech.edu/'
                   alt='Infrared Processing and Analysis Center' target='ipac'
                   title='Infrared Processing and Analysis Center'><img alt='Icon_ipac'
                                                                        src='footer/icon_ipac-white-78x60.png'/></a>
                <a href='http://www.caltech.edu/'
                   alt='California Institute of Technology'
                   target='caltech' title='California Institute of Technology'><img
                    alt='Icon_caltech' src='footer/icon_caltech-new.png'/></a>
                <a href='http://www.jpl.nasa.gov/' alt='Jet Propulsion Laboratory'
                   target='jpl' title='Jet Propulsion Laboratory'><img alt='Icon_jpl'
                                                                       src='footer/icon_jpl-white-91x60.png'/></a>
                <a href='http://www.nasa.gov/'
                   alt='National Aeronautics and Space Administration' target='nasa'
                   title='National Aeronautics and Space Administration'><img
                    alt='Icon_nasa' src='footer/icon_nasa-white-59x60.png'/></a>
            </div>
        </div>
    );
};

const Alerts = (props) => {
    return (
        <div id="region-alerts" aria-hidden="true" style="width: 100%; height: 100%; display: none;">
            <div align="left" style="width: 100%; height: 100%;"></div>
        </div>
    );
};
