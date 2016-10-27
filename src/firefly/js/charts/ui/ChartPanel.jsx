import './ChartPanel.css';
import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import Resizable from 'react-component-resizable';
import {flux} from '../../Firefly.js';
import {get, debounce, defer} from 'lodash';
import {reduxFlux} from '../../core/ReduxFlux.js';

import * as ChartsCntlr from '../ChartsCntlr.js';

class ChartPanelView extends Component {

    constructor(props) {
        super(props);
        this.state = {
            optionsShown: false,
            componentKey: undefined // used when there are multiple 'options' components
        };

        const normal = (size) => {
            if (size && this.iAmMounted) {
                var widthPx = size.width;
                var heightPx = size.height;
                //console.log('width: '+widthPx+', height: '+heightPx);
                if (widthPx !== this.state.widthPx || heightPx !== this.state.heightPx) {
                    this.setState({widthPx, heightPx});
                }
            }
        };
        const debounced = debounce(normal, 100);
        this.onResize = (size) => {
            if (this.state.widthPx === 0) {
                defer(normal, size);
            } else {
                debounced(size);
            }
        };

        this.toggleOptions = this.toggleOptions.bind(this);
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }

    toggleOptions(key) {
        const {optionsShown, componentKey} = this.state;
        if (key === componentKey) {
            const newShowOptions = !optionsShown;
            const newKey = newShowOptions ? key : undefined;
            this.setState({optionsShown: newShowOptions, key: newKey});
        } else if (key) {
            this.setState({optionsShown: true, componentKey: key});
        } else {
            this.setState({optionsShown: false, componentKey: key});
        }
    }

    componentDidMount() {
        const {chartId} = this.props;
        ChartsCntlr.dispatchChartMounted(chartId);
        this.iAmMounted = true;
    }

    componentWillReceiveProps(nextProps) {
        const {chartId} = nextProps;
        if (!chartId) { return; }

        if (chartId !== this.props.chartId) {
            ChartsCntlr.dispatchChartUnmounted(this.props.chartId);
            ChartsCntlr.dispatchChartMounted(chartId);
            this.setState({optionsShown: false});
        }
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        const {chartId} = this.props;
        ChartsCntlr.dispatchChartUnmounted(chartId);
    }

    render() {
        const {chartId, chartData, expandable, expandedMode, renderChart, renderOptions, renderToolbar} = this.props;


        if (!chartData) {
            return (<div/>);
        }

        var {widthPx, heightPx, componentKey, optionsShown} = this.state;
        const knownSize = widthPx && heightPx;

        return (
            <div className='ChartPanel__container'>
                <div className='ChartPanel__wrapper'>
                    {renderToolbar(chartId, expandable, expandedMode, this.toggleOptions)}
                    <div className='ChartPanel__chartarea'>
                        {optionsShown &&
                        <div className='ChartPanelOptions'>
                            <div style={{height: 14}}>
                                <div style={{ right: -6, float: 'right'}}
                                     className='btn-close'
                                     title='Remove Panel'
                                     onClick={() => this.toggleOptions()}/>
                            </div>
                            {renderOptions(chartId, componentKey)}
                        </div>
                        }
                        <Resizable id='chart-resizer' onResize={this.onResize} className='ChartPanel__chartresizer'>
                            <div style={{overflow:'auto',width:widthPx,height:heightPx}}>
                                {knownSize ? renderChart(Object.assign({}, this.props, {widthPx,heightPx})) : <div/>}
                            </div>
                        </Resizable>
                    </div>
                </div>
            </div>
        );
    }
}

ChartPanelView.propTypes = {
    chartId: PropTypes.string.isRequired,
    chartData: PropTypes.object,
    expandable: PropTypes.bool,
    expandedMode: PropTypes.bool,
    renderChart: PropTypes.func,
    renderOptions: PropTypes.func,
    renderToolbar: PropTypes.func
};

export class ChartPanel extends Component {

    constructor(props) {
        super(props);
        this.state = this.getNextState();
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }

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
        const chartData =  ChartsCntlr.getChartData(chartId);
        if (chartData) {
            const chartType = get(chartData, 'chartType');
            const {renderChart,renderOptions,renderToolbar,getChartProperties,updateOnStoreChange} = reduxFlux.getChartType(chartType);
            return {
                chartId, ...getChartProperties(chartId),
                renderChart,
                renderOptions,
                renderToolbar,
                updateOnStoreChange
            };
        }
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const ns = this.getNextState();
            if (ns && ns.updateOnStoreChange(ns)) {
                this.setState(ns);
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
    chartId: PropTypes.string.isRequired
};