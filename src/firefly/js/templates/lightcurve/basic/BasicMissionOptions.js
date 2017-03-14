import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {get, has, isEmpty, set} from 'lodash';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {SuggestBoxInputField} from '../../../ui/SuggestBoxInputField.jsx';
import {makeFileRequest} from '../../../tables/TableUtil.js';
import {getColumnIdx, getTblById, smartMerge} from '../../../tables/TableUtil.js';
import {sortInfoString} from '../../../tables/SortInfo.js';
import {ReadOnlyText, getTypeData} from '../LcUtil.jsx';
import {LC, getViewerGroupKey} from '../LcManager.js';
import {getMissionName, coordSysOptions} from '../LcConverterFactory.js';

const labelWidth = 90;


export class BasicSettingBox extends Component {
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
        /*
         var allCommonEntries = Object.keys(generalEntries).map((key) =>
         <ValidationField key={key} fieldKey={key} wrapperStyle={wrapperStyle}
         style={{width: 80}}/>
         );
         */
        const missionKeys = [LC.META_TIME_CNAME, LC.META_FLUX_CNAME];
        const missionUrl = [LC.META_URL_CNAME];
        const missionOtherKeys = [LC.META_ERR_CNAME];
        const tblModel = getTblById(LC.RAW_TABLE);
        const topZ = 3;

        var getList = (val, type, valDefault) => {
            var colType = (!type || (type === 'numeric')) ?
                ['double', 'd', 'long', 'l', 'int', 'i', 'float', 'f'] : ['char', 'c', 's', 'str'];

            return get(tblModel, ['tableData', 'columns']).reduce((prev, col) => {
                if ((colType.includes(col.type)) &&
                    (!has(col, 'visibility') || get(col, 'visibility') !== 'hidden') &&
                    (col.name.startsWith(val) || val === valDefault)) {
                    prev.push(col.name);
                }
                return prev;
            }, []);
        };

        var missionInputs = missionKeys.map((key) =>
            <SuggestBoxInputField key={key} fieldKey={key} wrapperStyle={wrapperStyle} popupIndex={topZ}
                                  getSuggestions={(val) => getList(val)}/>
        );

        var missionData = missionUrl.map((key) =>
            <SuggestBoxInputField key={key} fieldKey={key} wrapperStyle={wrapperStyle} popupIndex={topZ}
                                  getSuggestions={(val) => getList(val, 'char', '')}/>
        );

        var missionOthers = missionOtherKeys.map((key) =>
            <ValidationField key={key} fieldKey={key} wrapperStyle={wrapperStyle}/>
        );

        const groupKey = getViewerGroupKey(missionEntries);
        const converterId = get(missionEntries, LC.META_MISSION);

        return (
            <FieldGroup groupKey={groupKey}
                        reducerFunc={basicOptionsReducer(missionEntries, generalEntries)} keepState={true}>
                <div style={{display: 'flex', flexDirection: 'column'}}>
                    {getMissionName(converterId) !== '' &&
                    <ReadOnlyText label='Mission:' content={getMissionName(converterId)}
                                  labelWidth={labelWidth} wrapperStyle={{margin: '3px 0 6px 0'}}/>}
                    <div style={{display: 'flex'}}>
                        <div>
                            {missionInputs}
                            {/*missionData*/}
                        </div>
                    </div>
                </div>
            </FieldGroup>
        );
    }
}

BasicSettingBox.propTypes = {
    generalEntries: PropTypes.object,
    missionEntries: PropTypes.object
};

function getStringCol(rawTbl) {
    var colType = ['char', 'c', 's', 'str'];
    let cols = get(rawTbl, 'tableData.columns');
    let charCols = cols.filter((c) => {
        return colType.includes(c.type)
    });
    let c = [];
    charCols.forEach((el) => {
        c.push(el.name);
    });
    return c;
}

function getNumCols(rawTbl) {
    var colType = ['double', 'd', 'long', 'l', 'int', 'i', 'float', 'f'];
    let cols = get(rawTbl, 'tableData.columns');
    let charCols = cols.filter((c) => {
        return colType.includes(c.type)
    });
    let c = [];
    charCols.forEach((el) => {
        c.push(el.name);
    });
    return c;
}

export const basicOptionsReducer = (missionEntries, generalEntries) => {
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
                'Cutout Size (deg):', 100)),
            [LC.META_URL_CNAME]: Object.assign(getTypeData(LC.META_URL_CNAME, '',
                'Image url column name',
                'Source Column:', labelWidth))
        };

        var defV = Object.assign({}, defValues);
        const validators = getFieldValidators(missionEntries, getTblById(LC.RAW_TABLE));

        const missionKeys = [LC.META_TIME_CNAME, LC.META_FLUX_CNAME, LC.META_URL_CNAME, LC.META_ERR_CNAME,
            LC.META_COORD_XNAME, LC.META_COORD_YNAME, LC.META_COORD_SYS];
        const missionListKeys = [LC.META_TIME_NAMES, LC.META_FLUX_NAMES];

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

        /*
         Object.keys(generalEntries).forEach((key) => {
         set(defV, [key, 'value'], get(generalEntries, key, ''));
         });
         */
        return defV;
    };
};


function getFieldValidators(missionEntries, rawTable) {

    const fldsWithValidators = [
        {key: LC.META_TIME_CNAME, vkey: LC.META_TIME_NAMES},
        {key: LC.META_FLUX_CNAME, vkey: LC.META_FLUX_NAMES},
        {key: LC.META_URL_CNAME, vkey: LC.META_URL_NAMES}
    ];
    return fldsWithValidators.reduce((all, fld) => {
        all[fld.key] =
            (val) => {
                let retVal = {valid: true, message: ''};
                const cols = get(missionEntries, fld.vkey, []);
                if (cols.length === 0 && rawTable) {
                    if (getColumnIdx(rawTable, val) < 0) {
                        retVal = {valid: false, message: `${val} is not a valid column name`};
                    }
                } else if (!cols.includes(val)) {
                    retVal = {valid: false, message: `${val} is not a valid column name`};
                }
                return retVal;
            };
        return all;
    }, {});
}

const posColumnInfo = (posCoords) => {
    const posElement = [LC.META_COORD_XNAME, LC.META_COORD_YNAME, LC.META_COORD_SYS];
    var posInfo = posElement.reduce((prev, ele) => {
        prev[ele] = '';
        return prev;
    }, {});

    if (posCoords) {
        var s = posCoords.split(';');
        if (s && s.length === 3) {
            s.forEach((ele, idx) => posInfo[posElement[idx]] = ele);
        }
    }

    return posInfo;
};

/**
 * Return true if the user inputs make sense to show images in result layout
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

    let numericalCols = getNumCols(rawTable);

    //let strCols = getStringCol(rawTable);
    //strCols.push('');// Empty means no images
    let defaultCTimeName = (getColumnIdx(rawTable, converterData.defaultTimeCName) > 0) ? converterData.defaultTimeCName : numericalCols[0];
    let defaultYColName = (getColumnIdx(rawTable, converterData.defaultYCname) > 0) ? converterData.defaultYCname : numericalCols[1];

    defaultCTimeName = numericalCols.filter((el) => {
            if (el.toLocaleLowerCase().match(xyColPattern[0]) != null) {
                return el;
            }
        })[0] || defaultCTimeName;
    const defaultValues = {
        [LC.META_TIME_CNAME]: get(metaInfo, LC.META_TIME_CNAME, defaultCTimeName),
        [LC.META_FLUX_CNAME]: get(metaInfo, LC.META_FLUX_CNAME, defaultYColName)
        //[LC.META_URL_CNAME]: get(metaInfo, LC.META_URL_CNAME, converterData.dataSource),
        //[LC.META_URL_NAMES]: get(metaInfo, LC.META_URL_NAMES, strCols)
    };

    missionEntries = Object.assign({}, missionEntries, defaultValues);
    const newLayoutInfo = smartMerge(layoutInfo, {missionEntries, generalEntries});
    return {newLayoutInfo, shouldContinue: false};
}

export function basicRawTableRequest(converter, source) {
    const options = {
        tbl_id: LC.RAW_TABLE,
        sortInfo: sortInfoString('mjd'),   //TODO: tentative solution to get around 'ROWID' issue
        tblType: 'notACatalog',
        META_INFO: {[LC.META_MISSION]: converter.converterId},
        pageSize: LC.TABLE_PAGESIZE
    };
    return makeFileRequest('Input Data', source, null, options);

}

export function basicOnFieldUpdate(fieldKey, value) {
    if ([LC.META_TIME_CNAME, LC.META_FLUX_CNAME].includes(fieldKey)) {
        return {[fieldKey]: value};
    }
}