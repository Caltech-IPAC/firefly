/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Stack, Typography} from '@mui/joy';
import React, {useEffect} from 'react';
import PropTypes from 'prop-types';

import {ALERT, alertManager} from './AlertManager.js';
import {AlertResultView} from './AlertResultView.jsx';
import {makeBannerTitle} from '../../ui/Banner.jsx';
import {getActionFromUrl} from '../../core/History.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {getWorkspaceConfig, initWorkspace} from '../../visualize/WorkspaceCntlr.js';
import {setIf as setIfUndefined} from '../../util/WebUtil.js';
import App from 'firefly/ui/App.jsx';
import {cloneDeep} from 'lodash/lang.js';
import {showInfoPopup} from 'firefly/ui/PopupUtil';
import {makeFileRequest} from 'firefly/tables/TableRequestUtil';
import WebPlotRequest from 'firefly/visualize/WebPlotRequest';
import RangeValues from 'firefly/visualize/RangeValues';
import {dispatchPlotImage} from 'firefly/visualize/ImagePlotCntlr';
import {findSingleAxisImages, getSelectedRows, makeSummaryModel} from 'firefly/ui/FileUploadProcessor';
import {IMAGES, TABLES} from 'firefly/ui/FileUploadUtil';
import {FileAnalysisType} from 'firefly/data/FileAnalysis';
import {getField} from '../../fieldGroup/FieldGroupUtils';
import {LoadingMessage} from 'firefly/visualize/ui/FileUploadViewPanel';
import {flux} from 'firefly/core/ReduxFlux.js';
import {dispatchHideDropDown, getLayouInfo, SHOW_DROPDOWN} from 'firefly/core/LayoutCntlr.js';
import {dispatchNotifyRemoteAppReady, dispatchOnAppReady, dispatchSetMenu} from 'firefly/core/AppDataCntlr.js';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';

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
             dropdownPanels={[...dropdownPanels, <UploadPanel key='AlertUpload' name='AlertUpload' />]}
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

    //show result view if we have tables or images, otherwise landing page
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
    const {hasImages, hasTables, hasXyPlots} = getLayouInfo();
    if (!(hasImages || hasTables || hasXyPlots)) {
        const goto = getActionFromUrl() || {type: SHOW_DROPDOWN};
        if (goto) flux.process(goto);
    }
    dispatchNotifyRemoteAppReady();
}

let lastProcessedAnalysisResult = '';
export const UploadPanel = () => {
    const instruction = 'Enter a URL to a FITS file to upload and view in the Alert Viewer.';
    const [isLoading, setIsLoading] = React.useState(false);

    //pull the fitsUrl field from the store (this is how FileUploadViewPanel does it)
    const {fld} = useStoreConnector(() => {
        return { fld: getField(vFileKey, 'fitsUrl') };
    });

    const onLoading = (loading) => {setIsLoading(loading);};

    //when analysisResult exists, run loader and close dropdown
    useEffect(() => {
        const analysisResult = fld?.analysisResult;
        const message = fld?.message;

        if (message) {
            //upload or analysis failed
            showInfoPopup(message, 'Upload Error');
            return;
        }
        if (!analysisResult) return;

        //avoid re-running if store has same value
        if (analysisResult === lastProcessedAnalysisResult) return;
        lastProcessedAnalysisResult = analysisResult;

        let report;
        try {
            report = JSON.parse(analysisResult);
        } catch (e) {
            showInfoPopup('Upload succeeded but analysisResult is not valid JSON', 'Upload Error');
            return;
        }

        //file location/source on the server
        const fileLocation = fld?.value;
        if (!fileLocation) {
            showInfoPopup('Upload analysis returned, but no server file key was found (fitsUrl.value).', 'Upload Error');
            return;
        }

        const displayName = report?.fileName || fld?.displayValue || 'Uploaded FITS';

        //“load 2 tables + 3 images”
        loadFromReportAndHide(fileLocation, report, displayName);
    }, [fld?.analysisResult, fld?.message, fld?.value]);

    return (
        <Stack width={1} alignItems='center'>
            <Stack ml={0} mt={4} spacing={3} sx={{position: 'relative'}}>
                <Typography level='body-lg'>{instruction}</Typography>
                <FieldGroup groupKey={vFileKey} keepState={true}>
                    <Stack spacing={2}>
                        <FileUpload
                            fieldKey='fitsUrl'
                            isFromURL={true}
                            canDragDrop={false}
                            fileAnalysis={onLoading}
                            initialState={{
                                label: '',
                                tooltip: 'Enter a URL to a FITS file'
                            }}
                        />
                    </Stack>
                </FieldGroup>
                {isLoading && <LoadingMessage/>}
            </Stack>
        </Stack>
    );
};

UploadPanel.propTypes = {};

function loadFromReportAndHide(fileLocation, report, uploadedFileName) {
    try {
        if (!report?.parts) {
            showInfoPopup('Could not analyze the uploaded file.', 'File Analysis Error');
            dispatchHideDropDown();
            return;
        }

        const acceptList = [IMAGES, TABLES];
        const summaryModel = makeSummaryModel(report, '', acceptList);
        const summaryTblId = '';
        const singleAxisImageAsTable = true;

        const tableIdxs = getSelectedRows(FileAnalysisType.Table, summaryTblId, report, summaryModel, singleAxisImageAsTable) ?? [];
        const imageIdxs = getSelectedRows(FileAnalysisType.Image, summaryTblId, report, summaryModel, singleAxisImageAsTable) ?? [];

        const singleAxisIdxs = (findSingleAxisImages(report) ?? []).map(({index}) => index);

        const pickedTableIdxs = tableIdxs.slice(0, 2);
        if (pickedTableIdxs.length < 2) {
            showInfoPopup(`Need at least 2 tables. Found ${pickedTableIdxs.length}.`, 'Invalid File Format');
            dispatchHideDropDown();
            return;
        }

        const pickedImageIdxs = imageIdxs
            .filter((idx) => !singleAxisIdxs.includes(idx))
            .slice(0, 3);

        if (pickedImageIdxs.length < 3) {
            showInfoPopup(`Need at least 3 images. Found ${pickedImageIdxs.length}.`, 'Invalid File Format');
            dispatchHideDropDown();
            return;
        }

        const getExtNum = (part, fallbackIdx) =>
            part?.fileLocationIndex ?? part?.index ?? fallbackIdx;

        //---- Load 2 tables ----
        for (let i = 0; i < 2; i++) {
            const partIdx = pickedTableIdxs[i];
            const part = report.parts.find((p) => (p?.index ?? p?.fileLocationIndex) === partIdx);
            const extNum = getExtNum(part, partIdx);
            const desc = part?.desc;

            const tblReq = makeFileRequest(
                desc || `${uploadedFileName} Table ${i + 1}`,
                fileLocation,
                null,
                {
                    tbl_id: i === 0 ? ALERT.TABLE_1_ID : ALERT.TABLE_2_ID,
                    pageSize: ALERT.TABLE_PAGESIZE,
                    META_INFO: {}
                }
            );
            tblReq.tbl_index = extNum;

            dispatchTableSearch(
                tblReq,
                {removable: true, tbl_group: i === 0 ? ALERT.TABLE_GROUP_MAIN : ALERT.TABLE_GROUP_DETAILS}
            );
        }

        //---- Load 3 images ----
        const viewerIds = [ALERT.IMG_VIEWER_1, ALERT.IMG_VIEWER_2, ALERT.IMG_VIEWER_3];
        const plotIds = [ALERT.IMG_PLOT_1, ALERT.IMG_PLOT_2, ALERT.IMG_PLOT_3];

        for (let i = 0; i < 3; i++) {
            const partIdx = pickedImageIdxs[i];
            const part = report.parts[partIdx];
            const extNum = getExtNum(part, partIdx);
            const desc = part?.desc;

            const viewerId = viewerIds[i];
            const plotId = plotIds[i];

            const wpRequest = WebPlotRequest.makeFilePlotRequest(fileLocation);
            wpRequest.setInitialRangeValues(RangeValues.make2To10SigmaLinear());
            wpRequest.setPlotGroupId(viewerId);
            wpRequest.setMultiImageExts(`${extNum}`);
            wpRequest.setTitle(desc || `${uploadedFileName} [ext ${extNum}]`);

            dispatchPlotImage({plotId, wpRequest, viewerId, setNewPlotAsActive: i === 0});
        }

    } catch (error) {
        console.error('Error loading file:', error);
        showInfoPopup(`Error loading file: ${error.message}`, 'Load Error');
    }

    dispatchHideDropDown();
}