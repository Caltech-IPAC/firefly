/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get, includes, isNil,countBy} from 'lodash';
import {flux} from '../Firefly.js';
import {FormPanel} from './FormPanel.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {SizeInputFields} from './SizeInputField.jsx';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {ServerParams} from '../data/ServerParams.js';
import {showInfoPopup} from './PopupUtil.jsx';
import {FieldGroupCollapsible, CollapseBorder, CollapseHeaderCorner} from '../ui/panel/CollapsiblePanel.jsx';
import {FieldGroupTabs, Tab} from './panel/TabPanel.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {FileUpload} from './FileUpload.jsx';
import {ValidationField} from './ValidationField.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {parseWorldPt} from '../visualize/Point.js';
import WebPlotRequest, {WPConst} from '../visualize/WebPlotRequest.js';
import {dispatchPlotImage} from '../visualize/ImagePlotCntlr.js';
import {getImageMasterData} from '../visualize/ui/AllImageSearchConfig.js';
import {ImageSelect} from './ImageSelect.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {logError} from '../util/WebUtil.js';
import './NewImageSearchPanel.css';


const [RED, GREEN, BLUE] = [0, 1, 2];

const rgbFieldGroup = ['REDFieldGroup','GREENFieldGroup','BLUEFieldGroup'];
const rgb = ['red', 'green', 'blue'];
const FG_KEY= 'MULTI_IMAGE_SEARCH_PANEL';

const imageTypes =
    [
        {label: 'Single channel image', value: 'scImage'},
        {label: '3-color', value: 'threeColor'}
    ];


const imageSources =
    [
        {label: 'IRSA', value: 'irsa'},
        {label: 'Upload', value: 'upLoad'},
        {label: 'URL', value: 'url'}

    ];



const maskWrapper= {
    position:'absolute',
    left:0,
    top:0,
    width:'100%',
    height:'100%'
};

var outputMessage = (errMsg) => errMsg&&showInfoPopup(errMsg, 'Load Selected Image Error');
function getAllGroupFields(...keys) {
    return keys.reduce((prev, fg) => (Object.assign(prev, {[fg]: FieldGroupUtils.getGroupFields(fg)})), {});
}
export class NewImageSearchPanel extends PureComponent {

    constructor(props) {

            super(props);
            this.groupKey=FG_KEY;
            this.allfields = getAllGroupFields(this.groupKey,...rgbFieldGroup);

            this.state= {imageMasterData:null, showError: false,
                fields: this.allfields[this.groupKey],
                [rgbFieldGroup[RED]]: this.allfields[rgbFieldGroup[RED]],
                [rgbFieldGroup[GREEN]]: this.allfields[rgbFieldGroup[GREEN]],
                [rgbFieldGroup[BLUE]]: this.allfields[rgbFieldGroup[BLUE]],
              allFields: getAllGroupFields(this.groupKey,...rgbFieldGroup)};
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
    componentWillReceiveProps (nextProps) {
        this.allfields = getAllGroupFields(this.groupKey,...rgbFieldGroup);

        this.setState({
            fields: this.allfields[this.groupKey],
            [rgbFieldGroup[RED]]: this.allfields[rgbFieldGroup[RED]],
            [rgbFieldGroup[GREEN]]: this.allfields[rgbFieldGroup[GREEN]],
            [rgbFieldGroup[BLUE]]: this.allfields[rgbFieldGroup[BLUE]]

        });
    }
    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        /*this.removeListener = FieldGroupUtils.bindToStore(FG_KEY, (fields) => {
            if (this.iAmMounted && fields) this.setState(fields);
        });*/
        this.removeListener= flux.addListener(() => this.stateUpdate());
    }

    stateUpdate() {
        var allfields = getAllGroupFields(this.groupKey,...rgbFieldGroup);

        var isfieldChanged = (allfields) =>
        {
            var fields = rgbFieldGroup.find((fg) =>
                (this.state[fg] && allfields[fg] && this.state[fg] !== allfields[fg]));

            return (fields) ? true : (((allfields[this.groupKey]) || (this.state.fields))&&
                ( allfields[this.groupKey] !== this.state.fields ));
        };

        if (isfieldChanged(allfields) ) {

            if (this.iAmMounted) {
                this.setState({
                    fields: allfields[this.groupKey],
                    [rgbFieldGroup[RED]]: allfields[rgbFieldGroup[RED]],
                    [rgbFieldGroup[GREEN]]: allfields[rgbFieldGroup[GREEN]],
                    [rgbFieldGroup[BLUE]]: allfields[rgbFieldGroup[BLUE]]
                });
            }
        }
    }
    render() {
        const {imageMasterData, showError= false}= this.state;
        //const fields = FieldGroupUtils.getGroupFields(FG_KEY);
        const fields = this.state.fields;

        //const fs= this.state.fields;

        //const allFields = getAllGroupFields(FG_KEY,...rgbFieldGroup);
       // const mainPanel = allFields[FG_KEY];

      // const imageType1 = mainPanel && mainPanel['imageTypeOptions']?  mainPanel['imageTypeOptions']:'scImage';

        const imageType = FieldGroupUtils.getFldValue(fields, 'imageTypeOptions', 'scImage');
        const imageSource = FieldGroupUtils.getFldValue(fields, 'imageSourceOptions', 'irsa');


        if (imageMasterData) {
            return renderPanel(imageMasterData, imageType,imageSource);
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
function renderPanel(imageMasterData, imageType,imageSource) {

    const imagePanel = imageType==='scImage'? renderSingleColor(FG_KEY, imageMasterData, imageSource): renderThreeColors(imageMasterData, imageSource); //render3ColorPanels(imageMasterData, imageSource);

    return (
        <div style={{padding: 10}}>
            <FormPanel
                width='640px' height='500px'
                groupKey={FG_KEY}
                includeUnmounted={true}
                params={{hideOnInvalid: false}}
                onSubmit={(request) => onSearchSubmit(request, imageMasterData)}
                onError={(request) => searchFailed(request)}
                onCancel={hideSearchPanel}>
                <FieldGroup groupKey={FG_KEY} validatorFunc={null}
                            reducerFunc={mainReducer} keepState={true}>
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

                    </div>

                    <ImageTypeOptionPanel groupKey={FG_KEY}/>
                    <div >
                        {imagePanel}
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


// map of key/listener
const changeListeners = {};
function addChangeListener(key, changeListener) {
    changeListeners[key] = changeListener;
}
function mainReducer(inFields, action) {
    // put reducing logic here is any

    // call all listeners for
    inFields = Object.values(changeListeners).reduce( (p, l) => l(p, action), inFields);
    return inFields;
};


function hideSearchPanel() {
    dispatchHideDropDown();
}


function onSearchSubmit(request, imageMasterData) {
    console.log(request);
    const validInfo= validateInput(request);
    if (!validInfo.valid)  {
        showInfoPopup(validInfo.message);
        return false;
    }
    doImageSearch(request, imageMasterData);
}

function searchFailed(request) {
    const validInfo= validateInput(request);
    showInfoPopup(!validInfo.valid ? validInfo.message :'One or more fields are not valid');
}

function ImageTypeOptionPanel({groupKey}) {
    return (
         <div >
           <div className='OptionPanelsToolbar'>2. Choose image type</div>
           <div  className = 'OptionPanels' >
             <RadioGroupInputField
              initialState= {{
                  value: 'scImage',
                  tooltip: 'Please select the image type'

              }}
              options={imageTypes}
              alignment='horizontal'
              hPaddingRight={80}
              fieldKey='imageTypeOptions'
              groupKey={groupKey}/>
           </div>

         </div>
    );

};

ImageTypeOptionPanel.propTypes = {
    groupKey: PropTypes.string
};
//
function ImageSourceOptionPanel ({groupKey}){

    return (
        <div >
            <div className='OptionPanelsToolbar'>3. Select image source</div>
            <div  className = 'OptionPanels' >
            <RadioGroupInputField
                initialState= {{
                    value: 'irsa',
                    tooltip: 'Please select the image source',
                }}
                options={imageSources}
                alignment='horizontal'
                fieldKey='imageSourceOptions'
                hPaddingRight={60}
                groupKey={groupKey}/>
          </div>
        </div>
    );

};


ImageSourceOptionPanel.propTypes = {
    groupKey: PropTypes.string
};

function renderSingleColor(groupKey, imageMasterData, imageSource){

    var sourcePanel;
    switch (imageSource) {
        case 'irsa':
            sourcePanel=  <ImageSelect key='ImageSelect' {...{groupKey:{groupKey},  addChangeListener, imageMasterData, style:{width: 800, height: 400}}} />;
            break;

        case 'upLoad':
            sourcePanel= (
                <div  >
                    <FileUpload
                        wrapperStyle={{margin: '15px 10px 21px 10px'}}
                        fieldKey='fileUpload'
                        initialState= {{
                            tooltip: 'Select a file to upload' }}
                    />
                </div>
            );
            break;
        case 'url':
            sourcePanel= (
                <div >
                    <ValidationField
                        labelWidth={150}
                        label='Enter URL or FITS file:'
                        wrapperStyle={{margin: '15px 10px 21px 10px'}}
                        style={{width: 200}}
                        fieldKey='txURL'
                    />
                </div>
            );
            break;
    };

    return (
          <div >
            <ImageSourceOptionPanel groupKey={FG_KEY}/>
            {sourcePanel}
         </div>
    );

}

function renderThreeColors(imageMasterData,imageSource) {

 {/*   <FieldGroup groupKey={rgbFieldGroup[index]} reducerFunc={ImageSelPanelChangeOneColor}
                keepState={true}>
        {renderSingleColor(rgbFieldGroup[index],imageMasterData, imageSource)}
    </FieldGroup>*/}


    var corner = CollapseHeaderCorner.BottomLeft;
    const RGB = ['rgb(255, 51, 51)', 'rgb(51, 153, 51)', 'rgb(51, 51, 255)'];
    const threeColorTabs = rgb.map((color, index) => {

        return (
            <Tab name= {rgb[index]} id={rgbFieldGroup[index]}  >

                <FieldGroupCollapsible initialState= {{value:'open'}}
                                       fieldKey={`collapsible${index}`}
                                       key={index}
                                       header={rgb[index].toUpperCase()}
                                       isOpen={index===0}
                                       borderStyle={CollapseBorder.Oneborder}
                                       headerRoundCorner={index === 0 ? corner | CollapseHeaderCorner.TopRight : corner}
                                       wrapperStyle={ {marginBottom: 3} }
                                       headerStyle={{background: RGB[index],
                                           color: 'white',
                                           fontWeight: 'bold',
                                           paddingTop: 5,
                                           paddingBottom: 5 }}
                                       contentStyle={{padding: 10,
                                           paddingTop: 10,
                                           paddingBottom: 10,
                                           margin: '0px 0px 0px 10px'}}
                >

               {renderSingleColor(rgbFieldGroup[index],imageMasterData, imageSource)}

               </FieldGroupCollapsible>

            </Tab>

        );

    });

    return  (<div>
            <FieldGroupTabs fieldKey='threeColor'>
                {threeColorTabs}
            </FieldGroupTabs>
        </div>
    );
    //return threeColorTabs;
}


//-------------------------------------------------------------------------
// ----------- validation and successful results Code - todo: needs edit to work with new UI
//-------------------------------------------------------------------------


//todo: needs to work with changes in the UI
function validateInput(request) {
    const ret= {valid:true, message: 'success'};
    if (request.imageTypeOptions==='threeColor'){
        return ret;
    }
    if(request.imageSourceOptions==='upLoad'){
        if(!includes(request.fileUpload, 'fits') ){
            return ({valid:false, message:'FITS file is required'});
        }
        else {
            return ret;
        }
    }
    else if (request.imageSourceOptions==='url')  {
        if (isNil(request.txURL)){
            return ({valid:false, message:'invalid URL'});
        }
        else {
            return ret;
        }
    }
    else {
        const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
        if (!wp) {
            return ({valid: false, message: 'Target is required'});
        }

        if (!Object.keys(request).filter((k) => k.startsWith('IMAGES_')).some((k) => request[k].length > 0)) { // are any checked
            return ({valid: false, message: 'You must select at least one image'});
        }
        return ret;
    }
}


//todo: after UI changes: needs to come up with list of plotRequestParams to make into WebPlotRequest
function doImageSearch(request, imageMasterData) {
    const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
    const radius= request.conesize;
    const isThreeColor = request.imageTypeOptions==='threeColor';

    if (isThreeColor){

        loadThreeColorImages(request, imageMasterData);
    }
    else {
        loadSingleChannelImage(request, imageMasterData,wp, radius);
    }
}

function getWPRArray(req, imageMasterData,wp, radius, isThreeColor=false){

    const imageIdList = [];

    Object.keys(req).filter((k) => k.startsWith('IMAGES_')).forEach((p) => {

        const missionId = p.split('_')[1].split('||')[0]; //remove the trailing || symbol if any

        const list = isThreeColor? (req['PROJ_ALL_'+ missionId] && req['PROJ_ALL_'+ missionId].value ? req[p].value.split(','):null) :req[p].split(',');
        if(list && list.length>0) {
            list.forEach((e) => imageIdList.push(e));
        }
    });

    if (imageIdList.length===0){
        return [null];
    }
    const paramAry = imageMasterData.filter((d) => imageIdList.includes(d.imageId));


    return  paramAry
        .map((d) => makeWPRequest(wp, radius, d.plotRequestParams));

}

function loadSingleChannelImage(request, imageMasterData,wp, radius){
    if (request.imageSourceOptions === 'upLoad') {

        const wpr = makeFitsWebRequest(request);
        dispatchPlotImage({wpRequest:wpr});

    }
    else if (request.imageSourceOptions === 'url') {
        const wpr = makeURLWebRequest(request);
        dispatchPlotImage({wpRequest:wpr});

    }
    else {
        const wpr = getWPRArray(request, imageMasterData,wp, radius);
        wpr.forEach( (r) => dispatchPlotImage({wpRequest:r}));

    }
}



function loadThreeColorImages(request, imageMasterData){

    const allfields = getAllGroupFields(FG_KEY,...rgbFieldGroup);
    var wpSet = [];
    const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
    const radius= request.conesize;
    const threeColorPanel = FieldGroupUtils.getGroupFields('threeColor');
    rgbFieldGroup.map((item, index) => {
        const r = allfields[item];
        const req = FieldGroupUtils.getGroupFields(item);
        var wpr=null;
        if (!isNil(req) && request['collapsible'+index]==='open') {
            switch (request.imageSourceOptions){
                case 'upLoad':
                    wpr = makeFitsWebRequest(req);
                    break;
                case 'url':
                    wpr = makeURLWebRequest(req);
                    break;
                case 'irsa':
                    wpr= getWPRArray(req, imageMasterData,wp, radius, true)[0];
                    if (wpr) {
                        wpr.setTitle('3-Color Image');

                    }
                    break;

            }
        }
        if (!wpr) {
            logError(' no image on ' +  rgb[index] + ' is selected');
        }
        wpSet.push(wpr);

    });

    if (countBy(wpSet)[null]===3){
        return outputMessage('No image source is selected');
    }

    dispatchPlotImage({wpRequest: wpSet});

}
//-------------------------------------------------------------------------
// ----------- END: validation and successful results Code
//-------------------------------------------------------------------------


//------------------------------------------------------------------
//------ The code below should work in the final product
//------------------------------------------------------------------



//=========utility?====================

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

function makeURLWebRequest(request) {

    var url = get(request, 'txURL');
    const params = {
        [WPConst.PLOT_ID] : rootId+rootIdCnt,
        [WPConst.URLKEY] : url,
        [WPConst.PLOT_GROUP_ID] : 'multiImageGroup',
        [WPConst.GROUP_LOCKED] : 'true'
    };
    rootIdCnt++;

    return  WebPlotRequest.makeFromObj(params);
}

function makeFitsWebRequest(request, color='') {

    var fits = get(request, 'fileUpload');

    const params = {
        [WPConst.PLOT_ID] : rootId+rootIdCnt,
        [WPConst.FILE] : fits,
        [WPConst.PLOT_GROUP_ID] : 'multiImageGroup',
        [WPConst.GROUP_LOCKED] : 'true'
       // [WPConst.TITLE_OPTIONS] : TitleOptions.FILE_NAME
    };
    rootIdCnt++;
    return  WebPlotRequest.makeFromObj(params);


}
