import React, {PureComponent} from 'react';
import './ModalDialog.css';

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
        const wrapperStyle = {maxWidth: width, maxHeight: height, overflow: 'auto'};
        return (
            <div className='ModalWindow'>
                <div className='ModalDialog'>
                    <div className='ModalDialog__content' style={wrapperStyle}>
                        {this.props.children}
                    </div>
                </div>
            </div>
        );
    }
}

function getDocHeight() {
    return window.innerHeight;
}

function getDocWidth() {
    return window.innerWidth;
}