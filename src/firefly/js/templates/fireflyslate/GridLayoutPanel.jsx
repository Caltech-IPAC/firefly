/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {uniqBy, isEmpty} from 'lodash';
import {clone} from '../../util/WebUtil.js';
import sizeMe from 'react-sizeme';
import ReactGridLayout, {Responsive, WidthProvider} from 'react-grid-layout';
import {ImageMetaDataToolbar} from '../../visualize/ui/ImageMetaDataToolbar.jsx';
import {MultiImageViewer} from '../../visualize/ui/MultiImageViewer.jsx';
import {MultiViewStandardToolbar} from '../../visualize/ui/MultiViewStandardToolbar.jsx';
import {MultiChartViewer} from '../../charts/ui/MultiChartViewer.jsx';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {LO_VIEW, getGridDim, dispatchUpdateLayoutInfo} from '../../core/LayoutCntlr.js';
import {NewPlotMode} from '../../visualize/MultiViewCntlr.js';

import './react-grid-layout_styles.css';
import './react-resizable_styles.css';

const CV= 1;
const CELL_MARGIN=2;

 //Some of the docs from size me are included here
const config = {
  monitorWidth: true,
  monitorHeight: true,
  monitorPosition: false,

  // The maximum frequency, in milliseconds, at which size changes should be
  // recalculated when changes in your Component's rendered size are being
  // detected. This should not be set to lower than 16.
  refreshRate: 100,

  // The mode in which refreshing should occur.  Valid values are "debounce"
  // and "throttle".  "throttle" will eagerly measure your component and then
  // wait for the refreshRate to pass before doing a new measurement on size
  // changes. "debounce" will wait for a minimum of the refreshRate before
  // it does a measurement check on your component.  "debounce" can be useful
  // in cases where your component is animated into the DOM.
  // NOTE: When using "debounce" mode you may want to consider disabling the
  // placeholder as this adds an extra delay in the rendering time of your
  // component.
  refreshMode: 'throttle',   // 'throttle' or 'debounce'

  // By default we render a "placeholder" component initially so we can try
  // and "prefetch" the expected size for your component.  This is to avoid
  // any unnecessary deep tree renders.  If you feel this is not an issue
  // for your component case and you would like to get an eager render of
  // your component then disable the placeholder using this config option.
  // NOTE: You can set this globally. See the docs on first render.
  noPlaceholder: false
};


class SlateView extends PureComponent {


    SlateView(props) {
        this.state= {renderedLayout:{}};
        this.renderedLayoutUpdated= this.renderedLayoutUpdated.bind(this);

        this.doCollapse= false;
        this.renderedGridView= props.gridView;
        this.rowHeight= 1;
    }

    shouldComponentUpdate(nP) {
        const {gridView, size:{width,height}}= this.props;
        if (this.renderedGridView.length!==nP.gridView.length) {
            this.doCollapse= false;
        }
        return (nP.gridView!==gridView ||
                nP.size.width!==width ||
                nP.size.height!==height
                || this.renderedGridView!==nP.gridView);
    }

    renderedLayoutUpdated(layout) {
        console.log('onLayoutChange');
        console.log(layout);
        console.log(this.normalLayoutTMP);

        const {gridView}= this.props;

        const nextGridView= gridView.map( (entry) => {
            const l= layout.find( (le) => le.i===entry.cellId);
            return l ? clone(entry, {row: l.y/CV, col: l.x/CV, width: l.w/CV, height: l.h/CV}) : entry;
        });

        //-------
        // todo dispatch the new view
        dispatchUpdateLayoutInfo({gridView:nextGridView});
    }

    dragStart() {
        this.doCollapse= true;
    }

    dragStop() {
    }

    render() {
        const {gridView,size:{width,height}, gridColumns}= this.props;
        this.renderedGridView= gridView;


        if (isEmpty(gridView) || width<10 || height<10) return <div  style={{flex: '1 1 auto', overflow: 'auto'}}/>;

        const sourceGridDim= getGridDim(gridView);
        const maxSize= getGridDim(gridView, CV);
        const small= makeSmallLayout(gridView);
        const normalLayout= makeNormalLayout(gridView);

        // const rowHeight= height/maxSize.rows - CELL_MARGIN*maxSize.rows;
        if (!this.doCollapse) {
            this.rowHeight= sourceGridDim.rows<=2 ?
                Math.max(((height-25)/maxSize.rows), 5) :
                Math.max(height/(maxSize.rows+.5), 5);
        }

        const breakpoints= {xxs: 0, small: 400, normalLayout:600};
        const cols=        {xxs: CV, small:CV, normalLayout: gridColumns};
        const layouts=     {xxs: small, small, normalLayout};

        this.normalLayoutTMP= normalLayout;

        console.log(`GridLayoutPanel: w: ${width}, h:${height}, row Height:${this.rowHeight}, rows: ${maxSize.rows}`);

        return (
            <div className= 'GridLayoutPanel' style={{flex: '1 1 auto', overflow: 'auto'}}>
                <Responsive width={width-14} cols={cols} rowHeight={this.rowHeight}
                            verticalCompact={this.doCollapse}
                            breakpoints={breakpoints} layouts={layouts} margin={[CELL_MARGIN,CELL_MARGIN]}
                            onLayoutChange={(layout) => this.renderedLayoutUpdated(layout)}
                            onDragStart={(layout) => this.dragStart(layout)}
                            onDragStop={(layout) => this.dragStop(layout)}
                >
                    {gridView.map ( makeComponent)}
                </Responsive>
            </div>
        );

    }
    // onBreakpointChange={(newBreakpoint) => console.log(`bp: ${newBreakpoint}`)}
}


SlateView.propTypes= {
    gridView : PropTypes.array.isRequired,
    size: PropTypes.object.isRequired,
    gridColumns : PropTypes.number.isRequired
};

const sizeMeHOC = sizeMe(config);

export const GridLayoutPanel= sizeMeHOC(SlateView);

GridLayoutPanel.propTypes= {
    gridView : PropTypes.array.isRequired,
    gridColumns : PropTypes.number.isRequired
};






function makeNormalLayout(gridView) {
    return gridView.map ( (c) => ({ x : c.col*CV, y : c.row*CV, w : c.width*CV, h : c.height*CV,
                                    i : c.cellId, minW:CV, minH:CV }));
}

function makeSmallLayout(gridView) {
    return gridView
        .sort( (c1,c2) => c1.row===c2.row ? c1.col-c2.col : c1.row-c2.row)
        .map ( (c,idx) => ({ x : 0, y : idx*CV, w : CV, h : c.height*CV, i : c.cellId, minW:1}) );
}

function makeComponent(g) {




    switch (g.type) {
        case LO_VIEW.tableImageMeta:
            return (
                <div key={g.cellId} style={{flex: 'auto', display: 'flex'}} >
                    <MultiImageViewer key={g.cellId} viewerId= {g.cellId}
                                      insideFlex={true}
                                      canReceiveNewPlots={NewPlotMode.none.key}
                                      Toolbar={ImageMetaDataToolbar}/>
                </div>
            );
            break;
        case LO_VIEW.coverageImage:
            return (
                <div key={g.cellId} style={{flex: 'auto', display: 'flex'}} >
                    <MultiImageViewer key={g.cellId} viewerId= {g.cellId}
                                      insideFlex={true}
                                      canReceiveNewPlots={NewPlotMode.replace_only.key}
                                      alwaysShowToolbar={true}
                                      Toolbar={MultiViewStandardToolbar}/>
                </div>
            );
            break;
        case LO_VIEW.images:
            return (
                <div key={g.cellId} style={{flex: 'auto', display: 'flex'}} >
                    <MultiImageViewer key={g.cellId} viewerId= {g.cellId}
                                      insideFlex={true} Toolbar={MultiViewStandardToolbar}
                                      alwaysShowToolbar={true}/>
                </div>
            );
        case LO_VIEW.xyPlots:
            return (
                <div key={g.cellId} style={{flex: 'auto', display: 'flex'}} >
                    <MultiChartViewer key={g.cellId} closeable={true} viewerId={g.cellId} expandedMode={false}/>
                </div>
            );
        case LO_VIEW.tables:
            return (
                <div key={g.cellId} style={{flex: 'auto', display: 'flex'}} >
                    <TablesContainer key={g.cellId} mode='both' closeable={true} expandedMode={false} />
                </div>
            );
        default :
            return <div key={g.cellId} />;
            break;
    }
}

