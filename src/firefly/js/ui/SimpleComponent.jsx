import React, {PureComponent} from 'react';

import {flux} from '../Firefly.js';

export class SimpleComponent extends PureComponent {
    constructor(props) {
        super(props);
        this.state = this.getNextState();
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
