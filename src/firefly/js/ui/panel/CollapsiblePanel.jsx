/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import './CollapsiblePanel.css';
import React from 'react';



export default React.createClass({

    getInitialState() {
        return {
            isOpen: false
        };
    },

    handleClick() {
        this.setState({
            isOpen: !this.state.isOpen
        });
    },

    getContentHeight() {
        return this.state.isOpen ? '100%' : '0';
    },


    render () {
        var headerClassName = 'CollapsiblePanel__Header';
        if (this.state.isOpen) {
            headerClassName += ' CollapsiblePanel__Header--is-open';
        }
        return (
            <div {...this.props} className='CollapsiblePanel'>
                <div onClick={this.handleClick} className={headerClassName}>
                    {this.props.header}
                </div>
                <div className='CollapsiblePanel__Content'>
                    <div style={{height: this.getContentHeight()}}>
                        {this.props.children}
                    </div>
                </div>
            </div>
    );
    }
    });
