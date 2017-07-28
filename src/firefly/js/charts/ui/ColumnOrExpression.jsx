/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {Expression} from '../../util/expr/Expression.js';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import ColValuesStatistics from '../ColValuesStatistics.js';
import {showColSelectPopup} from './ColSelectView.jsx';

const EXPRESSION_TTIPS = `
Supported operators: ^, *, /, +, -, <, <=, =, <>, >=, >, and, or.
Supported functions: abs(x), acos(x), asin(x), atan(x), atan2(x,y), ceil(x), cos(x), exp(x), floor(x), if(x,y,z), lg(x), ln(x), log10(x), log(x), max(x,y), min(x,y), round(x), sin(x), sqrt(x), tan(x).
Example: sqrt(b^2 - 4*a*c) / (2*a), where a, b, c are column names.`;

/*
 * Split content into prior content and the last alphanumeric token in the text
 * @param {string} text - current content of suggest box
 * @return {Object} with token and priorContent properties
 */
function parseSuggestboxContent(text) {
    let token='', priorContent='';
    if (text && text.length) {
        // [entireMatch, firstCature, secondCapture] or null
        const match =  text.match(/^(.*[^A-Za-z\d_]|)([A-Za-z\d_]*)$/);
        if (match && match.length === 3) {
            priorContent = match[1];
            token = match[2];
        }
    }
    return {token, priorContent};
}

export function getColValidator(colValStats, required=true) {
    const colNames = colValStats.map((colVal) => {return colVal.name;});
    return (val) => {
        let retval = {valid: true, message: ''};
        if (!val) {
            if (required) {
                return {valid: false, message: 'Can not be empty. Please provide value or expression'};
            }
        } else if (colNames.indexOf(val) < 0) {
            const expr = new Expression(val, colNames);
            if (!expr.isValid()) {
                retval = {valid: false, message: `${expr.getError().error}. Unable to parse ${val}.`};
            }
        }
        return retval;
    };
}

export function ColumnOrExpression({colValStats,params,groupKey,fldPath,label,labelWidth=30,name,tooltip,nullAllowed}) {

    // the suggestions are indexes in the colValStats array - it makes it easier to render then with labels
    const allSuggestions = colValStats.map((colVal,idx)=>{return idx;});

    const getSuggestions = (val)=>{
        const {token} = parseSuggestboxContent(val);
        if (val && val.endsWith(')')) {
            return [];
        }
        const matches = allSuggestions.filter( (idx)=>{return colValStats[idx].name.startsWith(token);} );
        return matches.length ? matches : allSuggestions;
    };

    const renderSuggestion = (idx)=>{
        const colVal = colValStats[idx];
        return colVal.name + (colVal.unit && colVal.unit !== 'null' ? ' ('+colVal.unit+')' : ' ');
    };

    const valueOnSuggestion = (prevVal, idx)=>{
        const {priorContent} = parseSuggestboxContent(prevVal);
        return priorContent+colValStats[idx].name;
    };

    const value = params ? get(params, fldPath) : getFieldVal(groupKey, fldPath);
    const colValidator = getColValidator(colValStats,!nullAllowed);
    const {valid=true, message=''} = value ? colValidator(value) : {};

    var val = value;
    const onColSelected = (colName) => {
        val = colName;
        dispatchValueChange({fieldKey: fldPath, groupKey, value: colName, valid: true});
    };

    // http://www.charbase.com/1f50d-unicode-left-pointing-magnifying-glass
    // http://www.charbase.com/1f50e-unicode-right-pointing-magnifying-glass
    const cols = '\ud83d\udd0e';
    return (
        <div style={{whiteSpace: 'nowrap'}}>
            <SuggestBoxInputField
                inline={true}
                initialState= {{
                    value,
                    valid,
                    message,
                    validator: colValidator,
                    tooltip: `Column or expression for ${tooltip ? tooltip : name}.${EXPRESSION_TTIPS}`,
                    nullAllowed
                }}
                getSuggestions={getSuggestions}
                renderSuggestion={renderSuggestion}
                valueOnSuggestion={valueOnSuggestion}
                fieldKey={fldPath}
                groupKey={groupKey}
                label={label ? label : ''}
                labelWidth={labelWidth}
            />
            <div style={{display: 'inline-block', cursor:'pointer', paddingLeft: 3, verticalAlign: 'middle', fontSize: 'larger'}}
                 title={`Select ${name} column`}
                 onClick={() => showColSelectPopup(colValStats, onColSelected,`Choose ${name}`,'OK',val)}>
                {cols}
            </div>
        </div>
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
