"use strict";

var React= require('react/addons');

var InputFieldLabel= React.createClass(
    {

        mixins : [React.addons.PureRenderMixin],

        propTypes: {
            label : React.PropTypes.string.isRequired,
            tooltip : React.PropTypes.string.isRequired,
            labelStyle : React.PropTypes.object,
            labelWidth : React.PropTypes.number
        },
        getDefaultProps: function () {
            return {
                labelStyle : {
                    display:'inline-block',
                    paddingRight:'4px'
                },
                labelWidth : 200
            };
        },

        render() {
            var currStyle = this.props.labelStyle;
            currStyle.width = this.props.labelWidth;
            return (
                <div style={currStyle} title={this.props.tooltip}>
                        {this.props.label}
                </div>
            );
        }
    });

export default InputFieldLabel;

