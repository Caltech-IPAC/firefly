/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PureComponent, useRef, useCallback, useState} from 'react';
import {Cell} from 'fixed-data-table-2';
import {set, get, isEqual, pick, omit, isEmpty, isString, toNumber} from 'lodash';

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
import {applyLinkSub, applyTokenSub} from '../../util/VOAnalyzer.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {dispatchTableUpdate} from '../../tables/TablesCntlr.js';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {PopupPanel} from '../../ui/PopupPanel.jsx';

import infoIcon from 'html/images/info-icon.png';
import {dd2sex} from '../../visualize/CoordUtil.js';

const html_regex = /^<.+>$/;                    // A very limited check.  Will limit or remove the use of embedded HTML in the future
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
        rvalues =  col.links.map( ({value:val}) => applyTokenSub(tableModel, val, absRowIdx, value) );
        text = rvalues.join(' ');
    }
    textAlign = textAlign || rvalues.length > 1 ? 'middle': isColumnType(col, COL_TYPE.NUMBER) ? 'right' : 'left';
    return {col, value, rvalues, text, isArray, textAlign, absRowIdx, tableModel};
}

/**
 * Translate a string in this format into an object with properties.
 * p1=val1, p2=val2, [...p3=val3]
 * @param s  input string
 * @returns {object}
 */
function getPropsFromStr(s='') {
    return s.trim()
            .split(',')
            .reduce( (acc, cur) => {
                const [, k='',v=''] = cur.trim().match(/([^=]+)=(.*)/);
                if (k) acc[k.trim()] = v.trim();
                return acc;
            }, {});
}

export function makeDefaultRenderer(col={}) {
    let renderer = (col.type === 'location' || !isEmpty(col.links)) ? LinkCell : TextCell;
    if (col.cellRenderer) {
        const [name='', propsStr] = col.cellRenderer.split('::');
        const XRef = RendererXRef[name.trim()];
        const XRefProps = getPropsFromStr(propsStr.trim());
        renderer = XRef ? (props) => <XRef {...{...props, ...XRefProps}}/> : renderer;
    }
    return renderer;
}


/**
 * A wrapper tag that handles default styles, textAlign, and actions.
 */
export const CellWrapper =  React.memo( (props) => {
    const {tbl_id, startIdx, CellRenderer, style, columnKey, col, rowIndex, data, height, width} = props;
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
             onMouseLeave={() => setHasActions(false)} style={{display: 'flex', height: '100%'}}>
            <div style={{textAlign, lineHeight, ...style}} className='public_fixedDataTableCell_cellContent'>
                <CellRenderer {...omit(props, 'Content')} cellInfo={cellInfo}/>
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

    const label = 'View with formatting';
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
                    const rhref = applyLinkSub(tableModel, href, absRowIdx, rvalue);
                    if (!rhref) return '';
                    mstyle = idx > 0 ? {marginLeft: 3, ...mstyle} : mstyle;
                    return (<ATag key={'ATag_' + idx} href={rhref}
                                  {...{label:rvalue, title, target, style:mstyle}}
                            />);
                })
            }
            </div>
        );
    } else {
        const val = String(rvalues[0]);
        return  <ATag href={val} label={val} target='_blank' style={mstyle}/>;
    }
});


/**
 * creates a link cell renderer using the cell data as href.
 * @param obj
 * @param obj.hrefColIdx
 * @param obj.value  display this value for every cell.
 * @returns {Function}
 */
export const createLinkCell = ({hrefColIdx, value}) => {

    return ({rowIndex, data, colIdx, height, width, columnKey}) => {
        hrefColIdx = hrefColIdx || colIdx;
        const href = get(data, [rowIndex, hrefColIdx], 'undef');
        const val = value || get(data, [rowIndex, colIdx], 'undef');
        if (href === 'undef' || href === '#') {
            return (
                <Cell {...{rowIndex, height, width, columnKey}}>
                    {val}
                </Cell>
            );
        } else {
            return (
                <Cell {...{rowIndex, height, width, columnKey}}>
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
                <div style={{padding: '0 2px 0 2px', marginTop:'-2px'}}>
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

/**
 */

/**
 * Predefined cell renderers
 *
 * Usage when given as a column's property:
 *   col.<col-name>.cellRenderer = <renderer-name>:: <key1=val1>, <key2=val2>, <key3=val3>, ...
 *
 *   An example of a NumberRange renderer applied to a 'dist' column
 *
 *   <pre><code><font size=-3>
 * col.dist.cellRenderer = NumRange::base=val, upper=val, lower=val, ustyle=color:green;font-style:italic
 *   </font></code></pre>
 *
 *    `val` may contains token that can be substitute by values from other columns.
 *
 * see `A.1 link substitution` for syntax
 *
 * {@link http://www.ivoa.net/documents/VOTable/20130920/REC-VOTable-1.3-20130920.html#ToC54}
 *
 * @public
 * @namespace firefly.ui.table.renderers
 */


/**
 * A number with upper and lower bounds, displaying 3 values in the form or [base +upper -lower] or [base +- val] if only one of upper or lower exists.
 * @param p         parameters
 * @param [p.base]    base value, defaults to cell value if not given
 * @param [p.upper]   upper value
 * @param [p.lower]   lower value
 * @param [p.style]   a style object or string to apply to the container
 * @param [p.lstyle]  a style object or string to apply to the lower value
 * @param [p.ustyle]  a style object or string to apply to the upper value
 * @return React.element
 *
 * @public
 * @func NumberRange
 * @memberof firefly.ui.table.renderers
 */
export const NumberRange = React.memo(({cellInfo, base, upper, lower, style={}, lstyle, ustyle, ...rest}) => {
    const {absRowIdx, tableModel, value} = cellInfo || getCellInfo(rest);

    const baseVal  = applyTokenSub(tableModel, base, absRowIdx, value);
    const upperVal = upper && applyTokenSub(tableModel, upper, absRowIdx);
    const lowerVal = lower && applyTokenSub(tableModel, lower, absRowIdx);
    let delta = <div>&#177; {upperVal || lowerVal || 0}</div>;
    style = isString(style) ? parseStyles(style) : style;
    lstyle = isString(lstyle) ? parseStyles(lstyle) : lstyle;
    ustyle = isString(ustyle) ? parseStyles(ustyle) : ustyle;

    if (upperVal && lowerVal) {
        delta = (
            <div className='CellRenderer__numrange'>
                <div style={ustyle}>+{upperVal}</div>
                <div style={lstyle}>-{lowerVal}</div>
            </div>
        );
    }

    return (
        <div style={{display: 'inline-flex', ...style}}>
            <div style={{marginRight: 5}}>{baseVal}</div>
            {delta}
        </div>
    );
});

/**
 * An image with optional before and after description.
 * @param p             parameters
 * @param [p.src]       url source of the image, defaults to cell value if not given
 * @param [p.style]     a style object or string to apply to the cell
 * @param [p.alt]       alternate text for an image, if the image cannot be displayed. Defaults to cell value if not given
 * @param [p.before]    text before the image
 * @param [p.after]     text after the image
 * @return React.element
 *
 * @public
 * @func ImageCell
 * @memberof firefly.ui.table.renderers
 */
export const ImageCell = React.memo(({cellInfo, style={}, alt, src, before, after, ...rest}) => {
    const {absRowIdx, tableModel, value} = cellInfo || getCellInfo(rest);
    src  = applyTokenSub(tableModel, src, absRowIdx, value);
    alt = alt || value;
    before  = applyTokenSub(tableModel, before, absRowIdx);
    after   = applyTokenSub(tableModel, after, absRowIdx);
    style = isString(style) ? parseStyles(style) : style;
    src  = applyTokenSub(tableModel, src, absRowIdx, value);
    return (<div style={{display: 'inline-flex', ...style}}>
        {before && <div>{before}</div>}
        <img style={{margin: 3}} src={src} alt={alt} />
        {after && <div>{after}</div>}
    </div>);
});

/**
 * An anchor <a> tag
 * @param p             parameters
 * @param [p.href]      url to link to
 * @param [p.label]     text to display as a link
 * @param [p.title]     description to show on mouse over
 * @param [p.target]    name of the window to open in
 * @param [p.style]     a style object or string to apply to the cell
 * @return React.element
 *
 * @public
 * @func ATag
 * @memberof firefly.ui.table.renderers
 */
export const ATag = React.memo(({cellInfo, label, title, href, target, style={}, ...rest}) => {
    const {absRowIdx, tableModel, text} = cellInfo || getCellInfo(rest);
    label  = applyTokenSub(tableModel, label, absRowIdx, text) + '';
    href  = applyTokenSub(tableModel, href, absRowIdx, text);
    style = isString(style) ? parseStyles(style) : style;

    const [,imgStubKey] = label.match(/<img +data-src='(\w+)' *\/>/i) || [];

    if (imgStubKey) {
        label = imageStubMap[imgStubKey] || <img data-src={imgStubKey}/>;   // if a src is given but, not found.. show bad img.
    }

    return <a {...{title, href, target, style}}> {label} </a>;
});

export const TextCell = React.memo(({cellInfo, text, ...rest}) => {
    const {absRowIdx, tableModel, value, text:fmtVal} = cellInfo || getCellInfo(rest);
    text  = applyTokenSub(tableModel, text, absRowIdx, fmtVal);
    return (text?.search && text.search(html_regex) >= 0) ? <div dangerouslySetInnerHTML={{__html: text}}/> : text;
});

/**
 * An anchor <a> tag
 * @param p             parameters
 * @param [p.hms]       HMS value
 * @param [p.dms]       DMS value
 * @param [p.style]     a style object or string to apply to the cell
 * @return React.element
 *
 * @public
 * @func CoordCell
 * @memberof firefly.ui.table.renderers
 */
export const CoordCell = React.memo(({cellInfo, hms, dms, style, ...rest}) => {
    const {absRowIdx, tableModel, value} = cellInfo || getCellInfo(rest);
    hms = toNumber(applyTokenSub(tableModel, hms, absRowIdx));
    dms = toNumber(applyTokenSub(tableModel, dms, absRowIdx));
    style = isString(style) ? parseStyles(style) : style;

    hms = isNaN(hms) ? '' : dd2sex(hms, false, true);
    dms = isNaN(dms) ? '' : dd2sex(dms, true, true);
    const text = [hms, dms].filter((v) => v).join(' ') || value;
    return style ? <div style={style}>{text}</div> : text;
});


export const RendererXRef = {
    NumberRange,
    CoordCell,
    ImageCell,
    ATag
};



/**
 * Parses a string of inline styles into a javascript object with casing for react
 * @param {string} styles
 * @returns {Object}
 */
const parseStyles = (styles='') =>  {
    return styles
        .split(';')
        .filter((style) => style.split(':')[0] && style.split(':')[1])
        .map((style) => [
            style.split(':')[0].trim().replace(/^-ms-/, 'ms-').replace(/-./g, c => c.substr(1).toUpperCase()),
            style.split(':').slice(1).join(':').trim()
        ])
        .reduce((styleObj, style) => ({
            ...styleObj,
            [style[0]]: style[1],
        }), {});
    // some
};