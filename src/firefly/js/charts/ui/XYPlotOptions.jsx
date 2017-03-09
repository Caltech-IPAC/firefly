/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {PropTypes} from 'react';

import {get, isEmpty, isUndefined, omitBy, defer} from 'lodash';

import ColValuesStatistics from '../ColValuesStatistics.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {dispatchMultiValueChange, VALUE_CHANGE, MULTI_VALUE_CHANGE} from '../../fieldGroup/FieldGroupCntlr.js';
import Validate from '../../util/Validate.js';
import {Expression} from '../../util/expr/Expression.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {FieldGroupCollapsible} from '../../ui/panel/CollapsiblePanel.jsx';
import {plotParamsShape} from  './XYPlot.jsx';
import {hideColSelectPopup} from './ColSelectView.jsx';
import {ColumnOrExpression} from './ColumnOrExpression.jsx';
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

const Y_AXIS_OPTIONS_NOLOG = Y_AXIS_OPTIONS.filter((el) => {return el.label !== 'log';});

const helpStyle = {fontStyle: 'italic', color: '#808080', paddingBottom: 10};


/*
function getUnit(colValStats, colname) {
    const statrow = colValStats.find((el) => { return el.name===colname; });
    if (statrow && statrow.unit && statrow.unit !== 'null') { return statrow.unit; }
    else {return '';}
}
*/

export function resultsSuccess(callback, flds, optionParameters) {
    const tblId = optionParameters.tblId;

    const xName = get(flds, ['x.columnOrExpr']);
    const yName = get(flds, ['y.columnOrExpr']);

    const zoom = (xName!== get(optionParameters, ['x','columnOrExpr']) || yName!==get(optionParameters, ['y','columnOrExpr']))? undefined:optionParameters.zoom;
    const xErr = get(flds, ['x.error']);
    const yErr = get(flds, ['y.error']);

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


    /*
      const axisParamsShape = PropTypes.shape({
         columnOrExpr : PropTypes.string,
         label : PropTypes.string,
         unit : PropTypes.string,
         options : PropTypes.string, // ex. 'grid,log,flip'
      });

      const xyPlotParamsShape = PropTypes.shape({
         userSetBoundaries : PropTypes.selection,
         xyRatio : PropTypes.string,
         stretch : PropTypes.oneOf(['fit','fill']),
         nbins : PropTypes.shape({x : PropTypes.number, y : PropTypes.number}),
         shading : PropTypes.oneOf(['lin', 'log']),
         x : axisParamsShape,
         y : axisParamsShape
      });
      */

    const xyPlotParams = omitBy({
        plotStyle,
        sortColOrExpr,
        userSetBoundaries,
        xyRatio : Number.isFinite(xyRatio) ? xyRatio : undefined,
        stretch : flds.stretch,
        nbins : (nbinsX && nbinsY) ? {x: Number(nbinsX), y: Number(nbinsY)} : undefined,
        shading: flds.shading || undefined,
        x : { columnOrExpr : xName, error: xErr, label : xLabel, unit : xUnit, options : xOptions},
        y : { columnOrExpr : yName, error: yErr, label : yLabel, unit : yUnit, options : yOptions},
        tblId,
        zoom,
    }, isUndefined);

    if (xErr || yErr) {
        if (xErr) { xyPlotParams.x.error = xErr; }
        if (yErr) { xyPlotParams.y.error = yErr; }
    }

    callback(xyPlotParams);
}

export function resultsFail() {
    // TODO: do I need to do anything here?
}


export function setOptions(groupKey, xyPlotParams) {
    const flds = [
        {fieldKey: 'plotStyle', value: get(xyPlotParams, 'plotStyle', 'points')},
        {fieldKey: 'x.columnOrExpr', value: get(xyPlotParams, 'x.columnOrExpr')},
        {fieldKey: 'x.error', value: get(xyPlotParams, 'x.error')},
        {fieldKey: 'x.label', value: get(xyPlotParams, 'x.label')},
        {fieldKey: 'x.unit', value: get(xyPlotParams, 'x.unit')},
        {fieldKey: 'x.options', value: get(xyPlotParams, 'x.options', '_none_')},
        {fieldKey: 'y.columnOrExpr', value: get(xyPlotParams, 'y.columnOrExpr')},
        {fieldKey: 'y.error', value: get(xyPlotParams, 'y.error')},
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

export function getColValidator(colValStats, required=true) {
    const colNames = colValStats.map((colVal) => {return colVal.name;});
    return (val) => {
        let retval = {valid: true, message: ''};
        if (!val) {
            if (required) {
                return {valid: false, message: 'Can not be empty. Please provide value or expression'};
            }
        } else if (colNames.indexOf(val) < 0) {
            const expr = new Expression(val, colNames);
            if (!expr.isValid()) {
                retval = {valid: false, message: `${expr.getError().error}. Unable to parse ${val}.`};
            }
        }
        return retval;
    };
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
                {fieldKey: 'y.columnOrExpr', validator: colValidator},
                {fieldKey: 'y.error', validator: colValidatorOptValue}
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

        const xProps = {colValStats,params:xyPlotParams,groupKey,fldPath:'x.columnOrExpr',label:'X',tooltip:'X Axis',nullAllowed:false};
        const yProps = {colValStats,params:xyPlotParams,groupKey,fldPath:'y.columnOrExpr',label:'Y',tooltip:'Y Axis',nullAllowed:false};
        const xErrProps = {colValStats,params:xyPlotParams,groupKey,fldPath:'x.error',label:'X Err',tooltip:'X Error',nullAllowed:true};
        const yErrProps = {colValStats,params:xyPlotParams,groupKey,fldPath:'y.error',label:'Y Err',tooltip:'Y Error',nullAllowed:true};

        //var plotStyle = (xyPlotParams && xyPlotParams.plotStyle)? xyPlotParams.plotStyle:undefined;
        //const testParams = {plotStyle:plotStyle, x:(defaultParams && defaultParams.x)?defaultParams.x:undefined
       //     , y:(defaultParams && defaultParams.y)?defaultParams.y:undefined};
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
                            <button type='button' className='button std' onClick={() => setOptions(groupKey, {plotStyle:xyPlotParams.plotStyle})}>Clear</button>
                            <button type='button' className='button std' onClick={() => setOptions(groupKey, {plotStyle:xyPlotParams.plotStyle, x:defaultParams.x, y:defaultParams.y})}>Reset</button>
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
                    {!largeTable && <ColumnOrExpression {...xErrProps}/>}

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
                    {!largeTable && <ColumnOrExpression {...yErrProps}/>}

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

