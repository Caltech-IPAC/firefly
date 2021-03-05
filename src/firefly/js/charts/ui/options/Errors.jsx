import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';

import FieldGroupUtils, {getFieldVal} from '../../../fieldGroup/FieldGroupUtils.js';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {ColumnOrExpression} from '../ColumnOrExpression.jsx';
import ColValuesStatistics from '../../ColValuesStatistics.js';
import {getChartProps} from '../../ChartUtil.js';
import {getColValStats} from '../../TableStatsCntlr.js';
import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';


export const ERR_TYPE_OPTIONS = [
    {label: 'None', value: 'none'},
    {label: 'Symm', value: 'sym'},
    {label: 'Asym', value: 'asym'}

];

export function errorTypeFieldKey(activeTrace, axis) { return `fireflyData.${activeTrace}.error_${axis}.errorsType`; }
export function errorFieldKey(activeTrace, axis) { return `_tables.data.${activeTrace}.error_${axis}.array`; }
export function errorMinusFieldKey(activeTrace, axis) { return `_tables.data.${activeTrace}.error_${axis}.arrayminus`; }

export function getDefaultErrorType(chartData, activeTrace, axis) {
    const errorMinus = get(chartData, errorMinusFieldKey(activeTrace, axis).replace(/^_tables./, ''));
    if (errorMinus) {
        return 'asym';
    } else {
        const error = get(chartData, errorFieldKey(activeTrace, axis).replace(/^_tables./, ''));
        if (error) {
            return 'sym';
        } else {
            return 'none';
        }
    }
}

export class Errors extends PureComponent {

    constructor(props) {
        super(props);
        const {axis, groupKey, activeTrace} = props;
        this.state = {
            selectedErrType : getFieldVal(groupKey, errorTypeFieldKey(activeTrace, axis), 'none')
        };
    }

    static getDerivedStateFromProps(props,state) {
        const {axis, groupKey, activeTrace} = props;
        return {
            selectedErrType : getFieldVal(groupKey, errorTypeFieldKey(activeTrace, axis), 'none')
        };

    }

    componentDidMount() {
        const {axis, groupKey, activeTrace} = this.props;
        this.unbinder = FieldGroupUtils.bindToStore(groupKey,
            (fields) => {
                if (this.iAmMounted) {
                    const v = get(fields, [errorTypeFieldKey(activeTrace, axis), 'value'], 'none');
                    if (v !== this.state.selectedErrType) {
                        this.setState({selectedErrType: v});
                    }
                }
            });
        this.iAmMounted = true;
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
    }


    renderErrFld(props) {
        const {colValStats, groupKey} = this.props;
        const commonProps = {colValStats, groupKey, labelWidth: 5, nullAllowed:true};
        const allProps = Object.assign({}, commonProps, props);
        return  (<ColumnOrExpression {...allProps}/>);
    }


    render() {
        const {groupKey, axis, activeTrace=0, labelWidth=30, readonly} = this.props;
        const errType = this.state.selectedErrType;
        const axisU = axis.toUpperCase();

        return (
            <div style={{display: 'flex', alignItems: 'center', paddinngTop: 3}}>
                <ListBoxInputField
                    initialState= {{
                        //value: errType,
                        tooltip: 'Select type of the errors',
                        label: 'Error:',
                        labelWidth
                    }}
                    options={ERR_TYPE_OPTIONS}
                    fieldKey={errorTypeFieldKey(activeTrace, axis)}
                    groupKey={groupKey}
                    readonly={readonly}
                />
                <div style={{paddingLeft: 10}}>
                    {(errType==='sym') && this.renderErrFld({fldPath:errorFieldKey(activeTrace, axis), label: ' ', labelWidth: 6, name:`${axisU} Error`, readonly})}
                    {(errType==='asym') && this.renderErrFld({fldPath:errorFieldKey(activeTrace, axis), label: '\u2191', name:`${axisU} Upper Error`, readonly})}
                    {(errType==='asym') && this.renderErrFld({fldPath:errorMinusFieldKey(activeTrace, axis), label: '\u2193', name:`${axisU} Lower Error`, readonly})}
                </div>
            </div>
        );
    }
}


Errors.propTypes = {
    groupKey: PropTypes.string.isRequired,
    colValStats: PropTypes.arrayOf(PropTypes.instanceOf(ColValuesStatistics)).isRequired,
    axis: PropTypes.string.isRequired,
    activeTrace: PropTypes.number,
    labelWidth: PropTypes.number,
    readonly: PropTypes.bool
};



export function Error_X({activeTrace:pActiveTrace, tbl_id:ptbl_id, chartId, groupKey, labelWidth, readonly}) {
    const chartProps = getChartProps(chartId, ptbl_id, pActiveTrace);
    const {tbl_id, activeTrace, mappings} = chartProps;
    const fldName = errorTypeFieldKey(activeTrace, 'x');
    const defType = get(chartProps, fldName, getDefaultErrorType(chartProps, activeTrace, 'x'));
    const error = get(mappings, [activeTrace, 'error_x.array'], '');
    const errorMinus = get(mappings, [activeTrace, 'error_x.arrayminus'], '');
    const colValStats = getColValStats(tbl_id);

    const [type] = useStoreConnector(() => getFieldVal(groupKey, fldName, defType));

    return <Error {...{axis: 'x', groupKey, colValStats, activeTrace, type, error, errorMinus, labelWidth, readonly}}/>;
}

export function Error_Y({activeTrace:pActiveTrace, tbl_id:ptbl_id, chartId, groupKey, labelWidth, readonly}) {
    const chartProps = getChartProps(chartId, ptbl_id, pActiveTrace);
    const {tbl_id, activeTrace, mappings} = chartProps;
    const fldName = errorTypeFieldKey(activeTrace, 'y');
    const defType = get(chartProps, fldName, getDefaultErrorType(chartProps, activeTrace, 'y'));
    const error = get(mappings, ['error_y.array'], '');
    const errorMinus = get(mappings, ['error_y.arrayminus'], '');
    const colValStats = getColValStats(tbl_id);

    const [type] = useStoreConnector(() => getFieldVal(groupKey, fldName, defType));

    return <Error {...{axis: 'y', groupKey, colValStats, activeTrace, type, error, errorMinus, labelWidth, readonly}}/>;
}

Error_X.propTypes = Error_Y.propTypes = {
    activeTrace: PropTypes.number,
    tbl_id: PropTypes.string,
    groupKey: PropTypes.string.isRequired,
    chartId: PropTypes.string,
    labelWidth: PropTypes.number,
    readonly: PropTypes.bool
};



function Error({axis, groupKey, colValStats, activeTrace, type, error, errorMinus, labelWidth, readonly}) {

    const ErrFld = ({path, ...rest}) => {
        const props = {groupKey, colValStats, readonly, fldPath:path(activeTrace, axis), labelWidth:5, nullAllowed:true, ...rest};
        return  (<ColumnOrExpression {...props}/>);
    };

    const axisU = axis.toUpperCase();
    labelWidth -= type==='asym' ? 10 : 0;

    return (
        <div style={{display: 'flex', alignItems: 'center', paddinngTop: 3}}>
            <ListBoxInputField
                initialState= {{
                    value: type,
                    tooltip: 'Select type of the errors',
                    label: 'Error:',
                }}
                labelWidth={labelWidth}
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
    );
}
