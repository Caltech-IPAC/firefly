/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {PropTypes} from 'react';

import {get} from 'lodash';

import ColValuesStatistics from './ColValuesStatistics.js';
import CompleteButton from '../ui/CompleteButton.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
//import InputGroup from '../ui/InputGroup.jsx';
import Validate from '../util/Validate.js';
import {ValidationField} from '../ui/ValidationField.jsx';
import {CheckboxGroupInputField} from '../ui/CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from '../ui/RadioGroupInputField.jsx';
import {ListBoxInputField} from '../ui/ListBoxInputField.jsx';
import CollapsiblePanel from '../ui/panel/CollapsiblePanel.jsx';



var XYPlotOptions = React.createClass({

    unbinder : null,

    propTypes: {
        groupKey: PropTypes.string.isRequired,
        colValStats: PropTypes.arrayOf(PropTypes.instanceOf(ColValuesStatistics)).isRequired,
        onOptionsSelected: PropTypes.func.isRequired
    },

    getInitialState() {
        return {fields : FieldGroupUtils.getGroupFields(this.props.groupKey)};
    },

    shouldComponentUpdate(nextProps, nextState) {
        return Boolean(this.state.fields !== nextState.fields ||
            this.props.colValStats !== nextProps.colValStats);
    },

    componentWillUnmount() {
        if (this.unbinder) this.unbinder();
    },

    componentDidMount() {
        this.unbinder = FieldGroupUtils.bindToStore(this.props.groupKey, (fields) => this.setState({fields}));
    },

    getUnit(colname) {
        const statrow = this.props.colValStats.find((el) => { return el.name===colname; });
        if (statrow && statrow.unit && statrow.unit !== 'null') { return statrow.unit; }
        else {return '';}
    },

    resultsSuccess(flds) {
        const xName = get(flds, ['x.columnOrExpr']);
        const xOptions = get(flds, ['x.options']);
        let xLabel = get(flds, ['x.label']), xUnit = get(flds, ['x.unit']);
        if (!xLabel) { xLabel = xName; }
        if (!xUnit) {xUnit = this.getUnit(xName); }

        const yName = get(flds, ['y.columnOrExpr']);
        const yOptions = get(flds, ['y.options']);
        let yLabel = get(flds, ['y.label']);
        let yUnit = get(flds, ['y.unit']);
        if (!yLabel) { yLabel = yName; }
        if (!yUnit) {yUnit = this.getUnit(yName); }



        /*
          const axisParamsShape = PropTypes.shape({
             columnOrExpr : PropTypes.string,
             label : PropTypes.string,
             unit : PropTypes.string,
             options : PropTypes.string, // ex. 'grid,log,flip'
             nbins : PropTypes.number,
             min : PropTypes.number,
             max : PropTypes.number
          });

          const xyPlotParamsShape = PropTypes.shape({
             xyRatio : PropTypes.number,
             stretch : PropTypes.oneOf(['fit','fill']),
             x : axisParamsShape,
             y : axisParamsShape
          });
          */
        const xyPlotParams = {
            xyRatio : flds.xyRatio ? flds.xyRatio : undefined,
            stretch : flds.stretch,
            x : { columnOrExpr : xName, label : xLabel, unit : xUnit, options : xOptions},
            y : { columnOrExpr : yName, label : yLabel, unit : yUnit, options : yOptions}
        };

        this.props.onOptionsSelected(xyPlotParams);
    },

    resultsFail() {
        // TODO: do I need to do anything here?
    },

    render() {
        const { colValStats, groupKey }= this.props;
        const fields = FieldGroupUtils.getGroupFields(groupKey);
        return (
            <div style={{padding:'5px'}}>
                <br/>
                <FieldGroup groupKey={groupKey} validatorFunc={null} keepState={true}>
                    <ListBoxInputField
                        initialState= {{
                            value: FieldGroupUtils.getFldValue(fields, 'x.columnOrExpr'),
                            tooltip: 'Please select a column or expression for X axis',
                            label: 'X:'
                        }}
                        options={
                            colValStats.map((colVal) => {
                                return {
                                    label: colVal.name + ' ' + (colVal.unit && colVal.unit !== 'null' ? colVal.unit : ''),
                                    value: colVal.name
                                };
                            })
                        }
                        multiple={false}
                        fieldKey='x.columnOrExpr'
                        groupKey={groupKey}
                        labelWidth={20}
                    />
                    <CollapsiblePanel  header='X Label/Unit/Options'>
                        <ValidationField
                            initialState= {{
                                value: FieldGroupUtils.getFldValue(fields, 'x.label'),
                                validator() { return {valid: true,message: ''}; },
                                tooltip: 'X axis label',
                                label : 'Label:'
                            }}
                            fieldKey='x.label'
                            groupKey={groupKey}
                            labelWidth={50}/>
                        <ValidationField
                            initialState= {{
                                value: FieldGroupUtils.getFldValue(fields, 'x.unit'),
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
                                value: FieldGroupUtils.getFldValue(fields, 'x.options','_none_'),
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
                    </CollapsiblePanel>
                    <br/>

                    <ListBoxInputField
                        initialState= {{
                            tooltip: 'Please select a column or expression for Y axis',
                            label : 'Y:',
                            value: FieldGroupUtils.getFldValue(fields, 'y.columnOrExpr')
                        }}
                        options={
                            colValStats.map((colVal) => {
                                return {
                                    label: colVal.name + ' ' + (colVal.unit && colVal.unit !== 'null' ? colVal.unit : ''),
                                    value: colVal.name
                                };
                            })
                        }
                        multiple={false}
                        fieldKey='y.columnOrExpr'
                        groupKey={groupKey}
                        labelWidth={20}
                    />
                    <CollapsiblePanel  header='Y Label/Unit/Options'>
                        <ValidationField
                            initialState= {{
                                value: FieldGroupUtils.getFldValue(fields, 'y.label'),
                                validator() { return {valid: true,message: ''}; },
                                tooltip: 'Y axis label',
                                label : 'Label:'
                            }}
                            fieldKey='y.label'
                            groupKey={groupKey}
                            labelWidth={50}/>
                        <ValidationField
                            initialState= {{
                                value: FieldGroupUtils.getFldValue(fields, 'y.init'),
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
                                value: FieldGroupUtils.getFldValue(fields, 'y.options', 'grid'),
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
                    </CollapsiblePanel>
                    <ValidationField style={{width:50}}
                        initialState= {{
                            value: FieldGroupUtils.getFldValue(fields, 'xyRatio'),
                            validator: Validate.intRange.bind(null, 1, 10, 'X/Y ratio'),
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
                            value: FieldGroupUtils.getFldValue(fields, 'stretch'),
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

/*
                    <CheckboxGroupInputField
                        initialState= {{
                            value: '_none_',
                            tooltip: 'Check if you would like to plot grid',
                            label : 'Grid:'
                        }}
                        options={[
                            {label: 'grid', value: 'grid'}
                        ]}
                        fieldKey='grid'
                    />
                    <br/>
 */

export default XYPlotOptions;