/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useContext, useState, useEffect} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {flux} from '../../Firefly.js';
import {NewPlotMode, dispatchAddViewer, dispatchViewerUnmounted, WRAPPER, META_VIEWER_ID, IMAGE,
        getMultiViewRoot, getViewer, dispatchUpdateCustom} from '../MultiViewCntlr.js';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {ImageMetaDataToolbar} from './ImageMetaDataToolbar.jsx';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import { ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {DropDownToolbarButton} from '../../ui/DropDownToolbarButton.jsx';
import {download} from '../../util/WebUtil.js';
import {SINGLE} from '../MultiViewCntlr';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';


function getDisplayType(viewer) {// todo
    if (!viewer) return 'unsupported';
    return get(viewer,'customData.displayType', 'unsupported');
}


function activeMenuItem(menuItem, menu, viewerId) {
    switch (menuItem.type) {

        case 'image':
            if (menuItem.activate) {
                dispatchUpdateCustom(viewerId, {displayType: 'images', menu});
                menuItem.activate();
            }
            else {
                dispatchUpdateCustom(viewerId, {displayType: 'message', 'message': 'Image not supported',menu});
            }
            break;
        case 'table':
            if (menuItem.activate) {
                dispatchUpdateCustom(viewerId, {displayType: 'table', menu});
                menuItem.activate();
            }
            else {
                dispatchUpdateCustom(viewerId, {displayType: 'message', 'message': 'Table not supported',menu});
            }
            break;
        case 'xyplot':
            if (menuItem.activate) {
                dispatchUpdateCustom(viewerId, {displayType: 'xyplot', menu});
                menuItem.activate();
            }
            else {
                dispatchUpdateCustom(viewerId, {displayType: 'message', 'message': 'chart not supported',menu});
            }
            break;
        case 'png':
            dispatchUpdateCustom(viewerId, {displayType: 'png', url: menuItem.url, menu});
            break;
        case 'download':
            download(menuItem.url);
            console.log('MultiProductViewer: download something');
            break;
    }
}



function OtherOptionsDropDown({menu, viewerId}) {
    return (
        <SingleColumnMenu>
            {menu.map( (menuItem, idx) => {
                return (
                    <ToolbarButton text={menuItem.name} tip={`${menuItem.semantics} - ${menuItem.url}`}
                                   enabled={true} horizontal={false} key={'otherOptions-'+idx}
                                   onClick={() => activeMenuItem(menuItem, menu, viewerId)}/>
                );
            })}
        </SingleColumnMenu>
    );
}

OtherOptionsDropDown.propTypes= {
    viewerId : PropTypes.string.isRequired,
    menu : PropTypes.array
};


function getMakeDropdown(menu, viewerId) {
    return () => {
        return (
            <DropDownToolbarButton text={'More...'}
                                   tip='Other data to display'
                                   enabled={true} horizontal={true}
                                   visible={true}
                                   additionalStyle={{paddingRight:20}}
                                   hasHorizontalLayoutSep={false}
                                   dropDown={<OtherOptionsDropDown menu={menu} viewerId={viewerId}/>} />

            );

    };
}


export const MultiProductViewer= memo(({viewerId, imageMetaViewerId=META_VIEWER_ID,metaDataTableId,tableGroupViewerId }) => {

    const {renderTreeId} = useContext(RenderTreeIdCtx);
    const [viewer, setViewer] = useState(getViewer(getMultiViewRoot(),viewerId));
    const displayType= getDisplayType(viewer);
    const activate= get(viewer,'customData.activate');


    useEffect(() => {
        dispatchAddViewer(viewerId, NewPlotMode.none, WRAPPER,true, renderTreeId, SINGLE);
        dispatchAddViewer(imageMetaViewerId, NewPlotMode.none, IMAGE,true, renderTreeId, SINGLE);
        const removeFluxListener= flux.addListener(()=> {
            const newViewer= getViewer(getMultiViewRoot(),viewerId);
            if (newViewer!==viewer) setViewer(newViewer);
        });
        return () => {
            removeFluxListener();
            dispatchViewerUnmounted(viewerId);
            dispatchViewerUnmounted(imageMetaViewerId);
        };
    }, [viewerId]);

    useEffect(() => activate && activate(), [activate]);

    if (!viewer) return false;
    const {menu,message,url}= viewer.customData;
    let result;

    switch (displayType) {
        case 'images' :
            result= ( <MultiImageViewer viewerId= {imageMetaViewerId} insideFlex={true}
                                        canReceiveNewPlots={NewPlotMode.none.key}
                                        tableId={metaDataTableId} controlViewerMounting={false}
                                        makeDropDown={menu && getMakeDropdown(menu,viewerId) }
                                        Toolbar={ImageMetaDataToolbar}/>
            );
            break;
        case 'message' :
            result= (
                <div style={{display:'flex', flexDirection: 'column', background: '#c8c8c8', width:'100%', height:'100%'}}>
                    <div style={{height:menu?30:0}}>
                        {menu && getMakeDropdown(menu,viewerId)()}
                    </div>
                    <div style={{alignSelf:'center', fontSize:'14pt', paddingTop:40}}>{message}</div>
                </div>
            );
            break;
        case 'table' :
            result= (<TablesContainer tbl_group= {tableGroupViewerId} mode='both' closeable={false} expandedMode={false} />);
            break;
        case 'png' :
            result= (
                <div style={{display:'flex', flexDirection: 'column', background: '#c8c8c8', width:'100%', height:'100%'}}>
                    <div style={{height:30, width:'100%'}}>
                        {getMakeDropdown(menu,viewerId)()}
                    </div>
                    <div style={{overflow:'auto', display:'flex', justifyContent: 'center', alignItem:'center'}}>
                        <img src={url} alt={url} style={{maxWidth:'100%', flexGrow:0, flexShrink:0 }}/>
                    </div>
                </div>
        );
            break;
        default:
            result= ( <div>{'Unsupported or empty display type. Contact Support. Please check DataProductsFactory'}</div> );
            break;

    }

    return result;
});





MultiProductViewer.propTypes= {
    viewerId : PropTypes.string.isRequired,
    imageMetaViewerId: PropTypes.string.isRequired,
    tableGroupViewerId: PropTypes.string.isRequired,
    metaDataTableId : PropTypes.string
};


MultiProductViewer.contextType= RenderTreeIdCtx;

