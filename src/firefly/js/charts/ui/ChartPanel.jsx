import './ChartPanel.css';
import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {wrapResizer} from '../../ui/SizeMeConfig.js';
import {flux} from '../../Firefly.js';
import {get, isEmpty, isUndefined} from 'lodash';
import {reduxFlux} from '../../core/ReduxFlux.js';
import {singleTraceUI} from '../ChartUtil.js';
import {dispatchChartUpdate, dispatchChartMounted, dispatchChartRemove, dispatchChartUnmounted, getChartData, getErrors} from '../ChartsCntlr.js';

import DELETE from 'html/images/blue_delete_10x10.png';

class ChartPanelView extends PureComponent {

    constructor(props) {
        super(props);
        //this.state = {};
        this.toggleOptions = this.toggleOptions.bind(this);
    }

    toggleOptions(key) {
        const {chartId, showOptions, optionsKey} = this.props;
        if (key === optionsKey) {
            const newShowOptions = !showOptions;
            const newKey = newShowOptions ? key : undefined;
            dispatchChartUpdate({chartId, changes: {showOptions: newShowOptions, optionsKey: newKey}});
        } else if (key) {
            dispatchChartUpdate({chartId, changes: {showOptions: true, optionsKey: key}});
        } else {
            dispatchChartUpdate({chartId, changes: {showOptions: false, optionsKey: key}});
        }
    }

    componentDidMount() {
        const {chartId, showChart} = this.props;
        if (showChart) {
            dispatchChartMounted(chartId);
        }
        this.iAmMounted = true;
    }

    componentWillReceiveProps(nextProps) {
        const {chartId, showChart} = nextProps;
        if (!chartId) { return; }

        if (chartId !== this.props.chartId) {
            if (this.props.showChart) { dispatchChartUnmounted(this.props.chartId); }
            if (showChart) { dispatchChartMounted(chartId); }
        }
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        const {chartId, showChart} = this.props;
        if (showChart) {
            dispatchChartUnmounted(chartId);
        }
    }

    render() {
        const {chartId, deletable:deletableProp, expandable, expandedMode, Options, Toolbar, showToolbar, showChart, showOptions, optionsKey} = this.props;
        const chartData =  getChartData(chartId);
        const deletable = isUndefined(get(chartData, 'deletable')) ? deletableProp : get(chartData, 'deletable');
        const showMultiTrace = !singleTraceUI();

        if (isEmpty(chartData) || isUndefined(Toolbar) || isUndefined(Options)) {
            return (
                <div/>
            );
        }

        // var {widthPx, heightPx} = this.state;
        const errors  = getErrors(chartId);

        //console.log(`${chartId}: ${showChart}, ${showToolbar}, ${showOptions}`);

        if (showChart) {
            // chart with toolbar and options
            if (showToolbar) {
                return (
                    <div className='ChartPanel__container'>
                        <div className='ChartPanel__wrapper'>
                            <Toolbar {...{chartId, expandable, expandedMode, showMultiTrace, toggleOptions: this.toggleOptions}}/>
                            <div className='ChartPanel__chartarea--withToolbar'>
                                {showOptions &&
                                <div className='ChartPanelOptions'>
                                    <div style={{height: 14}}>
                                        <div style={{ right: -6, float: 'right'}}
                                             className='btn-close'
                                             title='Remove Panel'
                                             onClick={() => this.toggleOptions()}/>
                                    </div>
                                    <Options {...{chartId, optionsKey}}/>
                                </div>}
                                <ResizableChartArea
                                    {...Object.assign({}, this.props, {errors})} />
                                { !showOptions && deletable &&
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
            // toolbar and options
            return (
                <div className='ChartPanel__chartarea'>
                    <Toolbar {...{chartId, expandable, expandedMode, showMultiTrace, toggleOptions: this.toggleOptions}}/>
                    <div className='ChartPanel__chartarea--withToolbar'>
                        {showOptions &&
                        <div className='ChartPanelOptions'
                             onClick={stopPropagation}
                             onTouchStart={stopPropagation}
                             onMouseDown={stopPropagation}>
                            <div style={{height: 14}}>
                                <div style={{ right: -6, float: 'right'}}
                                     className='btn-close'
                                     title='Remove Panel'
                                     onClick={() => this.toggleOptions()}/>
                            </div>
                            <Options {...{chartId, optionsKey}}/>
                        </div>
                        }
                    </div>
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
    showOptions: PropTypes.bool,
    optionsKey: PropTypes.string,
    showToolbar: PropTypes.bool,
    showChart: PropTypes.bool,
    Chart: PropTypes.func,
    Options: PropTypes.func,
    Toolbar: PropTypes.func
};

ChartPanelView.defaultProps = {
    showToolbar: true,
    showChart: true
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
            const errors = getErrors(chartId);
            const {chartType, activeTrace, showOptions, optionsKey} = chartData;
            //if (chartType === 'plot.ly') return {};
            const {Chart,Options,Toolbar,getChartProperties=()=>{},updateOnStoreChange} = reduxFlux.getChartType(chartType) || {};
            return {
                chartData, activeTrace, showOptions, optionsKey, errors, ...getChartProperties(chartId),
                Chart,
                Options,
                Toolbar,
                updateOnStoreChange
            };
        } else {
            return {};
        }
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const {updateOnStoreChange} = this.state;
            if (!updateOnStoreChange) {
                if (getChartData(this.props.chartId) !== this.state.chartData) {
                    this.setState(this.getNextState());
                }
            } else if (updateOnStoreChange(this.state)) {
                    this.setState(this.getNextState());
            }  else {
                const {showOptions, optionsKey} = getChartData(this.props.chartId);
                if (showOptions !== this.state.showOptions || optionsKey !== this.state.optionsKey) {
                    this.setState({showOptions, optionsKey});
                }
            }
        }
    }

    render() {
        return (
            <ChartPanelView {...this.props} {...this.state}/>
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

