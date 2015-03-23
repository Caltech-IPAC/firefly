"use strict";

var React= require('react/addons');

var InputFieldLabel= React.createClass(
    {

        mixins : [React.addons.PureRenderMixin],

        propTypes: {
            label : React.PropTypes.string.isRequired,
            tooltip : React.PropTypes.string.isRequired,
            style : React.PropTypes.object.optional,
            width : React.PropTypes.number.optional
        },
        getDefaultProps: function () {
            return {
                style : {
                    display:'inline-block',
                    paddingRight:'4px'
                },
                width : 200
            };
        },

        render() {
            var currStyle = this.props.style;
            currStyle.width = this.props.width;
            return (
                <div style={this.props.style} title={this.props.tooltip}>
                        {this.props.label}
                </div>
            );
        }
    });

export default InputFieldLabel;

