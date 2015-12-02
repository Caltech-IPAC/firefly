/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import PlotViewUtil from '../PlotViewUtil.js';
import ImageViewerDecorate from './ImageViewerDecorate.jsx';
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
        var allPlots= PlotViewUtil.getAllPlots();
        if (allPlots!==this.state.allPlots) {
            var plotView= PlotViewUtil.getPlotViewById(this.props.plotId);
            this.setState({plotView,allPlots:PlotViewUtil.getAllPlots()});
        }
    },



    render() {
        var {plotView,allPlots}= this.state;
        if (plotView) {
            return (
                <ImageViewerDecorate plotView={plotView} allPlots={allPlots}/>
            );
        }
        else {
            return false;
        }
    }



});

export default ImageViewer;
