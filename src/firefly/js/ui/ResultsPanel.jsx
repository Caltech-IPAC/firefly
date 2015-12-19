/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
//import ReactGridLayout from 'react-grid-layout';

/**
 * This layout demonstrates how to use static grid elements.
 * Static elements are not draggable or resizable, and cannot be moved.
 */
var ResultsPanel = React.createClass({
    mixins: [PureRenderMixin],

    getInitialState() {
        return {
            layout: []
        };
    },

    onLayoutChange(layout) {
        this.setState({layout: layout});
    },


    render() {
        var {children, title} = this.props;
        return (
            <div style={{height: '100%', textAlign: 'center'}}>
                <h2>{title}</h2>
                <div style={{display:'inline-flex', width: '100%'}} >
                    <div style={{width: '50%'}}>{children[0]}</div>
                    <div style={{width: '50%'}}>{children[1]}</div>
                </div>
            </div>
        );
    }
});

export default ResultsPanel;


