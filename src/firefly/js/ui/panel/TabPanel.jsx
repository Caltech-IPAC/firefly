/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useEffect, useState, useCallback, useRef} from 'react';
import PropTypes, {object, shape} from 'prop-types';
import {
    Tab as JoyTab,
    Tabs as JoyTabs,
    TabPanel as JoyTabPanel,
    TabList, ListItemDecorator, Box, Chip, Stack, Sheet} from '@mui/joy';
import {tabClasses} from '@mui/joy/Tab';
import sizeMe from 'react-sizeme';
import {omit, isString, uniqueId} from 'lodash';

import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {useFieldGroupConnector} from '../FieldGroupConnector.jsx';
import {useStoreConnector} from '../SimpleComponent.jsx';
import {hideDropDown, isDropDownShowing, showDropDown} from '../DialogRootContainer.jsx';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getCellValue, getTblById, watchTableChanges} from '../../tables/TableUtil.js';
import {TABLE_FILTER, TABLE_HIGHLIGHT, TABLE_SORT} from '../../tables/TablesCntlr.js';


/*---------------------------------------------------------------------------------------------
There are several type of Tab panels, each with slightly different behavior and use case.

TabPanel:   TabPanel is a basic functional component designed to work exclusively with props.
            This allow users of this component the flexibility to handle their own states if needed.

Tabs:       A TabPanel with internal state.  State will be lost after unmount.

StatefulTabs:  TabPanel with state backed by ComponentCntlr.
               Selected state is stored as <componentKey>.selectedIdx.

FieldGroupTabs:  TabPanel with state backed by FieldGroup
                 The selected index is saved as the value of the field named by fieldKey
----------------------------------------------------------------------------------------------*/


/**
 * @param {object} p
 * @param p.value           value of ID of the selected tab
 * @param p.onTabSelect     callback function on tab select change
 * @param p.tabId           unique ID to identify this tab panel
 * @param p.showOpenTabs    true to render a dropdown that display all tabs with the ability to switch between them
 * @param p.actions         additional actions rendered as a button at the right end of the tab panel
 * @param p.slotProps       properties to insert into predefined slots of this tab panel
 * @param p.sx              see JoyUI sx
 * @param p.children        a set of Tab
 * @param p.rest            the remaining props intended as pass-along props to Joy Tabs component
 * @return {node}
 * @constructor
 */
export function TabPanel ({value, onTabSelect, tabId=uniqueTabId(), showOpenTabs, actions, slotProps, sx, children, ...rest}) {

    const {useFlex, resizable, borderless,
        style={}, headerStyle, contentStyle={}, label, size, ...joyTabsProps} = rest;     // these are deprecated.  the rest(joyTabsProps) are pass-along props to Tabs.

    const arrowEl = useRef(null);

    useEffect( ()=> {
        if (isDropDownShowing(tabId)) handleOpenTabs({tabId, doOpen: false});
    }, [tabId]);

    // get the content(JoyTabPanel)
    const childrenAry = React.Children.toArray(children);
    const tabContents = childrenAry.map((c, idx) => getContentFromTab(c.props, idx, slotProps));

    const showTabs = (ev) => handleOpenTabs({ev, doOpen: !isDropDownShowing(tabId), tabId, onSelect: onTabSelect, arrowEl, childrenAry});
    const onChange = useCallback((ev, val) => onTabSelect?.(val), []);

    // because we support additional actions to the right of the TabList, we need to implement some of TabList feature here.
    const tlVar = slotProps?.tabList?.variant || 'soft';
    const sticky = slotProps?.tabList?.sticky;
    const tlSticky = sticky === 'top' ? {position: 'sticky', top:0} :
                     sticky === 'bottom' ? {position: 'sticky', bottom:0} : {};
    return (
        <JoyTabs key={tabId}
                 size='sm'
                 sx={{height: 1, boxSizing: 'border-box', ...sx}}
                 aria-label='tabs'
                 value={value}
                 onChange={onChange}
                 variant='outlined'
                 {...joyTabsProps}
        >
            <Sheet component={Stack} direction='row' variant={tlVar}
                   sx={{
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
                        <Chip onClick={showTabs} ref={arrowEl} size='sm' title='Search open tabs' sx={{m:'2px'}}>
                            <div className='arrow-down'/>
                        </Chip>
                    )}
                </Stack>
            </Sheet>
            {tabContents}
        </JoyTabs>
    );
}

TabPanel.propTypes = {
    tabId: PropTypes.string,            // a unique identifier used as an ID for this component
    value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    onTabSelect: PropTypes.func,
    showOpenTabs: PropTypes.bool,
    actions: PropTypes.elementType,
    sx: PropTypes.object,
    slotProps: shape({
        tabList: object,
        tab: object,            // will inject into each one
        panel: object        // will inject into each one
    })
};


/**
 * Tab panel with internal state
 * State will be lost after unmount.
 */
export const Tabs = React.memo( ({defaultSelected, onTabSelect, ...rest}) => {

    defaultSelected = convertToTabValue(rest.children, defaultSelected);
    const [selectedIdx, setSelectedIdx] = useState(defaultSelected);

    let localSelectIdx= selectedIdx; // keep a closure variable because useCallback if memorized not recreated on every render
                                     // I am not sure why we need a memorized callback here but i don't want to change that.
    const onSelect = useCallback( (index,id,name) => {
        if (index !== localSelectIdx) {
            setSelectedIdx(index);
            localSelectIdx= index;
            onTabSelect && onTabSelect(index,id,name);
        }
    });

    return (<TabPanel {...rest} value={selectedIdx} onTabSelect={onSelect} />);
});

Tabs.propTypes = {
    defaultSelected:  PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    ...omit(TabPanel.propTypes, 'value'),
};


/**
 * Tab panel with ComponentCntlr supported state
 * Selected state is stored as <componentKey>.selectedIdx
 */
export const StatefulTabs = React.memo( ({defaultSelected, onTabSelect, componentKey, ...rest}) => {

    defaultSelected = convertToTabValue(rest.children, defaultSelected);
    let selectedIdx = useStoreConnector( () => getComponentState(componentKey)?.selectedIdx ?? defaultSelected);

    const onSelect = useCallback( (index,id,name) => {
        dispatchComponentStateChange(componentKey, {selectedIdx: index});
        onTabSelect && onTabSelect(index,id,name);
    }, []);

    useEffect( ()=> {
        if (selectedIdx >= rest.children.length) {
            // selectedIdx is greater than the number of tabs.. update store's state
            selectedIdx = rest.children.length-1;
            dispatchComponentStateChange(componentKey, {selectedIdx});
        }
    });

    return (<TabPanel {...rest} onTabSelect={onSelect} value={selectedIdx} />);

});

StatefulTabs.propTypes = {
    componentKey: PropTypes.string,
    ...Tabs.propTypes
};

/**
 * TabPanel with FieldGroup supported state
 * The selected index is saved as the value of the field named by fieldKey
 */
export const FieldGroupTabs = memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    viewProps.value = convertToTabValue(props.children, viewProps.value);

    const onChange = useCallback((idx, id, name) => {
        let value= id||name;
        if (!value) value= idx;

        fireValueChange({ value});
        if (viewProps.onTabSelect) {
            viewProps.onTabSelect(idx, id, name);
        }
    }, [viewProps, fireValueChange]);

    const newProps= {
        ...viewProps,
        defaultSelected : viewProps.value,
        onTabSelect: onChange
    };
    return (<Tabs {...newProps} />);
});

FieldGroupTabs.propTypes = {
    fieldKey: PropTypes.string,
    forceReinit: PropTypes.bool,
    initialState: PropTypes.shape({
        value: PropTypes.string,
    }),
    ...omit(Tabs.propTypes, 'defaultSelected')     //  defaultSelected is not used.. use value for defaultSelected.
};

/*---------------------------------------------------------------------------------------------
 Exported function and components
----------------------------------------------------------------------------------------------*/

/**
 * For backward compatibility, this is a composite of JoyUI's Tab and TabPanel
 * 'label' is converted to Tab, and 'children' are wrapped inside a Joy TabPanel
 * It is designed to be used with TabPanel.
 */
export const Tab = React.memo( ({label, name, value, startDecorator, removable, onTabRemove}) => {
    // maxTitleWidth:  deprecated.  default to 400
    return;
});

Tab.propTypes = {
    label:  PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
    name: PropTypes.string,
    value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    startDecorator: PropTypes.node,
    removable: PropTypes.bool,
    onTabRemove: PropTypes.func,
};


export function switchTab(componentKey, selectedIdx) {
    dispatchComponentStateChange(componentKey, {selectedIdx});
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
            const width = tbEl.getBoundingClientRect().width;
            setMaxTitleWidth( (width/tabHeaders.length) -4 );   // -4 account for margin and padding
        }
    }, [toolbarEl?.current?.getBoundingClientRect()?.width]);

    return (
        <TabList
            ref={toolbarEl}
            sx={{
                flexGrow: 1,
                overflow: 'auto',
                scrollSnapType: 'x mandatory',              // make tab snap to the nearest tab
                '&::-webkit-scrollbar': { display: 'none' },
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
                },
            }}
            {...{variant:tabListVar, ...slotProps?.tabList}}
        >
            {tabHeaders}
        </TabList>
    );
}

function getHeaderFromTab({name, value, label, startDecorator, removable, onTabRemove, ...rest}, idx, maxTitleWidth) {
    const {selected, onSelect, style, id, ...joyTabProps} = rest;     // deprecated; filtered out.

    // to support deprecated props
    label ??= name;
    value ??= id ?? idx;


    if (isString(label)) {
        label = <Box title={label} sx={{whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis'}}>{label}</Box>;
    }

    return (
        <JoyTab key={idx} component='div' {...{value, indicatorPlacement:'top', ...joyTabProps}}
                sx={{
                    flex: 'none', scrollSnapAlign: 'start',
                    maxWidth: maxTitleWidth,
                    minWidth: 100
                }}
        >
            {startDecorator && (
                <ListItemDecorator>
                    {startDecorator}
                </ListItemDecorator>
            )}
            {label}
            {removable &&
                <Chip variant='soft'
                      onClick={(e) => {
                          onTabRemove && onTabRemove(name);
                          e.stopPropagation && e.stopPropagation();
                }}>x</Chip>
            }
        </JoyTab>
    );
}

function getContentFromTab({value, id, children}, idx, slotProps) {
    value ??= id ?? idx;
    const props = slotProps?.panel;

    return (
        <JoyTabPanel key={idx} value={value} sx={{p:1, ...props?.sx}} {...props} >
            <Stack height={1} width={1}>
                {children}
            </Stack>
        </JoyTabPanel>
    );
}


/*----------------------------------------------------------------------------------------------*/

function handleOpenTabs({ev, doOpen, tabId, onSelect, childrenAry, arrowEl}) {
    ev?.stopPropagation?.();
    if (doOpen) {
        // create table model for the drop down
        const columns = [{name: 'OPEN TABS', width: 50}];
        const highlightedRow = childrenAry.findIndex((child) => child?.props?.selected);
        const tbl_id = tabId;
        const data = getTabTitles(childrenAry);
        const tableModel = {tbl_id, tableData: {columns, data}, highlightedRow, totalRows: data.length};
        // monitor for changes
        watchTableChanges(tabId, [TABLE_HIGHLIGHT, TABLE_SORT, TABLE_FILTER], () => {
            const tbl = getTblById(tabId) || {};
            const {highlightedRow} = tbl;
            const selRowIdx = getCellValue(tbl, highlightedRow, 'ROW_IDX');
            if (selRowIdx >= 0) onSelect(selRowIdx);
        }, tabId);          // make watcherId same as tabId so there can only be one watcher per tabpanel

        const width = 381;
        const content =  (
            <Stack width={width} height={200} position='relative'>
                <TablePanel tbl_ui_id={tabId+'-ui'} tableModel={tableModel} showTypes={false}
                            showToolbar={false} showFilters={true} selectable={false} showOptionButton={false}/>
            </Stack>);
        showDropDown({id: tabId, content, atElRef: arrowEl.current, locDir: 43,
                    style: {marginLeft: -width+10, marginTop: -4}, wrapperStyle: {zIndex: 110}}); // 110 is the z-index of a dropdown
    } else {
        hideDropDown(tabId);
    }
};

function getTabTitles(childrenAry) {
    return childrenAry.map((child, idx) => {
        const p = child?.props;
        return [isString(p.label) ? p.label : p.name || `[blank]-${idx}`];
    });

}

/**
 * Old API uses only tab index.  So, this is needed to return the value of the tab defined by `id`.
 * @param children  the tabs to search
 * @param value     index of the Tab or the value of that Tab
 * @return {string|number} the value of the Tab.
 */
function convertToTabValue(children, value=0) {
    return React.Children.toArray(children)[value]?.props.id ?? value;
}

function uniqueTabId() {
    return `TapPanel-${uniqueId()}`;
}


