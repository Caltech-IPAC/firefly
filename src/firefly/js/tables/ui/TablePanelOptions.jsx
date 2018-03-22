/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {isEmpty} from 'lodash';
import {SimpleComponent} from './../../ui/SimpleComponent.jsx';
import {FilterEditor} from './FilterEditor.jsx';
import {InputField} from '../../ui/InputField.jsx';
import {intValidator} from '../../util/Validate.js';
import {FieldGroup} from './../../ui/FieldGroup.jsx';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';

const labelStyle = {display: 'inline-block', width: 70};

const TableOptionGroupKey = 'tableOption';

export class TablePanelOptions extends SimpleComponent{
    constructor(props) {
        super(props);

        this.state = this.getNextState();
    }
    getNextState() {

        const showFilters = getFieldVal(TableOptionGroupKey, 'filters') || false;
        const showUnits= getFieldVal(TableOptionGroupKey, 'units') || false;
        return {showFilters, showUnits};

    }
    render() {
        const {columns, pageSize, showToolbar=true, onChange, onOptionReset, optSortInfo, filterInfo,  tbl_ui_id} = this.props;

        const {showFilters, showUnits} = this.state;

        if (isEmpty(columns)) return false;

        const {onPageSize, onPropChanged} = makeCallbacks(onChange, columns);

        return (

            <FieldGroup style={{padding: 5}} keepState={true} groupKey={TableOptionGroupKey }>
                <div className='TablePanelOptions'>
                 <div
                    style={{flexGrow: 0, marginBottom: 32, display: 'flex', flexDirection: 'row', justifyContent: 'space-between'}}>
                    <div style={{display: 'inline-block', whiteSpace: 'nowrap'}}>
                        <div>
                            <div style={labelStyle}>Show Units:</div>
                            <input type='checkbox' onChange={(e) => onPropChanged(e.target.checked, 'showUnits')}
                                   checked={showUnits}/>

                            <CheckboxGroupInputField
                                options={[{value: 'separateByObject'}]}
                                fieldKey='showUnits'
                                labelWidth={90}
                                groupKey={TableOptionGroupKey }
                            />
                        </div>
                        <div>
                            <div style={labelStyle}>Show Filters:</div>
                            <input type='checkbox' onChange={(e) => onPropChanged(e.target.checked, 'showFilters')}
                                   checked={showFilters}/>
                        </div>
                    </div>
                    {showToolbar &&
                        <div style={{display: 'inline-block'}}>
                            <div style={{marginTop: 17}}>
                                <InputField
                                    validator={intValidator(1,10000)}
                                    tooltip='Set page size'
                                    label='Page Size:'
                                    labelStyle={{...labelStyle, width: 60}}
                                    size={3}
                                    value={pageSize+''}
                                    onChange={onPageSize}
                                    actOn={['blur','enter']}
                                />
                            </div>
                        </div>
                    }
                    <span>

                        <button type='button' className='TablePanelOptions__button' onClick={onOptionReset}
                                title='Reset all options to defaults'>Reset</button>
                    </span>
                </div>
                <div style={{height: 'calc(100% - 40px)'}}>
                    <FilterEditor sortInfo={optSortInfo}
                        {...{tbl_ui_id, columns, filterInfo, onChange}}
                    />
                </div>
               </div>
            </FieldGroup>

     );
    };
}

TablePanelOptions.propTypes = {
    groupKey:PropTypes.string,
    tbl_ui_id: PropTypes.string,
    columns: PropTypes.arrayOf(PropTypes.object),
    optSortInfo: PropTypes.string,
    filterInfo: PropTypes.string,
    pageSize: PropTypes.number,
    showUnits: PropTypes.bool,
    showFilters: PropTypes.bool,
    showToolbar: PropTypes.bool,
    onChange: PropTypes.func,
    onOptionReset: PropTypes.func,
    toggleOptions: PropTypes.func
};

function makeCallbacks(onChange) {
    var onPageSize = (pageSize) => {
        if (pageSize.valid) {
            onChange && onChange({pageSize: pageSize.value});
        }
    };

    var onPropChanged = (v, prop) => {
        onChange && onChange({[prop]: v});
    };

    return {onPageSize, onPropChanged};
}
