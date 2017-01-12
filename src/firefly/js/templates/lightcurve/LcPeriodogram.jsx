/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';

import sCompare from 'react-addons-shallow-compare';
import { get, set, isEmpty} from 'lodash';
import SplitPane from 'react-split-pane';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {createContentWrapper} from '../../ui/panel/DockLayoutPanel.jsx';
import {LC, periodPageMode} from './LcManager.js';
import {getTypeData} from './LcPeriod.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {dispatchValueChange, dispatchMountComponent} from '../../fieldGroup/FieldGroupCntlr.js';
import Validate from '../../util/Validate.js';
import {makeTblRequest, getTblById} from '../../tables/TableUtil.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {loadXYPlot} from '../../charts/dataTypes/XYColsCDT.js';
import {dispatchLayoutDisplayMode, LO_VIEW} from '../../core/LayoutCntlr.js';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../../core/ComponentCntlr.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {InputGroup} from '../../ui/InputGroup.jsx';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';

var pgramkey = LC.PERIOD_FINDER;

const algorOptions = [
    {label: 'Lomb‑Scargle ', value: 'ls', proj: 'LCViewer'},
    {label: 'Box-fitting Least Squares', value: 'bls', proj: 'LCViewer'},
    {label: 'Plavchan 2008', value: 'plav', proj: 'LCViewer'}
];

const stepOptions = [
    {label: 'Fixed Frequency', value: 'fixedf', proj: 'LCViewer'},
    {label: 'Fixed Period', value: 'fixedp', proj: 'LCViewer'},
    {label: 'Exponential', value: 'exp', proj: 'LCViewer'},
    {label: 'Plavchan', value: 'plav', proj: 'LCViewer'}
];

const cPeriodKeyDef = {
    time: {fkey: 'time', label: 'Time Column'},
    flux: {fkey: 'flux', label: 'Flux Column'},
    min: {fkey: 'periodMin', label: 'Period Min'},
    max: {fkey: 'periodMax', label: 'Period Max'},
    timecols: {fkey: 'timeCols', label: ''},
    fluxcols: {fkey: 'fluxCols', label: ''}
};

const pKeyDef = Object.assign({}, cPeriodKeyDef,
                    { algor: {fkey: 'periodAlgor', label: 'Periodogram Type'},
                      stepmethod: {fkey: 'stepMethod', label: 'Period Step Method'},
                      stepsize: {fkey: 'stepSize', label: 'Fixed Step Size'},
                      peaks: {fkey: 'peaks', label:'Number of Peaks'} });


function getDefaultStepSize(fields) {
    //const fields = FieldGroupUtils.getGroupFields(gKey);
    const min = get(fields, [pKeyDef.min.fkey]);
    const max = get(fields, [pKeyDef.max.fkey]);
    const {totalRows} = getTblById(LC.RAW_TABLE) || {};

    const s = min&&max&&totalRows ? (parseFloat(max)-parseFloat(min))/(totalRows*10) : 1.0e-7;

    return `${s}`;
}


var defValues = {
    [pKeyDef.algor.fkey]: Object.assign(getTypeData(pKeyDef.algor.fkey,
        algorOptions[0].value,
        'periodogram algorithm',
        `${pKeyDef.algor.label}:`, 150)),
    [pKeyDef.stepmethod.fkey]: Object.assign(getTypeData(pKeyDef.stepmethod.fkey,
        stepOptions[0].value,
        'periodogram step method',
        `${pKeyDef.stepmethod.label}:`, 150)),
    [pKeyDef.stepsize.fkey]: Object.assign(getTypeData(pKeyDef.stepsize.fkey, '',
        'period fixed step size',
        `${pKeyDef.stepsize.label}:`, 150),
        {validator: Validate.floatRange.bind(null, 1.0e-7, undefined, 'fixed step size field')}),
    [pKeyDef.peaks.fkey]: Object.assign(getTypeData(pKeyDef.peaks.fkey, '50',
        'number of peaks to return',
        `${pKeyDef.peaks.label}:`, 50),
        {validator: Validate.intRange.bind(null, 1, 500, 'peaks number field')})
};

var defPeriod = {
    [pKeyDef.time.fkey]: {value: ''},
    [pKeyDef.flux.fkey]: {value: ''},
    [pKeyDef.min.fkey]: {value: ''},
    [pKeyDef.max.fkey]: {value: ''}
};

var defPeriodogram = {
    [pKeyDef.algor.fkey]: {value: ''},
    [pKeyDef.stepmethod.fkey]: {value: ''},
    [pKeyDef.stepsize.fkey]: {value: ''},
    [pKeyDef.peaks.fkey]: {value: ''}
};

export function LcPeriodogram(props) {
    const {display, groupKey=pgramkey, expanded} = props;
    const content = {};

    content.tables =  (<TablesContainer key='res-tables'
                                        mode='both'
                                        tbl_group={LC.PERIODOGRAM_GROUP}
                                        closeable={true}
                                        expandedMode={expanded===LO_VIEW.tables}/>);
    content.xyPlot = (<ChartsContainer key='res-charts'
                                       closeable={true}
                                       expandedMode={expanded===LO_VIEW.xyPlots}/>);

    const resultProps = {...content, expanded, display, groupKey};

    return (
        <div style={{height: '100%'}}>
            {(display&&display==='period') ? <PeriodogramButton groupKey={groupKey} /> :
                                             <PeriodogramResult {...resultProps} />}
        </div>
    );
}


LcPeriodogram.propTypes = {
    display: PropTypes.string.isRequired,
    expanded: PropTypes.object,
    groupKey: PropTypes.string
};

LcPeriodogram.defaultProps = {
    display: LC.PERIOD_PAGE
};


function  PeriodogramButton(props) {
    const {groupKey} = props;
    return (
        <div style={{height: '100%',
                     display: 'flex', justifyContent: 'center', alignItems: 'center'}}>
            <CompleteButton style={{maxWidth: '50%'}}
                            groupKey={groupKey}
                            text={'Find Periodogram'}
                            onSuccess={startPeriodogramPopup(groupKey)}
            />
        </div>
    )
}



PeriodogramButton.propTypes = {
    groupKey: PropTypes.string.isRequired
};

const popupId = 'periodogramPopup';

var startPeriodogramPopup = (groupKey) =>  {
    return (request) => {

        Object.keys(defPeriod).forEach((key) => {
            set(defPeriod, [key, 'value'], get(request, [key]));     // init default time, flux value, period
        });

        updatePeriodGroup(groupKey, request);

        var popup = (<PopupPanel title={'Periodogram'}>
            {<PeriodogramOptionsBox groupKey={groupKey} />}
            <div>
                <CompleteButton
                    dialogId={popupId}
                    groupKey={groupKey}
                    onSuccess={periodogramSuccess(true)}
                    text={'Periodogram Calculation'}
                />
            </div>
        </PopupPanel>);

        DialogRootContainer.defineDialog(popupId, popup);
        dispatchShowDialog(popupId);
    };
};


function updatePeriodGroup(gkey, request) {

    if (!Object.keys(request).includes(pKeyDef.algor.fkey)){
        var defV = Object.assign({}, defValues);

        set(defV, [pKeyDef.stepsize.fkey, 'value'], getDefaultStepSize(request));

        Object.keys(defV).forEach((key) => {
           var v = defV[key].value;

           dispatchMountComponent(gkey, key, true, v, defV[key]);
           set(defPeriodogram, [key, 'value'], v);
        });
    } else {

        Object.keys(defPeriodogram).forEach((key) => {
            set(defPeriodogram, [key, 'value'], get(request, [key]));     // init default time, flux value, period
        });
    }
}

class PeriodogramOptionsBox extends Component {
    constructor(props) {
        super(props);
        var {groupKey} = props;

        this.state = {fields: FieldGroupUtils.getGroupFields(groupKey)};
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        if (this.unbinder) this.unbinder();
    }

    componentDidMount() {
        var {groupKey} = this.props;

        this.iAmMounted = true;
        this.unbinder = FieldGroupUtils.bindToStore(groupKey, (fields) => {
            if (fields !== this.state.fields && this.iAmMounted) {
                this.setState({fields});
            }
        });
    }

    render() {
        const {groupKey} = this.props;
        const {timeCols, fluxCols } = this.state.fields || {};

        return (
            <div style={{padding:5}}>
                <FieldGroup groupKey={groupKey} keepState={true}>
                    <InputGroup labelWidth={150}>
                        <ListBoxInputField options={algorOptions}
                                           multiple={false}
                                           fieldKey={pKeyDef.algor.fkey}
                                           multiple={false}
                        />
                        <br/>
                        <SuggestBoxInputField
                            fieldKey={pKeyDef.time.fkey}
                            getSuggestions = {(val)=>{
                            const sList = timeCols&&get(timeCols, 'value');
                            const suggestions =  sList && sList.filter((el)=>{return el.startsWith(val);});
                            return suggestions && suggestions.length > 0 ? suggestions : [];
                        }}

                        />
                        <br/>
                        <SuggestBoxInputField
                            fieldKey={pKeyDef.flux.fkey}
                            getSuggestions = {(val)=>{
                            const sList = fluxCols&&get(fluxCols, 'value');
                            const suggestions = sList && sList.filter((el)=>{return el.startsWith(val);});
                            return suggestions && suggestions.length > 0 ? suggestions : [];
                        }}

                        />
                        <br/>
                        <ListBoxInputField options={stepOptions}
                                           multiple={false}
                                           fieldKey={pKeyDef.stepmethod.fkey}
                                           multiple={false}
                        />
                        <br/>
                        <ValidationField fieldKey={pKeyDef.stepsize.fkey} />
                        <br/>
                        <ValidationField fieldKey={pKeyDef.min.fkey} />
                        <br/>
                        <ValidationField fieldKey={pKeyDef.max.fkey} />
                        <br/>
                        <ValidationField fieldKey={pKeyDef.peaks.fkey} />
                        <br/>

                        <button type='button' className='button std hl' onClick={() => resetDefaults(groupKey)}>
                            <b>Reset</b>
                        </button>

                    </InputGroup>
                </FieldGroup>
                <br/>
            </div>
        );
    }
}


PeriodogramOptionsBox.propTypes = {
    groupKey: PropTypes.string.isRequired
};

function resetDefaults(groupKey) {
    Object.keys(defPeriodogram).forEach((fieldKey) => {
        dispatchValueChange({groupKey, fieldKey, value: defPeriodogram[fieldKey].value});
    });

    Object.keys(defPeriod).forEach((fieldKey) => {
        dispatchValueChange({groupKey, fieldKey, value: defPeriod[fieldKey].value});
    });
}

function periodogramSuccess(hideDropDown = false) {
    return (request) => {
        const tbl = getTblById(LC.RAW_TABLE);
        const fields = FieldGroupUtils.getGroupFields(pgramkey);
        const srcFile = tbl.request.source;

        var tReq2 = makeTblRequest('LightCurveProcessor', LC.PEAK_TABLE, {
            original_table: srcFile,
            x: fields.time.value || 'mjd',
            y: fields.flux.value || 'w1mpro_ep',
            alg: fields.periodAlgor.value,
            pmin: fields.periodMin.value,
            pmax: fields.periodMax.value,
            step_method: fields.stepMethod.value,
            step_size: fields.stepSize.value,
            peaks: fields.peaks.value,
            table_name: LC.PEAK_TABLE,
            sortInfo: sortInfoString('SDE')                 // sort peak table by column SDE
        }, {tbl_id: LC.PEAK_TABLE});

        if (tReq2 !== null) {
            const xyPlotParams = {
                x: {columnOrExpr: LC.PEAK_CNAME, options: 'grid'},
                y: {columnOrExpr: LC.POWER_CNAME, options: 'grid'}
            };
            loadXYPlot({chartId: LC.PEAK_TABLE, tblId: LC.PEAK_TABLE, xyPlotParams});
            dispatchTableSearch(tReq2, {removable: true, tbl_group: LC.PERIODOGRAM_GROUP});
        }

        var tReq = makeTblRequest('LightCurveProcessor', LC.PERIODOGRAM, {
            original_table: srcFile,
            x: fields.time.value || 'mjd',
            y: fields.flux.value || 'w1mpro_ep',
            alg: fields.periodAlgor.value,
            pmin: fields.periodMin.value,
            pmax: fields.periodMax.value,
            step_method: fields.stepMethod.value,
            step_size: fields.stepSize.value,
            peaks: fields.peaks.value,
            table_name: LC.PERIODOGRAM
        }, {tbl_id: LC.PERIODOGRAM});


        if (tReq !== null) {
            dispatchTableSearch(tReq, {removable: true, tbl_group: LC.PERIODOGRAM_GROUP});
            const xyPlotParams = {
                userSetBoundaries: {yMin: 0},
                x: {columnOrExpr: LC.PERIOD_CNAME, options: 'grid,log'},
                y: {columnOrExpr: LC.POWER_CNAME, options: 'grid'}
            };
            loadXYPlot({chartId: LC.PERIODOGRAM, tblId: LC.PERIODOGRAM, markAsDefault: true, xyPlotParams});
        }

        if (hideDropDown && isDialogVisible(popupId)) {
            dispatchHideDialog(popupId);
        }

        dispatchLayoutDisplayMode(LC.PERGRAM_PAGE);
        periodPageMode(LC.PERGRAM_PAGE);
    };
}

function  PeriodogramResult({expanded, groupKey, tables, xyPlot}) {

    var resultLayout;

    if (!expanded || expanded === LO_VIEW.none) {
        resultLayout = <SplitPane split='horizontal' minSize={20} defaultSize={'45%'}>
                            {createContentWrapper(tables)}
                            {createContentWrapper(xyPlot)}
                        </SplitPane>;
    } else {
        resultLayout = <div style={{ flex: 'auto', display: 'flex', flexFlow: 'column', overflow: 'hidden'}}>
            {expanded === LO_VIEW.tables ? tables : xyPlot}
        </div>;
    }
    return resultLayout;
}
