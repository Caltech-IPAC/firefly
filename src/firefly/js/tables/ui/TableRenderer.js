/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import FixedDataTable from 'fixed-data-table';
import sCompare from 'react-addons-shallow-compare';
import {set, get, isEqual, pick} from 'lodash';

import {FilterInfo, FILTER_TTIPS} from '../FilterInfo.js';
import {SortInfo} from '../SortInfo.js';
import {InputField} from '../../ui/InputField.jsx';
import {SORT_ASC, UNSORTED} from '../SortInfo';
import {toBoolean} from '../../util/WebUtil.js';

import ASC_ICO from 'html/images/sort_asc.gif';
import DESC_ICO from 'html/images/sort_desc.gif';
import FILTER_SELECTED_ICO from 'html/images/icons-2014/16x16_Filter.png';

const {Cell} = FixedDataTable;
const  html_regex = /<.+>/;

// the components here are small and used by table only.  not all props are defined.
/* eslint-disable react/prop-types */

/*---------------------------- COLUMN HEADER RENDERERS ----------------------------*/
function Label({sortable, title, name, sortByCols, sortInfoCls, onSort}) {
    const sortDir = sortInfoCls.getDirection(name);
    sortByCols = sortByCols || name;

    if (toBoolean(sortable, true)) {
        return (
            <div style={{width: '100%', cursor: 'pointer'}} onClick={() => onSort(sortByCols)} >{title || name}
                { sortDir!==UNSORTED && <SortSymbol sortDir={sortDir}/> }
            </div>
        );
    } else {
        return <div>{title || name}</div>;
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
        const {col, showUnits, showFilters, filterInfo, sortInfo, onSort, onFilter} = this.props;
        const cname = col.name;
        const cdesc = col.desc || col.title || cname;
        const style = {width: '100%', boxSizing: 'border-box'};
        const filterInfoCls = FilterInfo.parse(filterInfo);
        const sortInfoCls = SortInfo.parse(sortInfo);

        return (
            <div title={cdesc} className='TablePanel__header'>
                <Label {...{sortInfoCls, onSort}} {...col}/>
                {showUnits && col.units && <div style={{fontWeight: 'normal'}}>({col.units})</div>}
                {showFilters && <InputField
                    validator={FilterInfo.validator}
                    fieldKey={cname}
                    tooltip = {FILTER_TTIPS}
                    value = {filterInfoCls.getFilter(cname)}
                    onChange = {(v) => onFilter(v)}
                    actOn={['blur','enter']}
                    showWarning={false}
                    style={style}
                    wrapperStyle={style}
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
        const {checked, onSelectAll, showUnits, showFilters, onFilterSelected} = this.props;
        return (
            <div className='TablePanel__header'>
                <input type='checkbox' checked={checked} onChange ={(e) => onSelectAll(e.target.checked)}/>
                {showUnits && <div/>}
                {showFilters && <img className='button'
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
        const {rowIndex, selectInfoCls, onRowSelect} = this.props;
        return (
            <div className='tablePanel__checkbox tablePanel__checkbox--cell'>
                <input type='checkbox' checked={selectInfoCls.isSelected(rowIndex)} onChange={(e) => onRowSelect(e.target.checked, rowIndex)} />
            </div>
        );
    }
}

/*---------------------------- CELL RENDERERS ----------------------------*/

function getValue(props) {
    const {rowIndex, data, col} = props;
    return get(data, [rowIndex, col], 'undef');
}

export class TextCell extends React.Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nProps) {
        return  nProps.columnKey !== this.props.columnKey ||
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
        val = (val.search(html_regex) >= 0) ? <div dangerouslySetInnerHTML={{__html: val}} /> : val;
        return (
            <div style={{lineHeight}} className='public_fixedDataTableCell_cellContent'>{val}</div>
        );
    }
}


/**
 * Image cell renderer.  It will use the cell value as the image source.
 */
export const ImageCell = ({rowIndex, data, col}) => (
    <img src={get(data, [rowIndex, col],'undef')}/>
);

/**
 * creates a link cell renderer using the cell data as href.
 * @param valFromCol  display the value from this column index.
 * @param value  display this value for every cell.
 * @returns {function()}
 */
export const createLinkCell = ({valFromCol, value}) => {

    return ({rowIndex, data, col, ...CellProps}) => {
        const href = get(data, [rowIndex, col],'undef');
        const val = value || get(data, [rowIndex, valFromCol], 'undef');
        return (
            <Cell {...CellProps}>
                <a href={href}>{val}</a>
            </Cell>
        );
    };
};

/**
 * creates an input field cell renderer.
 * @param tooltips
 * @param size
 * @param validator
 * @param onChange
 * @returns {function()}
 */
export const createInputCell = (tooltips, size=10, validator, onChange) => {
    const changeHandler = (rowIndex, data, col, v) => {
        set(data, [rowIndex, col], v.value);
        onChange && onChange(v);
    };

    return ({rowIndex, data, col}) => {
        return (
            <div style={{margin: 2}}>
                <InputField
                    validator = {validator}
                    tooltip = {tooltips}
                    size = {size}
                    value = {get(data, [rowIndex, col],'')}
                    onChange = {(v) => changeHandler(rowIndex, data, col, v) }
                    actOn={['blur','enter']}
                />
            </div>
        );
    };
};


