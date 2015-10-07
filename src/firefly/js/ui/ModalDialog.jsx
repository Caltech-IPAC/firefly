/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react/addons';


var ModalDialog = React.createClass({
    propTypes: {
        title: React.PropTypes.string,
        showOverlay: React.PropTypes.bool,
        beforeOpen: React.PropTypes.func,
        afterOpen: React.PropTypes.func,
        beforeClose: React.PropTypes.func,
        afterClose: React.PropTypes.func
    },
    getDefaultProps: function () {
        return {
            title: '',
            showOverlay: true
        };
    },
    getInitialState: function () {
        return {
            isVisible: true
        };
    },
    show: function () {
        this.setState({isVisible: true});
    },
    hide: function () {
        this.setState({isVisible: false});
    },
    componentWillUpdate: function (nextProps, nextState) {
        if (nextState.isVisible && this.props.beforeOpen) {
            this.props.beforeOpen();
        }

        if (!nextState.isVisible && this.props.beforeClose) {
            this.props.beforeClose();
        }
    },
    componentDidUpdate: function (prevProps, prevState) {
        if (!prevState.isVisible && this.props.afterOpen) {
            this.props.afterOpen();
        }

        if (prevState.isVisible && this.props.afterClose) {
            this.props.afterClose();
        }
    },
    render: function () {

        var overlay;
        var displayStyle = this.state.isVisible ? {display: 'block'} : {display: 'none'};

        if (this.props.showOverlay) {
            overlay = (<div className="modal-dialog__overlay" style={displayStyle}></div>);
        }

        return (
            <section className="modal-wrapper">
                {overlay}
                <div className="modal-dialog" style={displayStyle}>
                    <a role="button" className="modal-dialog--close" onClick={this.hide}>&times;</a>
                    <h2>{this.props.title}</h2>
                    {this.props.children}
                </div>
            </section>
        );
    }
});

export default ModalDialog;

