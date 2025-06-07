import React, {useEffect} from 'react';
import {object, string, shape} from 'prop-types';
import {IconButton, Button, Sheet, Stack, Typography, ListItemDecorator, Tab} from '@mui/joy';
import moment from 'moment';
import {isEmpty} from 'lodash';

import {Slot, useStoreConnector} from '../../ui/SimpleComponent';
import {getBackgroundInfo, getJobInfo, getJobTitle, getPhaseTips, isActive, isArchived, isDone, isExecuting, isFail, isSearchJob, isSuccess, Phase} from './BackgroundUtil';
import {TablePanel} from '../../tables/ui/TablePanel';
import {dispatchFormSubmit, getAppOptions} from '../AppDataCntlr';
import {dispatchBgJobInfo, dispatchBgSetInfo, dispatchJobCancel, dispatchJobRemove, dispatchSetJobNotif} from './BackgroundCntlr';
import {InputField} from '../../ui/InputField';
import Validate from '../../util/Validate';
import {getRequestFromJob} from '../../tables/TableRequestUtil';
import {dispatchTableAddLocal, dispatchTableSearch, TABLE_HIGHLIGHT} from '../../tables/TablesCntlr';
import {showInfoPopup, showYesNoPopup} from '../../ui/PopupUtil';
import {updateSet} from '../../util/WebUtil';
import {download} from '../../util/fetch';
import {isJobInfoOpen, showJobInfo} from './JobInfo';
import {dispatchHideDropDown, dispatchShowDropDown} from '../LayoutCntlr';
import {getCellValue, getTableUiById, getTblById, processRequest, watchTableChanges} from '../../tables/TableUtil';
import {SORT_DESC, SortInfo} from '../../tables/SortInfo';
import {workingIndicator} from '../../ui/Menu';
import {dispatchHideDialog} from '../ComponentCntlr';
import {InfoButton} from '../../visualize/ui/Buttons';

import InsightsIcon from '@mui/icons-material/Insights';
import DownloadIcon from '@mui/icons-material/Download';
import ReadMoreIcon from '@mui/icons-material/ReadMore';
import StopCircleOutlinedIcon from '@mui/icons-material/StopCircleOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import NotificationsOffIcon from '@mui/icons-material/NotificationsOff';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import {FormWatcher} from '../../templates/router/RouteHelper';

export const jobMonitorPath = '/jobMonitor';

export function JobMonitor({initArgs, help_id, slotProps, ...props}) {
    const {title, table, note, ...rest} = getAppOptions()?.background?.history || {};
    const titleProps = {...slotProps?.title, ...title};
    const tableProps = {...slotProps?.table, ...table};
    const width = useStoreConnector(() => {
        const {columnWidths=[]} = getTableUiById('JobHistoryTable-ui') || {};
        return columnWidths.reduce((acc, val, idx) => idx < 7 ? acc + val : acc, 3);

    });
    return(
        <Slot component={Stack} spacing={1} mt={1} sx={{flexGrow:1}} alignItems='center' slotProps={{...props, ...rest}}>
            <Slot component={TitleSection} sx={{width}} slotProps={titleProps}/>
            <Slot component={JobMonitorTable} sx={{width}} slotProps={tableProps} help_id={help_id}/>
            {note && (width>100) && <Typography sx={{width}} level='body-sm' fontStyle='italic' color='warning'> {note} </Typography>}
        </Slot>
    );
}

JobMonitor.propTypes = {
    initArgs: object,
    help_id: string,
    slotProps: shape({
        title: Stack.propTypes,
        table: Stack.propTypes
    })
};


export function showJobMonitor(show=true) {
    if (show) {
        dispatchShowDropDown({view:'BackgroundMonitorCmd', });
    } else {
        dispatchHideDropDown();
    }
}

export function makeBackgroundMonitorMenuItem() {
    const label = getAppOptions()?.background?.history?.label || 'Job Monitor';
    const TabRenderer =  React.forwardRef((props, ref) => {
        const {jobs={}} = useStoreConnector(() => getBackgroundInfo());
        const loading = Object.values(jobs).some((j) => isExecuting(j));
        return (
            <Tab ref={ref} {...props}>
                {loading && <ListItemDecorator>{workingIndicator}</ListItemDecorator>}
                {label}
            </Tab>
        );
    });
    return { label, TabRenderer, action: 'BackgroundMonitorCmd', primary: true , path: jobMonitorPath};
}

export function MultiResultsPopup({job, ...props}) {
    return (
        <IconButton title='Show Download Requests' color='primary' onClick={() => showMultiResults(job)} {...props}>
            <ReadMoreIcon/>
        </IconButton>
    );
}

export function showMultiResults(job) {
    const {results} = job || [];
    const popup = results.map( (r, idx) => (
        <Stack direction='row' alignItems='center' gap={2}>
            <Typography>{r?.id || `Item-${idx}`}</Typography>
            <DownloadBtn key={idx} job={job} index={idx}/>
        </Stack>
    ));
    showInfoPopup(<Stack>{popup}</Stack>);
}


// ----------------------------------Start of private functions ------------------------------------------//

function TitleSection({summary, notification, ...props}) {
    const {jobs:jobMap={}, email, notifEnabled} = useStoreConnector(() => getBackgroundInfo());
    const jobs = getMonitoredJob(jobMap);

    return (
        <Stack component={Sheet} variant='soft' borderRadius={4} padding={1} spacing={1} {...props}>
            <Slot component={JobSummary} jobs={jobs} slotProps={summary}/>
            <Slot component={Notification} {...{email, notifEnabled}} slotProps={notification}/>
        </Stack>
    );
}

function JobSummary({jobs, ...props}) {
    const total = jobs?.length || 0;
    const active = jobs?.filter((j) => isActive(j)).length || 0;
    const failed = jobs?.filter((j) => isFail(j)).length || 0;
    const archived = jobs?.filter((j) => isArchived(j)).length || 0;
    const Entry = ({label, value, ...props}) => (
        <Stack direction='row' alignItems='center' gap={1} {...props}>
            <Typography level='title-sm'>{label}:</Typography>
            <Typography level='data-sm'>{value}</Typography>
        </Stack>
    );
    return (
        <Stack direction='row' gap={5} {...props}>
            <Typography level='title-md' color='primary'>Job Summary</Typography>
            <Stack direction='row' gap={10}>
                <Entry label='Total' value={total}/>
                <Entry label='Active' value={active}/>
                <Entry label='Failed' value={failed}/>
                {(archived>0) && <Entry label='Archived' value={archived} title={getPhaseTips(Phase.ARCHIVED)}/>}
            </Stack>
        </Stack>
    );
}

function Notification({email='', notifEnabled, ...props}) {
    const {enable, showEmail}= getAppOptions()?.background?.notification || {};

    useEffect(() => {
        dispatchBgSetInfo({email, notifEnabled: enable});    // sync client with server
    }, []);

    if (!showEmail) return null;

    const onEmailChanged = (v) => {
        if (v?.valid && email !== v.value) dispatchBgSetInfo({email: v.value, notifEnabled});
    };

    return (
        <Stack direction='row' gap={2} alignItems='center' {...props}>
            <Typography level='title-md' color='primary'>Notification Email</Typography>
            <Stack direction='row' gap={2}>
                <InputField
                    validator={Validate.validateEmail.bind(null, 'an email field')}
                    tooltip='Enter an email to be notified when a process completes.'
                    slotProps={{label:{sx:{ml:0}}}}
                    value={email || ''}
                    placeholder='Enter an email to get notification'
                    sx={{width:250, justifyContent: 'center'}}
                    onChange={onEmailChanged}
                    actOn={['blur','enter']}
                />
                <Typography level='body-xs' color='warning' width='45em'>
                    By default, email notification is turned off. To toggle email notification for any given job in progress, click on the blue bell icon:
                    <NotificationsOffIcon color='primary' sx={{marginBottom: '-0.25em'}}/>
                </Typography>
            </Stack>
        </Stack>
    );
}

function JobMonitorTable({help_id, ...props}) {
    const jobMap = useStoreConnector(() => getBackgroundInfo()?.jobs || {});

    const tbl_id = 'JobHistoryTable';
    useEffect(() => {
        const table = convertToTableModel(getMonitoredJob(jobMap), tbl_id);
        dispatchTableAddLocal(table, undefined, false);
    }, [jobMap]); // refreshed only when jobMap changes

    useEffect(() => {
        return watchTableChanges(tbl_id,
            [TABLE_HIGHLIGHT],
            (action) => {
                const {highlightedRow} = action.payload || {};
                const jobId = getCellValue(getTblById(tbl_id), highlightedRow, 'Control');
                if (isJobInfoOpen())    showJobInfo(jobId);
            });
    }, []);     // only need to do once

    const renderers =  {
        Phase:    {cellRenderer: PhaseRenderer},
        Control:  {cellRenderer: ControlRenderer},
    };

    return (
        <Slot component={TablePanel} rowHeight={32} {...{tbl_id, help_id, renderers}}
                    sx={{'& .fixedDataTableCellGroupLayout_cellGroup > :last-child': {borderRight: 'none'}}}
                    {...{showToolbar: false, selectable:false, showFilters:true, showOptionButton:false}}
                    slotProps={{...props}}
        />
    );
}

function PhaseRenderer({cellInfo}) {
    const {value} = cellInfo;
    const phase = {phase:value};
    const color = isActive(phase) ? 'warning' :
                            isFail(phase) ? 'danger' :
                            isArchived(phase) ? 'neutral' :
                            undefined;
    return (
            <Typography level='body-md' color={color} title={getPhaseTips(value)}>{value}</Typography>
    );
}

function ControlRenderer({cellInfo}) {
    const {value:jobId} = cellInfo;
    const job = useStoreConnector(() => getJobInfo(jobId), [jobId]);
    if (!job?.meta?.jobId) return null;

    return  (
        <Stack mx={2} direction='row' justifyContent='right'>
            <Progress job={job}/>
            <Results job={job}/>
            <InfoPopup job={job}/>
            <Delete job={job}/>
        </Stack>
    );
}

function Delete({job}) {
    const doDelete = () => {
        showYesNoPopup('Are you sure that you want to delete this job?',(id, yes) => {
            if (yes) dispatchJobRemove(job?.meta?.jobId);
            dispatchHideDialog(id);
        });
    };
    const title = job?.jobInfo?.title || job.jobId;
    if (isDone(job)) {
        return <IconButton  title={`Delete job ${job?.meta?.jobId}`} color='danger' onClick={doDelete}><DeleteOutlineOutlinedIcon/></IconButton>;
    } else if (isExecuting(job)) {
        return <IconButton  title={`Abort job ${title}`} color='danger' onClick={() => dispatchJobCancel(job?.meta?.jobId)}><StopCircleOutlinedIcon/></IconButton>;
    }
}


function InfoPopup({job}) {
    return <InfoButton color='warning' iconButtonSize='34px' onClick={() => showJobInfo(job?.meta?.jobId)}/>;
}

function Progress({job}) {
    if (!Phase.EXECUTING.is(job?.phase)) return null;
    return(
        <>
            <Button loading title={job?.meta?.progressDesc} variant='plain' color='success'/>
            <NotifBtn jobId={job.meta?.jobId} enable={job.meta?.sendNotif}/>
        </>
    );

}

function NotifBtn ({jobId, enable}) {
    const {email, notifEnabled} = useStoreConnector(() => getBackgroundInfo());
    const {showEmail}= getAppOptions()?.background?.notification || {};

    if (!notifEnabled || !jobId) return null;

    const toggleSendNotif = (enable) => {
        if (enable && showEmail && !email) {
            showInfoPopup('You must provide a valid email address to receive notifications.');
        } else {
            dispatchSetJobNotif({jobId, enable, email});
        }
    };

    const title = enable ? 'Notification is enabled. Click to disable.' : 'Notification is disabled. Click to enable';
    const icon = enable ? <NotificationsActiveIcon/> : <NotificationsOffIcon/>;
    return <IconButton title={title} color='primary' onClick={() => toggleSendNotif(!enable)}>{icon}</IconButton>;
}


function Results({job}) {
    if (!isSuccess(job)) return null;
    if (isSearchJob(job)) {
        const request = getRequestFromJob(job?.meta?.jobId);  // the request is initiated from Firefly
        const submitTo = request?.META_INFO?.form_submitTo;
        const onClick = showResults(request, submitTo);
        const icon = <IconButton  title='Show Search Result' disabled={!onClick} color='success' onClick={onClick}><InsightsIcon/></IconButton>;
        if (submitTo) {
            return (
                <FormWatcher submitTo={submitTo}>
                    {icon}
                </FormWatcher>
            );
        } else {
            return icon;
        }
    } else if (job?.results?.length === 1) {
        return <DownloadBtn job={job}/>;
    } else {
        return <MultiResultsPopup job={job}/>;
    }
}

function DownloadBtn({job, index=0}) {
    const dlState = useStoreConnector( () => getJobInfo(job?.meta?.jobId)?.downloadState?.[index], [job?.meta?.jobId, index]);
    const stateMap = (state) => ({
        DONE: ['neutral', 'Downloaded'],
        FAIL: ['danger', 'Download may have failed or timed out'],
    }[state] ?? ['primary', 'Click to download file']);
    const loading = dlState === 'WORKING';
    const color = stateMap(dlState)[0];
    const title = stateMap(dlState)[1];

    return  (
        <IconButton title='Download file' onClick={() => doDownload(job, index)} {...{color, loading, title}}><DownloadIcon/></IconButton>
    );
}

function convertToTableModel(jobs, tbl_id) {
    const cProps = {align: 'center'};
    const columns = [
        {name: 'Title', width: 22},
        {name: 'Service ID', width: 11, type: 'char',  ...cProps},
        {name: 'Type', width: 9, type: 'char', ...cProps},
        {name: 'Start Time', width: 14, ...cProps},
        {name: 'End Time', width: 14, ...cProps},
        {name: 'Phase', width: 13, type: 'char', ...cProps},
        {name: 'Control', width: 14, sortable: false, filterable:false}
    ];

    const data = jobs?.map((job) =>
        [
            getJobTitle(job),
            job.meta?.svcId,
            job.meta?.type,
            job.startTime && moment.utc(job.startTime).format('YYYY-MM-DD HH:mm:ss'),
            job.endTime && moment.utc(job.endTime).format('YYYY-MM-DD HH:mm:ss'),
            job.phase,
            job.meta?.jobId
        ]);

    const phaseIdx = columns.findIndex((c) => c.name === 'Phase');
    const doFilter = columns[phaseIdx].enumVals?.includes(Phase.ARCHIVED);

    const totalRows = data.length;
    const {origTableModel:prevTable, request:prevReq} = getTblById(tbl_id) || {};
    const request = (prevTable?.totalRows < totalRows || !prevReq) ? defaultRequest(doFilter) : prevReq;  // if a new job is added or no previous request, use default
    let table = { tbl_id, request, totalRows, tableData: { columns, data }};
    if (request.sortInfo || request.filters) {
        table = processRequest(table, request);
    }
    return table;
}

function defaultRequest(doFilter) {
    const sortInfo = SortInfo.newInstance(SORT_DESC, 'Start Time').serialize();
    const filters = doFilter ? "Phase IN ('EXECUTING', 'COMPLETED', 'ERROR', 'ABORTED')" : undefined;
    return {sortInfo, filters};
}

function showResults(request, submitTo) {
    // assuming job returns a table;  will expand to other types in the future
    if (!isEmpty(request)) {
        return () => {
            dispatchTableSearch(request);
            showJobMonitor(false);
            if (submitTo)  dispatchFormSubmit({submitTo}); // if this is a routed app, submit the form to update the route
        };
    }
}

function doDownload(job, index) {
    const url = job?.results?.[index]?.href;
    if (!url) {
        showInfoPopup('Bad URL. Cannot download');
        return;
    }
    dispatchBgJobInfo( updateSet(job, ['downloadState',index], 'WORKING') );
    download(url).then( () => {
        dispatchBgJobInfo( updateSet(job, ['downloadState',index], 'DONE') );
    }).catch(() => {
        dispatchBgJobInfo( updateSet(job, ['downloadState',index], 'FAIL') );
    });
}

function getMonitoredJob(jobMap) {
    return (jobMap ? Object.values(jobMap) : [])
        .filter((job) => job?.meta?.monitored)                   // only monitored jobs
        .sort((a,b) => b.startTime?.localeCompare(a.startTime));
}


