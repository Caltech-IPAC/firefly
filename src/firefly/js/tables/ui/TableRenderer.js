/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import FixedDataTable from 'fixed-data-table';
import sCompare from 'react-addons-shallow-compare';
import {set, get, isEmpty, isEqual, isArray, pick} from 'lodash';

import {FilterInfo, FILTER_CONDITION_TTIPS} from '../FilterInfo.js';
import {SortInfo} from '../SortInfo.js';
import {InputField} from '../../ui/InputField.jsx';
import {SORT_ASC, UNSORTED} from '../SortInfo';
import {toBoolean} from '../../util/WebUtil.js';

import ASC_ICO from 'html/images/sort_asc.gif';
import DESC_ICO from 'html/images/sort_desc.gif';
import FILTER_SELECTED_ICO from 'html/images/icons-2014/16x16_Filter.png';

const {Cell} = FixedDataTable;
const html_regex = /<.+>/;

// the components here are small and used by table only.  not all props are defined.
/* eslint-disable react/prop-types */

/*---------------------------- COLUMN HEADER RENDERERS ----------------------------*/
function Label({sortable, label, name, sortByCols, sortInfoCls, onSort}) {
    const sortDir = sortInfoCls.getDirection(name);
    sortByCols = sortByCols || name;

    if (toBoolean(sortable, true)) {
        return (
            <div style={{width: '100%', cursor: 'pointer'}} onClick={() => onSort(sortByCols)}>{label || name}
                { sortDir !== UNSORTED && <SortSymbol sortDir={sortDir}/> }
            </div>
        );
    } else {
        return <div>{label || name}</div>;
    }
}

function SortSymbol({sortDir}) {
    return <img style={{marginLeft: 2}} src={sortDir === SORT_ASC ? ASC_ICO : DESC_ICO}/>;
};

export class HeaderCell extends React.Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }
    //
    render() {
        const {col, showUnits, showFilters, filterInfo, sortInfo, onSort, onFilter, style} = this.props;
        const cname = col.name;
        const cdesc = col.desc || col.title || cname;
        const filterStyle = {width: '100%', boxSizing: 'border-box'};
        const filterInfoCls = FilterInfo.parse(filterInfo);
        const sortInfoCls = SortInfo.parse(sortInfo);
        return (
            <div style={style} title={cdesc} className='TablePanel__header'>
                <Label {...{sortInfoCls, onSort}} {...col}/>
                {showUnits && col.units &&
                <div style={{fontWeight: 'normal'}}>({col.units})</div>
                }
                {showFilters && get(col, 'filterable', true) &&
                <InputField
                    validator={FilterInfo.conditionValidator}
                    fieldKey={cname}
                    tooltip={FILTER_CONDITION_TTIPS}
                    value={filterInfoCls.getFilter(cname)}
                    onChange={(v) => onFilter(v)}
                    actOn={['blur','enter']}
                    showWarning={false}
                    style={filterStyle}
                    wrapperStyle={filterStyle}
                />
                }
            </div>
        );
    }
}

export class SelectableHeader extends React.Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nProps) {
        const toCompare = ['checked', 'showUnits', 'showFilters'];
        return !isEqual(pick(nProps, toCompare), pick(this.props, toCompare));
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }
    //
    render() {
        const {checked, onSelectAll, showUnits, showFilters, onFilterSelected, style} = this.props;
        return (
            <div style={{padding: 0, ...style}} className='TablePanel__header'>
                <input type='checkbox'
                       tabIndex={-1}
                       checked={checked}
                       onChange={(e) => onSelectAll(e.target.checked)}/>
                {showUnits && <div/>}
                {showFilters && <img className='clickable'
                                     src={FILTER_SELECTED_ICO}
                                     onClick={onFilterSelected}
                                     title='Filter on selected rows'/>}
            </div>
        );
    }
}

export class SelectableCell extends React.Component {
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

export class TextCell extends React.Component {
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
        var val = getValue(this.props);
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
                        validator={validator}
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


