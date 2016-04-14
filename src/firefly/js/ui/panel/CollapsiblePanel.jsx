/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import './CollapsiblePanel.css';
import React from 'react';

export const CollapseBorder = {
                            Noborder: 0,
                            Oneborder: 1,    // left side
                            Twoborder: 2,    // left and right sides
                            Threeborder: 3   // left, bottom and right sides
                        };

export const CollapseHeaderCorner = {
                            TopLeft: 0b0001,
                            TopRight: 0b0010,
                            BottomRight: 0b0100,
                            BottomLeft: 0b1000
                        };

export default React.createClass({

    propTypes: {
        header: React.PropTypes.string,
        isOpen: React.PropTypes.bool,
        headerRoundCorner: React.PropTypes.number,
        headerStyle: React.PropTypes.object,
        contentStyle: React.PropTypes.object,
        wrapperStyle: React.PropTypes.object,
        borderStyle: React.PropTypes.number
    },

    getDefaultProps() {
        return {
            headerRounderCorner: 0,
            borderStyle: CollapseBorder.Oneborder,
            isOpen: false
        };
    },

    getInitialState() {
        return {
            isOpen: this.props.isOpen? this.props.isOpen: false
        };
    },

    handleClick() {
        this.setState({
            isOpen: !this.state.isOpen
        });
    },

    getContentHeight() {
        return this.state.isOpen ? {display: 'block'} : {display: 'none'};
    },


    render () {
        const contentBorderClassName = ['', ' CollapsiblePanel__Content-oneborder',
                              ' CollapsiblePanel__Content-twoborder', ' CollapsiblePanel__Content-threeborder'];
        var headerClassName = 'CollapsiblePanel__Header';
        var contentClassName = 'CollapsiblePanel__Content';
        var headerCorner = '';
        var headerStyle, contentStyle;

       ['TopLeft', 'TopRight', 'BottomRight', 'BottomLeft'].forEach((corner) => {
            CollapseHeaderCorner[corner]&this.props.headerRoundCorner ?
                                            headerCorner += ' 0.5em' : headerCorner += ' 0em';
       });

        headerStyle = Object.assign({}, this.props.headerStyle, {'borderRadius': headerCorner});

        if (this.state.isOpen) {
            headerClassName += ' CollapsiblePanel__Header--is-open';
            contentClassName += contentBorderClassName[this.props.borderStyle];
        }

        contentStyle = Object.assign({}, this.props.contentStyle, this.getContentHeight());

        return (
            <div style={this.props.wrapperStyle}>
                <div style={headerStyle} onClick={this.handleClick} className={headerClassName}>
                    {this.props.header}
                </div>
                <div style={contentStyle} className={contentClassName}>
                    {this.props.children}
                </div>
            </div>
        );
    }
});
