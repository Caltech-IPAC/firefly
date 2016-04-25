/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import {isEmpty,omit} from 'lodash';
import {flux} from '../../Firefly.js';
import shallowequal from 'shallowequal';
import {ImageMetaDataToolbarView} from './ImageMetaDataToolbarView.jsx';
import {converterFactory} from '../../metaConvert/ConverterFactory.js';
import {findTblById, getActiveTableId} from '../../tables/TableUtil.js';
import {isMetaDataTable} from '../../metaConvert/converterUtils.js';
import {SINGLE} from '../MultiViewCntlr.js';

export class ImageMetaDataToolbar extends Component {

    constructor(props) {
        super(props);
        this.state= {activeTable : null};
    }

    shouldComponentUpdate(np,ns) {
        const {props,state}= this;
        const om= ['visRoot'];
        var update= !shallowequal(omit(props,om), omit(np,om)) || !shallowequal(ns,state);
        if (update) return true;

        return (np.layoutType===SINGLE && props.visRoot.activePlotId!==np.visRoot.activePlotId);
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
        const activeTable= findTblById(getActiveTableId());
        if (isMetaDataTable(getActiveTableId()) && activeTable!==this.state.activeTable) {
            this.setState({activeTable});
        }
    }

    render() {
        const {activeTable}= this.state;
        
        const {visRoot, viewerId, viewerPlotIds, layoutType}= this.props;
        return (
            <ImageMetaDataToolbarView activePlotId={visRoot.activePlotId} viewerId={viewerId}
                                      viewerPlotIds={viewerPlotIds} layoutType={layoutType}
                                      activeTable={activeTable} converterFactory={converterFactory}
                                        /> 
        );
    }
}

ImageMetaDataToolbar.propTypes= {
    visRoot : PropTypes.object,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string.isRequired,
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired
};
