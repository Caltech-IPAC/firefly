/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useContext, useState, useEffect} from 'react';
import PropTypes from 'prop-types';
import {get, isFunction, isArray, once} from 'lodash';
import {flux} from '../../Firefly.js';
import {NewPlotMode, dispatchAddViewer, dispatchViewerUnmounted, WRAPPER, META_VIEWER_ID, IMAGE,
        getMultiViewRoot, getViewer, PLOT2D, SINGLE, GRID} from '../MultiViewCntlr.js';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {ImageMetaDataToolbar} from './ImageMetaDataToolbar.jsx';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {MultiChartViewer} from '../../charts/ui/MultiChartViewer';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {CompleteButton} from '../../ui/CompleteButton';
import {DropDownToolbarButton} from '../../ui/DropDownToolbarButton.jsx';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {DPtypes, SHOW_CHART, SHOW_TABLE} from '../../metaConvert/DataProductsType';
import {
    dataProductRoot,
    dispatchActivateFileMenuItem,
    dispatchActivateMenuItem,
    getActiveFileMenuKey,
    getActiveMenuKey,
    getDataProducts,
    doDownload,
    getActiveFileMenuKeyByKey,
    dispatchUpdateActiveKey, dispatchInitDataProducts, getActivateParams, isInitDataProducts
} from '../../metaConvert/DataProductsCntlr';
import {RadioGroupInputFieldView} from '../../ui/RadioGroupInputFieldView';
import {dispatchChangeActivePlotView} from '../ImagePlotCntlr';


function FileMenuDropDown({fileMenu, dpId}) {
    const activeFileMenuKey= getActiveFileMenuKey(dpId,fileMenu);
    return (
        <SingleColumnMenu>
            {fileMenu.menu.map( (fileMenuItem, idx) => {
                return (
                    <ToolbarButton text={fileMenuItem.name} tip={fileMenuItem.name}
                                   enabled={true} horizontal={false} key={'fileMenuOptions-'+idx}
                                   hasCheckBox={true} checkBoxOn={fileMenuItem.menuKey===activeFileMenuKey}
                                   onClick={() => dispatchActivateFileMenuItem({dpId,fileMenu,newActiveFileMenuKey:fileMenuItem.menuKey})}/>
                );
            })}
        </SingleColumnMenu>
    );
}
FileMenuDropDown.propTypes= {
    dpId : PropTypes.string.isRequired,
    fileMenu : PropTypes.object,
};

function OtherOptionsDropDown({menu, dpId, activeMenuLookupKey}) {
    const activeMenuKey= getActiveMenuKey(dpId, activeMenuLookupKey);
    return (
        <SingleColumnMenu>
            {menu.map( (menuItem, idx) => {
                return (
                    <ToolbarButton text={menuItem.name} tip={`${menuItem.semantics} - ${menuItem.url}`}
                                   enabled={true} horizontal={false} key={'otherOptions-'+idx}
                                   hasCheckBox={true} checkBoxOn={menuItem.menuKey===activeMenuKey}
                                   onClick={() => dispatchActivateMenuItem(dpId,menuItem.menuKey) }/>
                );
            })}
        </SingleColumnMenu>
    );
}

OtherOptionsDropDown.propTypes= {
    dpId : PropTypes.string.isRequired,
    menu : PropTypes.array,
    activeMenuLookupKey : PropTypes.string,
};


function getMakeDropdown(menu, fileMenu, dpId,activeMenuLookupKey) {
    const hasFileMenu= get(fileMenu,'menu.length',0)>1;
    const hasMenu= menu && menu.length>0;
    if (!hasMenu && !hasFileMenu) return undefined;
    return () => {
        return (
            <div style={{display:'flex', flexDirection:'row'}}>

                {!hasMenu ?   <div style={{width: 50,height:1 }}/> :
                <DropDownToolbarButton
                       text={'More'}
                       tip='Other data to display'
                       enabled={true} horizontal={true}
                       visible={true}
                       additionalStyle={{paddingRight:20}}
                       hasHorizontalLayoutSep={false}
                       useDropDownIndicator={true}
                       dropDown={<OtherOptionsDropDown {...{menu, dpId, activeMenuLookupKey}} />} /> }

                {hasFileMenu &&
                <DropDownToolbarButton
                    text={'File Contents'}
                    tip='Other data in file'
                    enabled={true} horizontal={true}
                    visible={true}
                    additionalStyle={{paddingRight:20}}
                    hasHorizontalLayoutSep={false}
                    useDropDownIndicator={true}
                    dropDown={<FileMenuDropDown {...{fileMenu, dpId}} />} />
                }

            </div>
        );

    };
}

const makeChartTableLookupKey= (activeItemLookupKey, fileMenuKey) => `${activeItemLookupKey}-charTable-${fileMenuKey}`;

// const getActiveFileMenuDP= (fileMenu) => fileMenu?.menu.find( (m) => m.menuKey===fileMenu.activeFileMenuKey);

export const MultiProductViewer= memo(({ viewerId='DataProductsType', metaDataTableId}) => {

    const dpId= viewerId;
    !isInitDataProducts(dataProductRoot(), dpId) && dispatchInitDataProducts(dpId);
    const {renderTreeId} = useContext(RenderTreeIdCtx);
    const [viewer, setViewer] = useState(getViewer(getMultiViewRoot(),viewerId));
    const [dataProductsState, setDataProductsState] = useState(getDataProducts(dataProductRoot(),dpId));
    const {imageViewerId,chartViewerId,tableGroupViewerId}=  getActivateParams(dataProductRoot(),dpId);
    const {displayType='unsupported', menu,fileMenu,message,url, isWorkingState, menuKey,
        activate,activeMenuLookupKey,singleDownload= false, chartTableDefOption=SHOW_CHART}= dataProductsState;


    useEffect(() => {
        dispatchAddViewer(viewerId, NewPlotMode.none, WRAPPER,true, renderTreeId, SINGLE);
        dispatchAddViewer(imageViewerId, NewPlotMode.none, IMAGE,true, renderTreeId, SINGLE, true,true);
        dispatchAddViewer(chartViewerId, NewPlotMode.none, PLOT2D,true, renderTreeId, GRID, false, true);
        const removeFluxListener= flux.addListener(()=> {
            const newViewer= getViewer(getMultiViewRoot(),viewerId);
            if (newViewer!==viewer) setViewer(newViewer);
            const newDataProducts= getDataProducts(dataProductRoot(),dpId);
            if (newDataProducts && newDataProducts.displayType && newDataProducts.displayType!==DPtypes.DOWNLOAD && newDataProducts!==dataProductsState) {
                setDataProductsState(newDataProducts);
            }
        });
        return () => {
            removeFluxListener();
            dispatchViewerUnmounted(viewerId);
            dispatchViewerUnmounted(imageViewerId);
            dispatchViewerUnmounted(chartViewerId);
        };
    }, [viewerId]);

    useEffect(() => {
        const deActivate= activate?.();
        return () => {
            isFunction(deActivate) && deActivate();
        };
    }, [activate]);


    useEffect(() => {
        displayType!==DPtypes.IMAGE && dispatchChangeActivePlotView(undefined);
    } );




    if (!viewer) return false;
    const makeDropDown= (!singleDownload || menu.length>1) && getMakeDropdown(menu,fileMenu,dpId, activeMenuLookupKey);

    let result;
    switch (displayType) {
        case DPtypes.IMAGE :
            result= ( <MultiImageViewer viewerId= {imageViewerId} insideFlex={true}
                                        canReceiveNewPlots={NewPlotMode.none.key}
                                        tableId={metaDataTableId} controlViewerMounting={false}
                                        handleInlineToolsWhenSingle={false}
                                        makeDropDown={makeDropDown}
                                        Toolbar={ImageMetaDataToolbar}/>
            );
            break;
        case DPtypes.ANALYZE :
        case DPtypes.MESSAGE :
        case DPtypes.PROMISE :

            let dMsg= singleDownload  && menu[0].name;
            if (dMsg && menu[0].fileType) dMsg= `${dMsg}, type: ${menu[0].fileType}`;
            result= (
                <div style={{display:'flex', flexDirection: 'column', background: '#c8c8c8', width:'100%', height:'100%'}}>
                    <div style={{height:menu?30:0}}>
                        {makeDropDown && makeDropDown()}
                    </div>
                    <div style={{display:'flex', flexDirection: 'row', alignSelf:'center', paddingTop:40}}>
                        {isWorkingState ?
                            <div style={{width:20, height:20, marginRight: 10}} className='loading-animation' /> : ''}
                        <div style={{alignSelf:'center', fontSize:'14pt'}}>{message}</div>
                    </div>
                    {
                        singleDownload && isArray(menu) && menu.length &&
                        <CompleteButton style={{alignSelf:'center', paddingTop:25 }} text={dMsg}
                                        onSuccess={() => doDownload(menu[0].url)}/>
                    }
                </div>
            );
            break;
        case DPtypes.TABLE :
            result= (<MultiProductChartTable {...{dpId,makeDropDown,tableGroupViewerId,whatToShow:SHOW_TABLE}}/>);
            break;
        case DPtypes.CHART :
            result= (<MultiProductChartTable {...{dpId,makeDropDown,chartViewerId,whatToShow:SHOW_CHART}}/>);
            break;
        case DPtypes.CHART_TABLE :
            const lookupKey= fileMenu?.activeItemLookupKey ?? '';
            const ctLookupKey= makeChartTableLookupKey(lookupKey,menuKey || getActiveFileMenuKey(dpId,fileMenu));
            const whatToShow= lookupKey ?
                (getActiveFileMenuKeyByKey(dpId,ctLookupKey) || chartTableDefOption): chartTableDefOption;
            result= (<MultiProductChartTable {
                ...{dpId,makeDropDown,chartViewerId,tableGroupViewerId,whatToShow,ctLookupKey,mayToggle:true}}/>);
            break;
        case DPtypes.PNG :
            result= (
                <div style={{display:'flex', flexDirection: 'column', background: '#c8c8c8', width:'100%', height:'100%'}}>
                    {makeDropDown &&  <div style={{height:30, width:'100%'}}>
                        {makeDropDown()}
                    </div>}
                    <div style={{overflow:'auto', display:'flex', justifyContent: 'center', alignItem:'center'}}>
                        <img src={url} alt={url} style={{maxWidth:'100%', flexGrow:0, flexShrink:0 }}/>
                    </div>
                </div>
            );
            break;
        default:
            result= ( <div/> );
            break;

    }

    return result;
});


MultiProductViewer.propTypes= {
    viewerId : PropTypes.string,
    metaDataTableId : PropTypes.string,
};

const chartTableOptions= [
    {label: 'Table', value: SHOW_TABLE},
    {label: 'Chart', value: SHOW_CHART}
    ];

function MultiProductChartTable({dpId,makeDropDown, chartViewerId,
                                    tableGroupViewerId,whatToShow,
                                    ctLookupKey=undefined, mayToggle=false}) {

    const [ts, setTS] = useState(whatToShow);
    const [lookupKey, setLookKey] = useState(ctLookupKey);
    let result;

    useEffect(() => {
        setTS(whatToShow);
        setLookKey(ctLookupKey);
    }, [whatToShow,ctLookupKey]);


    const toolbar= (
        <div style={{display:'flex', flexDirection: 'row', alignItems:'center', height:30}}>
            {makeDropDown && <div style={{height:30}}> {makeDropDown()} </div>}
            {mayToggle && <RadioGroupInputFieldView wrapperStyle={{paddingLeft:20}}
                                      options={chartTableOptions}  value={ts} buttonGroup={true}
                                      onChange={(ev) => {
                                          setTS(ev.target.value);
                                          dispatchUpdateActiveKey({dpId, activeFileMenuKeyChanges:{[ctLookupKey]:ev.target.value}});
                                      }} />}
        </div>
    );

    if (ts===SHOW_CHART) {
        result= (
            <div style={{ width:'100%', height:'calc(100% - 30px)', background: '#c8c8c8'}}>
                {toolbar}
                <MultiChartViewer viewerId= {chartViewerId} closeable={false}
                                  canReceiveNewItems ={NewPlotMode.none.key} />
            </div>
        );
    }
    else {
        result= (
            <div style={{display:'flex', flexDirection: 'column', background: '#c8c8c8', width:'100%', height:'100%'}}>
                {toolbar}
                <TablesContainer tbl_group= {tableGroupViewerId} mode='both' closeable={false} expandedMode={false} />
            </div>
        );
    }
    return result;

}



MultiProductViewer.contextType= RenderTreeIdCtx;
