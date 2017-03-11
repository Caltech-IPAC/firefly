/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {PropTypes} from 'react';

import {get} from 'lodash';
import {Expression} from '../../util/expr/Expression.js';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {TextButton} from '../../ui/TextButton.jsx';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import ColValuesStatistics from '../ColValuesStatistics.js';
import {showColSelectPopup} from './ColSelectView.jsx';


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

export function ColumnOrExpression({colValStats,params,groupKey,fldPath,label,labelWidth=30,tooltip,nullAllowed}) {

    // the suggestions are indexes in the colValStats array - it makes it easier to render then with labels
    const allSuggestions = colValStats.map((colVal,idx)=>{return idx;});

    const getSuggestions = (val)=>{
        const {token} = parseSuggestboxContent(val);
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

    var val = get(params, fldPath);
    const onColSelected = (colName) => {
        val = colName;
        dispatchValueChange({fieldKey: fldPath, groupKey, value: colName, valid: true});
    };
    const colValidator = getColValidator(colValStats,!nullAllowed);
    const value = get(params, fldPath);
    const {valid=true, message=''} = value ? colValidator(value) : {};
    return (
        <div style={{whiteSpace: 'nowrap'}}>
            <SuggestBoxInputField
                inline={true}
                initialState= {{
                    value,
                    valid,
                    message,
                    validator: colValidator,
                    tooltip: `Column or expression for ${tooltip}`,
                    label: `${label}:`,
                    nullAllowed
                }}
                getSuggestions={getSuggestions}
                renderSuggestion={renderSuggestion}
                valueOnSuggestion={valueOnSuggestion}
                fieldKey={fldPath}
                groupKey={groupKey}
                labelWidth={labelWidth}
            />
            <TextButton style={{display: 'inline-block', paddingLeft: 3, verticalAlign: 'bottom'}}
                        groupKey={groupKey}
                        text='Cols'
                        tip={`Select ${label} column`}
                        onClick={() => showColSelectPopup(colValStats, onColSelected,`Choose ${label}`,'OK',val)}
            />
        </div>
    );
}


ColumnOrExpression.propTypes = {
    colValStats: PropTypes.arrayOf(PropTypes.instanceOf(ColValuesStatistics)).isRequired,
    params: PropTypes.object,
    groupKey: PropTypes.string.isRequired,
    fldPath: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    labelWidth: PropTypes.number,
    tooltip: PropTypes.string,
    nullAllowed: PropTypes.bool
};