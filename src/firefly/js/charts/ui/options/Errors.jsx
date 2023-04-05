import React from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';

import {getFieldVal} from '../../../fieldGroup/FieldGroupUtils.js';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {ColumnOrExpression} from '../ColumnOrExpression.jsx';
import {getChartProps} from '../../ChartUtil.js';
import {getColValStats} from '../../TableStatsCntlr.js';
import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {CheckboxGroupInputField} from 'firefly/ui/CheckboxGroupInputField.jsx';
import {toBoolean} from '../../../util/WebUtil.js';


export const ERR_TYPE_OPTIONS = [
    {label: 'Symm', value: 'sym'},
    {label: 'Asym', value: 'asym'}

];

export function errorShowFieldKey(activeTrace, axis) { return `fireflyData.${activeTrace}.error_${axis}.visible`; }
export function errorTypeFieldKey(activeTrace, axis) { return `fireflyData.${activeTrace}.error_${axis}.errorsType`; }
export function errorFieldKey(activeTrace, axis) { return `_tables.data.${activeTrace}.error_${axis}.array`; }
export function errorMinusFieldKey(activeTrace, axis) { return `_tables.data.${activeTrace}.error_${axis}.arrayminus`; }

export function getDefaultErrorType(chartData, activeTrace, axis) {
    const errorMinus = get(chartData, errorMinusFieldKey(activeTrace, axis).replace(/^_tables./, ''));
    return errorMinus ? 'asym' : 'sym';
}


export function Error_X({activeTrace, tbl_id, chartId, groupKey, error, errorMinus, labelWidth, readonly}) {
    return <Error {...{tbl_id, chartId, groupKey, axis: 'x', activeTrace, error, errorMinus, readonly}}/>;
}
Error_X.propTypes = {
    activeTrace: PropTypes.number,
    tbl_id: PropTypes.string,
    groupKey: PropTypes.string.isRequired,
    chartId: PropTypes.string,
    error: PropTypes.string,
    errorMinus: PropTypes.string,
    labelWidth: PropTypes.number,
    readonly: PropTypes.bool
};

export function Error_Y({activeTrace, tbl_id, chartId, groupKey, error, errorMinus, labelWidth, readonly}) {
    return <Error {...{tbl_id, chartId, groupKey, axis: 'y', activeTrace, error, errorMinus, readonly}}/>;
}
Error_Y.propTypes = Error_X.propTypes;

function Error({tbl_id:ptbl_id, chartId, groupKey, axis, activeTrace:pActiveTrace, error, errorMinus, readonly}) {
    const chartProps = getChartProps(chartId, ptbl_id, pActiveTrace);
    const {tbl_id, activeTrace, mappings} = chartProps;

    error = error || get(mappings, [`error_${axis}.array`], '');
    errorMinus = errorMinus || get(mappings, [`error_${axis}.arrayminus`], '');
    const colValStats = getColValStats(tbl_id);

    const fldName = errorTypeFieldKey(activeTrace, axis);
    const defType = get(chartProps, fldName, getDefaultErrorType(chartProps, activeTrace, axis));
    const type = useStoreConnector(() => getFieldVal(groupKey, fldName, defType));

    const showKey = errorShowFieldKey(activeTrace, axis);
    const showError = toBoolean(useStoreConnector(() => getFieldVal(groupKey, showKey, get(chartProps, showKey))));

    readonly = readonly || !showError;

    const ErrFld = ({path, ...rest}) => {
        const props = {groupKey, colValStats, readonly, fldPath:path(activeTrace, axis), labelWidth:5, nullAllowed:true, ...rest};
        return  (<ColumnOrExpression {...props}/>);
    };
    const axisU = axis.toUpperCase();


    return (
        <div style={{display: 'flex'}}>
            <CheckboxGroupInputField fieldKey={showKey}
                                     initialState= {{value: (showError ? 'true' : undefined)}}
                                     label='Error:'
                                     labelWidth={25}
                                     tooltip='Turn error bars on/off'
                                     options={[{value: 'true'}]}
                                     wrapperStyle={{display: 'inline-flex', alignItems: 'center'}}/>


            <div style={{display: 'flex', alignItems: 'center', paddinngTop: 3, marginLeft: 4}}>
                <ListBoxInputField
                    initialState= {{
                        value: type,
                        tooltip: 'Select type of the errors',
                    }}
                    options={ERR_TYPE_OPTIONS}
                    fieldKey={errorTypeFieldKey(activeTrace, axis)}
                    groupKey={groupKey}
                    readonly={readonly}
                />
                <div style={{paddingLeft: 10}}>
                    {(type==='sym')  && <ErrFld name={`${axisU} Error`}       initValue={error}      path={errorFieldKey} label='' labelWidth={6}/>}
                    {(type==='asym') && <ErrFld name={`${axisU} Upper Error`} initValue={error}      path={errorFieldKey} label={'\u2191'}/>}
                    {(type==='asym') && <ErrFld name={`${axisU} Lower Error`} initValue={errorMinus} path={errorMinusFieldKey} label={'\u2193'}/>}
                </div>
            </div>
        </div>
    );
}
