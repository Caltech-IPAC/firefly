/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useCallback, useEffect, useRef, useState} from 'react';
import {Cell} from 'fixed-data-table-2';
import {get, isEmpty, isString, omit, set, toNumber} from 'lodash';
import {Box, Button, Checkbox, Chip, Link, MenuItem, Sheet, Stack, Tooltip, Typography} from '@mui/joy';

import {FILTER_CONDITION_TTIPS, FilterInfo, NULL_TOKEN} from '../FilterInfo.js';
import {COL_TYPE, formatValue, getCellValue, getColumn, getColumnIdx, getRowValues, getTblById, getTypeLabel, isColumnType, isOfType, splitCols, splitVals} from '../TableUtil.js';
import {SortInfo} from '../SortInfo.js';
import {InputField} from '../../ui/InputField.jsx';
import {SORT_ASC, UNSORTED} from '../SortInfo';
import {copyToClipboard, encodeUrlString, toBoolean} from '../../util/WebUtil.js';

import ASC_ICO from 'html/images/sort_asc.gif';
import DESC_ICO from 'html/images/sort_desc.gif';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField';
import DialogRootContainer, {DropDown} from '../../ui/DialogRootContainer.jsx';
import {FieldGroup} from '../../ui/FieldGroup';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {useStoreConnector} from './../../ui/SimpleComponent.jsx';
import {applyLinkSub, applyTokenSub} from '../../voAnalyzer/VoCoreUtils.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {dispatchTableUpdate} from '../TablesCntlr.js';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {PopupPanel} from '../../ui/PopupPanel.jsx';

import infoIcon from 'html/images/info-icon.png';
import {dd2sex} from '../../visualize/CoordUtil.js';
import MoreHorizIcon from '@mui/icons-material/MoreHoriz';
import {FilterButton} from 'firefly/visualize/ui/Buttons.jsx';


export const headerStyle = {fontSize:'var(--joy-fontSize-sm)', fontWeight:'var(--joy-fontWeight-md)'};  // maybe faulty becuase it's translated from Typography title-sm, which is dynamic.

const html_regex = /<.+>|&.+;/;           // A rough detection of html elements or entities

const imageStubMap = {
    info: <img style={{width:'14px'}} src={infoIcon} alt='info'/>
};


/**
 * Custom cell renderer.
 * @typedef {function} CellRenderer
 * @param {CellInfo} cellInfo the cell info to render
 */

/**
 * Custom cell renderer.
 * @typedef {object} CellInfo
 * @prop {TableColumn} col
 * @prop {string} value of the cell to render
 * @prop {string[]} rvalues values of cell after it gone through a resolver, e.g. Links
 * @prop {string} text text representation of the value, e.g. after number format
 * @prop {boolean} isArray true if value is an array
 * @prop {int} absRowIdx the absolute row index
 * @prop {string} textAlign one of middle, right, left
 * @prop {TableModel} tableModel the full table model; data, columns, meta, etc
 */


/*---------------------------- COLUMN HEADER RENDERERS ----------------------------*/

function SortSymbol({sortDir}) {
    if (sortDir === UNSORTED) return null;
    return (
        <img src={sortDir === SORT_ASC ? ASC_ICO : DESC_ICO} width='9px' height='5px'/>
    );
};

export const HeaderCell = React.memo( ({col, showUnits, showTypes, showFilters, filterInfo, sortInfo, onSort, onFilter, sx={}, tbl_id}) => {
    const {label, name, desc, sortByCols, sortable} = col || {};
    const cdesc = desc || label || name;
    const sortDir = SortInfo.parse(sortInfo).getDirection(name);
    const sortCol = sortByCols ? splitCols(sortByCols) : [name];
    const typeVal = getTypeLabel(col);
    const unitsVal = col.units ? `(${col.units})`: '';
    const clickable = toBoolean(sortable, true) ? 'clickable' : undefined;
    const color = col.DERIVED_FROM ? 'warning' : undefined;

    const onClick = toBoolean(sortable, true) ? () => onSort(sortCol) : () => showInfoPopup('This column is not sortable');
    const centerIt = {justifyContent:'center', alignItems:'center'};

    sx = {py: '2px', ...sx};
    return (
        <Sheet variant='plain' sx={sx}>
            <Stack width={1} height={1} {...centerIt}>
                <Tooltip title={cdesc} sx={{maxWidth:'20em'}}>
                    <Stack width={1} height={1} {...centerIt} className={clickable} onClick={onClick}>
                        <Stack direction='row' {...centerIt}>
                            <Stack textOverflow='ellipsis' overflow='hidden'>
                                <HeaderText val={label || name} level='title-sm' color={color}/>
                            </Stack>
                            <SortSymbol sortDir={sortDir}/>
                        </Stack>
                        { showUnits && <HeaderText val={unitsVal}/> }
                        { showTypes && <HeaderText val={typeVal}/> }
                    </Stack>
                </Tooltip>
                {showFilters && (<Filter {...{cname:name, onFilter, filterInfo, tbl_id}}/>)}
            </Stack>
        </Sheet>
    );
});

export function HeaderText({val, level='body-sm', sx, ...rest}) {
    return (
        <Typography component='div' level={level} {...rest}
                    sx={{lineHeight:1.2, height:'1.2em', whiteSpace:'nowrap', ...sx}}>
            {val || ''}
        </Typography>
    );
}

const blurEnter = ['blur','enter'];

function Filter({cname, onFilter, filterInfo, tbl_id}) {

    const colGetter= () => getColumn(getTblById((tbl_id)), cname) ?? {};
    const col = useStoreConnector(colGetter);
    const [showTooltip, setShowTooltip] = useState(true);
    const dropdownEl = useRef(null);

    useEffect(() => {
        dropdownEl.current?.focus();
    }, [dropdownEl.current]);

    const validator = useCallback((cond) => {
        return FilterInfo.conditionValidator(cond, tbl_id, cname);
    }, [tbl_id, cname]);

    const {name, filterable=true, enumVals} = col;

    if (!filterable) return <div style={{height:19}} />;      // column is not filterable

    const filterInfoCls = FilterInfo.parse(filterInfo);

    const endDecorator = enumVals && (
        <DropDown onFocusChange={(v) => setShowTooltip(!v)}     // only show input tooltip when dropdown is not active
                  slotProps={{button: {sx: {mr: -1}}}}
                  title='Filter to a subset of values in this column'
        >
            <EnumSelect {...{col, tbl_id, filterInfo, filterInfoCls, onFilter}} />
        </DropDown>
    );

    return (
        <InputField
            validator={validator}
            fieldKey={name}
            sx={{width: 1, '--Input-radius': ''}}
            tooltip={showTooltip && FILTER_CONDITION_TTIPS}
            value={filterInfoCls.getFilter(name)}
            onChange={onFilter}
            actOn={blurEnter}
            showWarning={false}
            slotProps={{
                input: {size:'sm', endDecorator }
            }}
        />
    );
}

function EnumSelect({col, tbl_id, filterInfoCls, onFilter}) {
    const {name, enumVals} = col || {};
    const groupKey = 'TableRenderer_enum';
    const fieldKey = tbl_id + '-' + name;
    const options = splitVals(enumVals)                             // split by comma(,) ignoring those in single-quotes
        .map( (s) => {
            const value = s === '' ? '%EMPTY' : s;                  // because CheckboxGroupInputField does not support '' as an option, use '%EMPTY' as substitute
            let label = value;
            if (value === NULL_TOKEN)       label = isOfType(col.type, COL_TYPE.BOOL) ? 'false' : '<NULL>';  // handle null value
            else if (value === '%EMPTY')    label = '<EMPTY_STR>';                                           // handle empty string
            label = label.replace(/^'(.*)'$/, '$1');     // remove enclosed quotes if any
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

    const onClear = () => {
        dispatchValueChange({fieldKey, groupKey, value: '', valid: true});
    };
    const onApply = () => {
        let value = getFieldVal(groupKey, fieldKey);
        if (value) {
            value = splitVals(value).map((s) => s === '%EMPTY' ? '' : s).join();           // convert %EMPTY back into ''
            if (isColumnType(col, COL_TYPE.TEXT)) {
                value = splitVals(value).map((s) => s.startsWith("'") ? s :`'${s.trim()}'`).join(',');
            }
            value = `IN (${value})`;
        }
        onFilter({fieldKey: name, valid: true, value});
    };

    return (
        <Sheet sx={{minWidth:'10em'}}>
            <FieldGroup groupKey='TableRenderer_enum'>
                <Stack spacing={2} px={2}>
                    <Chip onClick={onClear} title='Clear selection'>clear</Chip>
                    <CheckboxGroupInputField slotProps={{ input: {size: 'sm'} }} {...{fieldKey, alignment: 'vertical', options, initialState:{value}}}/>
                    <Stack direction='row' flexGrow={1}>
                        <Button color='primary' variant='solid' size='sm' onClick={onApply} title='Apply selected filter'>Apply</Button>
                    </Stack>
                </Stack>
            </FieldGroup>
        </Sheet>
    );
}

export function SelectableHeader ({checked, onSelectAll, showUnits, showTypes, showFilters, onFilterSelected, sx}) {
    return (
        <Stack alignItems='center' height={1} justifyContent='space-between' py='2px' sx={sx}>
            <Checkbox size='sm'
                tabIndex={-1}
                checked={checked}
                onChange={(e) => onSelectAll(e.target.checked)}/>
            {/*{showUnits && <Box height='1em'/>}*/}
            {/*{showTypes && <Box height='1em'/>}*/}
            {showFilters && <FilterButton  iconButtonSize='28px'
                                 onClick={onFilterSelected}
                                 tip='Filter on selected rows'/>}
        </Stack>
    );
}


export function SelectableCell ({rowIndex, selectInfoCls, onRowSelect, sx={}}) {
    return (
        <Stack alignItems='center' justifyContent='center' height={1} sx={sx}>
            <Checkbox size='sm' mt='1x' mb='1px'
                      tabIndex={-1}
                      checked={selectInfoCls.isSelected(rowIndex)}
                      onChange={(e) => onRowSelect(e.target.checked, rowIndex)}/>
        </Stack>
    );
}

/*---------------------------- CELL RENDERERS ----------------------------*/

/*
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
    renderer.allowActions = true;
    if (col.cellRenderer) {
        const [name='', propsStr] = col.cellRenderer?.split('::');
        const XRef = RendererXRef[name.trim()];
        if (XRef) {
            const XRefProps = getPropsFromStr(propsStr.trim());
            renderer = (props) => <XRef {...{...props, ...XRefProps}}/>;
            renderer.allowActions = false;
        }
    }
    return renderer;
}

export function ContentEllipsis({children, text, textAlign, sx, actions=[]}) {

    const [showActions, setShowActions] = useState(false);
    const [dropdown, setDropdown] = useState(false);
    const contentEl = useRef(null);

    const checkOverflow = (ev) => {
        const w = ev?.currentTarget;
        setShowActions(w?.clientWidth < contentEl?.current?.scrollWidth-4);  // account for paddings
    };

    return (
        <Stack direction='row'  overflow='hidden' whiteSpace='nowrap' alignItems='center' justifyContent={textAlign} sx={sx}
               onMouseEnter={checkOverflow} onMouseLeave={() => setShowActions(false)}
        >
            <Stack ref={contentEl}>{children}</Stack>
            { (showActions || dropdown) &&
                <ActionDropdown {...{text, actions, onChange: (v)=> setDropdown(v) | setShowActions(v)}}/>
            }
        </Stack>
    );
}

function ActionDropdown({text, actions, onChange}) {
    const popupID = 'actions--popup';
    const copyCB = () => {
        copyToClipboard(text);
    };
    const viewAsText = () => {
        DialogRootContainer.defineDialog(popupID, <ViewAsText text={text}/>);
        dispatchShowDialog(popupID);
    };
    return (
        <DropDown button={<MoreHorizIcon/>} onOpenChange={onChange} useIconButton={false}
                  slotProps={{
                      button: {
                          variant:'soft',
                          size:'sm',
                          sx:{position: 'absolute', right:0, paddingInline:'.25em'}
                  }}}
        >
            <MenuItem onClick={copyCB}>Copy to clipboard</MenuItem>
            <MenuItem onClick={viewAsText}>View full text</MenuItem>
            {actions?.map((text, action) => <MenuItem onClick={action}>{text}</MenuItem>)}
        </DropDown>
    );
};



/**
 * A wrapper tag that handles default styles, textAlign, and actions.
 */
export const CellWrapper =  React.memo( (props) => {
    const {tbl_id, startIdx, CellRenderer, style, columnKey, col, rowIndex, data, height, width} = props;

    const cellInfo = getCellInfo({columnKey, col, rowIndex, data, tbl_id, startIdx});
    const {textAlign, text} = cellInfo;

    const content = <CellRenderer {...omit(props, 'Content')} cellInfo={cellInfo}/>;
    const contentWithWrapper = (
        <Stack alignItems={textAlign} justifyContent='center' whiteSpace='nowrap' height={1} width={1}>
            {content}
        </Stack>
    );

    return CellRenderer?.allowActions ? <ContentEllipsis sx={{height:1, width:1}} {...{textAlign, text}}>{content}</ContentEllipsis> : contentWithWrapper;

}, skipCellRender);

function skipCellRender(prev={}, next={}) {
    const {width, colIdx, rowIndex} = prev;
    const {width:nwidth, colIdx:ncolIdx, rowIndex:nrowIndex} = next;

    return width === nwidth && colIdx === ncolIdx && rowIndex === nrowIndex &&
        getCellInfo(prev)?.text === getCellInfo(next)?.text;
}


function ViewAsText({text, ...rest}) {
    const [doFmt, setDoFmt] = useState(true);

    const onChange = (e) => {
        setDoFmt(e.target.checked);
    };

    if (doFmt && text.match(/^\[.+\]$|^{.+}$/)) {
        try {
            text = JSON.stringify(JSON.parse(text), null, 2, 2);
        } catch (e) {}      // if text is not JSON, just show as is.
    }

    const content = doFmt && html_regex.test(text) ? <div dangerouslySetInnerHTML={{__html: text}}/> : <Typography whiteSpace='pre'>{text}</Typography>;

    const label = 'View with formatting';
    return (
        <PopupPanel title={'View full text'} sx={{flexDirection: 'column'}} {...rest}>
            <div style={{display: 'flex', alignItems: 'center'}}>
                <input id='doFormat' type='checkbox' title={label} onChange={onChange} checked={doFmt}/>
                <label htmlFor='doFormat' style={{verticalAlign: ''}}>{label}</label>
            </div>
            <Sheet variant='outlined' sx={{resize:'both', overflow:'auto', minWidth:'30em', minHeight:'15em', maxWidth:'90vw', maxHeight:'90vh', p:1}} >
                {content}
            </Sheet>
        </PopupPanel>

    );
}

/**
 * @see {@link http://www.ivoa.net/documents/VOTable/20130920/REC-VOTable-1.3-20130920.html#ToC54}
 * LinkCell is implementing A.4 using link substitution based on A.1
 * Adding custom vocabularies to content-role:
 * content-role may contain more than one value. Values are separated by semicolon.
 *   encode:values =>  apply encodeURIComponent to all the resolved values.
 */
export const LinkCell = React.memo(({cellInfo, style, ...rest}) => {
    const {absRowIdx, col, textAlign, rvalues, tableModel} = cellInfo || getCellInfo(rest);
    let mstyle = omit(style, 'backgroundColor');
    if (col.links) {
        return (
            <div style={{textAlign, overflow: 'visible'}}>
            {
                col.links.map( (link={}, idx) => {
                    const {href, title, action, role=''} = link;
                    const target = action || '_blank';
                    const rvalue = rvalues[idx];
                    const rhref = applyLinkSub(tableModel, href, absRowIdx, rvalue, role.includes('encode:values'));
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
        const href = get(data, [rowIndex, hrefColIdx], '#');
        const val = value || get(data, [rowIndex, colIdx], '#');
        if (href && href !== '#') {
            return (
                <Cell {...{rowIndex, height, width, columnKey}}>
                    <Link target='_blank' href={href}>{val}</Link>
                </Cell>
            );
        } else {
            return (
                <Cell {...{rowIndex, height, width, columnKey}}>
                    {val}
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
                <Box px={1/4}>
                    <InputField
                        key={rowIndex + '-' +colIdx}
                        validator={(v) => validator(v, data, rowIndex, colIdx)}
                        sx={{'.MuiInput-root':{'minHeight':'3px', 'borderRadius':4}}}
                        tooltip={tooltips}
                        size={size}
                        style={style}
                        value={val}
                        onChange={(v) => changeHandler(rowIndex, data, colIdx, v) }
                        actOn={['blur','enter']}
                        showWarning={false}
                    />
                </Box>
            );
        }
    };
};

/**
 * an input field renderer that update tableModel.
 * @param p
 * @param p.tbl_id
 * @param p.cname         the column for this render
 * @param p.tooltips
 * @param p.style
 * @param p.isReadOnly  a function returning true if this row is read only
 * @param p.validator   a validator function used to validate the input
 * @param p.onChange
 * @returns {Function}
 */
export const inputColumnRenderer = ({tbl_id, cname, tooltips, style={}, isReadOnly, validator, onChange, ...props}) => {

    return ({rowIndex, data, colIdx}) => {
        const val = get(data, [rowIndex, colIdx]);
        if (isReadOnly && isReadOnly(rowIndex, data, colIdx)) {
            return <div style={{width: '100%', height: '100%', ...style}}>{val}</div>;
        } else {
            return (
                <Box {...props}>
                    <InputField
                        key={rowIndex + '-' +colIdx}
                        sx={{'.MuiInput-root':{'minHeight':'3px', 'borderRadius':4}}}
                        validator={ validator && ((v) => validator(v, data, rowIndex, colIdx))}
                        tooltip={tooltips}
                        style={{width: '100%', boxSizing: 'border-box', ...style}}
                        value={val}
                        onChange={makeChangeHandler(rowIndex, data, colIdx, tbl_id, cname, validator, onChange)}
                        actOn={['blur','enter']}
                    />
                </Box>
            );
        }
    };
};


/**
 * a checkbox renderer that update tableModel.
 * @param p
 * @param p.tbl_id
 * @param p.cname       the column name of this renderer
 * @param p.tooltips
 * @param p.sx
 * @param p.isReadOnly  a function returning true if this row is read only
 * @param p.validator   a validator function used to validate the input
 * @param p.onChange
 * @returns {Function}
 */
export const checkboxColumnRenderer = ({tbl_id, cname, tooltips, sx={}, isReadOnly, validator, onChange}) => {

    return ({rowIndex, data, colIdx}) => {
        const val = get(data, [rowIndex, colIdx]);
        const disabled = isReadOnly && isReadOnly(rowIndex, data, colIdx);
        const changeHandler = makeChangeHandler(rowIndex, data, colIdx, tbl_id, cname, validator, onChange);

        return (
            <Stack height={1} alignItems='center' justifyContent='center' style={sx}>
                <input type = 'checkbox'
                       disabled = {disabled}
                       title = {tooltips}
                       onChange = {(e) => changeHandler({valid: true, value: e.target.checked})}
                       checked = {val}/>
            </Stack>
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
            <Stack justifyContent='center' lineHeight='normal'>
                <div style={ustyle}>+{upperVal}</div>
                <div style={lstyle}>-{lowerVal}</div>
            </Stack>
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
    } else {
        label = html_regex.test(label) ? <div dangerouslySetInnerHTML={{__html: label}}/> : label;
    }
    href = encodeUrlString(href);
    return href ? <Link {...{title, href, target, style}}> {label} </Link> : '';
});

export const TextCell = React.memo(({cellInfo, text, ...rest}) => {
    const {absRowIdx, tableModel, value, text:fmtVal} = cellInfo || getCellInfo(rest);
    text  = applyTokenSub(tableModel, text, absRowIdx, fmtVal);
    return html_regex.test(text) ? <div dangerouslySetInnerHTML={{__html: text}}/> : text;
});

export const ColorSwatch = React.memo(({cellInfo, text, size, ...rest}) => {
    const {value} = cellInfo || getCellInfo(rest);
    if (!value) return <div/>;
              // must use style (not sx): {backgroundColor: value}, sx will get overridden.
    return (
        <Stack direction='column' alignItems='center'>
            <Box {...{
                width:size, height:size, border:'1px solid transparent', borderRadius:3,
                style: {backgroundColor: value}
            }}/>
        </Stack>
    );
});

/**
 * Custom coordinate cell rendering
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
    ATag,
    ColorSwatch
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
            style.split(':')[0].trim().replace(/^-ms-/, 'ms-').replace(/-./g, (c) => c.substr(1).toUpperCase()),
            style.split(':').slice(1).join(':').trim()
        ])
        .reduce((styleObj, style) => ({
            ...styleObj,
            [style[0]]: style[1],
        }), {});
    // some
};


export function getPxWidth({text, fontSize, fontWeight}) {
    const div = document.createElement('div');

    // Set the style if given
    if (fontSize)   div.style.fontSize = fontSize + 'px';
    if (fontWeight) div.style.fontWeight = fontWeight + '';
    div.style.fontFamily = 'var(--joy-fontFamily-body)';
    div.textContent = text;

    // Hide the element so it doesn't affect the layout
    div.style.position = 'absolute';
    div.style.visibility = 'hidden';

    document.body.appendChild(div);
    const width = div.getBoundingClientRect().width;
    document.body.removeChild(div);

    return width;
}
