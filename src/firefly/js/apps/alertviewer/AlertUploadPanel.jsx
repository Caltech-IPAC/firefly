import React, {useContext, useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {showInfoPopup} from 'firefly/ui/PopupUtil';
import {Button, IconButton, Input, Stack, Typography} from '@mui/joy';
import {LoadingMessage} from 'firefly/visualize/ui/FileUploadViewPanel';
import {addToRecentAlertIDs, showAlertIdDialog} from 'firefly/apps/alertviewer/AlertIDDialog';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import {InitArgsCtx} from 'firefly/templates/common/InitArgsCtx';
import {getRootURL} from 'firefly/util/WebUtil';
import {getJsonData} from 'firefly/rpc/SearchServicesJson';
import {ServerRequest} from 'firefly/data/ServerRequest';
import {makeFileRequest} from 'firefly/tables/TableRequestUtil';
import {ALERT} from './AlertIDs.js';
import {dispatchTableSearch} from 'firefly/tables/TablesCntlr';
import WebPlotRequest, {TitleOptions} from 'firefly/visualize/WebPlotRequest';
import RangeValues from 'firefly/visualize/RangeValues';
import {dispatchDeletePlotView, dispatchPlotImage} from 'firefly/visualize/ImagePlotCntlr';
import {dispatchComponentStateChange} from 'firefly/core/ComponentCntlr';
import {removeTablesFromGroup} from 'firefly/tables/TableUtil';
import {dispatchChartRemove} from 'firefly/charts/ChartsCntlr';
import {dispatchHideDropDown} from 'firefly/core/LayoutCntlr';
import {dispatchFormSubmit} from 'firefly/core/AppDataCntlr';

const ALERT_LOAD_REQUEST = 'AlertViewerSearchProcessor';
const IMAGE_TITLES = ['Science', 'Template', 'Difference'];

export const AlertIdPanel = ({loadInPlace=false}) => {
    const instruction = 'Enter an Alert ID to load in the Alert Viewer:';
    const [isLoading, setIsLoading] = useState(false);
    const [alertId, setAlertId] = useState('');
    const {initArgs} = useContext(InitArgsCtx);

    const doLoadInPlace = async (loadId) => {
        const trimmedId = loadId.trim();
        if (!trimmedId) return;

        setIsLoading(true);
        try {
            const request = new ServerRequest(ALERT_LOAD_REQUEST);
            request.setParam('source', trimmedId);

            const result = await getJsonData(request);
            if (!result?.success) {
                showInfoPopup(result?.message || 'Unable to load alert data.', 'Load Error');
                return;
            }
            clearAlertProducts();
            loadFromEntries(result, trimmedId);
        } catch (error) {
            showInfoPopup(`Error loading file: ${error.message}`, 'Load Error');
        } finally {
            setIsLoading(false);
        }
    };

    const doLoad = async (loadId = alertId) => {
        const trimmedId = loadId.trim();
        if (!trimmedId) {
            showInfoPopup('Please enter an alert ID.', 'Load Error');
            return;
        }

        addToRecentAlertIDs(trimmedId);
        try {
            if (loadInPlace) {
                await doLoadInPlace(trimmedId);
            } else {
                setIsLoading(true);
                const url = new URL('alertviewer', getRootURL());
                url.searchParams.set('api', 'alert');
                url.searchParams.set('id', trimmedId);
                window.open(url.href, '_blank');
            }
        } catch (error) {
            const errMsg = loadInPlace ? `Error loading file: ${error.message}` : `Error opening alert viewer: ${error.message}`;
            showInfoPopup(errMsg, 'Load Error');
        } finally {
            if (!loadInPlace) setIsLoading(false);
        }
    };

    useEffect(() => {
        const urlApiId = initArgs?.urlApi?.id?.trim?.();
        if (!urlApiId) return;
        setAlertId(urlApiId);
        void doLoadInPlace(urlApiId);
    }, [initArgs]);

    return (
        <Stack width={1} alignItems='center'>
            <Stack spacing={3} sx={{mt: 4}}>
                <Typography level='body-lg'>{instruction}</Typography>
                    <Stack direction='row' spacing={1}>
                        <Input
                            sx={{width: '50rem'}}
                            value={alertId}
                            placeholder='Enter an Alert ID'
                            onChange={(ev) => setAlertId(ev.target.value)}
                            onKeyDown={(ev) => ev.key === 'Enter' && doLoad()}
                        />
                        <Button onClick={() => doLoad()} loading={isLoading}>Load</Button>
                        <IconButton
                            size='sm'
                            variant='outlined'
                            title='Recent Alert IDs'
                            onClick={() => {
                                showAlertIdDialog(alertId, (newId) => {
                                    if (newId) setAlertId(newId);
                                });
                            }}
                        >
                            <EditOutlinedIcon/>
                        </IconButton>
                    </Stack>
                    {isLoading && <LoadingMessage/>}
            </Stack>
        </Stack>
    );
};

AlertIdPanel.propTypes = {
    loadInPlace: PropTypes.bool,
};

function clearAlertProducts() {
    removeTablesFromGroup(ALERT.TABLE_GROUP_MAIN);
    removeTablesFromGroup(ALERT.TABLE_GROUP_DETAILS);
    dispatchChartRemove(ALERT.CHART_1_ID);

    [ALERT.IMG_PLOT_1, ALERT.IMG_PLOT_2, ALERT.IMG_PLOT_3].forEach((plotId) =>
        dispatchDeletePlotView({plotId, holdWcsMatch: true})
    );
}

function loadFromEntries(result, alertId) {
    try {
        const entries = result?.entries ?? [];
        const mainTablePart = entries[1];
        const detailsTablePart = entries[0];
        const imageParts = entries.slice(2, 5);

        if (entries.length < 5) {
            showInfoPopup(`Alert viewer response must include 5 entries. Found ${entries.length}.`, 'Load Error');
            return;
        }

        if (mainTablePart?.type !== 'Table' || detailsTablePart?.type !== 'Table') {
            showInfoPopup('Alert viewer response did not return the first two entries as tables.', 'Invalid File Format');
            return;
        }
        if (imageParts.length < 3 || imageParts.some((entry) => entry?.type !== 'Image')) {
            showInfoPopup('Alert viewer response did not return the last three entries as images.', 'Invalid File Format');
            return;
        }

        const getExtNum = (part, fallbackIdx) => part?.extNum ?? fallbackIdx;
        const getFileLocation = (part) => part?.fileKey ?? result?.fileKey;
        const getFileName = (part) => part?.fileName ?? result?.fileName ?? result?.source ?? 'Uploaded FITS';
        const pickedTableParts = [detailsTablePart, mainTablePart];

        for (let i = 0; i < 2; i++) {
            const part = pickedTableParts[i];
            const extNum = getExtNum(part, i);
            const desc = part?.desc;
            const fileLocation = getFileLocation(part);
            const uploadedFileName = getFileName(part);
            const isDetailsTable = i === 0;

            if (!fileLocation) {
                showInfoPopup(`Alert viewer table entry ${i + 1} is missing a file key.`, 'Load Error');
                return;
            }

            const tblReq = makeFileRequest(
                desc || `${uploadedFileName} Table ${i + 1}`,
                fileLocation,
                null,
                {
                    tbl_id: isDetailsTable ? ALERT.TABLE_2_ID : ALERT.TABLE_1_ID,
                    pageSize: ALERT.TABLE_PAGESIZE,
                    META_INFO: {}
                }
            );
            tblReq.tbl_index = extNum;

            dispatchTableSearch(
                tblReq,
                {removable: true, tbl_group: isDetailsTable ? ALERT.TABLE_GROUP_DETAILS : ALERT.TABLE_GROUP_MAIN}
            );
        }

        const plotIds = [ALERT.IMG_PLOT_1, ALERT.IMG_PLOT_2, ALERT.IMG_PLOT_3];

        for (let i = 0; i < 3; i++) {
            const part = imageParts[i];
            const extNum = getExtNum(part, i);
            const fileLocation = getFileLocation(part);
            const plotId = plotIds[i];

            if (!fileLocation) {
                showInfoPopup(`Alert viewer image entry ${i + 1} is missing a file key.`, 'Load Error');
                return;
            }

            const wpRequest = WebPlotRequest.makeFilePlotRequest(fileLocation);
            wpRequest.setInitialRangeValues(RangeValues.make2To10SigmaLinear());
            wpRequest.setPlotGroupId(ALERT.IMG_VIEWER);
            wpRequest.setMultiImageExts(`${extNum}`);
            wpRequest.setTitle(IMAGE_TITLES[i] ?? `Image ${i + 1}`);
            wpRequest.setTitleOptions(TitleOptions.NONE);

            dispatchPlotImage({plotId, wpRequest, viewerId: ALERT.IMG_VIEWER, setNewPlotAsActive: i === 0});
        }
        dispatchComponentStateChange(ALERT.STATE_ID, {
            id: alertId,
            source: result?.source,
            fileName: result?.fileName,
        });
        dispatchFormSubmit({submitTo: '/results'});
        dispatchHideDropDown();
    } catch (error) {
        console.error('Error loading file:', error);
        showInfoPopup(`Error loading file: ${error.message}`, 'Load Error');
    }
}
