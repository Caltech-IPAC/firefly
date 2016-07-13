/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {PropTypes} from 'react';

import {get, isUndefined, omitBy, defer} from 'lodash';

import ColValuesStatistics from './../ColValuesStatistics.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {dispatchMultiValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import Validate from '../../util/Validate.js';
import {Expression} from '../../util/expr/Expression.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import {FieldGroupCollapsible} from '../../ui/panel/CollapsiblePanel.jsx';
import {plotParamsShape} from  './XYPlot.jsx';
import {showColSelectPopup} from './ColSelectView.jsx';

const DECI_ENABLE_SIZE = 5000;

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

    /*
      const axisParamsShape = PropTypes.shape({
         columnOrExpr : PropTypes.string,
         label : PropTypes.string,
         unit : PropTypes.string,
         options : PropTypes.string, // ex. 'grid,log,flip'
      });

      const xyPlotParamsShape = PropTypes.shape({
         xyRatio : PropTypes.string,
         stretch : PropTypes.oneOf(['fit','fill']),
         nbins : PropTypes.shape({x : PropTypes.number, y : PropTypes.number}),
         shading : PropTypes.oneOf(['lin', 'log']),
         x : axisParamsShape,
         y : axisParamsShape
      });
      */
    const xyPlotParams = omitBy({
        xyRatio : flds.xyRatio || undefined,
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
        {fieldKey: 'y.options', value: get(xyPlotParams, 'y.options', 'grid')},
        {fieldKey: 'xyRatio', value: get(xyPlotParams, 'xyRatio')},
        {fieldKey: 'stretch', value: get(xyPlotParams, 'stretch', 'fit')},
        {fieldKey: 'nbins.x', value: get(xyPlotParams, 'nbins.x', 100)},
        {fieldKey: 'nbins.y', value: get(xyPlotParams, 'nbins.y', 100)},
        {fieldKey: 'shading', value: get(xyPlotParams, 'shading', 'log')}
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

export class XYPlotOptions extends React.Component {

    constructor(props) {
        super(props);
    }

    componentDidMount() {
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
    }

    shouldComponentUpdate(np) {
        return this.props.groupKey !== np.groupKey || this.props.colValStats !== np.colValStats ||
            this.props.xyPlotParams !== np.xyPlotParams;
    }

    componentWillReceiveProps(np) {
        if (this.props.xyPlotParams !== np.xyPlotParams) {
            defer(setOptions, np.groupKey, np.xyPlotParams);
        }
    }


    renderBinningOptions() {
        const { colValStats, groupKey, xyPlotParams }= this.props;
        const displayBinningOptions = Boolean(colValStats.find((el) => { return el.numpoints>DECI_ENABLE_SIZE; }));
        if (displayBinningOptions) {
            return ( <FieldGroupCollapsible  header='Binning Options'
                                             initialState= {{ value:'closed' }}
                                             fieldKey='binningOptions'>
                <ValidationField style={{width:50}}
                                 initialState= {{
                        value: get(xyPlotParams, 'nbins.x', 100),
                        nullAllowed : false,
                        validator: Validate.intRange.bind(null, 1, 300, 'X-Bins'),
                        tooltip: 'Number of bins along X axis',
                        label : 'X-Bins:'
                    }}
                                 fieldKey='nbins.x'
                                 groupKey={groupKey}
                                 labelWidth={60}/>
                <ValidationField style={{width:50}}
                                 initialState= {{
                        value: get(xyPlotParams, 'nbins.y', 100),
                        nullAllowed : false,
                        validator: Validate.intRange.bind(null, 1, 300, 'Y-Bins'),
                        tooltip: 'Number of bins along Y axis',
                        label : 'Y-Bins:'
                    }}
                                 fieldKey='nbins.y'
                                 groupKey={groupKey}
                                 labelWidth={60}/>
                <br/>
                <RadioGroupInputField
                    alignment='horizontal'
                    initialState= {{
                        value: get(xyPlotParams, 'shading', 'log'),
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
            const flds = [
                {fieldKey: 'x.columnOrExpr', value: colName},
                {fieldKey: 'x.label', value: ''},
                {fieldKey: 'x.unit', value: ''}
            ];
            dispatchMultiValueChange(groupKey, flds);
        };
        const onYColSelected = (colName) => {
            y = colName;
            const flds = [
                {fieldKey: 'y.columnOrExpr', value: colName},
                {fieldKey: 'y.label', value: ''},
                {fieldKey: 'y.unit', value: ''}
            ];
            dispatchMultiValueChange(groupKey, flds);
        };

        return (
            <div style={{padding:'7px 5px'}}>
                <FieldGroup groupKey={groupKey} validatorFunc={null} keepState={true}>
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
                            labelWidth={50}/>
                        <ValidationField
                            initialState= {{
                                value: get(xyPlotParams, 'x.unit'),
                                validator() { return {valid: true,message: ''}; },
                                tooltip: 'X axis unit',
                                label : 'Unit:'
                                }}
                            fieldKey='x.unit'
                            groupKey={groupKey}
                            labelWidth={50}/>

                        <br/>
                        <CheckboxGroupInputField
                            initialState= {{
                                value: get(xyPlotParams, 'x.options', '_none_'),
                                tooltip: 'Check if you would like to plot grid',
                                label : 'Options:'
                            }}
                            options={[
                                {label: 'grid', value: 'grid'},
                                {label: 'flip', value: 'flip'},
                                {label: 'log', value: 'log'}
                            ]}
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
                            labelWidth={50}/>
                        <ValidationField
                            initialState= {{
                                value: get(xyPlotParams, 'y.unit'),
                                validator() { return {valid: true,message: ''}; },
                                tooltip: 'Y axis unit',
                                label : 'Unit:'
                                }}
                            fieldKey='y.unit'
                            groupKey={groupKey}
                            labelWidth={50}/>

                        <br/>
                        <CheckboxGroupInputField
                            initialState= {{
                                value: get(xyPlotParams, 'y.options', 'grid'),
                                tooltip: 'Check if you would like to plot grid',
                                label : 'Options:'

                            }}
                            options={[
                                {label: 'grid', value: 'grid'},
                                {label: 'flip', value: 'flip'},
                                {label: 'log', value: 'log'}
                            ]}
                            fieldKey='y.options'
                            groupKey={groupKey}
                            labelWidth={50}
                        />
                        <br/>
                    </FieldGroupCollapsible>
                    <div style={helpStyle}>
                        Enter display aspect ratio below.<br/>
                        Leave it blank to use all available space.<br/>
                    </div>
                    <ValidationField style={{width:50}}
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
                    <RadioGroupInputField
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
                    />
                    {this.renderBinningOptions()}
                    <br/>

                    {onOptionsSelected &&
                    <div style={{display: 'flex', flexDirection: 'row', padding: '5px 0'}}>
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