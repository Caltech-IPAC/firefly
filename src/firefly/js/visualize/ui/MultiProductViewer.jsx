/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useContext, useState, useEffect} from 'react';
import PropTypes from 'prop-types';
import {get,isFunction,isArray} from 'lodash';
import {flux} from '../../Firefly.js';
import {NewPlotMode, dispatchAddViewer, dispatchViewerUnmounted, WRAPPER, META_VIEWER_ID, IMAGE,
        getMultiViewRoot, getViewer, PLOT2D, SINGLE} from '../MultiViewCntlr.js';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {ImageMetaDataToolbar} from './ImageMetaDataToolbar.jsx';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {MultiChartViewer} from '../../charts/ui/MultiChartViewer';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {CompleteButton} from '../../ui/CompleteButton';
import {DropDownToolbarButton} from '../../ui/DropDownToolbarButton.jsx';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {DPtypes} from '../../metaConvert/DataProductsType';
import {
    dataProductRoot,
    dispatchActivateFileMenuItem,
    dispatchActivateMenuItem,
    getActiveFileMenuKey,
    getActiveMenuKey,
    getDataProducts,
    doDownload,
    getActiveFileMenuKeyByKey,
    dispatchUpdateActiveKey
} from '../../metaConvert/DataProductsCntlr';
import {RadioGroupInputFieldView} from '../../ui/RadioGroupInputFieldView';


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
                    text={'In File'}
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

const SHOW_CHART='showChart';
const SHOW_TABLE='showTable';
const makeChartTableLookupKey= (activeItemLookupKey, fileMenuKey) => `${activeItemLookupKey}-charTable-${fileMenuKey}`;

export const MultiProductViewer= memo(({viewerId, imageMetaViewerId=META_VIEWER_ID,chartMetaViewerId='notsetup-chart', metaDataTableId,tableGroupViewerId }) => {

    const {renderTreeId} = useContext(RenderTreeIdCtx);
    const dpId= viewerId;
    const [viewer, setViewer] = useState(getViewer(getMultiViewRoot(),viewerId));
    const [dataProductsState, setDataProductsState] = useState(getDataProducts(dataProductRoot(),dpId));
    const {displayType='unsupported', menu,fileMenu,message,url, isWorkingState, menuKey,
        activate,activeMenuLookupKey,singleDownload= false}= dataProductsState;


    useEffect(() => {
        dispatchAddViewer(viewerId, NewPlotMode.none, WRAPPER,true, renderTreeId, SINGLE);
        dispatchAddViewer(imageMetaViewerId, NewPlotMode.none, IMAGE,true, renderTreeId, SINGLE);
        dispatchAddViewer(chartMetaViewerId, NewPlotMode.none, PLOT2D,true, renderTreeId, SINGLE);
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
            dispatchViewerUnmounted(imageMetaViewerId);
            dispatchViewerUnmounted(chartMetaViewerId);
        };
    }, [viewerId]);

    useEffect(() => {
        const deActivate= activate && activate();
        return () => {
            isFunction(deActivate) && deActivate();
        };
    }, [activate]);

    if (!viewer) return false;
    const makeDropDown= (!singleDownload || menu.length>1) && getMakeDropdown(menu,fileMenu,dpId, activeMenuLookupKey);

    let result;
    switch (displayType) {
        case DPtypes.IMAGE :
            result= ( <MultiImageViewer viewerId= {imageMetaViewerId} insideFlex={true}
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
                        <CompleteButton style={{alignSelf:'center', paddingTop:25 }} text={menu[0].name}
                                        onSuccess={() => doDownload(menu[0].url)}/>
                    }
                </div>
            );
            break;
        case DPtypes.TABLE :
            result= (<MultiProductChartTable {...{dpId,makeDropDown,tableGroupViewerId,SHOW_TABLE}}/>);
            break;
        case DPtypes.CHART :
            result= (<MultiProductChartTable {...{dpId,makeDropDown,chartMetaViewerId,SHOW_CHART}}/>);
            break;
        case DPtypes.CHART_TABLE :
            const lookupKey= get(fileMenu,'activeItemLookupKey','');
            const ctLookupKey= makeChartTableLookupKey(lookupKey,menuKey || getActiveFileMenuKey(dpId,fileMenu));
            const whatToShow= lookupKey ?
                getActiveFileMenuKeyByKey(dpId,ctLookupKey) || SHOW_CHART: SHOW_CHART;
            result= (<MultiProductChartTable {
                ...{dpId,makeDropDown,chartMetaViewerId,tableGroupViewerId,whatToShow,ctLookupKey,mayToggle:true}}/>);
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
    viewerId : PropTypes.string.isRequired,
    imageMetaViewerId: PropTypes.string.isRequired,
    tableGroupViewerId: PropTypes.string.isRequired,
    metaDataTableId : PropTypes.string,
    chartMetaViewerId: PropTypes.string
};

const chartTableOptions= [
    {label: 'Chart', value: SHOW_CHART},
    {label: 'Table', value: SHOW_TABLE}
    ];

function MultiProductChartTable({dpId,makeDropDown, chartMetaViewerId,
                                    tableGroupViewerId,whatToShow,ctLookupKey=undefined, mayToggle=false}) {

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
            <div style={{ width:'100%', background: '#c8c8c8'}}>
                {toolbar}
                <MultiChartViewer viewerId= {chartMetaViewerId} closeable={false}
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
