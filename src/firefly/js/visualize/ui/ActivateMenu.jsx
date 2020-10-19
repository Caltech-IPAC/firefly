/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import PropTypes, {string,func, object, arrayOf} from 'prop-types';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {CompleteButton} from '../../ui/CompleteButton.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';


const GROUP_KEY= 'ActivateMenu';


export const ActivateMenu= memo(({ serDefParams, setSearchParams, title, makeDropDown}) => {


    const loadParams= (request) => {
        setSearchParams(request);
    };

    return (
        <div>
            {makeDropDown?.()}
            <div style={{padding: '5px 5px 5px 5px'}}>
                <FieldGroup groupKey={GROUP_KEY} validatorFunc={null} keepState={false}>
                    {makeActivateInput(serDefParams)}
                </FieldGroup>
                <CompleteButton style={{padding: '10px 0 0 0'}} onSuccess={loadParams} text={'Load - '+title} groupKey={GROUP_KEY} />
            </div>
        </div>
    );
});

ActivateMenu.propTypes= {
    serDefParams: arrayOf(object),
    title: string,
    setSearchParams: func,
    makeDropDown: func
};


/**
 * @param {ServiceDescriptorInputParam} serviceDefParams
 * @return {*}
 */

function makeActivateInput(serDefParams) {
    return serDefParams
        .filter( (sdP) => !sdP.ref )
        .map( (sdP) => {
            const {optionalParam,minValue, maxValue,value, name,options,desc:tooltip} = sdP;

            if (options) {
                const fieldOps = options.split(',').map( (op) => ({label:op,value:op}));
                return (
                    <RadioGroupInputField
                        key={name}
                        initialState= {{
                            value: fieldOps[0],
                            tooltip,
                            label: {name}
                        }}
                        options={fieldOps} alignment='vertical' fieldKey={name} groupKey={GROUP_KEY}/>
                    );
            }
            else {
                return (
                    <div key={name} style={{paddingTop:8}}>
                        <ValidationField
                            style={{width: 300}}
                            labelStyle={{textAlign:'right'}}
                            initialState= {{
                                value: value || '',
                                tooltip,
                                validator: (s) => {
                                    return s||optionalParam ? { valid : true, message : '' } :
                                        { valid : false, message : 'Value is Required' };
                                }
                            }}
                            fieldKey={name}
                            groupKey={GROUP_KEY}
                            labelWidth={100}
                            label={`${name}${optionalParam?(' (optional)'):''}: `}
                        />
                        {minValue && <div style={{paddingTop:5, textAlign:'center'}}>{`Min Value: ${minValue}`}</div>}
                        {maxValue && <div style={{paddingTop:5, textAlign:'center'}}>{`Max Value: ${maxValue}`}</div>}
                    </div>
                );
            }
    });

}

