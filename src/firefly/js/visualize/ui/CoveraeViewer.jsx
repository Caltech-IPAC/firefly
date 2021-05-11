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
import {getActivePlotView} from '../PlotViewUtil';
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


export function CoverageViewer({viewerId='coverageImages',insideFlex=true, noCovMessage='No Coverage Available',
                                workingMessage='Working...', noCovStyle={}}) {

    startWatcher(viewerId);
    const [pv,tbl_id,isFetching,covState] = useStoreConnector(
        () => getActivePlotView(visRoot()),
        () => getActiveOrFirstTblId(),
        () => getTblById(getActiveOrFirstTblId())?.isFetching ?? false,
        () => getComponentState(COVERAGE_WATCH_CID,[]));


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
            <MultiImageViewer viewerId={viewerId}
                              insideFlex={insideFlex}
                              canReceiveNewPlots={NewPlotMode.replace_only.key}
                              controlViewerMounting={false}
                              Toolbar={MultiViewStandardToolbar}/>
        );
    }
    else {
        let msg= noCovMessage;
        if (tblHasCoverage || isFetching) {
            msg= isCoverageFail(covState,tbl_id) ? noCovMessage : workingMessage;
        }
        else if (forceShow) {
            msg= getTblIdsByGroup().some( (tbl_id) => hasCoverageData(tbl_id) && !isCoverageFail(covState,tbl_id))
                ? workingMessage : noCovMessage;
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
};
