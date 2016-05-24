import React, {PropTypes} from 'react';

import {get} from 'lodash';
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

export const histogramParamsShape = PropTypes.shape({
         algorithm : PropTypes.oneOf(['fixedSizeBins','byesianBlocks']),
         numBins : PropTypes.string,
         falsePositiveRate : PropTypes.string,
         minCutoff : PropTypes.number,
         maxCutoff : PropTypes.number
      });

var HistogramOptions = React.createClass({

    unbinder : null,

    propTypes: {
        groupKey: PropTypes.string.isRequired,
        colValStats: PropTypes.arrayOf(React.PropTypes.instanceOf(ColValuesStatistics)).isRequired,
        onOptionsSelected: PropTypes.func.isRequired,
        histogramParams: histogramParamsShape
    },

    shouldComponentUpdate(np, ns) {
        return this.props.groupKey !== np.groupKey || this.props.colValStats !== np.colValStats ||
            this.props.histogramParams !== np.histogramParams ||
            FieldGroupUtils.getFldValue(this.state.fields, 'algorithm') !== FieldGroupUtils.getFldValue(ns.fields, 'algorithm');
    },

    getInitialState() {
        return {fields : FieldGroupUtils.getGroupFields(this.props.groupKey)};
    },

    componentWillUnmount() {
        if (this.unbinder) this.unbinder();
    },

    componentDidMount() {
        this.unbinder = FieldGroupUtils.bindToStore(this.props.groupKey,
            (fields) => {
                if (fields != this.state.fields) {
                    this.setState({fields});
                }
            });
    },

    resultsSuccess(histogramParams) {
        this.props.onOptionsSelected(histogramParams);
    },

    resultsFail() {
        // TODO: do I need to do anything here?
    },

    renderAlgorithmParameters() {
        const {groupKey, histogramParams} = this.props;
        const {fields} = this.state;

        var algorithm =  FieldGroupUtils.getFldValue(fields, 'algorithm', 'fixedSizeBins');

        if (algorithm === 'byesianBlocks') {
            return (
                <div>
                <ValidationField
                    style={{width: 30}}
                    initialState= {{
                        value: get(histogramParams, 'falsePositiveRate')||'0.05',
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
                        value: get(histogramParams, 'numBins')||'50',
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
        const { colValStats, groupKey, histogramParams}= this.props;
        return (
            <div style={{padding:'5px'}}>
                <br/>
                <FieldGroup groupKey={groupKey} validatorFunc={null} keepState={true}>
                    <ListBoxInputField
                        initialState= {{
                            value: get(histogramParams, 'columnOrExpr'),
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
                                    value: get(histogramParams, 'x')||'_none_',
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
                                    value: get(histogramParams, 'y')||'_none_',
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
                                value: get(histogramParams, 'algorithm')||'fixedSizeBins',
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