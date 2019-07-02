import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get,  set, isNil} from 'lodash';
import {getCellValue, getTblById} from '../../../tables/TableUtil.js';
import {getInitialDefaultValues,renderMissionView,validate,setValueAndValidator} from '../LcUtil.jsx';
import {LC} from '../LcManager.js';


const labelWidth = 80;
export class ZTFSettingBox extends PureComponent {
    constructor(props) {
        super(props);

    }

    render() {
        var {generalEntries, missionEntries} = this.props;
        const tblModel = getTblById(LC.RAW_TABLE);
        const wrapperStyle = {margin: '3px 0'};

        var missionFilters = ( <div style={{ width:{labelWidth}, display:'inline-block', margin: '3px 0 6px 0'}} >
            <br /> </div>);

        const imageStyle = { padding: '0 6px 0 6px', marginLeft: '54px'};
        return renderMissionView({generalEntries,missionEntries,missionFilters,tblModel,wrapperStyle, imageEntriesStyle:imageStyle, labelWidth , callback:ztfOptionsReducer});


    }
}

ZTFSettingBox.propTypes = {
    generalEntries: PropTypes.object,
    missionEntries: PropTypes.object
};

export const ztfOptionsReducer = (missionEntries, generalEntries) => {
    return (inFields, action) => {
        if (inFields) {
            return inFields;
        }


        const defValues =getInitialDefaultValues(labelWidth,'ztf');

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

    ];
    return validate(fldsWithValidators, missionEntries);

}


export function isValidZTFTable() {

    const tableModel = getTblById(LC.RAW_TABLE);

    const field = getCellValue(tableModel, 0, 'field');
    if (!isNil(field)) {
        return {errorMsg: undefined, isValid: true};
    }
    else {
         const errorMsg = `The uploaded table is not valid. The ZTF option requires field.
                        Please select the "Other" upload option for tables that do not meet these requirements.`;
        return {errorMsg, isValid:false};
   }
}
