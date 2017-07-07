import React from 'react';
import PropTypes from 'prop-types';
import {get, has} from 'lodash';
import {LC} from './LcManager.js';
import {getConverter} from './LcConverterFactory.js';
import {getCellValue, getTblById, findIndex} from '../../tables/TableUtil.js';
import {dispatchTableHighlight} from '../../tables/TablesCntlr.js';

export function getTypeData(key, val='', tip = '', labelV='', labelW) {
    return {
        fieldKey: key,
        label: labelV,
        value: val,
        tooltip: tip,
        labelWidth: labelW
    };
}

/**
 * Returns only numerical column names form raw lc table
 * @param {Object} rawTbl
 * @returns {TableColumn[]} - array of table columns objects
 */
export function getOnlyNumericalCol(rawTbl) {

    var colType = ['double', 'd', 'long', 'l', 'int', 'i', 'float', 'f'];
    return get(rawTbl, ['tableData', 'columns']).reduce((prev, col) => {
        if ((colType.includes(col.type)) &&
            (!has(col, 'visibility') || get(col, 'visibility') !== 'hidden')) {
            prev.push(col.name);
        }
        return prev;
    }, []);
}

export var ReadOnlyText = ({label, content, labelWidth, wrapperStyle}) => {
    return (
        <div style={{display: 'flex',...wrapperStyle}}>
            <div style={{width: labelWidth, paddingRight: 4}}> {label} </div>
            <div> {content} </div>
        </div>
    );
};

ReadOnlyText.propTypes = {
    label: PropTypes.string,
    content: PropTypes.string,
    labelWidth: PropTypes.number,
    wrapperStyle: PropTypes.object
};


export function makeMissionEntries(tableMeta, layoutInfo={}) {
    var converterData = getConverter(get(tableMeta, LC.META_MISSION)) || getConverter();
    var missionEntries = layoutInfo.missionEntries || {};
    Object.assign(missionEntries, {
        [LC.META_MISSION]: converterData.converterId,
        [LC.META_TIME_CNAME]: get(tableMeta, LC.META_TIME_CNAME, converterData.defaultTimeCName),
        [LC.META_FLUX_CNAME]: get(tableMeta, LC.META_FLUX_CNAME, converterData.defaultYCname),
        [LC.META_ERR_CNAME]: get(tableMeta, LC.META_ERR_CNAME, converterData.defaultYErrCname),
        [LC.META_TIME_NAMES]: get(tableMeta, LC.META_TIME_NAMES, converterData.timeNames),
        [LC.META_FLUX_NAMES]: get(tableMeta, LC.META_FLUX_NAMES, converterData.yNames),
        [LC.META_ERR_NAMES]: get(tableMeta, LC.META_ERR_NAMES, converterData.yErrNames),
        [LC.META_FLUX_BAND]: get(tableMeta, LC.META_FLUX_BAND, converterData.bandName)
    });
    return {converterData, missionEntries};
}

export function keepHighlightedRowSynced(tbl_id, highlightedRow=0) {
    // ensure the highlighted row of the raw and phase-folded tables are in sync.
    if ([LC.PHASE_FOLDED, LC.RAW_TABLE].includes(tbl_id)) {
        let filterInfo, actOn, rowid;
        const tableModel = getTblById(tbl_id);
        if (tbl_id === LC.RAW_TABLE) {
            actOn = LC.PHASE_FOLDED;
            rowid = getCellValue(tableModel, highlightedRow, 'ROWID') || highlightedRow;
            filterInfo = `RAW_ROWID = ${rowid}`;
        } else {
            rowid = getCellValue(tableModel, highlightedRow, 'RAW_ROWID') || highlightedRow;
            actOn = LC.RAW_TABLE;
            filterInfo = `ROWID = ${rowid}`;
        }
        findIndex(actOn, filterInfo)
            .then( (index) => {
                if (index >=0) {
                    dispatchTableHighlight(actOn, index);
                }
            });
    }
}
