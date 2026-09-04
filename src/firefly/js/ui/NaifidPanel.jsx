/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useRef} from 'react';
import PropTypes from 'prop-types';
import {sortBy} from 'lodash';
import {AutocompleteOption} from '@mui/joy';
import {resolveNaifidObj} from  './NaifidPanelWorker.js';
import {AutoCompleteInputView, useAsyncOptions, useDebounced} from './AutoCompleteInput.jsx';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {TargetFeedback} from './TargetFeedback';


const LABEL_DEFAULT = 'Moving Target Name:';
const DEFAULT_FORMAT = 'default';
const searchHistory = {[DEFAULT_FORMAT]: []}; // defining as global to persist it throughout the lifetime of app

// the naif lookup is done on the server, joy must not filter the result set on top of it
const noClientFilter= (options) => options;

const SUGGEST_DEBOUNCE_MS= 200;


function renderNaifOption(optProps, suggestion) {
    const {key, ...rest}= optProps ?? {};
    return (
        <AutocompleteOption {...rest} key={key ?? `${suggestion.name}-${suggestion.naifid}`}>
            <span>Name: <b>{suggestion.name}</b>, NAIF ID: <b>{suggestion.naifid}</b></span>
        </AutocompleteOption>
    );
}


function NaifidPanelView({showHelp, valid, message, examples, feedback, value, feedbackStyle, popStyle,
                             label= LABEL_DEFAULT, fireValueChange, naifNameRef,
                             naifIdFormat=DEFAULT_FORMAT}){
    // the name just picked, so the value change it causes does not trigger an unnecessary lookup
    const selectedNameRef= useRef(value);

    const getSuggestions = (val= '') => {
        if (!val) return [];
        if (val===selectedNameRef.current) return []; // just picked, already resolved
        if (naifIdFormat && !searchHistory[naifIdFormat]) searchHistory[naifIdFormat] = [];

        const getResSuggestionsList = (suggestionsList) => {
            const resSuggestionsList = Object.values(suggestionsList).map((v) => ({name: v.naifName, naifid: v.naifId}));
            // joy uses value/label; naifid rides along for renderOption and onChange
            return sortBy(resSuggestionsList, 'naifid').reverse()
                .map((s) => ({...s, value: s.name, label: s.name}));
        };

        // if value has been searched previously, no need to request from server
        if (searchHistory[naifIdFormat].length > 0){
            const cachedSuggList = Object.values(searchHistory[naifIdFormat]).find((v) => (v.searchVal === val));
            if (cachedSuggList?.searchRes) return getResSuggestionsList(cachedSuggList.searchRes);
        }

        // else request naif IDs from the server
        const rval = resolveNaifidObj(val, naifIdFormat);
        if (!rval.p) return [];
        return rval.p.then((response)=>{
            if (response.valid) {
                const suggestionsList = response.data.map(({naifID, name}) => ({naifId: naifID, naifName: name}));
                searchHistory[naifIdFormat].push({searchVal: val, searchRes: suggestionsList});
                return getResSuggestionsList(suggestionsList);

            } else {
                // console.error(response);
                fireValueChange({valid: false, message: response.feedback});
            }
        });
    };

    const {options, loading}= useAsyncOptions(getSuggestions, useDebounced(value, SUGGEST_DEBOUNCE_MS));

    // only called on an actual pick from the list; free typing goes through fireValueChange below
    const onOptionSelect = (ev, selectedSugg) => {
        if (!selectedSugg || typeof selectedSugg === 'string') return; // cleared, or free-solo text
        naifNameRef.current = selectedSugg.name; // NaifidPanel treats text matching this as valid
        selectedNameRef.current = selectedSugg.name;
        fireValueChange({
            feedback: `Object Name: <b>${selectedSugg.name}</b>, NAIF ID: <b>${selectedSugg.naifid}</b>`,
            valid : true,
            message: '',
            displayValue: selectedSugg.name,
            value: selectedSugg.name + ';' + selectedSugg.naifid, //this is the returned value from the component.
        });
    };

    return (
        <div>
            <AutoCompleteInputView
                orientation='vertical'
                slotProps={{control:{sx:{width:200}}, listbox:{style:popStyle}}}
                label = {label}
                valid={valid}
                message={message}
                value={value}
                options={options}
                loading={loading}
                filterOptions={noClientFilter}
                onChange={onOptionSelect}
                renderOption={renderNaifOption}
                fireValueChange={({message='', valid=true, value}) => {
                    selectedNameRef.current= undefined; // typed or cleared, so re-typing a picked name looks up again
                    fireValueChange({message, valid, displayValue: value, showHelp: value === ''});
                }}/>
            <TargetFeedback {...{showHelp, feedback, examples}} style={feedbackStyle}/>
        </div>);
}


NaifidPanelView.propTypes = {
    label : PropTypes.string,
    valid   : PropTypes.bool.isRequired,
    showHelp   : PropTypes.bool.isRequired,
    feedback: PropTypes.string.isRequired,
    message: PropTypes.string.isRequired,
    examples: PropTypes.object,
    value : PropTypes.string.isRequired,
    labelWidth : PropTypes.number,
    popStyle : PropTypes.object, //style for the suggestion box popup list
    onUnmountCB : PropTypes.func,
    feedbackStyle: PropTypes.object,
    fireValueChange: PropTypes.func,
    naifNameRef: PropTypes.object,
    naifIdFormat: PropTypes.string
};


export const NaifidPanel= memo( (props) => {
    const {fieldKey='NaifId'} = props;
    const {viewProps, fireValueChange} = useFieldGroupConnector({fieldKey, ...props});

    // ref instead of state so that we can mutate it without causing re-renders
    const naifNameRef = useRef(props.value ?? '');

    const handleValueChange = (payload, fireValueChange) => {
        const newPayload = {...payload};
        newPayload.valid  = Boolean(newPayload.value) || (newPayload.displayValue === '' || newPayload.displayValue === naifNameRef.current);
        if (!newPayload.valid) {
            if (!newPayload.message) {
                newPayload.message = 'Please use name from the list';
            }
            if (naifNameRef.current) {
                newPayload.value = '';
                newPayload.feedback = '';
            }
        }
        fireValueChange(newPayload);
    };

    const newProps =
        {
            ...viewProps,
            visible: true,
            label: viewProps.label || LABEL_DEFAULT,
            tooltip: 'Enter a target',
            value: viewProps.displayValue,
            feedback: viewProps.feedback || '',
            showHelp: viewProps?.showHelp ?? true,
            fireValueChange: (payload) => handleValueChange(payload, fireValueChange),
            naifNameRef
        };
    return <NaifidPanelView {...newProps} />;
});
