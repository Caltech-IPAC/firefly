import React from 'react';
import PropTypes from 'prop-types';
import {get, has, set, isEmpty} from 'lodash';
import {LC} from './LcManager.js';
import {getConverter} from './LcConverterFactory.js';
import {getCellValue, getTblById, findIndex, getColsByType, getColumnIdx, COL_TYPE} from '../../tables/TableUtil.js';
import {dispatchTableHighlight} from '../../tables/TablesCntlr.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import {getMissionName} from './LcConverterFactory.js';
import {getLayouInfo} from '../../core/LayoutCntlr.js';
import {getViewerGroupKey, onTimeColumnChange} from './LcManager.js';
import {FieldGroup} from '../../ui/FieldGroup.jsx';

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

/**
 * @desc This method is created a mission entries for all missions
 * @param   {object} tableMeta - the meta data in the IpacTable Object
 * @param   {object} layoutInfo - the layout of the UI
 * @returns {{converterData: *, missionEntries: (*|{})}}
 */
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
/**
 * @desc This method returns a suggested list of the column names based on the val entered and the defaultVal if provided.
 * @param {string} val - the input in the text field
 * @param {array} columnNames - the array of string
 * @param {string} defaultVal - the default value, it can be null.
 * @returns {*}
 */
export function getSuggestedList (val, columnNames, defaultVal) {
    return columnNames.reduce((prev, name) => {
        if (  name.startsWith(val) || (defaultVal && val===defaultVal)) {
            prev.push(name);
        }
        return prev;
    }, []);
}

export function getInitialDefaultValues(labelWidth, missionName) {
    var commonDefault = {
        [LC.META_TIME_CNAME]: Object.assign(getTypeData(LC.META_TIME_CNAME, '',
            'Time column name',
            'Time Column:', labelWidth),
            {validator: null}),
        [LC.META_FLUX_CNAME]: Object.assign(getTypeData(LC.META_FLUX_CNAME, '',
            'Value column name',
            'Value Column:', labelWidth),
            {validator: null}),
        [LC.META_TIME_NAMES]: Object.assign(getTypeData(LC.META_TIME_NAMES, '',
            'Value column suggestion'),
            {validator: null}),
        [LC.META_FLUX_NAMES]: Object.assign(getTypeData(LC.META_FLUX_NAMES, '',
            'Value column suggestion'),
            {validator: null}),
        ['cutoutSize']: Object.assign(getTypeData('cutoutSize', '',
            'image cutout size',
            'Cutout Size (arcmin):', 100)),
        [LC.META_URL_CNAME]: Object.assign(getTypeData(LC.META_URL_CNAME, '',
            'Image url column name',
            'Source Column:', labelWidth))

    };

    switch (missionName){
        case 'generic':
            var genericDefault = {
                [LC.META_ERR_CNAME]: Object.assign(getTypeData(LC.META_ERR_CNAME, '',
                    'flux error column name',
                    'Error Column:', labelWidth)),
                [LC.META_COORD_XNAME]: Object.assign(getTypeData(LC.META_COORD_XNAME, '',
                    'Coordinate X column name',
                    'X Column:', labelWidth)),
                [LC.META_COORD_YNAME]: Object.assign(getTypeData(LC.META_COORD_YNAME, '',
                    'Coordinate Y column name',
                    'Y Column:', labelWidth)),
                [LC.META_COORD_SYS]: Object.assign(getTypeData(LC.META_COORD_SYS, '',
                    'Coordinate system',
                    'Coord System:', labelWidth))
            };

            return Object.assign ({},commonDefault, genericDefault );
        case 'other':
            return commonDefault;
        case 'wise':
            const wiseDefault = {
                [LC.META_FLUX_BAND]: Object.assign(getTypeData(LC.META_FLUX_BAND, '',        '' +
                    'Select WISE band for images to be displayed',        'Image display:', 70)),
                [LC.META_ERR_CNAME]: Object.assign(getTypeData(LC.META_ERR_CNAME, '',
                    'value error column name',       'Error Column:', labelWidth))
            };
            return Object.assign ({},commonDefault, wiseDefault );
        case 'ptf':
            return commonDefault;
    }

}

/**
 *
 * @param  {array} numColumns - the array of numerical column names
 * @param  {object} wrapperStyle
 * @returns {Array}
 */
export function getMissionInput (numColumns, wrapperStyle){
    const missionKeys = [LC.META_TIME_CNAME, LC.META_FLUX_CNAME];

    const topZ = 3;
    return  missionKeys.map((key) =>
        <SuggestBoxInputField key={key} fieldKey={key} wrapperStyle={wrapperStyle} popupIndex={topZ}
                              getSuggestions={(val) => getSuggestedList(val,numColumns)}/>
    );

}

/**
 * @desc This method returns the common entries nd the missionInout UI's
 * @param {object} generalEntries
 * @param {object}  missionEntries
 * @param {array} tblColumns
 * @param wrapperStyle
 * @returns {*}
 */
export function getMissionEntries(generalEntries, missionEntries,tblColumns,wrapperStyle){

    if (isEmpty(generalEntries) || isEmpty(missionEntries)) return false;

    const numColumns = getColsByType(tblColumns, COL_TYPE.NUMBER).map( (c) => c.name);

    var allCommonEntries = Object.keys(generalEntries).map((key) =>
        <ValidationField key={key} fieldKey={key} wrapperStyle={wrapperStyle}
                         style={{width: 80}}/>
    );
    var missionInputs = getMissionInput(numColumns, wrapperStyle);
    return {allCommonEntries, missionInputs};
}
/**
 * @desc This method returns the mission's information, such as missName, period, title, and uploaded file name.
 * @param {object} missionEntries
 * @param {TableModel} tblModel
 * @returns {{missionName: (*|string), period: *, title: *, uploadedFileName: string}}
 */
export function getMissionInfo(missionEntries, tblModel){
    const converterId = get(missionEntries, LC.META_MISSION);
    var missionName = getMissionName(converterId) || 'Mission';

    const layoutInfo = getLayouInfo();
    var period =  get(layoutInfo, ['periodRange','period'], '');
    const title = get(tblModel, 'request.uploadFileName','');
    //if the name is too long, truncates it and displays it as a tip
    const uploadedFileName =( title && title.length>20)?title.substring(0, 20)+'...':title;

    return {missionName, period, title, uploadedFileName};
}


export function renderMissionView(generalEntries,missionEntries,missionBands,tblModel, wrapperStyle,labelWidth,callback ){

    //const wrapperStyle = {margin: '3px 0'};
    const tblColumns = get(tblModel, ['tableData', 'columns'], []);
    const groupKey = getViewerGroupKey(missionEntries);
    const {allCommonEntries, missionInputs} = getMissionEntries(generalEntries, missionEntries,tblColumns ,wrapperStyle);

    const {missionName, period, title, uploadedFileName} = getMissionInfo(missionEntries, tblModel);
    return (
        <FieldGroup groupKey={groupKey}
                    reducerFunc={callback(missionEntries, generalEntries)} keepState={true}>

            <div >
                <div style={{ with:{labelWidth}, fontWeight:'bold', display:'inline-block', margin: '3px 0 6px 0'}} > Column Selection</div>
                <label style = {{width: '170px', paddingLeft: '10px', display:'inline-block'}} title={title}>{uploadedFileName}</label>
                <div style = {{fontWeight:'bold',paddingLeft:'13px', display:'inline-block'}}>Images</div>
            </div>

            <div style={{ display: 'flex', alignItems: 'flex-end'}}>
                <div >
                    <ReadOnlyText label='Mission:' content={missionName}
                                  labelWidth={labelWidth} wrapperStyle={{margin: '3px 0 6px 0'}}/>
                    {missionInputs}
                </div>
                <div style={{ padding: '0 6px 0 6px', border: '1px solid #a3aeb9', marginLeft: '54px'}}>
                    {missionBands}
                    {allCommonEntries}
                </div>
            </div>
            <div >
                <ReadOnlyText label='Period:' content={period}
                              labelWidth={labelWidth} wrapperStyle={{margin: '3px 0 6px 0'}}/>

            </div>
        </FieldGroup>
    );
}

/**
 * @desc This method is validating if the fields are valid entries.
 * @param fldsWithValidators
 * @param missionEntries
 * @param typeColumns
 * @returns {*}
 */
export function validate(fldsWithValidators, missionEntries, typeColumns ){
    return fldsWithValidators.reduce((all, fld) => {
        all[fld.key] =
            (val) => {
                let valid = true;
                const cols = get(missionEntries, fld.vkey, []);

                if (cols.length === 0 && typeColumns) {
                    const selColumns = fld.type === 'char' ? typeColumns.charColumns : typeColumns.numColumns;

                    if (selColumns.findIndex((col) => (col === val)) < 0) {
                        valid = false;
                    }
                } else if (!cols.includes(val)) {
                    valid = false;
                }
                return valid ? {valid, message: ''} : {valid, message: `${val} is not a valid column name`};
            };
        return all;
    }, {});
}

export function getTimeAndYColInfo(numericalCols, xyColPattern, rawTable, converterData ){
    let defaultCTimeName = (getColumnIdx(rawTable, converterData.defaultTimeCName) > 0) ? converterData.defaultTimeCName : numericalCols[0];
    let defaultYColName = (getColumnIdx(rawTable, converterData.defaultYCname) > 0) ? converterData.defaultYCname : numericalCols[1];

    defaultYColName = numericalCols.filter((el) => {
            if (el.toLocaleLowerCase().match(xyColPattern[1]) !== null) {
                return el;
            }
        })[0] || defaultYColName;
    defaultCTimeName = numericalCols.filter((el) => {
            if (el.toLocaleLowerCase().match(xyColPattern[0]) !== null) {
                return el;
            }
        })[0] || defaultCTimeName;

    return {defaultCTimeName,defaultYColName};
}

export function fileUpdateOnTimeColumn(fieldKey, value){
    const {missionEntries} = getLayouInfo() || {};
    if (!missionEntries) return;

    onTimeColumnChange(missionEntries[fieldKey], value);
    return {[fieldKey]: value};
}

/**
 *
 * @desc This method is updating the defV based on the input.  The defV is an object, thus, its value can be modified.
 * @param missionListKeys
 * @param missionEntries
 * @param missionKeys
 * @param validators
 * @param defV
 */
export function setValueAndValidator(missionListKeys, missionEntries,missionKeys,  validators, defV){
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
}