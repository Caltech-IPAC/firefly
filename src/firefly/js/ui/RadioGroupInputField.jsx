import React, {PureComponent} from 'react';
import {bool, array, string, number, object} from 'prop-types';
import {isEmpty, isUndefined, get}  from 'lodash';
import {RadioGroupInputFieldView} from './RadioGroupInputFieldView.jsx';
import {FieldGroupEnable} from './FieldGroupEnable.jsx';


const assureValue= (value,options,defaultValue) => {
    if (value) return value;
    return isUndefined(defaultValue) ? options[0].value : defaultValue;
};

function handleOnChange(ev, params, fireValueChange) {
    const val = get(ev, 'target.value', '');
    const checked = get(ev, 'target.checked', false);
    if (checked) {
        fireValueChange({ value: val, valid: true});
    }
}



function checkForUndefined(v,props) {
    const {options=[], defaultValue, isGrouped=false} = props;
    const optionContain = (v) => v && options.find((op) => op.value === v);
    if (isEmpty(options) || optionContain(v) || isGrouped) {
        return v;
    } else {
        return isUndefined(defaultValue) ? options[0].value : defaultValue;
    }
}

export class RadioGroupInputField extends PureComponent {

    render()  {
        const {fieldKey, initialState, forceReinit, options, isGrouped, defaultValue}= this.props;
        return (
            <FieldGroupEnable fieldKey={fieldKey} initialState={initialState}
                              forceReinit={forceReinit} options={options}
                              isGrouped={isGrouped} defaultValue={defaultValue}
                              confirmInitialValue={checkForUndefined}>
                {
                    (propsFromStore, fireValueChange) => {
                        const {options, defaultValue}= propsFromStore;
                        const value= assureValue(propsFromStore.value,options, defaultValue);
                        const newProps= Object.assign({}, this.props, propsFromStore, { value});
                        return <RadioGroupInputFieldView {...newProps}
                                           onChange={(ev) => handleOnChange(ev,propsFromStore, fireValueChange)}/> ;
                    }
                }
            </FieldGroupEnable>
        );

    }
}


RadioGroupInputField.propTypes= {
    inline : bool,
    options: array,
    defaultValue: string,
    alignment:  string,
    labelWidth : number,
    labelStyle: object,
    isGrouped: bool,
    initialState:  object,
    forceReinit:  bool,
    fieldKey:   string
};
