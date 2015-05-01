/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * User: roby
 * Date: Apr 2, 2009
 * Time: 9:18:47 AM
 */
'use strict';

import Enum from 'enum';
import Point from 'ipac-firefly/visualize/Point.js';
import WebPlotRequest from 'ipac-firefly/visualize/WebPlotRequest.js';
import BackgroundState from './BackgroundState.js';
import PackageProgress from './PackageProgress.js';
import ServerRequest from 'ipac-firefly/data/ServerRequest.js';
import ServerRequest from 'ipac-firefly/visualize/WebPlotRequest.js';
import replaceAll from 'underscore.string/replaceAll';
import words from 'underscore.string/words';
import validator from 'validator';

export const BgType = new Enum(['SEARCH', 'PACKAGE', 'UNKNOWN', 'PERSISTENT']);
export const PushType = new Enum(['WEB_PLOT_REQUEST', 'REGION_FILE_NAME', 'TABLE_FILE_NAME', 'FITS_COMMAND_EXT']);

const PARAM_SEP = '<<BGSEP>>';
const URL_SUB = 'URL_PARAM_SEP';
const KW_VAL_SEP = '==>>';
const NO_ID = 'WARNING:_UNKNOWN_PACKAGE_ID';

const Keys= {
    TYPE : 'TYPE',
    ID : 'ID',
    ATTRIBUTE : 'ATTRIBUTES',
    STATE : 'STATE',
    DATA_SOURCE : 'DATA_SOURCE',
    MESSAGE_BASE : 'MESSAGE_',
    MESSAGE_CNT : 'MESSAGE_CNT',
    PACKAGE_PROGRESS_BASE : 'PACKAGE_PROGRESS_',
    PACKAGE_CNT : 'PACKAGE_CNT',
    CLIENT_REQ : 'CLIENT_REQ',
    SERVER_REQ : 'SERVER_REQ',
    WEB_PLOT_REQ : 'WEB_PLOT_REQ',
    FILE_PATH : 'FILE_PATH',
    TOTAL_BYTES : 'TOTAL_BYTES',
    PUSH_DATA_BASE : 'PUSH_DATA_#',
    PUSH_TYPE_BASE : 'PUSH_TYPE_#',
    PUSH_CNT :       'PUSH_CNT',
    USER_RESPONSE :  'USER_RESPONSE_#',
    USER_DESC     :  'USER_DESC_#',
    RESPONSE_CNT :   'RESPONSE_CNT',
    ACTIVE_REQUEST_CNT :'ACTIVE_REQUEST_CNT'
};


class BackgroundStatus {

    constructor(id, state, type, inParams) {
        this.params= {};
        if (inParams) this.setParams(inParams);
        if (!type) {
            if (!this.getBackgroundType()) this.setBackgroundType(BgType.UNKNOWN);
        }
        else {
            this.setBackgroundType(type);
        }
        if (!state) {
            if (!this.getState()) this.setState(BackgroundState.STARTING);
        }
        else {
            this.setState(state);
        }
        if (!id) {
            if (!this.getID()) this.setID(NO_ID);
        }
        else {
            this.setID(id);
        }
    }


    // --------Keys -------------------
    // --------End Keys -------------------


//====================================================================
//====================================================================
//====================================================================

    static cloneWithState(state, copyFromStatus) {
        var retval= new BackgroundStatus();
        if (copyFromStatus) retval.copyFrom(copyFromStatus);
        retval.setState(state);
        return retval;
    }



    containsParam(paramKey) {
        return (this.params.hasOwnProperty(paramKey));
    }

    getParam(paramKey) {
        return this.params.hasOwnProperty(paramKey) ? this.params[paramKey] : null;
    }


    setPara(name, values) {
        if (values) {
            if (Array.isArray(values) && values.length) {
                this.params[name]= values.join(',');
            }
            else {
                this.params[name]= values;
            }
        }
    }


    //setParam(Param param) {
    //    if (param!=null) params.put(param.getName(), param.getValue());
    //}
    //
    //public void setParams(...params) {
    //    for(Param p : params) {
    //        setParam(p);
    //    }
    //}


    //public void setParam(String name, WorldPt wpt) {
    //    setParam(name,wpt==null ? null : wpt.toString());
    //}


    setSafeParam(name,val) {
        var newVal= null;
        if (val) newVal= replaceAll(val,PARAM_SEP,URL_SUB);
        this.params[name]= newVal;
    }

    getSafeParam(name) {
        var val= this.getParam(name);
        var newVal= null;
        if (val!=null) newVal= replaceAll(val,URL_SUB,PARAM_SEP);
        return newVal;
    }


    removeParam(name) { delete this.params[name]; }

//====================================================================
//====================================================================


    /**
     * @return {BgType} return the background type
     */
    getBackgroundType() {
        return BgType.get(Keys.TYPE) || BgType.UNKNOWN;
    }

    /**
     * @param {BgType} background Type
     */
    setBackgroundType(type) {
        this.params[Keys.TYPE]= type.toString();
    }


    /**
     * @param {JobAttributes} a job atribute
     */
    hasAttribute(a) {
        var attributes = this.getParam(Keys.ATTRIBUTE);
        return (attributes && attributes.includes(a.toString()));
    }


    /**
     * @param {JobAttributes} a job attribute
     */
    addAttribute(a) {
        var attributes= this.getParam(Keys.ATTRIBUTE);
        if (!attributes) {
            attributes= a.toString();
        }
        else if (!attributes.includes(a.toString())) {
            attributes+= '|'+a.toString();
        }
        this.params[Keys.ATTRIBUTE]= attributes;
    }

    setState(state) {
        this.params[Keys.STATE]= state.toString();
    }

    getState() {
        return BackgroundState.get(this.getParam(Keys.STATE)) || BackgroundState.STARTING;
    }

    setDataSource(dataSource) {
        this.params[Keys.DATA_SOURCE]= dataSource;
    }

    getDataSource() { this.getParam(Keys.DATA_SOURCE); }

    setID(id) { this.params[Keys.ID]=id; }
    getID() { return this.getParam(Keys.ID); }


    /**
     * @return {boolean}
     */
    isDone() {
        var state= this.getState();
        return (state===BackgroundState.USER_ABORTED ||
                state===BackgroundState.CANCELED ||
                state===BackgroundState.FAIL ||
                state===BackgroundState.SUCCESS ||
                state===BackgroundState.UNKNOWN_PACKAGE_ID);
    }

    /**
     * @return {boolean}
     */
    isFail() {
        var state= this.getState();
        return (state=== BackgroundState.FAIL ||
                state=== BackgroundState.USER_ABORTED ||
                state=== BackgroundState.UNKNOWN_PACKAGE_ID ||
                state=== BackgroundState.CANCELED);
    }

    /**
     * @return {boolean}
     */
    isSuccess() {
        return (this.getState()===BackgroundState.SUCCESS);
    }

    /**
     * @return {boolean}
     */
    isActive() {
        var state= this.getState();
        return (state=== BackgroundState.WAITING ||
                state=== BackgroundState.WORKING ||
                state=== BackgroundState.STARTING);
    }

    //------------------------------------------
    //---- Request browser load data -----------
    //------------------------------------------

    /**
     * @return {number}
     */
    getNumPushData() {
        return this.getIntParam(Keys.PUSH_CNT,0);
    }

    /**
     *
     * @param {string} serializeData
     * @param {PushType} pushType
     */
    addPushData(serializeData, pushType) {
        var total= this.getNumPushData();
        this.setParam(Keys.PUSH_DATA_BASE +total,serializeData);
        this.setParam(Keys.PUSH_TYPE_BASE +total,pushType.toString());
        total++;
        this.setParam(Keys.PUSH_CNT,total+'');
    }

    /**
     *
     * @param {number} idx
     * @return {string}
     */
    getPushData(idx) {
        return this.getParam(Keys.PUSH_DATA_BASE +idx);
    }

    /**
     *
     * @param {number} idx
     * @return {PushType}
     */
    getPushType(idx) {
        return BackgroundState.get(this.getParam(Keys.PUSH_TYPE_BASE + idx)) || PushType.WEB_PLOT_REQUEST;
    }

    //------------------------------------------
    //---- Contains responses from User --------
    //------------------------------------------

    getNumResponseData() {
        return this.getIntParam(Keys.RESPONSE_CNT,0);
    }

    addResponseData(desc, data) {
        var total= this.getNumResponseData();
        this.setParam(Keys.USER_RESPONSE +total,data);
        this.setParam(Keys.USER_DESC +total,desc);
        total++;
        this.setParam(Keys.RESPONSE_CNT,total+'');
    }


    getResponseData(idx) {
        return this.getParam(Keys.USER_RESPONSE +idx);
    }

    getResponseDesc(idx) {
        return this.getParam(Keys.USER_DESC +idx);
    }

    getRequestedCnt() {
        return this.getIntParam(Keys.ACTIVE_REQUEST_CNT,0);
    }

    incRequestCnt() {
        var cnt= this.getRequestedCnt();
        cnt++;
        this.setParam(Keys.ACTIVE_REQUEST_CNT,cnt+'');
    }

    decRequestCnt() {
        var cnt= this.getRequestedCnt();
        if (cnt>0) {
            cnt--;
            this.setParam(Keys.ACTIVE_REQUEST_CNT,cnt+'');
        }
    }

    //------------------------------------------
    //------------------------------------------
    //------------------------------------------

    addMessage(message) {
        var total= this.getNumMessages();
        this.setParam(Keys.MESSAGE_BASE +total,message);
        total++;
        this.setParam(Keys.MESSAGE_CNT,total+'');
    }

    getNumMessages() {
        return this.getIntParam(Keys.MESSAGE_CNT,0);
    }

    getMessage(idx) {
        return this.getParam(Keys.MESSAGE_BASE +idx);
    }

    getMessageList() {
        var cnt= this.getNumMessages();
        var list= [];
        if (cnt) {
            for(var i= 0; (i<cnt); i++) {
                var m= this.getMessage(i);
                if (m) list.push(m);
            }
        }
        return list;
    }
    //------------------------------------------
    //------------------------------------------
    //------------------------------------------


    /**
     *
     * @param int
     * @return {PackageProgress}
     */
    getPartProgress(i) {
        var s= this.getParam(Keys.PACKAGE_PROGRESS_BASE+i);
        var retval= PackageProgress.parse(s);
        if (!retval) retval= new PackageProgress();
        return retval;
    }

    /**
     *
     * @return {Array} an array of PackageProgress
     */
    getPartProgressList() {
        var cnt= this.getPackageCount();
        var list= [];
        var pp;
        if (cnt) {
            for(var i= 0; (i<cnt); i++) {
                pp= this.getPartProgress(i);
                if (pp) list.push(pp);
            }
        }
        return list;
    }


    isMultiPart() {
        return this.getPackageCount()>1;
    }

    getPackageCount() {
        return this.getIntParam(Keys.PACKAGE_CNT,0);
    }

    setPackageCount(cnt) { this.setParam(Keys.PACKAGE_CNT,cnt+''); }

    /**
     *
     * @param {PackageProgress} progress
     */
    addPackageProgress(progress) {
        var total= this.getPackageCount();
        this.setPartProgress(progress,total);
        total++;
        this.setParam(Keys.PACKAGE_CNT,total+'');
    }

    /**
     *  set a specific PackageProgress item
     * @param {PackageProgress} progress
     * @param i
     */
    setPartProgress(progress, i) {
        this.setParam(Keys.PACKAGE_PROGRESS_BASE+i,progress.serialize());
    }


    /**
     *
     * @return {Request} request
     */
    getClientRequest() {
        return Request.parse(this.getParam(Keys.CLIENT_REQ));
    }

    /**
     *
     * @param {Request} request
     */
    setClientRequest(request) {
        if (request) this.setParam(Keys.CLIENT_REQ,request.toString());
    }

    /**
     *
     * @return {ServerRequest}
     */
    getServerRequest() {
        return ServerRequest.parse(this.getParam(Keys.SERVER_REQ));
    }

    /**
     *
     * @param {ServerRequest} request
     */
    setServerRequest(request) {
        if (request) this.setParam(Keys.SERVER_REQ,request.toString());
    }

    /**
     *
     * @param {WebPlotRequest} wpr
     */
    setWebPlotRequest(wpr) {
        if (wpr) this.setParam(Keys.WEB_PLOT_REQ,wpr.toString());
    }

    /**
     *
     * @return {WebPlotRequest} wpr
     */
    getWebPlotRequest() {
        return WebPlotRequest.parse(this.getParam(Keys.WEB_PLOT_REQ));
    }

    /**
     *
     * @return {BackgroundStatus }
     */
    createUnknownFailStat() {
        return new BackgroundStatus(NO_ID, BgType.UNKNOWN, BackgroundState.FAIL);
    }


    setFilePath(filePath) { this.setParam(Keys.FILE_PATH,filePath); }

    getFilePath() { return this.getParam(Keys.FILE_PATH); }

    /**
     * @return {number} processed bytes if all bundles were processed successfully, otherwise previously estimated size in bytes
     */
    getTotalSizeInBytes() {
        var actualProcessSize = 0;
        if (this.getState()===BackgroundState.SUCCESS && this.getPackageCount()>0) {
            actualProcessSize= this.getPartProgressList().reduce( (pp,total) => total+pp.getProcessedBytes() ,0);
        }
        return actualProcessSize ? actualProcessSize : this.getIntParam(Keys.TOTAL_BYTES,0);
    }

//====================================================================
//====================================================================


    /**
     * copy all the params from a BackgroundStatus or an object literal
     * @param {BackgroundStatus} backgroundStatus
     */
    copyFrom(backgroundStatus) {
        Object.assign(this.params,backgroundStatus.params? backgroundStatus.params:backgroundStatus );
    }




    /**
     * Parses the string argument into a ServerRequest object.
     * This method is reciprocal to serialize().
     * @param {string } the serialized string
     * @return {BackgroundStatus}
     */
    parse(str) {
        if (!str) return null;
        const bgStat= new BackgroundStatus();
        const params = str.split(PARAM_SEP);
        if (params.length>0) {
            words(str, PARAM_SEP).forEach(p => {
                var outParam= words(p,KW_VAL_SEP);
                if (outParam.length===2) bgStat.setParam(outParam[0], outParam[1]);
            });
            return bgStat;
        }
        return null;
    }

    /**
     * Serialize this object into its string representation.
     * @return {string} the serialized string
     */
    serialize() {
        var retStr= Object.keys(this.params).map((key) => key+KW_VAL_SEP+this.params[key]).join(PARAM_SEP);
        return retStr;
    }

    toString() {
        return 'packageID: ' + this.getID() + ', state: ' + this.getState() + ', type: ' + this.getBackgroundType();
    }

    /**
     * @param {BackgroundState} newState the new state
     * @return {BackgroundStatus} a Background status witha new state
     */
    cloneWithState(newState) {
        var s = new BackgroundStatus();
        s.copyFrom(this);
        s.setState(newState);
        return s;
    }



//====================================================================
//  overriding equals
//====================================================================

    /**
     *
     * @param {BackgroundStatus} obj
     * @return {boolean} true if equal
     */
    equals(obj) {
        if (obj instanceof BackgroundStatus) {
            return this.serialize()===obj.serialize();
        }
        return false;
    }

    //====================================================================
//  convenience data converting routines
//====================================================================

    getBooleanParam(key, def=false) {
        return this.params[key] ? validator.toBoolean(this.params[key]) : def;
    }

    getIntParam(key, def=0) {
        var retval= validator.toInt(this.getParam(key));
        return !isNaN(retval) ? retval : def;
    }

    getFloatParam(key, def=0) {
        var retval= validator.toFloat(this.getParam(key));
        return !isNaN(retval) ? retval : def;
    }


    getDateParam(key) {
        var dateValue= validator.toInt(this.getParam(key));
        return !isNaN(dateValue) ? new Date(dateValue) : null;
    }

    getWorldPtParam(key) {
        var wpStr= this.getParam(key);
        return wpStr ? Point.parseWorldPt(wpStr) : null;
    }



//====================================================================
//
//====================================================================

//====================================================================
//  convenience data converting routines
//====================================================================

    addParam(str, key, value) {
        if (str && key && value) {
            str+= (PARAM_SEP+key+KW_VAL_SEP+value);
        }
        return str;
    }




}

