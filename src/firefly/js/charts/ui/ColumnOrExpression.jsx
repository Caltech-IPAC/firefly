/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {useContext} from 'react';
import PropTypes, {object, shape} from 'prop-types';
import {defaultsDeep} from 'lodash';
import {Expression} from '../../util/expr/Expression.js';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import ColValuesStatistics from '../ColValuesStatistics.js';
import {showColSelectPopup} from './ColSelectView.jsx';
import MAGNIFYING_GLASS from 'html/images/icons-2014/magnifyingGlass.png';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {FieldGroupCtx} from '../../ui/FieldGroup.jsx';
import {AutoCompleteInput} from 'firefly/ui/AutoCompleteInput.jsx';


export const EXPRESSION_TTIPS = `
Supported operators: *, /, +, -.
Supported functions: abs(x), acos(x), asin(x), atan(x), atan2(x,y), ceil(x), 
cos(x), degrees(x), exp(x), floor(x), lg(x), ln(x), log10(x), log(x), power(x,y), 
radians(x), round(x), sin(x), sqrt(x), tan(x).
Example: sqrt(power(b,4) - 4*a*c) / (2*a), where a, b, c are column names.
Non-alphanumeric column names should be quoted in expressions.`;

export const ColsShape = PropTypes.arrayOf(PropTypes.shape({
    name: PropTypes.string,
    units: PropTypes.string,
    type: PropTypes.string,
    desc: PropTypes.string
}));


const DEFAULT_MSG= 'Can not be empty. Please provide value or expression';

export function getColValidator(cols, required=true, canBeExpression=true, message=DEFAULT_MSG) {
    const colNames = cols.map((colVal) => {return colVal.name;});
    return (val) => {
        let retval = {valid: true, message: ''};
        if (!val) {
            if (required) return {valid: false, message};
        } else if (colNames.indexOf(val) < 0) {
            if (canBeExpression) {
                const expr = new Expression(val, colNames);
                if (!expr.isValid()) {
                    retval = {valid: false, message: `${expr.getError().error}. Unable to parse ${val}.`};
                }
            } else {
                retval = {valid: false, message: `Invalid column: ${val}.`};
            }
        }
        return retval;
    };
}

function getOptions(cols, canBeExpression=true) {
    return cols.filter(({visibility}) => visibility !== 'hidden').map( ({name, label})=> ( { value: name, label: label || name} ));
}


export function ColumnOrExpression({colValStats,params,groupKey,fldPath,label,labelWidth=30,name,tooltip,
                                       nullAllowed,readOnly,initValue, slotProps, sx}) {
    if (!colValStats) return <div/>;
    return (
        <ColumnFld
            cols={colValStats.map((c)=>{return {name: c.name, units: c.unit, type: c.type, desc: c.descr};})}
            fieldKey={fldPath}
            initValue={initValue || params?.[fldPath]}
            canBeExpression={true}
            tooltip={`Column or expression for ${tooltip ? tooltip : name}.${EXPRESSION_TTIPS}`}
            {...{groupKey, label, labelWidth, name, nullAllowed, readOnly, slotProps, sx}} />
    );
}

ColumnOrExpression.propTypes = {
    colValStats: PropTypes.arrayOf(PropTypes.instanceOf(ColValuesStatistics)),
    params: PropTypes.object,
    groupKey: PropTypes.string.isRequired,
    fldPath: PropTypes.string.isRequired,
    label: PropTypes.string,
    labelWidth: PropTypes.number,
    name: PropTypes.string.isRequired,
    tooltip: PropTypes.string,
    nullAllowed: PropTypes.bool,
    readOnly: PropTypes.bool,
    initValue: PropTypes.string,
    slotProps: PropTypes.object,
    sx: PropTypes.object,
};

export function ColumnFld({cols, groupKey, fieldKey, initValue, label, tooltip='Table column', slotProps, sx,
                              doQuoteNonAlphanumeric,
                           name, nullAllowed, canBeExpression=false, readOnly, helper, required, validator,
                              placeholder, colTblId=null,onSearchClicked=null}) {
    const value = initValue || getFieldVal(groupKey, fieldKey);
    const colValidator = getColValidator(cols, !nullAllowed, canBeExpression);
    const {valid=true, message=''} = value ? colValidator(value) : {};
    const context= useContext(FieldGroupCtx);
    groupKey= groupKey || context.groupKey;

    let val = value;
    const onColSelected = (colName) => {
        val = colName;
        dispatchValueChange({fieldKey, groupKey, value: val, valid: true});
    };

    if (!helper) {
        helper = (
            <ToolbarButton icon={MAGNIFYING_GLASS}
                           tip={`Select ${name} column`}
                           onClick={(e) => {
                               if (!onSearchClicked || onSearchClicked()) {
                                   showColSelectPopup(cols, onColSelected, `Choose ${name}`, 'OK',
                                       val, false, colTblId,doQuoteNonAlphanumeric);
                               }
                           }}
            />
        );
    }
    return (
        <AutoCompleteInput fieldKey={fieldKey}
                           groupKey={groupKey}
                           size='sm'
                           title={tooltip}
                           label={label}
                           options={getOptions(cols, canBeExpression)}
                           initialState= {{
                               value,
                               valid,
                               message,
                               validator: validator || colValidator,
                               tooltip,
                               nullAllowed
                           }}
                           disableClearable={true}
                           endDecorator={!readOnly && helper ? helper : undefined}
                           {...{required,readOnly,placeholder,sx}}
                           slotProps={defaultsDeep(slotProps,
                               {tooltip: {
                                   disableInteractive: true, //to hide tooltip when hovering over it, to prevent its long size blocking other fields
                               }}
                           )}

        />
    );
}

ColumnFld.propTypes = {
    cols: ColsShape.isRequired,
    initValue: PropTypes.string,
    groupKey: PropTypes.string,
    fieldKey: PropTypes.string.isRequired,
    label: PropTypes.string,
    labelWidth: PropTypes.number,
    name: PropTypes.string.isRequired,
    tooltip: PropTypes.string,
    nullAllowed: PropTypes.bool,
    canBeExpression: PropTypes.bool,
    readOnly: PropTypes.bool,
    required: PropTypes.bool,
    helper: PropTypes.element,
    doQuoteNonAlphanumeric: PropTypes.bool,
    colTblId: PropTypes.string,
    onSearchClicked: PropTypes.func,
    placeholder: PropTypes.string,
    validator: PropTypes.func,
    slotProps: shape({
        input: object,
        control: object,
        label: object,
        tooltip: object
    }),
    sx: object
};

