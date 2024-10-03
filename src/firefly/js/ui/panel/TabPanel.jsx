/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useEffect, useState, useCallback, useRef} from 'react';
import {bool, node, func, number, object, oneOfType, shape, string, array} from 'prop-types';
import {
    Tab as JoyTab,
    Tabs as JoyTabs,
    TabPanel as JoyTabPanel,
    TabList, ListItemDecorator, Box, Stack, Sheet, ChipDelete, Tooltip
} from '@mui/joy';
import {tabClasses} from '@mui/joy/Tab';
import sizeMe from 'react-sizeme';
import {omit, isString} from 'lodash';

import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {useFieldGroupConnector} from '../FieldGroupConnector.jsx';
import {useStoreConnector} from '../SimpleComponent.jsx';
import {DropDown} from '../DialogRootContainer.jsx';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getCellValue, getTblById, watchTableChanges} from '../../tables/TableUtil.js';
import {TABLE_FILTER, TABLE_HIGHLIGHT, TABLE_SORT} from '../../tables/TablesCntlr.js';
import {hashCode} from 'firefly/util/WebUtil.js';

/*---------------------------------------------------------------------------------------------
There are several type of Tab panels, each with slightly different behavior and use case.

TabPanel:   TabPanel is a basic functional component designed to work exclusively with props.
            This allow users of this component the flexibility to handle their own states if needed.

Tabs:       A TabPanel with internal state.  State will be lost after unmount.

StatefulTabs:  TabPanel with state backed by ComponentCntlr.
               Selected state is stored as <componentKey>.selected.

FieldGroupTabs:  TabPanel with state backed by FieldGroup
                 The selected index is saved as the value of the field named by fieldKey
----------------------------------------------------------------------------------------------*/


/**
 * @param {object} p
 * @param p.value           value of ID of the selected tab
 * @param p.onTabSelect     callback function on tab select change
 * @param p.showOpenTabs    true to render a dropdown that display all tabs with the ability to switch between them
 * @param p.actions         additional actions rendered as a button at the right end of the tab panel
 * @param p.slotProps       properties to insert into predefined slots of this tab panel
 * @param p.sx              see JoyUI sx
 * @param p.children        a set of Tab
 * @param p.rest            the remaining props intended as pass-along props to Joy Tabs component
 * @return {node}
 * @constructor
 */
export function TabPanel ({value, onTabSelect, showOpenTabs, actions, slotProps, sx, children, ...rest}) {

    const {useFlex, resizable, borderless,
        style={}, headerStyle, contentStyle={}, label, size, ...joyTabsProps} = rest;     // these are deprecated.  the rest(joyTabsProps) are pass-along props to Tabs.

    const onChange = (ev, val) => onTabSelect?.(val);

    // because we support additional actions to the right of the TabList, we need to implement some of TabList feature here.
    const tlVar = slotProps?.tabList?.variant || 'soft';
    const sticky = slotProps?.tabList?.sticky;
    const tlSticky = sticky === 'top' ? {position: 'sticky', top:0, zIndex:1} :
                     sticky === 'bottom' ? {position: 'sticky', bottom:0, zIndex:1} : {};

    return (
        <JoyTabs size='sm'
                 sx={{height: 1, overflow: 'hidden', borderRadius:5, ...sx}}
                 aria-label='tabs'
                 value={value}
                 onChange={onChange}
                 variant='outlined'
                 {...joyTabsProps}
        >
            <Sheet component={Stack} direction='row' variant={tlVar}
                   sx={{
                       pl:1,
                       boxShadow: 'inset 0 -1px var(--joy-palette-divider)',
                       position: 'unset',
                       justifyContent: 'space-between',
                       ...tlSticky
                   }}
            >
                <ResizeTabHeader slotProps={slotProps} children={children}/>
                <Stack direction='row' flexShrink={0}>
                    {actions && actions()}
                    {showOpenTabs && (
                        <DropDown title='Search open tabs'>
                            <OpenTabs onSelect={onTabSelect} selTabId={value}>{children}</OpenTabs>
                        </DropDown>
                    )}
                </Stack>
            </Sheet>
            {children}
        </JoyTabs>
    );
}

TabPanel.propTypes = {
    value: oneOfType([number, string]),
    onTabSelect: func,
    showOpenTabs: bool,
    actions: func,
    sx: object,
    slotProps: shape({
        tabList: object,
        tab: object,            // will inject into each one
    })
};


/*
 * Tab panel with internal state
 * State will be lost after unmount.
 */
export const Tabs =  ({defaultSelected, onTabSelect, ...rest}) => {

    const [selected, setSelected] = useState(defaultSelected ?? getDefaultTab(rest.children));

    const onSelect = (val) => {
        setSelected(val);
        onTabSelect && onTabSelect(val);
    };

    return (<TabPanel {...rest} value={selected} onTabSelect={onSelect} />);
};

Tabs.propTypes = {
    defaultSelected:  oneOfType([number, string]),
    ...omit(TabPanel.propTypes, 'value'),
};


/*
 * Tab panel with ComponentCntlr supported state
 * Selected state is stored as <componentKey>.selected
 */
export const StatefulTabs = ({defaultSelected, onTabSelect, componentKey, ...rest}) => {

    const selected = useStoreConnector( () => getComponentState(componentKey)?.selected ?? defaultSelected ?? getDefaultTab(rest.children));

    const onSelect = (val) => {
        dispatchComponentStateChange(componentKey, {selected: val});
        onTabSelect && onTabSelect(val);
    };

    return (<TabPanel {...rest} onTabSelect={onSelect} value={selected} />);
};

StatefulTabs.propTypes = {
    componentKey: string,
    ...Tabs.propTypes
};

/**
 * TabPanel with FieldGroup supported state
 * The selected index is saved as the value of the field named by fieldKey
 */
export const FieldGroupTabs = memo( ({children, ...props}) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);

    const onChange = useCallback((value) => {
        fireValueChange({value});
        viewProps.onTabSelect?.(value);
    }, [viewProps, fireValueChange]);

    const newProps= {
        ...omit(props, Object.keys(fieldPropTypes)),        // useFieldGroupConnector is not removing all of its props, causing bad props going into the Tabs
        value: viewProps.value,
        children,
        onTabSelect: onChange
    };

    return (<TabPanel {...newProps}/>);
});

const fieldPropTypes = {
    fieldKey : string,
    groupKey : string,
    forceReinit:  bool,
    options: array,
    initialState: shape({
        value: string,
    }),
};

FieldGroupTabs.propTypes = {
    ...fieldPropTypes,
    ...TabPanel.propTypes
};

/*---------------------------------------------------------------------------------------------
 Exported function and components
----------------------------------------------------------------------------------------------*/

/*
 * For backward compatibility, this is a composite of JoyUI's Tab and TabPanel
 * 'label' is converted to Tab, and 'children' are wrapped inside a Joy TabPanel
 * It is designed to be used with TabPanel.
 */
export const Tab = React.memo(({id, children, value, sx, removable, onTabRemove, colorSwatch, label, name, ...props}) => {
    // removable, onTabRemove;  not used by JoyTabPanel
    value ??= getTabId({id, name, label});
    return (
        <JoyTabPanel value={value} sx={{p:0, overflow:'hidden', ...sx}} {...props}>
            <Stack height={1} width={1}>
                {children}
            </Stack>
        </JoyTabPanel>
    );
});


Tab.propTypes = {
    id: string,           // ID for this tab; otherwises index will be used.
    label: node,
    name: string,
    value: oneOfType([number, string]),
    sx: object,
    startDecorator: node,
    removable: bool,
    onTabRemove: func,
    colorSwatch: string,
};


export function switchTab(componentKey, selected) {
    dispatchComponentStateChange(componentKey, {selected});
}


/*---------------------------------------------------------------------------------------------
 Internal use only
----------------------------------------------------------------------------------------------*/

const ResizeTabHeader = sizeMe({refreshRate: 16})(TabHeader);

function TabHeader({children, slotProps}) {
    const toolbarEl = useRef(null);
    const [maxTitleWidth, setMaxTitleWidth] = useState(400);


    const childrenAry = React.Children.toArray(children);
    // get the headers(Tab)
    const tabHeaders = childrenAry.map((c, idx) => getHeaderFromTab(c.props, idx, maxTitleWidth));

    const tabListVar = slotProps?.tabList?.variant || 'soft';
    const activeBg = tabListVar === 'plain' ? 'neutral.softBg' : 'background.surface';

    useEffect(() => {
        const tbEl = toolbarEl.current;
        if (tbEl) {
            const maxWidth = calcOptimalTabSize(tbEl);
            setMaxTitleWidth(maxWidth);
        }
    }, [toolbarEl?.current?.getBoundingClientRect()?.width]);

    return (
        <TabList
            ref={toolbarEl}
            sx={ (theme) => ( {
                    flexGrow: 1,
                    overflow: 'auto',
                    scrollSnapType: 'x mandatory',              // make tab snap to the nearest tab
                    fontWeight: theme.fontWeight.md,
                    scrollbarWidth: 'none',                     // ensure scrollbar is not visible
                    '&::-webkit-scrollbar': { display: 'none' },// ensure scrollbar is not visible
                    [`& .${tabClasses.root}`]: {
                        '&[aria-selected="true"]': {            // apply this to the selected tab
                            bgcolor: activeBg,                  // set tab background to active color
                            borderColor: 'divider',
                            '&::before': {                      // this is the strip under the tab to cover the border
                                content: '""',
                                display: 'block',
                                position: 'absolute',
                                height: 2,
                                bottom: -2,
                                left: 0,
                                right: 0,
                                bgcolor: activeBg,              // set this to the active color so that it look like it's part of the active tab
                            },
                        },
                        '&[aria-selected="false"]': {            // add pipes after non-selected tabs
                            '&::after': {
                                content: '""',
                                display: 'block',
                                position: 'absolute',
                                height: '1.1rem',
                                bottom: -2,
                                left: 0,
                                right: -2,
                                zIndex: 1,                      //zIndex necessary so the hover does not cover pipe
                                borderRightColor: 'divider',
                                borderRightStyle: 'solid',
                                borderRightWidth: '1px'
                            },
                        },
                    }
            } )
            }
            {...{variant:tabListVar, ...slotProps?.tabList}}
        >
            {tabHeaders}
        </TabList>
    );
}

function getHeaderFromTab({name, value, label, startDecorator, removable, onTabRemove, ...rest}, idx, maxTitleWidth) {
    const {selected, onSelect, style, colorSwatch, id, ...joyTabProps} = rest;     // deprecated; filtered out.

    // to support deprecated props
    label ??= name;
    value ??= getTabId({id, name, label, idx});


    if (isString(label)) {
        label = <Box title={label} sx={{whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis'}}>{label}</Box>;
    }

    return (
        <JoyTab key={idx} component='div' {...{value, indicatorPlacement:'top', ...joyTabProps}}
                sx={{
                    flex: 'none', scrollSnapAlign: 'start',
                    justifyContent: 'space-between',
                    maxWidth: maxTitleWidth,
                    minWidth: removable ? 60 : 40
                }}
        >
            {startDecorator && (
                <ListItemDecorator>
                    {startDecorator}
                </ListItemDecorator>
            )}
            {label}
            {removable &&
                <Tooltip title='Remove Tab'>
                    <ChipDelete sx={{'--Chip-deleteSize': '1.2rem', zIndex:2}}
                                onClick={(e) => {
                                    onTabRemove && onTabRemove(name);
                                    e.stopPropagation?.();
                                }}
                    />
                </Tooltip>
            }
        </JoyTab>
    );
}

/*----------------------------------------------------------------------------------------------*/




function OpenTabs({onSelect, selTabId, children}) {

    const [tableModel, setTableModel] = useState();
    const tbl_id = hashCode(getAllTabId(children).join('|'));

    useEffect(() => {       //
        // create table model for the drop down
        const childrenAry = React.Children.toArray(children);
        const hasColor= childrenAry.some( ({props})  => Boolean(props?.colorSwatch));
        const columns = [
            {name: 'Open Tabs', width: 50},
            {name: 'tabID', visibility: 'hidden'}
        ];
        if (hasColor) {
            columns.unshift({name: '', width: 1, cellRenderer: 'ColorSwatch::size=10px',
                sortable:false, filterable:false});
        }
        const highlightedRow = childrenAry.findIndex((child) => getTabId(child.props) === selTabId);
        const data = makeOpenTabsData(childrenAry,hasColor);
        setTableModel({tbl_id, tableData: {columns, data}, highlightedRow, totalRows: data.length});

        return watchTableChanges(tbl_id, [TABLE_HIGHLIGHT, TABLE_SORT, TABLE_FILTER], () => {
            const tbl = getTblById(tbl_id) || {};
            const {highlightedRow} = tbl;
            const newSelTabId = getCellValue(tbl, highlightedRow, 'tabID');
            selTabId && onSelect(newSelTabId);
        }, tbl_id);          // make watcherId same as tbl_id so there can only be one watcher per tabpanel
    }, [tbl_id]);

    const width = '30rem';
    return  (
        <Stack width={width} height={200} position='relative'>
            <TablePanel tbl_ui_id={tbl_id+'-ui'} tableModel={tableModel} showTypes={false} slotProps={{root: {variant:'plain'}}}
                        showToolbar={false} showFilters={true} selectable={false} showOptionButton={false}/>
        </Stack>
    );
}

function makeOpenTabsData(childrenAry,hasColor) {
    return childrenAry.map((child, idx) => {
        const p = child?.props || {};
        const title = isString(p.label) ? p.label : p.name || `[blank]-${idx}`;
        const tabId = getTabId(p);
        return hasColor ? [p.colorSwatch ?? '', title, tabId] : [title, tabId];
    });

}

function getTabId({id, name, label, idx}) {
    return id ?? name ?? label ?? idx;
}

function getDefaultTab(children) {
    return getAllTabId(children)[0];
}

function getAllTabId(children) {
    return React.Children.toArray(children).map((c) => getTabId(c.props)).filter((id) => id);
}

function calcOptimalTabSize(tabEl) {
    const tabAry = Array.from(tabEl.children).filter((c) => c.offsetWidth > 0);        // ignore hidden children.
    const availW = tabEl.getBoundingClientRect().width;
    const maxW = availW/tabAry.length;
    const relTabs = tabAry.filter((t) => t.offsetWidth > maxW);
    if (relTabs.length === 0) return maxW;      // all tabs fit, no adjustment needed.

    // these don't need adjusting. so, ignore them in calculation
    const ignTabsW = tabAry.map((t) => t.offsetWidth)
                           .filter((w) => w <= maxW).reduce((sum, w) => sum + w, 0);
    const adjAvailWidth = availW - ignTabsW;
    return adjAvailWidth/relTabs.length;
}
