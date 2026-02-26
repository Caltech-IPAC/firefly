/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Button, Stack, Typography} from '@mui/joy';
import React, {useEffect} from 'react';
import PropTypes from 'prop-types';

import {dispatchNotifyRemoteAppReady, dispatchOnAppReady, dispatchSetMenu} from '../../core/AppDataCntlr.js';
import {dispatchHideDropDown, getLayouInfo, SHOW_DROPDOWN} from '../../core/LayoutCntlr.js';
import {ALERT, alertManager} from './AlertManager.js';
import {AlertResultView} from './AlertResultView.jsx';
import {makeBannerTitle} from '../../ui/Banner.jsx';
import {getActionFromUrl} from '../../core/History.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {getWorkspaceConfig, initWorkspace} from '../../visualize/WorkspaceCntlr.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {setIf as setIfUndefined} from '../../util/WebUtil.js';
import {flux} from '../../core/ReduxFlux.js';
import App from 'firefly/ui/App.jsx';
import {cloneDeep} from 'lodash/lang.js';
import {showInfoPopup} from 'firefly/ui/PopupUtil';
import {makeFileRequest} from 'firefly/tables/TableRequestUtil';
import {upload} from '../../rpc/CoreServices.js';
import WebPlotRequest from 'firefly/visualize/WebPlotRequest';
import RangeValues from 'firefly/visualize/RangeValues';
import {dispatchPlotImage} from 'firefly/visualize/ImagePlotCntlr';
import {findSingleAxisImages, getSelectedRows, makeSummaryModel} from 'firefly/ui/FileUploadProcessor';
import {IMAGES, TABLES} from 'firefly/ui/FileUploadUtil';
import {FileAnalysisType} from 'firefly/data/FileAnalysis';

const vFileKey = ALERT.FG_UPLOAD;
const DEFAULT_TITLE = 'Alert Viewer';

/**
 * Alert Viewer - Upload FITS files and view in custom layout
 * @param {Object} p
 * @param {Array} p.menu - menu items
 * @param {Array} p.dropdownPanels - dropdown panels
 * @param {string} p.appTitle - application title
 * @param {Object} p.slotProps - slot properties
 * @param {Object} p.appProps - additional app properties
 */
export function AlertViewer({menu, dropdownPanels=[], appTitle, slotProps, ...appProps}) {

    useEffect(() => {
        dispatchAddSaga(alertManager, {views: 'tables | images | xyplots'});
        getWorkspaceConfig() && initWorkspace();
        dispatchOnAppReady((state) => onReady({state, menu}));
    }, []);

    const {showTables, showImages, showXyPlots, error} = useStoreConnector(() => {
        const layoutInfo = getLayouInfo();
        return {
            showTables: layoutInfo?.showTables,
            showImages: layoutInfo?.showImages,
            showXyPlots: layoutInfo?.showXyPlots,
            error: layoutInfo?.error
        };
    });

    const title = makeBannerTitle(appTitle || DEFAULT_TITLE);

    const mSlotProps = cloneDeep(slotProps || {});
    setIfUndefined(mSlotProps,'drawer.allowMenuHide', false);
    setIfUndefined(mSlotProps,'banner.title', title);

    return (
        <App slotProps={mSlotProps}
             dropdownPanels={[...dropdownPanels, <UploadPanel {...{name:'AlertUpload'}}/>]}
             appTitle={appTitle} {...appProps}
        >
            <MainView {...{error, showTables, showImages, showXyPlots}}/>
        </App>
    );
}

AlertViewer.propTypes = {
    title: PropTypes.string,
    menu: PropTypes.arrayOf(PropTypes.object),
    appTitle: PropTypes.string,
    appIcon: PropTypes.element,
    footer: PropTypes.element,
    dropdownPanels: PropTypes.arrayOf(PropTypes.element),
    slotProps: PropTypes.object,
    style: PropTypes.object
};

const MainView = ({error, showTables, showImages, showXyPlots}) => {
    if (error) {
        return (
            <div style={{display: 'flex', width: '100%', marginTop: 20, justifyContent: 'center', alignItems: 'baseline'}}>
                <div style={{display: 'inline-flex', border: '1px solid #a3aeb9', padding:20, fontSize:'150%'}}>
                    <div>{error}</div>
                </div>
            </div>
        );
    }

    //Show result view if we have tables or images, otherwise landing page
    return (showTables || showImages || showXyPlots) ? <AlertResultView/> : <LandingView/>;
};

const LandingView = () => {
    return (
        <Stack sx={{width: 1, height: 1, justifyContent: 'center', alignItems: 'center', p: 4}}>
            <Stack spacing={3} sx={{maxWidth: 600, textAlign: 'center'}}>
                <Typography level='h1'>Alert Viewer</Typography>
                <Typography level='body-lg'>
                    Upload FITS files to view in a custom layout with 2 tables side-by-side and 3 images below.
                </Typography>
                <Typography level='body-md' color='neutral'>
                    Click the "Upload" tab above or drag and drop files here to get started.
                </Typography>
            </Stack>
        </Stack>
    );
};



function onReady({menu}) {
    if (menu) {
        dispatchSetMenu({menuItems: menu});
    }
    const {hasImages, hasTables} = getLayouInfo();
    if (!(hasImages || hasTables)) {
        const goto = getActionFromUrl() || {type: SHOW_DROPDOWN};
        if (goto) flux.process(goto);
    }
    dispatchNotifyRemoteAppReady();
}

export const UploadPanel = () =>{
    const instruction = 'Enter a URL to a FITS file to upload and view in the Alert Viewer.';

    return (


                <Stack {...{width:1, alignItems:'center'}}>
                    <Stack {...{ml:0, mt:4, spacing:3}}>
                        <Typography level='body-lg'>{instruction}</Typography>
                        <FieldGroup groupKey={vFileKey} keepState={true}>
                            <Stack {...{spacing:2}}>
                                <FileUpload
                                    fieldKey='fitsUrl'
                                    isFromURL={true}
                                    canDragDrop={false}
                                    fileAnalysis={false}
                                    initialState={{
                                        label: '',
                                        tooltip: 'Enter a URL to a FITS file'
                                    }}
                                />
                                <Stack direction='row' justifyContent='flex-end'>
                                    <Button onClick={onSearchSubmit}>Upload</Button>
                                </Stack>
                            </Stack>
                        </FieldGroup>
                    </Stack>
                </Stack>
    );
};

UploadPanel.propTypes = {};

function onSearchSubmit(request) {
    console.log('onSearchSubmit called with request: ', request);

    const fields = FieldGroupUtils.getGroupFields(vFileKey);
    const urlVal = fields?.fitsUrl?.value?.trim();

    if (!urlVal) {
        showInfoPopup('Please enter a URL.', 'Missing URL');
        return false;
    }

    const displayName = urlVal.split('/').pop() || 'URL FITS';
    loadFileAndHide(urlVal, displayName);
    return false; //don't hide dropdown yet - wait for loadFileAndHide to complete
}

async function loadFileAndHide(uploadPath, uploadedFileName) {
    try {
        // Upload and analyze the file
        const {cacheKey, analysisResult} = await upload(uploadPath, 'Details');

        const report = analysisResult ? JSON.parse(analysisResult) : null;
        if (!report?.parts) {
            console.error('No analysis or parts found');
            showInfoPopup('Could not analyze the uploaded file.', 'File Analysis Error');
            dispatchHideDropDown();
            return;
        }
        const acceptList = [IMAGES, TABLES];          // what AlertViewer supports
        const summaryModel = makeSummaryModel(report, '', acceptList); // tbl_id can be anything; key point is summaryTblId below is falsy
        const summaryTblId = '';

        // choose whether you want the Nx1 images to be treated as table/chart
        const singleAxisImageAsTable = true;

        const tableIdxs = getSelectedRows(FileAnalysisType.Table, summaryTblId, report, summaryModel, singleAxisImageAsTable) ?? [];
        const imageIdxs = getSelectedRows(FileAnalysisType.Image, summaryTblId, report, summaryModel, singleAxisImageAsTable) ?? [];

        //(optional) single-axis image indices the UI would “special case”
        const singleAxisIdxs = (findSingleAxisImages(report) ?? []).map(({index}) => index);

        //pick 2 table part indices
        const pickedTableIdxs = tableIdxs.slice(0, 2);
        if (pickedTableIdxs.length < 2) {
            showInfoPopup(`Need at least 2 tables. Found ${pickedTableIdxs.length}.`, 'Invalid File Format');
            dispatchHideDropDown();
            return;
        }

        //pick 3 image part indices
        //todo: if you want to treat single-axis images as NOT images, remove them here
        const pickedImageIdxs = imageIdxs
            .filter((idx) => !singleAxisIdxs.includes(idx)) //optional
            .slice(0, 3);

        if (pickedImageIdxs.length < 3) {
            showInfoPopup(`Need at least 3 images. Found ${pickedImageIdxs.length}.`, 'Invalid File Format');
            dispatchHideDropDown();
            return;
        }

        const getExtNum = (part, fallbackIdx) =>
            part?.fileLocationIndex ?? part?.index ?? fallbackIdx;

        //Load 2 tables into 2 dedicated viewers
        console.log('Loading tables...');
        for (let i = 0; i < 2; i++) {
            const partIdx = pickedTableIdxs[i];
            const part = report.parts.find((p) => (p?.index ?? p?.fileLocationIndex) === partIdx);
            const extNum = getExtNum(part, partIdx);
            const desc = part?.desc;
            const tblReq = makeFileRequest(
                desc || `${uploadedFileName} Table ${i + 1}`,
                cacheKey,
                null,
                {
                    tbl_id: i === 0 ? ALERT.TABLE_1_ID : ALERT.TABLE_2_ID,
                    pageSize: ALERT.TABLE_PAGESIZE,
                    META_INFO: {}
                }
            );
            tblReq.tbl_index = extNum;
            console.log(`Table ${i + 1} request:`, tblReq);
            /*dispatchTableSearch(
                tblReq,
                {removable: true, tbl_group: i === 0 ? ALERT.TABLE_GROUP_MAIN : ALERT.TABLE_GROUP_DETAILS}
            );*/
            if (i === 0) {
                dispatchTableSearch(tblReq, {removable: true, tbl_group: ALERT.TABLE_GROUP_MAIN});
            } else {
                dispatchTableSearch(tblReq, {removable: true, tbl_group: ALERT.TABLE_GROUP_DETAILS});
            }
            }

            //Load 3 images into 3 dedicated viewers

            const viewerIds = [ALERT.IMG_VIEWER_1, ALERT.IMG_VIEWER_2, ALERT.IMG_VIEWER_3];
            const plotIds = [ALERT.IMG_PLOT_1, ALERT.IMG_PLOT_2, ALERT.IMG_PLOT_3];

            for (let i = 0; i < 3; i++) {
                const partIdx = pickedImageIdxs[i];
                const part = report.parts[partIdx];
                const extNum = getExtNum(part, partIdx);
                const desc = part?.desc;

                const viewerId = viewerIds[i];
                const plotId = plotIds[i];

                const wpRequest = WebPlotRequest.makeFilePlotRequest(cacheKey);
                wpRequest.setInitialRangeValues(RangeValues.make2To10SigmaLinear());
                wpRequest.setPlotGroupId(viewerId);
                wpRequest.setMultiImageExts(`${extNum}`);
                wpRequest.setTitle(desc || `${uploadedFileName} [ext ${extNum}]`);

                dispatchPlotImage({plotId, wpRequest, viewerId, setNewPlotAsActive: i === 0});
            }

    } catch (error) {
        console.error('=== loadFileAndHide ERROR ===');
        console.error('Error loading file:', error);
        console.error('Error stack:', error.stack);
        showInfoPopup(`Error loading file: ${error.message}`, 'Load Error');
    }

    dispatchHideDropDown();
}