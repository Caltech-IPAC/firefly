import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get, has,  set, isNil} from 'lodash';
import {RadioGroupInputField} from '../../../ui/RadioGroupInputField.jsx';
import {getLayouInfo} from '../../../core/LayoutCntlr.js';
import {makeFileRequest, getCellValue, getTblById, getColumnIdx, smartMerge,getOnlyNumericalColNames} from '../../../tables/TableUtil.js';
import {sortInfoString} from '../../../tables/SortInfo.js';
import {getInitialDefaultValues,renderMissionView,validate,getTimeAndYColInfo,fileUpdateOnTimeColumn,setValueAndValidator} from '../LcUtil.jsx';
import {LC,  onTimeColumnChange} from '../LcManager.js';


const labelWidth = 80;

export class WiseSettingBox extends PureComponent {
    constructor(props) {
        super(props);


    }

    render() {
        var {generalEntries, missionEntries} = this.props;
        const tblModel = getTblById(LC.RAW_TABLE);
        const wrapperStyle = {margin: '3px 0'};

        var missionBands = (<RadioGroupInputField key='band'
                                                  fieldKey={LC.META_FLUX_BAND} wrapperStyle={wrapperStyle}
                                                  alignment='horizontal'
                                                  options={[
                    {label: 'W1', value: 'w1'},
                    {label: 'W2', value: 'w2'},
                    {label: 'W3', value: 'w3'},
                    {label: 'W4', value: 'w4'}
                ]}/>);

         return renderMissionView(generalEntries,missionEntries,missionBands,tblModel,wrapperStyle,labelWidth, wiseOptionsReducer );


    }
}

WiseSettingBox.propTypes = {
    generalEntries: PropTypes.object,
    missionEntries: PropTypes.object
};


export const wiseOptionsReducer = (missionEntries, generalEntries) => {

    const labelWidth = 80;

    return (inFields) => {
        if (inFields) {
            return inFields;
        }

        // defValues used to keep the initial values for parameters in the field group of result page
        const defValues =getInitialDefaultValues(labelWidth,'wise');

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
        //{key: LC.META_ERR_CNAME, vkey: LC.META_ERR_NAMES} // error can have no value
    ];

    return validate(fldsWithValidators, missionEntries);

}

export function isValidWiseTable(){

    const tableModel = getTblById(LC.RAW_TABLE);

    // For images from AllWise:
    const frameId = getCellValue(tableModel, 0, 'frame_id');

    // For other single exposure tables (NEOWISE, etc)
    const frameNum = getCellValue(tableModel, 0, 'frame_num');
    const scanId = getCellValue(tableModel, 0, 'scan_id');
    const sourceId = getCellValue(tableModel, 0, 'source_id');

    if (!isNil(frameId) || !isNil(sourceId) || (!isNil(scanId) && !isNil(frameNum)) ){
        return {errorMsg:undefined, isValid:true};
    }
    else {
        const errorMsg=`The uploaded table is not valid. The WISE  option requires frame_id, or source_id, or both scan_id and frame_num.
                Please select the "Other" upload option for tables that do not meet these requirements.`;
        return {errorMsg, isValid:false};
    }
}


/**
 * Pregex pattern for wise, at least to find mjd and w1mpro if present
 * @type {string[]}
 */
const xyColPattern = ['\\w*jd\\w*', 'w[1-4]mpro\\w*'];
export function wiseOnNewRawTable(rawTable, missionEntries, generalEntries, converterData, layoutInfo) {

    // Update default values AND sortInfo and
    const metaInfo = rawTable && rawTable.tableMeta;
    let numericalCols = getOnlyNumericalColNames(rawTable);
    let defaultDataSource = (getColumnIdx(rawTable, converterData.dataSource) > 0) ? converterData.dataSource : numericalCols[3];

    const {defaultCTimeName,defaultYColName } = getTimeAndYColInfo(numericalCols,xyColPattern,rawTable,converterData );
    const defaultValues = {
        [LC.META_TIME_CNAME]: get(metaInfo, LC.META_TIME_CNAME, defaultCTimeName),
        [LC.META_FLUX_CNAME]: get(metaInfo, LC.META_FLUX_CNAME, defaultYColName),
        [LC.META_TIME_NAMES]: get(metaInfo, LC.META_TIME_NAMES, numericalCols),
        [LC.META_FLUX_NAMES]: get(metaInfo, LC.META_FLUX_NAMES, numericalCols),
        [LC.META_URL_CNAME]: get(metaInfo, LC.META_URL_CNAME, defaultDataSource),
        [LC.META_FLUX_BAND]: get(metaInfo, LC.META_FLUX_BAND, 'w1'),

    };

    missionEntries = Object.assign({}, missionEntries, defaultValues);
    const newLayoutInfo = smartMerge(layoutInfo, {missionEntries, generalEntries});

    return {newLayoutInfo, shouldContinue: false};
}

export function wiseRawTableRequest(converter, source, uploadFileName='') {
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

export function wiseOnFieldUpdate(fieldKey, value) {
    // images are controlled by radio button -> band w1,w2,w3,w4.
    if (fieldKey === LC.META_TIME_CNAME) {
        return fileUpdateOnTimeColumn(fieldKey, value);
    } else if ([LC.META_FLUX_CNAME, LC.META_ERR_CNAME, LC.META_URL_CNAME, LC.META_FLUX_BAND].includes(fieldKey)) {
        return {[fieldKey]: value};
    }
}
