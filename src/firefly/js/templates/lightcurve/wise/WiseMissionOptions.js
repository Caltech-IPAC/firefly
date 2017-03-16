import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {get, has, isEmpty, set, pick, cloneDeep} from 'lodash';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {SuggestBoxInputField} from '../../../ui/SuggestBoxInputField.jsx';
import {RadioGroupInputField} from '../../../ui/RadioGroupInputField.jsx';
import {getLayouInfo} from '../../../core/LayoutCntlr.js';
import {makeFileRequest, getCellValue, getTblById, getColumnIdx, smartMerge} from '../../../tables/TableUtil.js';
import {updateMerge} from '../../../util/WebUtil.js';
import {sortInfoString} from '../../../tables/SortInfo.js';
import {ReadOnlyText, getTypeData} from '../LcUtil.jsx';
import {LC, getViewerGroupKey} from '../LcManager.js';
import {getMissionName} from '../LcConverterFactory.js';
import {isNil} from 'lodash';

const labelWidth = 80;

export class WiseSettingBox extends Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    render() {
        var {generalEntries, missionEntries} = this.props;

        if (isEmpty(generalEntries) || isEmpty(missionEntries)) return false;

        const wrapperStyle = {margin: '3px 0'};

        const tblModel = getTblById(LC.RAW_TABLE);
        //const validTimes = get(missionEntries, LC.META_FLUX_NAMES, []);
        //const validValues = get(missionEntries, LC.META_FLUX_NAMES, []);

        var getList = (val, type) => {
            var colType = (!type || type === 'numeric') ?
                ['double', 'd', 'long', 'l', 'int', 'i', 'float', 'f'] : ['char', 'c', 's', 'str', 'double', 'd', 'long', 'l', 'int', 'i', 'float', 'f'];

            return get(tblModel, ['tableData', 'columns']).reduce((prev, col) => {
                if ((colType.includes(col.type)) &&
                    (!has(col, 'visibility') || get(col, 'visibility') !== 'hidden') &&
                    (col.name.startsWith(val))) {
                    prev.push(col.name);
                }
                return prev;
            }, []);
        };
        var allCommonEntries = Object.keys(generalEntries).map((key) =>
            <ValidationField key={key} fieldKey={key} wrapperStyle={wrapperStyle}
                             style={{width: 80}}/>
        );

        const missionKeys = [LC.META_TIME_CNAME, LC.META_FLUX_CNAME];
        const missionOtherKeys = [LC.META_ERR_CNAME];
        const imageDataSource = [LC.META_URL_CNAME]; // use meta_url from generic case, see LcManager.js:320 for why
        const missionListKeys = [LC.META_TIME_NAMES, LC.META_FLUX_NAMES];
        const topZ = 3;
        var missionInputs = missionKeys.map((key, index) =>
            <SuggestBoxInputField key={key} fieldKey={key} wrapperStyle={wrapperStyle} popupIndex={topZ}
                                  getSuggestions={(val) => getList(val, 'numeric')}/>
        );

        var missionData = imageDataSource.map((key) =>
            <SuggestBoxInputField key={key} fieldKey={key} wrapperStyle={wrapperStyle} popupIndex={topZ}
                                  getSuggestions={(val) => getList(val)}/>
        );

        //const frameId = getColumnIdx(tblModel, 'frame_id');
        //var missionOthers = (frameId) => {
        //    if (frameId > -1) {
        //        return <RadioGroupInputField key='band'
        //                                     fieldKey='band' wrapperStyle={wrapperStyle}
        //                                     alignment='horizontal'
        //                                     options={[
        //            {label: 'W1', value: 'w1'},
        //            {label: 'W2', value: 'w2'},
        //            {label: 'W3', value: 'w3'},
        //            {label: 'W4', value: 'w4'}
        //        ]}/>;
        //    } else {
        //        return <div><em>frame_id</em> column is missing, no images will be displayed </div>;
        //    }
        //};
        var missionOthers = <RadioGroupInputField key='band'
                                                  fieldKey={LC.META_FLUX_BAND} wrapperStyle={wrapperStyle}
                                                  alignment='horizontal'
                                                  options={[
                    {label: 'W1', value: 'w1'},
                    {label: 'W2', value: 'w2'},
                    {label: 'W3', value: 'w3'},
                    {label: 'W4', value: 'w4'}
                ]}/>;
        const groupKey = getViewerGroupKey(missionEntries);
        const converterId = get(missionEntries, LC.META_MISSION);
        var missionName = getMissionName(converterId) || 'Mission';

        return (
            <FieldGroup groupKey={groupKey}
                        reducerFunc={wiseOptionsReducer(missionEntries, generalEntries)} keepState={true}>
                <div style={{display: 'flex', alignItems: 'flex-end'}}>
                    <div >
                        <ReadOnlyText label='Mission:' content={missionName}
                                      labelWidth={labelWidth} wrapperStyle={{margin: '3px 0 6px 0'}}/>

                        {missionInputs}
                        {/*missionData*/}
                    </div>
                    <div style={{padding: '0 6px 0 6px', border: '1px solid #a3aeb9'}}>
                        {missionOthers}
                        {allCommonEntries}
                    </div>
                </div>
            </FieldGroup>
        );
    }
}

WiseSettingBox.propTypes = {
    generalEntries: PropTypes.object,
    missionEntries: PropTypes.object
};


export const wiseOptionsReducer = (missionEntries, generalEntries) => {
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
        const defValues = {
            [LC.META_FLUX_BAND]: Object.assign(getTypeData(LC.META_FLUX_BAND, '',
                'Select WISE band for images to be displayed',
                'Image display:', 70)),
            [LC.META_TIME_CNAME]: Object.assign(getTypeData(LC.META_TIME_CNAME, '',
                'time column name',
                'Time Column:', labelWidth),
                {validator: null}),
            [LC.META_FLUX_CNAME]: Object.assign(getTypeData(LC.META_FLUX_CNAME, '',
                'value column name',
                'Value Column:', labelWidth),
                {validator: null}),
            [LC.META_TIME_NAMES]: Object.assign(getTypeData(LC.META_TIME_NAMES, '',
                'time column suggestion'),
                {validator: null}),
            [LC.META_FLUX_NAMES]: Object.assign(getTypeData(LC.META_FLUX_NAMES, '',
                'value column suggestion'),
                {validator: null}),
            ['cutoutSize']: Object.assign(getTypeData('cutoutSize', '',
                'image cutout size',
                'Cutout Size (deg):', 100)),
            [LC.META_URL_CNAME]: Object.assign(getTypeData(LC.META_URL_CNAME, '',
                'WISE Image identifier column name (frame_id)',
                'Image Column:', labelWidth)),
            [LC.META_ERR_CNAME]: Object.assign(getTypeData(LC.META_ERR_CNAME, '',
                'value error column name',
                'Error Column:', labelWidth))
        };

        var defV = Object.assign({}, defValues);

        const missionKeys = [LC.META_TIME_CNAME, LC.META_FLUX_CNAME, LC.META_URL_CNAME];
        const missionListKeys = [LC.META_TIME_NAMES, LC.META_FLUX_NAMES];
        const validators = getFieldValidators(missionEntries, getTblById(LC.RAW_TABLE));

        missionListKeys.forEach((key) => {
            set(defV, [key, 'value'], get(missionEntries, key, []));
        });

        // set value and validator
        missionKeys.forEach((key) => {
            set(defV, [key, 'value'], get(missionEntries, key, ''));
            if (has(validators, key)) {
                set(defV, [key, 'validator'], validators[key]);
            }
        });
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
    return fldsWithValidators.reduce((all, fld) => {
        all[fld.key] =
            (val) => {
                let retVal = {valid: true, message: ''};
                const cols = get(missionEntries, fld.vkey, []);
                if (cols.length !== 0 && !cols.includes(val)) {
                    retVal = {valid: false, message: `${val} is not a valid column name`};
                }
                return retVal;
            };
        return all;
    }, {});
}
/*
 function setFields(missionEntries, generalEntries) {
 const groupKey = getViewerGroupKey(missionEntries);
 const fields = FieldGroupUtils.getGroupFields(groupKey);
 const validators = getFieldValidators(missionEntries);
 if (fields) {
 const initState = Object.keys(fields).reduce((prev, fieldKey) => {
 if (has(missionEntries, fieldKey)) {
 prev.push({fieldKey, value: get(missionEntries, fieldKey), validator: validators[fieldKey]});
 } else if (has(generalEntries,fieldKey)) {
 prev.push({fieldKey, value: get(generalEntries, fieldKey)});
 }
 return prev;
 }, []);
 dispatchMultiValueChange(groupKey, initState);
 }
 }
 */

/**
 * Returns only numerical column names form raw lc table
 * @param rawTbl - raw table
 * @returns {TableColumn[]} - array of table columns objects
 */
function getOnlyNumericalCol(rawTbl) {
    //let cols = get(rawTbl, 'tableData.columns');
    //let cnum = cols.filter((c) => c.type != 'char');
    //let c = [];
    //cnum.forEach((el) => {
    //    c.push(el.name);
    //});
    //return c;

    var colType = ['double', 'd', 'long', 'l', 'int', 'i', 'float', 'f'];
    return get(rawTbl, ['tableData', 'columns']).reduce((prev, col) => {
        if ((colType.includes(col.type)) &&
            (!has(col, 'visibility') || get(col, 'visibility') !== 'hidden')) {
            prev.push(col.name);
        }
        return prev;
    }, []);
}

/**
 * Check if this is WISE MEP table on upload, otherise bailout
 * @returns {boolean} error message to be picked up by UI
 */
export function isBasicTableUploadValid() {

    const tableModel = getTblById(LC.RAW_TABLE);
    // For wcs target match and overlay
    const ra = getCellValue(tableModel, 0, 'ra');
    const dec = getCellValue(tableModel, 0, 'dec');

    // For images from AllWise:
    const frameId = getCellValue(tableModel, 0, 'frame_id');

    // For other single exposure tables (NEOWISE, etc)
    const frameNum = getCellValue(tableModel, 0, 'frame_num');
    const scanId = getCellValue(tableModel, 0, 'scan_id');
    const sourceId = getCellValue(tableModel, 0, 'source_id');

    // For now, bailout when images fetched will fail:
    if(isNil(frameId)){
        if(isNil(sourceId)){
           return (!isNil(scanId) && !isNil(frameNum));
        }else{
            return true;
        }
    }else{
        return true;
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

    let error = '';
    if(!isBasicTableUploadValid()){
        const frameId = getCellValue(rawTable, 0, 'frame_id');
        const sourceId = getCellValue(rawTable, 0, 'source_id');
        const frameNum = getCellValue(rawTable, 0, 'frame_num');
        const scanId = getCellValue(rawTable, 0, 'scan_id');
        var a = [];
        isNil(frameId) ? a.push('frame_id') :'';
        isNil(sourceId) ? a.push('source_id'):'';
        isNil(scanId) ? a.push('scan_id') : '';
        isNil(frameNum) ? a.push('frame_num'):'';

        for (let i=0; i< a.length-1;i++){
            error+= a[i]+', ';
        }
        error+= a[a.length-1];
    }


    let numericalCols = getOnlyNumericalCol(rawTable);

    //Find column based on a pattern, if not, just get the constant value from the converter (=mjd, =w1mpro_ep)

    let defaultCTimeName = (getColumnIdx(rawTable, converterData.defaultTimeCName) > 0) ? converterData.defaultTimeCName : numericalCols[0];
    let defaultYColName = (getColumnIdx(rawTable, converterData.defaultYCname) > 0) ? converterData.defaultYCname : numericalCols[1];
    let defaultDataSource = (getColumnIdx(rawTable, converterData.dataSource) > 0) ? converterData.dataSource : numericalCols[3];

    defaultYColName = numericalCols.filter((el) => {
            if (el.toLocaleLowerCase().match(xyColPattern[1]) != null) {
                return el;
            }
        })[0] || defaultYColName;
    defaultCTimeName = numericalCols.filter((el) => {
            if (el.toLocaleLowerCase().match(xyColPattern[0]) != null) {
                return el;
            }
        })[0] || defaultCTimeName;
    const defaultValues = {
        [LC.META_TIME_CNAME]: get(metaInfo, LC.META_TIME_CNAME, defaultCTimeName),
        [LC.META_FLUX_CNAME]: get(metaInfo, LC.META_FLUX_CNAME, defaultYColName),
        [LC.META_TIME_NAMES]: get(metaInfo, LC.META_TIME_NAMES, numericalCols),
        [LC.META_FLUX_NAMES]: get(metaInfo, LC.META_FLUX_NAMES, numericalCols),
        [LC.META_URL_CNAME]: get(metaInfo, LC.META_URL_CNAME, defaultDataSource),
        [LC.META_FLUX_BAND]: get(metaInfo, LC.META_FLUX_BAND, 'w1')
    };

    missionEntries = Object.assign({}, missionEntries, defaultValues);
    //
    //
    //var {rawTableRequest} = layoutInfo;
    //rawTableRequest = cloneDeep(rawTable.request);
    //const options = {
    //    sortInfo: sortInfoString(defaultCTimeName),
    //    META_INFO: {...pick(missionEntries, [LC.META_MISSION, LC.META_TIME_CNAME, LC.META_FLUX_CNAME])}
    //};


    const newLayoutInfo = smartMerge(layoutInfo, {missionEntries, generalEntries, error});
    return {newLayoutInfo, shouldContinue: false};
}

export function wiseRawTableRequest(converter, source) {
    const timeCName = converter.defaultTimeCName;
    const mission = converter.converterId;
    const options = {
        tbl_id: LC.RAW_TABLE,
        sortInfo: sortInfoString(timeCName), // if present, it will skip LcManager.js#ensureValidRawTable
        META_INFO: {[LC.META_MISSION]: mission, timeCName},
        pageSize: LC.TABLE_PAGESIZE
    };
    return makeFileRequest('Input Data', source, null, options);

}

export function wiseOnFieldUpdate(fieldKey, value) {

    // images are controlled by radio button -> band w1,w2,w3,w4.
    if ([LC.META_TIME_CNAME, LC.META_FLUX_CNAME, LC.META_ERR_CNAME, LC.META_URL_CNAME, LC.META_FLUX_BAND].includes(fieldKey)) {
        return {[fieldKey]: value};
    }
}
