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
import {dispatchTableAddLocal, dispatchTableRemove, TABLE_SELECT, dispatchTableFilter} from '../TablesCntlr.js';
import {MetaInfo} from './TablePanel.jsx';
import {COL_TYPE,  getTblById, isOfType, parsePrecision, getColumnValues, watchTableChanges, getFilterCount, getColumn, hasAuxData} from '../TableUtil.js';
import {TablePanel} from './TablePanel';
import {FILTER_CONDITION_TTIPS, FilterInfo} from '../FilterInfo';
import {inputColumnRenderer} from './TableRenderer.js';
import {dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {POPUP_DIALOG_ID} from '../../ui/PopupUtil.jsx';
import {StatefulTabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {SelectInfo} from '../SelectInfo.js';
import {makeBadge} from '../../ui/ToolbarButton.jsx';
import {getSqlFilter} from '../TableUtil';

export const TablePanelOptions = React.memo(({tbl_ui_id, tbl_id, onChange, onOptionReset}) => {

    const [uiState] = useStoreConnector(() => getTableUiById(tbl_ui_id));
    const ctm_tbl_id = `${tbl_ui_id}-columnOptions`;
    const showAdvFilter = !isClientTable(tbl_id);

    const {sql=''} = getSqlFilter(tbl_id);
    const advFilterName = 'Advanced Filter';
    const label = () => {
        if (!sql) return advFilterName;
        return (
            <div style={{display: 'inline-flex'}}>
                <div style={{marginRight: 10}}>{advFilterName}</div>
                <div title='Advanced filter applied.  Click on tab to view details.'
                    style={{
                        position: 'absolute',
                        right: 2,
                        borderLeft: '10px solid transparent',
                        borderTop: '10px solid rgb(71, 138, 163)',
                        position: 'absolute'
                    }}/>
            </div>
        );
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
        <div className='TablePanelOptions'>
            <Options {...{uiState, tbl_id, tbl_ui_id, ctm_tbl_id, onOptionReset, onChange}} />
            <OptionsFilterStats tbl_id={ctm_tbl_id}/>
            <StatefulTabs componentKey='TablePanelOptions' defaultSelected={0} borderless={true} useFlex={true} style={{flex: '1 1 0'}}>
                <Tab name='Column Options'>
                    <ColumnOptions {...{tbl_id, tbl_ui_id, ctm_tbl_id, onChange}} />
                </Tab>
                {showAdvFilter &&
                    <Tab name={advFilterName} label={label()}>
                        <div style={{display: 'flex', flex: '1 1 0', position: 'relative'}}>
                            <SqlTableFilter {...{tbl_id, tbl_ui_id, onChange}} />
                        </div>
                    </Tab>
                }
                <Tab name='Table Meta'>
                    <div style={{width: '100%', height: '100%', overflow: 'auto'}}>
                        <MetaContent tbl_id={tbl_id}/>
                    </div>
                </Tab>
            </StatefulTabs>
            <div style={{margin: '5px 15px 0 0'}}>
                <button type='button' className='button std' style={{marginRight: 5}}
                        onClick={onReset}>Reset
                </button>
                <button type='button' className='button std'
                        onClick={onClose}>Close
                </button>
            </div>
        </div>
    );
});

TablePanelOptions.propTypes = {
    tbl_ui_id: PropTypes.string,
    tbl_id: PropTypes.string,
    onChange: PropTypes.func,
    onOptionReset: PropTypes.func
};

function MetaContent({tbl_id}) {
    if (hasAuxData(tbl_id)) {
        return <MetaInfo tbl_id={tbl_id} isOpen={true} style={{ width: '100%', border: 'none', margin: 'unset', padding: 'unset'}} />;
    } else {
        return <div style={{margin: 20, fontWeight: 'bold'}}>No metadata available</div>;
    }
}

function OptionsFilterStats({tbl_id}) {

    const [filterCnt] = useStoreConnector(() => getFilterCount(getTblById(tbl_id)));
    const filterStr = filterCnt === 0 ? '' : filterCnt === 1 ? '1 filter' : `${filterCnt} filters`;
    const clearFilters = () => dispatchTableFilter({tbl_id, filters: ''});;

    if (filterCnt === 0) return null;
    return (
        <div style={{ position: 'absolute', top: 30, zIndex: 1, right: 5}}>
            <button onClick={clearFilters}> remove {filterStr}</button>
        </div>
    );
}

function Options({uiState, tbl_id, tbl_ui_id, ctm_tbl_id, onOptionReset, onChange}) {
    const {pageSize, showPaging=true, showUnits=false, allowUnits=true,showTypes=true, showFilters=false} = uiState || {};

    const onPageSize = (pageSize) => {
        if (pageSize.valid) {
            onChange && onChange({pageSize: pageSize.value});
        }
    };

    const labelStyle = {display: 'inline-block', whiteSpace: 'nowrap', width: 50};

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
        </div>
    );
}


const columns = [
    {name: 'name', fixed: true, width: 12},
    {name: 'filter', fixed: true, width: 10, sortable: false, filterable: false},
    {name: 'format'},
    {name: 'null_string'},
    {name: 'type'},
    {name: 'units'},
    {name: 'arraySize', width: 7},
    {name: 'utype'},
    {name: 'UCD'},
    {name: 'links'},
    {name: 'description'},
    {name: 'cname', visibility: 'hidden'},
    {name: 'show', visibility: 'hidden'},
];

const showIdx = columns.length-1;
const cnameIdx = columns.length-2;
const filterIdx = 1;
const typeIdx = 4;


export const ColumnOptions = React.memo(({tbl_id, tbl_ui_id, ctm_tbl_id, onChange}) => {

    useEffect(() => {
        let  ctm = getTblById(ctm_tbl_id);
        if (!ctm) {
            ctm = createColumnTableModel(tbl_id, tbl_ui_id);
            ctm.tbl_id = ctm_tbl_id;
            dispatchTableAddLocal(ctm, undefined, false);
        }

        watchTableChanges(ctm_tbl_id, [TABLE_SELECT], ({payload}) => {
            const {selectInfo} = payload || {};
            const selectInfoCls = SelectInfo.newInstance(selectInfo);
            const cnames = getColumnValues(getTblById(ctm_tbl_id), 'cname');
            const nColumns = cloneDeep(get(getTableUiById(tbl_ui_id), 'columns', []));
            nColumns.forEach((c) => {
                const ridx = cnames.findIndex((cn) => cn === c.name);
                if (ridx >= 0) {
                    c.visibility = selectInfoCls.isSelected(ridx) ? 'show' : 'hide';
                }
            });
            onChange({columns: nColumns});
        });

        return () => {
            dispatchTableRemove(ctm_tbl_id, false);
        };

    }, [tbl_ui_id, tbl_id, ctm_tbl_id]);     // run only if table changes

    // filters state are kept in tablemodel
    // the rest of the state are kept in the source table ui data
    const renderers =  {
        filter:     {cellRenderer: makeFilterRenderer(tbl_id, ctm_tbl_id, onChange)},
        // format:  {cellRenderer: makePrecisionRenderer(tbl_ui_id, ctm_tbl_id, onChange)},
        // null_string:   {cellRenderer: makeNullStringRenderer(tbl_ui_id, ctm_tbl_id, onChange)}
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
                showFilters={true}
                selectable = {true}
                rowHeight = {24}
                highlightedRowHandler = {()=>undefined}
            />
        </div>
    );
});

function createColumnTableModel(tbl_id, tbl_ui_id) {
    const filterInfoCls = FilterInfo.parse(get(getTblById(tbl_id), 'request.filters'));
    const data =  get(getTableUiById(tbl_ui_id), 'columns', [])
        .filter((c) => c.visibility !== 'hidden')
        .map( (c) => [
            c.label || c.name || '',
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
            c.name || '',
            !c.visibility || c.visibility === 'show'
        ]);
    const selectInfoCls = SelectInfo.newInstance({rowCount: data.length});
    for(let idx = 0; idx < data.length; idx++) {
        selectInfoCls.setRowSelect(idx, get(data,[idx, showIdx]));
    }
    const ctm = {selectInfo: selectInfoCls.data,  tableData: {columns, data}, totalRows: data.length};

    // hide empty columns
    ['format', 'null_string', 'arraySize', 'utype', 'UCD', 'links', 'description'].forEach((cname) => {
        const cl = getColumnValues(ctm, cname).filter((v) => v);
        if ( cl.length === 0 ) {
            getColumn(ctm, cname).visibility = 'hidden';
        }
    });
    return ctm;
}


/* -------------------------------------- custom table cell renderers ------------------------------------------------------------*/
const cellStyle = {backgroundColor: 'white'};

function getActiveInput(data, rowIdx, tbl_ui_id) {
    const selColName = get(data, [rowIdx, cnameIdx]);
    const nColumns = cloneDeep(get(getTableUiById(tbl_ui_id), 'columns', []));
    const selCol = nColumns.find((col) => col.name === selColName);
    return {selColName, nColumns, selCol};
}

function makePrecisionRenderer(tbl_ui_id, ctm_tbl_id, onChange) {
    const tooltips = 'A string Tn where T is either F, E, G, HMS, or DMS\n' +
        '- When T is F or E, n is the number of significant figures after the decimal point\n' +
        '- When T is G, n is the number of significant digits\n' +
        '- When T is HMS or DMS, n is ignored\n' +
        'e.g. E5, F4, G6, or HMS';

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
        return !isOfType(data[rowIndex][typeIdx],COL_TYPE.FLOAT);
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

    return  inputColumnRenderer({tbl_id:ctm_tbl_id, cname: 'null_string', onChange:onNullString, style: cellStyle, tooltips});
}

