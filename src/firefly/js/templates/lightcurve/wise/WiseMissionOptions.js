import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get,  set, isNil} from 'lodash';
import {RadioGroupInputField} from '../../../ui/RadioGroupInputField.jsx';
import {getCellValue, getTblById, getColumnIdx} from '../../../tables/TableUtil.js';
import {getInitialDefaultValues,renderMissionView,validate,setValueAndValidator} from '../LcUtil.jsx';

import {LC} from '../LcManager.js';


const labelWidth = 80;

export class WiseSettingBox extends PureComponent {
    constructor(props) {
        super(props);


    }

    render() {
        var {generalEntries, missionEntries} = this.props;
        const tblModel = getTblById(LC.RAW_TABLE);
        const wrapperStyle = {margin: '3px 0'};

        var missionBands = (<RadioGroupInputField key='band'
                                                  fieldKey={LC.META_FLUX_BAND} wrapperStyle={wrapperStyle}
                                                  alignment='horizontal'
                                                  options={[
                    {label: 'W1', value: 'w1'},
                    {label: 'W2', value: 'w2'},
                    {label: 'W3', value: 'w3'},
                    {label: 'W4', value: 'w4'}
                ]}/>);

         return renderMissionView({generalEntries,missionEntries,missionBands,tblModel,wrapperStyle,labelWidth, callback:wiseOptionsReducer });


    }
}

WiseSettingBox.propTypes = {
    generalEntries: PropTypes.object,
    missionEntries: PropTypes.object
};


export const wiseOptionsReducer = (missionEntries, generalEntries) => {

    const labelWidth = 80;

    return (inFields, action) => {
        if (inFields) {
            return inFields;
        }

        // defValues used to keep the initial values for parameters in the field group of result page
        const defValues =getInitialDefaultValues(labelWidth,'wise');

        var defV = Object.assign({}, defValues);

        const missionKeys = [LC.META_TIME_CNAME, LC.META_FLUX_CNAME, LC.META_URL_CNAME];
        const missionListKeys = [LC.META_TIME_NAMES, LC.META_FLUX_NAMES];
        const validators = getFieldValidators(missionEntries);


        setValueAndValidator(missionListKeys, missionEntries,missionKeys, validators, defV);
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

    return validate(fldsWithValidators, missionEntries);

}

export function isValidWiseTable(){

    const tableModel = getTblById(LC.RAW_TABLE);

    // For images from AllWise:
    const frameId = getCellValue(tableModel, 0, 'frame_id');

    // For other single exposure tables (NEOWISE, etc)
    const frameNum = getCellValue(tableModel, 0, 'frame_num');
    const scanId = getCellValue(tableModel, 0, 'scan_id');
    const sourceId = getCellValue(tableModel, 0, 'source_id');

    if (!isNil(frameId) || !isNil(sourceId) || (!isNil(scanId) && !isNil(frameNum)) ){
        return {errorMsg:undefined, isValid:true};
    }
    else {
        const errorMsg=`The uploaded table is not valid. The WISE  option requires frame_id, or source_id, or both scan_id and frame_num.
                Please select the "Other" upload option for tables that do not meet these requirements.`;
        return {errorMsg, isValid:false};
    }
}

export function wiseYColMappings(tbl_id, yCol) {
    const tableModel = getTblById(tbl_id);

    if (['w1mpro_ep', 'w2mpro_ep', 'w3mpro_ep', 'w4mpro_ep'].includes(yCol)) {
        const yUnc = yCol.replace('mpro', 'sigmpro');
        if (getColumnIdx(tableModel, yUnc)>=0) {
            return {
                // when using ycol for y, column value mapping is transparent:
                // the limit is represented only by the annotation;
                // when using an expression (dash instead of a point for upper limits),
                // yTTLabelSrc needs to be set to avoid showing the truncated expression in point tooltip
                y: `nvl2(${yUnc}, ${yCol}, NULL)`,
                yMin: `nvl2(${yUnc}, NULL, ${yCol})`
            };
        }
    }
    return {y: yCol};
}
