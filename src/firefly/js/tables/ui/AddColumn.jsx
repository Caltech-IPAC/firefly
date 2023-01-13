/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useCallback} from 'react';
import PropTypes from 'prop-types';
import {delay} from 'lodash';

import {SqlTableFilter, code} from './FilterEditor.jsx';
import {addTableColumn} from 'firefly/rpc/SearchServicesJson.js';
import {getGroupFields, validateFieldGroup, getFieldVal} from 'firefly/fieldGroup/FieldGroupUtils.js';
import {ValidationField} from 'firefly/ui/ValidationField.jsx';
import {getAllColumns, getTblById} from '../TableUtil.js';
import {showPopup, showInfoPopup} from '../../ui/PopupUtil.jsx';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';

import {ColumnFld} from 'firefly/charts/ui/ColumnOrExpression.jsx';
import {ListBoxInputField} from 'firefly/ui/ListBoxInputField.jsx';
import {FieldGroup} from 'firefly/ui/FieldGroup.jsx';
import {dispatchValueChange} from 'firefly/fieldGroup/FieldGroupCntlr.js';
import {dispatchTableFetch, dispatchTableUiUpdate} from 'firefly/tables/TablesCntlr.js';
import {textValidator} from 'firefly/util/Validate.js';
import {formatColExpr} from 'firefly/charts/ChartUtil.js';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';
import {SuggestBoxInputField} from 'firefly/ui/SuggestBoxInputField.jsx';
import {UCDList} from 'firefly/util/VOAnalyzer.js';

import MAGNIFYING_GLASS from 'images/icons-2014/magnifyingGlass.png';
import INSERT_COLUMN from 'html/images/insert-col-right-24-24.png';


let hideExpPopup, hideDialogPopup;

export const AddColumn = React.memo(({tbl_ui_id, tbl_id}) => {


    const groupKey='table-add-column';
    const expression = 'expression';

    const dtype = useStoreConnector(() => getFieldVal(groupKey, 'dtype', 'double'));

    const ref = useCallback((node) => delay(() => node?.focus(), 100), []);
    const cols = getAllColumns(getTblById(tbl_id));
    const colNames = cols.map((c) => c.name);

    const props = {labelWidth: 75, style: {width: 200}};
    const onChange = ({sql}) => {
        dispatchValueChange({fieldKey: expression, groupKey, value:sql, valid: true});
        hideExpPopup?.();
    };

    const addColumn = () => {
        const {request} = getTblById(tbl_id);
        const fields = getGroupFields(groupKey);
        validateFieldGroup(groupKey, false).then((valid) => {
            if (valid) {
                const params = Object.keys(fields).reduce((pval, key) => ({...pval, [key]: fields[key].value}), {});
                params.expression = formatColExpr({colOrExpr: params.expression, quoted: true, colNames});

                addTableColumn(request, params)
                    .then( () => {
                        hideDialogPopup?.();
                        dispatchTableUiUpdate( {tbl_ui_id, columns:[], columnWidths: undefined, scrollLeft:100000});        // reset columns and scroll to the right-most of the table so the added column is visible
                        dispatchTableFetch(request);
                    }).catch( (err) => {
                    showInfoPopup(parseError(err, params), 'Add Column Failed');
                });
            }
        });

    };

    return (
        <div>
            <div className='required-msg'/>
            <FieldGroup groupKey={groupKey} className='AddColumn__form'>
                <ValidationField fieldKey='cname' label='Name:' inputRef={ref} required={true} {...props} initialState={{
                    validator:textValidator({min:1, max: 128,
                        pattern: /^[a-z0-9_]+$/i,
                        message:'Column name is required and must contain only A to Z, 0 to 9, and underscore (_) characters'})
                }}/>
                <ColumnFld fieldKey={expression} name='Expression' cols={cols} label='Expression:' required={true}
                           canBeExpression={true} nullAllowed={false} inputStyle={{width: 200}} validator={textValidator({min:1})}
                           helper={<Helper {...{tbl_ui_id, tbl_id, onChange}}/>} tooltip='Expression of the new column' {...props}/>
                <div className='AddColumn__datatype'>
                    <ListBoxInputField fieldKey='dtype' label='Data Type:'
                                       options={[{value:'double'}, {value:'long'}, {value:'char'}]} {...props}/>
                    {dtype === 'double' &&
                        <ValidationField fieldKey='precision' label='Precision:' labelWidth={50} style={{width:70}}
                            placeholder='e.g. F6'
                            initialState={{validator:textValidator({pattern: /^$|^[FE]?[1-9]$/i,
                                    message:'Precision must be Fn or En. When Fn, n is the number of digits after the decimal.  ' +
                                        'And when En, n is the precision in scientific notation'
                            })}}
                        />
                    }
                </div>
                <ValidationField fieldKey='units' label='Units:' {...props}/>
                <ValidationField fieldKey='desc' label='Description:' {...props}/>
                <SuggestBoxInputField fieldKey='ucd' label='UCD' getSuggestions={getSuggestions} valueOnSuggestion={valueOnSuggestion} inputStyle={{width:200}} {...props}/>
            </FieldGroup>
            <div className='AddColumn__actions'>
                <button type='button' className='button std' style={{marginRight: 5}}
                        onClick={addColumn}>Add Column
                </button>
                <button type='button' className='button std'
                        onClick={() => hideDialogPopup?.()}>Cancel
                </button>
                <HelpIcon helpId={'tables.addColumn'} style={{float: 'right', marginTop: 4}}/>
            </div>
        </div>
    );

});

AddColumn.propTypes = {
    tbl_ui_id: PropTypes.string,
    tbl_id: PropTypes.string
};

export const AddColumnBtn = ({tbl_ui_id, tbl_id}) => (
    <>
        <ToolbarButton icon={INSERT_COLUMN} tip='Add a new column' onClick={() => showAddColumn({tbl_ui_id, tbl_id})}/>
    </>
);

function showAddColumn({tbl_ui_id, tbl_id}) {
    hideDialogPopup = showPopup({ID:'AddColumnDialog', content: <AddColumn {...{tbl_id, tbl_ui_id}}/>, title: 'Add a column', modal: true});
}

function Helper({tbl_ui_id, tbl_id, onChange}) {
    const content = (
        <div style={{height: 425, width: 700, position: 'relative'}}>
            <SqlTableFilter inputLabel='Expression' placeholder='e.g. w3mpro - w4mpro'
                            usages={<Usages/>} samples={<Samples/>} {...{tbl_ui_id, tbl_id, onChange}}/>
        </div>

    );
    const onClick = () => hideExpPopup = showPopup({ID:'ExpressionCreator', title: 'Expression Creator', content});
    return (
        <ToolbarButton icon={MAGNIFYING_GLASS} tip='Add a new column' style={{height: 14}} onClick={onClick}/>
    );
}

const Usages = () => {
    return (
        <>
            <h4>Usage</h4>
            <div style={{marginLeft: 5}}>
                <div>Input should follow the syntax of an SQL expression.</div>
                <div>Click on a Column name to insert the name into the Expression input box.</div>
                <div style={{marginTop: 5}}>Standard SQL-like operators can be used where applicable.
                    Supported operators are:
                    <span {...code}>{'  +, -, *, /, =, >, <, >=, <=, !=, LIKE, IN, IS NULL, IS NOT NULL'}</span>
                </div>
                <div style={{marginTop: 5}}>
                    You may use functions as well.  A few of the common functions are listed below.
                    For a list of all available functions, click <a href='http://hsqldb.org/doc/2.0/guide/builtinfunctions-chapt.html' target='_blank'>here</a>
                </div>
                <div style={{marginLeft: 5}}>
                    <li style={{whiteSpace: 'nowrap'}}>String functions: <span {...code}>CONCAT(s1,s2[,...]]) SUBSTR(s,offset,length)</span></li>
                    <li style={{whiteSpace: 'nowrap'}}>Numeric functions: <span {...code}>LOG10(x) LN(x) DEGREES(x) COS(x) POWER(x,y)</span></li>
                </div>
            </div>
        </>
    );
};

const Samples = () => {
    return (
        <>
            <h4>Sample Expressions</h4>
            <div style={{marginLeft: 5}}>
                <li style={{whiteSpace: 'nowrap'}}><div {...code}>{'"w3mpro" - "w4mpro"'}</div></li>
                <li style={{whiteSpace: 'nowrap'}}><div {...code}>{'sqrt(power("w3sigmpro",2) + power("w4sigmpro",2))'}</div></li>
                <li style={{whiteSpace: 'nowrap'}}><div {...code}>{'("ra"-82.0158188)*cos(radians("dec"))'}</div></li>
                <li style={{whiteSpace: 'nowrap'}}><div {...code}>{'"phot_g_mean_mag"-(5*log10(1000/"parallax") - 5)'}</div></li>
            </div>
        </>
    );
};


function parseError(err, {cname, expression}) {
    // samples: "StatementCallback; bad SQL grammar [ALTER TABLE DATA ADD COLUMN newCol double]; nested exception is java.sql.SQLSyntaxErrorException: object name already exists in statement [ALTER TABLE DATA ADD COLUMN newCol double]"
    //          "StatementCallback; bad SQL grammar [ALTER TABLE DATA ADD COLUMN newCol-2 double]; nested exception is java.sql.SQLSyntaxErrorException: unexpected token: -"

    // column exists in table
    if (err?.message?.match(/name already exists/)) return `ERROR: A column with the name "${cname}" already exists in the table`;

    // unexpected error
    return err?.message;
}


function getSuggestions(val) {
    if (!val) return [];
    const cvals = val.toLowerCase().split(';').map((v) => v.trim());
    return UCDList.filter((ucd) => cvals.some((v) => ucd.includes(v)));
}

function valueOnSuggestion(cval='', suggestion) {
    if (cval.includes(';')) {
        const vals = cval.split(';');
        return [...vals.slice(0, vals.length-1), suggestion].map((v) => v.trim()).join(';');
    }
    return suggestion;
}