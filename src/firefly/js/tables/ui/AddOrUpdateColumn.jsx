/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useCallback, useState} from 'react';
import PropTypes from 'prop-types';
import {delay} from 'lodash';

import {SqlTableFilter, code} from './FilterEditor.jsx';
import {addOrUpdateColumn, deleteColumn} from '../../rpc/SearchServicesJson.js';
import {getGroupFields, validateFieldGroup, getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {getAllColumns, getColumn, getTblById} from '../TableUtil.js';
import {showPopup, showInfoPopup, showYesNoPopup} from '../../ui/PopupUtil.jsx';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';

import {ColumnFld, EXPRESSION_TTIPS} from '../../charts/ui/ColumnOrExpression.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {dispatchTableFetch, dispatchTableUiUpdate} from '../TablesCntlr.js';
import {textValidator} from '../../util/Validate.js';
import {formatColExpr} from '../../charts/ChartUtil.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import {UCDList} from '../../util/VOAnalyzer.js';

import MAGNIFYING_GLASS from 'images/icons-2014/magnifyingGlass.png';
import INSERT_COLUMN from 'html/images/insert-col-right-24-24.png';
import INFO from 'html/images/info-icon.png';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {setSelectInfo} from 'firefly/tables/TableRequestUtil.js';
import {dispatchHideDialog} from 'firefly/core/ComponentCntlr.js';
import {FilterInfo} from 'firefly/tables/FilterInfo.js';


let hideExpPopup;

export const AddOrUpdateColumn = React.memo(({tbl_ui_id, tbl_id, hidePopup, editColName, onChange}) => {

    const groupKey='table-add-column';
    const {preset} = getEditColInfo(tbl_id, editColName);

    const [isWorking, setIsWorking] = useState(false);
    const mode = useStoreConnector(() => getFieldVal(groupKey, 'mode', preset ? 'Preset' : 'Custom'));

    const ref = useCallback((node) => delay(() => node?.focus(), 100), []);
    const cols = getAllColumns(getTblById(tbl_id));
    const colNames = cols.map((c) => c.name);

    const fieldProps = {labelWidth: 75, style: {width: 200}};

    const doUpdate = () => {
        const {request, selectInfo} = getTblById(tbl_id);
        const fields = getGroupFields(groupKey);
        validateFieldGroup(groupKey).then((valid) => {
            if (valid) {
                const params = Object.values(fields).filter((f) => f.mounted)
                                .reduce((pval, f) => ({...pval, [f.fieldKey]: f.value}), {});
                params.expression = formatColExpr({colOrExpr: params.expression, quoted: true, colNames});
                if (editColName) params.editColName = editColName;
                if (params.preset === 'filtered' || params.preset === 'selected')  params.dtype = 'boolean';
                if (params.preset === 'ROW_NUM') params.dtype = 'long';

                setSelectInfo(request, selectInfo);
                setIsWorking(true);
                addOrUpdateColumn(request, params).then( () => {
                        hidePopup?.();
                        dispatchTableUiUpdate( {tbl_ui_id, columns:[], columnWidths: undefined, scrollLeft:100000});        // reset columns and scroll to the right-most of the table so the added column is visible
                        onChange?.();
                    reloadTable(request,editColName, params.cname);
                    }).catch( (err) => {
                        showInfoPopup(parseError(err, params), 'Add Column Failed');
                    }).finally(() => setIsWorking(false));
            }
        });
    };
    const doDelete = () => {
        showYesNoPopup('Are you sure that you want to delete this column from the table?',(id, yes) => {
            if (yes) {
                const {request} = getTblById(tbl_id);
                deleteColumn(request, editColName).then( () => {
                    hidePopup?.();
                    dispatchTableUiUpdate( {tbl_ui_id, columns:[], columnWidths: undefined});        // reset columns and scroll to the right-most of the table so the added column is visible
                    // dispatchTableUiUpdate()
                    reloadTable(request,editColName);
                    onChange?.();
                }).catch( (err) => showInfoPopup(parseError(err), 'Delete Column Failed'));
            }
            dispatchHideDialog(id);
        });

    };

    const buttonLabel = editColName ? 'Update Column' : 'Add Column';
    const DelBtn = (<button type='button' className='button std'onClick={doDelete}>Delete Column</button>);

    return (
        <div style={{position:'relative'}}>
            {isWorking && <div className='loading-mask' style={{zIndex:1}}/>}
            <div className='required-msg'/>
            <FieldGroup groupKey={groupKey} className='AddColumn__form'>
                <ValidationField fieldKey='cname' label='Name:' inputRef={ref} required={true} {...fieldProps} initialState={{
                    value: editColName,
                    validator:textValidator({min:1, max: 128,
                        pattern: /^[a-z0-9_]+$/i,
                        message:'Column name is required and must contain only A to Z, 0 to 9, and underscore (_) characters'})
                }}/>
                <RadioGroupInputField fieldKey='mode' initialState={{label: 'Mode:', value:mode}} options={[{value:'Custom', label:'Enter expression'}, {value:'Preset', label:'Use preset function'}]} {...fieldProps}/>
                {mode === 'Custom' ? <CustomFields {...{tbl_ui_id, tbl_id, groupKey, fieldProps, editColName}}/> : <PresetFields {...{tbl_id, editColName, fieldProps}}/>}
            </FieldGroup>
            <div className='AddColumn__actions'>
                <button type='button' className='button std' style={{marginRight: 5}}
                        onClick={doUpdate}>{buttonLabel}
                </button>
                {editColName && DelBtn}
                <button type='button' className='button std'
                        onClick={() => hidePopup?.()}> Cancel
                </button>
                <HelpIcon helpId={'tables.addColumn'} style={{float: 'right', marginTop: 4}}/>
            </div>
        </div>
    );

});

AddOrUpdateColumn.propTypes = {
    tbl_ui_id: PropTypes.string,
    tbl_id: PropTypes.string,
    hidePopup: PropTypes.func,
    editColName: PropTypes.string
};

function reloadTable(request, editColName, newColName) {

    if (editColName && editColName !== newColName) {
        const {sortInfo, filters, inclCols, sqlFilter} = request;

        // if editColumn is in sort or filter, clear it.
        if (sortInfo) {
            if (sortInfo.match(/[A-Z],(.+)/)?.[1]?.replaceAll('"','').split(',')?.includes(editColName)) {
                Reflect.deleteProperty(request,'sortInfo');
            }
        }
        if (filters) {
            if (FilterInfo.parse(filters).getFilter(editColName)) {
                Reflect.deleteProperty(request,'filters');
            }
        }
        if (sqlFilter) {
            if (sqlFilter.match(/"(\\.|[^"\\])*"/g)?.includes(editColName)) {
                Reflect.deleteProperty(request,'sqlFilter');
            }
        }
        // if editColumn is in the 'select' portion, remove it.
        if (inclCols) {
            const cols = inclCols.split(',');
            if (cols?.includes(editColName)) {
                request.inclCols = cols.filter((c) => c !== editColName).join();
            }
        }
    }
    dispatchTableFetch(request);
}

export const AddColumnBtn = ({tbl_ui_id, tbl_id}) => (
    <ToolbarButton icon={INSERT_COLUMN} tip='Add a column'
                   onClick={() => showAddOrUpdateColumn({tbl_ui_id, tbl_id})}/>
);

export function showAddOrUpdateColumn({tbl_ui_id, tbl_id, editColName, onChange}) {
    let popup;
    const hidePopup = () => popup?.();
    const title =  editColName ? 'Edit a derived column' : 'Add a column';

    popup = showPopup({ID:'addOrUpdateColumn', content: <AddOrUpdateColumn {...{tbl_id, tbl_ui_id, hidePopup, editColName, onChange}}/>, title, modal: true});
}

function getEditColInfo(tbl_id, editColName) {
    const col = getColumn(getTblById(tbl_id), editColName) || {};
    const desc = col?.desc?.replace(/\(DERIVED_FROM=.+\) /, '');
    const derivedFrom = col?.desc?.match(/\(DERIVED_FROM=(.+)\).*/)?.[1];
    const preset = derivedFrom?.match(/preset:(.+)/)?.[1];
    const expression = preset ? '' : derivedFrom;
    return {col,desc,preset,expression};
}

function DescField ({desc, fieldProps}) {
    return <ValidationField fieldKey='desc' label='Description:' initialState={{value:desc}}
                            tooltip='A one-line description for the column heading tooltip and table options' {...fieldProps}/>;
}

function PresetFields ({tbl_id, editColName, fieldProps}) {
    const {preset, desc} = getEditColInfo(tbl_id, editColName);

    return (
        <>
            <ListBoxInputField fieldKey='preset' label='Select a preset:'
                               options={[
                                   {value:'filtered', label:"Set filtered rows to 'true' and the rest to 'false'"},
                                   {value:'selected', label:"Set selected rows to 'true' and the rest to 'false'"},
                                   {value:'ROW_NUM', label:'Number rows in current sort order'}]}
                               initialState={{value: preset}} {...fieldProps}/>
            <DescField {...{desc, fieldProps}}/>
        </>
    );
}

function CustomFields({tbl_ui_id, tbl_id, groupKey, fieldProps, editColName}) {

    const dtype = useStoreConnector(() => getFieldVal(groupKey, 'dtype', 'double'));
    const exprKey = 'expression';
    const cols = getAllColumns(getTblById(tbl_id));

    const onChange = ({sql}) => {
        dispatchValueChange({fieldKey: exprKey, groupKey, value:sql, valid: true});
        hideExpPopup?.();
    };
    const {col={}, desc} = getEditColInfo(tbl_id, editColName);
    if (col.type === 'boolean') col.type = 'double';        // when switching over from boolean type.

    return (
        <>
            <ColumnFld fieldKey={exprKey} name='Expression' cols={cols} label='Expression:' required={true} initValue={col.DERIVED_FROM}
                       canBeExpression={true} nullAllowed={false} inputStyle={{width: 200}} validator={textValidator({min:1})}
                       helper={<Helper {...{tbl_ui_id, tbl_id, onChange}}/>} tooltip={EXPRESSION_TTIPS} {...fieldProps}
            />
            <div className='AddColumn__datatype'>
                <ListBoxInputField fieldKey='dtype' label='Data Type:'
                                   options={[{value:'double'}, {value:'long'}, {value:'char'}]} initialState={{value: col.type}} {...fieldProps}/>
                {dtype === 'double' &&
                <ValidationField fieldKey='precision' label='Precision:' labelWidth={50} style={{width:70}}
                                 placeholder='e.g. F6'
                                 initialState={{ value: col.precision, validator:textValidator({pattern: /^$|^[FE]?[1-9]$/i,
                                         message:'Precision must be Fn or En. When Fn, n is the number of digits after the decimal.  ' +
                                             'And when En, n is the precision in scientific notation'
                                     })}}
                />
                }
            </div>
            <Info url='https://ivoa.net/documents/VOUnits'>
                <ValidationField fieldKey='units' label='Units:' initialState={{value: col.units}} tooltip='Units of measurement, IVOA VOUnits preferred' {...fieldProps}/>
            </Info>
            <Info url='https://ivoa.net/documents/UCD1+'>
                <SuggestBoxInputField fieldKey='ucd' label='UCD:' initialState={{value: col.UCD}} tooltip='IVOA Unified Content Descriptor, UCD1+ style'
                                      getSuggestions={getSuggestions} valueOnSuggestion={valueOnSuggestion} inputStyle={{width:200}} {...fieldProps}/>
            </Info>
            <DescField {...{desc, fieldProps}}/>
        </>
    );
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


function parseError(err) {
    if (err) {
        const [main, details] = err.message.split(';');
        return  (
            <div>
                <div style={{fontWeight: 'bold', marginBottom: 5}}>{main}</div>
                <code>{details}</code>
            </div>
        );
    } else return 'Operation failed with unexpected error.';
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

function Info({url, target='info', style={}, children}) {
    return (
        <div style={{display:'inline-flex', alignItems:'center', ...style}}>
            {children}
            <a href={url} target={target} tabIndex='-1'><img src={INFO} style={{height: 16}} /></a>
        </div>
    );

}