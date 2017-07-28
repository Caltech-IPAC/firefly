import React from 'react';
import PropTypes from 'prop-types';
import {get,isEmpty} from 'lodash';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {makeFileRequest} from '../../../tables/TableUtil.js';
import {getColumnIdx, smartMerge,  getColsByType, COL_TYPE} from '../../../tables/TableUtil.js';
import {ReadOnlyText, getInitialDefaultValues,getMissionInput,getMissionInfo,validate,fileUpdateOnTimeColumn, setValueAndValidator} from '../LcUtil.jsx';
import {LC, getViewerGroupKey} from '../LcManager.js';
import {getMissionName} from '../LcConverterFactory.js';
import {SettingBox} from '../SettingBox.jsx';
const labelWidth = 90;

export class BasicSettingBox extends SettingBox {
    constructor(props) {
        super(props);
    }
    render() {
        var {generalEntries, missionEntries} = this.props;
        var {tblColumns, numColumns, charColumns} = this.state;


        if (isEmpty(tblColumns) || isEmpty(generalEntries) || isEmpty(missionEntries)) return false;

        const wrapperStyle = {margin: '3px 0'};

        const  missionInputs=getMissionInput (numColumns, wrapperStyle);
        const groupKey = getViewerGroupKey(missionEntries);
        const typeColumns = {charColumns, numColumns};

        const {missionName, period, title, uploadedFileName} = getMissionInfo(missionEntries, typeColumns);

        return (
            <FieldGroup groupKey={groupKey}
                        reducerFunc={basicOptionsReducer(missionEntries, generalEntries, typeColumns)} keepState={true}>

                <div >
                    <div style={{ with:{labelWidth}, fontWeight:'bold', display:'inline-block', margin: '3px 0 6px 0'}} > Column Selection</div>
                    <label style = {{width: '170px', paddingLeft: '10px', display:'inline-block'}} title={title}>{uploadedFileName}</label>

                </div>
                <div style={{display: 'flex', flexDirection: 'column'}}>
                    {getMissionName(missionName) !== '' &&
                    <ReadOnlyText label='Mission:' content={getMissionName(missionName)}
                                  labelWidth={labelWidth} wrapperStyle={{margin: '3px 0 6px 0'}}/>}
                    <div style={{display: 'flex'}}>
                        <div>
                            {missionInputs}

                        </div>
                    </div>
                </div>
                <ReadOnlyText label='Period:' content={period}
                              labelWidth={labelWidth} wrapperStyle={{margin: '3px 0 6px 0'}}/>

            </FieldGroup>
        );
    }
}

BasicSettingBox.propTypes = {
    generalEntries: PropTypes.object,
    missionEntries: PropTypes.object
};


export const basicOptionsReducer = (missionEntries, generalEntries, typeColumns) => {
    return (inFields, action) => {
        if (inFields) {
            return inFields;
        }

        // defValues used to keep the initial values for parameters in the field group of result page
        // time: time column
        // flux: flux column
        // timecols:  time column candidates
        // fluxcols:  flux column candidates
        // errorcolumm: error column
        // cutoutsize: image cutout size
        const defValues =getInitialDefaultValues(labelWidth, 'other');

        var defV = Object.assign({}, defValues);
        const validators = getFieldValidators(missionEntries, typeColumns);

        const missionKeys = [LC.META_TIME_CNAME, LC.META_FLUX_CNAME, LC.META_URL_CNAME, LC.META_ERR_CNAME,
            LC.META_COORD_XNAME, LC.META_COORD_YNAME, LC.META_COORD_SYS];
        const missionListKeys = [LC.META_TIME_NAMES, LC.META_FLUX_NAMES];



        setValueAndValidator(missionListKeys, missionEntries,missionKeys, validators, defV);
        return defV;
    };
};


function getFieldValidators(missionEntries, typeColumns) {

    const fldsWithValidators = [
        {key: LC.META_TIME_CNAME, vkey: LC.META_TIME_NAMES},
        {key: LC.META_FLUX_CNAME, vkey: LC.META_FLUX_NAMES},
        {key: LC.META_URL_CNAME, vkey: LC.META_URL_NAMES, type:'char'}
    ];

    return validate(fldsWithValidators, missionEntries, typeColumns );

}


/**
 * Return true if the user inputs make sense to show images in result layout
 * @param {object} layoutInfo
 * @return {bool}
 */
export function imagesShouldBeDisplayed(layoutInfo) {

    //Control image display by lookig at data source input field:
    // if (isEmpty(get(layoutInfo, [LC.MISSION_DATA, LC.META_URL_CNAME]))) {
    //    return false;
    //}
    //return get(layoutInfo, [LC.MISSION_DATA, LC.META_URL_CNAME]).length > 0;
    //
    return false;
}

/**
 * Pregex pattern for wise, at least to find mjd and w1mpro if present
 * @type {string[]}
 */
const xyColPattern = ['\\w*jd\\w*'];

export function basicOnNewRawTable(rawTable, missionEntries, generalEntries, converterData, layoutInfo = {}) {

    // Update default values AND sortInfo and
    const metaInfo = rawTable && rawTable.tableMeta;
    const tblColumns = get(rawTable, ['tableData', 'columns'], []);
    const numericalCols = getColsByType(tblColumns, COL_TYPE.NUMBER).map((c) => c.name);

    //let strCols = getStringCol(rawTable);
    //strCols.push('');// Empty means no images
    let defaultCTimeName = (getColumnIdx(rawTable, converterData.defaultTimeCName) > 0) ? converterData.defaultTimeCName : numericalCols[0];
    const defaultYColName = (getColumnIdx(rawTable, converterData.defaultYCname) > 0) ? converterData.defaultYCname : numericalCols[1];

    defaultCTimeName = numericalCols.filter((el) => {
            if (el.toLocaleLowerCase().match(xyColPattern[0]) !== null) {
                return el;
            }
        })[0] || defaultCTimeName;
    const defaultValues = {
        [LC.META_TIME_CNAME]: get(metaInfo, LC.META_TIME_CNAME, defaultCTimeName),
        [LC.META_FLUX_CNAME]: get(metaInfo, LC.META_FLUX_CNAME, defaultYColName)

    };

    missionEntries = Object.assign({}, missionEntries, defaultValues);
    const newLayoutInfo = smartMerge(layoutInfo, {missionEntries, generalEntries});
    return {newLayoutInfo, shouldContinue: true};
}

export function basicRawTableRequest(converter, source, uploadFileName='') {
    const options = {
        tbl_id: LC.RAW_TABLE,
        tblType: 'notACatalog',
        META_INFO: {[LC.META_MISSION]: converter.converterId},
        pageSize: LC.TABLE_PAGESIZE,
        uploadFileName
    };
   return  makeFileRequest('Input Data', source, null, options);


}

export function basicOnFieldUpdate(fieldKey, value) {
    if ([LC.META_TIME_CNAME].includes(fieldKey)) {
        return fileUpdateOnTimeColumn(fieldKey, value);
    } else if ([LC.META_FLUX_CNAME].includes(fieldKey)) {
        return {[fieldKey]: value};
    }
}