/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {PropTypes} from 'react';

import {get, isUndefined, omitBy} from 'lodash';

import ColValuesStatistics from './ColValuesStatistics.js';
import CompleteButton from '../ui/CompleteButton.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import Validate from '../util/Validate.js';
import {Expression} from '../util/expr/Expression.js';
import {ValidationField} from '../ui/ValidationField.jsx';
import {CheckboxGroupInputField} from '../ui/CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from '../ui/RadioGroupInputField.jsx';
import {SuggestBoxInputField} from '../ui/SuggestBoxInputField.jsx';
import {FieldGroupCollapsible} from '../ui/panel/CollapsiblePanel.jsx';
import {plotParamsShape} from  './XYPlot.jsx';

const DECI_ENABLE_SIZE = 5000;

const helpStyle = {fontStyle: 'italic', color: '#808080', paddingBottom: 10};

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


var XYPlotOptions = React.createClass({


    propTypes: {
        groupKey: PropTypes.string.isRequired,
        colValStats: PropTypes.arrayOf(PropTypes.instanceOf(ColValuesStatistics)).isRequired,
        onOptionsSelected: PropTypes.func.isRequired,
        xyPlotParams: plotParamsShape
    },


    shouldComponentUpdate(np) {
        return this.props.groupKey !== np.groupKey || this.props.colValStats !== np.colValStats ||
            this.props.xyPlotParams !== np.xyPlotParams;
    },


    getUnit(colname) {
        const statrow = this.props.colValStats.find((el) => { return el.name===colname; });
        if (statrow && statrow.unit && statrow.unit !== 'null') { return statrow.unit; }
        else {return '';}
    },

    resultsSuccess(flds) {
        const xName = get(flds, ['x.columnOrExpr']);
        const yName = get(flds, ['y.columnOrExpr']);

        const xOptions = get(flds, ['x.options']);
        let xLabel = get(flds, ['x.label']), xUnit = get(flds, ['x.unit']);
        if (!xLabel) { xLabel = xName; }
        if (!xUnit) {xUnit = this.getUnit(xName); }

        const yOptions = get(flds, ['y.options']);
        let yLabel = get(flds, ['y.label']);
        let yUnit = get(flds, ['y.unit']);
        if (!yLabel) { yLabel = yName; }
        if (!yUnit) {yUnit = this.getUnit(yName); }

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

        this.props.onOptionsSelected(xyPlotParams);
    },

    resultsFail() {
        // TODO: do I need to do anything here?
    },

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
    },

    render() {
        const { colValStats, groupKey, xyPlotParams }= this.props;
        const colNames = colValStats.map((colVal) => {return colVal.name;});

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

        const colValidator = (val) => {
            let retval = {valid: true, message: ''};
            if (!val) {
                return {valid: false, message: 'Can not be empty. Please provide value or expression'};
            } else if (colNames.indexOf(val)<0) {
                const expr = new Expression(val,colNames);
                if (!expr.isValid()) {
                    retval = {valid: false, message: `${expr.getError().error}. Unable to parse ${val}.`};
                }
            }
            return retval;
        };

        return (
            <div style={{padding:'5px'}}>
                <br/>
                <FieldGroup groupKey={groupKey} validatorFunc={null} keepState={true}>
                    <div style={helpStyle}>
                        For X and Y, enter a column or an expression<br/>
                        ex. log(col); 100*col1/col2; col1-col2
                    </div>
                    <SuggestBoxInputField
                        initialState= {{
                            value: get(xyPlotParams, 'x.columnOrExpr'),
                            tooltip: 'Column or expression for X axis',
                            label: 'X:',
                            nullAllowed : false,
                            validator: colValidator
                        }}
                        getSuggestions={getSuggestions}
                        renderSuggestion={renderSuggestion}
                        valueOnSuggestion={valueOnSuggestion}
                        fieldKey='x.columnOrExpr'
                        groupKey={groupKey}
                        labelWidth={20}
                    />
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

                    <SuggestBoxInputField
                        initialState= {{
                            value: get(xyPlotParams, 'y.columnOrExpr'),
                            tooltip: 'Column or expression for Y axis',
                            label : 'Y:',
                            nullAllowed : false,
                            validator: colValidator
                        }}
                        getSuggestions={getSuggestions}
                        renderSuggestion={renderSuggestion}
                        valueOnSuggestion={valueOnSuggestion}
                        fieldKey='y.columnOrExpr'
                        groupKey={groupKey}
                        labelWidth={20}
                    />
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
                            value: get(xyPlotParams, 'stretch'),
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

                    <CompleteButton groupKey={groupKey}
                                    onSuccess={this.resultsSuccess}
                                    onFail={this.resultsFail}
                        />
                </FieldGroup>

            </div>
        );
    }
});

export default XYPlotOptions;