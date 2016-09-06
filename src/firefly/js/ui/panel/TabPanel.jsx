/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import './TabPanel.css';
import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import Resizable from 'react-component-resizable';
import {debounce} from 'lodash';
import {fieldGroupConnector} from '../FieldGroupConnector.jsx';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';

function tabsStateFromProps(props) {
    const {defaultSelected, componentKey} = props;
    var selectedIdx;
    // component key should be defined if the state needs to be saved though unmount/mount
    var savedIdx = componentKey && getComponentState(componentKey).selectedIdx;
    if (!isNaN(savedIdx)) {
        selectedIdx = savedIdx;
    } else if (!isNaN(defaultSelected)) {
        selectedIdx = defaultSelected;
    }
    else {
        const idx= React.Children.toArray(props.children).findIndex( (c) => c.props.id===defaultSelected );
        selectedIdx= idx>-1 ? idx : 0;
    }
    return {selectedIdx};
}


class TabsHeader extends Component {
    constructor(props) {
        super(props);
        this.state = {widthPx: 0};
        this.onResize = debounce((size) => {
            if (size && size.width !== this.state.widthPx && !this.isUnmounted) {
                this.setState({widthPx: size.width});
            }
        }, 100).bind(this);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    componentWillUnmount() {
        this.isUnmounted = true;
    }

    render() {
        const {widthPx} = this.state;
        const {children, resizable} = this.props;
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
        return (
            <div style={{flexGrow: 0, height: 18}}>
                {(widthPx||!resizable) ? <ul className='TabPanel__Tabs'>
                    {sizedChildren}
                </ul> : <div/>}
                {resizable && <Resizable id='tabs-resizer' style={{position: 'relative', top: '18px', width: '100%', height: '100%'}} onResize={this.onResize}/>}
            </div>
        );
    }
}

TabsHeader.propTypes= {
    resizable: PropTypes.bool
};

export class Tabs extends Component {


    constructor(props) {
        super(props);
        this.state= tabsStateFromProps(props);
    }

    componentWillReceiveProps(nextProps) {
        this.state= tabsStateFromProps(nextProps);
    }
    
    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
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
        var { selectedIdx}= this.state;
        const {children, useFlex, resizable} = this.props;
        const numTabs = React.Children.count(children);

        var content;
        selectedIdx = Math.min(selectedIdx, numTabs-1);
        var newChildren = React.Children.toArray(children).filter((el) => !!el).map((child, index) => {
            if (index === selectedIdx) {
                content = React.Children.only(child.props.children);
            }

            return React.cloneElement(child, {
                selected: (index == selectedIdx),
                onSelect: this.onSelect.bind(this, index),
                ref: 'tab-' + (index)
            });
        });
        const contentDiv = useFlex ? content :
            (   <div style={{display: 'block', position: 'absolute', top:0, bottom:0, left:0, right:0}}>
                    {content}
                </div>
            );

        return (
            <div style={{display: 'flex', height: '100%', flexDirection: 'column', flexGrow: 1, overflow: 'hidden'}}>
                <TabsHeader {...{resizable}}>{newChildren}</TabsHeader>
                <div ref='contentRef' className='TabPanel__Content'>
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
    resizable: PropTypes.bool
};

Tabs.defaultProps= {
    defaultSelected: 0,
    useFlex: false,
    resizable: false
};


export class Tab extends Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }


    componentWillMount() {
        const {selected, onSelect, id, name} = this.props;
        if (selected) {
            onSelect(id, name);
        }
    }

    render () {
        const {name, selected, onSelect, removable, onTabRemove, id, maxTitleWidth} = this.props;

        var tabClassName = 'TabPanel__Tab' ;
        if (selected) {
            tabClassName += ' TabPanel__Tab--selected';
        }

        // removable width: 14px
        const textStyle = maxTitleWidth ? {float: 'left', width: maxTitleWidth-(removable?14:0)} : {};

        return (
            <li className={tabClassName} onClick={() => onSelect(id,name)}>
                <div>
                    <div style={textStyle} className='text-ellipsis' title={name}>
                         {name}
                    </div>
                    {removable &&
                            <div style={{right: -4, top: -2}} className='btn-close'
                                 title='Remove Tab'
                                 onClick={() => onTabRemove && onTabRemove(name)}/>
                    }
                </div>
            </li>);
    }
}


Tab.propTypes= {
    name: PropTypes.string.isRequired, //public
    id: PropTypes.string,
    selected:  PropTypes.bool.isRequired, // private - true is the tab is currently selected
    onSelect: PropTypes.func, // private - called whenever the tab is clicked
    removable: PropTypes.bool,
    onTabRemove: PropTypes.func,
    maxTitleWidth: PropTypes.number
};

Tab.defaultProps= { selected: false };



function onChange(idx,id, name, params, fireValueChange) {
    var value= id||name;
    if (!value) value= idx;

    fireValueChange({ value});
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
