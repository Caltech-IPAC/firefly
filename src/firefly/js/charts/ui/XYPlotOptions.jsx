/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {PropTypes} from 'react';

import {get, isEmpty, isUndefined, omitBy, defer, set} from 'lodash';

import ColValuesStatistics from './../ColValuesStatistics.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {dispatchValueChange, dispatchMultiValueChange, VALUE_CHANGE, MULTI_VALUE_CHANGE} from '../../fieldGroup/FieldGroupCntlr.js';
import Validate from '../../util/Validate.js';
import {Expression} from '../../util/expr/Expression.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import {FieldGroupCollapsible} from '../../ui/panel/CollapsiblePanel.jsx';
import {plotParamsShape} from  './XYPlot.jsx';
import {showColSelectPopup, hideColSelectPopup} from './ColSelectView.jsx';

const DECI_ENABLE_SIZE = 5000;

const AXIS_OPTIONS = [
    {label: 'grid', value: 'grid'},
    {label: 'reverse', value: 'flip'},
    {label: 'log', value: 'log'}
];

const AXIS_OPTIONS_NOLOG = AXIS_OPTIONS.filter((el) => {return el.label !== 'log';});

const helpStyle = {fontStyle: 'italic', color: '#808080', paddingBottom: 10};
import {TextButton} from '../../ui/TextButton.jsx';

/*
 * Split content into prior content and the last alphanumeric token in the text
 * @param {string} text - current content of suggest box
 * @return {Object} with token and priorContent properties
 */
function parseSuggestboxContent(text) {
    let token='', priorContent='';
    if (text && text.length) {
        // [entireMatch, firstCature, secondCapture] or null
        const match =  text.match(/^(.*[^A-Za-z\d_]|)([A-Za-z\d_]*)$/);
        if (match && match.length == 3) {
            priorContent = match[1];
            token = match[2];
        }
    }
    return {token, priorContent};
}

/*
function getUnit(colValStats, colname) {
    const statrow = colValStats.find((el) => { return el.name===colname; });
    if (statrow && statrow.unit && statrow.unit !== 'null') { return statrow.unit; }
    else {return '';}
}
*/

export function resultsSuccess(callback, flds) {
    const xName = get(flds, ['x.columnOrExpr']);
    const yName = get(flds, ['y.columnOrExpr']);

    const xOptions = get(flds, ['x.options']);
    const xLabel = get(flds, ['x.label']);
    const xUnit = get(flds, ['x.unit']);

    const yOptions = get(flds, ['y.options']);
    const yLabel = get(flds, ['y.label']);
    const yUnit = get(flds, ['y.unit']);

    const nbinsX = get(flds, ['nbins.x']);
    const nbinsY = get(flds, ['nbins.y']);

    let {xMin, xMax, yMin, yMax} = flds;  // string values
    [xMin, xMax, yMin, yMax] = [xMin, xMax, yMin, yMax].map((v) => { return (v && isFinite(v)) ? Number(v) : undefined; });
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
        userSetBoundaries,
        xyRatio : Number.isFinite(xyRatio) ? xyRatio : undefined,
        stretch : flds.stretch,
        nbins : (nbinsX && nbinsY) ? {x: Number(nbinsX), y: Number(nbinsY)} : undefined,
        shading: flds.shading || undefined,
        x : { columnOrExpr : xName, label : xLabel, unit : xUnit, options : xOptions},
        y : { columnOrExpr : yName, label : yLabel, unit : yUnit, options : yOptions}
    }, isUndefined);

    callback(xyPlotParams);
}

export function resultsFail() {
    // TODO: do I need to do anything here?
}


export function setOptions(groupKey, xyPlotParams) {
    const flds = [
        {fieldKey: 'x.columnOrExpr', value: get(xyPlotParams, 'x.columnOrExpr')},
        {fieldKey: 'x.label', value: get(xyPlotParams, 'x.label')},
        {fieldKey: 'x.unit', value: get(xyPlotParams, 'x.unit')},
        {fieldKey: 'x.options', value: get(xyPlotParams, 'x.options', '_none_')},
        {fieldKey: 'y.columnOrExpr', value: get(xyPlotParams, 'y.columnOrExpr')},
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

export function getColValidator(colValStats) {
    const colNames = colValStats.map((colVal) => {return colVal.name;});
    return (val) => {
        let retval = {valid: true, message: ''};
        if (!val) {
            return {valid: false, message: 'Can not be empty. Please provide value or expression'};
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
 * @returns {*} reducer, which clears label and unit whenever x or y field changes
 */
function fldChangeReducer(inFields, action) {
    if (!inFields) { return {}; }
    let fieldKey = undefined;
    if (action.type === VALUE_CHANGE) {
        // when field changes, clear the label and unit
        fieldKey = get(action.payload, 'fieldKey');
        if (fieldKey === 'x.columnOrExpr') {
            set(inFields, ['x.label', 'value'], undefined);
            set(inFields, ['x.unit', 'value'], undefined);
            set(inFields, ['xMin', 'value'], undefined);
            set(inFields, ['xMax', 'value'], undefined);
        } else if (fieldKey === 'y.columnOrExpr') {
            set(inFields, ['y.label', 'value'], undefined);
            set(inFields, ['y.unit', 'value'], undefined);
            set(inFields, ['yMin', 'value'], undefined);
            set(inFields, ['yMax', 'value'], undefined);
        } else if (fieldKey === 'xyRatio') {
            if (get(inFields, 'nbins.x')) {
                set(inFields, ['nbins.x', 'value'], undefined);
                set(inFields, ['nbins.y', 'value'], undefined);
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
                            set(inFields, [v.min, 'valid'], false);
                            set(inFields, [v.min, 'message'], 'The minimum of a log axis can not be 0 or less');
                        } else {
                            const valMax = Number.parseFloat(get(inFields, [v.max, 'value']));
                            if (Number.isFinite(valMax) && valMin > valMax) {
                                set(inFields, [fieldKey||v.min, 'valid'], false);
                                set(inFields, [fieldKey||v.min , 'message'], 'Min value greater than max');
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
            const flds = [
                {fieldKey: 'x.columnOrExpr', validator: colValidator},
                {fieldKey: 'y.columnOrExpr', validator: colValidator}
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
        const { colValStats, groupKey, xyPlotParams, onOptionsSelected}= this.props;

        // the suggestions are indexes in the colValStats array - it makes it easier to render then with labels
        const allSuggestions = colValStats.map((colVal,idx)=>{return idx;});

        const getSuggestions = (val)=>{
            const {token} = parseSuggestboxContent(val);
            const matches = allSuggestions.filter( (idx)=>{return colValStats[idx].name.startsWith(token);} );
            return matches.length ? matches : allSuggestions;
        };

        const renderSuggestion = (idx)=>{
            const colVal = colValStats[idx];
            return colVal.name + (colVal.unit && colVal.unit !== 'null' ? ', '+colVal.unit : ' ');
        };

        const valueOnSuggestion = (prevVal, idx)=>{
            const {priorContent} = parseSuggestboxContent(prevVal);
            return priorContent+colValStats[idx].name;
        };

        var x = get(xyPlotParams, 'x.columnOrExpr');
        var y = get(xyPlotParams, 'y.columnOrExpr');
        const onXColSelected = (colName) => {
            x = colName;
            dispatchValueChange({fieldKey: 'x.columnOrExpr', groupKey, value: colName, valid: true});
        };
        const onYColSelected = (colName) => {
            y = colName;
            dispatchValueChange({fieldKey: 'y.columnOrExpr', groupKey, value: colName, valid: true});
        };

        const noLogOption = possibleDecimatedTable(colValStats);

        return (
            <div style={{padding:'0 5px 7px'}}>
                <FieldGroup groupKey={groupKey} validatorFunc={null} keepState={true}
                    reducerFunc={fldChangeReducer}>
                    {onOptionsSelected &&
                    <div style={{display: 'flex', flexDirection: 'row', padding: '5px 0 15px'}}>
                        <CompleteButton style={{flexGrow: 0}}
                                        groupKey={groupKey}
                                        onSuccess={(flds) => resultsSuccess(onOptionsSelected, flds)}
                                        onFail={resultsFail}
                                        text = 'Apply'
                        />
                        <div style={{flexGrow: 1}}/>
                        <div style={{flexGrow: 0}}>
                            <button type='button' className='button std' onClick={() => setOptions(groupKey, {})}>Clear</button>
                            <button type='button' className='button std' onClick={() => setOptions(groupKey, xyPlotParams)}>Reset</button>
                        </div>
                    </div>}

                    <div style={helpStyle}>
                        For X and Y, enter a column or an expression<br/>
                        ex. log(col); 100*col1/col2; col1-col2
                    </div>
                    <div style={{whiteSpace: 'nowrap'}}>
                        <SuggestBoxInputField
                            inline={true}
                            initialState= {{
                                            value: get(xyPlotParams, 'x.columnOrExpr'),
                                            tooltip: 'Column or expression for X axis',
                                            label: 'X:',
                                            nullAllowed : false
                                        }}
                            getSuggestions={getSuggestions}
                            renderSuggestion={renderSuggestion}
                            valueOnSuggestion={valueOnSuggestion}
                            fieldKey='x.columnOrExpr'
                            groupKey={groupKey}
                            labelWidth={20}
                        />
                        <TextButton style={{display: 'inline-block', paddingLeft: 3, verticalAlign: 'bottom'}}
                                    groupKey={groupKey}
                                    text='Cols'
                                    tip='Select X column'
                                    onClick={() => showColSelectPopup(colValStats, onXColSelected,'Choose X','Set X',x)}
                        />
                    </div>

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
                            options={noLogOption ? AXIS_OPTIONS_NOLOG : AXIS_OPTIONS}
                            fieldKey='x.options'
                            groupKey={groupKey}
                            labelWidth={50}
                        />
                    </FieldGroupCollapsible>
                    <br/>

                    <div style={{whiteSpace: 'nowrap'}}>
                        <SuggestBoxInputField
                            inline={true}
                            initialState= {{
                                            value: get(xyPlotParams, 'y.columnOrExpr'),
                                            tooltip: 'Column or expression for Y axis',
                                            label: 'Y:',
                                            nullAllowed : false
                                        }}
                            getSuggestions={getSuggestions}
                            renderSuggestion={renderSuggestion}
                            valueOnSuggestion={valueOnSuggestion}
                            fieldKey='y.columnOrExpr'
                            groupKey={groupKey}
                            labelWidth={20}
                        />

                        <TextButton style={{display: 'inline-block', paddingLeft: 3, verticalAlign: 'bottom'}}
                                    groupKey={groupKey}
                                    text='Cols'
                                    tip='Select Y column'
                                    onClick={() => showColSelectPopup(colValStats,onYColSelected,'Choose Y','Set Y',y)}
                        />
                    </div>

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
                            options={noLogOption ? AXIS_OPTIONS_NOLOG : AXIS_OPTIONS}
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
    xyPlotParams: plotParamsShape
};