/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes, Component} from 'react';
//import Resizable from 'react-component-resizable';
//import debounce from 'lodash/function/debounce';
import numeral from 'numeral';
import {ExpandedSingleView}  from './ExpandedSingleView.jsx';
import {ExpandedGridView}  from './ExpandedGridView.jsx';
import {RotateType}  from '../PlotState.js';
import {visRoot, ExpandType} from '../ImagePlotCntlr.js';
import {convertZoomToString} from '../ZoomUtil.js';
import {VisCtxToolbarView} from './../ui/VisCtxToolbarView.jsx';
import {VisInlineToolbarView} from './../ui/VisInlineToolbarView.jsx';
import PlotViewUtil from '../PlotViewUtil.js';
import {AnnotationOps} from '../WebPlotRequest.js';
import BrowserInfo from '../../util/BrowserInfo.js';
import {AREA_SELECT,LINE_SELECT,POINT} from '../PlotCmdExtension.js';
import {getTaskCount} from '../../core/AppDataCntlr.js';
import './ImageViewerDecorate.css';

export class ExpandedModeDisplayView extends Component {

    constructor(props) {
        super(props);
    }

    render() {
        //var {width, height} = this.state;
        var {allPlots}= this.props;
        if (allPlots.expandedMode===ExpandType.COLLAPSE) return <div></div>;
        return (
            allPlots.expandedMode===ExpandType.GRID ?
                <ExpandedGridView allPlots={allPlots}/> :
                <ExpandedSingleView allPlots={allPlots}/>
        );
    }
}
//<Resizable id='expandedViewResizer' style={{width, height}} onResize={this.onResize} >
//</Resizable>

//{makeInlineTitle(visRoot,pv)}

ExpandedModeDisplayView.propTypes= {
    allPlots : PropTypes.object.isRequired
};

