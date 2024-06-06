/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Accordion, AccordionDetails, AccordionGroup, AccordionSummary, Button} from '@mui/joy';
import React, {memo} from 'react';
import PropTypes, {string, func, node, object, bool} from 'prop-types';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {useFieldGroupConnector} from '../FieldGroupConnector.jsx';
import {useStoreConnector} from 'firefly/ui/SimpleComponent';


export function AccordionPanel(props){
    const isExpandedState = useStoreConnector(()=>(getComponentState(props.componentKey)?.expanded ?? props.expanded));
    const newProps = {...props,
            expanded : isExpandedState,
            onChange: (newExpanded)=>{
                dispatchComponentStateChange(props.componentKey, {expanded: newExpanded});
                props.onChange?.(newExpanded);
        }};

    return <AccordionPanelView{...newProps} />;
}

const MyDiv= (props) => <span {...{...props}}>{props.children}</span>;

export function AccordionPanelView({header, expanded=false, sx, onChange, children }) {
    return (
        <AccordionGroup sx={sx}>
            <Accordion {...{expanded, onChange:(ev,expanded) => onChange(expanded)}}>
                <AccordionSummary variant='soft' component='div'>
                    {header}
                </AccordionSummary>
                <AccordionDetails>
                    {children}
                </AccordionDetails>
            </Accordion>
        </AccordionGroup>
    );
}


export const FieldGroupAccordionPanel= memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    const newProps= {...viewProps,
            expanded: viewProps.value,
            onChange: (expanded) => fireValueChange({ value: expanded })
        };
    return ( <AccordionPanelView{...newProps} /> );
});

AccordionPanelView.propTypes = {
    header: node,
    isOpen: bool,
    headerStyle: object,
    contentStyle: object,
    style: object,
    onChange: func,
};

AccordionPanel.propTypes = {
    componentKey: string.isRequired,
    ...AccordionPanelView.propTypes
};

FieldGroupAccordionPanel.propTypes = {
    fieldKey: string.isRequired,
    groupKey: string,
    initialState: PropTypes.shape({
        value: bool,
    }),
    ...AccordionPanelView.propTypes
};
