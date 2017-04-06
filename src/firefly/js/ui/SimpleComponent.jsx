import React, {Component} from 'react';
import sCompare from 'react-addons-shallow-compare';

import {flux} from '../Firefly.js';

export class SimpleComponent extends Component {
    constructor(props) {
        super(props);
        this.state = this.getNextState();
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }
    componentWillReceiveProps(np) {
        if (!this.isUnmounted) {
            this.setState(this.getNextState(np));
        }
    }
    componentDidMount() {
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }
    componentWillUnmount() {
        this.isUnmounted=true;
        this.removeListener && this.removeListener();
    }

    getNextState(np) {
        return {};      // need to implement
    }

    storeUpdate() {
        if (!this.isUnmounted) {
            this.setState(this.getNextState());
        }
    }
}
