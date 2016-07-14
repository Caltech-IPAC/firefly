import React, {PropTypes} from 'react';

import {get} from 'lodash';
import ColValuesStatistics from './../ColValuesStatistics.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {dispatchMultiValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {InputGroup} from '../../ui/InputGroup.jsx';
import Validate from '../../util/Validate.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {FieldGroupCollapsible} from '../../ui/panel/CollapsiblePanel.jsx';

export const histogramParamsShape = PropTypes.shape({
         algorithm : PropTypes.oneOf(['fixedSizeBins','byesianBlocks']),
         numBins : PropTypes.string,
         falsePositiveRate : PropTypes.string,
         minCutoff : PropTypes.number,
         maxCutoff : PropTypes.number
      });


export function resultsSuccess(callback, histogramParams) {
    callback(Object.assign({}, histogramParams));
}

export function resultsFail() {
    // TODO: do I need to do anything here?
}

export function setOptions(groupKey, histogramParams) {
    const flds = [
        {fieldKey: 'columnOrExpr', value: get(histogramParams, 'columnOrExpr')},
        {fieldKey: 'x', value: get(histogramParams, 'x', '_none_')},
        {fieldKey: 'y', value: get(histogramParams, 'y', '_none_')},
        {fieldKey: 'algorithm', value: get(histogramParams, 'algorithm', 'fixedSizeBins')},
        {fieldKey: 'falsePositiveRate', value: get(histogramParams, 'falsePositiveRate','0.05')},
        {fieldKey: 'numBins', value: get(histogramParams, 'numBins','50')}
    ];
    dispatchMultiValueChange(groupKey, flds);
}

export class HistogramOptions extends React.Component {

        constructor(props) {
            super(props);
            this.state = {
                fields : FieldGroupUtils.getGroupFields(this.props.groupKey)
            };
        }


    shouldComponentUpdate(np, ns) {
        return this.props.groupKey !== np.groupKey || this.props.colValStats !== np.colValStats ||
            this.props.histogramParams !== np.histogramParams ||
            FieldGroupUtils.getFldValue(this.state.fields, 'algorithm') !== FieldGroupUtils.getFldValue(ns.fields, 'algorithm');
    }

    componentWillReceiveProps(np) {
        if (this.props.histogramParams !== np.histogramParams) {
            setOptions(np.groupKey, np.histogramParams);
        }
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
    }

    componentDidMount() {
        this.unbinder = FieldGroupUtils.bindToStore(this.props.groupKey,
            (fields) => {
                if (this.iAmMounted && fields != this.state.fields) {
                    this.setState({fields});
                }
            });
        this.iAmMounted= true;
    }

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
                        value: get(histogramParams, 'falsePositiveRate','0.05'),
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
                        value: get(histogramParams, 'numBins', '50'),
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
    }

    render() {
        const { colValStats, groupKey, histogramParams, onOptionsSelected}= this.props;
        return (
            <div style={{padding:'7px 5px'}}>
                <FieldGroup groupKey={groupKey} validatorFunc={null} keepState={true}>
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
                            <button type='button' className='button std' onClick={() => setOptions(groupKey, histogramParams)}>Reset</button>
                        </div>
                    </div>}

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
                                    value: get(histogramParams, 'x', '_none_'),
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
                                    value: get(histogramParams, 'y', '_none_'),
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
                                value: get(histogramParams, 'algorithm', 'fixedSizeBins'),
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
                    <br/>
                </FieldGroup>

            </div>
        );
    }
}

HistogramOptions.propTypes = {
    groupKey: PropTypes.string.isRequired,
    colValStats: PropTypes.arrayOf(PropTypes.instanceOf(ColValuesStatistics)).isRequired,
    onOptionsSelected: PropTypes.func,
    histogramParams: histogramParamsShape
};