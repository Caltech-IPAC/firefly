import React from 'react';
import PropTypes from 'prop-types';
import {defaultsDeep, get} from 'lodash';

import {getFieldVal} from '../../../fieldGroup/FieldGroupUtils.js';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {ColumnOrExpression} from '../ColumnOrExpression.jsx';
import {getChartProps} from '../../ChartUtil.js';
import {getColValStats} from '../../TableStatsCntlr.js';
import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {toBoolean} from '../../../util/WebUtil.js';
import {Stack} from '@mui/joy';
import {omit} from 'lodash/object';
import {SwitchInputField} from 'firefly/ui/SwitchInputField';


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


export function Error_X(props) {
    return <Error {...{...props, axis: 'x'}}/>;
}

Error_X.propTypes = omit(Error.propTypes, 'axis');

export function Error_Y(props) {
    return <Error {...{...props, axis: 'y'}}/>;
}

Error_Y.propTypes = Error_X.propTypes;

Error.propTypes = {
    activeTrace: PropTypes.number,
    tbl_id: PropTypes.string,
    groupKey: PropTypes.string.isRequired,
    chartId: PropTypes.string,
    error: PropTypes.string,
    errorMinus: PropTypes.string,
    readonly: PropTypes.bool,
    axis: PropTypes.string,
    slotProps: PropTypes.shape({
        control: PropTypes.object,
        label: PropTypes.object,
        errorToggleInput: PropTypes.object,
        errorTypeInput: PropTypes.object,
        errorInput: PropTypes.object,
        upperErrorInput: PropTypes.object,
        lowerErrorInput: PropTypes.object
    })
};

function Error({tbl_id:ptbl_id, chartId, groupKey, axis, activeTrace:pActiveTrace, error, errorMinus, readonly, slotProps}) {
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

    const ErrFld = ({path, slotProps, ...rest}) => {
        const props = {
            groupKey, colValStats, readonly, fldPath:path(activeTrace, axis), nullAllowed:true,
            slotProps: defaultsDeep(slotProps, {
                control: {
                    orientation: 'horizontal',
                    sx: {'& label.MuiFormLabel-root': {width: 'min-content'}}
                }
            }),
            ...rest
        };
        return  (<ColumnOrExpression {...props}/>);
    };
    const axisU = axis.toUpperCase();


    return (
        <Stack direction='row' spacing={1} alignItems='baseline'>
            <SwitchInputField fieldKey={showKey}
                              initialState= {{value: showError}}
                              label='Error:'
                              tooltip='Turn error bars on/off'
                              slotProps={slotProps?.errorToggleInput}
            />
            <Stack direction='row' spacing={1} alignItems='baseline'>
                <ListBoxInputField
                    initialState= {{value: type}}
                    tooltip='Select type of the errors'
                    options={ERR_TYPE_OPTIONS}
                    fieldKey={errorTypeFieldKey(activeTrace, axis)}
                    groupKey={groupKey}
                    readonly={readonly}
                    slotProps={defaultsDeep(slotProps?.errorTypeInput, {input: {size: 'sm'}})}
                    sx={{visibility: showError ? 'visible' : 'hidden'}} //to prevent layout from jumping when switch is toggled
                />
                {showError &&
                    <Stack spacing={.5}>
                        {(type === 'sym') && <ErrFld name={`${axisU} Error`} initValue={error} path={errorFieldKey}
                                                     slotProps={slotProps?.errorInput}/>}
                        {(type === 'asym') &&
                            <ErrFld name={`${axisU} Upper Error`} initValue={error} path={errorFieldKey} label={'\u2191'}
                                    slotProps={slotProps?.upperErrorInput}/>}
                        {(type === 'asym') &&
                            <ErrFld name={`${axisU} Lower Error`} initValue={errorMinus} path={errorMinusFieldKey}
                                    label={'\u2193'}
                                    slotProps={slotProps?.lowerErrorInput}/>}
                    </Stack>
                }
            </Stack>
        </Stack>
    );
}
