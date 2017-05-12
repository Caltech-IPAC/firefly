import './ChartPanel.css';
import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import Resizable from 'react-component-resizable';
import {flux} from '../../Firefly.js';
import {get, debounce, defer} from 'lodash';
import {PlotlyWrapper} from './PlotlyWrapper.jsx';

import * as ChartsCntlr from '../ChartsCntlr.js';
import {getOptionsUI} from '../ChartUtil.js';
import {dispatchChartUpdate, dispatchChartHighlighted, getChartData} from '../ChartsCntlr.js';
import {SimpleComponent} from '../../ui/SimpleComponent.jsx';
import {getToolbarUI} from '../ChartUtil.js';

export class PlotlyChartPanel extends SimpleComponent {

    getNextState() {
        const {activeTrace} = ChartsCntlr.getChartData(this.props.chartId) || {};
        return {activeTrace};
    }

    render() {
        const {chartId, showToolbar=true, expandable} = this.props;
        const  {activeTrace=0} = this.state;
        const ToolbarUI = getToolbarUI(chartId, activeTrace);
        if (showToolbar) {
            return (
                <div className='ChartPanel__container'>
                    <ToolbarUI {...{chartId, expandable}}/>
                    <div style={{flexGrow: 1, position: 'relative'}}>
                        <div style={{position: 'absolute', top:3, bottom:3, left:1, right:1, border: 'solid 1px rgba(0,0,0,.2)'}}>
                            <ChartArea {...{chartId}}/>
                        </div>
                    </div>
                </div>
            );
        } else {
            return <ChartArea {...{chartId}}/>;
        }
    }
}

PlotlyChartPanel.propTypes = {
    chartId: PropTypes.string.isRequired,
    expandable: PropTypes.bool,
    expandedMode: PropTypes.bool,
    deletable: PropTypes.bool,
    showToolbar: PropTypes.bool
};

export class ChartArea extends PureComponent {

    constructor(props) {
        super(props);
        this.state = this.getNextState();
        this.afterRedraw = this.afterRedraw.bind(this);

        const normal = (size) => {
            if (size && !this.isUnmounted) {
                var widthPx = size.width;
                var heightPx = size.height;

                if (widthPx !== this.state.widthPx || heightPx !== this.state.heightPx) {
                    this.setState({widthPx, heightPx});
                }
            }
        };
        this.onResize = (size) => {
            defer(normal, size);
        };
    }

    componentDidMount() {
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.isUnmounted=true;
        this.removeListener && this.removeListener();
    }

    getNextState() {
        const {chartId} = this.props;
        const {data, highlighted, layout, selected, showOptions} = ChartsCntlr.getChartData(chartId);
        return  {data, highlighted, selected, layout, showOptions};
    }

    storeUpdate() {
        if (!this.isUnmounted) {
            this.setState(this.getNextState());
        }
    }

    afterRedraw(chart, pl) {
        const {chartId} = this.props;
        chart.on('plotly_click', onClick(chartId));
        chart.on('plotly_selected', onSelect(chartId));
    }

    render() {
        const {chartId} = this.props;
        const {data=[], highlighted, selected, layout, showOptions, widthPx, heightPx} = this.state;
        const layoutDim=  {width: widthPx, height: heightPx};


        // if (layout && (!layout.width || !layout.height)) Object.assign(layout, layoutDim);
        const handleAsResize= (layout && (layout.width!==widthPx || layout.height!==heightPx));
        Object.assign(layout,layoutDim);


        const OptionsUI = getOptionsUI(chartId);
        const showlegend = data.length > 1;
        let pdata = data;
        pdata = selected ? pdata.concat([selected]) : pdata;
        pdata = highlighted ? pdata.concat([highlighted]) : pdata;
        const playout = Object.assign({showlegend}, layout);

        return (
            <Resizable className='ChartPanel__chartresizer' onResize={this.onResize}>
                <PlotlyWrapper newPlotCB={this.afterRedraw} data={pdata} layout={playout}
                               handleRenderAsResize={handleAsResize}/>
                {showOptions && <OptionsUI {...{chartId}}/>}
            </Resizable>
        );
    }
}

function onClick(chartId) {
    return (evData) => {
        const {activeTrace=0}  = ChartsCntlr.getChartData(chartId);
        const highlighted = get(evData.points.find( (p) => p.curveNumber === activeTrace), 'pointNumber');
        dispatchChartHighlighted({chartId, activeTrace, highlighted});
    };
}

function onSelect(chartId) {
    return (evData) => {
        // this is for range selection only... lasso selection is not implemented yet.
        const {activeTrace=0}  = ChartsCntlr.getChartData(chartId);
        const [xMin, xMax] = get(evData, 'range.x', []);
        const [yMin, yMax] = get(evData, 'range.y', []);
        const points = get(evData, 'points', []).filter((o) => {
            return o.curveNumber === activeTrace;
        }).map((o) => {
            return o.pointNumber;
        });

        dispatchChartUpdate({chartId, changes:{'selection': {points, range: {x:[xMin, xMax], y: [yMin, yMax]}}}});
    };
}