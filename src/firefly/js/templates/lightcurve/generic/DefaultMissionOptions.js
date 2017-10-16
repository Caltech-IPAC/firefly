import React from 'react';
import PropTypes from 'prop-types';
import {get, isEmpty} from 'lodash';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {SuggestBoxInputField} from '../../../ui/SuggestBoxInputField.jsx';
import {smartMerge} from '../../../tables/TableUtil.js';
import {makeFileRequest} from '../../../tables/TableRequestUtil.js';
import {ReadOnlyText,getSuggestedList,getInitialDefaultValues,getMissionInput,validate,fileUpdateOnTimeColumn,setValueAndValidator} from '../LcUtil.jsx';
import {LC, getViewerGroupKey} from '../LcManager.js';
import {getMissionName, coordSysOptions} from '../LcConverterFactory.js';
import {SettingBox} from '../SettingBox.jsx';

const labelWidth = 90;

export class DefaultSettingBox extends SettingBox {
    constructor(props) {
        super(props);

    }

    render() {
        var {generalEntries, missionEntries} = this.props;
        var {tblColumns, numColumns, charColumns} = this.state;

        if (isEmpty(tblColumns) || isEmpty(generalEntries) || isEmpty(missionEntries)) return false;

        const wrapperStyle = {margin: '3px 0'};

        const missionUrl = [LC.META_URL_CNAME];
        const missionOtherKeys = [ LC.META_ERR_CNAME];
        const topZ = 3;

        const  missionInputs=getMissionInput (numColumns, wrapperStyle);
        var missionData = missionUrl.map((key) =>
            <SuggestBoxInputField key={key} fieldKey={key} wrapperStyle={wrapperStyle} popupIndex={topZ}
                                  getSuggestions={(val) => getSuggestedList(val, charColumns)} />
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
                                      getSuggestions={(val) => getSuggestedList(val,  charColumns)} />
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
        const defValues = getInitialDefaultValues(labelWidth, 'generic');
        var   defV = Object.assign({}, defValues);
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
             {key: LC.META_TIME_CNAME},
             {key: LC.META_FLUX_CNAME},
             {key: LC.META_URL_CNAME, type: 'char'},
             {key: LC.META_COORD_XNAME},
             {key: LC.META_COORD_YNAME},
             {key: LC.META_COORD_SYS, vkey: coordSysOptions}
        ];

    return validate(fldsWithValidators, missionEntries, typeColumns );

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
        return fileUpdateOnTimeColumn(fieldKey, value);
    } else if ([LC.META_FLUX_CNAME, LC.META_ERR_CNAME, LC.META_URL_CNAME,
            LC.META_COORD_XNAME, LC.META_COORD_YNAME, LC.META_COORD_SYS].includes(fieldKey)) {
        return {[fieldKey] : value};
    }
}
