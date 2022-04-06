/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useEffect, useState, useCallback, useRef} from 'react';
import PropTypes from 'prop-types';
import sizeMe from 'react-sizeme';
import {omit, isString, uniqueId} from 'lodash';

import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {useFieldGroupConnector} from '../FieldGroupConnector.jsx';
import {useStoreConnector} from '../SimpleComponent.jsx';
import {hideDropDown, isDropDownShowing, showDropDown} from '../DialogRootContainer.jsx';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getCellValue, getTblById, watchTableChanges} from '../../tables/TableUtil.js';
import {TABLE_FILTER, TABLE_HIGHLIGHT, TABLE_SORT} from '../../tables/TablesCntlr.js';

import './TabPanel.css';
import {dispatchChartUpdate} from '../../charts/ChartsCntlr.js';


export function uniqueTabId() {
    return `TapPanel-${uniqueId()}`;
}

/**
 * There are 4 implementations of TabPanel:  Tabs, TabsView, StatefulTabs, and FieldGroupTabs
 * See each component description below for more details.
 */
const TabsHeaderInternal = React.memo((props) => {
    const {tabId, children, resizable, headerStyle={}, size, label, onSelect, showOpenTabs} = props;

    const childrenAry = React.Children.toArray(children);
    const {width:widthPx} = size;
    const numTabs = children.length;
    let maxTitleWidth = undefined;
    let sizedChildren = children;
    if (widthPx && resizable) {
        // 2*5px - border, for each tab: 2x1px - border, 2x6px - padding, 6px - left margin
        const availableWidth = widthPx - 25 - 20 * numTabs;
        maxTitleWidth = Math.min(200, Math.trunc(availableWidth / numTabs));
        if (maxTitleWidth < 0) { maxTitleWidth = 1; }
        sizedChildren = childrenAry.map((child) => {
            return React.cloneElement(child, {maxTitleWidth});
        });
    }
    const style = {display: 'flex', flexShrink: 0, height: 20, ...headerStyle};
    const layoutLabel= isString(label) ? <div style={{padding: '0 10px 0 5px'}}>{label}</div> : label;

    const arrowEl = useRef(null);
    const showTabs = (ev) => handleOpenTabs({ev, doOpen: !isDropDownShowing(tabId), tabId, onSelect, arrowEl, childrenAry});

    const titles = getTabTitles(childrenAry).join();
    useEffect( ()=> {
        if (isDropDownShowing(tabId)) handleOpenTabs({tabId, doOpen: false});
    }, [tabId, titles]);

    return (
        <div style={style}>
            {label && layoutLabel}
            <div className='TabPanel__Header' style={{marginRight: 5}}>
                {(widthPx||!resizable) ? <ul className='TabPanel__Tabs'>
                    {sizedChildren}
                </ul> : <div/>}
                {showOpenTabs && (
                    <div style={{width: 20, height: 20, marginLeft: 3}}>
                        <div className='round-btn' onClick={showTabs} ref={arrowEl} title='Search open tabs'>
                            <div className='arrow-down' style={{borderWidth: '7px 7px 0 7px'}}/>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
});

TabsHeaderInternal.propTypes= {
    resizable: PropTypes.bool,
    headerStyle: PropTypes.object,
    label: PropTypes.node,
    size: PropTypes.object.isRequired,
    tabId: PropTypes.string,
    onSelect: PropTypes.func,
    showOpenTabs: PropTypes.bool
};


/**
 * Wrapper supporting resize
 */
const TabsHeader= sizeMe({refreshRate: 16})(TabsHeaderInternal);


/*----------------------------- exported components ----------------------------------*/

export const Tab = React.memo( (props) => {
    const {name, label, selected, onSelect, removable, onTabRemove, id, maxTitleWidth, style={}} = props;

    let tabClassName = 'TabPanel__Tab' ;
    if (selected) {
        tabClassName += ' TabPanel__Tab--selected';
    }
    const tabTitle = label || name;
    // removable width: 14px
    const textStyle = maxTitleWidth ? {float: 'left', width: maxTitleWidth-(removable?14:0)} : {};

    return (
        <li className={tabClassName} onClick={() => !selected && onSelect(id,name)}>
            <div style={{height: '100%', ...style}}>
                <div style={{...textStyle, height: '100%'}} className='text-ellipsis' title={name}>
                    {tabTitle}
                </div>
                {removable &&
                <div style={{right: -4, top: -2}} className='btn-close'
                     title='Remove Tab'
                     onClick={(e) => {
                         onTabRemove && onTabRemove(name);
                         e.stopPropagation && e.stopPropagation();
                     }}/>
                }
            </div>
        </li>);
});


Tab.propTypes= {
    name: PropTypes.string.isRequired, //public
    label: PropTypes.node,      // used for tab label.  if not given, name will be used as text.
    id: PropTypes.string,
    selected:  PropTypes.bool.isRequired, // private - true is the tab is currently selected
    onSelect: PropTypes.func, // private - called whenever the tab is clicked
    removable: PropTypes.bool,
    onTabRemove: PropTypes.func,
    maxTitleWidth: PropTypes.number,
    style: PropTypes.object,
};

Tab.defaultProps= { selected: false };


/*----------------------------------------------------------------------------------------------*/
/**
 * A strictly presentational(dumb) component.  No state is used.
 * The selected Tab is determine by defaultSelected which can be an index or the 'id' of its Tabs.
 */
export const TabsView = React.memo((props) => {

    const {children, onTabSelect, defaultSelected, useFlex, resizable, borderless, tabId=uniqueTabId(),
        style={}, headerStyle, contentStyle={}, label, showOpenTabs} = props;

    const onSelect = useCallback( (index,id,name) => {
        onTabSelect && onTabSelect(index,id,name);
    }, []);

    const childrenAry = React.Children.toArray(children);         // this returns only valid children excluding undefined and false values.
    const selectedIdx = Number.isInteger(defaultSelected) ? defaultSelected : childrenAry.findIndex((c) => c?.props?.id === defaultSelected);    // convert defaultSelected to idx if it's an ID

    const headers = childrenAry.map((child, index) => {
        return React.cloneElement(child, {
            selected: (index === selectedIdx),
            onSelect: onSelect.bind(this, index),
            key: 'tab-' + (index)
        });
    });

    let  content = childrenAry.filter( (c, idx) => idx === selectedIdx).map((c) => React.Children.only(c.props.children));
    if (content) {
        content = useFlex ? content : <div style={{display: 'block', position: 'absolute', top:0, bottom:0, left:0, right:0}}>{content}</div>;
        content = borderless ? content : <div className='TabPanel__Content--inside'>{content}</div>;
    }

    const contentClsName = borderless ? 'TabPanel__Content borderless' : 'TabPanel__Content';
    const mainClsName = showOpenTabs ? 'TabPanel__main boxed' : 'TabPanel__main';

    return (
        <div className={mainClsName} style={style}>
            <TabsHeader {...{resizable, headerStyle, label, tabId, onSelect, showOpenTabs}}>{headers}</TabsHeader>
            <div style={contentStyle} className={contentClsName}>
                {(content) ? content : ''}
            </div>
        </div>

    );
});


TabsView.propTypes = {
    defaultSelected:  PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    onTabSelect: PropTypes.func,
    useFlex: PropTypes.bool,
    resizable: PropTypes.bool,
    style: PropTypes.object,
    headerStyle: PropTypes.object,
    contentStyle: PropTypes.object,
    borderless: PropTypes.bool,
    label: PropTypes.node,
    tabId: PropTypes.string
};

TabsView.defaultProps= {
    defaultSelected: 0,
    useFlex: false,
    resizable: false,
    borderless: false
};


/*----------------------------------------------------------------------------------------------*/
/**
 * TabPanel with internal state
 * State will be lost after unload.
 */
export const Tabs = React.memo( (props) => {
    const {defaultSelected=0, onTabSelect} = props;

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

    return (<TabsView {...props} defaultSelected={selectedIdx} onTabSelect={onSelect} />);
});

Tabs.propTypes = TabsView.propTypes;
Tabs.defaultProps = TabsView.defaultProps;


/*----------------------------------------------------------------------------------------------*/
/**
 * TabPanel with ComponentCntlr supported state
 * Selected state is stored as <componentKey>.selectedIdx
 */
export const StatefulTabs = React.memo( (props) => {
    const {children=[], defaultSelected=0, onTabSelect, componentKey} = props;

    let selectedIdx = useStoreConnector( () => getComponentState(componentKey)?.selectedIdx ?? defaultSelected);

    const onSelect = useCallback( (index,id,name) => {
        dispatchComponentStateChange(componentKey, {selectedIdx: index});
        onTabSelect && onTabSelect(index,id,name);
    }, []);

    useEffect( ()=> {
        if (selectedIdx >= children.length) {
            // selectedIdx is greater than the number of tabs.. update store's state
            selectedIdx = children.length-1;
            dispatchComponentStateChange(componentKey, {selectedIdx});
        }
    });

    return (<TabsView {...props} onTabSelect={onSelect} defaultSelected={selectedIdx} />);

});

StatefulTabs.propTypes = {
    componentKey: PropTypes.string,
    ...TabsView.propTypes
};
StatefulTabs.defaultProps = TabsView.defaultProps;


/*----------------------------------------------------------------------------------------------*/

function onChange(idx,id, name, viewProps, fireValueChange) {
    let value= id||name;
    if (!value) value= idx;

    fireValueChange({ value});
    if (viewProps.onTabSelect) {
        viewProps.onTabSelect(idx, id, name);
    }
}

/**
 * TabPanel with FieldGroup supported state
 * The selected index is saved as the value of the field named by fieldKey
 */
export const FieldGroupTabs = memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    const newProps= {
        ...viewProps,
        defaultSelected : viewProps.value,
        useFlex: true,
        onTabSelect: (idx,id,name) => onChange(idx,id,name,viewProps, fireValueChange)
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
FieldGroupTabs.defaultProps = omit(Tabs.defaultProps, 'defaultSelected');


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
            <div style={{width, height: 200, position: 'relative'}}>
                <TablePanel tbl_ui_id={tabId+'-ui'} tableModel={tableModel} border={false} showTypes={false}
                            showToolbar={false} showFilters={true} selectable={false} showOptionButton={false}/>
            </div>);
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