/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import sCompare from 'react-addons-shallow-compare';
import {isEmpty, cloneDeep} from 'lodash';

import {FilterEditor} from './FilterEditor.jsx';
import {InputField} from '../../ui/InputField.jsx';
import {intValidator} from '../../util/Validate.js';
// import {deepDiff} from '../../util/WebUtil.js';

const labelStyle = {display: 'inline-block', width: 70};

export class TablePanelOptions extends React.Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         'TablePanelOptions');
    // }

    render() {
        const {columns, origColumns, pageSize, showUnits, showFilters, onChange, optSortInfo, filterInfo, toggleOptions} = this.props;
        if (isEmpty(columns)) return false;

        const {onPageSize, onPropChanged, onReset} = makeCallbacks(onChange, columns, origColumns);
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
                    <span>
                        <div style={{ position: 'relative',
                                      display: 'block',
                                      height: 16,
                                      right: -42,
                                      top: -4}}
                             className='btn-close'
                             title='Remove Tab'
                             onClick={() => toggleOptions()}/>

                        <button className='TablePanelOptions__button' onClick={onReset}
                                title='Reset all options to defaults'>Reset</button>
                    </span>
                </div>
                <div style={{height: 'calc(100% - 40px)'}}>
                    <FilterEditor
                        columns={columns}
                        filterInfo={filterInfo}
                        sortInfo={optSortInfo}
                        onChange={onChange}
                    />
                </div>
            </div>
        );
    };
}

TablePanelOptions.propTypes = {
    columns: React.PropTypes.arrayOf(React.PropTypes.object),
    origColumns: React.PropTypes.arrayOf(React.PropTypes.object),
    optSortInfo: React.PropTypes.string,
    filterInfo: React.PropTypes.string,
    pageSize: React.PropTypes.number,
    showUnits: React.PropTypes.bool,
    showFilters: React.PropTypes.bool,
    onChange: React.PropTypes.func,
    toggleOptions: React.PropTypes.func
};

function makeCallbacks(onChange, columns, origColumns, data) {
    var onPageSize = (pageSize) => {
        if (pageSize.valid) {
            onChange && onChange({pageSize: pageSize.value});
        }
    };

    var onPropChanged = (v, prop) => {
        onChange && onChange({[prop]: v});
    };

    var onReset = () => {
        onChange && onChange({pageSize: 100, showUnits: false, showFilters: false, filterInfo: '', columns: cloneDeep(origColumns)});
    };

    return {onPageSize, onPropChanged, onReset};
}
