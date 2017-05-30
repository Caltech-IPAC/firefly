import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';

import FieldGroupUtils, {getFieldVal} from '../../../fieldGroup/FieldGroupUtils.js';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {ColumnOrExpression} from '../ColumnOrExpression.jsx';
import ColValuesStatistics from '../../ColValuesStatistics.js';


const ERR_TYPE_OPTIONS = [
    {label: 'None', value: 'none'},
    {label: 'Sym', value: 'sym'},
    {label: 'Asym', value: 'asym'}

];

export function errorTypeFieldKey(activeTrace, axis) { return `data.${activeTrace}.firefly.error_${axis}.errorsType`; }
export function errorFieldKey(activeTrace, axis) { return `_tables.data.${activeTrace}.error_${axis}.array`; }
export function errorMinusFieldKey(activeTrace, axis) { return `_tables.data.${activeTrace}.error_${axis}.arrayminus`; }

export class Errors extends PureComponent {

    constructor(props) {
        super(props);
        const {axis, groupKey, activeTrace} = props;
        this.state = {
            selectedErrType : getFieldVal(groupKey, errorTypeFieldKey(activeTrace, axis), 'none')
        };
    }

    componentWillUpdate(nextProps) {
        const {axis, groupKey, activeTrace} = nextProps;
        this.setState({
            selectedErrType : getFieldVal(groupKey, errorTypeFieldKey(activeTrace, axis), 'none')
        });
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
        const {groupKey, axis, activeTrace=0, labelWidth=30} = this.props;
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
                />
                <div style={{paddingLeft: 10}}>
                    {(errType==='sym') && this.renderErrFld({fldPath:errorFieldKey(activeTrace, axis), name:`${axisU} Error`})}
                    {(errType==='asym') && this.renderErrFld({fldPath:errorFieldKey(activeTrace, axis), label: '\u2191', name:`${axisU} Upper Error`})}
                    {(errType==='asym') && this.renderErrFld({fldPath:errorMinusFieldKey(activeTrace, axis), label: '\u2193', name:`${axisU} Lower Error`})}
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
    labelWidth: PropTypes.number
};
