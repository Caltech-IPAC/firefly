import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {get, has, isEmpty, set} from 'lodash';
import {flux} from '../../../Firefly.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {SuggestBoxInputField} from '../../../ui/SuggestBoxInputField.jsx';
import {smartMerge, makeFileRequest, getNumericColNames, getStringColNames} from '../../../tables/TableUtil.js';
import {ReadOnlyText, getTypeData} from '../LcUtil.jsx';
import {LC, getViewerGroupKey,  onTimeColumnChange} from '../LcManager.js';
import {getMissionName, coordSysOptions} from '../LcConverterFactory.js';
import {getLayouInfo} from '../../../core/LayoutCntlr.js';

const labelWidth = 90;

export class DefaultSettingBox extends Component {
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
        const missionOtherKeys = [ LC.META_ERR_CNAME];
        const topZ = 3;

        var getList = (val, type) => {
            const selCols = (!type || (type === 'numeric')) ? numColumns : charColumns;
            return selCols.reduce((prev, col) => {
                        if ((col.startsWith(val))) {
                            prev.push(col);
                        }
                        return prev;
                    }, []);
        };

        var missionInputs = missionKeys.map((key) =>
            <SuggestBoxInputField key={key} fieldKey={key} wrapperStyle={wrapperStyle} popupIndex={topZ}
                                  getSuggestions={(val) => getList(val)} />
        );

        var missionData = missionUrl.map((key) =>
            <SuggestBoxInputField key={key} fieldKey={key} wrapperStyle={wrapperStyle} popupIndex={topZ}
                                  getSuggestions={(val) => getList(val, 'char')} />
        );

        var missionOthers = missionOtherKeys.map((key) =>
            <ValidationField key={key} fieldKey={key} wrapperStyle={wrapperStyle}/>
        );

        var missionPosEntries = () => {
            var sKey = LC.META_COORD_SYS;

            var sysCol = (
                <SuggestBoxInputField key={sKey} fieldKey={sKey} wrapperStyle={wrapperStyle}
                                      getSuggestions={() =>get(missionEntries, coordSysOptions, [])} />
            );
            var xyCols = [LC.META_COORD_XNAME, LC.META_COORD_YNAME].map((key) =>
                <SuggestBoxInputField key={key} fieldKey={key} wrapperStyle={wrapperStyle} popupIndex={topZ}
                                      getSuggestions={(val) => getList(val)} />
            );

            return [sysCol, xyCols];
        };

        const groupKey = getViewerGroupKey(missionEntries);
        const converterId = get(missionEntries, LC.META_MISSION);
        const typeColumns = {charColumns, numColumns};

        return (
            <FieldGroup groupKey={groupKey}
                        reducerFunc={defaultOptionsReducer(missionEntries, generalEntries, typeColumns)} keepState={true}>
                <div style={{display: 'flex', flexDirection: 'column'}} >
                    <ReadOnlyText label='Mission:' content={getMissionName(converterId)}
                                  labelWidth={labelWidth} wrapperStyle={{margin: '3px 0 6px 0'}}/>
                    <div style={{display: 'flex'}}>
                        <div>
                            {missionInputs}
                            {missionData}
                            {missionOthers}
                        </div>
                        <div>
                            {missionPosEntries()}
                        </div>
                    </div>
                </div>
            </FieldGroup>
        );
    }
}

DefaultSettingBox.propTypes = {
    generalEntries: PropTypes.object,
    missionEntries: PropTypes.object
};


export const defaultOptionsReducer = (missionEntries, generalEntries, typeColumns) => {
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
            [LC.META_ERR_CNAME]: Object.assign(getTypeData(LC.META_ERR_CNAME, '',
                'flux error column name',
                'Error Column:', labelWidth)),
            [LC.META_URL_CNAME]: Object.assign(getTypeData(LC.META_URL_CNAME, '',
                'Image url column name',
                'Source Column:', labelWidth)),
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

        var   defV = Object.assign({}, defValues);
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
        {key: LC.META_TIME_CNAME},
        {key: LC.META_FLUX_CNAME},
        {key: LC.META_URL_CNAME, type: 'char'},
        {key: LC.META_COORD_XNAME},
        {key: LC.META_COORD_YNAME},
        {key: LC.META_COORD_SYS, vkey: coordSysOptions}
        //{key: LC.META_ERR_CNAME, vkey: LC.META_ERR_NAMES} // error can have no value
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


export function defaultOnNewRawTable(rawTable, missionEntries, generalEntries, converterData, layoutInfo={}) {
    const metaInfo = rawTable && rawTable.tableMeta;
    var posInfo = posColumnInfo(get(metaInfo, LC.META_POS_COORD));

    const addtlEntries = {
        [LC.META_URL_CNAME]: get(metaInfo, LC.META_URL_CNAME, converterData.dataSource),
        [LC.META_COORD_XNAME]: get(posInfo, LC.META_COORD_XNAME),
        [LC.META_COORD_YNAME]: get(posInfo, LC.META_COORD_YNAME),
        [LC.META_COORD_SYS]: get(posInfo, LC.META_COORD_SYS),
        [coordSysOptions]: get(converterData, coordSysOptions)
    };

    missionEntries = Object.assign({}, missionEntries, addtlEntries);
    return {shouldContinue: true, newLayoutInfo: smartMerge(layoutInfo, {missionEntries, generalEntries})};
}

export function defaultRawTableRequest(converter, source) {
    const options = {
        tbl_id: LC.RAW_TABLE,
        tblType: 'notACatalog',
        pageSize: LC.TABLE_PAGESIZE
    };
    return makeFileRequest('Input Data', source, null, options);

}

export function defaultOnFieldUpdate(fieldKey, value) {
    if ([LC.META_TIME_CNAME].includes(fieldKey)) {
        const {missionEntries} = getLayouInfo() || {};
        if (!missionEntries) return;

        onTimeColumnChange(missionEntries[fieldKey], value);
        return {[fieldKey]: value};
    } else if ([LC.META_FLUX_CNAME, LC.META_ERR_CNAME, LC.META_URL_CNAME,
            LC.META_COORD_XNAME, LC.META_COORD_YNAME, LC.META_COORD_SYS].includes(fieldKey)) {
        return {[fieldKey] : value};
    }
}
