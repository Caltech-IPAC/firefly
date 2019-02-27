/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PureComponent} from 'react';
import ReactDOM from 'react-dom';
import FixedDataTable from 'fixed-data-table-2';
import {set, get, isEqual, pick} from 'lodash';

import {FilterInfo, FILTER_CONDITION_TTIPS} from '../FilterInfo.js';
import {isNumericType, tblDropDownId} from '../TableUtil.js';
import {SortInfo} from '../SortInfo.js';
import {InputField} from '../../ui/InputField.jsx';
import {SORT_ASC, UNSORTED} from '../SortInfo';
import {toBoolean} from '../../util/WebUtil.js';

import ASC_ICO from 'html/images/sort_asc.gif';
import DESC_ICO from 'html/images/sort_desc.gif';
import FILTER_SELECTED_ICO from 'html/images/icons-2014/16x16_Filter.png';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField';
import {showDropDown, hideDropDown} from '../../ui/DialogRootContainer.jsx';
import {FieldGroup} from '../../ui/FieldGroup';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {SimpleComponent} from './../../ui/SimpleComponent.jsx';
import {getTblById, getColumn} from '../TableUtil.js';

const {Cell} = FixedDataTable;
const html_regex = /<.+>/;
const filterStyle = {width: '100%', boxSizing: 'border-box'};

/*---------------------------- COLUMN HEADER RENDERERS ----------------------------*/

function SortSymbol({sortDir}) {
    return (
        <div style={{marginLeft: 2}}>
            <img style={{verticalAlign: 'middle'}} src={sortDir === SORT_ASC ? ASC_ICO : DESC_ICO}/>
        </div>
    );
};

export class HeaderCell extends PureComponent {
    constructor(props) {
        super(props);
    }

    render() {
        const {col, showUnits, showTypes, showFilters, filterInfo, sortInfo, onSort, onFilter, style, tbl_id} = this.props;
        const {label, name, desc, sortByCols, sortable} = col || {};
        const cdesc = desc || label || name;
        const sortDir = SortInfo.parse(sortInfo).getDirection(name);
        const sortCol = sortByCols || name;
        const typeVal = col.type || '';
        const unitsVal = col.units ? `(${col.units})`: '';
        
        const onClick = toBoolean(sortable, true) ?(() => onSort(sortCol)) : undefined;
        return (
            <div style={style} title={cdesc} className='TablePanel__header'>
                <div style={{height: '100%', width: '100%'}} className='clickable' onClick={onClick}>
                    <div style={{display: 'inline-flex', width: '100%', justifyContent: 'center'}}>
                        <div style={{textOverflow: 'ellipsis', overflow: 'hidden'}}>
                            {label || name}
                        </div>
                        { sortDir !== UNSORTED && <SortSymbol sortDir={sortDir}/> }
                    </div>
                    {showUnits && <div style={{height: 11, fontWeight: 'normal'}}>{unitsVal}</div>}
                    {showTypes && <div style={{height: 11, fontWeight: 'normal', fontStyle: 'italic'}}>{typeVal}</div>}
                </div>
                {showFilters && <Filter {...{col, onFilter, filterInfo, tbl_id}}/>}
            </div>
        );
    }
}

class Filter extends SimpleComponent{

    getNextState(np) {
        const {col={}, tbl_id} = np;
        const ncol = getColumn(getTblById((tbl_id)), col.name);
        return {col: ncol};
    }
    render() {
        const {onFilter, filterInfo, tbl_id} = this.props;
        const {col} = this.state;
        const {name, filterable=true, enumVals} = col || {};

        if (!filterable) return <div style={{height:19}} />;      // column is not filterable

        let atElRef;
        const validator = (cond) => FilterInfo.conditionValidator(cond, tbl_id, name);
        const filterInfoCls = FilterInfo.parse(filterInfo);
        const content =  <EnumSelect {...{col, tbl_id, filterInfo, filterInfoCls, onFilter}} />;
        const onEnumClicked = () => {
            showDropDown({id: tblDropDownId(tbl_id), content, atElRef, locDir: 33, style: {marginLeft: -10},
                wrapperStyle: {zIndex: 110}}); // 110 is the z-index of a dropdown
        };

        return (
            <div style={{height:29, display: 'inline-flex', alignItems: 'center', width: '100%'}}>
                <InputField
                    validator={validator}
                    fieldKey={name}
                    tooltip={FILTER_CONDITION_TTIPS}
                    value={filterInfoCls.getFilter(name)}
                    onChange={(v) => onFilter(v)}
                    actOn={['blur','enter']}
                    showWarning={false}
                    style={filterStyle}
                    wrapperStyle={filterStyle}/>
                {enumVals && <div ref={(r) => atElRef=r} className='arrow-down clickable' onClick={onEnumClicked} style={{borderWidth: 6, borderRadius: 2}}/>}
            </div>
        );
    }
}


function EnumSelect({col, tbl_id, filterInfo, filterInfoCls, onFilter}) {
    const {name, enumVals} = col || {};
    const groupKey = 'TableRenderer_enum';
    const fieldKey = tbl_id + '-' + name;
    const options = col.enumVals.split(',')
                        .map( (s) => s.trim())
                        .map( (s) => ({label: s, value: s}) );
    let value = '';
    const filterBy = (filterInfoCls.getFilter(name) || '').match(/IN \((.+)\)/);
    if (filterBy) {
        // IN condition is used, set value accordingly.  remove enclosed quote if exists
        value = filterBy[1].split(',')
                           .map( (s) => s.trim().replace(/^'(.+)'$/, '$1'))
                           .join(',');
    }

    const hideEnumSelect = () => hideDropDown(tblDropDownId(tbl_id));
    const onApply = () => {
        let value = getFieldVal(groupKey, fieldKey, '');
        if (value) {
            value = isNumericType(col) ? value :
                    value.split(',')
                         .map((s) => `'${s.trim()}'`).join(',');
            value = `IN (${value})`;
        }
        onFilter({fieldKey: name, valid: true, value});
        hideEnumSelect();
    };

    return (
        <FieldGroup groupKey='TableRenderer_enum' style={{minWidth: 100}}>
            <div style={{display: 'inline-flex', marginBottom: 5, width: '100%', justifyContent: 'space-between'}}>
                <div className='ff-href' style={{marginLeft: 3, fontSize: 13}} onClick={onApply}>filter</div>
                <div className='btn-close' onClick={hideEnumSelect} style={{margin: -2, fontSize: 12}}/>
            </div>
            <CheckboxGroupInputField {...{fieldKey, alignment: 'vertical', initialState:{options,value}}}/>
        </FieldGroup>
    );
}

export class SelectableHeader extends Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nProps) {
        const toCompare = ['checked', 'showUnits', 'showTypes', 'showFilters'];
        return !isEqual(pick(nProps, toCompare), pick(this.props, toCompare));
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }
    //
    render() {
        const {checked, onSelectAll, showUnits, showTypes, showFilters, onFilterSelected, style} = this.props;
        return (
            <div style={{padding: 0, ...style}} className='TablePanel__header'>
                <input type='checkbox'
                       tabIndex={-1}
                       checked={checked}
                       onChange={(e) => onSelectAll(e.target.checked)}/>
                {showUnits && <div/>}
                {showTypes && <div/>}
                {showFilters && <img className='clickable'
                                     style={{marginBottom: 3}}
                                     src={FILTER_SELECTED_ICO}
                                     onClick={onFilterSelected}
                                     title='Filter on selected rows'/>}
            </div>
        );
    }
}

export class SelectableCell extends Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nProps) {
        const toCompare = ['rowIndex', 'selectInfoCls'];
        return !isEqual(pick(nProps, toCompare), pick(this.props, toCompare));
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.displayName);
    // }
    //
    render() {
        const {rowIndex, selectInfoCls, onRowSelect, style} = this.props;
        return (
            <div style={style} className='TablePanel__checkbox'>
                <input type='checkbox'
                       tabIndex={-1}
                       checked={selectInfoCls.isSelected(rowIndex)}
                       onChange={(e) => onRowSelect(e.target.checked, rowIndex)}/>
            </div>
        );
    }
}

/*---------------------------- CELL RENDERERS ----------------------------*/

function getValue(props) {
    const {rowIndex, data, columnKey} = props;
    return get(data, [rowIndex, columnKey], 'undef');
}

export class TextCell extends Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nProps) {
        return nProps.columnKey !== this.props.columnKey ||
            nProps.rowIndex !== this.props.rowIndex ||
            getValue(nProps) !== getValue(this.props);
    }

    // componentDidUpdate(prevProps) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.displayName);
    // }
    //
    render() {
        var val = getValue(this.props) || '';
        const lineHeight = this.props.height - 6 + 'px';  // 6 is the top/bottom padding.
        val = (val.search && val.search(html_regex) >= 0) ? <div dangerouslySetInnerHTML={{__html: val}}/> : val;
        return (
            <div style={{lineHeight, ...this.props.style}} className='public_fixedDataTableCell_cellContent'>{val}</div>
        );
    }
}


/**
 * @param {{rowIndex,data,colIdx}}
 * Image cell renderer.  It will use the cell value as the image source.
 */
export const ImageCell = ({rowIndex, data, colIdx}) => (
    <img src={get(data, [rowIndex, colIdx],'undef')}/>
);

/**
 * creates a link cell renderer using the cell data as href.
 * @param obj
 * @param obj.hrefColIdx
 * @param obj.value  display this value for every cell.
 * @returns {Function}
 */
export const createLinkCell = ({hrefColIdx, value}) => {

    return ({rowIndex, data, colIdx, ...CellProps}) => {
        hrefColIdx = hrefColIdx || colIdx;
        const href = get(data, [rowIndex, hrefColIdx], 'undef');
        const val = value || get(data, [rowIndex, colIdx], 'undef');
        if (href === 'undef' || href === '#') {
            return (
                <Cell {...CellProps}>
                    {val}
                </Cell>
            );
        } else {
            return (
                <Cell {...CellProps}>
                    <a target='_blank' href={href}>{val}</a>
                </Cell>
            );
        }
    };
};

export const NOT_CELL_DATA = '__NOT_A_VALID_DATA___';
/**
 * creates an input field cell renderer.
 * @param tooltips
 * @param size
 * @param validator
 * @param onChange
 * @param style
 * @returns {Function}
 */
export const createInputCell = (tooltips, size = 10, validator, onChange, style) => {
    const changeHandler = (rowIndex, data, colIdx, v) => {
        set(data, [rowIndex, colIdx], v.value);
        onChange && onChange(v);
    };

    return ({rowIndex, data, colIdx}) => {
        const val = get(data, [rowIndex, colIdx]);


        if (val === NOT_CELL_DATA) {
            return null;
        } else {
            return (
                <div style={{margin: 2}}>
                    <InputField
                        validator={(v) => validator(v, data, rowIndex, colIdx)}
                        tooltip={tooltips}
                        size={size}
                        style={style}
                        value={val}
                        onChange={(v) => changeHandler(rowIndex, data, colIdx, v) }
                        actOn={['blur','enter']}
                    />
                </div>
            );
        }
    };
};


