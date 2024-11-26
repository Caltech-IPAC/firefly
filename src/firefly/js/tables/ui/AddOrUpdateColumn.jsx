/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useCallback, useState} from 'react';
import PropTypes from 'prop-types';
import {Button, Stack, Link, Sheet, Typography, Box, Skeleton} from '@mui/joy';
import {delay} from 'lodash';
import {UCDList} from '../../voAnalyzer/VoConst.js';

import {SqlTableFilter} from './FilterEditor.jsx';
import {addOrUpdateColumn, deleteColumn} from '../../rpc/SearchServicesJson.js';
import {getGroupFields, validateFieldGroup, getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {DOC_FUNCTIONS_URL, getAllColumns, getColumn, getColumns, getTableUiById, getTblById} from '../TableUtil.js';
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

import MAGNIFYING_GLASS from 'images/icons-2014/magnifyingGlass.png';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {setSelectInfo} from 'firefly/tables/TableRequestUtil.js';
import {dispatchHideDialog} from 'firefly/core/ComponentCntlr.js';
import {FilterInfo} from 'firefly/tables/FilterInfo.js';
import {AddColumnButton} from 'firefly/visualize/ui/Buttons.jsx';
import {RequiredFieldMsg} from 'firefly/ui/InputField.jsx';
import {Stacker} from 'firefly/ui/Stacker.jsx';
import {TableErrorMsg} from './TablePanel';


let hideExpPopup;

export const AddOrUpdateColumn = React.memo(({tbl_ui_id, tbl_id, hidePopup, editColName, onChange}) => {

    const groupKey='table-add-column';
    const {preset} = getEditColInfo(tbl_id, editColName);

    const [isWorking, setIsWorking] = useState(false);
    const mode = useStoreConnector(() => getFieldVal(groupKey, 'mode', preset ? 'Preset' : 'Custom'));

    const ref = useCallback((node) => delay(() => node?.focus(), 100), []);
    const cols = getAllColumns(getTblById(tbl_id));
    const colNames = cols.map((c) => c.name);

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
                        clearColumnInfo(tbl_ui_id, request, editColName, params.cname);
                        onChange?.();
                    }).catch( (err) => {
                        showInfoPopup(<TableErrorMsg error={err?.cause || err}/>, 'Add Column Failed');
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
                    clearColumnInfo(tbl_ui_id, request, editColName);
                    onChange?.();
                }).catch( (err) => showInfoPopup(<TableErrorMsg error={err?.cause || err}/>, 'Delete Column Failed'));
            }
            dispatchHideDialog(id);
        });

    };

    const buttonLabel = editColName ? 'Update Column' : 'Add Column';
    const DelBtn = (<Button color='danger' variant='solid' onClick={doDelete}>Delete Column</Button>);

    return (
        <Stack p={1} width={500} spacing={1} position='relative'
            sx={{
                '.ff-Input':{mb:1},
                '.MuiInput-endDecorator > a':{width:12},
                label:{width:80},
                input:{width:265}
            }}>
            {isWorking && <Skeleton sx={{inset:0}}/>}
            <RequiredFieldMsg/>
            <FieldGroup groupKey={groupKey}>
                <ValidationField fieldKey='cname' label='Name:' inputRef={ref}
                    tooltip='Column name'
                    required={true}
                    orientation='horizontal'
                    initialState={{
                        value: editColName,
                        validator:textValidator({min:1, max: 128,
                            pattern: /^[a-z0-9_]+$/i,
                            message:'Column name is required and must contain only A to Z, 0 to 9, and underscore (_) characters'})
                    }}/>
                <RadioGroupInputField fieldKey='mode'
                    orientation='horizontal'
                    sx={{'& .MuiRadio-label':{width:115}}}
                    initialState={{label: 'Mode:', value:mode}}
                    options={[
                        {value:'Custom', label:'Enter expression'},
                        {value:'Preset', label:'Use preset function'}
                    ]}/>
                {mode === 'Custom' ? <CustomFields {...{tbl_ui_id, tbl_id, groupKey, editColName}}/> : <PresetFields {...{tbl_id, editColName}}/>}
            </FieldGroup>
            <Stacker endDecorator={<HelpIcon helpId={'tables.addColumn'}/>}>
                <Button color='primary' variant='solid' onClick={doUpdate}>{buttonLabel}</Button>
                {editColName && DelBtn}
                <Button onClick={() => hidePopup?.()}> Cancel</Button>
            </Stacker>
        </Stack>
    );

});

AddOrUpdateColumn.propTypes = {
    tbl_ui_id: PropTypes.string,
    tbl_id: PropTypes.string,
    hidePopup: PropTypes.func,
    editColName: PropTypes.string
};

function clearColumnInfo(tbl_ui_id, request, editColName, newColName) {

    const add = newColName && !editColName;

    let {scrollLeft} = getTableUiById(tbl_ui_id);

    if (add) {
        // for now, we always add to the end
        scrollLeft = 100000;    // scroll to the right-most of the table so the added column is visible
    } else {
        // when update or delete a column; clear sort, filter, or inclCols if they contains this column
        const {sortInfo, filters, inclCols, sqlFilter} = request;

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
        if (inclCols) {
            const cols = inclCols.split(',');
            if (cols?.includes(editColName)) {
                request.inclCols = cols.filter((c) => c !== editColName).join();
            }
        }
    }

    dispatchTableUiUpdate( {tbl_ui_id, columns:undefined, columnWidths:undefined, scrollLeft});        // clear column infos
    dispatchTableFetch(request);
}

export const AddColumnBtn = ({tbl_ui_id, tbl_id}) => (
    <AddColumnButton onClick={() => showAddOrUpdateColumn({tbl_ui_id, tbl_id})}/>
);

export function showAddOrUpdateColumn({tbl_ui_id, tbl_id, editColName, onChange}) {
    const hidePopup = () => popup?.();
    const title =  editColName ? 'Edit a derived column' : 'Add a column';

    const popup = showPopup({ID:'addOrUpdateColumn', content: <AddOrUpdateColumn {...{tbl_id, tbl_ui_id, hidePopup, editColName, onChange}}/>, title, modal: true});
}

function getEditColInfo(tbl_id, editColName) {
    const col = getColumn(getTblById(tbl_id), editColName) || {};
    const desc = col?.desc?.replace(/\(DERIVED_FROM=.+\) /, '');
    const derivedFrom = col?.desc?.match(/\(DERIVED_FROM=(.+)\).*/)?.[1];
    const preset = derivedFrom?.match(/preset:(.+)/)?.[1];
    const expression = preset ? '' : derivedFrom;
    return {col,desc,preset,expression};
}

function DescField ({desc}) {
    return (
        <ValidationField fieldKey='desc' label='Description:'
                         orientation='horizontal'
                         initialState={{value:desc}}
                         tooltip='A one-line description for the column heading tooltip and table options'/>
    );
}

function PresetFields ({tbl_id, editColName}) {
    const {preset, desc} = getEditColInfo(tbl_id, editColName);

    return (
        <>
            <ListBoxInputField fieldKey='preset' label='Select a preset:' orientation='horizontal'
                               options={[
                                   {value:'filtered', label:"Set filtered rows to 'true' and the rest to 'false'"},
                                   {value:'selected', label:"Set selected rows to 'true' and the rest to 'false'"},
                                   {value:'ROW_NUM', label:'Number rows in current sort order'}]}
                               initialState={{value: preset}}/>
            <DescField {...{desc}}/>
        </>
    );
}

function CustomFields({tbl_ui_id, tbl_id, groupKey, editColName}) {

    const dtype = useStoreConnector(() => getFieldVal(groupKey, 'dtype', 'double'));
    const exprKey = 'expression';
    const cols = getColumns(getTblById(tbl_id));

    const onChange = ({sql}) => {
        dispatchValueChange({fieldKey: exprKey, groupKey, value:sql, valid: true});
        hideExpPopup?.();
    };
    const {col={}, desc} = getEditColInfo(tbl_id, editColName);
    if (col.type === 'boolean') col.type = 'double';        // when switching over from boolean type.

    return (
        <>
            <ColumnFld fieldKey={exprKey} name='Expression' cols={cols} label='Expression:' required={true} initValue={col.DERIVED_FROM}
                       slotProps={{
                           control:{orientation:'horizontal'},
                           tooltip:{placement: 'bottom'},
                       }}
                       canBeExpression={true} nullAllowed={false} validator={textValidator({min:1})}
                       helper={<Helper {...{tbl_ui_id, tbl_id, onChange}}/>} tooltip={EXPRESSION_TTIPS}
            />
            <Stack direction='row' spacing={3} alignItems='center'>
                <ListBoxInputField fieldKey='dtype' label='Data Type:'
                                   options={[{value:'double'}, {value:'long'}, {value:'char'}]}
                                   initialState={{value: col.type}}
                />
                {dtype === 'double' &&
                <ValidationField fieldKey='precision' label='Precision:'
                                 sx={{
                                     label:{width:60},
                                     input:{width: 100}
                                 }}
                                 orientation='horizontal'
                                 placeholder='e.g. F6'
                                 initialState={{ value: col.precision, validator:textValidator({pattern: /^$|^[FE]?[1-9]$/i,
                                         message:'Precision must be Fn or En. When Fn, n is the number of digits after the decimal.  ' +
                                             'And when En, n is the precision in scientific notation'
                                     })}}
                />}
            </Stack>
            <ValidationField fieldKey='units' label='Units:'
                             slotProps={{
                                 input:{endDecorator: <Info url='https://ivoa.net/documents/VOUnits'/>}
                             }}
                             orientation='horizontal'
                             initialState={{value: col.units}}
                             tooltip='Units of measurement, IVOA VOUnits preferred'
            />
            <SuggestBoxInputField fieldKey='ucd' label='UCD:'
                              slotProps={{
                                  control:{orientation:'horizontal'},
                                  tooltip: {placement: 'bottom'},
                                  input:{endDecorator: <Info url='https://ivoa.net/documents/UCD1+'/>}
                              }}
                              initialState={{value: col.UCD}}
                              tooltip='IVOA Unified Content Descriptor, UCD1+ style'
                              getSuggestions={getSuggestions} valueOnSuggestion={valueOnSuggestion}
            />
            <DescField desc={desc}/>
        </>
    );
}

function Helper({tbl_ui_id, tbl_id, onChange}) {
    const content = (
        <Box height={500} width={700} position='relative'>
            <SqlTableFilter inputLabel='Expression' placeholder='e.g. w3mpro - w4mpro'
                            usages={<Usages/>} samples={<Samples/>} {...{tbl_ui_id, tbl_id, onChange}}
            />
        </Box>

    );
    const onClick = () => hideExpPopup = showPopup({ID:'ExpressionCreator', title: 'Expression Creator', content});
    return (
        <ToolbarButton icon={MAGNIFYING_GLASS} tip='Add a new column' style={{height: 14}} onClick={onClick}/>
    );
}

const Usages = () => {
    return (
        <Sheet>
            <Typography level='title-md'>Usage</Typography>
            <Typography level='body-sm' sx={{code:{ml:1, whiteSpace:'nowrap'}}}>
                Input should follow the syntax of an SQL expression. <br/>
                Click on a Column name to insert the name into the Expression input box. <br/>
                Standard SQL-like operators can be used where applicable. <br/>
                Supported operators are: <br/>
                <code>{'  +, -, *, /, =, >, <, >=, <=, !=, LIKE, IN, IS NULL, IS NOT NULL'}</code> <br/><br/>

                You may use functions as well.  A few of the common functions are listed below. <br/>
                For a list of all available functions, click <Link href={DOC_FUNCTIONS_URL} target='_blank'>here</Link> <br/>
                String functions: <br/>
                <code>CONCAT(s1,s2[,...]]) SUBSTR(s,offset,length)</code> <br/>
                Numeric functions: <br/>
                <code>LOG10(x) LN(x) DEGREES(x) COS(x) POWER(x,y)</code>
            </Typography>
        </Sheet>
    );
};

const Samples = () => {
    return (
        <Sheet>
            <Typography level='title-md'>Sample Expressions</Typography>
            <Typography level='body-sm' sx={{code:{ml:1, whiteSpace:'nowrap'}}}>
                <code>{'"w3mpro" - "w4mpro"'}</code> <br/>
                <code>{'sqrt(power("w3sigmpro",2) + power("w4sigmpro",2))'}</code> <br/>
                <code>{'("ra"-82.0158188)*cos(radians("dec"))'}</code> <br/>
                <code>{'"phot_g_mean_mag"-(5*log10(1000/"parallax") - 5)'}</code>
            </Typography>
        </Sheet>
    );
};

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

function Info({url, target='info'}) {
    return <Link href={url} target={target} tabIndex={-1} startDecorator={<InfoOutlinedIcon  sx={{height: 16}}/>}/>;
}