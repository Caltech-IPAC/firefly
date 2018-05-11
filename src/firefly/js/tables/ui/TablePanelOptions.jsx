/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {isEmpty} from 'lodash';

import {SimpleComponent} from '../../ui/SimpleComponent.jsx';
import {FilterEditor} from './FilterEditor.jsx';
import {InputField} from '../../ui/InputField.jsx';
import {intValidator} from '../../util/Validate.js';

import * as TblUtil from '../TableUtil.js';


const labelStyle = {display: 'inline-block', width: 70};


export class TablePanelOptions extends SimpleComponent  {

    getNextState(np) {
        const {tbl_ui_id} = np || this.props;
        const uiState = TblUtil.getTableUiById(tbl_ui_id) || {columns: []};
        return uiState;
    }

    render() {
        const {onChange, onOptionReset, tbl_ui_id} = this.props;
        const {columns, pageSize, showUnits=false, showTypes=false, showFilters=false, showToolbar=true, optSortInfo, filterInfo} = this.state;

        if (isEmpty(columns)) return false;

        const {onPageSize, onPropChanged} = makeCallbacks(onChange, columns);
        return (

               <div className='TablePanelOptions'>
                 <div
                    style={{flexGrow: 0, marginBottom: 32, display: 'flex', flexDirection: 'row', justifyContent: 'space-between'}}>
                    <div style={{display: 'inline-block', whiteSpace: 'nowrap'}}>
                        <div>
                            <div style={labelStyle}>Show Units:</div>
                            <input type='checkbox' onChange={(e) => onPropChanged(e.target.checked, 'showUnits')}
                                   checked={showUnits}/>
                        </div>
                        <div>
                            <div style={labelStyle}>Show Filters:</div>
                            <input type='checkbox' onChange={(e) => onPropChanged(e.target.checked, 'showFilters')}
                                   checked={showFilters}/>
                        </div>
                    </div>
                    <div style={{display: 'inline-block'}}>
                        <div>
                            <div style={labelStyle}>Show Types:</div>
                            <input type='checkbox' onChange={(e) => onPropChanged(e.target.checked, 'showTypes')}
                                   checked={showTypes}/>
                        </div>
                        {showToolbar &&
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
                        }
                    </div>
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

     );
    };
}

TablePanelOptions.propTypes = {
    tbl_ui_id: PropTypes.string,
    onChange: PropTypes.func,
    onOptionReset: PropTypes.func
};

function makeCallbacks(onChange) {
    const onPageSize = (pageSize) => {
        if (pageSize.valid) {
            onChange && onChange({pageSize: pageSize.value});
        }
    };

    const onPropChanged = (v, prop) => {
        onChange && onChange({[prop]: v});
    };

    return {onPageSize, onPropChanged};
}
