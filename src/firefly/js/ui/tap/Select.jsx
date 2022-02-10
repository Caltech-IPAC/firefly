import React, {memo} from 'react';
import PropTypes from 'prop-types';
import Select, {components} from 'react-select';
import {truncate} from 'lodash';
import LOADING from 'html/images/gxt/loading.gif';
import {useFieldGroupConnector} from '../FieldGroupConnector.jsx';

export const commonSelectStyles = {
    option: (styles) => (
        {...styles,
            color: 'black', // avoid switching to white for selected item
            height: '100%'
        }
    ),
    singleValue: () =>
        ( { color: 'black', marginLeft: 2, marginRight: 2, maxWidth: 'calc(100% - 8px)', position: 'absolute' })
};

export const selectTheme = (theme) => ({
      ...theme,
      colors: {
          ...theme.colors,
          primary: '#7df26a',
          primary75: '#31eb14',
          primary50: '#c1f9b9',
          primary25: '#eafde8'
      }});

// LoadingIndicator component
const LoadingIndicator = () => ( <img style={{width: 20, height: 20, padding: 5}} src={LOADING}/> );

const achoreRE= /<a.*(\/>|<\/a>)/;

function cleanUp(s) {
    if (!s.includes('<a ')) return truncate(s, {length: 140});
    const aStr= s.match(achoreRE)?.[0];
    if (!aStr) return truncate(s, {length: 140});
    const tmp= document.createElement('div');
    tmp.innerHTML= aStr;
    tmp.children[0].target= 'tapOpen';
    tmp.children[0].title= tmp.children[0].innerHTML;
    tmp.children[0].innerHTML= truncate(tmp.children[0].innerHTML, {length: 80});
    return s.replace(achoreRE, tmp.innerHTML);

}

// Option component
const SelectOption = (props) => {
    const {data} = props;
    return (
        <components.Option {...props}>
            <div>
                <b>{data.value}</b>
                <div dangerouslySetInnerHTML={{__html: `${cleanUp(data.label)}`}}/>
            </div>
        </components.Option>
    );
};

const getSelectSingleValueComponent= (type,internalHeight) =>
    (props) => {
        const {data, options} = props;
        if (!options?.length) return null;
        return (
            <components.SingleValue {...props}>
                <div style={{whiteSpace: 'normal', height: internalHeight}} >
                    <div style={{paddingBottom: 5}}>
                        <span style={{paddingRight: 5}}>{type}: </span>
                        <b style={{fontSize: '9pt'}}> {data.value} </b>
                    </div>
                    <div dangerouslySetInnerHTML={{__html: `${cleanUp(data.label)}`}}/>
                </div>
            </components.SingleValue>
        );
    };

const groupStyles = {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    fontSize: 'larger',
    color: 'black'
};


function formatGroupLabel(data) {
    // display group label with the number of items
    // alternatively, we can use custom menu component or menu list header instead of group
    return (
        <div style={groupStyles}>
            <span>{data.options.length} {data.label} </span>
        </div>
    );
}

export function NameSelect({options, value, onSelect, typeDesc, typeDescPlural, selectProps={}, internalHeight='45px'}) {
    const selectStyles = {
        valueContainer: (styles) => ( { ...styles, height: 55 }),
        ...commonSelectStyles
    };

    const placeholder = value ? `${typeDesc} <${value}>. Replace...` : `Select ${typeDesc}...`;

    let groupedOptions = [];
    if (options && options.length > 0) {
        const nOpts = options.length;
        const groupLabel = `${nOpts>1?typeDescPlural:typeDesc}`;
        groupedOptions = [{
            label: groupLabel,
            options
        }];
    }

    return (
        <Select key={`select${typeDesc}`}
                components={{ LoadingIndicator, Option: SelectOption, SingleValue: getSelectSingleValueComponent(typeDesc,internalHeight) }}
                isLoading={!options}
                formatGroupLabel={formatGroupLabel}
                options={groupedOptions}
                value={options ? options.find((e) => e.value === value) : null}
                onChange={(selectedOption, action) => {
                    if (selectedOption && selectedOption.value) {
                        onSelect(selectedOption.value);
                    }
                }}
                styles={selectStyles}
                theme={selectTheme}
                placeholder={placeholder}
                {...selectProps}
        />
    );
}


export const NameSelectField = memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    return ( <NameSelect {...viewProps }
                         onSelect={(selectedValue) => {
                             fireValueChange({value: selectedValue});
                             if (viewProps.onSelect) viewProps.onSelect(selectedValue);
                         } }/>
    );

});

NameSelectField.propTypes = {
    fieldKey : PropTypes.string,
    groupKey : PropTypes.string,
    options: PropTypes.array,
    onSelect: PropTypes.func,
    typeDesc: PropTypes.string,
    typeDescPlural: PropTypes.string,
    selectProps: PropTypes.object,
    internalHeight: PropTypes.string,
    initialState: PropTypes.shape({
        value: PropTypes.string,
    })
};
