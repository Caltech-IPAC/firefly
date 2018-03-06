import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import './ModalDialog.css';
import {PopupPanel} from './PopupPanel.jsx';

import Draggable from 'react-draggable';
export class ModalDialog extends PureComponent {
    constructor(props) {
        super(props);
        this.state = {
            width: getDocWidth(),
            height: getDocHeight()
        };
        this.browserResizeCallback = () => {
            if (!this.isUnmounted) {
                this.setState({width: getDocWidth(), height: getDocHeight()});
            }
        };
    }

    componentDidMount() {
        window.addEventListener('resize', this.browserResizeCallback);
    }

    componentWillUnmount() {
        this.isUnmounted = true;
        window.removeEventListener('resize', this.browserResizeCallback);
    }

    render() {
        const {width, height} = this.state;
        // make sure the modal fits into the viewport
        const wrapperStyle = {maxWidth: width, maxHeight: height, overflow: 'auto', resize:'vertical'};

        return (
            <div  style={{zIndex: this.props.zIndex}}>
                <div className='ModalDialog'>
                    <div className='ModalDialog__content' style={wrapperStyle}>
                        {this.props.children}
                    </div>
                </div>
            </div>

        );
    }
}

ModalDialog.propTypes= {
    zIndex : PropTypes.number
};

function getDocHeight() {
    return window.innerHeight;
}

function getDocWidth() {
    return window.innerWidth;
}