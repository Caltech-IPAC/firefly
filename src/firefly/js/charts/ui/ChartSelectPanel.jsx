import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';

import {FormPanel} from './../../ui/FormPanel.jsx';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {RadioGroupInputFieldView} from './../../ui/RadioGroupInputFieldView.jsx';
import {SimpleComponent, useStoreConnector} from './../../ui/SimpleComponent.jsx';
import {getChartData, dispatchChartTraceRemove, dispatchChartUpdate} from '../ChartsCntlr.js';
import {NewTracePanel, getNewTraceType, getSubmitChangesFunc, addNewTrace} from './options/NewTracePanel.jsx';
import {PopupPanel} from './../../ui/PopupPanel.jsx';
import {isSpectralOrder, isScatter2d, getTblIdFromChart} from '../ChartUtil.js';
import {basicOptions, BasicOptions} from './options/BasicOptions.jsx';
import {ScatterOptions} from './options/ScatterOptions.jsx';
import {HeatmapOptions} from './options/HeatmapOptions.jsx';
import {SpectrumOptions} from './options/SpectrumOptions.jsx';
import {FireflyHistogramOptions} from './options/FireflyHistogramOptions.jsx';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {DEFAULT_PLOT2D_VIEWER_ID} from '../../visualize/MultiViewCntlr.js';
import {dispatchHideDialog, dispatchShowDialog} from '../../core/ComponentCntlr.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';


export const [CHART_ADDNEW, CHART_TRACE_ADDNEW, CHART_TRACE_MODIFY, CHART_TRACE_REMOVE ] =
    ['chartAddnew', 'traceAddnew', 'traceModify', 'traceRemove'];


function getChartActions({chartId, tbl_id}) {
    const chartActions = [];
    if (chartId) {
        const {data=[], viewerId} = getChartData(chartId);

        if (data.length > 0) {
            // can modify active trace
            chartActions.push(CHART_TRACE_MODIFY);
            if (data.length > 1 && !isSpectralOrder(chartId)) {
                // can remove active trace
                chartActions.push(CHART_TRACE_REMOVE);
            }
        }
        if (tbl_id) {
            if (!isSpectralOrder(chartId)) {
                // can add trace
                chartActions.push(CHART_TRACE_ADDNEW);
            }

            // TODO: adding charts to non-default viewer does not work from charts options dialog
            // to be able to add charts to any viewer, we need to pass viewerId to addNewTrace
            // and eventually to dispatchChartAdd in BasicOptions.jsx
            if (!viewerId || viewerId === DEFAULT_PLOT2D_VIEWER_ID) {
                // can add chart
                chartActions.push(CHART_ADDNEW);
            }
        }
    } else {
        if (tbl_id) {
            // can add chart
            chartActions.push(CHART_ADDNEW);
        }
    }
    return chartActions;
}


function onChartAction({chartAction, tbl_id, chartId, hideDialog, renderTreeId}) {
    return (fields) => {
        const {activeTrace, data, fireflyData} = getChartData(chartId);
        tbl_id = tbl_id || getTblIdFromChart(chartId, activeTrace);

        switch (chartAction) {
            case CHART_ADDNEW:
                addNewTrace({fields, tbl_id, hideDialog, renderTreeId}); // no chart id
                break;
            case CHART_TRACE_ADDNEW:
                addNewTrace({fields, tbl_id, chartId, hideDialog});
                break;
             case CHART_TRACE_MODIFY:
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
                dispatchChartTraceRemove(chartId, activeTrace);
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
        const {tbl_id, chartId, inputStyle={}, hideDialog, style={}} = this.props;
        const {renderTreeId}= this.context;

        const chartActions = getChartActions({chartId, tbl_id});
        const {chartAction} = this.state;
        const groupKey = getGroupKey(chartId, chartAction);

        const chartActionChanged = (chartAction) => { this.setState({chartAction}); };

        return (
            <div style={{padding: 10, ...style}}>
                <FormPanel
                    groupKey={groupKey}
                    submitText={chartAction===CHART_TRACE_MODIFY ? 'Apply' : 'OK'}
                    onSuccess={onChartAction({chartAction, tbl_id, chartId, hideDialog, renderTreeId})}
                    cancelText='Close'
                    onError={() => {}}
                    onCancel={hideDialog}
                    inputStyle = {inputStyle}
                    style={{padding:5}}
                    changeMasking={this.changeMasking}>
                    <ChartAction {...{chartId, chartActions, chartAction, chartActionChanged}}/>
                    <ChartActionOptions {...{chartAction, tbl_id, chartId, groupKey, hideDialog}}/>
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
    inputStyle: PropTypes.object
};
ChartSelectPanel.contextType= RenderTreeIdCtx;

function ChartAction({chartId, chartActions, chartAction, chartActionChanged}) {

    const activeTrace = useStoreConnector(() => getChartData(chartId)?.activeTrace ?? 0);
    const {ChooseTrace} = basicOptions({chartId, activeTrace});

    const options = [];

    if (chartActions.includes(CHART_ADDNEW)) {
        options.push({label: 'Add New Chart', value: CHART_ADDNEW});
    }
    if (chartActions.includes(CHART_TRACE_ADDNEW)) {
        options.push({label: 'Overplot New Trace', value: CHART_TRACE_ADDNEW});
    }
    if (chartActions.includes(CHART_TRACE_MODIFY)) {
        options.push({label: 'Modify Trace', value: CHART_TRACE_MODIFY});
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
            <div>
                <RadioGroupInputFieldView
                    options={options}
                    alignment={'horizontal'}
                    value={chartAction}
                    onChange={onChartActionChange}
                    wrapperStyle={{padding: 5}}
                />
                <ChooseTrace style={{marginLeft: 11}}/>
            </div>
        );
    }
}

ChartAction.propTypes = {
    chartActions: PropTypes.arrayOf(PropTypes.string),
    chartAction: PropTypes.string,
    chartActionChanged: PropTypes.func
};

function ChartActionOptions(props) {
    const {chartAction, tbl_id, chartId:chartIdProp, groupKey, hideDialog} = props;


    const chartId = chartAction === CHART_ADDNEW ? undefined : chartIdProp;

    if (chartAction === CHART_ADDNEW || chartAction === CHART_TRACE_ADDNEW) {
        return (<NewTracePanel key={chartAction} {...{groupKey, tbl_id, chartId, hideDialog}}/>);
    }
    if (chartAction === CHART_TRACE_MODIFY) {
        return (
            <div style={{padding: 10, maxHeight: 600, overflow: 'auto', borderBottom: 'solid 1px #cccccc', borderTop: 'solid 1px #cccccc'}}>
                <SyncedOptionsUI {...{chartId, groupKey}}/>
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

function SyncedOptionsUI (props) {
    // based on chartData, determine what options to display

    const {chartId, groupKey} = props;
    const {useSpectrum, dataType, type, activeTrace} = useStoreConnector(() =>  {
        const {data, fireflyData, activeTrace=0} = getChartData(chartId);
        const dataType = fireflyData?.[activeTrace]?.dataType;
        const fval = getFieldVal(groupKey, `fireflyData.${activeTrace}.useSpectrum`);
        const useSpectrum = fval ?? fireflyData?.[activeTrace]?.useSpectrum;
        const type = get(data, [activeTrace, 'type'], 'scatter');
        return {useSpectrum, dataType, type, activeTrace};
    });

    const {fireflyData} = getChartData(chartId);

    useEffect( ()=> {
        if (useSpectrum !== fireflyData?.[activeTrace]?.useSpectrum) {
            dispatchChartUpdate({chartId, changes: {[`fireflyData.${activeTrace}.useSpectrum`]: useSpectrum}});
        }
    }, [useSpectrum, activeTrace, chartId, fireflyData]);

    // check firefly types first -
    // trace type for them is populated
    // when the data arrive
    if (dataType === 'fireflyHistogram') {
        return <FireflyHistogramOptions {...props}/>;
    } else if (useSpectrum) {
        return <SpectrumOptions {...props}/>;
    } else if (dataType === 'fireflyHeatmap') {
        if (get(fireflyData, [activeTrace, 'scatterOrHeatmap']) && isScatter2d(type)) {
            return <ScatterOptions {...props}/>;
        } else {
            return <HeatmapOptions {...props}/>;
        }
    } else if (isScatter2d(type)) {
        return <ScatterOptions {...props}/>;
    } else {
        if (get(fireflyData, [activeTrace, 'scatterOrHeatmap']) && type === 'heatmap') {
            return <HeatmapOptions {...props}/>;
        } else {
            return <BasicOptions {...props}/>;
        }
    }
}

SyncedOptionsUI.propTypes = {
    chartId: PropTypes.string,
    groupKey: PropTypes.string
};

/**
 * Creates and shows the modal dialog with chart options.
 * @param {string} chartId
 */
export function showChartsDialog(chartId) {
    const {data, fireflyData, activeTrace} = getChartData(chartId);
    const tbl_id = get(data, `${activeTrace}.tbl_id`) || get(fireflyData, `${activeTrace}.tbl_id`);

    const popupId ='chartOptionsDialog';
    const dialogContent= (
        <PopupPanel title='Plot Parameters' modal={true}>
            <ChartSelectPanel {...{
                tbl_id,
                chartId,
                chartAction: CHART_TRACE_MODIFY,
                style: {marginTop: -10},
                inputStyle: {backgroundColor:'none', padding: 3, border: 'none', borderBottom: 'solid 1px #a5a5a'},
                hideDialog: ()=>dispatchHideDialog(popupId)}}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(popupId, dialogContent);
    dispatchShowDialog(popupId);
}


