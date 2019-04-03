import React from 'react';
import PropTypes from 'prop-types';
import Select, {components} from 'react-select';
import {truncate} from 'lodash';
import LOADING from 'html/images/gxt/loading.gif';

import {fieldGroupConnector} from '../FieldGroupConnector';

export const commonSelectStyles = {
    option: (styles) => (
        {...styles,
            color: 'black', // avoid switching to white for selected item
            height: '100%'
        }
    ),
    singleValue: () => {
        return {
            color: 'black',
            marginLeft: 2,
            marginRight: 2,
            maxWidth: 'calc(100% - 8px)',
            position: 'absolute'
        };
    }
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
const LoadingIndicator = () => {
  return (
      <img style={{width: 20, height: 20, padding: 5}} src={LOADING}/>
  );
};

// Option component
const SelectOption = ({chidren,...props}) => {
        const {data} = props;
        const html = `<b>${data.value}</b><br/>${data.label}`;
        const v = {__html: html};
        return (
            <components.Option {...props}>
                <div dangerouslySetInnerHTML={v}/>
            </components.Option>
        );
};

function getSelectSingleValueComponent(type) {
    return ({children, ...props}) => {
        const {data, options} = props;
        if (options && options.length > 0) {
            const html = `<b style="font-size: 9pt">${type}: ${data.value}</b><br/>${truncate(data.label, {length: 140})}`;
            const v = {__html: html};
            // use whiteSpace attribute to wrap the text and collapse the whitespace
            // without it the text does not wrap
            return (
                <components.SingleValue {...props}>
                    <div style={{whiteSpace: 'normal'}}
                         dangerouslySetInnerHTML={v}
                    />
                </components.SingleValue>
            );
        } else {
            return null;
        }
    };
}

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
            {false && <span>(todo: browser)</span>}
        </div>
    );
}

export function NameSelect({options, value, onSelect, type, selectProps={}}) {
    const selectStyles = Object.assign({
            valueContainer: (styles) => (
                { ...styles, height: 50 }),
        }, commonSelectStyles);

    const placeholder = value ? `${type} <${value}>. Replace...` : `Select ${type}...`;

    let groupedOptions = [];
    if (options && options.length > 0) {
        const nOpts = options.length;
        const groupLabel = `${type}${(nOpts>1?'s':'')}`;
        groupedOptions = [{
            label: groupLabel,
            options
        }];
    }

    return (
        <Select key={`select${type}`}
                components={{ LoadingIndicator, Option: SelectOption, SingleValue: getSelectSingleValueComponent(type) }}
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


export const NameSelectField = fieldGroupConnector(NameSelect, getProps, NameSelect.propTypes, null);

NameSelect.propTypes = {
    options: PropTypes.array,
    value: PropTypes.string,
    onSelect: PropTypes.func,
    type: PropTypes.string,
    selectProps: PropTypes.object
};

function getProps(params, fireValueChange) {
    return Object.assign({}, params, {
        onSelect: (selectedValue) => {
            fireValueChange({value: selectedValue});
            if (params.onSelect) {
                params.onSelect(selectedValue);
            }
        }
    });
}
