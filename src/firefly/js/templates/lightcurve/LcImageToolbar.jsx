/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import {omit} from 'lodash';
import {flux} from '../../Firefly.js';
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
    
    componentWillMount() {
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
        const {visRoot, viewerId, viewerPlotIds, layoutType, dlAry, tableId, closeFunc}= this.props;
        return (
            <LcImageToolbarView activePlotId={visRoot.activePlotId} viewerId={viewerId}
                                      viewerPlotIds={viewerPlotIds} layoutType={layoutType} dlAry={dlAry}
                                      tableId={tableId} closeFunc={closeFunc}
                                        /> 
        );
    }
}

LcImageToolbar.propTypes= {
    dlAry : PropTypes.arrayOf(React.PropTypes.object),
    visRoot : PropTypes.object,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string.isRequired,
    tableId: PropTypes.string,
    viewerPlotIds : PropTypes.arrayOf(React.PropTypes.string),
    closeFunc : PropTypes.func
};
