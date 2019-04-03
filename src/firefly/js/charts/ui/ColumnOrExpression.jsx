/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {Expression} from '../../util/expr/Expression.js';
import {quoteNonAlphanumeric} from '../../util/expr/Variable.js';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import ColValuesStatistics from '../ColValuesStatistics.js';
import {showColSelectPopup} from './ColSelectView.jsx';
import MAGNIFYING_GLASS from 'html/images/icons-2014/magnifyingGlass.png';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';


const EXPRESSION_TTIPS = `
Supported operators: *, /, +, -.
Supported functions: abs(x), acos(x), asin(x), atan(x), atan2(x,y), ceil(x), cos(x), exp(x), floor(x), lg(x), ln(x), log10(x), log(x), power(x,y), round(x), sin(x), sqrt(x), tan(x).
Example: sqrt(power(b,4) - 4*a*c) / (2*a), where a, b, c are column names.
Non-alphanumeric column names should be quoted in expressions.`;

export const ColsShape = PropTypes.arrayOf(PropTypes.shape({
    name: PropTypes.string,
    units: PropTypes.string,
    type: PropTypes.string,
    desc: PropTypes.string}));

/*
 * Split content into prior content and the last alphanumeric token in the text
 * @param {string} text - current content of suggest box
 * @return {Object} with token and priorContent properties
 */
function parseSuggestboxContent(text) {
    let token='', priorContent='';
    if (text && text.length) {
        // [entireMatch, firstCapture, secondCapture] or null
        const match =  text.match(/^(.*[^A-Za-z\d_"]|)([A-Za-z\d_"]*)$/);
        if (match && match.length === 3) {
            priorContent = match[1];
            token = match[2];
        }
    }
    return {token, priorContent};
}

export function getColValidator(cols, required=true, canBeExpression=true) {
    const colNames = cols.map((colVal) => {return colVal.name;});
    return (val) => {
        let retval = {valid: true, message: ''};
        if (!val) {
            if (required) {
                return {valid: false, message: 'Can not be empty. Please provide value or expression'};
            }
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

function getRenderSuggestion(cols) {
    return (idx)=>{
        const colVal = cols[idx];
        return colVal.name + (colVal.units && colVal.units !== 'null' ? ' ('+colVal.units+')' : ' ');
    };
}

function getSuggestions(cols, canBeExpression=true) {
    // the suggestions are indexes in the colValStats array - it makes it easier to render then with labels
    const allSuggestions = cols.map((colVal,idx)=>{return idx;});
    return (val)=>{
        if (!val) {  return []; }
        let token = val;
        if (canBeExpression) {
            token = get(parseSuggestboxContent(val), 'token');
            if (!token || val.endsWith(')')) {
                return [];
            }
        }
        const matches = allSuggestions.filter( (idx)=>{return cols[idx].name.toLowerCase().startsWith(token.toLowerCase());} );
        return matches.length ? matches : [];
    };
}

function getValueOnSuggestion(cols, canBeExpression=true) {
    return (prevVal, idx) => {
        const name = quoteNonAlphanumeric(cols[idx].name);
        if (canBeExpression) {
            const {priorContent} = parseSuggestboxContent(prevVal);
            return priorContent + name;
        } else {
            return name;
        }
    };
}

export function ColumnOrExpression({colValStats,params,groupKey,fldPath,label,labelWidth=30,name,tooltip,nullAllowed}) {
    return (
        <ColumnFld
            cols={colValStats.map((c)=>{return {name: c.name, units: c.unit, type: c.type, desc: c.descr};})}
            fieldKey={fldPath}
            initValue={get(params, fldPath)}
            canBeExpression={true}
            tooltip={`Column or expression for ${tooltip ? tooltip : name}.${EXPRESSION_TTIPS}`}
            {...{groupKey, label, labelWidth, name, nullAllowed}} />
    );
}

ColumnOrExpression.propTypes = {
    colValStats: PropTypes.arrayOf(PropTypes.instanceOf(ColValuesStatistics)).isRequired,
    params: PropTypes.object,
    groupKey: PropTypes.string.isRequired,
    fldPath: PropTypes.string.isRequired,
    label: PropTypes.string,
    labelWidth: PropTypes.number,
    name: PropTypes.string.isRequired,
    tooltip: PropTypes.string,
    nullAllowed: PropTypes.bool
};

export function ColumnFld({cols, groupKey, fieldKey, initValue, label, labelWidth, tooltip='Table column',
                           name, nullAllowed, canBeExpression=false, inputStyle}) {
    const value = initValue || getFieldVal(groupKey, fieldKey);
    const colValidator = getColValidator(cols, !nullAllowed, canBeExpression);
    const {valid=true, message=''} = value ? colValidator(value) : {};

    let val = value;
    const onColSelected = (colName) => {
        val = colName;
        dispatchValueChange({fieldKey, groupKey, value: colName, valid: true});
    };

    const labelProps = (label || labelWidth) ? { label: label || '', labelWidth} : {};

    return (
        <div style={{whiteSpace: 'nowrap'}}>
            <SuggestBoxInputField
                inline={true}
                initialState= {{
                    value,
                    valid,
                    message,
                    validator: colValidator,
                    tooltip,
                    nullAllowed
                }}
                getSuggestions={getSuggestions(cols, canBeExpression)}
                renderSuggestion={getRenderSuggestion(cols)}
                valueOnSuggestion={getValueOnSuggestion(cols,canBeExpression)}
                fieldKey={fieldKey}
                groupKey={groupKey}
                {...labelProps}
                inputStyle={inputStyle}
            />
            <div style={{display: 'inline-block', cursor:'pointer', paddingLeft: 2, verticalAlign: 'top'}}
                 title={`Select ${name} column`}
                 onClick={() => showColSelectPopup(cols, onColSelected,`Choose ${name}`,'OK',val)}>
                <ToolbarButton icon={MAGNIFYING_GLASS}/>
            </div>
        </div>
    );
}

ColumnFld.propTypes = {
    cols: ColsShape.isRequired,
    initValue: PropTypes.string,
    groupKey: PropTypes.string.isRequired,
    fieldKey: PropTypes.string.isRequired,
    label: PropTypes.string,
    labelWidth: PropTypes.number,
    name: PropTypes.string.isRequired,
    tooltip: PropTypes.string,
    nullAllowed: PropTypes.bool,
    canBeExpression: PropTypes.bool,
    inputStyle: PropTypes.object
};

