import React from 'react';
import {uniqueId,difference} from 'lodash';
import {createInputCell} from '../../tables/ui/TableRenderer.js';
import {FILTER_CONDITION_TTIPS, FilterInfo, parseInput} from '../../tables/FilterInfo.js';
import {getColumnIdx, getTblById} from '../../tables/TableUtil.js';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {maybeQuote} from 'firefly/ui/tap/TapUtil.js';
import {crunch} from 'firefly/util/WebUtil.js';

/**
 * @summary component to display column constraints and selections using a table
 * Constraints table is represented by a client table.
 * Its original table model holds all constraints and selections.
 * @param {Object} props
 * @param props.tableModel
 * @param props.style
 * @returns {Object} constraint table
 */
export function ColumnConstraintsPanel({tableModel}) {
    const onTableChanged = () => {
        const tbl = getTblById(tableModel.tbl_id);
        if (!tbl) return;
        mergeConstraintsIntoOrig(tbl);
    };

    const newInputCell = createInputCell(FILTER_CONDITION_TTIPS,
                15,
                FilterInfo.conditionValidatorNoAutoCorrect,
                onTableChanged, {width: '100%', boxSizing: 'border-box'});

    return (
        <TablePanel
            key={uniqueId()} tbl_ui_id={tableModel.tbl_id + '-ui'}
            sx={{borderRadius:5}}
            tableModel={tableModel}
            showToolbar={false} showFilters={true} rowHeight={26}
            renderers={{ constraints: { cellRenderer: newInputCell} }}
        />
    );
}

const COND_SEP = new RegExp('( and | or )', 'i');
const STRING_SEP= /('.*?'|".*?"|\S+)/g;
const splitStr= (s) => s.split(STRING_SEP).filter((s) => s.trim());
const startsWithAny= (str, startAry) => startAry.find( (s) => str.startsWith(s));

function isConditionValid(conditions) {
    if (!conditions) return true;
    const cc= crunch(conditions);
    const parts = cc.split(COND_SEP);
    const anyBad= parts.find( (s) => !validSingleCondition(s) );
    if (anyBad) return false;
    for (let i = 0; i < parts.length; i += 2) {
        const [cname, op, val] = parseInput(parts[i]);
        if (cname || !op || !val) return false;
    }
    return true;
}

function validSingleCondition(s) {
    const sUp= s.trim().toUpperCase();
    if (sUp.startsWith('LIKE ')) return validParam(sUp.substr(4));
    if (sUp.startsWith('IN')) return sUp.match(/^IN\s*\(.*\)$/);
    if (startsWithAny(sUp,['=','>','<',])) return validParam(sUp.substr(1));
    if (startsWithAny(sUp,['!=','>=','<='])) return validParam(sUp.substr(2));
    return !Boolean(sUp.match(/( [A-Za-z]\S* )/));
}

function validParam(param) {
    const p= param.trim();
    if (splitStr(p).length===1) return true;
    return (p.match(/^[A-Za-z]{2,}\s*\(.*\)$/));
}

export function getTableConstraints(tbl_id) {
    const tbl = getTblById(tbl_id)?.origTableModel;

    const tbl_data = tbl?.tableData?.data;
    if (!tbl_data) return;

    const sel_info  =  tbl.selectInfo;
    const filters = {};
    let whereFragment = '';
    let errors = '';

    tbl_data
        .filter( ([,fStr]) => fStr?.trim())
        .forEach(([colName, fStr]) => {
            const filterStrings = crunch(fStr);
            filters[colName] = filterStrings;
            if (isConditionValid(filterStrings)) {
                const condAry = filterStrings.split(COND_SEP).map( (s) => s.trim());
                const qCol= maybeQuote(colName);
                const constraint= condAry.reduce((full,part) => {
                    const pUpper= part.toUpperCase();
                    if (!full) return `${qCol} ${part}`;
                    return (pUpper==='AND' || pUpper==='OR') ? `${full} ${pUpper}` : `${full} ${qCol} ${part}`;
                },'');
                const oneConstraint= condAry>1 ? `(${constraint})` : constraint;
                whereFragment += (whereFragment.length > 0 ? `\n           AND ${oneConstraint}` : oneConstraint);
            }
            else {
                errors += (errors.length > 0 ? `, "${filterStrings}"` : `Invalid constraints: "${filterStrings}"`);
            }
        });

    // collect the names of all selected columns
    let allSelected = true;
    const selcolsArray = [];
    let selcolsFragment = tbl_data.reduce((prev, [colName], idx) => {
            if ((sel_info.selectAll && (!sel_info.exceptions.has(idx))) ||
                (!sel_info.selectAll && (sel_info.exceptions.has(idx)))) {
                prev += maybeQuote(colName) + ',';
                selcolsArray.push(colName);
            } else {
                allSelected = false;
            }
            return prev;
        }, '');

    if (!selcolsFragment || allSelected) selcolsFragment = '';
    return {whereFragment, selcolsFragment, errors, filters, selcolsArray};
}

/**
 * Merge constraints into original table model to preserve them through sorts and filters
 * @param tbl client side table model
 */
function mergeConstraintsIntoOrig(tbl) {
    const {origTableModel} = tbl;
    const tblData = tbl?.tableData?.data;

    if (!origTableModel || !tblData) return;

    // if ROW_IDX column is present in the table, its row indexes differ from the original table
    const idxCol = getColumnIdx(tbl, 'ROW_IDX');
    const origTblData = origTableModel?.tableData?.data;
    tblData.forEach((row, i) => {
        const origIdx = idxCol < 0 ? i : parseInt(row[idxCol]);
        //set column constraint
        origTblData[origIdx][1] = tblData[i][1];
    });
}