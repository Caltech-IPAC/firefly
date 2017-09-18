/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {uniqBy} from 'lodash';

import {FormPanel} from './FormPanel.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {SizeInputFields} from './SizeInputField.jsx';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {ServerParams} from '../data/ServerParams.js';
import {showInfoPopup} from './PopupUtil.jsx';

import {dispatchHideDropDown} from '../core/LayoutCntlr.js';

import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {parseWorldPt} from '../visualize/Point.js';
import WebPlotRequest, {WPConst} from '../visualize/WebPlotRequest.js';
import {dispatchPlotImage} from '../visualize/ImagePlotCntlr.js';
import {imageMasterData} from '../data/ImageMasterData.js';
import {getImageMasterData} from '../visualize/ui/AllImageSearchConfig.js';



const FG_KEY= 'MULTI_IMAGE_SEARCH_PANEL';

const maskWrapper= {
    position:'absolute',
    left:0,
    top:0,
    width:'100%',
    height:'100%'
};

export class NewImageSearchPanel extends PureComponent {

    constructor(props) {
        super(props);
        this.state= {imageMasterData:null, showError: false};
        this.updateData();
    }


    updateData() {

        setTimeout( () => {
            getImageMasterData()
                .then( (imageMasterData) => {
                    this.setState( () => ({imageMasterData}));
                })
                .catch( () => {
                    this.setState(() => ({showError:true}));
                });

        },10);

    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = FieldGroupUtils.bindToStore(FG_KEY, (fields) => {
            if (this.iAmMounted && fields) this.setState(fields);
        });
    }

    render() {
        const {imageMasterData, showError= false}= this.state;

        if (imageMasterData) {
            return renderPanel(imageMasterData);
        }
        else if (showError) {
            return (
                <div style={{width:500}}>
                    <div style={{ padding: '50px 0 50px 0', textAlign: 'center', fontSize: '20pt' }}>
                        Error loading image search meta data
                    </div>
                </div>
            );
        }
        else {
            return <div style={maskWrapper}> <div className='loading-mask'/> </div>;
        }
    }
}


//todo renderPanel should be updated to include new UI work
function renderPanel(imageMasterData) {
    const projects= uniqBy(imageMasterData, 'project').map( (d) => d.project);
    const imageByProjObj= projects
        .reduce((obj,p) => {
                obj[p]= imageMasterData.filter( (d) => p===d.project);
                return obj;
            }, {}
        );

    const projectUIAry= projects.map( (k) => makeProject(k,imageByProjObj[k]));


    return (
        <div style={{padding: 10}}>
            <FormPanel
                width='640px' height='500px'
                groupKey={FG_KEY}
                params={{hideOnInvalid: false}}
                onSubmit={(request) => onSearchSubmit(request)}
                onError={(request) => searchFailed(request)}
                onCancel={hideSearchPanel}>
                <FieldGroup groupKey={FG_KEY} validatorFunc={null} keepState={true}>
                    <div style={{padding:'5px 0 0 0', display: 'flex', textAlign:'center', justifyContent: 'center' }}>
                        <TargetPanel/>
                    </div>
                    <div style={{padding:'0 5px 0 5px', display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center' }}>
                        <SizeInputFields fieldKey='conesize' showFeedback={true}
                                         wrapperStyle={{padding:5, margin: '5px 0 5px 0'}}
                                         initialState={{
                                             unit: 'arcsec',
                                             labelWidth : 0,
                                             nullAllowed: false,
                                             value: (500/3600)+'',
                                             min: 1 / 3600,
                                             max: 1,
                                         }}
                                         label={'Choose Radius'}
                        />
                        <div style={{padding:'10px 5px 0 5px', display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center' }}>
                            {projectUIAry}
                        </div>
                    </div>
                </FieldGroup>
            </FormPanel>
        </div>
    );
}


NewImageSearchPanel.propTypes = {
    name: PropTypes.oneOf(['TestSearches']),
};

NewImageSearchPanel.defaultProps = {
    name: 'TestSearches',
};


function hideSearchPanel() {
    dispatchHideDropDown();
}


function onSearchSubmit(request) {
    console.log(request);
    const validInfo= validateInput(request);
    if (!validInfo.valid)  {
        showInfoPopup(validInfo.message);
        return false;
    }
    doImageSearch(request);
    console.log(request.imageSelection);
}

function searchFailed(request) {
    const validInfo= validateInput(request);
    showInfoPopup(!validInfo.valid ? validInfo.message :'One or more fields are not valid');
}



//-------------------------------------------------------------------------
// ----------- Begin Concept layout - todo: loi will replace all of this
//-------------------------------------------------------------------------

const toEntryAry= (a) => a.map ( (d) => ({label: d.title, value: d.imageId}));

function makeEntry(title, array, showTitle) {
    const textCnt= array.reduce( (cnt,d) => d.title.length+cnt,0);
    let imagesRows;
    if (textCnt>70) {
        const a1= toEntryAry(array.filter( (d,idx) => idx< array.length/2));
        const a2= toEntryAry(array.filter( (d,idx) => idx>= Math.trunc(array.length/2)));
        imagesRows= [a1,a2];
    }
    else {
        imagesRows= [toEntryAry(array)];

    }
    return (
            <div key={title} style={{padding:5, display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center' }}>
                <div style={{display:'inline-block'}}>
                    <div style={{textAlign: 'center', fontSize:'10pt'}}>
                    {showTitle ? title : ''}
                    </div>
                    {
                        imagesRows.map( (row,idx) => {
                            const fieldKey= 'IMAGES_'+idx+'_'+title;
                            return (
                                <CheckboxGroupInputField
                                    key={fieldKey}
                                    fieldKey={fieldKey}
                                    initialState={{
                                        value: '',   // workaround for _all_ for now
                                        tooltip: 'Please select some boxes',
                                        label : '' }}
                                    options={row}
                                    alignment='horizontal'
                                    labelWidth={35}
                                />
                            );
                        } )
                    }
                </div>
            </div>
    );
}

function makeProject(title, array) {
    let content;
    if (array.find( (e) => e.subProject)) {
        const subProjects= uniqBy(array, 'subProject').map( (d) => d.subProject);
        const imageBySubProjObj= subProjects
            .reduce((obj,p) => {
                    obj[p]= array.filter( (d) => p===d.subProject);
                    return obj;
                }, {}
            );
        content= subProjects.map( (k) => makeEntry(k,imageBySubProjObj[k],true));
    }
    else {
        content= makeEntry(title,array,false);
    }
    return (
        <div key= {title} style={{paddingBottom: 5}}>
            <div style={{textAlign: 'center', fontSize:'12pt'}}>{title}
            </div>
            <div>
                {content}
            </div>
        </div>
    );

}

//-------------------------------------------------------------------------
// ----------- End Concept layout
//-------------------------------------------------------------------------



//-------------------------------------------------------------------------
// ----------- validation and successful results Code - todo: needs edit to work with new UI
//-------------------------------------------------------------------------


//todo: needs to work with changes in the UI
function validateInput(request) {
    const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
    if (!wp) {
        return ({valid:false, message:'Target is required'});
    }

    if (!Object.keys(request).filter( (k) => k.startsWith('IMAGES_')).some( (k) => request[k].length>0))  { // are any checked
        return ({valid:false, message:'You must select at least one image'});
    }
    return {valid:true, message: 'success'};
}


//todo: after UI changes: needs to come up with list of plotRequestParams to make into WebPlotRequest
function doImageSearch(request) {
    const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
    const radius= request.conesize;

    const imageIdList= [];
    Object.keys(request).filter( (k) => k.startsWith('IMAGES_')).forEach( (p) => {
        const list= request[p].split(',');
        list.forEach( (e) => imageIdList.push(e));
    });


    const paramAry= imageMasterData.filter( (d) => imageIdList.includes(d.imageId));
    loadImages(wp,radius,paramAry);
}

//-------------------------------------------------------------------------
// ----------- END: validation and successful results Code
//-------------------------------------------------------------------------


//------------------------------------------------------------------
//------ The code below should work in the final product
//------------------------------------------------------------------

function loadImages(wp, radius, paramAry) {
    paramAry
        .map( (d) => makeWPRequest(wp, radius, d.plotRequestParams) )
        .forEach( (r) => dispatchPlotImage({wpRequest:r}));
}


const rootId='conceptPid-';
let rootIdCnt=1;

function makeWPRequest(wp, radius, params) {
    const inReq= Object.assign( {
        [WPConst.PLOT_ID] : rootId+rootIdCnt,
        [WPConst.WORLD_PT] : wp.toString(),
        [WPConst.SIZE_IN_DEG] : radius+'',
        [WPConst.PLOT_GROUP_ID] : 'multiImageGroup',
        [WPConst.GROUP_LOCKED] : 'true',
    }, params);

    rootIdCnt++;
    return WebPlotRequest.makeFromObj(inReq);
}

