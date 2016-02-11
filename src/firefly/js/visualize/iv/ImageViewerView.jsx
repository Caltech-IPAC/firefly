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
        this.previousDim= {prevWidth:0,prevHeight:0};
        this.state={width: 0,height:0};
        //this.timerId= null;
        this.onResize = (size) => {
            if (size) {
                var {width,height}= size;
                var {prevWidth,prevHeight}= this.previousDim;
                if (prevWidth===0 || prevHeight===0) { //the first reset should set right away, otherwise debounce
                    this.setState({ width,height });
                    this.previousDim= {prevWidth:width,prevHeight:height};
                }
                else {
                    this.onResizeDebounced(size);
                }
            }
        };


        this.onResizeDebounced = debounce( (size) => {
            if (size) {
                var {width,height}= size;
                this.setState({ width,height });
                this.previousDim= {prevWidth:width,prevHeight:height};
            }
        }, 100);
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



