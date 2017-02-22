import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {get, has, isEmpty, set} from 'lodash';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../../fieldGroup/FieldGroupUtils';
import {dispatchMultiValueChange} from '../../../fieldGroup/FieldGroupCntlr.js';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {SuggestBoxInputField} from '../../../ui/SuggestBoxInputField.jsx';
import {makeFileRequest, getTblById, smartMerge} from '../../../tables/TableUtil.js';
import {sortInfoString} from '../../../tables/SortInfo.js';
import {ReadOnlyText, getTypeData} from '../LcUtil.jsx';
import {LC, getViewerGroupKey} from '../LcManager.js';
import {getMissionName} from '../LcConverterFactory.js';


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
        var   missionName = getMissionName(converterId) || 'Mission';

        return (
            <FieldGroup groupKey={groupKey}
                        reducerFunc={wiseOptionsReducer(missionEntries, generalEntries)} keepState={true}>
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
            [LC.META_TIME_CNAME]: Object.assign(getTypeData(LC.META_TIME_CNAME, '',
                'time column name',
                'Time Column:', labelWidth),
                {validator: null}),
            [LC.META_FLUX_CNAME]: Object.assign(getTypeData(LC.META_FLUX_CNAME, '',
                'flux column name',
                'Flux Column:', labelWidth),
                {validator: null}),
            [LC.META_TIME_NAMES]: Object.assign(getTypeData(LC.META_TIME_NAMES, '',
                'time column suggestion'),
                {validator: null}),
            [LC.META_FLUX_NAMES]: Object.assign(getTypeData(LC.META_FLUX_NAMES, '',
                'flux column suggestion'),
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

export function wiseOnNewRawTable(rawTable, missionEntries, generalEntries, layoutInfo) {
    const newLayoutInfo = smartMerge(layoutInfo, {missionEntries, generalEntries});
    return {newLayoutInfo, shouldContinue: true};
}

export function wiseRawTableRequest(converter, source) {
    const timeCName = converter.defaultTimeCName;
    const mission = converter.converterId;
    const options = {
        tbl_id: LC.RAW_TABLE,
        sortInfo: sortInfoString(timeCName),
        META_INFO: {[LC.META_MISSION]: mission, timeCName},
        pageSize: LC.TABLE_PAGESIZE
    };
    return makeFileRequest('Raw Table', source, null, options);

}

export function wiseOnFieldUpdate(fieldKey, value) {
    if ([LC.META_TIME_CNAME, LC.META_FLUX_CNAME, LC.META_ERR_CNAME].includes(fieldKey)) {
        return {[fieldKey] : value};
    }
}
