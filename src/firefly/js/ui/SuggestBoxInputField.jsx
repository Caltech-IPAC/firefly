import React, {Component, PropTypes}  from 'react';
import {get, isUndefined} from 'lodash';

import {InputFieldView} from './InputFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import './SuggestBoxInputField.css';

function getProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            fireValueChange
        });
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
                return (
                    <Suggestion
                        className={'SuggestBox__Suggestion'+(idx === highlightedIdx ? ' SuggestBox__Suggestion--highlighted' : '')}
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
            message: ''
        };

        this.onValueChange = this.onValueChange.bind(this);
    }


    onValueChange(ev) {
        var displayValue = get(ev, 'target.value');
        var {valid,message} = this.props.validator(displayValue);
        this.setState({displayValue, valid, message, isOpen: true});
        this.props.fireValueChange({ value : displayValue, message, valid});
    }


    render() {

        const {displayValue, valid, message, highlightedIdx, isOpen } = this.state;
        var {validator, label, labelWidth, tooltip, inline, fireValueChange, getSuggestions, renderSuggestion, valueOnSuggestion} = this.props;
        const currentSuggestions = getSuggestions(displayValue);

        const changeHighlighted = (newHighlightedIdx) => {
            if (newHighlightedIdx !== highlightedIdx) { this.setState({highlightedIdx: newHighlightedIdx}); }
        };

        // used when input changes are triggered by suggest box
        const changeValue = (highlightedIdx) => {
            if (!isUndefined(highlightedIdx)) {
                const currentSuggestion = currentSuggestions[highlightedIdx];
                const value = valueOnSuggestion ? valueOnSuggestion(displayValue, currentSuggestion) : currentSuggestion;
                if (value !== displayValue) {
                    var {valid,message} = validator(value);
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
        };

        const handleKeyPress = (ev) => {
            switch (ev.keyCode) {
                case 13: // enter
                case 9: // tab
                    isOpen && changeValue(highlightedIdx);
                    break;
                case 27: // escape
                    this.setState({highlightedIdx : undefined, isOpen: false});
                    break;
                case 38: // arrow up
                    isOpen && changeHighlighted(isUndefined(highlightedIdx) ? currentSuggestions.length - 1 : Math.max(0, highlightedIdx - 1));
                    break;
                case 40: // arrow down
                    isOpen && changeHighlighted(isUndefined(highlightedIdx) ? 0 : Math.min(highlightedIdx + 1, currentSuggestions.length - 1));
                    break;
                default:
                    break;
            }
        };

        const leftOffset = (labelWidth?labelWidth:0)+10;

        return (
            <div className={'SuggestBoxInputField'} style={{display: inline?'inline-block':'block'}} onKeyDown={handleKeyPress}>
                <div>
                    <InputFieldView
                        valid={valid}
                        onChange={this.onValueChange}
                        onBlur={() => {isOpen && changeValue(undefined)}}
                        value={displayValue}
                        message={message}
                        label={label}
                        labelWidth={labelWidth}
                        tooltip={tooltip}
                    />
                </div>

                {isOpen && <div className={'SuggestBoxPopup'} style={{left: leftOffset}} onMouseLeave={() => this.setState({highlightedIdx : undefined})}>
                    <SuggestBox
                        suggestions={currentSuggestions}
                        highlightedIdx={highlightedIdx}
                        renderSuggestion={renderSuggestion || ((suggestion) => <span>{suggestion}</span>)}
                        onChange={changeHighlighted}
                        onComplete={changeValue}
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