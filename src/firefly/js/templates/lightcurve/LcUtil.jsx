import React, {PropTypes} from 'react';
import {get} from 'lodash';
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
        [LC.META_ERR_NAMES]: get(tableMeta, LC.META_ERR_NAMES, converterData.yErrNames)
    });
    return {converterData, missionEntries};
}

export function keepHighlightedRowSynced(tbl_id, highlightedRow) {
    // ensure the highlighted row of the raw and phase-folded tables are in sync.
    if ([LC.PHASE_FOLDED, LC.RAW_TABLE].includes(tbl_id)) {
        let filterInfo, actOn, rowid;
        const tableModel = getTblById(tbl_id);
        if (tbl_id === LC.RAW_TABLE) {
            actOn = LC.PHASE_FOLDED;
            rowid = getCellValue(tableModel, highlightedRow, 'ROWID');
            filterInfo = `RAW_ROWID = ${rowid}`;
        } else {
            rowid = getCellValue(tableModel, highlightedRow, 'RAW_ROWID');
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