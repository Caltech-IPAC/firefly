/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {debounce, defer} from 'lodash';
import Resizable from 'react-component-resizable';
import {ImageViewerDecorate} from './ImageViewerDecorate.jsx';

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
        this.state={width: 0,height:0};

        const normal = (size) => {
            if (size) {
                this.setState(size);
            }
        };
        const debounced = debounce(normal, 100);
        this.onResize =  (size) => {
            if (this.state.width === 0 || this.state.height === 0) {  //the first reset should set right away, otherwise debounce
                defer(normal, size);
            } else {
                debounced(size);
            }
        };
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

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
    mousePlotId : PropTypes.string,
    handleInlineTools : PropTypes.bool
};



