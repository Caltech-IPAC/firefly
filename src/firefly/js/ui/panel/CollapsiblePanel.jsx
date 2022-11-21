/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import './CollapsiblePanel.css';
import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {isFunction} from 'lodash';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {useFieldGroupConnector} from '../FieldGroupConnector.jsx';
import {useStoreConnector} from 'firefly/ui/SimpleComponent';


export function CollapsiblePanel(props){
    const isOpenState = useStoreConnector(()=>(getComponentState(props.componentKey)?.isOpen ?? props.isOpen));
    const newProps = {...props,
            isOpen : isOpenState,
            onToggle: (newIsOpen)=>{
                dispatchComponentStateChange(props.componentKey, {isOpen: newIsOpen});
                props.onToggle?.(newIsOpen);
        }};

    return <CollapsiblePanelView {...newProps} />;
}


export function CollapsiblePanelView({header, isOpen=false, headerStyle, contentStyle, style,
                                         onToggle, children }) {
    let headerClassName = 'CollapsiblePanel__Header';
    if (isOpen) headerClassName += ' CollapsiblePanel__Header--is-open';
    const headerContent = isFunction(header) ? header() : header;

    const contentClassName = 'CollapsiblePanel__Content';
    const contentHeight = isOpen ? 'initial' : 0;
    contentStyle = {display: 'block', ...contentStyle, height: contentHeight};

    return (
        <div style={{display: 'flex', flexDirection: 'column', ...style}}>
            <div style={headerStyle} onClick={()=>{onToggle(!isOpen);}} className={headerClassName}>
                {headerContent}
            </div>
            <div style={contentStyle} className={contentClassName}>
                {children}
            </div>
        </div>
    );
}


export const FieldGroupCollapsible= memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    const newProps= {...viewProps,
            isOpen: Boolean(viewProps.value === 'open'),
            onToggle: (isOpen) => fireValueChange({ value: isOpen ? 'open' : 'closed' })
        };
    return ( <CollapsiblePanelView {...newProps} /> );
});

CollapsiblePanelView.propTypes = {
    header: PropTypes.node,
    isOpen: PropTypes.bool,
    headerStyle: PropTypes.object,
    contentStyle: PropTypes.object,
    style: PropTypes.object,
    onToggle: PropTypes.func,
};

CollapsiblePanel.propTypes = {
    componentKey: PropTypes.string.isRequired,
    ...CollapsiblePanelView.propTypes
};

FieldGroupCollapsible.propTypes = {
    fieldKey: PropTypes.string.isRequired,
    groupKey: PropTypes.string,
    initialState: PropTypes.shape({
        value: PropTypes.string,  // 'open' or 'closed'
    }),
    ...CollapsiblePanelView.propTypes
};
