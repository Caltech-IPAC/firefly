/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Box, Stack} from '@mui/joy';
import {isFunction} from 'lodash';
import {bool, string} from 'prop-types';
import React, {memo, useContext, useEffect, useState} from 'react';
import {
    dataProductRoot, dispatchInitDataProducts, dispatchSetSearchParams, dispatchUpdateActiveKey, getActivateParams,
    getActiveFileMenuKey, getActiveFileMenuKeyByKey, getDataProducts, getSearchParams, getServiceParamsAry,
} from '../../../metaConvert/DataProductsCntlr.js';
import {DPtypes, SHOW_CHART, SHOW_IMAGE, SHOW_TABLE} from '../../../metaConvert/DataProductsType.js';
import {ServiceDescriptorPanel} from '../../../ui/dynamic/ServiceDescriptorPanel.jsx';
import {RenderTreeIdCtx} from '../../../ui/RenderTreeIdCtx.jsx';
import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {
    dispatchAddViewer, dispatchViewerUnmounted, getLayoutDetails, getMultiViewRoot, GRID, IMAGE, NewPlotMode,
    PLOT2D, SINGLE
} from '../../MultiViewCntlr.js';
import {createMakeDropdownFunc} from './DPDropdown.jsx';
import {AdvancedMessage, ProductMessage} from './MPMessages.jsx';
import {MultiProductChoice} from './MultiProductChoice.jsx';

const getInitList= () => dataProductRoot().map( ({dpId}) => dpId);


export function MultiProductViewer ({viewerId= 'DataProductsType', metaDataTableId, ...props}) {
    const [initList, setInitList]= useState(getInitList());
    const dpId= viewerId;
    const activateParams=  getActivateParams(dataProductRoot(),dpId);
    useEffect(() => {
        if (initList.includes(viewerId)) return;
        dispatchInitDataProducts(viewerId);
        setInitList(getInitList());
    },[viewerId,initList]);

    const layoutDetails= useStoreConnector(() => {
        const activateParams=  getActivateParams(dataProductRoot(),dpId);
        return {
            image:getLayoutDetails(getMultiViewRoot(), activateParams.imageViewerId, metaDataTableId),
            chart:getLayoutDetails(getMultiViewRoot(), activateParams.chartViewerId, metaDataTableId)
        };
    } );

    return initList.includes(viewerId) && <MultiProductViewerImpl {...{dpId, activateParams, metaDataTableId, layoutDetails, ...props}} />;
}

MultiProductViewer.propTypes= {
    viewerId : string,
    metaDataTableId : string,
    enableExtraction: bool,
};

const MultiProductViewerImpl= memo(({ dpId, activateParams, metaDataTableId, noProductMessage='No Data Available',
                                        factoryKey, enableExtraction=false}) => {
    const {renderTreeId} = useContext(RenderTreeIdCtx);
    const [currentCTIChoice, setCurrentCTIChoice] = useState(undefined);
    const [lookupKey, setLookKey] = useState(undefined);
    const dataProductsState = useStoreConnector((old) => {
            const newDp= getDataProducts(dataProductRoot(),dpId)||{};
            return (!old || (newDp!==old && newDp.displayType && newDp.displayType!==DPtypes.DOWNLOAD)) ? newDp : old;
        }, [dpId, metaDataTableId, factoryKey]);
    const serviceParamsAry = useStoreConnector(() => getServiceParamsAry(dataProductRoot(),dpId));

    const {imageViewerId,chartViewerId}= activateParams;

    const {displayType=DPtypes.UNSUPPORTED, menu,fileMenu, isWorkingState, menuKey,
        activeMenuLookupKey,singleDownload= false, chartTableDefOption=SHOW_CHART,
        imageActivate, allowsInput=false, serDef= undefined}= dataProductsState;
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

    const doResetButton= displayType!==DPtypes.ANALYZE && !isWorkingState && Boolean(searchParams || serDef?.serDefParams?.some( (sdp) => !sdp.ref));

    const getInput= displayType===DPtypes.ANALYZE && allowsInput && !searchParams;
    const showMenu= !singleDownload || (singleDownload && (displayType===DPtypes.DOWNLOAD_MENU_ITEM || displayType===DPtypes.MESSAGE));
    const doMakeDropdown= menu?.length || fileMenu?.menu?.length || extraction;

    const makeDropDown= doMakeDropdown ?
        createMakeDropdownFunc( {dpId, dataProductsState, showMenu, showRedoSearchButton:doResetButton,
            extraction: !getInput && extraction}) : undefined;

    return (
        <ViewerRender {...{dpId, dataProductsState, noProductMessage, metaDataTableId,makeDropDown,
                              setCurrentCTIChoice, ctiChoice, ctLookupKey, activateParams,
                              getInput, doResetButton, factoryKey}} />
    );
});


function ViewerRender({dpId, dataProductsState, noProductMessage, metaDataTableId, makeDropDown, activateParams,
                          setCurrentCTIChoice, ctiChoice, ctLookupKey, getInput, doResetButton, factoryKey}) {
    const {displayType=DPtypes.UNSUPPORTED, menu, singleDownload, isWorkingState, message, activeMenuLookupKey,
        menuKey, imageActivate, url, serDef, serviceDefRef, sRegion, name:title, standardID }= dataProductsState;
    const {imageViewerId,chartViewerId,tableGroupViewerId}=  activateParams;
    switch (displayType) {
        case DPtypes.ANALYZE :
            if (!getInput) return (<ProductMessage {...{menu, singleDownload, makeDropDown, isWorkingState, message}}/>);
            return (<ServiceDescriptorPanel {...{
                serDef, serviceDefRef, title, makeDropDown, sRegion, standardID,
                setSearchParams: (params) => dispatchSetSearchParams({dpId,activeMenuLookupKey,menuKey,params}),
            }} />);
        case DPtypes.MESSAGE :
        case DPtypes.PROMISE :
            return <AdvancedMessage {...{dpId, dataProductsState, noProductMessage, doResetButton, makeDropDown}}/>;
        case DPtypes.DOWNLOAD_MENU_ITEM :
            return (<ProductMessage {...{menu, singleDownload, makeDropDown, message}} />);
        case DPtypes.IMAGE :
            return (<MultiProductChoice {...{dataProductsState,dpId,makeDropDown,metaDataTableId, imageViewerId,whatToShow:SHOW_IMAGE, factoryKey}}/>);
        case DPtypes.TABLE :
            return (<MultiProductChoice {...{dataProductsState,dpId,makeDropDown,tableGroupViewerId,whatToShow:SHOW_TABLE, factoryKey}}/>);
        case DPtypes.CHART :
            return (<MultiProductChoice {...{dataProductsState,dpId,makeDropDown,chartViewerId,whatToShow:SHOW_CHART, factoryKey}}/>);
        case DPtypes.CHOICE_CTI :
            return (
                <MultiProductChoice { ...{
                    dataProductsState,dpId,
                    makeDropDown,chartViewerId, imageViewerId:imageActivate?imageViewerId:'',
                    metaDataTableId, tableGroupViewerId,whatToShow:ctiChoice, ctLookupKey,mayToggle:true,
                    factoryKey,
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
    <Stack {...{direction:'column', width:'100%', height:'100%'}}>
        {makeDropDown &&
            <Box style={{height:30, width:'100%'}}> {makeDropDown()} </Box>}
        <Stack direction='row' alignItems='center' justifyContent='center' overflow='auto'>
            <img src={url} alt={url} style={{maxWidth:'100%', flexGrow:0, flexShrink:0, objectFit: 'contain' }}/>
        </Stack>
    </Stack> );

const makeChartTableLookupKey= (activeItemLookupKey, fileMenuKey) => `${activeItemLookupKey}-charTable-${fileMenuKey}`;
