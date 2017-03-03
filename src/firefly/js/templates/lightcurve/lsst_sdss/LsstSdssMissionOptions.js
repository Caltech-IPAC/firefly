import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {get, isEmpty, set, pick, cloneDeep, defer} from 'lodash';
import {getLayouInfo} from '../../../core/LayoutCntlr.js';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {SuggestBoxInputField} from '../../../ui/SuggestBoxInputField.jsx';
import {RadioGroupInputField} from '../../../ui/RadioGroupInputField.jsx';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {makeFileRequest, makeTblRequest, smartMerge} from '../../../tables/TableUtil.js';
import {dispatchTableSearch} from '../../../tables/TablesCntlr.js';
import {sortInfoString} from '../../../tables/SortInfo.js';
import {FilterInfo} from '../../../tables/FilterInfo.js';

import {LC, getViewerGroupKey} from '../LcManager.js';
import {getTypeData} from './../LcUtil.jsx';

const labelWidth = 100;

export class LsstSdssSettingBox extends Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }


    render() {
        const {generalEntries, missionEntries} = this.props;


        if (isEmpty(generalEntries) || isEmpty(missionEntries)) return false;
        const wrapperStyle = {margin: '3px 0'};

        var rightEntries = Object.keys(generalEntries).map((key) =>
            <ValidationField key={key} fieldKey={key} wrapperStyle={wrapperStyle} style={{width: 80}}/>
        );

        const validFluxVals = get(missionEntries, LC.META_FLUX_NAMES, []);

        var leftEntries = [
            <RadioGroupInputField key='band' fieldKey='band' wrapperStyle={wrapperStyle}
                alignment='horizontal'
                options={[
                    {label: 'u', value: 'u'},
                    {label: 'g', value: 'g'},
                    {label: 'r', value: 'r'},
                    {label: 'i', value: 'i'},
                    {label: 'z', value: 'z'}
                ]}
            />,
            <SuggestBoxInputField key={LC.META_FLUX_CNAME}
                fieldKey={LC.META_FLUX_CNAME} wrapperStyle={wrapperStyle}
                getSuggestions={(val) => {
                    const suggestions =  validFluxVals && validFluxVals.filter((el) => {return el.startsWith(val);});
                    return suggestions.length > 0 ? suggestions : validFluxVals;
                }}
            />
        ];

        const groupKey = getViewerGroupKey(missionEntries);

        return (
            <FieldGroup groupKey={groupKey}
                        reducerFunc={lsstSdssReducer(missionEntries, generalEntries)} keepState={true}>
                <div style={{display: 'flex', alignItems: 'flex-end'}} >
                    <div >
                        {leftEntries}
                    </div>
                    <div>
                        {rightEntries}
                    </div>
                </div>
            </FieldGroup>
        );
    }
}

LsstSdssSettingBox.propTypes = {
    generalEntries: PropTypes.object,
    missionEntries: PropTypes.object
};

export const lsstSdssReducer = (missionEntries, generalEntries) => {
    return (inFields, action) => {
        if (inFields) {
            return inFields;
        }

        const validFluxVals = get(missionEntries, LC.META_FLUX_NAMES, []);
        const fluxFldValidator = (val) => {
            let retVal = {valid: true, message: ''};
            if (validFluxVals.length !== 0 && !validFluxVals.includes(val)) {
                retVal = {valid: false, message: `${val} is not a valid column name`};
            }
            return retVal;
        };

        // defValues used to keep the initial values for parameters in the field group of result page
        // band: lsst sdss band name
        // flux: flux column
        // cutoutsize: image cutout size

        const defValues = {
            band: Object.assign(getTypeData('band', '',
                'LSST SDSS band',
                'LSST SDSS Band:', labelWidth)),
            [LC.META_FLUX_CNAME]: Object.assign(getTypeData(LC.META_FLUX_CNAME, '',
                'Y column name',
                'Periodic Column:', labelWidth),
                {validator: fluxFldValidator}),
            ['cutoutSize']: Object.assign(getTypeData('cutoutSize', '',
                'image cutout size',
                'Cutout Size (deg):', labelWidth)),
            [LC.META_ERR_CNAME]: Object.assign(getTypeData(LC.META_ERR_CNAME, '',
                'flux error column name',
                'Error Column:', labelWidth))
        };

        var   defV = Object.assign({}, defValues);

        const missionKeys = ['band', LC.META_FLUX_CNAME];

        // set value
        missionKeys.forEach((key) => {
            set(defV, [key, 'value'], get(missionEntries, key, ''));
        });
        Object.keys(generalEntries).forEach((key) => {
            set(defV, [key, 'value'], get(generalEntries, key, ''));
        });
        return defV;
    };
};


export function lsstSdssOnNewRawTable(rawTable, missionEntries, generalEntries, converterData, layoutInfo={}) {
    const {band, lsst_filtered_band} = get(rawTable, 'request.META_INFO');
    var {rawTableRequest} = layoutInfo;

    missionEntries.band = band || 'u';
    missionEntries.lsst_filtered_band = lsst_filtered_band;

    if (lsst_filtered_band && band === lsst_filtered_band) {
        return {shouldContinue: true, newLayoutInfo: smartMerge(layoutInfo, {missionEntries, generalEntries})};
    };

    if (!lsst_filtered_band) {
        missionEntries.lsst_filtered_band = band;
        rawTableRequest = cloneDeep(rawTable.request);
    }
    const treq = makeRawTableRequest(missionEntries, rawTableRequest);
    defer(() => dispatchTableSearch(treq, {removable: true}));
    return {shouldContinue: false, newLayoutInfo: smartMerge(layoutInfo, {missionEntries, generalEntries, rawTableRequest})};
}

export function lsstSdssOnFieldUpdate(fieldKey, value) {
    const {missionEntries, rawTableRequest} = getLayouInfo() || {};
    if (!missionEntries) return;
    if (fieldKey === 'band' || fieldKey === LC.META_TIME_CNAME) {
        missionEntries[fieldKey] = value;
        const treq = makeRawTableRequest(missionEntries, rawTableRequest);
        defer(() => dispatchTableSearch(treq, {removable: true}));
        return {[fieldKey]: value};
    } else if ([LC.META_FLUX_CNAME, LC.META_ERR_CNAME].includes(fieldKey)) {
        return {[fieldKey]: value};
    }
}

export function lsstSdssRawTableRequest(converter, source) {
    const options = {
        tbl_id: LC.RAW_TABLE,
        META_INFO: {[LC.META_MISSION]: converter.converterId},
        pageSize: LC.TABLE_PAGESIZE
    };

    return makeFileRequest('Raw Table', source, null, options);
}

function makeRawTableRequest(missionEntries, rawTableRequest) {
    const band = missionEntries['band'];
    const filterInfo = new FilterInfo;
    filterInfo.addFilter('filterName', `LIKE ${band}`);
    var searchRequest = cloneDeep(rawTableRequest);
    searchRequest.filters = filterInfo.serialize();
    searchRequest = JSON.stringify(searchRequest);
    const options = {
        tbl_id: LC.RAW_TABLE,
        sortInfo: sortInfoString(missionEntries[LC.META_TIME_CNAME]),
        META_INFO: {band, lsst_filtered_band: band, ...pick(missionEntries, [LC.META_MISSION, LC.META_TIME_CNAME, LC.META_FLUX_CNAME])},
        pageSize: LC.TABLE_PAGESIZE
    };
    return makeTblRequest('IpacTableFromSource', `Raw Table ${band}`, {searchRequest}, options);
}