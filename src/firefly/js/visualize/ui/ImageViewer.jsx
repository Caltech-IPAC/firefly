/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import PlotViewUtil from '../PlotViewUtil.js';
import ImageViewerView from './ImageViewerView.jsx';
import {flux} from '../../Firefly.js';






var ImageViewer= React.createClass(
{


    mixins : [PureRenderMixin],

    storeListenerRemove : null,


    propTypes: {
        plotId : React.PropTypes.string.isRequired
    },

    getInitialState() {
        return {plotView:PlotViewUtil.getPlotViewById(this.props.plotId)};
    },


    componentWillUnmount() {
        if (this.formStoreListenerRemove) this.formStoreListenerRemove();
    },


    componentDidMount() {
        this.formStoreListenerRemove= flux.addListener(this.storeUpdate);
    },

    storeUpdate() {
        var plotView= PlotViewUtil.getPlotViewById(this.props.plotId);
        if (plotView!==this.state.plotView) {
            this.setState({plotView});
        }
    },



    render() {
        if (this.state.plotView) {
            return (
                <ImageViewerView plotView={this.state.plotView}/>
            );
        }
        else {
            return false;
        }
    }



});

export default ImageViewer;
