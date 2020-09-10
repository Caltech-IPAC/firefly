/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PureComponent, useRef, useCallback, useState} from 'react';
import {Cell} from 'fixed-data-table-2';
import {set, get, isEqual, pick, omit, isEmpty} from 'lodash';

import {FilterInfo, FILTER_CONDITION_TTIPS, NULL_TOKEN} from '../FilterInfo.js';
import {isColumnType, COL_TYPE, tblDropDownId, getTblById, getColumn, formatValue, getTypeLabel, getColumnIdx, getRowValues, getCellValue} from '../TableUtil.js';
import {SortInfo} from '../SortInfo.js';
import {InputField} from '../../ui/InputField.jsx';
import {SORT_ASC, UNSORTED} from '../SortInfo';
import {toBoolean, copyToClipboard} from '../../util/WebUtil.js';

import ASC_ICO from 'html/images/sort_asc.gif';
import DESC_ICO from 'html/images/sort_desc.gif';
import FILTER_SELECTED_ICO from 'html/images/icons-2014/16x16_Filter.png';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField';
import DialogRootContainer, {showDropDown, hideDropDown} from '../../ui/DialogRootContainer.jsx';
import {FieldGroup} from '../../ui/FieldGroup';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {useStoreConnector} from './../../ui/SimpleComponent.jsx';
import {resolveHRefVal} from '../../util/VOAnalyzer.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {dispatchTableUpdate} from '../../tables/TablesCntlr.js';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {PopupPanel} from '../../ui/PopupPanel.jsx';

import infoIcon from 'html/images/info-icon.png';

const html_regex = /<.+>/;
const filterStyle = {width: '100%', boxSizing: 'border-box'};

const imageStubMap = {
    info: <img style={{width:'14px'}} src={infoIcon} alt='info'/>
};


/*---------------------------- COLUMN HEADER RENDERERS ----------------------------*/

function SortSymbol({sortDir}) {
    return (
        <div style={{marginLeft: 2}}>
            <img style={{verticalAlign: 'middle'}} src={sortDir === SORT_ASC ? ASC_ICO : DESC_ICO}/>
        </div>
    );
};

export const HeaderCell = React.memo( ({col, showUnits, showTypes, showFilters, filterInfo, sortInfo, onSort, onFilter, style, tbl_id}) => {
    const {label, name, desc, sortByCols, sortable} = col || {};
    const cdesc = desc || label || name;
    const sortDir = SortInfo.parse(sortInfo).getDirection(name);
    const sortCol = sortByCols || name;
    const typeVal = getTypeLabel(col);
    const unitsVal = col.units ? `(${col.units})`: '';
    const className = toBoolean(sortable, true) ? 'clickable' : undefined;

    const onClick = toBoolean(sortable, true) ? () => onSort(sortCol) : () => showInfoPopup('This column is not sortable');
    return (
        <div style={style} title={cdesc} className='TablePanel__header'>
            <div style={{height: '100%', width: '100%'}} className={className} onClick={onClick}>
                <div style={{display: 'inline-flex', width: '100%', justifyContent: 'center'}}>
                    <div style={{textOverflow: 'ellipsis', overflow: 'hidden'}}>
                        {label || name}
                    </div>
                    { sortDir !== UNSORTED && <SortSymbol sortDir={sortDir}/> }
                </div>
                {showUnits && <div style={{height: 11, fontWeight: 'normal'}}>{unitsVal}</div>}
                {showTypes && <div style={{height: 11, fontWeight: 'normal', fontStyle: 'italic'}}>{typeVal}</div>}
            </div>
            {showFilters && <Filter {...{cname:name, onFilter, filterInfo, tbl_id}}/>}
        </div>
    );
});

const blurEnter = ['blur','enter'];

function Filter({cname, onFilter, filterInfo, tbl_id}) {

    const colGetter= () => getColumn(getTblById((tbl_id)), cname);

    const enumArrowEl = useRef(null);
    const [col={}] = useStoreConnector(colGetter);

    const validator = useCallback((cond) => {
        return FilterInfo.conditionValidator(cond, tbl_id, cname);
    }, [tbl_id, cname]);

    const {name, filterable=true, enumVals} = col;

    if (!filterable) return <div style={{height:19}} />;      // column is not filterable

    const filterInfoCls = FilterInfo.parse(filterInfo);
    const content =  <EnumSelect {...{col, tbl_id, filterInfo, filterInfoCls, onFilter}} />;
    const onEnumClicked = () => {
        showDropDown({id: tblDropDownId(tbl_id), content, atElRef: enumArrowEl.current, locDir: 33, style: {marginLeft: -10},
            wrapperStyle: {zIndex: 110}}); // 110 is the z-index of a dropdown
    };

    return (
        <div style={{height:29, display: 'inline-flex', alignItems: 'center', width: '100%'}}>
            <InputField
                validator={validator}
                fieldKey={name}
                tooltip={FILTER_CONDITION_TTIPS}
                value={filterInfoCls.getFilter(name)}
                onChange={onFilter}
                actOn={blurEnter}
                showWarning={false}
                style={filterStyle}
                wrapperStyle={filterStyle}/>
            {enumVals && <div ref={enumArrowEl} className='arrow-down clickable' onClick={onEnumClicked} style={{borderWidth: 6, borderRadius: 2}}/>}
        </div>
    );
}

function EnumSelect({col, tbl_id, filterInfoCls, onFilter}) {
    const {name, enumVals} = col || {};
    const groupKey = 'TableRenderer_enum';
    const fieldKey = tbl_id + '-' + name;
    const options = enumVals.split(',')
                        .map( (s) => {
                            const value = s === '' ? '%EMPTY' : s;                  // because CheckboxGroupInputField does not support '' as an option, use '%EMPTY' as substitute
                            const label = value === NULL_TOKEN ? '<NULL>' : value === '%EMPTY' ? '<EMPTY_STR>' : value;
                            return {label, value};
                        } );
    let value;
    const filterBy = (filterInfoCls.getFilter(name) || '').match(/IN \((.+)\)/i);
    if (filterBy) {
        // IN condition is used, set value accordingly.  remove enclosed quote if exists
        value = filterBy[1].split(',')
                           .map( (s) => s.trim().replace(/^'(.*)'$/, '$1'))
                           .map((s) => s === '' ? '%EMPTY' : s)                 // convert '' to %EMPTY
                           .join(',');
    }

    const hideEnumSelect = () => hideDropDown(tblDropDownId(tbl_id));
    const onClear = () => {
        dispatchValueChange({fieldKey, groupKey, value: '', valid: true});
    };
    const onApply = () => {
        let value = getFieldVal(groupKey, fieldKey);
        if (value) {
            value = value.split(',').map((s) => s === '%EMPTY' ? '' : s).join();           // convert %EMPTY back into ''
            value = isColumnType(col, COL_TYPE.NUMBER) ? value :
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
                <div className='ff-href' style={{marginLeft: 3, fontSize: 13}} onClick={onApply} title='Apply selected filter'>filter</div>
                <div className='ff-href' style={{marginLeft: 3, fontSize: 13}} onClick={onClear} title='Clear selection'>clear</div>
                <div className='btn-close' onClick={hideEnumSelect} style={{margin: -2, fontSize: 12}}/>
            </div>
            <CheckboxGroupInputField {...{fieldKey, alignment: 'vertical', options, initialState:{value}}}/>
        </FieldGroup>
    );
}

export class SelectableHeader extends PureComponent {
    constructor(props) {
        super(props);
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

/**
 * returns cell related attributes for display {col, value, rvalues, text, isArray, textAlign, absRowIdx}
 * @returns {{col, value, rvalues, text, isArray, textAlign, absRowIdx, tableModel}}
 */
function getCellInfo({col, rowIndex, data, columnKey, tbl_id, startIdx=0}) {
    if (!col) return {};

    const tableModel = getTblById(tbl_id);
    const absRowIdx = rowIndex + startIdx;   // rowIndex is the index of the current page.  Add startIdx to get the absolute index of the row in the full table.
    const value = get(data, [rowIndex, columnKey]);
    const isArray = Array.isArray(value);
    let text = formatValue(col, value);
    let rvalues = [value];
    let textAlign = col.align;

    if (col.links) {
        rvalues =  col.links.map( ({value:val}) => resolveHRefVal(tableModel, val, absRowIdx, value) );
        text = rvalues.join(' ');
    }
    textAlign = textAlign || rvalues.length > 1 ? 'middle': isColumnType(col, COL_TYPE.NUMBER) ? 'right' : 'left';
    return {col, value, rvalues, text, isArray, textAlign, absRowIdx, tableModel};
}

export function TextCell({columnKey, col, rowIndex, data, cellInfo}) {
    const {value, text} = cellInfo || getCellInfo({columnKey, col, rowIndex, data});
    return (value?.search && value.search(html_regex) >= 0) ? <div dangerouslySetInnerHTML={{__html: value}}/> : text;
}


export function makeDefaultRenderer(col={}, tbl_id, startIdx) {
    const Content = (col.type === 'location' || !isEmpty(col.links)) ? LinkCell : TextCell;
    return (props) => <CellWrapper {...{...props, tbl_id, startIdx, Content}} />;
}


/**
 * A wrapper tag that handles default styles, textAlign, and actions.
 */
export const CellWrapper =  React.memo( (props) => {
    const {tbl_id, startIdx, Content, style, columnKey, col, rowIndex, data, height, width} = props;
    const dropDownID = 'actions--dropdown';
    const popupID = 'actions--popup';

    const [hasActions, setHasActions] = useState(false);
    const actionsEl = useRef(null);
    const cellInfo = getCellInfo({columnKey, col, rowIndex, data, tbl_id, startIdx});
    const {textAlign, text} = cellInfo;

    const onActionsClicked = (ev) => {
        ev.stopPropagation();
        showDropDown({id: dropDownID, content: dropDown, atElRef: actionsEl.current, locDir: 33, style: {marginLeft: -10},
            wrapperStyle: {zIndex: 110}}); // 110 is the z-index of a dropdown
    };

    const copyCB = () => {
        copyToClipboard(text);
        hideDropDown(dropDownID);
    };

    const viewAsText = () => {
        DialogRootContainer.defineDialog(popupID, <ViewAsText text={text}/>);
        dispatchShowDialog(popupID);
        hideDropDown(dropDownID);
    };

    const dropDown =  (
        <div className='Actions__dropdown'>
            <div className='Actions__item' onClick={copyCB}>Copy to clipboard</div>
            <div className='Actions__item' onClick={viewAsText}>View as plain text</div>
        </div>
    );
    const lineHeight = height - 6 + 'px';  // 6 is the top/bottom padding.

    const checkOverflow = (ev) => {
        const c = ev?.currentTarget?.children?.[0] || {};
        setHasActions(c.clientWidth < c.scrollWidth-6);  // account for paddings
    };

    return (
        <div onMouseEnter={checkOverflow}
             onMouseLeave={() => setHasActions(false)} style={{display: 'flex'}}>
            <div style={{textAlign, lineHeight, ...style}} className='public_fixedDataTableCell_cellContent'>
                <Content {...omit(props, 'Content')} cellInfo={cellInfo}/>
            </div>
            {hasActions && <div ref={actionsEl} className='Actions__anchor clickable' onClick={onActionsClicked} title='Display full cell contents'>&#x25cf;&#x25cf;&#x25cf;</div>}
        </div>
    );
}, skipCellRender);

function skipCellRender(prev={}, next={}) {
    return prev.width === next.width &&
        getCellInfo(prev)?.text === getCellInfo(next)?.text;
}


function ViewAsText({text, ...rest}) {
    const [doFmt, setDoFmt] = useState(false);

    const onChange = (e) => {
        setDoFmt(e.target.checked);
    };

    if (doFmt && text.match(/^\[.+\]$|^{.+}$/)) {
        try {
            text = JSON.stringify(JSON.parse(text), null, 2, 2);
        } catch (e) {}      // if text is not JSON, just show as is.
    }

    const label = 'Apply formatting so that it is easier to read';
    return (
        <PopupPanel title={'View as plain text'} style={{flexDirection: 'column'}} {...rest}>
            <div style={{display: 'flex', alignItems: 'center'}}>
                <input id='doFormat' type = 'checkbox' title = {label} onChange = {onChange}/>
                <label htmlFor='doFormat' style={{verticalAlign: ''}}>{label}</label>
            </div>
            <textarea readOnly className='Actions__popup' value={text} style={{width: 650, height: 125}}/>
        </PopupPanel>

    );
}

/**
 * @see {@link http://www.ivoa.net/documents/VOTable/20130920/REC-VOTable-1.3-20130920.html#ToC54}
 * LinkCell is implementing A.4 using link substitution based on A.1
 */
export const LinkCell = React.memo(({cellInfo, style, ...rest}) => {
    const {absRowIdx, col, textAlign, rvalues, tableModel} = cellInfo || getCellInfo(rest);
    let mstyle = omit(style, 'backgroundColor');
    if (col.links) {
        return (
            <div style={{textAlign, overflow: 'visible'}}>
            {
                col.links.map( (link={}, idx) => {
                    const {href, title, action} = link;
                    const target = action || '_blank';
                    const rvalue = rvalues[idx];
                    const rhref = resolveHRefVal(tableModel, href, absRowIdx, rvalue);
                    if (!rhref) return '';
                    mstyle = idx > 0 ? {marginLeft: 3, ...mstyle} : mstyle;
                    return (<ATag key={'ATag_' + idx} href={rhref}
                                  {...{value:rvalue, title, target, style:mstyle}}
                            />);
                })
            }
            </div>
        );
    } else {
        const val = String(rvalues[0]);
        return  <ATag href={val} value={val} target='_blank' style={mstyle}/>;
    }
});


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
        onChange && onChange(v, data, rowIndex, colIdx);
    };

    return ({rowIndex, data, colIdx}) => {
        const val = get(data, [rowIndex, colIdx]);


        if (val === NOT_CELL_DATA) {
            return null;
        } else {
            return (
                <div style={{padding: 2}}>
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

/**
 * an input field renderer that update tableModel.
 * @param p
 * @param p.tbl_id
 * @param p.col         the column for this render
 * @param p.tooltips
 * @param p.style
 * @param p.isReadOnly  a function returning true if this row is read only
 * @param p.validator   a validator function used to validate the input
 * @param p.onChange
 * @returns {Function}
 */
export const inputColumnRenderer = ({tbl_id, cname, tooltips, style={}, isReadOnly, validator, onChange}) => {

    return ({rowIndex, data, colIdx}) => {
        const val = get(data, [rowIndex, colIdx]);
        if (isReadOnly && isReadOnly(rowIndex, data, colIdx)) {
            return <div style={{width: '100%', height: '100%', ...style}}>{val}</div>;
        } else {
            return (
                <div style={{padding: 2, ...style}}>
                    <InputField
                        validator={ validator && ((v) => validator(v, data, rowIndex, colIdx))}
                        tooltip={tooltips}
                        style={{width: '100%', boxSizing: 'border-box', ...style}}
                        value={val}
                        onChange={makeChangeHandler(rowIndex, data, colIdx, tbl_id, cname, validator, onChange)}
                        actOn={['blur','enter']}
                    />
                </div>
            );
        }
    };
};


/**
 * a checkbox renderer that update tableModel.
 * @param p
 * @param p.tbl_id
 * @param p.col         the column for this render
 * @param p.tooltips
 * @param p.style
 * @param p.isReadOnly  a function returning true if this row is read only
 * @param p.validator   a validator function used to validate the input
 * @param p.onChange
 * @returns {Function}
 */
export const checkboxColumnRenderer = ({tbl_id, cname, tooltips, style={}, isReadOnly, validator, onChange}) => {

    return ({rowIndex, data, colIdx}) => {
        const val = get(data, [rowIndex, colIdx]);
        const disabled = isReadOnly && isReadOnly(rowIndex, data, colIdx);
        const changeHandler = makeChangeHandler(rowIndex, data, colIdx, tbl_id, cname, validator, onChange);

        return (
            <div className='TablePanel__checkbox' style={style}>
                <input type = 'checkbox'
                       disabled = {disabled}
                       title = {tooltips}
                       onChange = {(e) => changeHandler({valid: true, value: e.target.checked})}
                       checked = {val}/>
            </div>
        );
    };
};


function makeChangeHandler (rowIndex, data, colIdx, tbl_id, cname, validator, onChange) {
    const table = getTblById(tbl_id);
    const rColIdx = getColumnIdx(table, cname);

    return ({valid, value}) => {
        if (valid) {
            const row = getRowValues(table, rowIndex);
            if (value !== row[rColIdx]) {
                const col =  getColumn(table, cname);
                const rvalue = isColumnType(col, COL_TYPE.INT) ? parseInt(value) :
                    isColumnType(col, COL_TYPE.FLOAT) ? parseFloat(value) : value;
                row[rColIdx] = rvalue;
                const upd = set({tbl_id}, ['tableData', 'data', rowIndex], row);
                if (table.origTableModel) {
                    const origRowIdx = getCellValue(table, rowIndex, 'ROW_IDX');
                    set(upd, ['origTableModel', 'tableData', 'data', origRowIdx], row);
                }
                dispatchTableUpdate(upd);
                onChange && onChange(rvalue, rowIndex, data, colIdx);
            }
        }
    };
};


const ATag = React.memo(({value='', title, href, target, style={}}) => {
    const [,imgStubKey] = value.match(/<img +data-src='(\w+)' *\/>/i) || [];

    if (imgStubKey) {
        value = imageStubMap[imgStubKey] || <img data-src={imgStubKey}/>;   // if a src is given but, not found.. show bad img.
    }

    return <a {...{title, href, target, style}}> {value} </a>;
});

