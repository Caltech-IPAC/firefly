/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isFunction} from 'lodash';
import {bool, string} from 'prop-types';
import React, {memo, useContext, useEffect, useState} from 'react';
import {
    dataProductRoot, dispatchInitDataProducts, dispatchSetSearchParams, dispatchUpdateActiveKey, getActivateParams,
    getActiveFileMenuKey, getActiveFileMenuKeyByKey, getDataProducts, getSearchParams, getServiceParamsAry,
    isInitDataProducts
} from '../../../metaConvert/DataProductsCntlr.js';
import {DPtypes, SHOW_CHART, SHOW_IMAGE, SHOW_TABLE} from '../../../metaConvert/DataProductsType.js';
import {ServiceDescriptorPanel} from '../../../ui/dynamic/ServiceDescriptorPanel.jsx';
import {RenderTreeIdCtx} from '../../../ui/RenderTreeIdCtx.jsx';
import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {
    dispatchAddViewer, dispatchViewerUnmounted, GRID, IMAGE, NewPlotMode, PLOT2D, SINGLE
} from '../../MultiViewCntlr.js';
import {createMakeDropdownFunc} from './DPDropdown.jsx';
import {AdvancedMessage, ProductMessage} from './MPMessages.jsx';
import {MultiProductChoice} from './MultiProductChoice.jsx';


export function MultiProductViewer ({viewerId= 'DataProductsType', ...props}) {
    const [init, setInit]= useState(isInitDataProducts(dataProductRoot(), viewerId));
    useEffect(() => {
        if (init) return;
        dispatchInitDataProducts(viewerId);
        setInit(true);
    },[viewerId,init]);
    const dpId= viewerId;
    const activateParams=  getActivateParams(dataProductRoot(),dpId);
    return init && <MultiProductViewerImpl {...{dpId, activateParams, ...props}} />;
}

MultiProductViewer.propTypes= {
    viewerId : string,
    metaDataTableId : string,
    enableExtraction: bool
};



const MultiProductViewerImpl= memo(({ dpId, activateParams, metaDataTableId, noProductMessage, enableExtraction=false}) => {
    const {renderTreeId} = useContext(RenderTreeIdCtx);
    const [currentCTIChoice, setCurrentCTIChoice] = useState(undefined);
    const [lookupKey, setLookKey] = useState(undefined);
    const dataProductsState = useStoreConnector((old) => {
            const newDp= getDataProducts(dataProductRoot(),dpId)||{};
            return (!old || (newDp!==old && newDp.displayType && newDp.displayType!==DPtypes.DOWNLOAD)) ? newDp : old;
        });
    const serviceParamsAry = useStoreConnector(() => getServiceParamsAry(dataProductRoot(),dpId));

    const {imageViewerId,chartViewerId}= activateParams;

    const {displayType='unsupported', menu,fileMenu, isWorkingState, menuKey,
        activeMenuLookupKey,singleDownload= false, chartTableDefOption=SHOW_CHART,
        imageActivate, extractionText, allowsInput=false, serDefParams= undefined}= dataProductsState;
    let {activate}= dataProductsState;
    const extraction= enableExtraction && dataProductsState.extraction;

   const searchParams= getSearchParams(serviceParamsAry,activeMenuLookupKey,menuKey);

    useEffect(() => {
        dispatchAddViewer(imageViewerId, NewPlotMode.none, IMAGE,true, renderTreeId, SINGLE, true,true);
        dispatchAddViewer(chartViewerId, NewPlotMode.none, PLOT2D,true, renderTreeId, GRID, false, true);
        return () => {
            dispatchViewerUnmounted(imageViewerId);
            dispatchViewerUnmounted(chartViewerId);
        };
    }, [imageViewerId, chartViewerId, renderTreeId]);

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

    const doResetButton= displayType!==DPtypes.ANALYZE && !isWorkingState && Boolean(searchParams || serDefParams?.some( (sdp) => !sdp.ref));

    const getInput= displayType===DPtypes.ANALYZE && allowsInput && !searchParams;
    const showMenu= !singleDownload || (singleDownload && (displayType===DPtypes.DOWNLOAD_MENU_ITEM || displayType===DPtypes.MESSAGE));
    const doMakeDropdown= menu?.length || fileMenu?.menu?.length || extraction;

    const makeDropDown= doMakeDropdown ?
        createMakeDropdownFunc( {dpId, dataProductsState, showMenu, showRedoSearchButton:doResetButton,
            extraction: !getInput && extraction}) : undefined;

    return (
        <ViewerRender {...{dpId, dataProductsState, noProductMessage, metaDataTableId,makeDropDown,
                              setCurrentCTIChoice, ctiChoice, ctLookupKey, activateParams,
                              getInput, doResetButton, }} />
    );

});


function ViewerRender({dpId, dataProductsState, noProductMessage, metaDataTableId, makeDropDown, activateParams,
                          setCurrentCTIChoice, ctiChoice, ctLookupKey, getInput, doResetButton}) {
    const {displayType='unsupported', menu, singleDownload, isWorkingState, message, activeMenuLookupKey,
        menuKey, imageActivate, url, serDefParams, serviceDefRef, sRegion, name:title }= dataProductsState;
    const {imageViewerId,chartViewerId,tableGroupViewerId}=  activateParams;
    switch (displayType) {
        case DPtypes.ANALYZE :
            if (!getInput) return (<ProductMessage {...{menu, singleDownload, makeDropDown, isWorkingState, message}}/>);
            return (<ServiceDescriptorPanel {...{
                serDefParams, serviceDefRef, title, makeDropDown, sRegion,
                setSearchParams: (params) => dispatchSetSearchParams({dpId,activeMenuLookupKey,menuKey,params}),
            }} />);
        case DPtypes.MESSAGE :
        case DPtypes.PROMISE :
            return <AdvancedMessage {...{dpId, dataProductsState, noProductMessage, doResetButton }}/>;
        case DPtypes.DOWNLOAD_MENU_ITEM :
            return (<ProductMessage {...{menu, singleDownload, makeDropDown, message}} />);
        case DPtypes.IMAGE :
            return (<MultiProductChoice {...{dpId,makeDropDown,metaDataTableId, imageViewerId,whatToShow:SHOW_IMAGE}}/>);
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

const makeChartTableLookupKey= (activeItemLookupKey, fileMenuKey) => `${activeItemLookupKey}-charTable-${fileMenuKey}`;
