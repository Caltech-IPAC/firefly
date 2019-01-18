import React from 'react';
import {get} from 'lodash';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {getColumnValues} from '../../tables/TableUtil.js';


const skey = 'TABLE_SEARCH_METHODS';

export function TableSearchMethods({columnsModel}) {
    const ucds = getColumnValues(columnsModel, 'ucd');
    const names = getColumnValues(columnsModel, 'column_name');
    const options = ucds.reduce((p,c,i) => {
        if (c) { p.push({value: names[i], label: `[${names[i]}] ${c}`}); }
        return p;
    }, []);
    return (
        <FieldGroup groupKey={skey} keepState={true}>
            <ListBoxInputField key='ucdList'
                               fieldKey='colWithUcd'
                               options={options}
                               multiple={false}
                               label='Columns with UCDs:'
                               tooltip='List of UCDs for the selected table columns'
                               labelWidth={100}
                               wrapperStyle={{paddingBottom: 2}}
            />
        </FieldGroup>
    );
}

/**
 * Get constraints as ADQL
 * @returns {string}
 */
export function tableSearchMethodsConstraints() {
    // construct ADQL string here
    const fields = FieldGroupUtils.getGroupFields(skey);
    const colWithUcd = get(fields, 'colWithUcd.value');
    return colWithUcd ? `${colWithUcd} IS NOT NULL` : '';
}