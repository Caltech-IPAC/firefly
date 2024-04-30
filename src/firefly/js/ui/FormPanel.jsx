/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Button, Sheet, Stack} from '@mui/joy';
import React, {useCallback} from 'react';
import {elementType, shape, node, string, func, oneOfType, element, bool, arrayOf} from 'prop-types';
import CompleteButton from './CompleteButton.jsx';
import {HelpIcon} from './HelpIcon.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {dispatchFormCancel, dispatchFormSubmit} from 'firefly/core/AppDataCntlr.js';
import {Stacker} from 'firefly/ui/Stacker.jsx';
import {Slot} from 'firefly/ui/SimpleComponent.jsx';

/*
onSuccess: This function is invoked when the submit button is pressed and all input fields pass validation.
           It's the responsibility of the callback function to perform further validation or proceed with form submission.
           The callback should return a boolean value indicating whether the form was submitted or not.
           By default, this value is set to true.
           When the form is submitted, it will call dispatchHideDropDown to hide the form.

onError: This function is invoked when the submit button is pressed and one or more input fields fail validation.
         dispatchHideDropDown is not triggered, allowing the form to remain visible with validation errors.

onCancel: This function is invoked when the cancel button is pressed.
 */
export const FormPanel = function ({groupKey, onSuccess, onError, onCancel, cancelText='Cancel', completeText='Search', help_id, disabledDropdownHide, slotProps, children, ...rootProps}) {

    const doSubmit = ((p, valid) => {

        const funcToCall = valid ? onSuccess : onError;

        const submitted = funcToCall?.(p) ?? valid;
        if (submitted) {
            dispatchFormSubmit(p);
            !disabledDropdownHide && dispatchHideDropDown();
        }
        return submitted;
    });

    const searchBarEnd = help_id && <HelpIcon helpId={help_id}/>;

    return (
        <Stack component={Sheet} className='ff-FormPanel' spacing={1} p={1} height={1} {...rootProps}>
            <Slot component={Stack} flexGrow={1} slotProps={slotProps?.input}>
                {children}
            </Slot>
            <Slot component={Stacker} endDecorator={searchBarEnd} slotProps={slotProps?.searchBar}>
                <Slot component={CompleteButton}
                      text={completeText}
                      groupKey={groupKey}
                      onSuccess={doSubmit} onFail={doSubmit}
                      slotProps={slotProps?.completeBtn}
                />
                <Slot component={CancelButton} size='md' text={cancelText} onClick={onCancel} slotProps={slotProps?.cancelBtn}/>
                {slotProps?.searchBar?.actions}
            </Slot>
        </Stack>
    );
};

FormPanel.propTypes = {
    groupKey: oneOfType([string, func]),
    title: oneOfType([string, func]),
    onSuccess: func,                // invoked when validation passed; func(params: object) => bool
    onError: func,                  // invoked when validation failed; func(params: object) => bool
    onCancel: func,                 // invoked when cancelBtn is pressed.
    completeText: node,             // Defaults to 'Search'
    cancelText: node,               // Defaults to 'Cancel'; falsy value will remove cancel button
    disabledDropdownHide: bool,     // if true, do not call dropdown hide when form is submitted.
    help_id: string,
    ...Sheet.propTypes,
    slotProps: shape({
        input: shape({
            ...Stack.propTypes
        }),
        searchBar: shape({
            component: elementType,   // set to null to hide the search bar; true for any slots defined here.
            actions: oneOfType([element, arrayOf(element)]),         // additional actions go between ok/cancel and help icon
            ...Stack.propTypes
        }),
        completeBtn: shape({
            component: elementType,
            ...CompleteButton.propTypes
        }),
        cancelBtn: shape({
            component: elementType,
            ...Button.propTypes
        }),
    })
};

function CancelButton({text, onClick}) {

    const doCancel = useCallback(() => {
        dispatchFormCancel();
        onClick?.();
    }, []);

    return text ? <Button size='md' onClick={doCancel}>{text}</Button> : false;
}
