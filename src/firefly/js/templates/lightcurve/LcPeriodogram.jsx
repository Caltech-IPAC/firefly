/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import { get, set} from 'lodash';
import SplitPane from 'react-split-pane';
import {createContentWrapper} from '../../ui/panel/DockLayoutPanel.jsx';
import {LC, periodPageMode, updateLayoutDisplay} from './LcManager.js';
import {getTypeData} from './LcPeriod.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {dispatchValueChange, dispatchMountComponent} from '../../fieldGroup/FieldGroupCntlr.js';
import Validate from '../../util/Validate.js';
import {makeTblRequest, getTblById} from '../../tables/TableUtil.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {dispatchTableSearch, dispatchActiveTableChanged} from '../../tables/TablesCntlr.js';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {loadXYPlot} from '../../charts/dataTypes/XYColsCDT.js';
import {LO_VIEW} from '../../core/LayoutCntlr.js';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../../core/ComponentCntlr.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {InputGroup} from '../../ui/InputGroup.jsx';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';

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

// parameter from period calculation used in the popup dialog
const cPeriodKeyDef = {
    time: {fkey: 'time', label: 'Time Column'},
    flux: {fkey: 'flux', label: 'Flux Column'},
    min: {fkey: 'periodMin', label: 'Period Min'},
    max: {fkey: 'periodMax', label: 'Period Max'},
    timecols: {fkey: 'timeCols', label: ''},
    fluxcols: {fkey: 'fluxCols', label: ''}
};


// parameter list in the popup dialog
const pKeyDef = Object.assign({}, cPeriodKeyDef,
                    { algor: {fkey: 'periodAlgor', label: 'Periodogram Type'},
                      stepmethod: {fkey: 'stepMethod', label: 'Period Step Method'},
                      stepsize: {fkey: 'stepSize', label: 'Fixed Step Size'},
                      peaks: {fkey: 'peaks', label:'Number of Peaks'} });

const SSIZE = 0.3;

// calculate default step size
function getDefaultStepSize(fields) {
    //const fields = FieldGroupUtils.getGroupFields(gKey);
    const min = get(fields, [pKeyDef.min.fkey, 'value']);
    const max = get(fields, [pKeyDef.max.fkey, 'value']);
    const {totalRows} = getTblById(LC.RAW_TABLE) || {};

    var s = min&&max&&totalRows ? (parseFloat(max)-parseFloat(min))/(totalRows*10) : SSIZE;

    if (s < SSIZE) {
        s = SSIZE;
    }

    return `${s}`;
}

function maxStepSize() {
    return 100.0;
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
        {validator: Validate.floatRange.bind(null, SSIZE, maxStepSize(), 3, 'step size')}),
    [pKeyDef.peaks.fkey]: Object.assign(getTypeData(pKeyDef.peaks.fkey, '50',
        'number of peaks to return (defalut is 50)',
        `${pKeyDef.peaks.label}:`, 50),
        {validator: Validate.intRange.bind(null, 1, 500, 'peaks number')})
};

// initial values of period parameters
var defPeriod = {
    [pKeyDef.time.fkey]: {value: ''},
    [pKeyDef.flux.fkey]: {value: ''},
    [pKeyDef.min.fkey]: {value: ''},
    [pKeyDef.max.fkey]: {value: ''}
};

// initial values of periodogram parameters
var defPeriodogram = {
    [pKeyDef.algor.fkey]: {value: ''},
    [pKeyDef.stepmethod.fkey]: {value: ''},
    [pKeyDef.stepsize.fkey]: {value: ''},
    [pKeyDef.peaks.fkey]: {value: ''}
};

/**
 * @summary component of periodogram panel (periodogram button or periodogram table/chart)
 * @param props
 * @returns {XML}
 * @constructor
 */
export function LcPeriodogram(props) {
    const {displayMode, groupKey=LC.PERIOD_FINDER, expanded} = props;
    const resultProps = {expanded, groupKey};

    return (
         <div style={{height: '100%', width: '100%', position: 'absolute'}}>
             {(displayMode&&displayMode==='period') ? <PeriodogramButton groupKey={groupKey} />
                                                    : <PeriodogramResult {...resultProps} />}
        </div>
    );
}


LcPeriodogram.propTypes = {
    displayMode: PropTypes.string.isRequired,
    expanded: PropTypes.object,
    groupKey: PropTypes.string
};

LcPeriodogram.defaultProps = {
    displayMode: LC.PERIOD_PAGE
};

/**
 * @summary component containing a button to start periodogram dialog popup
 * @param props
 * @returns {XML}
 * @constructor
 */
function  PeriodogramButton(props) {
    const {groupKey} = props;
    return (
        <div style={{height: '100%',
                     display: 'flex', justifyContent: 'center', alignItems: 'center'}}>
            <button type='button' style={{maxWidth: '50%'}}
                    className='button std'
                    onClick={startPeriodogramPopup(groupKey)}>Find Periodogram</button>
        </div>
    );
}


PeriodogramButton.propTypes = {
    groupKey: PropTypes.string.isRequired
};

export const popupId = 'periodogramPopup';

/**
 * @summry periodogram popup
 * @param groupKey
 * @returns {Function}
 */
export var startPeriodogramPopup = (groupKey) =>  {
    return () => {
        var fields = FieldGroupUtils.getGroupFields(groupKey);

        Object.keys(defPeriod).forEach((key) => {
            set(defPeriod, [key, 'value'], get(fields, [key, 'value']));     // init default time, flux value, period
        });

        updatePeriodGroup(groupKey, fields);

        var popup = (
            <PopupPanel title={'Periodogram'}
                                 closeCallback={cancelPeriodogram(groupKey, popupId)}>
                <PeriodogramOptionsBox groupKey={groupKey} />
                <div style={{display: 'flex', margin: '30px 10px 10px 10px'}} >
                    <div>
                        <button type='button' className='button std hl'
                                onClick={periodogramSuccess(groupKey, popupId, true)}>Periodogram Calculation
                        </button>
                    </div>
                    <div>
                        <button type='button' className='button std hl'
                                onClick={cancelPeriodogram(groupKey, popupId)}>Cancel
                        </button>
                    </div>
                </div>
            </PopupPanel>);

        DialogRootContainer.defineDialog(popupId, popup);
        dispatchShowDialog(popupId);
    };
};

/**
 * @summary add periodogram related parameters into FieldGroup of period finder and init values of
 *          those parameters
 * @param gkey
 * @param fields
 */
function updatePeriodGroup(gkey, fields) {

    if (!Object.keys(fields).includes(pKeyDef.algor.fkey)){
        var defV = Object.assign({}, defValues);

        set(defV, [pKeyDef.stepsize.fkey, 'value'], getDefaultStepSize(fields));

        Object.keys(defV).forEach((key) => {
           var v = defV[key].value;

           dispatchMountComponent(gkey, key, true, v, defV[key]);
           set(defPeriodogram, [key, 'value'], v);
        });
    } else {

        Object.keys(defPeriodogram).forEach((key) => {
            set(defPeriodogram, [key, 'value'], get(fields, [key, 'value']));     // init default time, flux value, period
        });
    }
}

/**
 * class for periodogram dialog content
 */
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
        const {timeCols, fluxCols} = this.state.fields || {};

        return (
            <div style={{padding:5, margin: 10, border: '1px solid #a3aeb9'}}>
                <FieldGroup groupKey={groupKey} keepState={true} keepMounted={true}>
                    <InputGroup labelWidth={150}>
                        <ListBoxInputField options={algorOptions}
                                           multiple={false}
                                           fieldKey={pKeyDef.algor.fkey}
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

/**
 * @summary reset parameters to the initial values
 * @param groupKey
 */
function resetDefaults(groupKey) {
    const fields = FieldGroupUtils.getGroupFields(groupKey);


    Object.keys(defPeriodogram).forEach((fieldKey) => {
        if (defPeriodogram[fieldKey].value !== get(fields, [fieldKey, 'value'])) {
            dispatchValueChange({groupKey, fieldKey, value: defPeriodogram[fieldKey].value});
        }
    });

    Object.keys(defPeriod).forEach((fieldKey) => {
        if (defPeriod[fieldKey].value !== get(fields, [fieldKey, 'value'])) {
            dispatchValueChange({groupKey, fieldKey, value: defPeriod[fieldKey].value});
        }
    });
}

/**
 * @summary reset the values and exit the popup
 * @param groupKey
 * @param popupId
 * @returns {Function}
 */
export function cancelPeriodogram(groupKey, popupId) {
    return () => {
        resetDefaults(groupKey);
        if (popupId && isDialogVisible(popupId)) {
            dispatchHideDialog(popupId);
        }
    };
}

/**
 * @summary create periodogram tables and charts
 * @param groupKey
 * @param popupId
 * @param hideDropDown
 * @returns {Function}
 */
function periodogramSuccess(groupKey, popupId, hideDropDown = false) {
    return () => {
        const tbl = getTblById(LC.RAW_TABLE);
        const fields = FieldGroupUtils.getGroupFields(groupKey);
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
            dispatchTableSearch(tReq2, {removable: true, tbl_group: LC.PERIODOGRAM_GROUP});
            const xyPlotParams = {
                x: {columnOrExpr: LC.PEAK_CNAME, options: 'grid'},
                y: {columnOrExpr: LC.POWER_CNAME, options: 'grid'}
            };
            loadXYPlot({chartId: LC.PEAK_TABLE, tblId: LC.PEAK_TABLE, xyPlotParams});
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

        dispatchActiveTableChanged(LC.PERIODOGRAM, LC.PERIODOGRAM_GROUP);
        if (hideDropDown && popupId && isDialogVisible(popupId)) {
            dispatchHideDialog(popupId);
        }

        updateLayoutDisplay(LC.PERGRAM_PAGE);
        periodPageMode(LC.PERGRAM_PAGE);
    };
}

/**
 * @summary component for showing periodogram result (table/chart) in standard or expeanded mode
 * @param expanded
 * @returns {*}
 * @constructor
 */
const  PeriodogramResult = ({expanded}) => {

    var resultLayout;
    const tables =  (<TablesContainer key='res-tables'
                                      mode='both'
                                      tbl_group={LC.PERIODOGRAM_GROUP}
                                      closeable={true}
                                      expandedMode={expanded===LO_VIEW.tables}/>);
    const xyPlot = (<ChartsContainer key='res-charts'
                                     closeable={true}
                                     expandedMode={expanded===LO_VIEW.xyPlots}/>);


    if (!expanded || expanded === LO_VIEW.none) {
        resultLayout = (<SplitPane split='horizontal' minSize={20} defaultSize={'45%'}>
                            {createContentWrapper(tables)}
                            {createContentWrapper(xyPlot)}
                        </SplitPane>);
    } else {
        resultLayout = (<div style={{ flex: 'auto', display: 'flex', flexFlow: 'column', overflow: 'hidden', height: '100%'}}>
            {expanded === LO_VIEW.tables ? tables : xyPlot}
        </div>);
    }
    return resultLayout;
};

