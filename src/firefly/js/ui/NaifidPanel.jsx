/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useRef} from 'react';
import PropTypes from 'prop-types';
import {sortBy} from 'lodash';
import {resolveNaifidObj} from  './NaifidPanelWorker.js';
import {SuggestBoxInputFieldView} from './SuggestBoxInputField';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {TargetFeedback} from './TargetFeedback';


const LABEL_DEFAULT = 'Moving Target Name:';
const DEFAULT_FORMAT = 'default';
const searchHistory = {[DEFAULT_FORMAT]: []}; // defining as global to persist it throughout the lifetime of app


function NaifidPanelView({showHelp, valid, message, examples, feedback, value, labelWidth, feedbackStyle, popStyle,
                             label= LABEL_DEFAULT, fireValueChange, updateNaifNameValue,
                             naifIdFormat=DEFAULT_FORMAT}){
    const getSuggestions = (val= '') => {
        if (!val) return [];
        if (naifIdFormat && !searchHistory[naifIdFormat]) searchHistory[naifIdFormat] = [];

        const getResSuggestionsList = (suggestionsList) => {
            const resSuggestionsList = Object.values(suggestionsList).map((v) => ({name: v.naifName, naifid: v.naifId}));
            return sortBy(resSuggestionsList, 'naifid').reverse();
        };

        //if value has been searched previously, no need to request from server
        if (searchHistory[naifIdFormat].length > 0){
            const cachedSuggList = Object.values(searchHistory[naifIdFormat]).find((v) => (v.searchVal === val));
            if (cachedSuggList?.searchRes) return getResSuggestionsList(cachedSuggList.searchRes);
        }

        //else request naif IDs from the server
        const rval = resolveNaifidObj(val, naifIdFormat);
        if (!rval.p) return [];
        return rval.p.then((response)=>{
            if (response.valid) {
                const suggestionsList = response.data.map(({naifID, name}) => ({naifId: naifID, naifName: name}));
                searchHistory[naifIdFormat].push({searchVal: val, searchRes: suggestionsList});
                return getResSuggestionsList(suggestionsList);

            } else {
                //console.error(response);
                fireValueChange({valid: false, message: response.feedback});
            }
        });
    };


    const getValueOnSuggestion = (val, selectedSugg) => {
        if (!selectedSugg) return;

        updateNaifNameValue(selectedSugg.name);
        fireValueChange({
            feedback: `Object Name: <b>${selectedSugg.name}</b>, NAIF ID: <b>${selectedSugg.naifid}</b>`,
            valid : true,
            displayValue: selectedSugg.name,
            value: selectedSugg.name + ';' + selectedSugg.naifid, //this is the returned value from the component.
        });

        return selectedSugg.name;
    };


    return (
        <div>
            <SuggestBoxInputFieldView
                wrapperStyle={{width:200}}
                label = {label}
                labelWidth={labelWidth}
                style={{width: 50}}
                valid={valid}
                popStyle={popStyle}
                message={message}
                value={value}
                valueOnSuggestion={getValueOnSuggestion}
                validator={() => ({valid: true, message: ''})}
                getSuggestions={getSuggestions}
                renderSuggestion={(suggestion) =>
                    (<span>Name: <b>{suggestion.name}</b>, NAIF ID: <b>{suggestion.naifid}</b></span>)}
                fireValueChange={({message, valid, value}) =>
                    fireValueChange({message, valid, displayValue: value, showHelp: value === ''})}/>
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
    updateNaifNameValue: PropTypes.func,
    naifIdFormat: PropTypes.string
};


export const NaifidPanel= memo( (props) => {
    const {fieldKey='NaifId'} = props;
    const {viewProps, fireValueChange} = useFieldGroupConnector({fieldKey, ...props});

    const naifNameValue = useRef(props.value ?? ''); // ref instead of state so that we can mutate it without causing re-renders
    const updateNaifNameValue = (newValue) => {naifNameValue.current = newValue;}; // for mutation by child component

    const handleValueChange = (payload, fireValueChange) => {
        const newPayload = {...payload};
        newPayload.valid  = Boolean(newPayload.value) || (newPayload.displayValue === '' || newPayload.displayValue === naifNameValue.current);
        if (!newPayload.valid) {
            if (!newPayload.message) {
                newPayload.message = 'Please use name from the list';
            }
            if (naifNameValue.current) {
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
            updateNaifNameValue
        };
    return <NaifidPanelView {...newProps} />;
});
