/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import './TabPanel.css';
import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';


export var Tabs = React.createClass({

    mixins : [PureRenderMixin],

    propTypes: {
        defaultSelected:  React.PropTypes.number,
        onTabSelect: React.PropTypes.func
    },

    getDefaultProps() {
        return {
            defaultSelected: 0
        };
    },

    getInitialState() {
        const {defaultSelected} = this.props;
        return {
            selectedIdx : defaultSelected ? defaultSelected : 0
        };
    },

    onSelect(index) {
        const {onTabSelect} = this.props;
        this.setState({
            selectedIdx: index
        });
        if (onTabSelect) {
            onTabSelect(index);
        }
    },

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
});

export var Tab = React.createClass({

    mixins : [PureRenderMixin],

    propTypes: {
        name: React.PropTypes.string.isRequired, //public
        selected:  React.PropTypes.bool.isRequired, // private - true is the tab is currently selected
        onSelect: React.PropTypes.func, // private - called whenever the tab is clicked
        removable: React.PropTypes.bool,
        onTabRemove: React.PropTypes.func
    },

    getDefaultProps() {
        return {
            selected: false
        };
    },

    componentWillMount() {
        const {selected, onSelect} = this.props;
        if (selected) {
            onSelect();
        }
    },

    render () {
        const {name, selected, onSelect, removable, onTabRemove} = this.props;
        var tabClassName = 'TabPanel__Tab';
        if (selected) {
            tabClassName += ' TabPanel__Tab--selected';
        }
        return (
            <li className={tabClassName}>
                <div style={{display: 'inline-block'}} onClick={onSelect} >{name}</div>
                {removable &&
                        <div style={{right: -5, top: -2}} className='btn-close'
                             title='Remove Tab'
                             onClick={() => onTabRemove && onTabRemove(name)}/>
                }
            </li>
        );

    }
});
