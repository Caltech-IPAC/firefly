/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {useEffect,useContext} from 'react';
import {get,once} from 'lodash';
import PropTypes from 'prop-types';
import {MultiImageViewer} from './MultiImageViewer';
import {
    dispatchAddViewer, dispatchViewerUnmounted, getMultiViewRoot, getViewer, IMAGE, NewPlotMode, SINGLE,
} from '../MultiViewCntlr';
import {COVERAGE_WATCH_CID, startCoverageWatcher, COVERAGE_FAIL} from '../saga/CoverageWatcher.js';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar';
import {DEFAULT_COVERAGE_VIEWER_ID, getActivePlotView} from '../PlotViewUtil';
import {visRoot} from '../ImagePlotCntlr';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {getActiveTableId, getBooleanMetaEntry, getTblById, getTblIdsByGroup} from '../../tables/TableUtil.js';
import {hasCoverageData} from '../../util/VOAnalyzer.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {MetaConst} from '../../data/MetaConst.js';
import {getComponentState} from '../../core/ComponentCntlr.js';


const startWatcher= once((viewerId) => {
    setTimeout(() => {
        const coverageOps= get(getAppOptions(), 'coverage',{});
        startCoverageWatcher({...coverageOps, viewerId, ignoreCatalogs:true});
    },1);
});

const isCoverageFail= (covState,tbl_id) => covState.find( (e) => e.tbl_id===tbl_id)?.status===COVERAGE_FAIL;

const getActiveOrFirstTblId= () => getActiveTableId() || getTblIdsByGroup()[0];

const anyTblHasCoverage= (covState) =>
    getTblIdsByGroup().some( (tbl_id) => hasCoverageData(tbl_id) && !isCoverageFail(covState,tbl_id));

function makeNovCovMsg(covState, baseNoCovMsg, tbl_id) {
    const titleStr= getTblById(tbl_id)?.request?.META_INFO?.title;
    return (anyTblHasCoverage(covState) && titleStr) ?
        `${baseNoCovMsg} for ${titleStr}, other tables have coverage` : baseNoCovMsg;
}


export function CoverageViewer({viewerId=DEFAULT_COVERAGE_VIEWER_ID,insideFlex=true, noCovMessage='No coverage available',
                                workingMessage='Working...', noCovStyle={}}) {

    startWatcher(viewerId);
    const pv        = useStoreConnector(() => getActivePlotView(visRoot()));
    const tbl_id    = useStoreConnector(() => getActiveOrFirstTblId());
    const isFetching= useStoreConnector(() => getTblById(getActiveOrFirstTblId())?.isFetching ?? false);
    const covState  = useStoreConnector(() => getComponentState(COVERAGE_WATCH_CID,[]));

    useEffect(() => {
        dispatchAddViewer(viewerId, NewPlotMode.replace_only, IMAGE, true, renderTreeId, SINGLE);
        return () => dispatchViewerUnmounted(viewerId);
    }, [viewerId]);



    const hasPlots = Boolean(getViewer(getMultiViewRoot(), viewerId)?.itemIdAry.length);
    const {renderTreeId} = useContext(RenderTreeIdCtx);
    const forceShow= getBooleanMetaEntry(tbl_id,MetaConst.COVERAGE_SHOWING,false);
    const tblHasCoverage= hasCoverageData(tbl_id);


    if (hasPlots && (tblHasCoverage || forceShow)) {
        return (
            <div style={{display:'flex', flexDirection:'column', width:'100%', background:'rgb(200, 200, 200)'}}>
                <MultiImageViewer viewerId={viewerId}
                                  insideFlex={insideFlex}
                                  canReceiveNewPlots={NewPlotMode.replace_only.key}
                                  controlViewerMounting={false}
                                  Toolbar={MultiViewStandardToolbar}/>
            </div>
        );
    }
    else {
        let msg;
        if (tblHasCoverage || isFetching) {
            msg= isCoverageFail(covState,tbl_id) ? makeNovCovMsg(noCovMessage,tbl_id) : workingMessage;
        }
        else if (forceShow) {
            msg= anyTblHasCoverage(covState) ? workingMessage : makeNovCovMsg(covState, noCovMessage,tbl_id);
        }
        else {
            msg= makeNovCovMsg(covState, noCovMessage,tbl_id);
        }
        return (
            <div style={{...{background: '#c8c8c8', paddingTop:35, width:'100%',textAlign:'center',fontSize:'14pt'},...noCovStyle}}>
                {msg}</div>
        );
    }
}



CoverageViewer.propTypes= {
    viewerId: PropTypes.string,
    noCovMessage: PropTypes.string,
    workingMessage: PropTypes.string,
    insideFlex: PropTypes.bool,
    noCovStyle: PropTypes.object
};
