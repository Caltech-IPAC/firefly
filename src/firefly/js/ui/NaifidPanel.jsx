/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent, memo} from 'react';
import PropTypes from 'prop-types';
import {get, sortBy} from 'lodash';
import {resolveNaifidObj} from  './NaifidPanelWorker.js';
import {SuggestBoxInputFieldView} from './SuggestBoxInputField';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {TargetFeedback} from './TargetFeedback';


const LABEL_DEFAULT='Moving Target Name:';
const makeValidRet= (valid,message='') => ({valid,message});
//const defValidator= (val) => val ? makeValidRet(true): makeValidRet(false,'Naif name not found');
const defValidator= () => makeValidRet(true);
const searchHistory =[];
let naifNamevalue = '';


class NaifidPanelView extends PureComponent {
    constructor(props) {
        super(props);
        this.state = {suggestions: ''};
        this.getSuggestions = this.getSuggestions.bind(this);
        naifNamevalue = props.value;
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
    }

    getSuggestions(val= '') {
        if (val.length<1) return [];
        const rval = resolveNaifidObj(val);
        if (!rval.p) return [];
        //if value has been searched previously, no need to call the api again.
        if(searchHistory.length > 0){
            const cachedSuggList = Object.values(searchHistory).find((v)=>(v.searchVal === val));
            if(get(cachedSuggList,'searchRes')){
                const resSuggestionsList = Object.values(cachedSuggList.searchRes).map( (v) => ({name:v.naifName, naifid:v.naifId}));
                return sortBy(resSuggestionsList, 'naifid').reverse();
            }
        }
        return rval.p.then((response)=>{
            if(response.valid) {
                const suggestionsList = Object.entries(response.data).map(([k,v])=>({naifId:v, naifName:k}));
                searchHistory.push({searchVal:val, searchRes: suggestionsList});

                const resSuggestionsList = Object.values(suggestionsList).map( (v) => ({name:v.naifName, naifid:v.naifId}));
                return sortBy(resSuggestionsList, 'naifid').reverse();

            } else {
                //console.error(response);
                this.props.fireValueChange({valid: false, message: response.feedback});
            }
        });
    }

    render() {
        const {showHelp, valid, message, examples, feedback, value,
            labelWidth, feedbackStyle, popStyle, label= LABEL_DEFAULT, fireValueChange}= this.props;

        const positionField = (<SuggestBoxInputFieldView
            wrapperStyle={{width:200}}
            label = {label}
            labelWidth={labelWidth}
            style={{width: 50}}
            valid={valid}
            popStyle={popStyle}
            message={message}
            value={value}
            valueOnSuggestion={getNaifidValue((selectedSugg) => {
                updateFeedback(selectedSugg, true, fireValueChange);
            })}
            validator={defValidator}
            getSuggestions={this.getSuggestions}
            renderSuggestion={renderSuggestion}
            fireValueChange={(payload) => {
                valueChanged(payload, fireValueChange);
            }}
        />);
        const naifidFeedback = (<TargetFeedback {...{showHelp, feedback, examples}} style={feedbackStyle}/>);
        return (
            <div>
                <div>{positionField}</div>
                <div>{naifidFeedback}</div>
            </div>
        );
    }
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
    fireValueChange: PropTypes.func
};

function valueChanged(payload,fireValueChange){
    let showHelp = false;
    if(payload.value ==='') showHelp = true;
    fireValueChange({message:payload.message, valid:payload.valid, displayValue:payload.value, showHelp});
}

function updateFeedback(naifObj, valid, fireValueChange){
    const payload={
        feedback: `Object Name: <b>${naifObj.name}</b>, NAIF ID: <b>${naifObj.naifid}</b>`,
        valid,
        displayValue: naifObj.name,
        value: naifObj.name+';'+naifObj.naifid,//this is the returned value from the component.
    };
    naifNamevalue = naifObj.name;
    fireValueChange(payload);
}


/**
 * Parses the selected option, and returns the value which gets populated in the input field of the suggestion box
 * @param onCallBack
 * @returns {function(*, *=)}
 */
function getNaifidValue(onCallBack) {
    return (val, obj) => {
        if (! obj) return;
        onCallBack(obj);
        return obj.name;
    };
}


/**
 * renders suggestion list's popup
 * @param naifObj
 * @returns {*}
 */
function renderSuggestion(naifObj){
    return  <span>Name:<b>{naifObj.name}</b>, NAIF ID: <b>{naifObj.naifid}</b></span>;
}


function handleValueChange(payload, fireValueChange) {
    const newPayload= {...payload};
    newPayload.valid  = Boolean(newPayload.value) || (newPayload.displayValue === '' || newPayload.displayValue === naifNamevalue);
    if (!newPayload.valid) {
        if (!newPayload.message) {
            newPayload.message = 'Please use name from the list';
        }
        if (naifNamevalue) {
            newPayload.value = '';
            newPayload.feedback = '';
        }
    }
    fireValueChange(newPayload) ;
}

export const NaifidPanel= memo( (props) => {

    const {fieldKey= 'NaifId'}= props;
    const {viewProps, fireValueChange}=  useFieldGroupConnector({fieldKey, ...props});
    const newProps=
        {
            ...viewProps,
            visible: true,
            label: viewProps.label || LABEL_DEFAULT,
            tooltip: 'Enter a target',
            value: viewProps.displayValue,
            feedback: viewProps.feedback|| '',
            showHelp: get(viewProps,'showHelp', true),
            fireValueChange: (payload) => handleValueChange(payload,fireValueChange)
        };
    return <NaifidPanelView {...newProps} /> ;
});
