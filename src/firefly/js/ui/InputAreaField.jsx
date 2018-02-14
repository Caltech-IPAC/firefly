import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';

import {InputAreaFieldView, propTypes} from './InputAreaFieldView.jsx';
import {clone} from '../util/WebUtil.js';
import {FieldGroupEnable} from './FieldGroupEnable';
import {InputFieldActOn} from './InputField';

export const InputAreaField = (props) => <InputFieldActOn View={InputAreaFieldView} {...props}/>;


function onChange(ev, validator, fireValueChange) {
    const {valid,message}= validator(ev.target.value);
    fireValueChange({ value : ev.target.value, message, valid });
}

export class InputAreaFieldConnected extends PureComponent {

    render()  {
        const {fieldKey, initialState, forceReinit}= this.props;
        return (
            <FieldGroupEnable fieldKey={fieldKey} initialState={initialState} forceReinit={forceReinit}>
                {
                    (propsFromStore, fireValueChange) => {
                        return <InputAreaFieldView {...clone(this.props, propsFromStore)}
                                               onChange={(ev) => onChange(ev,propsFromStore.validator, fireValueChange)}/> ;
                    }

                }
            </FieldGroupEnable>
        );

    }
}

InputAreaFieldConnected.defaultProps = {
    showWarning: true,
    actOn: ['changes'],
    labelWidth: 0,
    value: '',
    inline: false,
    visible: true,
    rows: 10,
    cols: 50
};

InputAreaFieldConnected.propTypes = {
    ...propTypes,
    fieldKey: PropTypes.string,
    onChange: PropTypes.func,
    actOn: PropTypes.arrayOf(PropTypes.oneOf(['blur', 'enter', 'changes'])),
    validator: PropTypes.func,
    value: PropTypes.string
};

