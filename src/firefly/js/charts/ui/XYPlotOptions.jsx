/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {PropTypes} from 'react';

import {defer, get, isEmpty, isUndefined, omitBy, set} from 'lodash';

import ColValuesStatistics from '../ColValuesStatistics.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {dispatchMultiValueChange, VALUE_CHANGE, MULTI_VALUE_CHANGE} from '../../fieldGroup/FieldGroupCntlr.js';
import Validate from '../../util/Validate.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {FieldGroupCollapsible} from '../../ui/panel/CollapsiblePanel.jsx';
import {plotParamsShape} from  './XYPlot.jsx';
import {hideColSelectPopup} from './ColSelectView.jsx';
import {ColumnOrExpression, getColValidator} from './ColumnOrExpression.jsx';
import {updateSet} from '../../util/WebUtil.js';

const DECI_ENABLE_SIZE = 5000;

const X_AXIS_OPTIONS = [
    {label: 'grid', value: 'grid'},
    {label: 'reverse', value: 'flip'},
    {label: 'top', value: 'opposite'},
    {label: 'log', value: 'log'}
];

const X_AXIS_OPTIONS_NOLOG = X_AXIS_OPTIONS.filter((el) => {return el.label !== 'log';});

const Y_AXIS_OPTIONS = [
    {label: 'grid', value: 'grid'},
    {label: 'reverse', value: 'flip'},
    {label: 'right', value: 'opposite'},
    {label: 'log', value: 'log'}
];

const ERR_TYPE_OPTIONS = [
    {label: 'None', value: 'none'},
    {label: 'Sym', value: 'sym'},
    {label: 'Asym', value: 'asym'}

];

const Y_AXIS_OPTIONS_NOLOG = Y_AXIS_OPTIONS.filter((el) => {return el.label !== 'log';});

const helpStyle = {fontStyle: 'italic', color: '#808080', paddingBottom: 10};


/*
function getUnit(colValStats, colname) {
    const statrow = colValStats.find((el) => { return el.name===colname; });
    if (statrow && statrow.unit && statrow.unit !== 'null') { return statrow.unit; }
    else {return '';}
}
*/

function getErrFldVals(xyPlotParams) {
    const xErr = get(xyPlotParams, 'x.error');
    const xErrLow = get(xyPlotParams, 'x.errorLow');
    const xErrHigh = get(xyPlotParams, 'x.errorHigh');
    const xErrType = xErr ? 'sym' : (xErrLow || xErrHigh) ? 'asym' : 'none';

    const yErr = get(xyPlotParams, 'y.error');
    const yErrLow = get(xyPlotParams, 'y.errorLow');
    const yErrHigh = get(xyPlotParams, 'y.errorHigh');
    const yErrType = yErr ? 'sym' : (yErrLow || yErrHigh) ? 'asym' : 'none';

    return {xErrType, xErr, xErrLow, xErrHigh, yErrType, yErr, yErrLow, yErrHigh};
}

export function resultsSuccess(callback, flds, optionParameters) {
    const tblId = optionParameters.tblId;

    const xName = get(flds, ['x.columnOrExpr']);
    const yName = get(flds, ['y.columnOrExpr']);

    const zoom = (xName!== get(optionParameters, ['x','columnOrExpr']) || yName!==get(optionParameters, ['y','columnOrExpr']))? undefined:optionParameters.zoom;

    const plotStyle = get(flds, ['plotStyle']);
    const sortColOrExpr = (plotStyle === 'line' || plotStyle === 'linepoints') ? xName : undefined;

    const xOptions = get(flds, ['x.options']);
    const xLabel = get(flds, ['x.label']);
    const xUnit = get(flds, ['x.unit']);

    const yOptions = get(flds, ['y.options']);
    const yLabel = get(flds, ['y.label']);
    const yUnit = get(flds, ['y.unit']);

    const nbinsX = get(flds, ['nbins.x']);
    const nbinsY = get(flds, ['nbins.y']);

    let {xMin, xMax, yMin, yMax} = flds;  // string values
    [xMin, xMax, yMin, yMax] = [xMin, xMax, yMin, yMax].map((v) => { return isFinite(parseFloat(v)) ? Number(v) : undefined; });
    let userSetBoundaries = omitBy({xMin, xMax, yMin, yMax}, isUndefined);
    userSetBoundaries = isEmpty(userSetBoundaries) ? undefined : userSetBoundaries;
    const xyRatio = parseFloat(flds.xyRatio);

    const xyPlotParams = omitBy({
        plotStyle,
        sortColOrExpr,
        userSetBoundaries,
        xyRatio : Number.isFinite(xyRatio) ? xyRatio : undefined,
        stretch : flds.stretch,
        nbins : (nbinsX && nbinsY) ? {x: Number(nbinsX), y: Number(nbinsY)} : undefined,
        shading: flds.shading || undefined,
        x : { columnOrExpr : xName, label : xLabel, unit : xUnit, options : xOptions},
        y : { columnOrExpr : yName, label : yLabel, unit : yUnit, options : yOptions},
        tblId,
        zoom
    }, isUndefined);

    // set error related parameters
    ['x', 'y'].forEach((axis) => {
        const errorsType = get(flds, [`${axis}.errorsType`]);
        if (errorsType && errorsType !== 'none') {
            if (errorsType === 'sym') {
                const fldPath = `${axis}.error`;
                const err = get(flds, [fldPath]);
                if (err) {
                    set(xyPlotParams, fldPath, err);
                }
            } else if (errorsType === 'asym') {
                const fldPathLow = `${axis}.errorLow`;
                const errLow = get(flds, [fldPathLow]);
                if (errLow) {
                    set(xyPlotParams, fldPathLow, errLow);
                }
                const fldPathHigh = `${axis}.errorHigh`;
                const errHigh = get(flds, [fldPathHigh]);
                if (errHigh) {
                    set(xyPlotParams, fldPathHigh, errHigh);
                }
            }
        }
    });

    callback(xyPlotParams);
}

export function resultsFail() {
    // TODO: do I need to do anything here?
}

export function setOptions(groupKey, xyPlotParams) {
    const {xErr, xErrLow, xErrHigh, yErr, yErrLow, yErrHigh} = getErrFldVals(xyPlotParams);
    const flds = [
        {fieldKey: 'plotStyle', value: get(xyPlotParams, 'plotStyle', 'points')},
        {fieldKey: 'x.columnOrExpr', value: get(xyPlotParams, 'x.columnOrExpr')},
        {fieldKey: 'x.error', value: xErr},
        {fieldKey: 'x.errorLow', value: xErrLow},
        {fieldKey: 'x.errorHigh', value: xErrHigh},
        {fieldKey: 'x.label', value: get(xyPlotParams, 'x.label')},
        {fieldKey: 'x.unit', value: get(xyPlotParams, 'x.unit')},
        {fieldKey: 'x.options', value: get(xyPlotParams, 'x.options', '_none_')},
        {fieldKey: 'y.columnOrExpr', value: get(xyPlotParams, 'y.columnOrExpr')},
        {fieldKey: 'y.error', value: yErr},
        {fieldKey: 'y.errorLow', value: yErrLow},
        {fieldKey: 'y.errorHigh', value: yErrHigh},
        {fieldKey: 'y.label', value: get(xyPlotParams, 'y.label')},
        {fieldKey: 'y.unit', value: get(xyPlotParams, 'y.unit')},
        {fieldKey: 'y.options', value: get(xyPlotParams, 'y.options', '_none_')},
        {fieldKey: 'xMin', value: get(xyPlotParams, 'userSetBoundaries.xMin')},
        {fieldKey: 'xMax', value: get(xyPlotParams, 'userSetBoundaries.xMax')},
        {fieldKey: 'yMin', value: get(xyPlotParams, 'userSetBoundaries.yMin')},
        {fieldKey: 'yMax', value: get(xyPlotParams, 'userSetBoundaries.yMax')},
        {fieldKey: 'xyRatio', value: get(xyPlotParams, 'xyRatio')},
        {fieldKey: 'stretch', value: get(xyPlotParams, 'stretch', 'fit')},
        {fieldKey: 'nbins.x', value: get(xyPlotParams, 'nbins.x')},
        {fieldKey: 'nbins.y', value: get(xyPlotParams, 'nbins.y')},
        {fieldKey: 'shading', value: get(xyPlotParams, 'shading', 'lin')}
    ];
    dispatchMultiValueChange(groupKey, flds);
}

/**
 * Reducer from field group component,
 *   clears label, unit, and userSetBoundaries whenever x or y field changes,
 *   validates min-max-log field relationship
 *   @param {Object} inFields
 *   @param {Object} action
 * @returns {*} reducer, which clears label and unit whenever x or y field changes
 *
 */
function fldChangeReducer(inFields, action) {
    if (!inFields) { return {}; }
    let fieldKey = undefined;
    if (action.type === VALUE_CHANGE) {
        // when field changes, clear the label and unit
        fieldKey = get(action.payload, 'fieldKey');
        if (fieldKey === 'x.columnOrExpr') {
            inFields = updateSet(inFields, ['x.error', 'value'], undefined);
            inFields = updateSet(inFields, ['x.label', 'value'], undefined);
            inFields = updateSet(inFields, ['x.unit', 'value'], undefined);
            inFields = updateSet(inFields, ['xMin', 'value'], undefined);
            inFields = updateSet(inFields, ['xMax', 'value'], undefined);
            const currOptions = get(inFields, ['x.options', 'value']);
            // do not reset grid selection
            inFields = updateSet(inFields, ['x.options', 'value'], getOption(currOptions, 'grid'));
        } else if (fieldKey === 'y.columnOrExpr') {
            inFields = updateSet(inFields, ['y.error', 'value'], undefined);
            inFields = updateSet(inFields, ['y.label', 'value'], undefined);
            inFields = updateSet(inFields, ['y.unit', 'value'], undefined);
            inFields = updateSet(inFields, ['yMin', 'value'], undefined);
            inFields = updateSet(inFields, ['yMax', 'value'], undefined);
            const currOptions = get(inFields, ['y.options', 'value']);
            // do not reset grid selection
            inFields = updateSet(inFields, ['y.options', 'value'], getOption(currOptions, 'grid'));
        } else if (fieldKey === 'xyRatio') {
            if (get(inFields, 'nbins.x')) {
                inFields = updateSet(inFields, ['nbins.x', 'value'], undefined);
                inFields = updateSet(inFields, ['nbins.y', 'value'], undefined);
            }
        }
    }
    if (action.type === MULTI_VALUE_CHANGE || action.type === VALUE_CHANGE) {
        // validate min/max/log relationship
        const fldsets =[
            {min: 'xMin', max: 'xMax', options: 'x.options'},
            {min: 'yMin', max: 'yMax', options: 'y.options'}
        ];
        fldsets.forEach(
            (v) => {
                if (!fieldKey || fieldKey === v.min || fieldKey === v.max || fieldKey === v.options) {
                    const valMin = Number.parseFloat(get(inFields, [v.min, 'value']));
                    const options = get(inFields, [v.options, 'value']);
                    const logVal = Boolean(options && options.includes('log'));
                    if (Number.isFinite(valMin)) {
                        if (logVal && valMin <= 0) {
                            inFields = updateSet(inFields, [v.min, 'valid'], false);
                            inFields = updateSet(inFields, [v.min, 'message'], 'The minimum of a log axis can not be 0 or less');
                        } else {
                            const valMax = Number.parseFloat(get(inFields, [v.max, 'value']));
                            if (Number.isFinite(valMax) && valMin > valMax) {
                                inFields = updateSet(inFields, [fieldKey||v.min, 'valid'], false);
                                inFields = updateSet(inFields, [fieldKey||v.min , 'message'], 'Min value greater than max');
                            }
                        }
                    }
                }
            });
    }
    return inFields;
}

function possibleDecimatedTable(colValStats) {
    return Boolean(colValStats.find((el) => { return el.numpoints>DECI_ENABLE_SIZE; }));
}

function getOption(options, opt) {
    // returns opt if it is included into options
    return (options && (options.includes(opt)||options.includes('_all_'))) ? opt : undefined;
}


export class XYPlotOptions extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            displayStretchOptions : Boolean(get(props, ['xyPlotParams', 'xyRatio']))
        };
        this.setStretchOptionsVisibility = this.setStretchOptionsVisibility.bind(this);
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
        hideColSelectPopup();
    }

    componentDidMount() {
        this.unbinder = FieldGroupUtils.bindToStore(this.props.groupKey,
            (fields) => {
                this.setStretchOptionsVisibility(Boolean(get(fields, ['xyRatio', 'value']) && get(fields, ['xyRatio', 'valid'])));
            });

        // make sure column validator matches current columns
        const {colValStats, groupKey, xyPlotParams} = this.props;
        if (colValStats) {
            const colValidator = getColValidator(colValStats);
            const colValidatorOptValue = getColValidator(colValStats, false); // value can be empty
            const flds = [
                {fieldKey: 'x.columnOrExpr', validator: colValidator},
                {fieldKey: 'x.error', validator: colValidatorOptValue},
                {fieldKey: 'x.errorLow', validator: colValidatorOptValue},
                {fieldKey: 'x.errorHigh', validator: colValidatorOptValue},
                {fieldKey: 'y.columnOrExpr', validator: colValidator},
                {fieldKey: 'y.error', validator: colValidatorOptValue},
                {fieldKey: 'y.errorLow', validator: colValidatorOptValue},
                {fieldKey: 'y.errorHigh', validator: colValidatorOptValue}
            ];
            dispatchMultiValueChange(groupKey, flds);
            if (xyPlotParams) {
                defer(setOptions, groupKey, xyPlotParams);
            }
        }
        this.iAmMounted= true;
    }

    shouldComponentUpdate(np, ns) {
        return this.props.groupKey !== np.groupKey || this.props.colValStats !== np.colValStats ||
            this.props.xyPlotParams !== np.xyPlotParams || this.state !== ns;
    }

    componentWillReceiveProps(np) {
        if (this.props.xyPlotParams !== np.xyPlotParams) {
            defer(setOptions, np.groupKey, np.xyPlotParams);
            this.setStretchOptionsVisibility(Boolean(get(np, ['xyPlotParams', 'xyRatio'])));
        }
    }

    setStretchOptionsVisibility(displayStretchOptions) {
        if (this.iAmMounted && displayStretchOptions  !== this.state.displayStretchOptions) {
            this.setState({displayStretchOptions});
        }
    }

    renderBinningOptions() {
        const { colValStats, groupKey, xyPlotParams }= this.props;
        const displayBinningOptions = possibleDecimatedTable(colValStats);
        if (displayBinningOptions) {
            return ( <FieldGroupCollapsible  header='Binning Options'
                                             initialState= {{ value:'closed' }}
                                             fieldKey='binningOptions'>
                <ValidationField style={{width:50}}
                                 initialState= {{
                        value: get(xyPlotParams, 'nbins.x'),
                        nullAllowed : true,
                        validator: Validate.intRange.bind(null, 1, 300, 'X-Bins'),
                        tooltip: 'Number of bins along X axis',
                        label : 'Number of X-Bins:'
                    }}
                                 fieldKey='nbins.x'
                                 groupKey={groupKey}
                                 labelWidth={90}/>
                <ValidationField style={{width:50}}
                                 initialState= {{
                        value: get(xyPlotParams, 'nbins.y'),
                        nullAllowed : true,
                        validator: Validate.intRange.bind(null, 1, 300, 'Y-Bins'),
                        tooltip: 'Number of bins along Y axis',
                        label : 'Number of Y-Bins:'
                    }}
                                 fieldKey='nbins.y'
                                 groupKey={groupKey}
                                 labelWidth={90}/>
                <br/>
                <RadioGroupInputField
                    alignment='horizontal'
                    initialState= {{
                        value: get(xyPlotParams, 'shading', 'lin'),
                        tooltip: 'When assigning shades to the number of bins, should we use linear or logarithmic scale?',
                        label : 'Shading:'
                    }}
                    options={[
                        {label: 'Linear', value: 'lin'},
                        {label: 'Logarithmic', value: 'log'}
                    ]}
                    fieldKey='shading'
                    groupKey={groupKey}
                    labelWidth={60}
                />
            </FieldGroupCollapsible> );
        } else { return null; }
    }


    renderMoreOptions() {
        const { groupKey, xyPlotParams } = this.props;
        const labelWidth = 35;
        //<FieldGroupCollapsible  header='More Options'
        //                        initialState= {{ value:'closed' }}
        //                        fieldKey='moreoptions'>
        return (
            <div>
                <div style={helpStyle}>
                    Set plot boundaries if different from data range.
                </div>
                <div style={{display: 'flex', flexDirection: 'row', padding: '5px 15px 15px'}}>
                    <div style={{paddingRight: 5}}>
                        <ValidationField
                            style={{width:55}}
                            initialState= {{
                                value: get(xyPlotParams, ['userSetBoundaries','xMin']),
                                validator: (val)=>Validate.isFloat('X Min', val),
                                tooltip: 'Minimum X value',
                                label : 'X Min:'
                            }}
                            fieldKey='xMin'
                            groupKey={groupKey}
                            labelWidth={labelWidth}/>
                        <ValidationField
                            style={{width:55}}
                            initialState= {{
                                value: get(xyPlotParams, ['userSetBoundaries','yMin']),
                                validator: (val)=>Validate.isFloat('Y Min', val),
                                tooltip: 'Minimum Y value',
                                label : 'Y Min:'
                            }}
                            fieldKey='yMin'
                            groupKey={groupKey}
                            labelWidth={labelWidth}/>
                    </div>
                    <div style={{paddingRight: 5}}>
                        <ValidationField
                            style={{width:55}}
                            initialState= {{
                                value: get(xyPlotParams, ['userSetBoundaries','xMax']),
                                validator: (val)=>Validate.isFloat('X Max', val),
                                tooltip: 'Maximum X value',
                                label : 'X Max:'
                            }}
                            fieldKey='xMax'
                            groupKey={groupKey}
                            labelWidth={labelWidth}/>
                        <ValidationField
                            style={{width:55}}
                            initialState= {{
                                value: get(xyPlotParams, ['userSetBoundaries','yMax']),
                                validator: (val)=>Validate.isFloat('Y Max', val),
                                tooltip: 'Maximum Y value',
                                label : 'Y Max:'
                            }}
                            fieldKey='yMax'
                            groupKey={groupKey}
                            labelWidth={labelWidth}/>
                    </div>
                </div>
                <div style={helpStyle}>
                    Enter display aspect ratio below.<br/>
                    Leave it blank to use all available space.<br/>
                </div>
                <ValidationField style={{width:30}}
                                 initialState= {{
                        value: get(xyPlotParams, 'xyRatio'),
                        validator: Validate.floatRange.bind(null, 1, 10, 1, 'X/Y ratio'),
                        tooltip: 'X/Y ratio',
                        label : 'X/Y ratio:'
                    }}
                                 fieldKey='xyRatio'
                                 groupKey={groupKey}
                                 labelWidth={60}/>
                <br/>
                {this.state.displayStretchOptions && <RadioGroupInputField
                    alignment='horizontal'
                    initialState= {{
                        value: get(xyPlotParams, 'stretch', 'fit'),
                        tooltip: 'Should the plot fit into the available space or fill the available width?',
                        label : 'Stretch to:'
                    }}
                    options={[
                        {label: 'Fit', value: 'fit'},
                        {label: 'Fill', value: 'fill'}
                    ]}
                    fieldKey='stretch'
                    groupKey={groupKey}
                    labelWidth={60}
                />}
            </div>
        );
        //</FieldGroupCollapsible>
    }

    render() {

        const { colValStats, groupKey, xyPlotParams, defaultParams, onOptionsSelected}= this.props;

        const largeTable = possibleDecimatedTable(colValStats);

        const xProps = {colValStats, params:xyPlotParams, groupKey, fldPath:'x.columnOrExpr', label:'X:', name:'X', tooltip:'X Axis', nullAllowed:false};
        const yProps = {colValStats, params:xyPlotParams, groupKey, fldPath:'y.columnOrExpr', label:'Y:', name:'Y', tooltip:'Y Axis', nullAllowed:false};
        const {xErrType, yErrType} = getErrFldVals(xyPlotParams);

        return (
            <div style={{padding:'0 5px 7px'}}>
                <FieldGroup groupKey={groupKey} validatorFunc={null} keepState={true}
                    reducerFunc={fldChangeReducer}>
                    {onOptionsSelected &&
                    <div style={{display: 'flex', flexDirection: 'row', padding: '5px 0 15px'}}>
                        <CompleteButton style={{flexGrow: 0}}
                                        groupKey={groupKey}
                                        onSuccess={(flds) => resultsSuccess(onOptionsSelected, flds, xyPlotParams)}
                                        onFail={resultsFail}
                                        text = 'Apply'
                        />
                        <div style={{flexGrow: 1}}/>
                        <div style={{flexGrow: 0}}>
                            <button type='button' className='button std' onClick={() => setOptions(groupKey, {})}>Clear</button>
                            <button type='button' className='button std' onClick={() => setOptions(groupKey, defaultParams)}>Reset</button>
                        </div>
                    </div>}

                    {!largeTable && <RadioGroupInputField
                        alignment='horizontal'
                        initialState= {{
                            value: get(xyPlotParams, 'plotStyle', 'points'),
                            tooltip: 'Select plot style',
                            label : 'Plot Style:'
                        }}
                        options={[
                            {label: 'Points', value: 'points'},
                            {label: 'Connected Points', value: 'linepoints'},
                            {label: 'Line', value: 'line'}
                        ]}
                        fieldKey='plotStyle'
                        groupKey={groupKey}
                        labelWidth={50}
                    />}
                    {!largeTable && <br/>}
                    <div style={helpStyle}>
                        For X and Y, enter a column or an expression<br/>
                        ex. log(col); 100*col1/col2; col1-col2
                    </div>
                    <ColumnOrExpression {...xProps}/>
                    {!largeTable && <Errors axis='x' errType={xErrType} {...{groupKey, xyPlotParams, colValStats}}/>}


                    <FieldGroupCollapsible  header='X Label/Unit/Options'
                                            initialState= {{ value:'closed' }}
                                            fieldKey='xplotoptions'>
                        <ValidationField
                            initialState= {{
                                value: get(xyPlotParams, 'x.label'),
                                validator() { return {valid: true,message: ''}; },
                                tooltip: 'X axis label',
                                label : 'Label:'
                            }}
                            fieldKey='x.label'
                            groupKey={groupKey}
                            labelWidth={35}/>
                        <ValidationField
                            initialState= {{
                                value: get(xyPlotParams, 'x.unit'),
                                validator() { return {valid: true,message: ''}; },
                                tooltip: 'X axis unit',
                                label : 'Unit:'
                                }}
                            fieldKey='x.unit'
                            groupKey={groupKey}
                            labelWidth={35}/>

                        <br/>
                        <CheckboxGroupInputField
                            initialState= {{
                                value: get(xyPlotParams, 'x.options', '_none_'),
                                tooltip: 'Check if you would like to plot grid',
                                label : 'Options:'
                            }}
                            options={largeTable ? X_AXIS_OPTIONS_NOLOG : X_AXIS_OPTIONS}
                            fieldKey='x.options'
                            groupKey={groupKey}
                            labelWidth={50}
                        />
                    </FieldGroupCollapsible>
                    <br/>

                    <ColumnOrExpression {...yProps}/>
                    {!largeTable && <Errors axis='y' errType={yErrType} {...{groupKey, xyPlotParams, colValStats}}/>}

                    <FieldGroupCollapsible  header='Y Label/Unit/Options'
                                            initialState= {{ value:'closed' }}
                                            fieldKey='yplotoptions'>
                        <ValidationField
                            initialState= {{
                                value: get(xyPlotParams, 'y.label'),
                                validator() { return {valid: true,message: ''}; },
                                tooltip: 'Y axis label',
                                label : 'Label:'
                            }}
                            fieldKey='y.label'
                            groupKey={groupKey}
                            labelWidth={35}/>
                        <ValidationField
                            initialState= {{
                                value: get(xyPlotParams, 'y.unit'),
                                validator() { return {valid: true,message: ''}; },
                                tooltip: 'Y axis unit',
                                label : 'Unit:'
                                }}
                            fieldKey='y.unit'
                            groupKey={groupKey}
                            labelWidth={35}/>

                        <br/>
                        <CheckboxGroupInputField
                            initialState= {{
                                value: get(xyPlotParams, 'y.options', '_none_'),
                                tooltip: 'Check if you would like to plot grid',
                                label : 'Options:'

                            }}
                            options={largeTable ? Y_AXIS_OPTIONS_NOLOG : Y_AXIS_OPTIONS}
                            fieldKey='y.options'
                            groupKey={groupKey}
                            labelWidth={50}
                        />
                        <br/>
                    </FieldGroupCollapsible>
                    <br/>
                    {this.renderMoreOptions()}
                    {this.renderBinningOptions()}
                    <br/>

                </FieldGroup>

            </div>
        );
    }
}

XYPlotOptions.propTypes = {
    groupKey: PropTypes.string.isRequired,
    colValStats: PropTypes.arrayOf(PropTypes.instanceOf(ColValuesStatistics)).isRequired,
    onOptionsSelected: PropTypes.func,
    xyPlotParams: plotParamsShape,
    defaultParams: plotParamsShape
};


class Errors extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            selectedErrType : get(FieldGroupUtils.getGroupFields(props.groupKey),[`${props.axis}.errorsType`,'value'], props.errType)
        };
    }

    componentDidMount() {
        const {axis, groupKey, errType} = this.props;
        this.unbinder = FieldGroupUtils.bindToStore(groupKey,
            (fields) => {
                if (this.iAmMounted) {
                    const v = get(fields, [`${axis}.errorsType`, 'value'], errType);
                    if (v !== this.state.selectedErrType) {
                        this.setState({selectedErrType: v});
                    }
                }
            });
        this.iAmMounted = true;
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
    }


    renderErrFld(props) {
        const {colValStats, groupKey, xyPlotParams} = this.props;
        const commonProps = {colValStats, params:xyPlotParams, groupKey, labelWidth: 5, nullAllowed:true};
        const allProps = Object.assign({}, commonProps, props);
        return  (<ColumnOrExpression {...allProps}/>);
    }


    render() {
        const {groupKey, axis} = this.props;
        const errType = this.state.selectedErrType;
        const axisU = axis.toUpperCase();

        return (
            <div style={{display: 'flex', alignItems: 'center', paddinngTop: 3}}>
                <ListBoxInputField
                    initialState= {{
                        value: errType,
                        tooltip: 'Select type of the errors',
                        label: 'Error:',
                        labelWidth: 30
                    }}
                    options={ERR_TYPE_OPTIONS}
                    fieldKey={`${axis}.errorsType`}
                    groupKey={groupKey}
                />
                <div style={{paddingLeft: 10}}>
                {(errType==='sym') && this.renderErrFld({fldPath:`${axis}.error`, name:`${axisU} Error`})}
                {(errType==='asym') && this.renderErrFld({fldPath:`${axis}.errorHigh`, label: '\u2191', name:`${axisU} Upper Error`})}
                {(errType==='asym') && this.renderErrFld({fldPath:`${axis}.errorLow`, label: '\u2193', name:`${axisU} Lower Error`})}
                </div>
            </div>
        );
    }
}


Errors.propTypes = {
    groupKey: PropTypes.string.isRequired,
    colValStats: PropTypes.arrayOf(PropTypes.instanceOf(ColValuesStatistics)).isRequired,
    xyPlotParams: plotParamsShape,
    axis: PropTypes.string.isRequired,
    errType: PropTypes.string.isRequired
};