/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import {isEmpty} from 'lodash';
import sCompare from 'react-addons-shallow-compare';
import {flux} from '../../Firefly.js';
import {ImageMetaDataToolbarView} from './ImageMetaDataToolbarView.jsx';
import {converterFactory} from '../../metaConvert/ConverterFactory.js';
// import {getActiveTableId} from '../../core/LayoutCntlr.js';
import {findTblById, getActiveTableId} from '../../tables/TableUtil.js';
import {isMetaDataTable} from '../../metaConvert/converterUtils.js';

export class ImageMetaDataToolbar extends Component {

    constructor(props) {
        super(props);
        this.state= {activeTable : null};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }


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
            <ImageMetaDataToolbarView visRoot={visRoot} viewerId={viewerId}
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
