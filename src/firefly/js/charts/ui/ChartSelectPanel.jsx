import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get, pick, uniqueId} from 'lodash';
import {FormPanel} from './../../ui/FormPanel.jsx';
import {dispatchMultiValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../../core/MasterSaga.js';

import {getReducerFunc} from '../../fieldGroup/FieldGroupUtils.js';

import {RadioGroupInputFieldView} from './../../ui/RadioGroupInputFieldView.jsx';
import {SimpleComponent} from './../../ui/SimpleComponent.jsx';
import {CHART_UPDATE, dataLoadedUpdate, getChartData, dispatchChartTraceRemove} from '../ChartsCntlr.js';
import {NewTracePanel, getNewTraceType, getSubmitChangesFunc, addNewTrace} from './options/NewTracePanel.jsx';

import {showOptionsPopup} from './../../ui/PopupUtil.jsx';

import {isScatter2d} from '../ChartUtil.js';
import {BasicOptions} from './options/BasicOptions.jsx';
import {ScatterOptions} from './options/ScatterOptions.jsx';
import {HeatmapOptions} from './options/HeatmapOptions.jsx';
import {FireflyHistogramOptions} from './options/FireflyHistogramOptions.jsx';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';


export const [CHART_ADDNEW, CHART_TRACE_ADDNEW, CHART_TRACE_MODIFY, CHART_TRACE_REMOVE ] =
    ['chartAddnew', 'traceAddnew', 'traceModify', 'traceRemove'];


function getChartActions({chartId, tbl_id}) {
    const chartActions = [];
    if (chartId) {
        const {data=[]} = getChartData(chartId);
        if (data.length > 0) {
            // can modify active trace
            chartActions.push(CHART_TRACE_MODIFY);
            if (data.length > 1) {
                // can remove active trace
                chartActions.push(CHART_TRACE_REMOVE);
            }
        }
        if (tbl_id) {
            // can add trace
            chartActions.push(CHART_TRACE_ADDNEW);
        }
    }
    if (tbl_id) {
        // can add chart
        chartActions.push(CHART_ADDNEW);
    }
    return chartActions;
}


function onChartAction({chartAction, tbl_id, chartId, hideDialog, renderTreeId}) {
    return (fields) => {
        switch (chartAction) {
            case CHART_ADDNEW:
                addNewTrace({fields, tbl_id, hideDialog, renderTreeId}); // no chart id
                break;
            case CHART_TRACE_ADDNEW:
                addNewTrace({fields, tbl_id, chartId, hideDialog});
                break;
             case CHART_TRACE_MODIFY:
                const {activeTrace, data, fireflyData} = getChartData(chartId);
                const type = get(data, `${activeTrace}.type`, 'scatter');
                let ftype = get(fireflyData, `${activeTrace}.dataType`);
                const scatterOrHeatmap = get(fireflyData, [activeTrace, 'scatterOrHeatmap']);
                if (scatterOrHeatmap) ftype = 'scatterOrHeatmap';
                const submitChangesFunc = getSubmitChangesFunc(type, ftype);
                //hideDialog();
                submitChangesFunc && submitChangesFunc({chartId, activeTrace, fields, tbl_id});
                break;
            case CHART_TRACE_REMOVE:
                hideDialog();
                const {activeTrace:traceNum} = getChartData(chartId);
                dispatchChartTraceRemove(chartId, traceNum);
                break;
            default:
                console.log(`onChartAction - unsupported action ${chartAction}`);
                console.log(fields);
                hideDialog();
        }
    };
}

function getGroupKey(chartId, chartAction) {
    if (chartAction === CHART_ADDNEW || chartAction === CHART_TRACE_ADDNEW) {
        const type = getNewTraceType();
        const  cid = (chartAction === CHART_ADDNEW) ? 'newchart' : chartId;
        return `${cid}-newtrace-${type}`;
    } else {
        const {activeTrace} = getChartData(chartId);
        return `${chartId}-${activeTrace}`;
    }
}


export class ChartSelectPanel extends SimpleComponent {
    constructor(props) {
        super(props);

        const {chartId, tbl_id, chartAction} = this.props;
        let newChartAction = chartAction;
        const chartActions = getChartActions({chartId, tbl_id});

        if (!newChartAction || !chartActions.includes(newChartAction)) {
            newChartAction = chartActions[0];
        }

        this.state = {chartAction: newChartAction};
    }

    render() {
        const {tbl_id, chartId, inputStyle={}, hideDialog, showMultiTrace} = this.props;
        const {renderTreeId}= this.context;

        const chartActions = getChartActions({chartId, tbl_id});
        const {chartAction} = this.state;
        const groupKey = getGroupKey(chartId, chartAction);

        const chartActionChanged = (chartAction) => { this.setState({chartAction}); };

        return (
            <div style={{padding: 10}}>
                <FormPanel
                    groupKey={groupKey}
                    submitText={chartAction===CHART_TRACE_MODIFY ? 'Apply' : 'OK'}
                    onSuccess={onChartAction({chartAction, tbl_id, chartId, hideDialog, renderTreeId})}
                    cancelText='Close'
                    onError={() => {}}
                    onCancel={hideDialog}
                    inputStyle = {inputStyle}
                    changeMasking={this.changeMasking}>
                    {showMultiTrace && <ChartAction {...{chartActions, chartAction, chartActionChanged}}/>}
                    <ChartActionOptions {...{chartAction, tbl_id, chartId, groupKey, hideDialog,showMultiTrace}}/>
                </FormPanel>
            </div>
        );
    }
}

ChartSelectPanel.propTypes = {
    tbl_id: PropTypes.string,
    chartId: PropTypes.string,
    chartAction: PropTypes.string, // suggested chart action
    hideDialog: PropTypes.func,
    inputStyle: PropTypes.object,
    showMultiTrace:PropTypes.bool
};
ChartSelectPanel.defaultProps = {
    showMultiTrace: true,

};
ChartSelectPanel.contextType= RenderTreeIdCtx;

function ChartAction(props) {

    const {chartActions, chartAction, chartActionChanged} = props;

    const options = [];

    if (chartActions.includes(CHART_ADDNEW)) {
        options.push({label: 'Add New Chart', value: CHART_ADDNEW});
    }
    if (chartActions.includes(CHART_TRACE_ADDNEW)) {
        options.push({label: 'Add New Trace', value: CHART_TRACE_ADDNEW});
    }
    if (chartActions.includes(CHART_TRACE_MODIFY)) {
        options.push({label: 'Modify Active Trace', value: CHART_TRACE_MODIFY});
    }
    if (chartActions.includes(CHART_TRACE_REMOVE)) {
        options.push({label: 'Remove Active Trace', value: CHART_TRACE_REMOVE});
    }

    if (options.length > 0) {

        const onChartActionChange = (ev) => {
            const val = get(ev, 'target.value', '');
            const checked = get(ev, 'target.checked', false);
            if (checked) {
                chartActionChanged(val);
            }
        };

        return (
            <RadioGroupInputFieldView
                options={options}
                alignment={'horizontal'}
                value={chartAction}
                onChange={onChartActionChange}
                wrapperStyle={{padding: 5}}
            />
        );
    }
}

ChartAction.propTypes = {
    chartActions: PropTypes.arrayOf(PropTypes.string),
    chartAction: PropTypes.string,
    chartActionChanged: PropTypes.func
};

function ChartActionOptions(props) {
    const {chartAction, tbl_id, chartId:chartIdProp, groupKey, hideDialog, showMultiTrace} = props;


    const chartId = chartAction === CHART_ADDNEW ? undefined : chartIdProp;

    if (chartAction === CHART_ADDNEW || chartAction === CHART_TRACE_ADDNEW) {
        return (<NewTracePanel key={chartAction} {...{groupKey, tbl_id, chartId, hideDialog, showMultiTrace}}/>);
    }
    if (chartAction === CHART_TRACE_MODIFY) {
        return (
            <div style={{padding: 10}}>
                <SyncedOptionsUI {...{chartId, groupKey, showMultiTrace}}/>
            </div>
        );
    } else if (chartAction === CHART_TRACE_REMOVE) {
        const {data=[], activeTrace} = getChartData(chartId);
        const traceName = get(data, `${activeTrace}.name`) || `trace ${activeTrace}`;
        return (
            <div style={{padding: 10}}>
                {`Remove ${traceName} (active trace) of the chart?`}
            </div>
        );
    } else {
        return (<div style={{padding: 10}}>Unsupported chart action</div>);
    }
}

ChartActionOptions.propTypes = {
    chartAction: PropTypes.string,
    tbl_id: PropTypes.string,
    chartId: PropTypes.string,
    groupKey: PropTypes.string,
    hideDialog: PropTypes.func
};

/**
 * Action watcher callback: watch chart data updates, and sync options with the store
 * Some options (ex. default axes titles) are updated with the data.
 * At this point options fields should be reset.
 * @callback actionWatcherCallback
 * @param action
 * @param cancelSelf
 * @param params
 * @param params.chartId
 * @param params.groupId
 */
function watchChartDataChange(action, cancelSelf, params) {
    const {chartId:actionChartId, changes={}} = action.payload;
    const {chartId, groupKey} = params;
    //options should be synced when data are received: fireflyData.traceNum.isLoading is switched to false
    if (actionChartId === chartId && dataLoadedUpdate(changes)) {
        const reducerFunc = getReducerFunc(params.groupKey);
        const flds = reducerFunc && reducerFunc(null);
        if (flds) {
            const fldAry = Object.values(flds).map((v) => pick(v, ['fieldKey', 'value']));
            dispatchMultiValueChange(groupKey, fldAry);
        }
    }
}

class SyncedOptionsUI extends PureComponent {

    componentDidMount() {
        const {chartId, groupKey} = this.props;
        if (chartId && groupKey) {
            this.watcherId = uniqueId('syncChartOpts');
            dispatchAddActionWatcher({id: this.watcherId,
                actions:[CHART_UPDATE],
                callback: watchChartDataChange,
                params: {chartId, groupKey}});
        }
    }

    componentWillUnmount() {
        if (this.watcherId) {
            dispatchCancelActionWatcher(this.watcherId);
        }
    }

    render() {
        const {chartId} = this.props;
        const OptionsUI = getOptionsUI(chartId);
        return (<OptionsUI {...this.props}/>);
    }
}

SyncedOptionsUI.propTypes = {
    chartId: PropTypes.string,
    groupKey: PropTypes.string
};

export function getOptionsUI(chartId) {
    // based on chartData, determine what options to display
    const {data, fireflyData, activeTrace=0} = getChartData(chartId);
    const type = get(data, [activeTrace, 'type'], 'scatter');
    const dataType = get(fireflyData, [activeTrace, 'dataType'], '');
    // check firefly types first -
    // trace type for them is populated
    // when the data arrive
    if (dataType === 'fireflyHistogram') {
        return FireflyHistogramOptions;
    } else if (dataType === 'fireflyHeatmap') {
        if (get(fireflyData, [activeTrace, 'scatterOrHeatmap']) && isScatter2d(type)) {
            return ScatterOptions;
        } else {
            return HeatmapOptions;
        }
    } else if (isScatter2d(type)) {
        return ScatterOptions;
    } else {
        if (get(fireflyData, [activeTrace, 'scatterOrHeatmap']) && type === 'heatmap') {
            return HeatmapOptions;
        } else {
            return BasicOptions;
        }
    }
}

/**
 * Creates and shows the modal dialog with chart options.
 * @param {string} chartId
 * @param {boolean} showMultiTrace
 */
export function showChartsDialog(chartId,  showMultiTrace) {
    const {data, fireflyData, activeTrace} = getChartData(chartId);
    const tbl_id = get(data, `${activeTrace}.tbl_id`) || get(fireflyData, `${activeTrace}.tbl_id`);
    
    const content= (
        <ChartSelectPanel {...{
            tbl_id,
            chartId,
            chartAction: CHART_TRACE_MODIFY,
            inputStyle: {backgroundColor:'none'},
            showMultiTrace,
            hideDialog: ()=>showOptionsPopup({show:false})}}/>
    );
    showOptionsPopup({content, title: 'Plot Parameters', modal: true, show: true});
}


