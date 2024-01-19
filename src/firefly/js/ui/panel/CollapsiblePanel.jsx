/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import './CollapsiblePanel.css';
import React, {memo} from 'react';
import PropTypes, {bool, func, object, oneOfType, shape, string} from 'prop-types';
import {isFunction, pick} from 'lodash';
import {Tooltip, Accordion, AccordionGroup} from '@mui/joy';
import AccordionDetails, {accordionDetailsClasses} from '@mui/joy/AccordionDetails';
import AccordionSummary, {accordionSummaryClasses} from '@mui/joy/AccordionSummary';


import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {useFieldGroupConnector} from '../FieldGroupConnector.jsx';
import {useStoreConnector} from 'firefly/ui/SimpleComponent';


/**
 * Convenient wrapper to include one CollapsibleGroup and one CollapsibleItem
 * @param props
 * @param props.slotProps
 */
export function CollapsiblePanel({slotProps, ...props}) {
    return (
        <CollapsibleGroup {...slotProps?.root}>
            <CollapsibleItem slotProps={slotProps} {...props} />
        </CollapsibleGroup>
    );
}
CollapsiblePanel.propTypes = {
    componentKey: PropTypes.string.isRequired,
    ...CollapsibleItemView.propTypes
};

export const FieldGroupCollapsible= memo( ({children, ...props}) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    const newProps= {
        ...pick(viewProps, Object.keys(CollapsibleItemView.propTypes)),  // useFieldGroupConnector is not removing all of its props, causing bad props going into the Tabs
        children,
        isOpen: Boolean(viewProps.value === 'open'),
        onToggle: (isOpen) => fireValueChange({ value: isOpen ? 'open' : 'closed' })
    };
    return (
        <CollapsibleGroup>
            <CollapsibleItemView {...newProps} />
        </CollapsibleGroup>
    );
});

FieldGroupCollapsible.propTypes = {
    fieldKey: PropTypes.string.isRequired,
    groupKey: PropTypes.string,
    initialState: PropTypes.shape({
        value: PropTypes.string,  // 'open' or 'closed'
    }),
    ...CollapsibleItemView.propTypes
};

/**
 * A stateful collapsible container, intended to be used with CollapsibleGroup. State is backed by ComponentCntlr.
 */
export const CollapsibleItem= memo( ({componentKey, isOpen:defOpen, ...props}) => {

    const isOpen = useStoreConnector(()=>(getComponentState(componentKey)?.isOpen ?? defOpen));
    const onToggle = (newIsOpen)=>{
        dispatchComponentStateChange(componentKey, {isOpen: newIsOpen});
    };

    return (
        <CollapsibleItemView {...{isOpen, onToggle, ...props}}/>
    );
});



/**
 * A container that groups multiple CollapsibleItems.  View only, no state management.
 * @param props
 * @param props.sx
 * @param props.children
 */
export function CollapsibleGroup({sx, children, ...props}) {

    return(
        <AccordionGroup
            variant='outlined'
            transition='0.2s'
            sx={{
                borderRadius: 'sm',
                [`& .${accordionSummaryClasses.button}:hover`]: {
                    bgcolor: 'transparent',
                    },
                [`& .${accordionDetailsClasses.content}`]: {
                    [`&.${accordionDetailsClasses.expanded}`]: {
                        paddingBlock: '0.75rem',
                    },
                },
                ...sx
            }}
            {...props}
        >
            {children}
        </AccordionGroup>
    );

}

/**
 * A collapsible container, intended to be used with CollapsibleGroup. View only, no state management.
 * @param props
 * @param props.isOpen
 * @param props.header
 * @param props.title
 * @param props.onToggle
 * @param props.slotProps
 * @param props.children
 */
export function CollapsibleItemView({isOpen, header, title, onToggle, slotProps, children, ...props}) {

    header = isFunction(header) ? header() : header;
    const onChange = (e,v) => {
        onToggle?.(v);
    };

    return (
        <Accordion {...props} expanded={isOpen} onChange={onChange}
        >
            <Tooltip {...{title, ...slotProps?.tooltip}}>
                <AccordionSummary {...slotProps?.header}
                                  sx={{whiteSpace:'nowrap', ...slotProps?.header?.sx}}
                >
                    {header}
                </AccordionSummary>
            </Tooltip>
            <AccordionDetails {...slotProps?.content}>{children}</AccordionDetails>
        </Accordion>
    );

}

CollapsibleItemView.propTypes = {
    componentKey: string,
    isOpen: bool,
    header: oneOfType([string, func]),
    title: string,
    slotProps: shape({  // consult JoyUI doc for details.
        header: object,     // passed to AccordionSummary
        content: object,    // passed to AccordionDetails
        tooltip: object     // passed to Tooltip
    })
};

