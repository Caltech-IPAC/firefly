import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {get, has, isEmpty, set} from 'lodash';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {dispatchMultiValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import {makeFileRequest} from '../../tables/TableUtil.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {ReadOnlyText, getTypeData} from './LcUtil.jsx';
import {LC, getViewerGroupKey} from './LcManager.js';
import {getMissionName} from './LcConverterFactory.js';


const labelWidth = 80;

export class DefaultSettingBox extends Component {
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

        var allCommonEntries = Object.keys(generalEntries).map((key) =>
            <ValidationField key={key} fieldKey={key} wrapperStyle={wrapperStyle}
                             style={{width: 80}}/>
        );

        const missionKeys = [LC.META_TIME_CNAME, LC.META_FLUX_CNAME];
        const missionOtherKeys = [LC.META_ERR_CNAME];
        const missionListKeys = [LC.META_TIME_NAMES, LC.META_FLUX_NAMES];

        var missionInputs = missionKeys.map((key, index) =>
            <SuggestBoxInputField key={key} fieldKey={key} wrapperStyle={wrapperStyle}
                                  getSuggestions={(val) => {
                                    const list = get(missionEntries, missionListKeys[index], []);
                                    const suggestions =  list && list.filter((el) => {return el.startsWith(val);});
                                    return suggestions.length > 0 ? suggestions : list;
                                  }}
            />
        );

        var missionOthers = missionOtherKeys.map((key) =>
            <ValidationField key={key} fieldKey={key} wrapperStyle={wrapperStyle}/>
        );


        const groupKey = getViewerGroupKey(missionEntries);
        const converterId = get(missionEntries, LC.META_MISSION);
        const missionName = getMissionName(converterId);

        return (
            <FieldGroup groupKey={groupKey}
                        reducerFunc={defaultOptionsReducer(missionEntries, generalEntries)} keepState={true}>
                <div style={{display: 'flex', alignItems: 'flex-end'}} >
                    <div >
                        <ReadOnlyText label='Mission:' content={missionName}
                                      labelWidth={labelWidth} wrapperStyle={{margin: '3px 0 6px 0'}}/>

                        {missionInputs}
                        {missionOthers}
                    </div>
                    <div>
                        {allCommonEntries}
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


export const defaultOptionsReducer = (missionEntries, generalEntries) => {
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
                'Error Column:', labelWidth))
        };

        var   defV = Object.assign({}, defValues);

        const missionKeys = [LC.META_TIME_CNAME, LC.META_FLUX_CNAME];
        const missionListKeys = [LC.META_TIME_NAMES, LC.META_FLUX_NAMES];

        missionListKeys.forEach((key) => {
            set(defV, [key, 'value'], get(missionEntries, key, []));
        });

        // set value and validator
        missionKeys.forEach((key, idx) => {
            set(defV, [key, 'value'], get(missionEntries, key, ''));
            set(defV, [key, 'validator'], (val) => {
                let retVal = {valid: true, message: ''};
                        const cols = get(missionEntries, missionListKeys[idx], []);

                if (cols.length !== 0 && !cols.includes(val)) {
                    retVal = {valid: false, message: `${val} is not a valid column name`};
                }

                return retVal;
            });
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
        {key: LC.META_FLUX_CNAME, vkey: LC.META_FLUX_NAMES}
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



export function defaultOnNewRawTable(rawTable, converterData, generalEntries) {
    const metaInfo = rawTable && rawTable.tableMeta;
    const missionEntries = {
        [LC.META_MISSION]: converterData.converterId,
        [LC.META_TIME_CNAME]: get(metaInfo, LC.META_TIME_CNAME, converterData.defaultTimeCName),
        [LC.META_FLUX_CNAME]: get(metaInfo, LC.META_FLUX_CNAME, converterData.defaultYCname),
        [LC.META_ERR_CNAME]: get(metaInfo, LC.META_ERR_CNAME, converterData.defaultYErrCname),
        [LC.META_TIME_NAMES]: get(metaInfo, LC.META_TIME_NAMES, converterData.timeNames),
        [LC.META_FLUX_NAMES]: get(metaInfo, LC.META_FLUX_NAMES, converterData.yNames),
        [LC.META_ERR_NAMES]: get(metaInfo, LC.META_ERR_NAMES, converterData.yErrNames)
    };
    setFields(missionEntries, generalEntries);
    return missionEntries;
}

export function defaultRawTableRequest(converter, source) {
    const timeCName = converter.defaultTimeCName;
    const mission = converter.converterId;
    const options = {
        tbl_id: LC.RAW_TABLE,
        tblType: 'notACatalog',
        sortInfo: sortInfoString(timeCName),
        META_INFO: {[LC.META_MISSION]: mission, timeCName},
        pageSize: LC.TABLE_PAGESIZE
    };
    return makeFileRequest('Input Data', source, null, options);

}

export function defaultOnFieldUpdate(fieldKey, value) {
    if ([LC.META_TIME_CNAME, LC.META_FLUX_CNAME, LC.META_ERR_CNAME].includes(fieldKey)) {
        return {[fieldKey] : value};
    }
}
