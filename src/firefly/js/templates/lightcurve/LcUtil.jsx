import React, {PropTypes} from 'react';

export function getTypeData(key, val='', tip = '', labelV='', labelW) {
    return {
        fieldKey: key,
        label: labelV,
        value: val,
        tooltip: tip,
        labelWidth: labelW
    };
}

export var ReadOnlyText = ({label, content, labelWidth, wrapperStyle}) => {
    return (
        <div style={{display: 'flex',...wrapperStyle}}>
            <div style={{width: labelWidth, paddingRight: 4}}> {label} </div>
            <div> {content} </div>
        </div>
    );
};

ReadOnlyText.propTypes = {
    label: PropTypes.string,
    content: PropTypes.string,
    labelWidth: PropTypes.number,
    wrapperStyle: PropTypes.object
};