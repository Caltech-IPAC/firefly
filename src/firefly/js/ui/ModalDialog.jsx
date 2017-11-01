import React, {PureComponent} from 'react';
import './ModalDialog.css';

export class ModalDialog extends PureComponent {
    constructor(props) {
        super(props);
        this.state = {
            width: getDocWidth(),
            height: getDocHeight(),
            scrollY: window.scrollY
        };
    }

    componentDidMount() {
        this.browserResizeCallback = () => { this.setState({width: getDocWidth(), height: getDocHeight()}); };
        window.addEventListener('resize', this.browserResizeCallback);
    }

    componentWillUnmount() {
        window.removeEventListener('resize', this.browserResizeCallback);
    }

    render() {
        const {width, height, scrollY} = this.state;

        return (
            <div className='ModalWindow' style={{width, height}}>
                <div className='ModalDialog' style={{top: scrollY}}>
                    <div className='ModalDialog__content'>
                        {this.props.children}
                    </div>
                </div>
            </div>
        );
    }
}

function getDocHeight() {
    let appEl = document.querySelector('div#App.rootStyle');
    if (!appEl) {
        appEl = document.body;
    }
    return Math.max(appEl.clientHeight, appEl.scrollHeight);
}

function getDocWidth() {
    let appEl = document.querySelector('div#App.rootStyle');
    if (!appEl) {
        appEl = document.body;
    }
    return Math.max(appEl.clientWidth, appEl.scrollWidth);
}