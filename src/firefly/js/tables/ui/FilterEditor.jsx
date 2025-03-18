/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect, useRef} from 'react';
import PropTypes from 'prop-types';
import {Box, Button, Link, Sheet, Stack, Typography} from '@mui/joy';
import {cloneDeep, get} from 'lodash';
import SplitPane from 'react-split-pane';
import Tree from 'rc-tree';
import 'rc-tree/assets/index.css';

import {FilterInfo, getFiltersAsSql} from '../FilterInfo.js';
import {getTableUiById, getSqlFilter, DOC_FUNCTIONS_URL} from '../TableUtil.js';
import {SplitContent} from '../../ui/panel/DockLayoutPanel.jsx';
import {InputAreaFieldConnected} from '../../ui/InputAreaField.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {insertAtCursor} from '../../ui/tap/AdvancedADQL.jsx';
import {dispatchMultiValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {CopyToClipboard} from '../../visualize/ui/MouseReadout';

import RIGHT_ARROW from 'html/images/right-arrow-in-16x16.png';
import {applyFilterChanges} from 'firefly/tables/TableConnector';
import {makeConnector} from '../TableConnector';
import {TablePanelOptions} from './TablePanelOptions';

/*----------------------------------------------  Filter only TablePanelOptions -----------------------------------------------*/
export function TableFilterPopup({tbl_id, tbl_ui_id}) {
    const connector = makeConnector(tbl_id);
    const clearFilter = () => connector.applyFilterChanges({filterInfo: '', sqlFilter: ''});
    return (
        <Stack height={450} width={650} overflow='hidden' position='relative' sx={{resize:'both', minWidth:550, minHeight:200}}>
            <Stack position='absolute' sx={{inset:'5px'}}>
                <TablePanelOptions tbl_id={tbl_id} tbl_ui_id={tbl_ui_id}
                                   onChange={connector.onOptionUpdate}
                                   onOptionReset={connector.onOptionReset}
                                   clearFilter={clearFilter}
                                   allowColumnSelection={false}
                                   slotProps={{
                                       header: {component: null},
                                       columnOptTab: {name: 'Basic Column Filter'},
                                   }}
                />
            </Stack>
        </Stack>
    );
}

/*----------------------------------------------  Advanced Filter panel -----------------------------------------------*/

export const code = {style: {color: 'green', whiteSpace: 'pre', fontFamily: 'monospace', display: 'inline-block'}};
const sqlKey = 'SqlTableFilter-sql';
const opKey =  'SqlTableFilter-op';
const groupKey = 'sqltablefilter';

export function setSqlFilter(op, sql) {
    dispatchMultiValueChange(groupKey, [
                            {fieldKey:sqlKey, groupKey, value: sql, valid: true},
                            {fieldKey:opKey, groupKey, value: op, valid: true}
                        ]);
}

export function SqlTableFilter({tbl_ui_id, tbl_id, onChange, style={}, samples, usages, inputLabel, placeholder}) {


    const sqlEl = useRef(null);                                                // using a useRef hook
    const uiState = useStoreConnector(() => getTableUiById(tbl_ui_id));

    useEffect(() => {
        const {op='AND', sql=''} = getSqlFilter(tbl_id);
        setSqlFilter(op, sql);
    }, [tbl_id]);     // run only once

    const {columns, error} = uiState;
    const treeData = cloneDeep(columns)
                        .filter( (c) => c.visibility !== 'hidden')
                        .sort( (c1,c2) => (c1.label || c1.name).localeCompare(c2.label || c2.name) )
                        .map((c) => ({
                            style:{marginLeft: -10},
                            key:c.name,
                            title:`  ${c.label || c.name} (${c.type || '---'})`,
                            isLeaf:true
                        }));

    const onApply = () => {
        const sql = getFieldVal(groupKey, sqlKey);
        const op = getFieldVal(groupKey, opKey);
        onChange?.({op, sql});
    };

    const onNodeClick = (skeys, {node}) => {
        const textArea = document.getElementById('advFilterInput');
        const key = get(node, 'props.eventKey');
        insertAtCursor(textArea, ` "${key}" `, sqlKey, groupKey);
    };

    const colFilters = getFiltersAsSql(tbl_id);
    inputLabel = inputLabel || (colFilters ? 'Additional Constraints (SQL):' : 'Constraints (SQL):');
    const iconGen = () => <img width="14" height="14" src={RIGHT_ARROW}/> ;

    usages = usages || <Usages/>;
    samples = samples || <Samples/>;
    placeholder = placeholder || 'e.g., "ra" > 180 AND "ra" < 185';

    return (
        <SplitPane split='vertical' defaultSize={200} style={{display: 'inline-flex', ...style}}>
            <SplitContent style={{display: 'flex', flexDirection: 'column'}}>
                <Typography level='title-md'>Columns (sorted)</Typography>
                <Box  style={{overflow: 'auto', flexGrow: 1}}>
                    <Tree defaultExpandAll showLine onSelect={onNodeClick} icon={iconGen} treeData={treeData}/>
                </Box>
            </SplitContent>
            <SplitContent style={{overflow: 'auto'}}>
                <Stack height={1} spacing={1} overflow='hidden'>
                    <ColumnFilter {...{colFilters, tbl_id}}/>
                    <Stack direction='row' justifyContent='space-between' alignItems='center'>
                        <Typography level='title-md'>{inputLabel}</Typography>
                        <Stack direction='row' alignItems='center' spacing={1}>
                            <Button title='Apply the constraints' onClick={onApply}>Apply</Button>
                            {colFilters &&
                                <Stack direction='row' alignItems='center' spacing={1}>
                                    <Typography> with: </Typography>
                                    <RadioGroupInputField
                                        fieldKey={opKey}
                                        groupKey={groupKey}
                                        alignment='horizontal'
                                        options={[
                                            {label: 'AND', value: 'AND'},
                                            {label: 'OR', value: 'OR'}
                                        ]}/>
                                </Stack>
                            }
                        </Stack>
                    </Stack>
                    <InputAreaFieldConnected
                        ref={sqlEl}
                        validator={FilterInfo.validator.bind(null,columns)}
                        groupKey={groupKey}
                        fieldKey={sqlKey}
                        tooltip='Additional filter to apply to the table'
                        placeholder={placeholder}
                        slotProps={{
                            textArea: {id: 'advFilterInput'}
                        }}
                    />
                    {error && <li style={{color: 'red', fontStyle: 'italic'}}>{error}</li>}
                    <Stack spacing={1} overflow='auto'>
                        {usages}
                        {samples}
                    </Stack>
                </Stack>
            </SplitContent>
        </SplitPane>
    );
}

SqlTableFilter.propTypes= {
    tbl_ui_id: PropTypes.string,
    tbl_id: PropTypes.string,
    onChange: PropTypes.func,
    onOptionReset: PropTypes.func
};

function ColumnFilter({colFilters, tbl_id}) {
    const clearFilters = () => applyFilterChanges({tbl_id, filterInfo: ''});

    if (colFilters){
        return (
            <Stack>
                <Stack direction='row' spacing={1} alignItems='center'>
                    <Typography level='title-md'>Current Constraints: </Typography>
                    <Link onClick={clearFilters}>Clear</Link>
                    <CopyToClipboard value={colFilters}/>
                </Stack>
                <Sheet variant='soft' title={colFilters}>
                    <Typography color='warning' level='body-sm'><code>{colFilters}</code></Typography>
                </Sheet>
            </Stack>
        );
    } else return null;
}

const Usages = () => {
    return (
        <Sheet>
            <Typography level='title-md'>Usage</Typography>
            <Typography level='body-sm' sx={{code:{ml:1, whiteSpace:'nowrap'}}}>
                Input should follow the syntax of an SQL WHERE clause. <br/>
                Click on a Column name to insert the name into the SQL Filter input box. <br/>
                Standard SQL-like operators can be used where applicable. <br/>
                Supported operators are: <br/>
                <code>{'  +, -, *, /, =, >, <, >=, <=, !=, LIKE, IN, IS NULL, IS NOT NULL'}</code> <br/><br/>

                You may use functions as well.  A few of the common functions are listed below. <br/>
                For a list of all available functions, click <Link href={DOC_FUNCTIONS_URL} target='_blank'>here</Link> <br/>
                String functions: <br/>
                <code>CONCAT(s1,s2[,...]]) INSTR(s,pattern[,offset]) LENGTH(s) SUBSTR(s,offset,length)</code> <br/>
                Numeric functions: <br/>
                <code>LOG10(x)/LG(x) LN(x)/LOG(x) DEGREES(x) ABS(x) COS(x) SIN(x) TAN(x) POWER(x,y)</code>
            </Typography>
        </Sheet>
    );
};

const Samples = () => {
    return (
        <Sheet>
            <Typography level='title-md'>Sample Filters</Typography>
            <Typography level='body-sm' sx={{code:{ml:1, whiteSpace:'nowrap'}}}>
                <code>{'("ra" > 185 AND "ra" < 185.1) OR ("dec" > 15 AND "dec" < 15.1) AND "band" IN (1,2)'}</code> <br/>
                <code>{'POWER("v",2) / POWER("err",2) > 4 AND "band" = 3'}</code>
            </Typography>
        </Sheet>
    );
};