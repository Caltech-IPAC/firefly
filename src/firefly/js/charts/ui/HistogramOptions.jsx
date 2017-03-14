import React, {PropTypes} from 'react';

import {get} from 'lodash';
import ColValuesStatistics from './../ColValuesStatistics.js';
import {DATATYPE_HISTOGRAM} from '../dataTypes/HistogramCDT.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {dispatchValueChange, dispatchMultiValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {InputGroup} from '../../ui/InputGroup.jsx';
import Validate from '../../util/Validate.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {FieldGroupCollapsible} from '../../ui/panel/CollapsiblePanel.jsx';
import {ColumnOrExpression, getColValidator} from './ColumnOrExpression.jsx';
import {getAppOptions} from '../../core/AppDataCntlr.js';


export const histogramParamsShape = PropTypes.shape({
         algorithm : PropTypes.oneOf(['fixedSizeBins','bayesianBlocks']),
         numBins : PropTypes.string,
         binWidth : PropTypes.string,
         fixedBinSizeSelection : PropTypes.oneOf(['numBins','binWidth']),
         falsePositiveRate : PropTypes.string,
         minCutoff : PropTypes.string,
         maxCutoff : PropTypes.string
      });


export function resultsSuccess(callback, flds, tblId) {
    const options = Object.assign({}, flds, {tblId, type: DATATYPE_HISTOGRAM.id});
    callback(options);
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
        {fieldKey: 'fixedBinSizeSelection', value:'numBins'},
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

export class HistogramOptions extends React.Component {

        constructor(props) {
            super(props);
            this.state = {
                fields : FieldGroupUtils.getGroupFields(this.props.groupKey),
                fixedAlgorithm: get(getAppOptions(), 'charts.ui.HistogramOptions.fixedAlgorithm') || props.fixedAlgorithm
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
                if (this.iAmMounted && fields !== this.state.fields) {
                    this.setState({fields});
                }
            });
        // make sure column validator matches current columns
        const {colValStats, groupKey, histogramParams} = this.props;
        if (colValStats) {
            const colValidator = getColValidator(colValStats);
            var payload = {groupKey, fieldKey: 'columnOrExpr', validator: colValidator};
            const value = get(histogramParams, 'columnOrExpr');
            if (value) {
                var {valid, message} = colValidator(value);
                payload = Object.assign(payload, {value, valid, message});
            }
            dispatchValueChange(payload);
        }
        this.iAmMounted= true;
    }


    renderAlgorithmParameters() {
        const {groupKey, histogramParams} = this.props;
        const {fields} = this.state;

        var algorithm =  FieldGroupUtils.getFldValue(fields, 'algorithm', 'fixedSizeBins');

        if (algorithm === 'bayesianBlocks') {
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
               <div >
                   {renderFixedBinSizeOptions(groupKey, histogramParams)}

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
        const { colValStats, groupKey, histogramParams, defaultParams, onOptionsSelected} = this.props;
        const {fixedAlgorithm=false} = this.state;
        const xProps = {colValStats,params:histogramParams,groupKey,fldPath:'columnOrExpr',label:'Column or expression', labelWidth:120, tooltip:'X Axis',nullAllowed:false};

        const algorithm = get(histogramParams, 'algorithm', 'fixedSizeBins');
        const m_algorithmOptions = fixedAlgorithm ? algorithmOptions.filter((el) => el.value === algorithm) : algorithmOptions;
        // set minimum height to fit full height suggest box,
        // to avoid width change due to scroll bar appearing when full height suggest box is rendered
        return (
            <div style={{padding:'0 5px', minHeight: 250}}>
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
                            <button type='button' className='button std' onClick={() => setOptions(groupKey, defaultParams)}>Reset</button>
                        </div>
                    </div>}
                    <ColumnOrExpression {...xProps}/>

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
                                    {label: 'reverse', value: 'flip'},
                                    {label: 'top', value: 'opposite'},
                                    {label: 'log', value: 'log'}
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
                    </FieldGroupCollapsible>
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
                </FieldGroup>

            </div>
        );
    }
}

//Make the fixed bin layout
function renderFixedBinSizeOptions(groupKey, histogramParams){
    return (
      <div style={{display: 'flex', flexDirection: 'row', padding: '5px 0 15px'}} >
         <RadioGroupInputField
            initialState= {{
                                value: get(histogramParams, 'fixedBinSizeSelection', 'numBins'),
                                tooltip: 'Please select number of bins or bin width',
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
                                  validator:Validate.intRange.bind(null, 1, 500, 'numBins'),
                                  tooltip: 'Number of bins'

                             }}
                 fieldKey='numBins'
                 groupKey={groupKey}
                 labelWidth={80}
             />
             <ValidationField
                 style={{width: 80}}
                 initialState= {{
                                  value: get(histogramParams, 'binWidth', ''),
                                  validator:Validate.isFloat.bind(null,  'binWidth'),
                                  tooltip: 'Bin width'

                             }}
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
    colValStats: PropTypes.arrayOf(PropTypes.instanceOf(ColValuesStatistics)).isRequired,
    onOptionsSelected: PropTypes.func,
    histogramParams: histogramParamsShape,
    defaultParams: histogramParamsShape,
    fixedAlgorithm: PropTypes.bool
};