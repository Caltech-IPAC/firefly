/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {getFeedback, formatPosForTextField} from './TargetPanelWorker.js';
import {resolveNaifidObj} from  './NaifidPanelWorker.js';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {dispatchActiveTarget, getActiveTarget} from '../core/AppDataCntlr.js';
import {isValidPoint, parseWorldPt} from '../visualize/Point.js';
import {SuggestBoxInputField} from './SuggestBoxInputField';
import {flux} from 'firefly/Firefly.js';
import {TargetFeedback} from './TargetFeedback';
import {dispatchValueChange} from '../fieldGroup/FieldGroupCntlr';


const LABEL_DEFAULT='Moving Target Name:';


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
            const rval = resolveNaifidObj(val);
            if (!rval.p) return undefined;
            return rval.p.then((response)=>{
                if(response.valid) {
                    let suggestionsList = response.data || [];

                    if(this.iAmMounted && this.state.suggestions !== suggestionsList) {
                        this.setState({suggestions: suggestionsList});
                    }else if(this.state.suggestions === suggestionsList){
                        this.setState({suggestions:''});
                    }

                    return Object.keys(suggestionsList).map( (k) => `Name:${k}, Naifid:${suggestionsList[k]}`);

                }else {
                   console.error("Error: "+response.feedback);
                }
            });
    }


    render() {
        const {fieldKey, groupKey, showHelp, valid, message, feedback, value,
            labelWidth, feedbackStyle, popStyle, label= LABEL_DEFAULT}= this.props;

        let positionField = (<SuggestBoxInputField
            fieldKey = {fieldKey}
            wrapperStyle={{width:200}}
            label = {label}
            labelWidth={labelWidth}
            style={{width: 50}}
            popStyle={popStyle}
            message={message}
            value={value}
            valueOnSuggestion={getNaifidValue((selectedSugg) => updateFeedback(selectedSugg, fieldKey, groupKey, valid))}
            getSuggestions={this.getSuggestions}
            renderSuggestion={renderSuggestion}
        />);
        let naifidFeedback = (<TargetFeedback {...{feedback}} {...{feedbackStyle}}/>);

        return (
            <div>
                <div>{positionField}</div>
                <div>{naifidFeedback}</div>
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
    message: PropTypes.string.isRequired,
    value : PropTypes.string.isRequired,
    labelWidth : PropTypes.number,
    popStyle : PropTypes.object, //style for the suggestion box popup list
    onUnmountCB : PropTypes.func,
    feedbackStyle: PropTypes.object
};



function didUnmount(fieldKey,groupKey, props) {
    const wp= parseWorldPt(FieldGroupUtils.getFldValue(FieldGroupUtils.getGroupFields(groupKey),fieldKey));

    if (props.nullAllowed && !wp) {
        dispatchActiveTarget(null);
    }
    else if (isValidPoint(wp)) {
        dispatchActiveTarget(wp);
    }
}



function getProps(params) {
    let feedback= params.feedback|| '';
    let value= params.displayValue;
    let showHelp= get(params,'showHelp', true);
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
            label: params.label || LABEL_DEFAULT,
            tooltip: 'Enter a target',
            value,
            feedback,
            showHelp,
            nullAllowed:params.nullAllowed,
            onUnmountCB: didUnmount
        });
}


function updateFeedback(value, fieldKey, groupKey, valid){
   const payload={
       fieldKey: fieldKey,
       groupKey: groupKey,
       feedback: value,
       valid: valid
   };
   dispatchValueChange(payload);
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

function getNaifidValue(onCallBack) {
    return (val, str) => {
        if (! str) return;

        const naifId = (str.match(/Naifid:.*/))[0].split('Naifid:').pop();
        onCallBack(str);
        return naifId;
    }
}

function renderSuggestion(str){
    let name = str.split(',')[0].split('Name:');
    let naifid = str.split(',')[1].split('Naifid:');

    return  <span>Name:<b>{name}</b>, Naifid: <b>{naifid}</b></span>;
}

export const NaifidPanel= fieldGroupConnector(NaifidPanelView,getProps,null,connectorDefaultProps, replaceValue);

