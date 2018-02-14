/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent, memo} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {resolveNaifidObj} from  './NaifidPanelWorker.js';
import {SuggestBoxInputFieldView} from './SuggestBoxInputField';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {TargetFeedback} from './TargetFeedback';


const LABEL_DEFAULT='Moving Target Name:';

const makeValidRet= (valid,message='') => ({valid,message});

class NaifidPanelView extends PureComponent {

    constructor(props) {
        super(props);
        this.state = {suggestions: ''};
        this.getSuggestions = this.getSuggestions.bind(this);
        this.activeValidator= () => makeValidRet(true);
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
    }


    getSuggestions(val) {
            const rval = resolveNaifidObj(val);
            if (!rval.p) return undefined;
            this.activeValidator= (val) => {
                return val ? makeValidRet(false,'Naif name not found') : makeValidRet(true);
            };
            return rval.p.then((response)=>{
                //const [result] = response;
                if(response.valid) {
                    let suggestionsList = response.data || [];

                    if(this.iAmMounted && this.state.suggestions !== suggestionsList) {
                        this.setState({suggestions: suggestionsList});
                    }else if(this.iAmMounted && this.state.suggestions === suggestionsList){
                        this.setState({suggestions:''});
                    }

                    this.activeValidator= (val= '') => {
                        if (Object.keys(suggestionsList).find((k) => k.toUpperCase()===val.toUpperCase())) {
                            return makeValidRet(true);
                        }
                        else {
                            return makeValidRet(false,'Choose naif name from list');
                        }
                    };

                    return Object.keys(suggestionsList).map( (k) => `Object Name:${k}, NAIF ID:${suggestionsList[k]}`);

                }else {
                   //console.error("Error: "+response.feedback);
                }
            });
    }


    render() {
        const {showHelp, valid, message, feedback, value,
            labelWidth, feedbackStyle, popStyle, label= LABEL_DEFAULT, fireValueChange}= this.props;


        const validator=  (val) => {
            return this.activeValidator(val);
        };


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
            getSuggestions={this.getSuggestions}
            renderSuggestion={renderSuggestion}
            fireValueChange={(payload) => {
                fireValueChange({message:payload.message, valid:payload.valid, displayValue:payload.value});
            }}
            validator={validator}
        />);
        const naifidFeedback = (<TargetFeedback {...{feedback}} {...{feedbackStyle}}/>);

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
    value : PropTypes.string.isRequired,
    labelWidth : PropTypes.number,
    popStyle : PropTypes.object, //style for the suggestion box popup list
    onUnmountCB : PropTypes.func,
    feedbackStyle: PropTypes.object,
    fireValueChange: PropTypes.func
};



function updateFeedback(resolvedStr, valid, fireValueChange){
   const {objName, naifId}=  parseNaifPair(resolvedStr);
   const payload={
       feedback: resolvedStr,
       valid,
       displayValue: objName,
       value: naifId,
   };
   fireValueChange(payload);
}

/** Parses the selected option, and returns the value which gets populated in the input field of the suggestion box*/
function getNaifidValue(onCallBack) {
    return (val, str) => {
        if (! str) return;
        const result= parseNaifPair(str);
        onCallBack(str);
        return result.objName;
    };
}

function parseNaifPair(str) {
    const [,objName=''] = str.match(/Object Name:(.*),/) || [];
    const [,naifId=''] = str.match(/NAIF ID:(.*)/) || [];
    return {objName,naifId};
}


/** renders suggestion list's popup */
function renderSuggestion(str){
    const {objName, naifId}=  parseNaifPair(str);
    return  <span>Name:<b>{objName}</b>, NAIF ID: <b>{naifId}</b></span>;
}


function handleValueChange(payload, fireValueChange) {
    const newPayload= {...payload};
    if (!payload.valid) {
        newPayload.value= '';
        newPayload.feedback= '';
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
