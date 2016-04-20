/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import './TabPanel.css';
import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {fieldGroupConnector} from '../FieldGroupConnector.jsx';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';


export class Tabs extends Component {


    constructor(props) {
        super(props);
        const {defaultSelected, componentKey} = props;
        var selectedIdx;
        // component key needs to be defined if the state needs to be saved though unmount/mount
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

        this.state= {selectedIdx};
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
        var content;
        selectedIdx = Math.min(selectedIdx, this.props.children.length-1);
        var children = React.Children.map(this.props.children, (child, index) => {
                if (index === selectedIdx) {
                    content = React.Children.only(child.props.children);
                }

                return React.cloneElement(child, {
                    selected: (index == selectedIdx),
                    onSelect: this.onSelect.bind(this, index),
                    ref: 'tab-' + (index++)
                });
            });
        return (
            <div style={{display: 'flex', flexDirection: 'column', flexGrow: 1, overflow: 'hidden'}}>
                <div style={{flexGrow: 0, height: 18}}>
                    <ul className='TabPanel__Tabs'>
                        {children}
                    </ul>
                </div>
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
        const {name, selected, onSelect, removable, onTabRemove,id} = this.props;
        var tabClassName = 'TabPanel__Tab' ;
        if (selected) {
            tabClassName += ' TabPanel__Tab--selected';
        }

        return (
            <li className={tabClassName}>
                <div style={{display: 'inline-block'}} onClick={() => onSelect(id,name)} >{name}</div>
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
