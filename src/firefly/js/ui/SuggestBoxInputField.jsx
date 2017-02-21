import React, {Component, PropTypes}  from 'react';
import ReactDOM from 'react-dom';
import {get, isArray, isUndefined, debounce} from 'lodash';

import {logError} from '../util/WebUtil.js';

import {InputFieldView} from './InputFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import './SuggestBoxInputField.css';


function getProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            fireValueChange
        });
}

/**
 *  Make sure a component (like highlighted suggestion) is visible
 *  @param {ReactComponent} c
 *  @param {Number} highlightedIdx
 */
function ensureVisible(c, highlightedIdx) {
    const el = ReactDOM.findDOMNode(c); //DOMElement
    if (el && highlightedIdx) {
        const nSuggestions = el.children.length;
        if (nSuggestions>1) {
            const highlightedTop = el.childNodes[highlightedIdx].offsetTop;
            const scrollTop = el.parentNode.scrollTop;
            if (highlightedTop<scrollTop) {
                el.parentNode.scrollTop = scrollTop-180;
                //console.log('highlightedTop: '+highlightedTop+' scrollTop: '+scrollTop+' '+el.parentNode.scrollTop );
            } else if (highlightedTop>scrollTop+180) {
                el.parentNode.scrollTop = scrollTop+180;
                //console.log('highlightedTop: '+highlightedTop+' scrollTop: '+scrollTop+' '+el.parentNode.scrollTop );
            }
        }
    }
}


const Suggestion = (props) => {
    const {suggestion, renderSuggestion, ...otherProps} = props;
    return ( <li {...otherProps}>{renderSuggestion(suggestion)}</li>);
};

Suggestion.propTypes = {
    suggestion : PropTypes.any,
    renderSuggestion: PropTypes.func
};

const SuggestBox = (props) => {
    var {suggestions, highlightedIdx, renderSuggestion, onChange, onComplete, mouseTrigger} = props;
    suggestions = suggestions || [];

    var mouseEnterIdx; // to keep track where the mouse is

    const handleOptionMouseEnter = (idx, event) => {
        if (mouseTrigger) {
            //console.log('handle mouse enter ' + idx + ' ' + event.target);
            mouseEnterIdx = undefined;
            onChange(idx);
        } else {
            mouseEnterIdx = idx;
            event.stopPropagation();
        }
    };

    const handleMouseMove = (event) => {
        if (!mouseTrigger) {
            mouseTrigger = true;
            event.stopPropagation();
        } else {
            if (!isUndefined(mouseEnterIdx)) {
                onChange(mouseEnterIdx);
            }
        }
    };

    const handleOptionClick = (idx, event) => {
        onComplete(idx);
    };

    return (

        <ul className={'SuggestBox'} ref={(c)=>{ensureVisible(c, highlightedIdx);}}
            onMouseDown={handleOptionClick.bind(this, highlightedIdx)} //onClick competes with onBlur
            onMouseMove={handleMouseMove.bind(this)}>
            {suggestions.map((suggestion, idx) => {
                const highlighted = idx === highlightedIdx;
                return (
                    <Suggestion
                        className={'SuggestBox__Suggestion'+(highlighted ? ' SuggestBox__Suggestion--highlighted' : '')}
                        renderSuggestion={renderSuggestion}
                        key={idx}
                        onMouseEnter={handleOptionMouseEnter.bind(this, idx)}
                        suggestion={suggestion}
                    />
                );
            })}
        </ul>
    );
};

class SuggestBoxInputFieldView extends Component {
    constructor(props) {
        super(props);
        this.state = {
            isOpen: false,
            displayValue: props.value,
            valid: get(props, 'valid', true),
            message: get(props.message, ''),
            inputWidth: undefined,
            suggestions: [],
            mouseTrigger: false
        };

        this.onValueChange = this.onValueChange.bind(this);
        this.changeValue = this.changeValue.bind(this);
        this.changeHighlighted = this.changeHighlighted.bind(this);
        this.handleKeyPress = this.handleKeyPress.bind(this);
        this.updateSuggestions = debounce(this.updateSuggestions.bind(this), 200);
    }

    componentWillReceiveProps(nextProps) {
        const {valid, message, value} = nextProps;
        if (valid !== this.state.valid || message !== this.state.message || value !== this.state.displayValue) {
            this.setState({valid, message, displayValue: value});
        }
    }

    onValueChange(ev) {
        var displayValue = get(ev, 'target.value');
        var {valid,message} = this.props.validator(displayValue);
        const inputWidth = ev.target.offsetWidth ? ev.target.offsetWidth : this.state.inputWidth;
        this.setState({displayValue, valid, message, isOpen: false, suggestions: [], inputWidth});
        this.updateSuggestions(displayValue);
        this.props.fireValueChange({ value : displayValue, message, valid});
    }

    updateSuggestions(displayValue) {
        const arrayOrPromise = this.props.getSuggestions(displayValue);
        this.suggestionsPromise = arrayOrPromise;
        Promise.resolve(arrayOrPromise).then((suggestions) => {
            // make sure the suggestions are still relevant when promise returns
            if (arrayOrPromise === this.suggestionsPromise && isArray(suggestions) && suggestions.length > 0) {
                this.setState({isOpen: true, suggestions});
            }
        }).catch((err) => logError(err));
    }


    changeHighlighted(mouseTrigger, newHighlightedIdx) {

        if (newHighlightedIdx !== this.state.highlightedIdx || mouseTrigger !== this.state.mouseTrigger) {
            //console.log('setting mouse trigger: '+mouseTrigger );
            this.setState({highlightedIdx: newHighlightedIdx, mouseTrigger});
        }
    }


    /*
     Change value based on a suggest box action
     @param {Array} current suggestions
     @param {Number} index of the highlighted suggestion
    */
    changeValue(highlightedIdx) {
        const {validator, valueOnSuggestion, fireValueChange} = this.props;
        const {displayValue, suggestions} = this.state;

        if (!isUndefined(highlightedIdx)) {
            const currentSuggestion = suggestions[highlightedIdx];
            const value = valueOnSuggestion ? valueOnSuggestion(displayValue, currentSuggestion) : currentSuggestion;
            if (value !== displayValue) {
                var {valid,message} = validator? validator(value) : {true : ''};
                this.setState({
                    isOpen: false,
                    highlightedIdx: undefined,
                    displayValue: value,
                    valid,
                    message
                });
                fireValueChange({value, valid, message});
            } else {
                this.setState({isOpen: false, highlightedIdx: undefined});
            }
        } else {
            this.setState({isOpen: false});
        }
    }

    handleKeyPress(ev) {
        const {isOpen, highlightedIdx, suggestions} = this.state;

        switch (ev.keyCode) {
            case 13: // enter
            case 9: // tab
                isOpen && this.changeValue(highlightedIdx);
                break;
            case 27: // escape
                this.setState({highlightedIdx : undefined, isOpen: false});
                break;
            case 38: // arrow up
                isOpen && this.changeHighlighted(false, isUndefined(highlightedIdx) ? suggestions.length - 1 : Math.max(0, highlightedIdx - 1));
                break;
            case 40: // arrow down
                isOpen && this.changeHighlighted(false, isUndefined(highlightedIdx) ? 0 : Math.min(highlightedIdx + 1, suggestions.length - 1));
                break;
            default:
                break;
        }
    };


    render() {

        const {displayValue, valid, message, highlightedIdx, isOpen, inputWidth, suggestions, mouseTrigger } = this.state;
        var {label, labelWidth, tooltip, inline, renderSuggestion, wrapperStyle} = this.props;

        const leftOffset = (labelWidth?labelWidth:0)+4;
        const minWidth = (inputWidth?inputWidth-4:50);
        const style = Object.assign({display: inline?'inline-block':'block'}, wrapperStyle);

        return (
            <div className={'SuggestBoxInputField'} style={style} onKeyDown={this.handleKeyPress}>
                <div>
                    <InputFieldView
                        valid={valid}
                        onChange={this.onValueChange}
                        onBlur={() => {isOpen && this.changeValue(undefined);}}
                        value={displayValue}
                        message={message}
                        label={label}
                        labelWidth={labelWidth}
                        tooltip={tooltip}
                    />
                </div>

                {isOpen && <div className={'SuggestBoxPopup'} style={{left: leftOffset, minWidth}} onMouseLeave={() => this.setState({highlightedIdx : undefined})}>
                    <SuggestBox
                        suggestions={suggestions}
                        highlightedIdx={highlightedIdx}
                        renderSuggestion={renderSuggestion || ((suggestion) => <span>{suggestion}</span>)}
                        onChange={this.changeHighlighted.bind(this, true)}
                        onComplete={this.changeValue}
                        mouseTrigger={mouseTrigger}
                    />
                </div>}
            </div>
        );
    }
}


SuggestBoxInputFieldView.propTypes = {
    value:  PropTypes.string,
    fieldKey : PropTypes.string,
    inline : PropTypes.bool,
    label:  PropTypes.string,
    tooltip:  PropTypes.string,
    labelWidth : React.PropTypes.number,
    wrapperStyle: PropTypes.object,     //style to merge into the container div
    getSuggestions : PropTypes.func,   //suggestionsArr = getSuggestions(displayValue)
    valueOnSuggestion : PropTypes.func, //newDisplayValue = valueOnSuggestion(prevValue, suggestion),
    renderSuggestion : PropTypes.func   // ReactElem = renderSuggestion(suggestion)
};


export const SuggestBoxInputField = fieldGroupConnector(SuggestBoxInputFieldView, getProps, SuggestBoxInputFieldView.propTypes, null);