import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get,  set, isNil} from 'lodash';
import {getCellValue, getTblById, getColumnIdx, smartMerge, getColumns, COL_TYPE} from '../../../tables/TableUtil.js';
import {makeFileRequest} from '../../../tables/TableRequestUtil.js';
import {sortInfoString} from '../../../tables/SortInfo.js';
import {getInitialDefaultValues,renderMissionView,validate,getTimeAndYColInfo,fileUpdateOnTimeColumn,setValueAndValidator} from '../LcUtil.jsx';
import {LC} from '../LcManager.js';
import {defaultDownloadPanel} from '../LcResult.jsx';


const labelWidth = 80;
export class ZTFSettingBox extends PureComponent {
    constructor(props) {
        super(props);

    }

    render() {
        var {generalEntries, missionEntries} = this.props;
        const tblModel = getTblById(LC.RAW_TABLE);
        var missionFilters = ( <div style={{ width:{labelWidth}, display:'inline-block', margin: '3px 0 6px 0'}} >
            <br /> </div>);

        const imageStyle = { padding: '0 6px 0 6px', marginLeft: '54px'};
        return renderMissionView({generalEntries,missionEntries,missionFilters,tblModel,imageEntriesStyle:imageStyle, labelWidth , callback:ztfOptionsReducer});


    }
}

ZTFSettingBox.propTypes = {
    generalEntries: PropTypes.object,
    missionEntries: PropTypes.object
};

export const ztfOptionsReducer = (missionEntries, generalEntries) => {
    return (inFields, action) => {
        if (inFields) {
            return inFields;
        }


        const defValues =getInitialDefaultValues(labelWidth,'ztf');

        var defV = Object.assign({}, defValues);

        const missionKeys = [LC.META_TIME_CNAME, LC.META_FLUX_CNAME, LC.META_URL_CNAME];
        const missionListKeys = [LC.META_TIME_NAMES, LC.META_FLUX_NAMES];
        const validators = getFieldValidators(missionEntries);

        setValueAndValidator(missionListKeys, missionEntries,missionKeys, validators, defV);
        Object.keys(generalEntries).forEach((key) => {
            set(defV, [key, 'value'], get(generalEntries, key, ''));
        });
        return defV;
    };
};

function getFieldValidators(missionEntries) {
    const fldsWithValidators = [
        {key: LC.META_TIME_CNAME, vkey: LC.META_TIME_NAMES},
        {key: LC.META_FLUX_CNAME, vkey: LC.META_FLUX_NAMES},
        {key: LC.META_URL_CNAME}

    ];
    return validate(fldsWithValidators, missionEntries);

}


export function isValidZTFTable() {

    const tableModel = getTblById(LC.RAW_TABLE);

    const field = getCellValue(tableModel, 0, 'field');
    if (!isNil(field)) {
        return {errorMsg: undefined, isValid: true};
    }
    else {
         const errorMsg = `The uploaded table is not valid. The ZTF option requires field.
                        Please select the "Other" upload option for tables that do not meet these requirements.`;
        return {errorMsg, isValid:false};
   }
}

/**
 * Pregex pattern for ztf, at least to find mjd and meanmag if present
 * @type {string[]}
 */
const xyColPattern = ['(?:^|\\W)mjd(?:$|\\W)', '(?:^|\\W)mag(?:$|\\W)'];
export function ztfOnNewRawTable(rawTable, missionEntries, generalEntries, converterData, layoutInfo) {

    // Update default values AND sortInfo and
    const metaInfo = rawTable && rawTable.tableMeta;

    const numericalCols = getColumns(rawTable, COL_TYPE.NUMBER).map((c) => c.name);
    const defaultDataSource = (getColumnIdx(rawTable, converterData.dataSource) > 0) ? converterData.dataSource : numericalCols[3];

    const {defaultCTimeName,defaultYColName } = getTimeAndYColInfo(numericalCols,xyColPattern,rawTable,converterData );

    const defaultValues = {
        [LC.META_TIME_CNAME]: get(metaInfo, LC.META_TIME_CNAME, defaultCTimeName),
        [LC.META_FLUX_CNAME]: get(metaInfo, LC.META_FLUX_CNAME, defaultYColName),
        [LC.META_TIME_NAMES]: get(metaInfo, LC.META_TIME_NAMES, numericalCols),
        [LC.META_FLUX_NAMES]: get(metaInfo, LC.META_FLUX_NAMES, numericalCols),
        [LC.META_URL_CNAME]: get(metaInfo, LC.META_URL_CNAME, defaultDataSource),
        [LC.META_FLUX_BAND]: get(metaInfo, LC.META_FLUX_BAND, 'zg')

    };

    missionEntries = Object.assign({}, missionEntries, defaultValues);
    const newLayoutInfo = smartMerge(layoutInfo, {missionEntries, generalEntries});

    return {newLayoutInfo, shouldContinue: false};
}

//TODO if ztfRawTableRequest and ztfOnFieldUpdate are nothing different from the ones in ztfMssionOption, these two can be replaced.
export function ztfRawTableRequest(converter, source, uploadFileName='') {
    const timeCName = converter.defaultTimeCName;
    const mission = converter.converterId;
    const options = {
        tbl_id: LC.RAW_TABLE,
        sortInfo: sortInfoString(timeCName), // if present, it will skip LcManager.js#ensureValidRawTable
        META_INFO: {[LC.META_MISSION]: mission, timeCName},
        pageSize: LC.TABLE_PAGESIZE,
        uploadFileName

    };
    return makeFileRequest('Input Data', source, null, options);

}


export function ztfOnFieldUpdate(fieldKey, value) {
    // images are controlled by radio button -> filter zg, zr etc.
    if (fieldKey === LC.META_TIME_CNAME) {
        return fileUpdateOnTimeColumn(fieldKey, value);
    } else if ([LC.META_FLUX_CNAME, LC.META_ERR_CNAME,  LC.META_FLUX_BAND].includes(fieldKey)) {
        return {[fieldKey]: value};
    }


}

/**
 *  This is specialized for ZTF download.
 *  Gets the download option panel for ZTF with specific file processor id 'ZtfDownload'
 * @param mission
 * @param cutoutSizeInDeg
 * @returns {XML}
 */
export function ztfDownloaderOptPanel (mission, cutoutSizeInDeg) {

    return defaultDownloadPanel(mission, cutoutSizeInDeg,
        {
            FileGroupProcessor: 'ZtfLcDownload',
            ProductLevel:'sci',
            schema:'products',
            table:'sci'
        }
    );
}
