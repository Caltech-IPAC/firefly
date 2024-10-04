/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import {omit} from 'lodash';
import {flux} from '../../core/ReduxFlux.js';
import shallowequal from 'shallowequal';
import {ImageMetaDataToolbarView} from './ImageMetaDataToolbarView.jsx';
import {makeDataProductsConverter} from '../../metaConvert/DataProductsFactory.js';
import {getTblById} from '../../tables/TableUtil.js';
import {SINGLE} from '../MultiViewCntlr.js';

export class ImageMetaDataToolbar extends Component {

    constructor(props) {
        super(props);
        this.state= {activeTable : getTblById(this.props.tableId)};
    }

    shouldComponentUpdate(np,ns) {
        const {props,state}= this;
        const om= ['visRoot'];
        const update= !shallowequal(omit(props,om), omit(np,om)) || !shallowequal(ns,state);
        if (update) return true;
        return ((np.layoutType===SINGLE && props.visRoot.activePlotId!==np.visRoot.activePlotId));
    }


    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.removeListener) this.removeListener();
    }
    
    componentDidMount() {
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
        const {visRoot, viewerId, viewerPlotIds, layoutType, dlAry, makeDropDown, serDef, factoryKey, enableCutout, pixelBasedCutout= false}= this.props;
        return (
            <ImageMetaDataToolbarView activePlotId={visRoot.activePlotId} viewerId={viewerId}  serDef={serDef}
                                      enableCutout={enableCutout} pixelBasedCutout={pixelBasedCutout}
                                      viewerPlotIds={viewerPlotIds} layoutType={layoutType} dlAry={dlAry}
                                      activeTable={activeTable} makeDataProductsConverter={makeDataProductsConverter}
                                      makeDropDown={makeDropDown} factoryKey={factoryKey} />
        );
    }
}

ImageMetaDataToolbar.propTypes= {
    dlAry : PropTypes.arrayOf(PropTypes.object),
    visRoot : PropTypes.object,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string.isRequired,
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired,
    tableId: PropTypes.string,
    factoryKey: PropTypes.string,
    makeDropDown: PropTypes.func,
    enableCutout: PropTypes.bool,
    pixelBasedCutout: PropTypes.bool,
    serDef: PropTypes.object
};
