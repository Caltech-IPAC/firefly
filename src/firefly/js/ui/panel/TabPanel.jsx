/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import './TabPanel.css';
import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import sizeMe from 'react-sizeme';
import {fieldGroupConnector} from '../FieldGroupConnector.jsx';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';



function tabsStateFromProps(props) {
    const {defaultSelected, componentKey} = props;
    let selectedIdx;
    const childrenAry = React.Children.toArray(props.children);         // this returns only valid children excluding undefined and false values.

    if (componentKey) {
        // component key should be defined if the state needs to be saved though unmount/mount
        selectedIdx = getComponentState(componentKey).selectedIdx;

        if (selectedIdx >= childrenAry.length) {
            // selectedIdx is greater than the number of tabs.. update store's state
            selectedIdx = childrenAry.length-1;
            dispatchComponentStateChange(componentKey, {selectedIdx});
        }
    } else {
        selectedIdx = childrenAry.findIndex( (c) => c.props.id===defaultSelected );
    }

    selectedIdx = selectedIdx >= 0 ? selectedIdx : defaultSelected;
    return {selectedIdx};
}


class TabsHeaderInternal extends PureComponent {
    constructor(props) {
        super(props);
    }

    render() {
        const {width:widthPx} = this.props.size;
        const {children, resizable, headerStyle={}} = this.props;
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
        const style = Object.assign({flexGrow: 0, height: 20}, headerStyle);
        return (
            <div style={style}>
                {(widthPx||!resizable) ? <ul className='TabPanel__Tabs'>
                    {sizedChildren}
                </ul> : <div/>}
            </div>
        );
    }
}

TabsHeaderInternal.propTypes= {
    resizable: PropTypes.bool,
    headerStyle: PropTypes.object,
    size: PropTypes.object.isRequired
};


const TabsHeader= sizeMe({refreshRate: 16})(TabsHeaderInternal);


export class Tabs extends PureComponent {


    constructor(props) {
        super(props);
        this.state= tabsStateFromProps(props);
    }

    componentWillReceiveProps(nextProps) {
        this.setState( () => tabsStateFromProps(nextProps));
    }
    
    componentWillUnmount() {
        this.isUnmounted = true;
    }


    onSelect(index,id,name) {
        const {onTabSelect, componentKey} = this.props;
        if (this.state.selectedIdx !== index && !this.isUnmounted) {
            if (componentKey) {
                dispatchComponentStateChange(componentKey, {selectedIdx: index});
            }
            this.setState({
                selectedIdx: index
            });
        }
        if (onTabSelect) {
            onTabSelect(index,id,name);
        }
    }

    render () {
        const { selectedIdx}= this.state;
        const {children, useFlex, resizable, borderless, headerStyle, contentStyle={}} = this.props;

        let  content;
        const newChildren = React.Children.toArray(children).filter((el) => !!el).map((child, index) => {
            if (index === selectedIdx) {
                content = React.Children.only(child.props.children);
            }

            return React.cloneElement(child, {
                selected: (index === selectedIdx),
                onSelect: this.onSelect.bind(this, index),
                key: 'tab-' + (index)
            });
        });
        const contentDiv = useFlex ? content :
            (   <div style={{display: 'block', position: 'absolute', top:0, bottom:0, left:0, right:0}}>
                    {content}
                </div>
            );
        const contentClsName = borderless ? 'TabPanel__Content borderless' : 'TabPanel__Content';
        return (
            <div style={{display: 'flex', height: '100%', flexDirection: 'column', flexGrow: 1, overflow: 'hidden'}}>
                <TabsHeader {...{resizable, headerStyle}}>{newChildren}</TabsHeader>
                <div ref='contentRef' style={contentStyle} className={contentClsName}>
                    {(content)?contentDiv:''}
                </div>
            </div>

        );
    }
}


Tabs.propTypes= {
    componentKey: PropTypes.string, // if need to preserve state and is not part of the field group
    defaultSelected:  PropTypes.any,
    onTabSelect: PropTypes.func,
    useFlex: PropTypes.bool,
    resizable: PropTypes.bool,
    headerStyle: PropTypes.object,
    contentStyle: PropTypes.object,
    borderless: PropTypes.bool
};

Tabs.defaultProps= {
    defaultSelected: 0,
    useFlex: false,
    resizable: false,
    borderless: false
};


export class Tab extends PureComponent {
    constructor(props) {
        super(props);
    }

    componentWillMount() {
        const {selected, onSelect, id, name} = this.props;
        if (selected) {
            onSelect(id, name);
        }
    }

    render () {
        const {name, label, selected, onSelect, removable, onTabRemove, id, maxTitleWidth} = this.props;

        let tabClassName = 'TabPanel__Tab' ;
        if (selected) {
            tabClassName += ' TabPanel__Tab--selected';
        }
        const tabTitle = label || name;
        // removable width: 14px
        const textStyle = maxTitleWidth ? {float: 'left', width: maxTitleWidth-(removable?14:0)} : {};

        return (
            <li className={tabClassName} onClick={() => onSelect(id,name)}>
                <div style={{height: '100%'}}>
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
    }
}


Tab.propTypes= {
    name: PropTypes.string.isRequired, //public
    label: PropTypes.node,      // used for tab label.  if not given, name will be used as text.      
    id: PropTypes.string,
    selected:  PropTypes.bool.isRequired, // private - true is the tab is currently selected
    onSelect: PropTypes.func, // private - called whenever the tab is clicked
    removable: PropTypes.bool,
    onTabRemove: PropTypes.func,
    maxTitleWidth: PropTypes.number
};

Tab.defaultProps= { selected: false };



function onChange(idx,id, name, params, fireValueChange) {
    let value= id||name;
    if (!value) value= idx;

    fireValueChange({ value});
    if (params.onTabSelect) {
        params.onTabSelect(idx, id, name);
    }
}

function getProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            onTabSelect: (idx,id,name) => onChange(idx,id,name,params, fireValueChange),
            defaultSelected:params.value,
            useFlex: true
        });
}

export const FieldGroupTabs= fieldGroupConnector(Tabs,getProps);
