/**
 * Created by loi on 3/17/16.
 */

import React from 'react';
import FixedDataTable from 'fixed-data-table';
import {set, get, omit, isEqual} from 'lodash';

import {FilterInfo, FILTER_TTIPS} from '../FilterInfo.js';
import {InputField} from '../../ui/InputField.jsx';
import {SORT_ASC, UNSORTED} from '../SortInfo';

import ASC_ICO from 'html/images/sort_asc.gif';
import DESC_ICO from 'html/images/sort_desc.gif';
// import {deepDiff} from '../../util/WebUtil.js';

const {Cell} = FixedDataTable;



const SortSymbol = ({sortDir}) => {
    return <img style={{marginLeft: 2}} src={sortDir === SORT_ASC ? ASC_ICO : DESC_ICO}/>;
};

/*---------------------------- COLUMN HEADER RENDERERS ----------------------------*/

export class HeaderCell extends React.Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nProps, nState) {
        const excludes = ['onSort', 'onFilter'];
        return !isEqual(omit(nProps, excludes), omit(this.props, excludes));
    }

    render() {
        const {col, showUnits, showFilters, filterInfoCls, sortInfoCls, onSort, onFilter} = this.props;
        const cname = col.name;
        const sortDir = sortInfoCls.getDirection(cname);
        const style = {width: '100%', boxSizing: 'border-box'};

        return (
            <div title={col.title || cname} className='TablePanel__header'>
                <div style={{width: '100%', cursor: 'pointer'}} onClick={() => onSort(cname)} >{cname}
                    { sortDir!==UNSORTED && <SortSymbol sortDir={sortDir}/> }
                </div>
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

    shouldComponentUpdate(nProps, nState) {
        return nProps.checked !== this.props.checked;
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }

    render() {
        const {checked, onSelectAll} = this.props;
        return (
            <div className='tablePanel__checkbox'>
                <input type='checkbox' checked={checked} onChange ={(e) => onSelectAll(e.target.checked)}/>
            </div>
        );
    }
}

export class SelectableCell extends React.Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nProps, nState) {
        return !isEqual(omit(nProps, 'onRowSelect'), omit(this.props, 'onRowSelect'));
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.displayName);
    // }

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

    shouldComponentUpdate(nProps, nState) {
        return  nProps.columnKey !== this.props.columnKey ||
               nProps.rowIndex !== this.props.rowIndex ||
               getValue(nProps) !== getValue(this.props);
    }

    componentDidUpdate(prevProps, prevState) {
        // deepDiff({props: prevProps, state: prevState},
        //     {props: this.props, state: this.state},
        //     this.constructor.displayName);
    }

    render() {
        return (
            <div className='public_fixedDataTableCell_cellContent'>
                {getValue(this.props)}
            </div>
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


