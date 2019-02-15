/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {parseTarget, getFeedback, formatPosForTextField} from './TargetPanelWorker.js';
import {resolveNaifidObj} from  './NaifidPanelWorker.js';
import {TargetFeedback} from './TargetFeedback.jsx';
import {InputFieldView} from './InputFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import {ListBoxInputFieldView} from './ListBoxInputField.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {dispatchActiveTarget, getActiveTarget} from '../core/AppDataCntlr.js';
import {isValidPoint, parseWorldPt} from '../visualize/Point.js';
import {ValidationField} from "./ValidationField";
import {SuggestBoxInputField} from "./SuggestBoxInputField";
import {flux} from 'firefly/Firefly.js';
import {gkey} from "../visualize/ui/CatalogSelectViewPanel";

const TARGET= 'targetSource';
const RESOLVER= 'resolverSource';
const LABEL_DEFAULT='Moving Target Name:';

const nedThenSimbad= 'nedthensimbad';
const simbadThenNed= 'simbadthenned';
const naifidResolver = 'https://ssd.jpl.nasa.gov/x/smb_spk.cgi?OPTION=Look+up&OBJECT=';

class NaifidPanelView extends PureComponent {

    constructor(props) {
        super(props);
        this.state = {suggestions: '',fld: FieldGroupUtils.getGroupFields(this.props.groupKey)};
        this.getSuggestions = this.getSuggestions.bind(this);
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.unbinder = FieldGroupUtils.bindToStore(this.props.groupKey, (fields) => {
            if (fields !== this.state && this.iAmMounted) {
                this.setState({fields});
            }
        });
    }


    getSuggestions(val) {
        //let getSugg = new Promise((resolve, reject)=>{
            const rval = resolveNaifidObj(val);
            if (!rval.p) return undefined;
            return rval.p.then((response)=>{
                if(response.valid) {
                    let suggestionsList = response.data;
                    console.log(suggestionsList);
                    //resolve(JSON.parse(response.data));

                    if(this.iAmMounted && this.state.suggestions !== suggestionsList) {
                        this.setState({suggestions: suggestionsList});
                    }else if(this.state.suggestions === suggestionsList){
                        this.setState({suggestions:''});
                    }

                    return Object.keys(suggestionsList).map( (k) => `${k}, id: ${suggestionsList[k]}`);

                }else {
                    //reject('Error: ' + response.feedback);
                   console.error("Error: "+response.feedback);
                }
            });
        //});
    }


    render() {
        const {showHelp, feedback, valid, message, onChange, value,
            labelWidth, children, resolver, feedbackStyle, label= LABEL_DEFAULT}= this.props;
        //const {suggestions} = this.state;

        let positionField = (<SuggestBoxInputField
            label = {label}
            labelWidth={labelWidth}
            message={message}
            value={value}
            getSuggestions={this.getSuggestions}

        />);

        return (
            <div>
                <div style= {{display: 'flex'}}>
                    {positionField}
                </div>
            </div>
        );
    }

}


NaifidPanelView.propTypes = {
    fieldKey : PropTypes.string,
    groupKey : PropTypes.string,
    label : PropTypes.string,
    valid   : PropTypes.bool.isRequired,
    showHelp   : PropTypes.bool.isRequired,
    feedback: PropTypes.string.isRequired,
    resolver: PropTypes.string.isRequired,
    message: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value : PropTypes.string.isRequired,
    labelWidth : PropTypes.number,
    onUnmountCB : PropTypes.func,
    feedbackStyle: PropTypes.object
};



/*function getSuggestions(value,obj){

    const {showHelp, valid, feedback, p} = resolveNaifidObj(value);

    /*if (p) {
        p.then( ({showHelp, valid, feedback, results}) => {
           console.log(JSON.stringify(results.data));
           console.log(JSON.parse(results.data));
           //we need to setState to re-render the component
            let suggestList= (JSON.parse(results.data)).naifids;

            return suggestList;//.filter((e)=>e.startsWith(value));
        });
    }else {
        let suggestList = ['Test suggestion'];
        //let promies = Promise.resolve(resolveNaifidObj(value));
        //promies.then(function(val){
        //console.log(val);
        //}).catch(err => console.log(err));
        if(value)
          return suggestList;//.filter((e) => e.startsWith(value));
    }*/
    /*let promies = Promise.resolve(p)
        .then(function(val){
            console.log(val);
            obj.setState({suggestions: val});
            return val;
        })
        .catch(err=>console.log(err));
}*/



function didUnmount(fieldKey,groupKey, props) {
    const wp= parseWorldPt(FieldGroupUtils.getFldValue(FieldGroupUtils.getGroupFields(groupKey),fieldKey));

    if (props.nullAllowed && !wp) {
        dispatchActiveTarget(null);
    }
    else if (isValidPoint(wp)) {
        dispatchActiveTarget(wp);
    }
}



function getProps(params, fireValueChange) {

    var feedback= params.feedback|| '';
    var value= params.displayValue;
    var resolver= params.resolver || nedThenSimbad;
    var showHelp= get(params,'showHelp', true);
    const wpStr= params.value;
    const wp= parseWorldPt(wpStr);

    if (isValidPoint(wp) && !value) {
        feedback= getFeedback(wp);
        value= wp.objName || formatPosForTextField(wp);
        showHelp= false;
    }

    return Object.assign({}, params,
        {
            visible: true,
            onChange: (value,source) => handleOnChange(value,source,params, fireValueChange),
            label: params.label || LABEL_DEFAULT,
            tooltip: 'Enter a target',
            value,
            feedback,
            resolver,
            showHelp,
            nullAllowed:params.nullAllowed,
            onUnmountCB: didUnmount
        });
}



const connectorDefaultProps = {
    fieldKey : 'UserTargetWorldPt',
    initialState  : {
        fieldKey : 'UserTargetWorldPt'
    }
};

function replaceValue(v,props) {
    const t= getActiveTarget();
    // if (props.nullAllowed && !v) return v;
    let retVal= v;
    if (t && t.worldPt) {
        if (get(t,'worldPt')) retVal= t.worldPt.toString();
    }
    return retVal;
}


export const NaifidPanel= fieldGroupConnector(NaifidPanelView,getProps,null,connectorDefaultProps, replaceValue);




















/*****************Backup*********/
/*

class NaifidPanelView extends PureComponent {

    componentWillUnmount() {
        const {onUnmountCB, fieldKey, groupKey}= this.props;
        if (onUnmountCB) onUnmountCB(fieldKey,groupKey, this.props);
    }

    render() {
        const {showHelp, feedback, valid, message, onChange, value,
            labelWidth, children, resolver, feedbackStyle, label= LABEL_DEFAULT}= this.props;
        let positionField = (<InputFieldView
            valid={valid}
            visible= {true}
            message={message}
            onChange={(ev) => onChange(ev.target.value, TARGET)}
            label={label}
            value={value}
            tooltip='Enter a target'
            labelWidth={labelWidth}
        />);
        //positionField = children ? (<div style={{display: 'flex'}}>{positionField} {children}</div>) : positionField;

        let positionField2 = (<SuggestBoxInputField
            label = {label}
            labelWidth={labelWidth}
            getSuggestions={getSuggestions}
        />);

        return (
            <div>
                <div style= {{display: 'flex'}}>

                    {positionField2}
                </div>
                <TargetFeedback {...{showHelp, feedback, style:feedbackStyle}}/>
</div>
);
}
}


NaifidPanelView.propTypes = {
    fieldKey : PropTypes.string,
    groupKey : PropTypes.string,
    label : PropTypes.string,
    valid   : PropTypes.bool.isRequired,
    showHelp   : PropTypes.bool.isRequired,
    feedback: PropTypes.string.isRequired,
    resolver: PropTypes.string.isRequired,
    message: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value : PropTypes.string.isRequired,
    labelWidth : PropTypes.number,
    onUnmountCB : PropTypes.func,
    feedbackStyle: PropTypes.object
};



const getSuggestions = (value)=>{
    let suggestList=['1','2','3'];
    if(suggestList.includes((value)))
        return suggestList;
    else
        return;
}


function didUnmount(fieldKey,groupKey, props) {
    const wp= parseWorldPt(FieldGroupUtils.getFldValue(FieldGroupUtils.getGroupFields(groupKey),fieldKey));

    if (props.nullAllowed && !wp) {
        dispatchActiveTarget(null);
    }
    else if (isValidPoint(wp)) {
        dispatchActiveTarget(wp);
    }
}



function getProps(params, fireValueChange) {

    var feedback= params.feedback|| '';
    var value= params.displayValue;
    var resolver= params.resolver || nedThenSimbad;
    var showHelp= get(params,'showHelp', true);
    const wpStr= params.value;
    const wp= parseWorldPt(wpStr);

    if (isValidPoint(wp) && !value) {
        feedback= getFeedback(wp);
        value= wp.objName || formatPosForTextField(wp);
        showHelp= false;
    }

    return Object.assign({}, params,
        {
            visible: true,
            onChange: (value,source) => handleOnChange(value,source,params, fireValueChange),
            label: params.label || LABEL_DEFAULT,
            tooltip: 'Enter a target',
            value,
            feedback,
            resolver,
            showHelp,
            nullAllowed:params.nullAllowed,
            onUnmountCB: didUnmount
        });
}




function handleOnChange(value, source, params, fireValueChange) {
    let {parseResults={}}= params;

    let displayValue;
    let resolver;

    if (source===TARGET) {
        resolver= params.resolver || nedThenSimbad;
        displayValue= value;
    }
    else if (source===RESOLVER) {
        resolver= value;
        displayValue= params.displayValue || '';
    }
    else {
        console.error('should never be here');
    }

    parseResults= parseTarget(displayValue, parseResults, resolver);
    let {resolvePromise}= parseResults;

    const targetResolve= (asyncParseResults) => {
        return asyncParseResults ? makePayloadAndUpdateActive(displayValue, asyncParseResults, null, resolver) : null;
    };

    if (!displayValue && params.nullAllowed) {
        parseResults.valid= true;
        parseResults.feedback= 'valid: true';
    }



    resolvePromise= resolvePromise ? resolvePromise.then(targetResolve) : null;

    fireValueChange(makePayloadAndUpdateActive(displayValue,parseResults, resolvePromise, resolver));

}

**
 * make a payload and update the active target
 * Note- this function has as side effect to fires an action to update the active target
 * @param displayValue
 * @param parseResults
 * @param resolvePromise
 * @param {string} resolver the key to specify the resolver
 * @return {{message: string, displayValue: *, wpt: (*|null), value: null, valid: *, showHelp: (*|boolean), feedback: (string|*|string), parseResults: *}}
 *
function makePayloadAndUpdateActive(displayValue, parseResults, resolvePromise, resolver) {
    const {wpt}= parseResults;
    const wpStr= parseResults && wpt ? wpt.toString() : null;

    const payload= {
        message : parseResults.parseError || 'Could not resolve object: Enter valid object',
        displayValue,
        wpt,
        value : resolvePromise ? resolvePromise  : wpStr,
        valid : parseResults.valid,
        showHelp : parseResults.showHelp,
        feedback : parseResults.feedback,
        parseResults
    };
    if (resolver) payload.resolver= resolver;
    return payload;
}
/*
const connectorDefaultProps = {
    fieldKey : 'UserTargetWorldPt',
    initialState  : {
        fieldKey : 'UserTargetWorldPt'
    }
};
*/
/*
function replaceValue(v,props) {
    const t= getActiveTarget();
    // if (props.nullAllowed && !v) return v;
    let retVal= v;
    if (t && t.worldPt) {
        if (get(t,'worldPt')) retVal= t.worldPt.toString();
    }
    return retVal;
}


export const NaifidPanel= fieldGroupConnector(NaifidPanelView,getProps,null,connectorDefaultProps, replaceValue);

 */