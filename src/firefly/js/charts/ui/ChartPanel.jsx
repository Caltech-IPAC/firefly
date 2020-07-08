import './ChartPanel.css';
import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {wrapResizer} from '../../ui/SizeMeConfig.js';
import {flux} from '../../core/ReduxFlux.js';
import {get, isEmpty, isUndefined} from 'lodash';
import {singleTraceUI} from '../ChartUtil.js';
import {PlotlyChartArea} from './PlotlyChartArea.jsx';
import {PlotlyToolbar} from './PlotlyToolbar.jsx';
import {dispatchChartMounted, dispatchChartRemove, dispatchChartUnmounted, getChartData, getErrors} from '../ChartsCntlr.js';

import DELETE from 'html/images/blue_delete_10x10.png';

class ChartPanelView extends PureComponent {

    constructor(props) {
        super(props);
    }


    componentDidMount() {
        const {chartId, showChart} = this.props;
        if (showChart) {
            dispatchChartMounted(chartId);
        }
    }

    componentWillUnmount() {
        const {chartId, showChart} = this.props;
        if (showChart) {
            dispatchChartUnmounted(chartId);
        }
    }

    render() {
        const {chartId, chartData, deletable:deletableProp, expandable, expandedMode, Toolbar, showToolbar, showChart, glass} = this.props;
        const deletable = isUndefined(get(chartData, 'deletable')) ? deletableProp : get(chartData, 'deletable');
        const showMultiTrace = !singleTraceUI();

        if (isEmpty(chartData) || isUndefined(Toolbar)) {
            return (
                <div/>
            );
        }

        // var {widthPx, heightPx} = this.state;
        const errors  = getErrors(chartId);

        if (showChart) {
            // chart with toolbar
            if (showToolbar) {
                return (
                    <div className='ChartPanel__container'>
                        <div className='ChartPanel__wrapper'>
                            <Toolbar {...{chartId, expandable, expandedMode, showMultiTrace}}/>
                            <div className='ChartPanel__chartarea--withToolbar'>
                                <ResizableChartArea
                                    {...Object.assign({}, this.props, {errors})} />
                                {glass && <div className='ChartPanel__chartarea--withToolbar ChartPanel__glass'/>}
                                { deletable &&
                                <img style={{display: 'inline-block', position: 'absolute', top: 29, right: 0, alignSelf: 'baseline', padding: 2, cursor: 'pointer'}}
                                     title='Delete this chart'
                                     src={DELETE}
                                     onClick={(ev) => {dispatchChartRemove(chartId); stopPropagation(ev);}}
                                />}
                            </div>
                        </div>
                    </div>
                );
            } else {
                // chart only
                return (
                    <div className='ChartPanel__container'>
                        <div className='ChartPanel__chartarea'>
                            <ResizableChartArea
                                {...Object.assign({}, this.props, {errors})} />
                            {glass && <div className='ChartPanel__chartarea ChartPanel__glass'/>}
                            {deletable &&
                            <img style={{display: 'inline-block', position: 'absolute', top: 0, right: 0, alignSelf: 'baseline', padding: 2, cursor: 'pointer'}}
                                 title='Delete this chart'
                                 src={DELETE}
                                 onClick={(ev) => {dispatchChartRemove(chartId); stopPropagation(ev);}}
                            />}
                        </div>
                    </div>
                );
            }
        } else {
            // toolbar only
            return (
                <div className='ChartPanel__chartarea'>
                    <Toolbar {...{chartId, expandable, expandedMode, showMultiTrace}}/>
                </div>
            );
        }
    }
}

ChartPanelView.propTypes = {
    chartId: PropTypes.string.isRequired,
    chartData: PropTypes.object,
    deletable: PropTypes.bool,
    expandable: PropTypes.bool,
    expandedMode: PropTypes.bool,
    showToolbar: PropTypes.bool,
    showChart: PropTypes.bool,
    glass: PropTypes.bool, // for non-interactive chart area (when toolbar is missing)
    Chart: PropTypes.func,
    Toolbar: PropTypes.func
};

ChartPanelView.defaultProps = {
    showToolbar: true,
    showChart: true,
    Chart: PlotlyChartArea,
    Toolbar: PlotlyToolbar
};


class ResizableChartAreaInternal extends PureComponent {
    render() {
        const {errors, Chart}= this.props;
        const {width:widthPx, height:heightPx}= this.props.size;
        const knownSize = widthPx && heightPx;
        return (
            <div id='chart-resizer' className='ChartPanel__chartresizer'>
                {knownSize ?
                    errors.length > 0 || isUndefined(Chart) ?
                        <ErrorPanel errors={errors}/> :
                        <Chart {...Object.assign({}, this.props, {widthPx, heightPx})}/> :
                    <div/>}
            </div>
        );

    }
}

const ResizableChartArea= wrapResizer(ResizableChartAreaInternal);

const stopPropagation= (ev) => ev.stopPropagation();

function ErrorPanel({errors}) {
    return (
      <div style={{position: 'relative', width: '100%', height: '100%'}}>
          {errors.map((error, i) => {
              const {message='Error', reason=''} = error;
                    return (
                        <div key={i} style={{padding: 20, textAlign: 'center', overflowWrap: 'normal'}}>
                            <h3>{message}</h3>
                            {`${reason}`}
                        </div>
                    );
              })}
      </div>
    );
}

ErrorPanel.propTypes = {
    errors: PropTypes.arrayOf(
        PropTypes.shape({
            message: PropTypes.string,
            reason: PropTypes.oneOfType([PropTypes.object,PropTypes.string]) // reason can be an Error object
        }))
};

export class ChartPanel extends PureComponent {

    constructor(props) {
        super(props);
        this.state = this.getNextState();
        this.test = props;
    }

    componentDidMount() {
        this.removeListener = flux.addListener(() => this.storeUpdate());
        this.iAmMounted = true;
    }

    componentWillUnmount() {
        this.iAmMounted=false;
        this.removeListener && this.removeListener();
    }

    getNextState() {
        const {chartId} = this.props;
        const chartData =  getChartData(chartId);
        if (!isEmpty(chartData)) {

            return {
                chartData
            };
        } else {
            return {};
        }
    }

    storeUpdate() {
        if (this.iAmMounted) {

            if (getChartData(this.props.chartId) !== this.state.chartData) {
                this.setState(this.getNextState());
            }
        }
    }

    render() {
        return (
            <ChartPanelView key={this.props.chartId} {...this.props} {...this.state}/>
        );
    }
}

ChartPanel.propTypes = {
    chartId: PropTypes.string.isRequired,
    expandable: PropTypes.bool,
    expandedMode: PropTypes.bool,
    deletable: PropTypes.bool,
    showToolbar: PropTypes.bool,
    showChart: PropTypes.bool
};

