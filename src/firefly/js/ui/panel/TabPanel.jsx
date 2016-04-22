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

export class Tabs extends Component {


    constructor(props) {
        super(props);
        this.state= tabsStateFromProps(props);
        this.onResize = debounce((size) => {
            if (size) {
                this.setState({widthPx: size.width});
            }
        }, 100);
    }

    componentWillReceiveProps(nextProps) {
        this.state= Object.assign({widthPx:this.state.widthPx}, tabsStateFromProps(nextProps));
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
        var { selectedIdx, widthPx}= this.state;
        const numTabs = this.props.children.length;
        const maxTabWidth = widthPx && numTabs && Math.trunc(widthPx/numTabs-20);

        var content;
        selectedIdx = Math.min(selectedIdx, numTabs-1);
        var children = React.Children.map(this.props.children, (child, index) => {
                if (index === selectedIdx) {
                    content = React.Children.only(child.props.children);
                }

                return React.cloneElement(child, {
                    maxTabWidth,
                    selected: (index == selectedIdx),
                    onSelect: this.onSelect.bind(this, index),
                    ref: 'tab-' + (index++)
                });
            });
        return (
            <div style={{display: 'flex', flexDirection: 'column', flexGrow: 1, overflow: 'hidden'}}>
                <div style={{flexGrow: 0, height: 18}}>
                    <Resizable id='tabs-resizer' style={{position: 'relative', width: '100%', height: '100%', overflow: 'hidden'}} onResize={this.onResize}>
                        <ul className='TabPanel__Tabs'>
                            {children}
                        </ul>
                    </Resizable>
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
        if (!this.props.maxTabWidth) {return null;} // will render when width is available

        const {name, maxTabWidth, selected, onSelect, removable, onTabRemove, id} = this.props;

        var tabClassName = 'TabPanel__Tab' ;
        if (selected) {
            tabClassName += ' TabPanel__Tab--selected';
        }

        const nameDivProps = {
            className: 'ellipsis',
            title: name,
            onClick: () => onSelect(id,name)
        };
        // 2*6px - padding, 2*1px - border, 12px - btn
        if (maxTabWidth) { nameDivProps.style = {maxWidth: (maxTabWidth-25)+'px'}; }

        return (
            <li className={tabClassName}>
                <div {...nameDivProps}>
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
    maxTabWidth: PropTypes.number, // private - set in parent
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
