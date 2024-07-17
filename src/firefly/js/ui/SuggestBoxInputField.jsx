import React, {memo, PureComponent} from 'react';
import PropTypes, {object, shape} from 'prop-types';
import ReactDOM from 'react-dom';
import {get, isArray, isUndefined, debounce} from 'lodash';
import {dispatchHideDialog, dispatchShowDialog, isDialogVisible} from '../core/ComponentCntlr.js';
import DialogRootContainer from './DialogRootContainer.jsx';
import {DropDownMenuWrapper} from './DropDownMenu.jsx';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {logger} from '../util/Logger.js';


import {InputFieldView} from './InputFieldView.jsx';
import './SuggestBoxInputField.css';


/**
 *  Make sure a component (like highlighted suggestion) is visible
 *  @param  el
 *  @param {Number} highlightedIdx
 */
function ensureVisible(el, highlightedIdx) {
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

function computeDropdownXY(element) {
    const bodyRect = document.body.parentElement.getBoundingClientRect();
    const elemRect = element.getBoundingClientRect();
    const x = elemRect.left - bodyRect.left;
    const y = elemRect.bottom -10;
    return {x,y};
}

const dropKey= 'suggestion-box-drop';

function showDrop(entryElement,dropDown, offComponentCB) {

    const beforeVisible= (e) =>{
        if (!e) return;
        const {x,y}= computeDropdownXY(entryElement);
        e.style.left= x+'px';
        e.style.top= y+'px';
    };
    const dd= <DropDownMenuWrapper x={0} y={0} content={dropDown} beforeVisible={beforeVisible}/>;
    DialogRootContainer.defineDialog(dropKey,dd);
    dispatchShowDialog(dropKey);
    document.removeEventListener('mousedown', offComponentCB);
    setTimeout(() => {
        document.addEventListener('mousedown', offComponentCB);
    },10);
}

export class SuggestBoxInputFieldView extends PureComponent {
    constructor(props) {
        super(props);
        this.state = {
            isOpen: false,
            displayValue: props.value,
            validator: props.validator,
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
        this.offComponentCallback= this.offComponentCallback.bind(this);
    }



    static getDerivedStateFromProps(props,state) {
        const {valid, message, value, validator} = props;
        if (valid !== state.valid || message !== state.message || value !== state.displayValue || validator !== state.validator) {
            return {valid, message, displayValue: value, validator};
        }
        return null;
    }

    onValueChange(ev) {
        var displayValue = get(ev, 'target.value');
        var {valid,message} = this.props.validator(displayValue);
        const inputWidth = ev.target.offsetWidth ? ev.target.offsetWidth : this.state.inputWidth;
        this.setState({displayValue, valid, message, inputWidth});
        this.updateSuggestions(displayValue);
        this.props.fireValueChange({ value : displayValue, message, valid});
    }

    componentWillUnmount() {
        document.removeEventListener('mousedown', this.offComponentCallback);// just in case
        setTimeout(() => {
            if (isDialogVisible(dropKey)) dispatchHideDialog(dropKey);
        },5);
    }

    offComponentCallback(ev) {
        this.setState({isOpen: false, highlightedIdx: undefined});
        document.removeEventListener('mousedown', this.offComponentCallback);// just in case
    }

    updateSuggestions(displayValue) {
        const arrayOrPromise = this.props.getSuggestions(displayValue);
        this.suggestionsPromise = arrayOrPromise;
        Promise.resolve(arrayOrPromise).then((suggestions) => {
            // make sure the suggestions are still relevant when promise returns
            if (arrayOrPromise === this.suggestionsPromise && isArray(suggestions) && suggestions.length > 0) {
                this.setState({isOpen: true, suggestions});
            } else {
                if (this.state.isOpen) this.setState({isOpen: false, suggestions: []});
            }
        }).catch((err) => logger.error(err));
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
                var {valid,message} = validator? validator(value) : {valid: true, message:''};
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
        this.divElement= ev.target;

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
        const {label, tooltip, renderSuggestion, wrapperStyle, placeholder, slotProps={}, sx,
            popStyle, popupIndex, readonly=false, required=false} = this.props;

        const leftOffset = 0;
        const minWidth = (inputWidth?inputWidth-4:50);
        const style = Object.assign({display: 'flex'}, wrapperStyle);
        const pStyle = Object.assign({left: leftOffset, minWidth, zIndex: popupIndex}, popStyle);

        if (isOpen) {
            if ((!isDialogVisible(dropKey) || this.lastHightlighedIdx!==highlightedIdx || this.lastSuggestions!==suggestions))  {
                this.lastHightlighedIdx= highlightedIdx;
                this.lastSuggestions= suggestions;
                const box= (
                    <div className={'SuggestBoxPopup'} style={pStyle} onMouseLeave={() => this.setState({highlightedIdx : undefined})}>
                        <SuggestBox
                            suggestions={suggestions}
                            highlightedIdx={highlightedIdx}
                            renderSuggestion={renderSuggestion || ((suggestion) => <span>{suggestion}</span>)}
                            onChange={this.changeHighlighted.bind(this, true)}
                            onComplete={this.changeValue}
                            mouseTrigger={mouseTrigger}
                        />
                    </div>);
                setTimeout(() => {
                    showDrop(this.divElement,box,this.offComponentCallback);
                },5);
            }
        }
        else {
            setTimeout(() => dispatchHideDialog(dropKey),5);
        }

        return (
            <div className={'SuggestBoxInputField'} style={style} onKeyDown={this.handleKeyPress}>
                <InputFieldView {...{
                    valid: Boolean(valid),
                    onChange: this.onValueChange,
                    placeholder,
                    value: displayValue,
                    message, label, tooltip, readonly, required,
                    onBlur: () => isOpen && this.changeValue(undefined),
                    endDecorator: this.props.endDecorator,
                    slotProps: { tooltip: {placement: 'right'}, ...slotProps },
                    sx
                }} />
            </div>
        );
    }
}


SuggestBoxInputFieldView.propTypes = {
    value:  PropTypes.string,
    fieldKey : PropTypes.string,
    inline : PropTypes.bool,
    label:  PropTypes.string,
    sx: PropTypes.object,
    placeholder:  PropTypes.string,
    tooltip:  PropTypes.string,
    popStyle : PropTypes.object, //style for the popup list
    wrapperStyle: PropTypes.object,     //style to merge into the container div
    getSuggestions : PropTypes.func,   //suggestionsArr = getSuggestions(displayValue)
    valueOnSuggestion : PropTypes.func, //newDisplayValue = valueOnSuggestion(prevValue, suggestion),
    renderSuggestion : PropTypes.func,   // ReactElem = renderSuggestion(suggestion)
    popupIndex: PropTypes.number,
    endDecorator : PropTypes.object,
    valid: PropTypes.bool,
    message: PropTypes.string,
    readonly: PropTypes.bool,
    validator: PropTypes.func,
    required: PropTypes.bool,
    slotProps: shape({
        input: object,
        control: object,
        label: object,
        tooltip: object
    }),
};


export const SuggestBoxInputField= memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    return <SuggestBoxInputFieldView {...{...viewProps, fireValueChange}} /> ;

});


SuggestBoxInputField.propTypes = {
    fieldKey : PropTypes.string.isRequired,
    groupKey : PropTypes.string,
    inline : PropTypes.bool,
    label:  PropTypes.string,
    placeholder:  PropTypes.string,
    sx: PropTypes.object,
    tooltip:  PropTypes.string,
    popStyle : PropTypes.object, //style for the popup list
    wrapperStyle: PropTypes.object,     //style to merge into the container div
    getSuggestions : PropTypes.func,   //suggestionsArr = getSuggestions(displayValue)
    valueOnSuggestion : PropTypes.func, //newDisplayValue = valueOnSuggestion(prevValue, suggestion),
    renderSuggestion : PropTypes.func,   // ReactElem = renderSuggestion(suggestion)
    endDecorator : PropTypes.object,
    popupIndex: PropTypes.number,
    required: PropTypes.bool,
    initialState: PropTypes.shape({
        value: PropTypes.string,
        tooltip: PropTypes.string,
        label:  PropTypes.string,
        validator: PropTypes.func
    }),
};

