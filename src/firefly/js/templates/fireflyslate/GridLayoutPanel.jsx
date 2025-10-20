/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import {isEmpty} from 'lodash';
import {wrapResizeMonitor} from '../../ui/ResizeMonitor';
import {clone} from '../../util/WebUtil.js';
import GridLayout from 'react-grid-layout';
import {MultiImageViewer} from '../../visualize/ui/MultiImageViewer.jsx';
import {MetaDataMultiProductViewer} from '../../visualize/ui/multiProduct/MetaDataMultiProductViewer.jsx';
import {MultiViewStandardToolbar} from '../../visualize/ui/MultiViewStandardToolbar.jsx';
import {MultiChartViewer} from '../../charts/ui/MultiChartViewer.jsx';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {LO_VIEW, getGridDim, dispatchUpdateGridView} from '../../core/LayoutCntlr.js';
import {NewPlotMode} from '../../visualize/MultiViewCntlr.js';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {getActiveTableId} from '../../tables/TableUtil.js';

import './react-grid-layout_styles.css';
import './react-resizable_styles.css';

const CV= 1;
const CELL_MARGIN=2;



class SlateView extends Component {


    constructor(props) {
        super(props);
        this.renderedLayoutUpdated= this.renderedLayoutUpdated.bind(this);
        this.dragStart= this.dragStart.bind(this);

        this.doCollapse= false;
        this.renderedGridView= props.gridView;
        this.rowHeight= 1;
    }

    shouldComponentUpdate(nP) {
        const {gridView, size:{width,height}}= this.props;
        if (this.renderedGridView.length!==nP.gridView.length) {
            this.doCollapse= false;
        }
        if (!nP.size.width && !nP.size.height) return false;
        return (nP.gridView!==gridView ||
                nP.size.width!==width ||
                nP.size.height!==height
                || this.renderedGridView!==nP.gridView);
    }

    renderedLayoutUpdated(layout) {

        const {gridView}= this.props;
        const {renderTreeId}= this.context;

        const nextGridView= gridView.map( (entry) => {
            const l= layout.find( (le) => le.i===entry.cellId);
            return l ? clone(entry, {row: l.y/CV, col: l.x/CV, width: l.w/CV, height: l.h/CV}) : entry;
        });

        //-------
        // todo dispatch the new view
        dispatchUpdateGridView(nextGridView, renderTreeId);
    }

    dragStart() {
        this.doCollapse= true;
    }

    render() {
        const {gridView,size:{width,height}, gridColumns}= this.props;
        this.renderedGridView= gridView;

        if (isEmpty(gridView) || width<10 || height<10) return <div  style={{flex: '1 1 auto', overflow: 'auto'}}/>;

        const sourceGridDim= getGridDim(gridView);
        const maxSize= getGridDim(gridView, CV);
        const normalLayout= makeNormalLayout(gridView);

        if (!this.doCollapse) {
            this.rowHeight= sourceGridDim.rows<=2 ? Math.max(((height-25)/maxSize.rows), 5) : Math.max(height/(maxSize.rows+.5), 5);
        }
        console.log(`GridLayoutPanel, Render: doCollapse: ${this.doCollapse}, w: ${width}, h:${height}, row Height:${this.rowHeight}, rows: ${maxSize.rows}`);

        return (
            <div className= 'GridLayoutPanel' style={{flex: '1 1 auto', overflow: 'auto'}}>
                <GridLayout width={width-1} cols={gridColumns} rowHeight={this.rowHeight}
                            layout={normalLayout} margin={[CELL_MARGIN,CELL_MARGIN]}
                            onLayoutChange={this.renderedLayoutUpdated} onDragStart={this.dragStart} >
                    {gridView.map ( makeComponent)}
                </GridLayout>
            </div>
        );

    }
}


SlateView.contextType= RenderTreeIdCtx;

SlateView.propTypes= {
    gridView : PropTypes.array.isRequired,
    size: PropTypes.object.isRequired,
    gridColumns : PropTypes.number.isRequired
};


export const GridLayoutPanel= wrapResizeMonitor(SlateView);

GridLayoutPanel.propTypes= {
    gridView : PropTypes.array.isRequired,
    gridColumns : PropTypes.number.isRequired,
};



function makeNormalLayout(gridView) {
    return gridView.map ( (c) => ({ x : c.col*CV, y : c.row*CV, w : c.width*CV, h : c.height*CV,
                                    i : c.cellId, minW:CV, minH:CV }));
}


function makeComponent(g) {




    switch (g.type) {
        case LO_VIEW.tableImageMeta:
            return (
                <div key={g.cellId} style={{flex: 'auto', display: 'flex'}} >
                    <MetaDataMultiProductViewer key={g.cellId}
                                        viewerId={g.cellId}
                                        dataProductTableId={getActiveTableId()}
                                        enableExtraction={true}/>
                </div>
            );
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
        case LO_VIEW.images:
            return (
                <div key={g.cellId} style={{flex: 'auto', display: 'flex'}} >
                    <MultiImageViewer key={g.cellId} viewerId= {g.cellId}
                                      insideFlex={true} Toolbar={MultiViewStandardToolbar}
                                      toolbarStyle={{marginTop:0,paddingBottom:0}}
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
    }
}

