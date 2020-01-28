/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {get, cloneDeep} from 'lodash';

import {SqlTableFilter, setSqlFilter} from './FilterEditor.jsx';
import {InputField} from '../../ui/InputField.jsx';
import {intValidator} from '../../util/Validate.js';
import {getTableUiById, isClientTable} from '../TableUtil.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {dispatchTableAddLocal, dispatchTableRemove, dispatchTableUpdate} from '../TablesCntlr.js';
import {MetaInfo} from './TablePanel.jsx';
import {COL_TYPE, getColumnIdx, getTblById, isOfType, parsePrecision} from '../TableUtil.js';
import {TablePanel} from './TablePanel';
import {FILTER_CONDITION_TTIPS, FilterInfo} from '../FilterInfo';
import {checkboxColumnRenderer, inputColumnRenderer} from './TableRenderer.js';
import {dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {POPUP_DIALOG_ID} from '../../ui/PopupUtil.jsx';
import {StatefulTabs, Tab} from '../../ui/panel/TabPanel.jsx';

const labelStyle = {display: 'inline-block', whiteSpace: 'nowrap', width: 50};




export const TablePanelOptions = React.memo(({tbl_ui_id, tbl_id, onChange, onOptionReset}) => {

    const [uiState] = useStoreConnector(() => getTableUiById(tbl_ui_id));
    const ctm_tbl_id = `${tbl_ui_id}-columnOptions`;
    const showAdvFilter = !isClientTable(tbl_id);

    return (
        <div className='TablePanelOptions'>
            <Options {...{uiState, tbl_id, tbl_ui_id, ctm_tbl_id, onOptionReset, onChange}} />
            <StatefulTabs componentKey='TablePanelOptions' defaultSelected={0} borderless={true} useFlex={true} style={{flex: '1 1 0'}}>
                <Tab name='Column Options'>
                    <ColumnOptions {...{tbl_id, tbl_ui_id, ctm_tbl_id, onChange}} />
                </Tab>
                {showAdvFilter &&
                    <Tab name='Advanced Filter'>
                        <div style={{display: 'flex', flex: '1 1 0', position: 'relative'}}>
                            <SqlTableFilter {...{tbl_id, tbl_ui_id, onChange}} />
                        </div>
                    </Tab>
                }
                <Tab name='Meta'>
                    <div style={{width: '100%', height: '100%', overflow: 'auto'}}>
                        <div style={{margin: '10px 10px 0px'}}>Additional table related information will appear below, if any.  Click on header to expand or collapse a section.</div>
                        <MetaInfo tbl_id={tbl_id} isOpen={true} style={{ width: '100%', border: 'none', margin: 'unset', padding: 'unset'}} />
                    </div>
                </Tab>
            </StatefulTabs>
        </div>
    );
});

TablePanelOptions.propTypes = {
    tbl_ui_id: PropTypes.string,
    tbl_id: PropTypes.string,
    onChange: PropTypes.func,
    onOptionReset: PropTypes.func
};



function Options({uiState, tbl_id, tbl_ui_id, ctm_tbl_id, onOptionReset, onChange}) {
    const {pageSize, showPaging=true, showUnits=false, allowUnits=true,showTypes=true, showFilters=false} = uiState || {};

    const onPageSize = (pageSize) => {
        if (pageSize.valid) {
            onChange && onChange({pageSize: pageSize.value});
        }
    };

    const onReset = () => {
        if (onOptionReset) {
            onOptionReset();
            const ctm = createColumnTableModel(tbl_id, tbl_ui_id);
            ctm.tbl_id = ctm_tbl_id;
            dispatchTableAddLocal(ctm, undefined, false);
            setSqlFilter('', '');
        }
    };

    const onClose = () => {
        dispatchHideDialog(POPUP_DIALOG_ID);
    };

    return (
        <div style={{display: 'inline-flex', justifyContent: 'space-between', marginBottom: 10}}>
            <div>
                <div style={{...labelStyle, width: 35, fontWeight: 'bold'}}>Show:</div>
                {allowUnits &&
                <div style={labelStyle}>
                    <input type='checkbox'  checked={showUnits} onChange={(e) => onChange({showUnits: e.target.checked})}/>
                    Units
                </div>
                }
                <div style={{...labelStyle, width: 80}}>
                    <input type='checkbox' checked={showTypes} onChange={(e) => onChange({showTypes: e.target.checked})}/>
                    Data Type
                </div>
                <div style={labelStyle}>
                    <input type='checkbox' checked={showFilters} onChange={(e) => onChange({showFilters: e.target.checked})} />
                    Filters
                </div>
            </div>
            <div>
                {showPaging &&
                <InputField
                    validator={intValidator(1,10000)}
                    tooltip='Set page size'
                    label='Page Size:'
                    labelStyle={{...labelStyle, fontWeight: 'bold', width: 60}}
                    size={5}
                    value={pageSize+''}
                    onChange={onPageSize}
                    actOn={['blur','enter']}
                />
                }
            </div>
            <div>
                <button type='button' className='TablePanelOptions__button' onClick={onReset} style={{marginRight: 5}}
                        title='Reset all options to defaults'>Reset</button>
                <button type='button' className='TablePanelOptions__button' onClick={onClose} style={{marginRight: 5}}
                        title='Close this dialog'>Close</button>
            </div>
        </div>
    );
}


const columns = [
    {name: 'name', fixed: true, width: 12},
    {name: 'show', fixed: true, width: 3, resizable: false},
    {name: 'filter', fixed: true, width: 10, sortable: false, filterable: false},
    {name: 'precision', fixed: true, width: 7, sortable: false, filterable: false},
    {name: 'null_str', fixed: true, width: 7, sortable: false, filterable: false},
    {name: 'type'},
    {name: 'units'},
    {name: 'arraySize', width: 7},
    {name: 'utype'},
    {name: 'UCD'},
    {name: 'links'},
    {name: 'description'},
    {name: 'cname', visibility: 'hidden'},
];

const cnameIdx = columns.length-1;
const filterIdx = 2;


export const ColumnOptions = React.memo(({tbl_id, tbl_ui_id, ctm_tbl_id, onChange}) => {

    useEffect(() => {
        let  ctm = getTblById(ctm_tbl_id);
        if (!ctm) {
            ctm = createColumnTableModel(tbl_id, tbl_ui_id);
            ctm.tbl_id = ctm_tbl_id;
            dispatchTableAddLocal(ctm, undefined, false);
        }
        return () => {
            dispatchTableRemove(ctm_tbl_id, false);
        };

    }, [tbl_ui_id, tbl_id, ctm_tbl_id]);     // run only if table changes

    // filters state are kept in tablemodel
    // the rest of the state are kept in the source table ui data
    const renderers =  {
        filter:     {cellRenderer: makeFilterRenderer(tbl_id, ctm_tbl_id, onChange)},
        precision:  {cellRenderer: makePrecisionRenderer(tbl_ui_id, ctm_tbl_id, onChange)},
        show:       {cellRenderer: makeShowRenderer(tbl_ui_id, ctm_tbl_id, onChange), headRenderer: makeShowHeaderRenderer(tbl_ui_id, ctm_tbl_id, onChange)},
        null_str:   {cellRenderer: makeNullStringRenderer(tbl_ui_id, ctm_tbl_id, onChange)}
    };
    return (
        <div style={{flex: '1 1 0'}}>
            <TablePanel
                border={false}
                tbl_ui_id = {tbl_ui_id + '-columnOptions'}
                tbl_id = {ctm_tbl_id}
                renderers = {renderers}
                showToolbar = {false}
                showOptionButton = {false}
                selectable = {false}
                rowHeight = {24}
                highlightedRowHandler = {()=>undefined}
            />
        </div>
    );
});

function createColumnTableModel(tbl_id, tbl_ui_id) {
    const filterInfoCls = FilterInfo.parse(get(getTblById(tbl_id), 'request.filters'));
    const data = get(getTableUiById(tbl_ui_id), 'columns', [])
        .filter((c) => c.visibility !== 'hidden')
        .map( (c) => [
            c.label || c.name || '',
            !c.visibility || c.visibility === 'show',
            filterInfoCls.getFilter(c.name) || '',
            c.precision || '',
            c.nullString || '',
            c.type || '',
            c.units || '',
            c.arraySize || '',
            c.utype || '',
            c.UCD || '',
            c.links || '',
            c.desc || '',
            c.name || ''
        ]);
    return {tableData: {columns, data}, totalRows: data.length};
}


/* -------------------------------------- custom table cell renderers ------------------------------------------------------------*/
const cellStyle = {backgroundColor: 'white'};

function getActiveInput(data, rowIdx, tbl_ui_id) {
    const selColName = get(data, [rowIdx, cnameIdx]);
    const nColumns = cloneDeep(get(getTableUiById(tbl_ui_id), 'columns', []));
    const selCol = nColumns.find((col) => col.name === selColName);
    return {selColName, nColumns, selCol};
}

function makeShowRenderer(tbl_ui_id, ctm_tbl_id, onChange) {

    const onCheckBoxSel = (checked, rowIdx,  data) => {
        const {nColumns, selCol} = getActiveInput(data, rowIdx, tbl_ui_id);
        selCol && (selCol.visibility = checked ? 'show' : 'hide');
        onChange && onChange({columns: nColumns});
    };

    return checkboxColumnRenderer({tbl_id:ctm_tbl_id, cname: 'show', onChange: onCheckBoxSel, style: cellStyle});
}

function makeShowHeaderRenderer(tbl_ui_id, ctm_tbl_id, onChange) {
    const onSelectAll = (checked) => {
        const nColumns = cloneDeep(get(getTableUiById(tbl_ui_id), 'columns', []));
        nColumns.filter( (c) => c.visibility !== 'hidden')
            .forEach((c) => c.visibility = checked ? 'show' : 'hide');
        onChange && onChange({columns: nColumns});
        // requires update to the ctm table
        const ctm_table = cloneDeep(getTblById(ctm_tbl_id));
        const showIdx = getColumnIdx(ctm_table, 'show');
        get(ctm_table, 'tableData.data',[])
            .forEach( (row) => row[showIdx] = checked ? true : false);
        get(ctm_table, 'origTableModel.tableData.data',[])
            .forEach( (row) => row[showIdx] = checked ? true : false);
        dispatchTableUpdate(ctm_table);
    };

    return () => {
        const selectAll = get(getTableUiById(tbl_ui_id), 'columns', [])
            .filter((c) => c.visibility === 'hide').length === 0;    // true when no columns are set to 'hide'
        return (
            <div className='TablePanel__header' style={cellStyle}>
                <div>show</div>
                <input type = 'checkbox'
                       title = 'Unselect checkbox to hide this column from the table UI'
                       onChange = {(e) => onSelectAll(e.target.checked)}
                       checked = {selectAll}/>
            </div>
        );
    };
}

function makePrecisionRenderer(tbl_ui_id, ctm_tbl_id, onChange) {
    const tooltips = 'A string Tn where T is either F, E, G, HMS, or DMS\n' +
        ' When T is F, E, HMS, or DMS, n is the number of significant figures after the decimal point.\n' +
        ' When T is G, n is the number of significant digits';

    const onPrecision = (val, rowIdx,  data) => {
        const {nColumns, selCol} = getActiveInput(data, rowIdx, tbl_ui_id);
        if (selCol) {
            selCol.precision = val;             // set new precision value and clear all other format settings.
            selCol.format = undefined;
            selCol.fmtDisp = undefined;
        }
        onChange && onChange({columns: nColumns});
    };

    const isReadOnly = (rowIndex, data) => {
        return !isOfType(data[rowIndex][5],COL_TYPE.FLOAT);
    };

    const validator = (value) => {
        const [type] = parsePrecision(value);
        const valid = !!(!value || type);
        return {valid, value, message: tooltips};
    };

    return inputColumnRenderer({tbl_id:ctm_tbl_id, cname: 'precision', validator, isReadOnly, onChange:onPrecision, style: cellStyle, tooltips});
}


function makeFilterRenderer(tbl_id, ctm_tbl_id, onChange) {

    const collectFilterInfo = (data=[]) => {
        const filterCls = FilterInfo.parse('');
        data.filter( (row) => row[filterIdx])
            .forEach( (row) => {
                filterCls.setFilter(row[cnameIdx], row[filterIdx]);
            });

        return filterCls.serialize();
    };

    const onFilter = (value, rowIndex, data) => {
        const filterInfo = collectFilterInfo(data);
        const prevFilterInfo = get(getTblById(tbl_id), 'request.filters');
        if (filterInfo !== prevFilterInfo) {
            onChange && onChange({filterInfo});
        }
    };

    const validator = (cond, data, rowIndex) => {
        const cname = get(data, [rowIndex, cnameIdx]);
        return FilterInfo.conditionValidator(cond, tbl_id, cname);
    };

    return inputColumnRenderer({tbl_id:ctm_tbl_id, cname: 'filter', validator, onChange:onFilter, style:cellStyle, tooltips:FILTER_CONDITION_TTIPS});
}

function makeNullStringRenderer(tbl_ui_id, ctm_tbl_id, onChange) {
    const tooltips = 'The Null Values define the data values that will indicate no data, for example "null" or "9999.99"';

    const onNullString = (val, rowIdx,  data) => {
        const {nColumns, selCol} = getActiveInput(data, rowIdx, tbl_ui_id);
        selCol && (selCol.nullString = val);
        onChange && onChange({columns: nColumns});
    };

    return  inputColumnRenderer({tbl_id:ctm_tbl_id, cname: 'null_str', onChange:onNullString, style: cellStyle, tooltips});
}

