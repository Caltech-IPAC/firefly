/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import {omit} from 'lodash';
import {flux} from '../../core/ReduxFlux.js';
import shallowequal from 'shallowequal';
import {LcImageToolbarView} from './LcImageToolbarView.jsx';
import {getTblById} from '../../tables/TableUtil.js';

export class LcImageToolbar extends Component {

    constructor(props) {
        super(props);
        this.state= {activeTable : getTblById(this.props.tableId)};
    }

    shouldComponentUpdate(np,ns) {
        const {props,state}= this;
        const om= ['visRoot'];
        var update= !shallowequal(omit(props,om), omit(np,om)) || !shallowequal(ns,state);
        if (update) return true;

        return (props.visRoot.activePlotId!==np.visRoot.activePlotId ||
                props.visRoot.wcsMatchType!==np.visRoot.wcsMatchType);
    }


    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.removeListener) this.removeListener();
    }
    
    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        if (!this.iAmMounted) return;
        const {tableId}= this.props;
        const activeTable = tableId && getTblById(this.props.tableId);

        if (activeTable) {   // no active table in case reload a new file
            this.setState(activeTable);
        }

    }

    render() {
        const {viewerId, tableId, closeFunc}= this.props;
        return (
            <LcImageToolbarView viewerId={viewerId} tableId={tableId} closeFunc={closeFunc} />
        );
    }
}

LcImageToolbar.propTypes= {
    visRoot : PropTypes.object,
    viewerId : PropTypes.string.isRequired,
    tableId: PropTypes.string,
    closeFunc : PropTypes.func
};
