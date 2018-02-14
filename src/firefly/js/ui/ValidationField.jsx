import React, {PureComponent} from 'react';
import {has} from 'lodash';
import {clone} from '../util/WebUtil.js';
import {FieldGroupEnable} from './FieldGroupEnable.jsx';
import {InputFieldView} from './InputFieldView.jsx';


function onChange(ev, validator, fireValueChange) {
    let value = ev.target.value;
    const {valid,message, ...others}= validator(value);
    has(others, 'value') && (value = others.value);    // allow the validator to modify the value.. useful in auto-correct.
    fireValueChange({ value, message, valid });
}



export class ValidationField extends PureComponent {

    render()  {
        const {fieldKey, initialState, forceReinit}= this.props;
        return (
            <FieldGroupEnable fieldKey={fieldKey} initialState={initialState} forceReinit={forceReinit}>
                {
                    (propsFromStore, fireValueChange) => {
                        return <InputFieldView {...clone(this.props, propsFromStore)}
                                               onChange={(ev) => onChange(ev,propsFromStore.validator, fireValueChange)}/> ;
                    }

                }
            </FieldGroupEnable>
        );

    }
}
