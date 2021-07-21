/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useEffect, useState, useCallback} from 'react';
import PropTypes from 'prop-types';
import sizeMe from 'react-sizeme';
import {omit, isString} from 'lodash';

import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {useFieldGroupConnector} from '../FieldGroupConnector.jsx';
import {useStoreConnector} from '../SimpleComponent.jsx';

import './TabPanel.css';


/**
 * There are 4 implementations of TabPanel:  Tabs, TabsView, StatefulTabs, and FieldGroupTabs
 * See each component description below for more details.
 */

const TabsHeaderInternal = React.memo((props) => {
    const {children, resizable, headerStyle={}, size, label} = props;

    const {width:widthPx} = size;
    const numTabs = children.length;
    let maxTitleWidth = undefined;
    let sizedChildren = children;
    if (widthPx && resizable) {
        // 2*5px - border, for each tab: 2x1px - border, 2x6px - padding, 6px - left margin
        const availableWidth = widthPx - 5 - 20 * numTabs;
        maxTitleWidth = Math.min(200, Math.trunc(availableWidth / numTabs));
        if (maxTitleWidth < 0) { maxTitleWidth = 1; }
        sizedChildren = React.Children.toArray(children).map((child) => {
            return React.cloneElement(child, {maxTitleWidth});
        });
    }
    const style = {display: 'flex', flexShrink: 0, height: 20, ...headerStyle};
    const layoutLabel= isString(label) ? <div style={{padding: '0 10px 0 5px'}}>{label}</div> : label;

    return (
        <div style={style}>
            {label && layoutLabel}
            {(widthPx||!resizable) ? <ul className='TabPanel__Tabs'>
                {sizedChildren}
            </ul> : <div/>}
        </div>
    );
});

TabsHeaderInternal.propTypes= {
    resizable: PropTypes.bool,
    headerStyle: PropTypes.object,
    label: PropTypes.node,
    size: PropTypes.object.isRequired
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

    const {children, onTabSelect, defaultSelected, useFlex, resizable, borderless,
        style={}, headerStyle, contentStyle={}, label} = props;

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

    return (
        <div style={{display: 'flex', flexDirection: 'column', overflow: 'hidden', ...style}}>
            <TabsHeader {...{resizable, headerStyle, label}}>{headers}</TabsHeader>
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
    label: PropTypes.node
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

    let [selectedIdx = defaultSelected] = useStoreConnector( () => {
        return getComponentState(componentKey)?.selectedIdx;
    });

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
