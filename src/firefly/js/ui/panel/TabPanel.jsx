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

    onSelect(index, content) {
        const {onTabSelect} = this.props;
        this.setState({
            selectedIdx: index,
            content
        });
        if (onTabSelect) {
            onTabSelect(index);
        }
    },

    render () {
        const { selectedIdx, content }= this.state;
        var index = 0,
            children = React.Children.map(this.props.children, (child) => {
                return React.cloneElement(child, {
                    selected: (index == selectedIdx),
                    onSelect: this.onSelect.bind(this, index),
                    ref: 'tab-' + (index++)
                });
            });
        return (
            <div>
                <div>
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
        onSelect: React.PropTypes.func // private - called whenever the tab is clicked
    },

    getDefaultProps() {
        return {
            selected: false
        };
    },

    componentWillMount() {
        const {selected, onSelect, children} = this.props;
        if (selected) {
            onSelect(React.Children.only(children));
        }
    },

    render () {
        const {name, selected, onSelect, children} = this.props;
        var content = React.Children.only(children);
        var tabClassName = 'TabPanel__Tab';
        if (selected) {
            tabClassName += ' TabPanel__Tab--selected';
        }
        return (
            <li className={tabClassName} onClick={onSelect.bind(null,content)}>{name}</li>
        );

    }
});
