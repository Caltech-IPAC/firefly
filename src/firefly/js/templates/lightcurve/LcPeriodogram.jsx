/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import { get, set, has} from 'lodash';
import SplitPane from 'react-split-pane';
import {SplitContent} from '../../ui/panel/DockLayoutPanel.jsx';
import {LC, getValidValueFrom, updateLayoutDisplay} from './LcManager.js';
import {getTypeData} from './LcUtil.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import FieldGroupCntlr, {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import Validate from '../../util/Validate.js';
import {makeTblRequest, getTblById} from '../../tables/TableUtil.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {dispatchTableSearch, dispatchActiveTableChanged} from '../../tables/TablesCntlr.js';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {loadXYPlot} from '../../charts/dataTypes/XYColsCDT.js';
import {LO_VIEW, getLayouInfo} from '../../core/LayoutCntlr.js';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../../core/ComponentCntlr.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {InputGroup} from '../../ui/InputGroup.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {updateSet} from '../../util/WebUtil.js';
import HelpIcon from '../../ui/HelpIcon.jsx';

const algorOptions = [
    {label: 'Lomb‑Scargle ', value: 'ls', proj: 'LCViewer'}
    //{label: 'Box-fitting Least Squares', value: 'bls', proj: 'LCViewer'},
    //{label: 'Plavchan 2008', value: 'plav', proj: 'LCViewer'}
];

const stepOptions = [
    {label: 'Fixed Frequency', value: 'fixedf', proj: 'LCViewer'},
    {label: 'Fixed Period', value: 'fixedp', proj: 'LCViewer'},
    //{label: 'Exponential', value: 'exp', proj: 'LCViewer'},
    //{label: 'Plavchan', value: 'plav', proj: 'LCViewer'}
];


// parameter list in the popup dialog
const pKeyDef = { time: {fkey: 'time', label: 'Time Column'},
                  flux: {fkey: 'flux', label: 'Flux Column'},
                  min: {fkey: 'periodMin', label: 'Period Min (day)'},
                  max: {fkey: 'periodMax', label: 'Period Max (day)'},
                  algor: {fkey: 'periodAlgor', label: 'Periodogram Type'},
                  stepmethod: {fkey: 'stepMethod', label: 'Period Step Method'},
                  stepsize: {fkey: 'stepSize', label: 'Fixed Step Size (day)'},
                  peaks: {fkey: 'peaks', label:'Number of Peaks'}};

const pgfinderkey = LC.FG_PERIODOGRAM_FINDER;
const labelWidth = 150;


// defValues used to keep the initial values for parameters in the field group of periodogram popup dialog
// min:  minimum period
// max:  maximum period
// algor: periodogram algorithm
// stepmethod:  step mothod algorithm
// stepsize:  fixed step size
// peaks: number of peaks in peak table

var defValues = {
    [pKeyDef.min.fkey]: Object.assign(getTypeData(pKeyDef.min.fkey,
        '', 'minimum period (> 0)',
        `${pKeyDef.min.label}:`, labelWidth),
        {validator: null}),
    [pKeyDef.max.fkey]: Object.assign(getTypeData(pKeyDef.max.fkey,
        '', 'maximum period (> 0)',
        `${pKeyDef.max.label}:`, labelWidth),
        {validator: null}),
    [pKeyDef.algor.fkey]: Object.assign(getTypeData(pKeyDef.algor.fkey,
        algorOptions[0].value,
        'periodogram algorithm',
        `${pKeyDef.algor.label}:`, labelWidth)),
    [pKeyDef.stepmethod.fkey]: Object.assign(getTypeData(pKeyDef.stepmethod.fkey,
        stepOptions[0].value,
        'periodogram step method',
        `${pKeyDef.stepmethod.label}:`, labelWidth)),
    [pKeyDef.stepsize.fkey]: Object.assign(getTypeData(pKeyDef.stepsize.fkey, '',
        'period fixed step size (> 0.00000001)',
        `${pKeyDef.stepsize.label}:`, labelWidth),
        {validator: null}),
    [pKeyDef.peaks.fkey]: Object.assign(getTypeData(pKeyDef.peaks.fkey, '50',
        'number of peaks to return (default is 50)',
        `${pKeyDef.peaks.label}:`, labelWidth),
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
    [pKeyDef.peaks.fkey]: {value: '50'}
};

/**
 * @summary component of periodogram panel (periodogram button or periodogram table/chart)
 * @param props
 * @returns {XML}
 * @constructor
 */
export function LcPeriodogram(props) {
    const {displayMode, groupKey=pgfinderkey, expanded} = props;
    const resultProps = {expanded, groupKey};

    if (displayMode&&displayMode==='period') {
        return (
            <SplitContent>
                <PeriodogramButton groupKey={groupKey}/>
            </SplitContent>
        );
    } else {
        return <PeriodogramResult {...resultProps} />;
    }
}


LcPeriodogram.propTypes = {
    displayMode: PropTypes.string.isRequired,
    expanded: PropTypes.object,
    groupKey: PropTypes.string
};

LcPeriodogram.defaultProps = {
    displayMode: 'period'
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

function ChangePeriodogram() {
    return (
        <button type='button' className='button std hl'
                 onClick={startPeriodogramPopup(LC.FG_PERIODOGRAM_FINDER)}>Change Periodogram
        </button>
    );
}

export const popupId = 'periodogramPopup';

/**
 * @summry periodogram popup
 * @param groupKey
 * @returns {Function}
 */
export var startPeriodogramPopup = (groupKey) =>  {
    return () => {
        const aroundButton = {margin: 5};

        var popup = (
            <PopupPanel title={'Periodogram'}>
                <PeriodogramOptionsBox groupKey={groupKey} />
                <div style={{display: 'flex', margin: '30px 10px 10px 10px'}} >
                    <div style={aroundButton}>
                        <button type='button' className='button std hl'
                                onClick={cancelPeriodogram(groupKey, popupId)}>Cancel
                        </button>
                    </div>
                    <div style={aroundButton}>
                        <CompleteButton
                                groupKey={groupKey}
                                onSuccess={periodogramSuccess(popupId, true)}
                                onFail={periodogramFail(popupId, true)}
                                text={'Periodogram Calculation'} />
                    </div>
                </div>
            </PopupPanel>);

        DialogRootContainer.defineDialog(popupId, popup);
        dispatchShowDialog(popupId);
    };
};

/**
 * class for periodogram dialog content
 */
class PeriodogramOptionsBox extends Component {
    constructor(props) {
        super(props);

        this.state = {fields: FieldGroupUtils.getGroupFields(props.groupKey)};
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        if (this.unbinder) this.unbinder();
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.unbinder = FieldGroupUtils.bindToStore(this.props.groupKey, (fields) => {
            if (fields !== this.state.fields && this.iAmMounted) {
                this.setState({fields});
            }
        });
    }

    render() {
        var {groupKey} = this.props;

        return (
            <div style={{padding:5, margin: 10, border: '1px solid #a3aeb9'}}>
                <FieldGroup groupKey={groupKey}
                            reducerFunc={LcPeriodogramReducer()} keepState={true} >
                    <InputGroup labelWidth={labelWidth}>
                        <ListBoxInputField options={algorOptions}
                                           multiple={false}
                                           fieldKey={pKeyDef.algor.fkey}
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
                        <br/>
                        <div style={{marginTop: 10}}>
                            {'Leave the fields blank to use default values.'}  <HelpIcon helpId={'periodogram.options'}/>
                        </div>
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

var LcPeriodogramReducer = () => {
    return (inFields, action) => {
        var pFields =  FieldGroupUtils.getGroupFields(LC.FG_PERIOD_FINDER);

        var initPeriodValues = (fromFields) => {
            Object.keys(defPeriod).forEach((key) => {
                if(key !== pKeyDef.min.fkey && key !== pKeyDef.max.fkey){
                    set(defPeriod, [key, 'value'], get(fromFields, [key, 'value']));
                }     // init default time, flux value, period
            });
        };

        var initPeriodogramValues = (fromFields) => {
            Object.keys(defPeriodogram).forEach((key) => {
                set(defPeriodogram, [key, 'value'], get(fromFields, [key, 'value']));
            });
        };

        if (!inFields) {
            var defV = Object.assign({}, defValues);

            //set(defV, [pKeyDef.min.fkey, 'value'], get(pFields, [pKeyDef.min.fkey, 'value']));
            //set(defV, [pKeyDef.max.fkey, 'value'], get(pFields, [pKeyDef.max.fkey, 'value']));
            set(defV, [pKeyDef.peaks.fkey, 'validator'], peaksValidator('peaks'));
            set(defV, [pKeyDef.min.fkey, 'validator'], periodMinValidator('minimum period'));
            set(defV, [pKeyDef.max.fkey, 'validator'], periodMaxValidator('maximum period'));
            set(defV, [pKeyDef.stepsize.fkey, 'validator'], stepsizeValidator('step size'));

            initPeriodValues(pFields);
            initPeriodogramValues(defV);

            return defV;
        } else {
            switch (action.type) {
                case FieldGroupCntlr.MOUNT_FIELD_GROUP:
                    initPeriodValues(pFields);
                    initPeriodogramValues(inFields);
                   // inFields = updateSet(inFields, [pKeyDef.min.fkey, 'value'],
                   //     get(defPeriod, [pKeyDef.min.fkey, 'value']));
                   // inFields = updateSet(inFields, [pKeyDef.max.fkey, 'value'],
                   //     get(defPeriod, [pKeyDef.max.fkey, 'value']));
                    break;
                case FieldGroupCntlr.VALUE_CHANGE:
                    var retVal;

                    if (action.payload.fieldKey === pKeyDef.max.fkey) {  // change max and validate min
                        var minP = get(inFields, [pKeyDef.min.fkey, 'value']);

                        retVal = isPeriodMinValid(minP, 'minimum period');

                        inFields = updateSet(inFields, [pKeyDef.min.fkey, 'valid'], retVal.valid);
                        inFields = updateSet(inFields, [pKeyDef.min.fkey, 'message'], retVal.message);
                    }

                    if (action.payload.fieldKey === pKeyDef.min.fkey) { // change min and validate max
                        var maxP = get(inFields, [pKeyDef.max.fkey, 'value']);

                        retVal = isPeriodMaxValid(maxP, 'maximum period');
                        inFields = updateSet(inFields, [pKeyDef.max.fkey, 'valid'], retVal.valid);
                        inFields = updateSet(inFields, [pKeyDef.max.fkey, 'message'], retVal.message);
                    }
                    break;
                default:
                    break;
            }
        }
        return Object.assign({}, inFields);
    };
};

/**
 * @summary validate the changed minimum period
 * @param valStr
 * @param description
 * @returns {*}
 */
var isPeriodMinValid = (valStr, description) => {
    if (!valStr) return {valid: true};

    var retval = Validate.isFloat(description, valStr);
    if (!retval.valid) return retval;

    var val = parseFloat(valStr);
    var max = getValidValueFrom(FieldGroupUtils.getGroupFields(pgfinderkey), pKeyDef.max.fkey);

    var mmax = max ? parseFloat(max) :  Number.MAX_VALUE;
    var pMin = parseFloat(0);//get(getLayouInfo(), ['periodRange', 'min']);

    return (val > pMin && val < mmax) ?  {valid: true} :
                   {valid: false, message: description + `: must be greater than ${pMin} ` +
                                                         (max&&` and less than ${mmax}`)};
};

/**
 * @summary validate the changed maximum period
 * @param valStr
 * @param description
 * @returns {*}
 */
var isPeriodMaxValid = (valStr, description) => {
    if (!valStr) return {valid: true};

    var retval = Validate.isFloat(description, valStr);
    if (!retval.valid) return retval;

    var val = parseFloat(valStr);
    //var min = getValidValueFrom(FieldGroupUtils.getGroupFields(pgfinderkey), pKeyDef.min.fkey);
    const min = 0.0;
    var bVal = val > min;
    //var mmin = min ?  parseFloat(min) : get(getLayouInfo(), ['periodRange', 'min']);  // min is invalid or null string
    //
    //return (val > mmin) ? {valid: true} :
    //                      {valid: false, message: description + `: must be greater than ${mmin}`};

    return bVal ? {valid: true} : {valid: false, message: description + `: must be greater than ${min}`};
};

/**
 * @summary validator for minimum period
 * @param description
 * @returns {Function}
 */
function periodMinValidator(description) {
    return (valStr) => {
        return isPeriodMinValid(valStr, description);
    };
}

/**
 * @summary validator for maximum period
 * @param description
 * @returns {Function}
 */
function periodMaxValidator(description) {
    return (valStr) => {
        return isPeriodMaxValid(valStr, description);
    };
}

/**
 * @summary step size validator
 * @param description
 * @returns {Function}
 */
function stepsizeValidator(description) {
    return (valStr) => {
        if (!valStr) return {valid: true};
        var retval = Validate.isFloat(description, valStr);

        if (!retval.valid) return retval;

        var val = parseFloat(valStr);
        const min = 0.0000001;
        var bVal = val > min;

        return bVal ? {valid: true} :
                      {valid: false, message: description + `: must be greater than ${min}`};
    };
}
/**
 * @summary peaks number validator
 * @param description
 * @returns {Function}
 */
function peaksValidator(description) {
    return (valStr) => {
        if (!valStr) return {valid: true};
        var retval = Validate.isInt(description, valStr);

        if (!retval.valid) return retval;

        var val = parseInt(valStr);
        const min = 0;
        var bVal = val > min;

        return bVal ? {valid: true} :
        {valid: false, message: description + `: must be greater than ${min}`};
    };
}

/**
 * @summary reset parameters to the initial values
 * @param groupKey
 */
function resetDefaults(groupKey) {
    const fields = FieldGroupUtils.getGroupFields(groupKey);

    Object.keys(defValues).forEach((fieldKey) => {
        if (has(defPeriodogram, fieldKey) && defPeriodogram[fieldKey].value !== get(fields, [fieldKey, 'value'])) {
            dispatchValueChange({groupKey, fieldKey, value: defPeriodogram[fieldKey].value});
        } else if (has(defPeriod, fieldKey) && defPeriod[fieldKey].value !== get(fields, [fieldKey, 'value'])) {
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
 * @param popupId
 * @param hideDropDown
 * @returns {Function}
 */
function periodogramSuccess(popupId, hideDropDown = false) {
    return (request) => {
        const tbl = getTblById(LC.RAW_TABLE);
        const layoutInfo = getLayouInfo();
        const srcFile = tbl.request.source;

        const pMin = get(request, [pKeyDef.min.fkey]);
        const pMax = get(request, [pKeyDef.max.fkey]);
        const ssize = get(request, [pKeyDef.stepsize.fkey]);
        const peak = get(request, [pKeyDef.peaks.fkey]);

        var tReq2 = makeTblRequest('LightCurveProcessor', LC.PEAK_TABLE, {
            original_table: srcFile,
            x: get(defPeriod, [pKeyDef.time.fkey, 'value']) || get(layoutInfo, [LC.MISSION_DATA, LC.META_TIME_CNAME]),
            y:  get(defPeriod, [pKeyDef.flux.fkey, 'value']) || get(layoutInfo, [LC.MISSION_DATA, LC.META_FLUX_CNAME]),
            alg: get(request, [pKeyDef.algor.fkey]),
            pmin: pMin ? pMin : undefined,
            pmax: pMax ? pMax : undefined,
            step_method: get(request, [pKeyDef.stepmethod.fkey]),
            step_size: ssize ? ssize : undefined,
            peaks: get(request, [pKeyDef.peaks.fkey]),
            table_name: LC.PEAK_TABLE,
            sortInfo: sortInfoString('SDE', false)                 // sort peak table by column SDE, descending
        }, {tbl_id: LC.PEAK_TABLE, pageSize: parseInt(peak)});

        if (tReq2 !== null) {
            dispatchTableSearch(tReq2, {removable: true, tbl_group: LC.PERIODOGRAM_GROUP});
            const xyPlotParams = {
                x: {columnOrExpr: LC.PEAK_CNAME, options: 'grid'},
                y: {columnOrExpr: LC.POWER_CNAME, options: 'grid'}
            };
            loadXYPlot({chartId: LC.PEAK_TABLE, tblId: LC.PEAK_TABLE, xyPlotParams});
        }

        var tReq = makeTblRequest('LightCurveProcessor', LC.PERIODOGRAM_TABLE, {
            original_table: srcFile,
            x: get(defPeriod, [pKeyDef.time.fkey, 'value']) || get(layoutInfo, [LC.MISSION_DATA, LC.META_TIME_CNAME]),
            y: get(defPeriod, [pKeyDef.flux.fkey, 'value']) || get(layoutInfo, [LC.MISSION_DATA, LC.META_FLUX_CNAME]),
            alg: get(request, [pKeyDef.algor.fkey]),
            pmin: pMin ? pMin : undefined,
            pmax: pMax ? pMax : undefined,
            step_method: get(request, [pKeyDef.stepmethod.fkey]),
            step_size: ssize ? ssize : undefined,
            peaks: get(request, [pKeyDef.peaks.fkey]),
            table_name: LC.PERIODOGRAM_TABLE
            /* Should we do the same for Power column in Periodogram? */
            /*sortInfo: sortInfoString('Power', false)*/
        }, {tbl_id: LC.PERIODOGRAM_TABLE});


        if (tReq !== null) {
            dispatchTableSearch(tReq, {removable: true, tbl_group: LC.PERIODOGRAM_GROUP});
            const xyPlotParams = {
                userSetBoundaries: {yMin: 0},
                x: {columnOrExpr: LC.PERIOD_CNAME, options: 'grid,log'},
                y: {columnOrExpr: LC.POWER_CNAME, options: 'grid'}
            };
            loadXYPlot({chartId: LC.PERIODOGRAM_TABLE, tblId: LC.PERIODOGRAM_TABLE, markAsDefault: true, xyPlotParams});
        }

        dispatchActiveTableChanged(LC.PERIODOGRAM_TABLE, LC.PERIODOGRAM_GROUP);
        if (hideDropDown && popupId && isDialogVisible(popupId)) {
            dispatchHideDialog(popupId);
        }

        if (pMin && (pMin !== get(defPeriod, [pKeyDef.min.fkey, 'value']))) {
            dispatchValueChange({fieldKey: pKeyDef.min.fkey, groupKey: LC.FG_PERIOD_FINDER, value: pMin});
        }
        if (pMax && (pMax !== get(defPeriod, [pKeyDef.max.fkey, 'value']))) {
            dispatchValueChange({fieldKey: pKeyDef.max.fkey, groupKey: LC.FG_PERIOD_FINDER, value: pMax});
        }
        updateLayoutDisplay(LC.PERGRAM_PAGE, LC.PERGRAM_PAGE);
    };
}


function periodogramFail() {
    return (request) => {
        return showInfoPopup('Periodogram parameter setting error');
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

        resultLayout = (<SplitPane split='vertical' maxSize={-20} minSize={20} defaultSize={565}>
                            <SplitContent>
                                <div style={{margin: '0 0 5px 6px'}}><ChangePeriodogram/></div>
                                <div style={{height: 'calc(100% - 28px'}}>{tables}</div>
                            </SplitContent>
                            <SplitContent>{xyPlot}</SplitContent>
                        </SplitPane>);
    } else {
        resultLayout = (<div style={{flexGrow: 1}}>
            {expanded === LO_VIEW.tables ? tables : xyPlot}
        </div>);
    }
    return resultLayout;
};

