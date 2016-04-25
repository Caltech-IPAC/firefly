/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import './TabPanel.css';
import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
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


function adjustTabsWidth(c) {
    const el = ReactDOM.findDOMNode(c); //DOMElement
    // el is <ul>, it's children are <li> for tabs, gradchildren <div> for name and <div> for close button
    if (el && el.childNodes && el.childNodes.length>0) {
        var i;
        const numTabs = el.childNodes.length;

        // 2*5px - border, 6px - left margin
        const availableWidth = el.offsetWidth-10-6*numTabs;

        let totalNameLengthPx = 0;
        for (i=0; i<el.childNodes.length; i++) {
            // name div is el.childNodes[i].childNodes[0] or el.childNodes[i].querySelector('.ellipsis')
            totalNameLengthPx += el.childNodes[i].childNodes[0].scrollWidth;
        }
        for (i=0; i<el.childNodes.length; i++) {
            const nameDivEl = el.childNodes[i].childNodes[0];
            const nameLengthPx = nameDivEl.scrollWidth;
            const maxTabWidth = availableWidth*nameLengthPx/totalNameLengthPx;
            const maxNameWidth = maxTabWidth - (el.childNodes[i].offsetWidth-nameDivEl.offsetWidth);
            // set the width of the tab
            el.childNodes[i].style.maxWidth = Math.trunc(maxTabWidth)+'px';
            // set the width of the name div
            nameDivEl.style.maxWidth = Math.trunc(maxNameWidth)+'px';
        }
    }
}

class TabsHeader extends Component {
    constructor(props) {
        super(props);
        this.state = {};
        this.onResize = debounce((size) => {
            if (size && size.width !== this.state.widthPx) {
                this.setState({widthPx: size.width});
            }
        }, 100);

        this.adjustTabsWidth = ((c) => {
            // update only when size is available
            this.state.widthPx && adjustTabsWidth(c);
        }).bind(this);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    render() {
        return (
            <div style={{flexGrow: 0, height: 18}}>
                <Resizable id='tabs-resizer' style={{position: 'relative', width: '100%', height: '100%', overflow: 'hidden'}} onResize={this.onResize}>
                    <ul className='TabPanel__Tabs'
                        ref={(c)=>{this.adjustTabsWidth(c);}}>
                        {this.props.children}
                    </ul>
                </Resizable>
            </div>
        );
    }
}

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

    onSelect(index,id,name) {
        const {onTabSelect, componentKey} = this.props;
        if (this.state.selectedIdx !== index) {
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
        const origChildren = this.props.children;
        const numTabs = React.Children.count(origChildren);

        var content;
        selectedIdx = Math.min(selectedIdx, numTabs-1);
        var children = React.Children.map(origChildren, (child, index) => {
                if (index === selectedIdx) {
                    content = React.Children.only(child.props.children);
                }

                return React.cloneElement(child, {
                    selected: (index == selectedIdx),
                    onSelect: this.onSelect.bind(this, index),
                    ref: 'tab-' + (index)
                });
            });
        return (
            <div style={{display: 'flex', flexDirection: 'column', flexGrow: 1, overflow: 'hidden'}}>
                <TabsHeader>{children}</TabsHeader>
                <div ref='contentRef' className='TabPanel__Content'>{(content)?content:''}</div>
            </div>
        );
    }
}


Tabs.propTypes= {
    componentKey: PropTypes.string, // if need to preserve state and is not part of the field group
    defaultSelected:  PropTypes.any,
    onTabSelect: PropTypes.func
};

Tabs.defaultProps= {
    defaultSelected: 0
};


export class Tab extends Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }


    componentWillMount() {
        const {selected, onSelect} = this.props;
        if (selected) {
            onSelect();
        }
    }

    render () {
        const {name, selected, onSelect, removable, onTabRemove, id} = this.props;

        var tabClassName = 'TabPanel__Tab' ;
        if (selected) {
            tabClassName += ' TabPanel__Tab--selected';
        }

        return (
            <li className={tabClassName}>
                <div className='text-ellipsis' title={name} onClick={() => onSelect(id,name)}>
                     {name}
                </div>
                {removable &&
                        <div style={{right: -5, top: -2}} className='btn-close'
                             title='Remove Tab'
                             onClick={() => onTabRemove && onTabRemove(name)}/>
                }
            </li>
        );

    }
}


Tab.propTypes= {
    name: PropTypes.string.isRequired, //public
    id: PropTypes.string,
    selected:  PropTypes.bool.isRequired, // private - true is the tab is currently selected
    onSelect: PropTypes.func, // private - called whenever the tab is clicked
    removable: PropTypes.bool,
    onTabRemove: PropTypes.func
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
            defaultSelected:params.value
        });
}

export const FieldGroupTabs= fieldGroupConnector(Tabs,getProps);
