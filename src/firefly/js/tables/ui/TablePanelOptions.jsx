/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {isEmpty} from 'lodash';

import {FilterEditor} from './FilterEditor.jsx';
import {InputField} from '../../ui/InputField.jsx';
import {intValidator} from '../../util/Validate.js';

const labelStyle = {display: 'inline-block', width: 70};


export class TablePanelOptions extends PureComponent {
    constructor(props) {
        super(props);
    }

    render() {
        const {columns, pageSize, showUnits, showFilters, showToolbar=true, onChange, onOptionReset, optSortInfo, filterInfo, toggleOptions, tbl_ui_id} = this.props;
        if (isEmpty(columns)) return false;

        const {onPageSize, onPropChanged} = makeCallbacks(onChange, columns);
        return (
            <div className='TablePanelOptions'>
                <div
                    style={{flexGrow: 0, marginBottom: 4, display: 'flex', flexDirection: 'row', justifyContent: 'space-between'}}>
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
                        <div style={{ position: 'relative',
                                      display: 'block',
                                      right: -42,
                                      top: -2}}
                             className='btn-close'
                             title='Remove Tab'
                             onClick={() => toggleOptions()}/>

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
