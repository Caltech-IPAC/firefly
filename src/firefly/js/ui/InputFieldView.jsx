import PointerPopup from '../ui/PointerPopup.jsx';
import InputFieldLabel from './InputFieldLabel.jsx';
import React from 'react/addons';

const EXCLAMATION = 'tmp-stuff/exclamation16x16.gif';

var InputFieldView = React.createClass(
{

       mixins : [React.addons.PureRenderMixin],

       propTypes: {
           valid   : React.PropTypes.bool,
           visible : React.PropTypes.bool,
           message : React.PropTypes.string,
           tooltip : React.PropTypes.string,
           label : React.PropTypes.string,
           inline : React.PropTypes.bool,
           value   : React.PropTypes.string.isRequired,
           onChange : React.PropTypes.func.isRequired
       },

       warnIcon : null,

       getDefaultProps() {
           return {
               valid : true,
               visible : true,
               message: ''

           };
       },

       getInitialState() {
           return {
               hasFocus : false,
               infoPopup : false,
               onChange : null,
               warningOffsetX : 0,
               warningOffsetY : 0
           };
       },

       onChange(ev) {
           if (this.props.onChange) {
               this.props.onChange(ev);
           }
       },

       alertEntry(ev) {

           this.setState({infoPopup:true});
       },
       alertLeave(ev) {
          this.setState({infoPopup:false});
       },

       onFocus(ev) {
           if (!this.state.hasFocus) {
               this.setState({hasFocus:true, infoPopup:false});
           }
       },


       onBlur(ev) {
           this.setState({hasFocus:false, infoPopup:false});
       },

       makeWarningArea(warn) {
           var warnIcon= '';
           if (warn) {
               //this.computeWarningXY(this.warnIcon);
               warnIcon= (
                       <div onMouseOver={this.alertEntry} onMouseLeave={this.alertLeave}>
                           <img ref={(c) => {
                                             this.computeWarningXY(c);
                                             this.warnIcon= c;
                                            }
                           }
                                src={EXCLAMATION}/>
                       </div>
               );
           }

           //<img ref={(c)=>this.computeWarningXY(c);} src={EXCLAMATION}/>
           return (
                   <div style={
                      {
                       paddingLeft: '3px',
                       width: '16px',
                       height: '16px',
                       display:'inline-block'}
                       }>
                        {warnIcon}
                   </div>
               );
       },

       computeStyle() {
           if (!this.props.valid) {
               return 'firefly-inputfield-error';
           }
           else {
               return this.state.hasFocus ? 'firefly-inputfield-focus' : 'firefly-inputfield-valid';
           }
       },

       makeMessage() {
           return (
                   <div>
                       <img src={EXCLAMATION}
                               style={{display:'inline-block',
                                       paddingRight:5}}/>
                       <div style={{display:'inline-block'}}> {this.props.message} </div>
                   </div>
           );
       },


    componentDidMount() {
        //if (!this.props.valid) {
        //    this.computeWarningXY();
        //}

    },

    componentDidUpdate() {
        this.computeWarningXY(this.warnIcon);
    },

    computeWarningXY(warnIcon) {
        if (warnIcon) {
            var e= React.findDOMNode(warnIcon);
            var bodyRect = document.body.getBoundingClientRect();
            var elemRect = e.getBoundingClientRect();
            var warningOffsetX = (elemRect.left - bodyRect.left) + e.offsetWidth/2;
            var warningOffsetY = elemRect.top - bodyRect.top;
            this.setState({warningOffsetX, warningOffsetY} );
        }
    },

    makeInfoPopup() {

        return (
                <PointerPopup x={this.state.warningOffsetX} y={this.state.warningOffsetY}
                message={this.makeMessage()}/>
        );
    },

    render() {
        var retval= null;
        if (this.props.visible) {
            retval= (
                <div style={{whiteSpace:'nowrap', display: this.props.inline?'inline-block':'block'} }>
                    <InputFieldLabel label={this.props.label}
                        tooltip={this.props.tooltip}
                        labelWidth={this.props.labelWidth}
                    />
                    <input style={{display:'inline-block'}}
                        className={this.computeStyle()}
                        onChange={this.onChange}
                        onFocus={this.onFocus}
                        onBlur={this.onBlur}
                        value={this.props.value}
                        title={this.props.tooltip}
                    />
                       {this.makeWarningArea(!this.props.valid)}
                       {this.state.infoPopup?this.makeInfoPopup() : ''}
                </div>
            );
        }

        return retval;
    }


});

export default InputFieldView;

