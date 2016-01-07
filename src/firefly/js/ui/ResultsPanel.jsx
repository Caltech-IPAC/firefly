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
                <div style={{width: '100%'}} >
                    <div style={{float: 'left', width: '50%', height: '60%'}}>{children[0]}</div>
                    <div style={{float: 'left', width: '50%', height: '60%'}}>{children[1]}</div>
                </div>
                <div>
                    {children[2]}
                </div>
            </div>
        );
    }
});

export default ResultsPanel;


