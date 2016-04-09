import React, {Component, PropTypes}  from 'react';
import ReactDOM from 'react-dom';
import {get, isArray, isUndefined} from 'lodash';

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
 */
function ensureVisible(c) {
    const el = ReactDOM.findDOMNode(c); //DOMElement
    if (el) {el.scrollIntoView();}
}

const Suggestion = (props) => {
    const {suggestion, renderSuggestion, highlighted, ...otherProps} = props;
    return ( <li ref={(c)=>highlighted&&ensureVisible(c)} {...otherProps}>{renderSuggestion(suggestion)}</li>);
};

Suggestion.propTypes = {
    suggestion : PropTypes.any,
    renderSuggestion: PropTypes.func
};

const SuggestBox = (props) => {
    var {suggestions, highlightedIdx, renderSuggestion, onChange, onComplete} = props;
    suggestions = suggestions || [];

    const handleOptionMouseEnter = (idx, event) => {
        onChange(idx);
    };

    const handleOptionClick = (idx, event) => {
        onComplete(idx);
    };

    return (
        <ul className={'SuggestBox'}>
            {suggestions.map((suggestion, idx) => {
                const highlighted = idx === highlightedIdx;
                return (
                    <Suggestion
                        className={'SuggestBox__Suggestion'+(highlighted ? ' SuggestBox__Suggestion--highlighted' : '')}
                        highlighted={highlighted}
                        renderSuggestion={renderSuggestion}
                        key={idx}
                        onMouseDown={handleOptionClick.bind(this, idx)} //onClick competes with onBlur
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
            valid: true,
            message: '',
            inputWidth: undefined,
            suggestions: []
        };

        this.onValueChange = this.onValueChange.bind(this);
        this.changeValue = this.changeValue.bind(this);
        this.changeHighlighted = this.changeHighlighted.bind(this);
        this.handleKeyPress = this.handleKeyPress.bind(this);
        this.updateSuggestions = this.updateSuggestions.bind(this);
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
        }).catch(err => logError(err))
    }


    changeHighlighted(newHighlightedIdx) {
        if (newHighlightedIdx !== this.state.highlightedIdx) { this.setState({highlightedIdx: newHighlightedIdx}); }
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
                isOpen && this.changeHighlighted(isUndefined(highlightedIdx) ? suggestions.length - 1 : Math.max(0, highlightedIdx - 1));
                break;
            case 40: // arrow down
                isOpen && this.changeHighlighted(isUndefined(highlightedIdx) ? 0 : Math.min(highlightedIdx + 1, suggestions.length - 1));
                break;
            default:
                break;
        }
    };


    render() {

        const {displayValue, valid, message, highlightedIdx, isOpen, inputWidth, suggestions } = this.state;
        var {label, labelWidth, tooltip, inline, renderSuggestion} = this.props;

        const leftOffset = (labelWidth?labelWidth:0)+4;
        const minWidth = (inputWidth?inputWidth-4:50);

        return (
            <div className={'SuggestBoxInputField'} style={{display: inline?'inline-block':'block'}} onKeyDown={this.handleKeyPress}>
                <div>
                    <InputFieldView
                        valid={valid}
                        onChange={this.onValueChange}
                        onBlur={() => {isOpen && this.changeValue(undefined)}}
                        value={displayValue}
                        message={message}
                        label={label}
                        labelWidth={labelWidth}
                        tooltip={tooltip}
                    />
                </div>

                {isOpen && <div className={'SuggestBoxPopup'} style={{left: leftOffset, minWidth: minWidth}} onMouseLeave={() => this.setState({highlightedIdx : undefined})}>
                    <SuggestBox
                        suggestions={suggestions}
                        highlightedIdx={highlightedIdx}
                        renderSuggestion={renderSuggestion || ((suggestion) => <span>{suggestion}</span>)}
                        onChange={this.changeHighlighted}
                        onComplete={this.changeValue}
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
    getSuggestions : PropTypes.func,   //suggestionsArr = getSuggestions(displayValue)
    valueOnSuggestion : PropTypes.func, //newDisplayValue = valueOnSuggestion(prevValue, suggestion),
    renderSuggestion : PropTypes.func   // ReactElem = renderSuggestion(suggestion)
};


export const SuggestBoxInputField = fieldGroupConnector(SuggestBoxInputFieldView, getProps, SuggestBoxInputFieldView.propTypes, null);