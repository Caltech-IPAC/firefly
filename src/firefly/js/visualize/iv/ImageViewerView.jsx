/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import ReactDOM from 'react-dom';
import sCompare from 'react-addons-shallow-compare';
import debounce from 'lodash/debounce';
import Resizable from 'react-component-resizable';
import {ImageViewerDecorate} from './ImageViewerDecorate.jsx';
import {dispatchZoom} from '../ImagePlotCntlr.js';
import {flux} from '../../Firefly.js';

const style= {
    width: '100%',
    height: '100%',
    overflow:'hidden',
    position:'relative'
};


export class ImageViewerView extends Component {

    constructor(props) {
        super(props);
        this.plotDrag= null;
        this.onResize = debounce( (size) => {
            if (size) {
                var {width,height}= size;
                //console.log(`width=${width}, height=${height}`)
                this.previousDim= {prevWidth:width,prevHeight:height};
                this.setState({ width,height });
            }
        }, 300);
        this.state={width: 0,height:0};
    }

    render() {
        var {width,height}= this.state;

        var {plotView,visRoot,drawLayersAry,mousePlotId, extensionList}= this.props;
        return (
            <Resizable id='imageViewerResizer' onResize={this.onResize} style={style}>
                <ImageViewerDecorate plotView={plotView}
                                     drawLayersAry={drawLayersAry}
                                     visRoot={visRoot}
                                     mousePlotId={mousePlotId}
                                     extensionList={extensionList}
                                     width={width} height={height} />
            </Resizable>
        );
    }
}


ImageViewerView.propTypes= {
    plotView : PropTypes.object.isRequired,
    drawLayersAry: PropTypes.array.isRequired,
    visRoot: PropTypes.object.isRequired,
    extensionList : PropTypes.array.isRequired,
    mousePlotId : PropTypes.string
};



