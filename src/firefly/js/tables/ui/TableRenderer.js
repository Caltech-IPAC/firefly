/**
 * Created by loi on 3/17/16.
 */

import React from 'react';
import FixedDataTable from 'fixed-data-table';
import {set, get} from 'lodash';

import {FilterInfo, FILTER_TTIPS} from '../FilterInfo.js';
import {InputField} from '../../ui/InputField.jsx';
import {SORT_ASC, UNSORTED} from '../SortInfo';

import ASC_ICO from 'html/images/sort_asc.gif';
import DESC_ICO from 'html/images/sort_desc.gif';

const {Cell} = FixedDataTable;



const SortSymbol = ({sortDir}) => {
    return <img style={{marginLeft: 2}} src={sortDir === SORT_ASC ? ASC_ICO : DESC_ICO}/>;
};

/*---------------------------- COLUMN HEADER RENDERERS ----------------------------*/
export const HeaderCell = ({col, showUnits, showFilters, filterInfoCls, sortInfoCls, onSort, onFilter}) => {

    const cname = col.name;
    const sortDir = sortInfoCls.getDirection(cname);

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
                width='100%'
            />
            }
        </div>
    );
};


/*---------------------------- CELL RENDERERS ----------------------------*/

export const TextCell = ({rowIndex, data, col, ...CellProps}) => {
    return (
        <Cell {...CellProps}>
            {get(data, [rowIndex, col],'undef')}
        </Cell>
    );
};

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
    }
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
    }
};

