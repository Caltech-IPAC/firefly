/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import {isEmpty,omit} from 'lodash';
import {flux} from '../../Firefly.js';
import shallowequal from 'shallowequal';
import {ImageMetaDataToolbarView} from './ImageMetaDataToolbarView.jsx';
import {converterFactory} from '../../metaConvert/ConverterFactory.js';
import {getTblById, getActiveTableId} from '../../tables/TableUtil.js';
import {isMetaDataTable} from '../../metaConvert/converterUtils.js';
import {SINGLE} from '../MultiViewCntlr.js';

export class ImageMetaDataToolbar extends Component {

    constructor(props) {
        super(props);
        this.state= {activeTable : getTblById(this.props.tableId)};
    }

    shouldComponentUpdate(np,ns) {
        const {props,state}= this;
        const om= ['visRoot'];
        var update= !shallowequal(omit(props,om), omit(np,om)) || !shallowequal(ns,state);
        if (update) return true;

        return ((np.layoutType===SINGLE && props.visRoot.activePlotId!==np.visRoot.activePlotId) ||
                props.visRoot.wcsMatchType!==np.visRoot.wcsMatchType);
    }


    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.removeListener) this.removeListener();
    }
    
    componentWillMount() {
        this.iAmMounted= true;
        this.removeListener= flux.addListener(() => this.storeUpdate(this.props));
    }

    storeUpdate() {
        if (!this.iAmMounted) return;
        const {tableId}= this.props;
        const activeTable= getTblById(tableId);
        if (activeTable!==this.state.activeTable) {
            this.setState({activeTable});
        }
    }

    render() {
        const {activeTable}= this.state;
        
        const {visRoot, viewerId, viewerPlotIds, layoutType, dlAry, tableId}= this.props;
        return (
            <ImageMetaDataToolbarView activePlotId={visRoot.activePlotId} viewerId={viewerId}
                                      wcsMatchType={visRoot.wcsMatchType}
                                      viewerPlotIds={viewerPlotIds} layoutType={layoutType} dlAry={dlAry}
                                      activeTable={activeTable} converterFactory={converterFactory}
                                        /> 
        );
    }
}

ImageMetaDataToolbar.propTypes= {
    dlAry : PropTypes.arrayOf(React.PropTypes.object),
    visRoot : PropTypes.object,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string.isRequired,
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired,
    tableId: PropTypes.string
};
