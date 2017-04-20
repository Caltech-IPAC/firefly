import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {flux} from '../../../Firefly.js';
import {get, has, isEmpty, set} from 'lodash';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {SuggestBoxInputField} from '../../../ui/SuggestBoxInputField.jsx';
import {makeFileRequest} from '../../../tables/TableUtil.js';
import {getColumnIdx, smartMerge, getNumericColNames, getStringColNames,getTblById} from '../../../tables/TableUtil.js';
import {ReadOnlyText, getTypeData} from '../LcUtil.jsx';
import {LC, getViewerGroupKey, onTimeColumnChange} from '../LcManager.js';
import {getMissionName} from '../LcConverterFactory.js';
import {getLayouInfo} from '../../../core/LayoutCntlr.js';
import FieldGroupUtils from '../../../fieldGroup/FieldGroupUtils';
import {ServerParams} from '../../../data/ServerParams.js';

const labelWidth = 90;

export class BasicSettingBox extends Component {
    constructor(props) {
        super(props);

        this.getNextState = () => {
            return Object.assign({}, {tblColumns: get(getLayouInfo(), 'rawTableColumns', [])});
        };

        const {tblColumns} = this.getNextState();
        const numColumns = getNumericColNames(tblColumns);
        const charColumns = getStringColNames(tblColumns);
        this.state = {tblColumns, charColumns, numColumns};
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        this.removeListener && this.removeListener();
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const {tblColumns} = this.getNextState();

            if (tblColumns !== this.state.tblColumns) {
                const numColumns = getNumericColNames(tblColumns);
                const charColumns = getStringColNames(tblColumns);
                this.setState({tblColumns, charColumns, numColumns});
            }
        }
    }

    render() {
        var {generalEntries, missionEntries} = this.props;
        var {tblColumns, numColumns, charColumns} = this.state;

        if (isEmpty(tblColumns) || isEmpty(generalEntries) || isEmpty(missionEntries)) return false;

        const wrapperStyle = {margin: '3px 0'};
        const missionKeys = [LC.META_TIME_CNAME, LC.META_FLUX_CNAME];
        const missionUrl = [LC.META_URL_CNAME];
        const missionOtherKeys = [LC.META_ERR_CNAME];
        const topZ = 3;

        var getList = (val, type, valDefault) => {
            const selCols = (!type || (type === 'numeric')) ? numColumns : charColumns;

            return selCols.reduce((prev, col) => {
                if ((col.startsWith(val) || val === valDefault)) {
                    prev.push(col);
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
        const typeColumns = {charColumns, numColumns};
        const tblModel = getTblById(LC.RAW_TABLE);
        //const periodFlds = FieldGroupUtils.getGroupFields(LC.FG_PERIOD_FINDER);
        const layoutInfo = getLayouInfo();
        var period = get(layoutInfo, ['periodRange','period'], '');
        return (
            <FieldGroup groupKey={groupKey}
                        reducerFunc={basicOptionsReducer(missionEntries, generalEntries, typeColumns)} keepState={true}>

                <div >
                    <div style={{ fontWeight:'bold', display:'inline-block'}} > Column Selection</div>
                    <div style = {{paddingLeft:'10px', display:'inline-block'}}>{tblModel.request.META_INFO.title} </div>

                </div>
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
                <div style={{ paddingBottom:'5px'}} > Period:    {period} </div>
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
        const validators = getFieldValidators(missionEntries, typeColumns);

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


function getFieldValidators(missionEntries, typeColumns) {

    const fldsWithValidators = [
        {key: LC.META_TIME_CNAME, vkey: LC.META_TIME_NAMES},
        {key: LC.META_FLUX_CNAME, vkey: LC.META_FLUX_NAMES},
        {key: LC.META_URL_CNAME, vkey: LC.META_URL_NAMES, type:'char'}
    ];

    return fldsWithValidators.reduce((all, fld) => {
        all[fld.key] =
            (val) => {
                let valid = true;
                const cols = get(missionEntries, fld.vkey, []);

                if (cols.length === 0) {
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
    const numericalCols = getNumericColNames(get(rawTable, ['tableData', 'columns'], []));

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
        //[LC.META_URL_CNAME]: get(metaInfo, LC.META_URL_CNAME, converterData.dataSource),
        //[LC.META_URL_NAMES]: get(metaInfo, LC.META_URL_NAMES, strCols)
    };

    missionEntries = Object.assign({}, missionEntries, defaultValues);
    const newLayoutInfo = smartMerge(layoutInfo, {missionEntries, generalEntries});
    return {newLayoutInfo, shouldContinue: true};
}

export function basicRawTableRequest(converter, source) {
    const options = {
        tbl_id: LC.RAW_TABLE,
        tblType: 'notACatalog',
        META_INFO: {[LC.META_MISSION]: converter.converterId},
        pageSize: LC.TABLE_PAGESIZE
    };

    var req = makeFileRequest('Input Data', source, null, options);
    req[ServerParams.USE_UPLOADED_FILENAME_AS_TABLE_TITLE]=true;
    return req;

}

export function basicOnFieldUpdate(fieldKey, value) {
    if ([LC.META_TIME_CNAME].includes(fieldKey)) {
        const {missionEntries}= getLayouInfo() || {};
        if (!missionEntries) return;

        onTimeColumnChange(missionEntries[fieldKey], value);
        return {[fieldKey]: value};
    } else if ([LC.META_FLUX_CNAME].includes(fieldKey)) {
        return {[fieldKey]: value};
    }
}