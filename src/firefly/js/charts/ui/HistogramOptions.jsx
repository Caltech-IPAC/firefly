import React, {Component} from 'react';
import PropTypes from 'prop-types';

import {get, isEmpty} from 'lodash';
import ColValuesStatistics from './../ColValuesStatistics.js';
import {FieldGroup,} from '../../ui/FieldGroup.jsx';
import FieldGroupUtils,{revalidateFields} from '../../fieldGroup/FieldGroupUtils.js';
import {dispatchValueChange, dispatchMultiValueChange, VALUE_CHANGE} from '../../fieldGroup/FieldGroupCntlr.js';
import {InputGroup} from '../../ui/InputGroup.jsx';
import Validate from '../../util/Validate.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {FieldGroupCollapsible} from '../../ui/panel/CollapsiblePanel.jsx';
import {ColumnOrExpression, getColValidator} from './ColumnOrExpression.jsx';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {updateSet} from '../../util/WebUtil.js';

export const histogramParamsShape = PropTypes.shape({
         algorithm : PropTypes.oneOf(['fixedSizeBins','bayesianBlocks']),
         numBins : PropTypes.oneOfType([PropTypes.string,PropTypes.number]),
         binWidth :PropTypes.oneOfType([PropTypes.string,PropTypes.number]),
         fixedBinSizeSelection : PropTypes.oneOf(['numBins','binWidth']),
         falsePositiveRate : PropTypes.oneOfType([PropTypes.string,PropTypes.number]),
         minCutoff : PropTypes.oneOfType([PropTypes.string,PropTypes.number]),
         maxCutoff :PropTypes.oneOfType([PropTypes.string,PropTypes.number])
      });


export function setOptions(groupKey, histogramParams) {
    const flds = [
        {fieldKey: 'columnOrExpr', value: get(histogramParams, 'columnOrExpr')},
        {fieldKey: 'x', value: get(histogramParams, 'x', '_none_')},
        {fieldKey: 'y', value: get(histogramParams, 'y', '_none_')},
        {fieldKey: 'algorithm', value: get(histogramParams, 'algorithm', 'fixedSizeBins')},
        {fieldKey: 'falsePositiveRate', value: get(histogramParams, 'falsePositiveRate','0.05')},
        {fieldKey: 'fixedBinSizeSelection', value:get(histogramParams, 'fixedBinSizeSelection', 'numBins')},
        {fieldKey: 'numBins', value: get(histogramParams, 'numBins','50')},
        {fieldKey: 'binWidth', value: get(histogramParams, 'binWidth','')},
        {fieldKey: 'minCutoff', value: get(histogramParams, 'minCutoff','')},
        {fieldKey: 'maxCutoff', value: get(histogramParams, 'maxCutoff','')}
    ];
    dispatchMultiValueChange(groupKey, flds);
}

const algorithmOptions = [  {label: 'Bayesian blocks', value: 'bayesianBlocks'},
                            {label: 'Uniform binning', value: 'fixedSizeBins'} ];
const binSizeOptions = [  {label: 'Number of bins:', value: 'numBins'},
    {label: 'Bin width:', value: 'binWidth'} ];


function isSingleColumn(colName, colValStats) {
    for (let i = 0; i < colValStats.length; i++) {
        if (colName === colValStats[i].name) {
            return true;
        }

    }
    return false;
}

function columnNameReducer(colValStats, basicFieldsReducer) {
    return (inFields, action) => {

        if (!inFields) {
            return basicFieldsReducer ? basicFieldsReducer(null) : {};
        }
        let fieldKey = undefined;
        if (!isEmpty(colValStats) && action.type === VALUE_CHANGE) {
            fieldKey = get(action.payload, 'fieldKey');
            switch (fieldKey) {
                // when column name changes, update the min/max input and clear x title
                case 'columnOrExpr': {
                    const numBins = get(inFields, ['numBins', 'value'], 50);
                    const colName = action.payload.value;
                    if (colName && !get(action.payload, 'validator')) {
                        // colName change is triggered by user input
                        if (inFields['layout.xaxis.title.text']) {
                            inFields = updateSet(inFields, ['layout.xaxis.title.text', 'value'], undefined);
                        }
                        const col = colName.replace(/^"(.+)"$/, '$1'); // remove quotes if any
                        if (isSingleColumn(col, colValStats)) {
                            for (let i = 0; i < colValStats.length; i++) {
                                if (col === colValStats[i].name) {
                                    const dataMin = colValStats[i].min;
                                    const dataMax = colValStats[i].max;
                                    const binWidth = ((dataMax - dataMin) / numBins).toFixed(6);
                                    inFields = updateSet(inFields, ['minCutoff', 'value'], `${dataMin}`);
                                    inFields = updateSet(inFields, ['maxCutoff', 'value'], `${dataMax}`);
                                    inFields = updateSet(inFields, ['binWidth', 'value'], `${binWidth}`);
                                    break;
                                }
                            }
                        }
                        else {
                            inFields = updateSet(inFields, ['minCutoff', 'value'], undefined);
                            inFields = updateSet(inFields, ['maxCutoff', 'value'], undefined);
                            inFields = updateSet(inFields, ['binWidth', 'value'], undefined);
                        }
                        if (inFields['__xreset']) {
                            // reset axes range
                            inFields = updateSet(inFields, ['__xreset', 'value'], 'true');
                            inFields = updateSet(inFields, ['__yreset', 'value'], 'true');
                        }
                    }

                }
                    break;
                //When numBins changes, update binWidth
                case 'numBins':  {
                    const numBins = get(inFields, ['numBins', 'value'], 50);
                    const min = get(inFields, ['minCutoff', 'value'], '');
                    const max = get(inFields, ['maxCutoff', 'value'], '');
                    const binWidth = (max - min) / numBins;
                    inFields = updateSet(inFields, ['binWidth', 'value'], `${binWidth}`);
                }
                    break;
                //when binWidth changes, update the numBins
                case 'binWidth': {
                    const binWidth = get(inFields, ['binWidth', 'value'], '');
                    const min = get(inFields, ['minCutoff', 'value'], '');
                    const max = get(inFields, ['maxCutoff', 'value'], '');
                    const numBins =Math.ceil((max - min) / binWidth);
                    inFields = updateSet(inFields, ['numBins', 'value'], `${numBins}`);
                }
                    break;
                //when minCutOff or maxCutOff change, update numBins
                case 'minCutoff':
                case 'maxCutoff':   {

                    const fixedBinSizeSelection = get(inFields, ['fixedBinSizeSelection', 'value']);
                    let numBins, min, max, binWidth;
                    if (fixedBinSizeSelection==='numBins'){
                        numBins = get(inFields, ['numBins', 'value'], 50);
                        min = get(inFields, ['minCutoff', 'value'], '');
                        max = get(inFields, ['maxCutoff', 'value'], '');
                        binWidth = (max - min) / numBins;
                        inFields = updateSet(inFields, ['binWidth', 'value'], `${binWidth}`);

                    }
                    else {
                        binWidth = get(inFields, ['binWidth', 'value'], '');
                        numBins = get(inFields, ['numBins', 'value'], 50);

                        min = get(inFields, ['minCutoff', 'value'], '');
                        max = get(inFields, ['maxCutoff', 'value'], '');

                        numBins = Math.ceil((max - min) / binWidth);
                        inFields = updateSet(inFields, ['numBins', 'value'], `${numBins}`);
                    }
                    break;
                }
                default:
                    break;
            }

            return revalidateFields(Object.assign({}, inFields));
        }
        else {
            return inFields;
        }

    };
}

export class HistogramOptions extends Component {

    constructor(props) {
        super(props);
        this.state = {
            fields : FieldGroupUtils.getGroupFields(this.props.groupKey),
            fixedAlgorithm: get(getAppOptions(), 'charts.ui.HistogramOptions.fixedAlgorithm') || props.fixedAlgorithm
        };
    }


    shouldComponentUpdate(np, ns) {

        return this.props.activeTrace !== np.activeTrace || this.props.groupKey !== np.groupKey ||
            this.props.colValStats !== np.colValStats || this.props.histogramParams !== np.histogramParams ||
            FieldGroupUtils.getFldValue(this.state.fields, 'algorithm') !== FieldGroupUtils.getFldValue(ns.fields, 'algorithm') ||
            FieldGroupUtils.getFldValue(this.state.fields, 'fixedBinSizeSelection') !== FieldGroupUtils.getFldValue(ns.fields, 'fixedBinSizeSelection');



    }

    UNSAFE_componentWillReceiveProps(np) {
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
                if (this.iAmMounted && fields !== this.state.fields) {
                    this.setState({fields});
                }
            });
        // make sure column validator matches current columns
        const {colValStats, groupKey, histogramParams} = this.props;
        if (colValStats) {
            const colValidator = getColValidator(colValStats);
            let payload = {groupKey, fieldKey: 'columnOrExpr', validator: colValidator};
            const value = get(histogramParams, 'columnOrExpr');
            if (value) {
                const {valid, message} = colValidator(value);
                payload = Object.assign(payload, {value, valid, message});
            }
            dispatchValueChange(payload);
            if (histogramParams) {
                setOptions(groupKey, histogramParams);
            }
        }


        this.iAmMounted= true;
    }


    renderAlgorithmParameters() {
        const {groupKey, histogramParams} = this.props;
        const {fields} = this.state;

        const algorithm =  FieldGroupUtils.getFldValue(fields, 'algorithm', 'fixedSizeBins');

        if (algorithm === 'bayesianBlocks') {
            // if label is in initialState, it does not show when first time displayed
            return (
                <ValidationField
                    style={{width: 30}}
                    initialState= {{
                        value: get(histogramParams, 'falsePositiveRate','0.05'),
                        validator: Validate.floatRange.bind(null, 0.01, 0.5, 2,'falsePositiveRate'),
                        tooltip: 'Acceptable false positive rate'
                    }}
                    fieldKey='falsePositiveRate'
                    groupKey={groupKey}
                    labelWidth={100}
                    label='False Positive Rate:'
                />
            );
        } else { // fixedSizeBins


         const disabled = FieldGroupUtils.getFldValue(this.state.fields, 'fixedBinSizeSelection') ?
             FieldGroupUtils.getFldValue(this.state.fields, 'fixedBinSizeSelection')!=='numBins':false;

         return (
               <div key='fixedsize'>
                   {renderFixedBinSizeOptions(groupKey, histogramParams, disabled) }

                   <ValidationField
                       style={{width: 156}}
                       initialState= {{
                                  value: get(histogramParams, 'minCutoff', ''),
                                  validator:Validate.isFloat.bind(null,  'minCutoff'),
                                  tooltip: 'Minimal value',
                                  label : 'Min:'

                             }}
                       fieldKey='minCutoff'
                       groupKey={groupKey}
                       labelWidth={30}
                   />
                   <ValidationField
                       style={{width: 156}}
                       initialState= {{
                                  value: get(histogramParams, 'maxCutoff', ''),
                                  validator:Validate.isFloat.bind(null,  'maxCutoff'),
                                  tooltip: 'Max value',
                                  label : 'Max:'

                             }}
                       fieldKey='maxCutoff'
                       groupKey={groupKey}
                       labelWidth={30}
                   />
                 </div>

            );
        }
    }

    render() {
        const { colValStats, groupKey, histogramParams, basicFieldsReducer, basicFields} = this.props;
        const {fixedAlgorithm=false} = this.state;
        const xProps = {colValStats,params:histogramParams,groupKey,fldPath:'columnOrExpr',label:'Column or expression:',labelWidth:120,name: 'X',tooltip:'X Axis',nullAllowed:false};

        const algorithm = get(histogramParams, 'algorithm', 'fixedSizeBins');
        const m_algorithmOptions = fixedAlgorithm ? algorithmOptions.filter((el) => el.value === algorithm) : algorithmOptions;
        // set minimum height to fit full height suggest box,
        // to avoid width change due to scroll bar appearing when full height suggest box is rendered
        return (
            <div>
                <FieldGroup groupKey={groupKey} keepState={false}
                            reducerFunc={columnNameReducer(colValStats,basicFieldsReducer)}>

                    <ColumnOrExpression {...xProps}/>

                    {!basicFields && <FieldGroupCollapsible  header='Options'
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
                                    {label: 'reverse', value: 'flip'},
                                    {label: 'top', value: 'opposite'},
                                    /*{label: 'log', value: 'log'}*/
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
                                    {label: 'reverse', value: 'flip'},
                                    {label: 'right', value: 'opposite'},
                                    {label: 'log', value: 'log'}
                                ]}
                                fieldKey='y'
                            />
                        </InputGroup>
                    </FieldGroupCollapsible>}
                    <br/>
                    <InputGroup labelWidth={60}>
                        <RadioGroupInputField
                            initialState= {{
                                value: algorithm,
                                tooltip: 'Please select an algorithm',
                                label: 'Algorithm:'
                            }}
                            options={m_algorithmOptions}
                            alignment='horizontal'
                            fieldKey='algorithm'
                            groupKey={groupKey}/>
                    </InputGroup>
                    <br/>
                    {this.renderAlgorithmParameters()}
                    <br/>
                    {basicFields && <br/>}
                    {basicFields}
                </FieldGroup>
            </div>
        );
    }
}

//Make the fixed bin layout
function renderFixedBinSizeOptions(groupKey, histogramParams, disabled){
    return (
      <div style={{display: 'flex', flexDirection: 'row', padding: '5px 0 15px'}} >
         <RadioGroupInputField
            initialState= {{
                                value: get(histogramParams, 'fixedBinSizeSelection', 'numBins'),
                                tooltip: 'Please select number of bins or bin width'
                                //label: 'BinSize:'
                            }}
            options={binSizeOptions}
            alignment='vertical'
            fieldKey='fixedBinSizeSelection'
            groupKey={groupKey}/>
        <div>
             <ValidationField
                 style={{width: 80}}
                 initialState= {{
                                  value: get(histogramParams, 'numBins', '50'),
                                  validator:Validate.intRange.bind('number of bins ', 1, 500, 'numBins', false),
                                  tooltip: 'Number of bins'

                             }}
                 disabled = {disabled}
                 fieldKey='numBins'
                 groupKey={groupKey}
                 labelWidth={80}
             />
             <ValidationField
                 style={{width: 80}}
                 initialState= {{
                                  value: get(histogramParams, 'binWidth', ''),
                                  validator:Validate.isFloat.bind('bin width',  'binWidth'),
                                  tooltip: 'Bin width'

                             }}
                 disabled = {!disabled}
                 fieldKey='binWidth'
                 groupKey={groupKey}
                 labelWidth={80}
            />
        </div>
      </div>


   );
}

HistogramOptions.propTypes = {
    groupKey: PropTypes.string.isRequired,
    activeTrace: PropTypes.number,
    colValStats: PropTypes.arrayOf(PropTypes.instanceOf(ColValuesStatistics)).isRequired,
    histogramParams: histogramParamsShape,
    fixedAlgorithm: PropTypes.bool,
    basicFieldsReducer: PropTypes.func,
    basicFields: PropTypes.element
};
