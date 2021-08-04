/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {memo, useContext, useState, useEffect} from 'react';
import {string,array,object} from 'prop-types';
import {get, isFunction, isArray} from 'lodash';
import {useStoreConnector} from '../../ui/SimpleComponent';
import { NewPlotMode, dispatchAddViewer, dispatchViewerUnmounted, IMAGE, PLOT2D, SINGLE, GRID,
    getViewer, getMultiViewRoot
} from '../MultiViewCntlr.js';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {ImageMetaDataToolbar} from './ImageMetaDataToolbar.jsx';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {MultiChartViewer} from '../../charts/ui/MultiChartViewer';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {CompleteButton} from '../../ui/CompleteButton';
import {DropDownToolbarButton} from '../../ui/DropDownToolbarButton.jsx';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {DPtypes, SHOW_CHART, SHOW_TABLE, SHOW_IMAGE} from '../../metaConvert/DataProductsType';
import {
    dataProductRoot,
    dispatchActivateFileMenuItem,
    dispatchActivateMenuItem,
    getActiveFileMenuKey,
    getActiveMenuKey,
    getDataProducts,
    doDownload,
    getActiveFileMenuKeyByKey,
    dispatchUpdateActiveKey,
    dispatchInitDataProducts,
    getActivateParams,
    isInitDataProducts,
    getSearchParams,
    dispatchSetSearchParams, dispatchUpdateDataProducts
} from '../../metaConvert/DataProductsCntlr';
import {RadioGroupInputFieldView} from '../../ui/RadioGroupInputFieldView';
import {dispatchChangeActivePlotView} from '../ImagePlotCntlr';
import {ActivateMenu} from './ActivateMenu.jsx';
import {getServiceParamsAry} from '../../metaConvert/DataProductsCntlr.js';



export function MultiProductViewer ({viewerId, ...props}) {
    const [init, setInit]= useState(isInitDataProducts(dataProductRoot(), viewerId));
    useEffect(() => {
        if (init) return;
        dispatchInitDataProducts(viewerId);
        setInit(true);
    },[viewerId,init]);
    return init && <MultiProductViewerImpl {...{dpId:viewerId, ...props}} />;
}

MultiProductViewer.propTypes= {
    viewerId : string,
    metaDataTableId : string,
};


const MultiProductViewerImpl= memo(({ dpId='DataProductsType', metaDataTableId, noProductMessage}) => {
    const {renderTreeId} = useContext(RenderTreeIdCtx);
    const [currentCTIChoice, setCurrentCTIChoice] = useState(undefined);
    const [lookupKey, setLookKey] = useState(undefined);
    // const [activateParams, setActivateParams]= useState(undefined);
    const [dataProductsState, serviceParamsAry]= useStoreConnector(
        (old) => {
            const newDp= getDataProducts(dataProductRoot(),dpId)||{};
            return (!old || (newDp!==old && newDp.displayType && newDp.displayType!==DPtypes.DOWNLOAD)) ? newDp : old;
        },
        () => getServiceParamsAry(dataProductRoot(),dpId)
    );


    const {imageViewerId,chartViewerId,tableGroupViewerId}=  getActivateParams(dataProductRoot(),dpId);

    const {displayType='unsupported', menu,fileMenu,
        message,url, isWorkingState, menuKey,
        activeMenuLookupKey,singleDownload= false,
        chartTableDefOption=SHOW_CHART, imageActivate,
        allowsInput=false, serDefParams= undefined, noProductsAvailable=false}= dataProductsState;

    const searchParams= getSearchParams(serviceParamsAry,activeMenuLookupKey,menuKey);


    let {activate}= dataProductsState;

    useEffect(() => {
        dispatchAddViewer(imageViewerId, NewPlotMode.none, IMAGE,true, renderTreeId, SINGLE, true,true);
        dispatchAddViewer(chartViewerId, NewPlotMode.none, PLOT2D,true, renderTreeId, GRID, false, true);
        return () => {
            dispatchViewerUnmounted(imageViewerId);
            dispatchViewerUnmounted(chartViewerId);
        };
    }, [imageViewerId, chartViewerId, renderTreeId]);

    useEffect(() => void (displayType!==DPtypes.IMAGE && dispatchChangeActivePlotView(undefined)), [displayType] );

    let ctLookupKey;
    let initCTIChoice;
    let ctiChoice;
    if (displayType===DPtypes.CHOICE_CTI) {
        ctLookupKey= makeChartTableLookupKey(fileMenu?.activeItemLookupKey ?? '',menuKey || getActiveFileMenuKey(dpId,fileMenu));
        initCTIChoice= lookupKey ? (getActiveFileMenuKeyByKey(dpId,ctLookupKey) || chartTableDefOption): chartTableDefOption;
        ctiChoice= currentCTIChoice||initCTIChoice;
        if (ctiChoice===SHOW_IMAGE) activate= imageActivate;
    }

    useEffect(() => {
        setCurrentCTIChoice(displayType===DPtypes.CHOICE_CTI ? initCTIChoice : undefined);
        setLookKey(displayType===DPtypes.CHOICE_CTI ? ctLookupKey : undefined);
    }, [displayType,initCTIChoice,ctLookupKey]);

    useEffect(() => {
        if (allowsInput && !searchParams) return;
        const deActivate= activate?.(menu,searchParams);
        return () => isFunction(deActivate) &&
            deActivate( {
                nextDisplayType:displayType,
                nextMetaDataTableId:metaDataTableId
            });
    }, [activate,searchParams,allowsInput]);



    const resetAllSearchParams= () => menu?.forEach( (entry) =>
            getSearchParams(serviceParamsAry,activeMenuLookupKey,entry.menuKey) && dispatchSetSearchParams(
                                        { dpId, activeMenuLookupKey, menuKey:entry.menuKey, params: undefined }));

    const doResetButton= displayType!==DPtypes.ANALYZE && !isWorkingState && Boolean(searchParams || serDefParams?.some( (sdp) => !sdp.ref));
    // const doResetButton= Boolean(searchParams);
    let makeDropDown;
    if (menu?.length>1 || fileMenu?.menu?.length>1) {
        const showMenu= !singleDownload || singleDownload && displayType===DPtypes.DOWNLOAD_MENU_ITEM;
        makeDropDown= getMakeDropdown(dpId, dataProductsState, showMenu, doResetButton, resetAllSearchParams);
    }


    switch (displayType) {
        case DPtypes.IMAGE :
            return makeMultiImageViewer(imageViewerId,metaDataTableId,makeDropDown,ImageMetaDataToolbar);
        case DPtypes.ANALYZE :
            if (allowsInput && !searchParams) {
                return (<ActivateMenu
                    {...{
                        serDefParams,
                        setSearchParams: (params) => dispatchSetSearchParams({dpId,activeMenuLookupKey,menuKey,params}),
                        title:dataProductsState.name,
                        makeDropDown,
                        }} />);
            }
            else {
                return (<ProductMessage {...{menu, singleDownload, makeDropDown, isWorkingState, message}}/>);
            }
        case DPtypes.MESSAGE :
        case DPtypes.PROMISE :
            if (dataProductsState.complexMessage) {
                return (<ComplexMessage {...{menu, makeDropDown, message,
                    detailMsgAry:dataProductsState.detailMsgAry, badUrl:dataProductsState.badUrl,
                    resetMenuKey:dataProductsState.resetMenuKey, dpId, activeMenuLookupKey, doResetButton }} />);

            }
            else {
                const useMessage= noProductsAvailable && noProductMessage ? noProductMessage : message;
                return (<ProductMessage {...{menu, singleDownload, makeDropDown, isWorkingState, message:useMessage}} />);
            }
        case DPtypes.DOWNLOAD_MENU_ITEM :
            return (<ProductMessage {...{menu, singleDownload, makeDropDown, isWorkingState:false, message}} />);
        case DPtypes.TABLE :
            return (<MultiProductChoice {...{dpId,makeDropDown,tableGroupViewerId,whatToShow:SHOW_TABLE}}/>);
        case DPtypes.CHART :
            return (<MultiProductChoice {...{dpId,makeDropDown,chartViewerId,whatToShow:SHOW_CHART}}/>);
        case DPtypes.CHOICE_CTI :
            return (
                <MultiProductChoice { ...{
                    makeDropDown,chartViewerId, imageViewerId:imageActivate?imageViewerId:'',
                    metaDataTableId, tableGroupViewerId,whatToShow:ctiChoice, ctLookupKey,mayToggle:true,
                    onChange: (ev) => {
                        setCurrentCTIChoice(ev.target.value);
                        dispatchUpdateActiveKey({dpId, activeFileMenuKeyChanges:{[ctLookupKey]:ev.target.value}});
                    },
                }} />);
        case DPtypes.PNG :
            return (<ProductPNG {...{makeDropDown, url}}/>);
    }

    if (noProductMessage) {
        return (<ProductMessage {...{menu, singleDownload, makeDropDown, isWorkingState, message:noProductMessage}} />);
    }
    else {
        return (<div/>);
    }
});


const makeChartTableLookupKey= (activeItemLookupKey, fileMenuKey) => `${activeItemLookupKey}-charTable-${fileMenuKey}`;


const chartTableOptions= [
    {label: 'Table', value: SHOW_TABLE},
    {label: 'Chart', value: SHOW_CHART}
    ];
const imageOp= {label: 'Image', value: SHOW_IMAGE};

const choiceTBStyle= {display:'flex', flexDirection: 'column', background: '#c8c8c8', width:'100%', height:'100%'};
const chartChoiceTBStyle= { width:'100%', height:'calc(100% - 30px)', background: '#c8c8c8'};
const tbInternalStyle= {display:'flex', flexDirection: 'row', alignItems:'center', height:30};

function MultiProductChoice({makeDropDown, chartViewerId, imageViewerId, metaDataTableId,
                                    tableGroupViewerId,whatToShow, onChange, mayToggle=false}) {

    const options= !imageViewerId ? chartTableOptions : [...chartTableOptions, imageOp];
    const toolbar= (
        <div style={tbInternalStyle}>
            {makeDropDown && <div style={{height:30}}> {makeDropDown()} </div>}
            {mayToggle && <RadioGroupInputFieldView wrapperStyle={{paddingLeft:20}}
                                                    {...{options, value:whatToShow, buttonGroup:true, onChange}} />}
        </div> );

    switch (whatToShow) {
        case SHOW_CHART:
            return (
                <div style={chartChoiceTBStyle}>
                    {toolbar}
                    <MultiChartViewer viewerId={chartViewerId} closeable={false} canReceiveNewItems={NewPlotMode.none.key}/>
                </div>
            );
        case SHOW_TABLE:
            return (
                <div style={choiceTBStyle}>
                    {toolbar}
                    <TablesContainer tbl_group={tableGroupViewerId} closeable={false} expandedMode={false}/>
                </div>
            );
        case SHOW_IMAGE:
            return (
                <div style={choiceTBStyle}>
                    {toolbar}
                    {makeMultiImageViewer(imageViewerId,metaDataTableId,undefined,ImageMetaDataToolbar)}
                </div>
            );
    }
    return false;
}


function ProductMessage({menu, singleDownload, makeDropDown, isWorkingState, message, url}) {
    let dMsg= singleDownload  && menu[0].name;
    if (dMsg && menu[0].fileType) dMsg= `${dMsg}, type: ${menu[0].fileType}`;
    let actionUrl= url;
    if (singleDownload && !url) actionUrl= isArray(menu) && menu.length && menu[0].url;

    return (
        <div style={{display:'flex', flexDirection: 'column', background: '#c8c8c8', width:'100%', height:'100%'}}>
            <div style={{height:menu?30:0}}>
                {makeDropDown && makeDropDown?.()}
            </div>
            <div style={{display:'flex', flexDirection: 'row', alignSelf:'center', paddingTop:40}}>
                {isWorkingState ?
                    <div style={{width:20, height:20, marginRight: 10}} className='loading-animation' /> : ''}
                <div style={{alignSelf:'center', fontSize:'14pt'}}>{message}</div>
            </div>
            {
                singleDownload && <CompleteButton style={{alignSelf:'center', paddingTop:25 }} text={dMsg}
                                onSuccess={() => doDownload(actionUrl)}/>
            }
        </div>
    );
}

function ComplexMessage({menu, makeDropDown, message, resetMenuKey, dpId,activeMenuLookupKey, doResetButton, detailMsgAry=[], badUrl}) {
    return (
        <div style={{display:'flex', flexDirection: 'column', background: '#c8c8c8', width:'100%', height:'100%'}}>
            <div style={{height:menu?30:0}}>
                {makeDropDown && makeDropDown?.()}
            </div>
            <div style={{display:'flex', flexDirection: 'column', alignSelf:'center', paddingTop:40}}>
                <div style={{alignSelf:'center', fontSize:'14pt', paddingBottom:10}}>{message}</div>
                {detailMsgAry.map( (m) => (<div style={{alignSelf:'center', fontSize:'12pt', paddingTop:5}} key={m}>{m}</div>))}
                {badUrl &&
                <div style={{alignSelf:'left', fontSize:'12pt', paddingTop:5, maxWidth:200}}>
                    <span style={{whiteSpace:'nowrap', paddingRight: 5}}>Failed URL:</span>
                    <a href={badUrl} target={'badURLTarget'}>
                        <span style={{fontSize: '10pt'}}> {badUrl} </span>
                    </a>
                </div>
                }
            </div>
            {doResetButton && <CompleteButton style={{alignSelf:'center', paddingTop:25 }} text={'Reset'}
                                              onSuccess={() => {
                                dispatchSetSearchParams({ dpId, activeMenuLookupKey, menuKey:resetMenuKey, params: undefined });
                                dispatchActivateMenuItem(dpId,resetMenuKey);
                            }}/>}
        </div>
    );
}

const ProductPNG = ( {makeDropDown, url}) => (
    <div style={{display:'flex', flexDirection: 'column', background: '#c8c8c8', width:'100%', height:'100%'}}>
        {makeDropDown &&  <div style={{height:30, width:'100%'}}>
            {makeDropDown()}
        </div>}
        <div style={{overflow:'auto', display:'flex', justifyContent: 'center', alignItem:'center'}}>
            <img src={url} alt={url} style={{maxWidth:'100%', flexGrow:0, flexShrink:0, objectFit: 'contain' }}/>
        </div>
    </div> );


const FileMenuDropDown= ({fileMenu, dpId}) => (
    <SingleColumnMenu>
        {fileMenu.menu.map( (fileMenuItem, idx) => (
                <ToolbarButton text={fileMenuItem.name} tip={fileMenuItem.name}
                               style={fileMenuItem.interpretedData ? {paddingLeft: 30} : {}}
                               enabled={true} horizontal={false} key={'fileMenuOptions-'+idx} hasCheckBox={true}
                               checkBoxOn={fileMenuItem.menuKey===getActiveFileMenuKey(dpId,fileMenu)}
                               onClick={() => dispatchActivateFileMenuItem({dpId,fileMenu,newActiveFileMenuKey:fileMenuItem.menuKey})}/> )
        )}
    </SingleColumnMenu> );

FileMenuDropDown.propTypes= { dpId : string, fileMenu : object};


const OtherOptionsDropDown= ({menu, dpId, activeMenuLookupKey, resetAllSearchParams}) => {
    return (
        <SingleColumnMenu>
            {menu.map( (menuItem, idx) => (
                <ToolbarButton text={menuItem.name} tip={`${menuItem.semantics} - ${menuItem.url}`}
                               enabled={true} horizontal={false} key={'otherOptions-'+idx} hasCheckBox={true}
                               checkBoxOn={menuItem.menuKey===getActiveMenuKey(dpId, activeMenuLookupKey)}
                               onClick={() => {
                                   resetAllSearchParams();
                                   dispatchActivateMenuItem(dpId,menuItem.menuKey);
                               } }/> )
            )}
        </SingleColumnMenu> );
};

OtherOptionsDropDown.propTypes= { dpId : string, menu : array, activeMenuLookupKey : string, };


const makeMultiImageViewer= (imageViewerId,metaDataTableId,makeDropDown, ImageMetaDataToolbar) => {
    return (
        <MultiImageViewer {...{
            viewerId:imageViewerId, insideFlex:true,
            canReceiveNewPlots: NewPlotMode.none.key, tableId:metaDataTableId, controlViewerMounting:false,
            makeDropDown, Toolbar:ImageMetaDataToolbar}} />);
};


/**
 *
 * @param {String} dpId
 * @param {DataProductState} dataProductState
 * @param {boolean} showMenu - true to show the Menu
 * @param {boolean} showRedoSearchButton - true to show the button
 * @param {Function} resetAllSearchParams - function that will reset all the search param input to undefined
 * @return {function|undefined} function to create the drapdown menu
 */
function getMakeDropdown(dpId, dataProductState, showMenu, showRedoSearchButton, resetAllSearchParams) {
    const {menu,fileMenu,activeMenuLookupKey, menuKey, analysisActivateFunc, originalTitle}= dataProductState;
    const hasFileMenu= get(fileMenu,'menu.length',0)>1;
    const hasMenu= showMenu && menu && menu.length>0;
    if (!hasMenu && !hasFileMenu && !showRedoSearchButton) return undefined;
    return () => (
            <div style={{display:'flex', flexDirection:'row'}}>
                {!hasMenu ? <div style={{width: 50,height:1 }}/> :
                    <DropDownToolbarButton
                        text={'More'}
                        tip='Other data to display'
                        enabled={true} horizontal={true}
                        visible={true}
                        style={{paddingRight:20}}
                        useDropDownIndicator={true}
                        dropDown={<OtherOptionsDropDown {...{menu, dpId, activeMenuLookupKey, resetAllSearchParams}} />} /> }

                {hasFileMenu &&
                <DropDownToolbarButton
                    text={'File Contents'}
                    tip='Other data in file'
                    enabled={true} horizontal={true}
                    visible={true}
                    style={{paddingRight:20}}
                    useDropDownIndicator={true}
                    dropDown={<FileMenuDropDown {...{fileMenu, dpId}} />} />
                }
                {showRedoSearchButton && analysisActivateFunc &&
                    <ToolbarButton
                        text='Redo Search' tip={'Redo Search'} horizontal={true}
                        onClick={() => {
                            dispatchSetSearchParams({ dpId, activeMenuLookupKey, menuKey, params: undefined });
                            dispatchUpdateDataProducts(dpId, {...dataProductState, allowsInput: true,
                                name: originalTitle,
                                displayType:DPtypes.ANALYZE, activate:analysisActivateFunc});
                        }} />
                }
            </div>
        );
}
