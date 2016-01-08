import React from 'react';
import sCompare from 'react-addons-shallow-compare';
import ReactDOM from 'react-dom';
import PointerPopup from '../ui/PointerPopup.jsx';
import InputFieldLabel from './InputFieldLabel.jsx';
import './InputFieldView.css';
import EXCLAMATION from 'html/images/exclamation16x16.gif';




function computeStyle(valid,hasFocus) {
    if (!valid) {
        return 'ff-inputfield-view-error';
    }
    else {
        return hasFocus ? 'ff-inputfield-view-focus' : 'ff-inputfield-view-valid';
    }
}


function makeMessage(message) {
    return (
        <div>
            <img src={EXCLAMATION} style={{display:'inline-block', paddingRight:5}}/>
            <div style={{display:'inline-block'}}> {message} </div>
        </div>
    );
}

const makeInfoPopup = (mess,x,y) => <PointerPopup x={x} y={y} message={makeMessage(mess)}/>;




class InputFieldView extends React.Component {
    constructor(props) {
        super(props);
        this.warnIcon= null;
        this.state= {
            hasFocus : false,
            infoPopup : false,
            onChange : null,
            warningOffsetX : 0,
            warningOffsetY : 0
        };
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    makeWarningArea(warn) {
        var warnIcon= '';
        if (warn) {
            warnIcon= (
                <div onMouseOver= {() => this.setState({infoPopup:true})}
                     onMouseLeave={() => this.setState({infoPopup:false})}>
                    <img src={EXCLAMATION}
                         ref={(c) => {
                                      this.computeWarningXY(c);
                                      this.warnIcon= c;
                                      }
                           } />
                </div>
            );
        }

        return (
            <div style={ { paddingLeft: '3px',
                           width: '16px',
                           height: '16px',
                           display:'inline-block'} }>
                {warnIcon}
            </div>
        );
    }



    componentDidUpdate() {
        this.computeWarningXY(this.warnIcon);
    }

    computeWarningXY(warnIcon) {
        if (warnIcon) {
            var e= ReactDOM.findDOMNode(warnIcon);
            var bodyRect = document.body.getBoundingClientRect();
            var elemRect = e.getBoundingClientRect();
            var warningOffsetX = (elemRect.left - bodyRect.left) + e.offsetWidth/2;
            var warningOffsetY = elemRect.top - bodyRect.top;
            if (warningOffsetX!==this.state.warningOffsetX && warningOffsetY!=this.state.warningOffsetY) {
                this.setState({warningOffsetX, warningOffsetY} );
            }
        }
    }


    render() {
        var {infoPopup, warningOffsetX, warningOffsetY,hasFocus}= this.state;
        var {visible,label,tooltip,labelWidth,value,style,valid,message,onChange}= this.props;
        if (visible) {
            return (
                <div style={{whiteSpace:'nowrap', display: this.props.inline?'inline-block':'block'} }>
                    <InputFieldLabel label={label} tooltip={tooltip} labelWidth={labelWidth} />
                    <input style={Object.assign({display:'inline-block'}, style)}
                           className={ () => computeStyle(valid,hasFocus)}
                           onChange={(ev) => onChange ? onChange(ev) : null}
                           onFocus={ () => !hasFocus ? this.setState({hasFocus:true, infoPopup:false}) : ''}
                           onBlur={ () => this.setState({hasFocus:false, infoPopup:false})}
                           value={value}
                           title={tooltip}
                    />
                    {this.makeWarningArea(!valid)}
                    {infoPopup?makeInfoPopup(message,warningOffsetX,warningOffsetY) : ''}
                </div>
            );
        }

        return null;
    }



}

InputFieldView.propTypes= {
    valid   : React.PropTypes.bool,
    visible : React.PropTypes.bool,
    message : React.PropTypes.string,
    tooltip : React.PropTypes.string,
    label : React.PropTypes.string,
    inline : React.PropTypes.bool,
    labelWidth: React.PropTypes.number,
    style: React.PropTypes.object,
    value   : React.PropTypes.string.isRequired,
    onChange : React.PropTypes.func.isRequired
};

InputFieldView.defaultProps= {
    valid : true,
    visible : true,
    message: ''
};

export default InputFieldView;

