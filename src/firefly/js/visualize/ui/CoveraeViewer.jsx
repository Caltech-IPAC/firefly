/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Stack, Typography} from '@mui/joy';
import React, {useEffect,useContext} from 'react';
import {get,once} from 'lodash';
import PropTypes from 'prop-types';
import {hasCoverageData} from '../../voAnalyzer/TableAnalysis.js';
import {visRoot} from '../ImagePlotCntlr';
import {MultiImageViewer} from './MultiImageViewer';
import {
    dispatchAddViewer, dispatchViewerUnmounted, getMultiViewRoot, getViewer, IMAGE, NewPlotMode, SINGLE,
} from '../MultiViewCntlr';
import {
    COVERAGE_WATCH_CID, startCoverageWatcher, COVERAGE_FAIL, COVERAGE_WAITING_MSG
} from '../saga/CoverageWatcher.js';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar';
import {DEFAULT_COVERAGE_VIEWER_ID, getActivePlotView} from '../PlotViewUtil';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {getActiveTableId, getBooleanMetaEntry, getTblById, getTblIdsByGroup} from '../../tables/TableUtil.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {MetaConst} from '../../data/MetaConst.js';
import {getComponentState} from '../../core/ComponentCntlr.js';


const startWatcher= once((viewerId) => {
    setTimeout(() => {
        const coverageOps= get(getAppOptions(), 'coverage',{});
        startCoverageWatcher({...coverageOps, viewerId, ignoreCatalogs:true});
    },1);
});

const isCoverageFail= (covState,tbl_id) => covState.find((e) => e.tbl_id === tbl_id)?.status === COVERAGE_FAIL;


const getActiveOrFirstTblId= () => getActiveTableId() || getTblIdsByGroup()[0];

const anyTblHasCoverage= (covState) =>
    getTblIdsByGroup().some( (tbl_id) => hasCoverageData(tbl_id) && !isCoverageFail(covState,tbl_id));

function makeNovCovMsg(covState, baseNoCovMsg, tbl_id) {
    const titleStr= getTblById(tbl_id)?.request?.META_INFO?.title;
    return (anyTblHasCoverage(covState) && titleStr) ?
        `${baseNoCovMsg} for ${titleStr}; other tables have coverage` : baseNoCovMsg;
}


export function CoverageViewer({viewerId=DEFAULT_COVERAGE_VIEWER_ID,noCovMessage='No coverage available',
                                workingMessage='Working...'}) {

    startWatcher(viewerId);
    const pv        = useStoreConnector(() => getActivePlotView(visRoot())); // do not remove, forces a rerender
    const tbl_id    = useStoreConnector(() => getActiveOrFirstTblId());
    const isFetching= useStoreConnector(() => getTblById(getActiveOrFirstTblId())?.isFetching ?? false);
    const covState  = useStoreConnector(() => getComponentState(COVERAGE_WATCH_CID,[]));
    const {msg:covWorkingMsg}= useStoreConnector(() => getComponentState(COVERAGE_WAITING_MSG,{msg:workingMessage}));

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
            <Stack height={1} width={1}>
                <MultiImageViewer viewerId={viewerId}
                                  insideFlex={true}
                                  canReceiveNewPlots={NewPlotMode.replace_only.key}
                                  controlViewerMounting={false}
                                  Toolbar={MultiViewStandardToolbar}/>
            </Stack>
        );
    }
    else {
        let msg;
        if (tblHasCoverage || isFetching) {
            msg= isCoverageFail(covState,tbl_id) ? makeNovCovMsg(covState,noCovMessage,tbl_id) : covWorkingMsg;
        }
        else if (forceShow) {
            msg= anyTblHasCoverage(covState) ? covWorkingMsg : makeNovCovMsg(covState,noCovMessage,tbl_id);
        }
        else {
            msg= makeNovCovMsg(covState,noCovMessage,tbl_id);
        }
        return (
            <Typography level='body-lg' sx={{...{pt:4.5, width:'100%',textAlign:'center',fontSize:'14pt'},}}>
                {msg}
            </Typography>
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
