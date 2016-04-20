import React from 'react';

import ColValuesStatistics from './ColValuesStatistics.js';
import CompleteButton from '../ui/CompleteButton.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {InputGroup} from '../ui/InputGroup.jsx';
import Validate from '../util/Validate.js';
import {ValidationField} from '../ui/ValidationField.jsx';
import {CheckboxGroupInputField} from '../ui/CheckboxGroupInputField.jsx';
import {ListBoxInputField} from '../ui/ListBoxInputField.jsx';
import {RadioGroupInputField} from '../ui/RadioGroupInputField.jsx';
import {FieldGroupCollapsible} from '../ui/panel/CollapsiblePanel.jsx';


var HistogramOptions = React.createClass({

    unbinder : null,

    propTypes: {
        groupKey: React.PropTypes.string.isRequired,
        colValStats: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ColValuesStatistics)).isRequired,
        onOptionsSelected: React.PropTypes.func.isRequired
    },

    /*
      @param histogramParams key value pairs
      React.PropTypes.shape({
         algorithm : React.PropTypes.oneOf(['fixedSizeBins','byesianBlocks']),
         numBins : React.PropTypes.number,
         falsePositiveRate : React.PropTypes.number,
         minCutoff : React.PropTypes.number,
         maxCutoff : React.PropTypes.number
      })
     */

    getInitialState() {
        return {fields : FieldGroupUtils.getGroupFields(this.props.groupKey)};
    },

    componentWillUnmount() {
        if (this.unbinder) this.unbinder();
    },

    componentDidMount() {
        this.unbinder = FieldGroupUtils.bindToStore(this.props.groupKey, (fields) => this.setState({fields}));
    },

    resultsSuccess(histogramParams) {
        this.props.onOptionsSelected(histogramParams);
    },

    resultsFail() {
        // TODO: do I need to do anything here?
    },

    renderAlgorithmParameters() {
        const {groupKey} = this.props;
        const {fields} = this.state;

        var algorithm =  FieldGroupUtils.getFldValue(fields, 'algorithm', 'fixedSizeBins');

        if (algorithm === 'byesianBlocks') {
            return (
                <div>
                <ValidationField
                    style={{width: 30}}
                    initialState= {{
                        value: FieldGroupUtils.getFldValue(fields, 'falsePositiveRate', 0.05),
                        validator: Validate.floatRange.bind(null, 0.01, 0.5, 2,'falsePositiveRate'),
                        tooltip: 'Acceptable false positive rate',
                        label : 'False Positive Rate:'
                    }}
                    fieldKey='falsePositiveRate'
                    groupKey={groupKey}
                    labelWidth={100}
                />
                </div>
            );
        } else { // fixedSizeBins
            return (
                <ValidationField
                    style={{width: 30}}
                    initialState= {{
                        value: FieldGroupUtils.getFldValue(fields, 'numBins', 10),
                        validator: Validate.intRange.bind(null, 1, 500, 'numBins'),
                        tooltip: 'Number of fixed size bins',
                        label : 'Number of bins:'
                    }}
                    fieldKey='numBins'
                    groupKey={groupKey}
                    labelWidth={80}
                />
            );
        }
    },

    render() {
        const { colValStats, groupKey }= this.props;
        const {fields} = this.state;
        return (
            <div style={{padding:'5px'}}>
                <br/>
                <FieldGroup groupKey={groupKey} validatorFunc={null} keepState={true}>
                    <ListBoxInputField
                        initialState= {{
                            value: FieldGroupUtils.getFldValue(fields, 'columnOrExpr'),
                            tooltip: 'Please select a column',
                            label : 'Column or expression:'
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
                        fieldKey='columnOrExpr'
                        groupKey={groupKey}
                        labelWidth={120}
                    />
                    <FieldGroupCollapsible  header='Options'
                                            initialState= {{ value:'closed' }}
                                            fieldKey='plotoptions'>
                        <InputGroup labelWidth={20}>
                            <CheckboxGroupInputField
                                initialState= {{
                                    value: FieldGroupUtils.getFldValue(fields, 'x', '_none_'),
                                    tooltip: 'X axis options',
                                    label : 'X:'
                                }}
                                options={[
                                    {label: 'log', value: 'log'},
                                    {label: 'flip', value: 'flip'}
                                ]}
                                fieldKey='x'
                            />
                            <CheckboxGroupInputField
                                initialState= {{
                                    value: FieldGroupUtils.getFldValue(fields, 'y', '_none_'),
                                    tooltip: 'Y axis options',
                                    label : 'Y:'
                                }}
                                options={[
                                    {label: 'log', value: 'log'},
                                    {label: 'flip', value: 'flip'}
                                ]}
                                fieldKey='y'
                            />
                        </InputGroup>
                    </FieldGroupCollapsible>
                    <br/>
                    <InputGroup labelWidth={60}>
                        <RadioGroupInputField
                            initialState= {{
                                value : FieldGroupUtils.getFldValue(fields, 'algorithm', 'fixedSizeBins'),
                                tooltip: 'Please select an algorithm',
                                label: 'Algorithm:'
                            }}
                            options={[
                                {label: 'Byesian blocks', value: 'byesianBlocks'},
                                {label: 'Fixed size', value: 'fixedSizeBins'}
                            ]}
                            alignment='horizontal'
                            fieldKey='algorithm'
                            groupKey={groupKey}/>
                    </InputGroup>
                    <br/>
                    {this.renderAlgorithmParameters()}

                    <br/><br/>
                    <CompleteButton groupKey={groupKey}
                                    onSuccess={this.resultsSuccess}
                                    onFail={this.resultsFail}
                    />
                </FieldGroup>

            </div>
        );
    }
});

export default HistogramOptions;