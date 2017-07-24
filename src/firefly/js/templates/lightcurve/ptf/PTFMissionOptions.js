import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get,  set, isNil} from 'lodash';
import {RadioGroupInputField} from '../../../ui/RadioGroupInputField.jsx';
import {makeFileRequest, getCellValue, getTblById, getColumnIdx, smartMerge, getColumns, COL_TYPE} from '../../../tables/TableUtil.js';
import {sortInfoString} from '../../../tables/SortInfo.js';
import {getInitialDefaultValues,renderMissionView,validate,getTimeAndYColInfo,fileUpdateOnTimeColumn,setValueAndValidator} from '../LcUtil.jsx';
import {LC} from '../LcManager.js';


const labelWidth = 80;
export class PTFSettingBox extends PureComponent {
    constructor(props) {
        super(props);

    }

    render() {
        var {generalEntries, missionEntries} = this.props;
        const tblModel = getTblById(LC.RAW_TABLE);
        const wrapperStyle = {margin: '3px 0'};

        var missionFilters = (<RadioGroupInputField key='band'
                                                  fieldKey={LC.META_FLUX_BAND} wrapperStyle={wrapperStyle}
                                                  alignment='horizontal'
                                                  options={[
                    {label: 'g', value: 'g'},
                    {label: 'R', value: 'r'}

                ]}/>);

        return renderMissionView(generalEntries,missionEntries, missionFilters,tblModel,wrapperStyle,labelWidth , ptfOptionsReducer);


    }
}

PTFSettingBox.propTypes = {
    generalEntries: PropTypes.object,
    missionEntries: PropTypes.object
};

//TODO, this is copied from WISE, need to refactor it after the images and other information are updated.
export const ptfOptionsReducer = (missionEntries, generalEntries) => {
    return (inFields, action) => {
        if (inFields) {
            return inFields;
        }


        const defValues =getInitialDefaultValues(labelWidth,'ptf');

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

//TODO, after the PTF images loaded, this function can be updated.  It is the place holder for now.
function getFieldValidators(missionEntries) {
    const fldsWithValidators = [
        {key: LC.META_TIME_CNAME, vkey: LC.META_TIME_NAMES},
        {key: LC.META_FLUX_CNAME, vkey: LC.META_FLUX_NAMES},
        {key: LC.META_URL_CNAME}

    ];
    return validate(fldsWithValidators, missionEntries);

}


export function isValidPTFTable() {

    const tableModel = getTblById(LC.RAW_TABLE);

    const pid = getCellValue(tableModel, 0, 'pid');
    if (!isNil(pid)) {
        return {errorMsg: undefined, isValid: true};
    }
    else {
         const errorMsg = `The uploaded table is not valid. The PTF  option requires pid.
                        Please select the "Other" upload option for tables that do not meet these requirements.`;
        return {errorMsg, isValid:false};
   }
}

/**
 * Pregex pattern for ptf, at least to find obsmjd and mag_autocorr if present
 * @type {string[]}
 */
const xyColPattern = ['obsmjd', 'mag_autocorr'];
export function ptfOnNewRawTable(rawTable, missionEntries, generalEntries, converterData, layoutInfo) {

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
        [LC.META_FLUX_BAND]: get(metaInfo, LC.META_FLUX_BAND, 'g')

    };

    missionEntries = Object.assign({}, missionEntries, defaultValues);
    const newLayoutInfo = smartMerge(layoutInfo, {missionEntries, generalEntries});

    return {newLayoutInfo, shouldContinue: false};
}

//TODO if ptfRawTableRequest and ptfOnFieldUpdate are nothing different from the ones in wiseMssionOption, these two can be replaced.
export function ptfRawTableRequest(converter, source, uploadFileName='') {
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


export function ptfOnFieldUpdate(fieldKey, value) {
    // images are controlled by radio button -> filter g, R.
    if (fieldKey === LC.META_TIME_CNAME) {
        return fileUpdateOnTimeColumn(fieldKey, value);
    } else if ([LC.META_FLUX_CNAME, LC.META_ERR_CNAME,  LC.META_FLUX_BAND].includes(fieldKey)) {
        return {[fieldKey]: value};
    }


}

