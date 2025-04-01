import React, {useEffect} from 'react';
import {Checkbox, IconButton, Button, Link, Sheet, Stack, Typography} from '@mui/joy';
import moment from 'moment';
import {uniq} from 'lodash';

import {Slot, useStoreConnector} from '../../ui/SimpleComponent';
import {getBackgroundInfo, getJobInfo, isActive, isArchived, isFail, isSearchJob, isSuccess, Phase} from './BackgroundUtil';
import {TablePanel} from '../../tables/ui/TablePanel';
import {getAppOptions} from '../AppDataCntlr';
import {dispatchBgJobInfo, dispatchBgSetInfo, dispatchJobArchive, dispatchJobCancel, dispatchJobRemove} from './BackgroundCntlr';
import {InputField} from '../../ui/InputField';
import Validate from '../../util/Validate';
import {getRequestFromJob} from '../../tables/TableRequestUtil';
import {dispatchTableAddLocal, dispatchTableSearch} from '../../tables/TablesCntlr';
import {showInfoPopup} from '../../ui/PopupUtil';
import {updateSet} from '../../util/WebUtil';
import {download} from '../../util/fetch';
import {showJobInfo} from './JobInfo';
import {dispatchHideDropDown, dispatchShowDropDown} from '../LayoutCntlr';
import {getTblById, processRequest} from '../../tables/TableUtil';
import {SORT_DESC, SortInfo} from '../../tables/SortInfo';

import InsightsIcon from '@mui/icons-material/Insights';
import DownloadIcon from '@mui/icons-material/Download';
import ReadMoreIcon from '@mui/icons-material/ReadMore';
import StopCircleOutlinedIcon from '@mui/icons-material/StopCircleOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import ArchiveOutlinedIcon from '@mui/icons-material/ArchiveOutlined';

export function showBackgroundMonitor(show=true) {
    if (show) {
        dispatchShowDropDown({view:'BackgroundMonitorCmd', });
    } else {
        dispatchHideDropDown();
    }
}

export function BackgroundMonitor({initArgs, help_id, slotProps, ...props}) {
    return(
        <Slot component={Stack} spacing={1} mt={1} sx={{flexGrow:1}} alignItems='center' {...props}>
            <Slot component={TitleSection}    sx={{width:714}} slotProps={slotProps?.title}/>
            <Slot component={JobHistoryTable} sx={{width:714}} slotProps={slotProps?.table} help_id={help_id}/>
        </Slot>
    );
}

function TitleSection({summary, notification, ...props}) {
    const {jobs:jobMap={}, email, sendNotif} = useStoreConnector(() => getBackgroundInfo());
    const jobs = getMonitoredJob(jobMap);

    return (
        <Stack component={Sheet} variant='soft' borderRadius={4} padding={1} spacing={1} {...props}>
            <Slot component={JobSummary} jobs={jobs} slotProps={summary}/>
            <Slot component={Notification} {...{email, sendNotif}} slotProps={notification}/>
        </Stack>
    );
}

function JobSummary({jobs, ...props}) {
    const total = jobs?.length || 0;
    const active = jobs?.filter((j) => isActive(j)).length || 0;
    const failed = jobs?.filter((j) => isFail(j)).length || 0;
    const archived = jobs?.filter((j) => isArchived(j)).length || 0;
    const Entry = ({label, value}) => (
        <Stack direction='row' alignItems='center' gap={1}>
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
                <Entry label='Archived' value={archived}/>
            </Stack>
        </Stack>
    );
}

function Notification({email='', sendNotif, ...props}) {
    const {enable, showEmail}= getAppOptions()?.background?.notification || {};
    const showNotify = enable || showEmail;

    if (!showNotify) return null;

    const onEmailChanged = (v) => {
        if (v?.valid && email !== v.value) dispatchBgSetInfo({email: v.value, sendNotif});
    };

    const toggleSendNotif = (e) => {
        dispatchBgSetInfo({email, sendNotif: e.target.checked});
    };

    return (
        <Stack direction='row' gap={5} {...props}>
            <Typography level='title-md' color='primary' mr={2}>Notification</Typography>
            <Stack direction='row' gap={9} alignItems='center'>
                <Stack direction='row'>
                    <Checkbox p={1} size='sm' checked={sendNotif} onChange={toggleSendNotif} label={'Enable'}/>
                </Stack>
                {showEmail &&
                    <Stack direction='row' alignItems='center'>
                        <Typography component='label' level='title-md' sx={{mr: 1}}>
                            Email:
                        </Typography>
                        <InputField
                            validator={Validate.validateEmail.bind(null, 'an email field')}
                            tooltip='Enter an email to be notified when a process completes.'
                            slotProps={{label:{sx:{ml:0}}}}
                            value={email || ''}
                            placeholder='Enter an email to get notification'
                            sx={{width:250}}
                            onChange={onEmailChanged}
                            actOn={['blur','enter']}
                        />
                    </Stack>
                }
            </Stack>
        </Stack>
    );
}

function JobHistoryTable({help_id, ...props}) {
    const jobMap = useStoreConnector(() => getBackgroundInfo()?.jobs || {});

    const tbl_id = 'JobHistoryTable';
    useEffect(() => {
        const table = convertToTableModel(getMonitoredJob(jobMap), tbl_id);
        dispatchTableAddLocal(table, undefined, false);
    }, [jobMap]); // refreshed only when jobMap changes

    const renderers =  {
        'Job ID': {cellRenderer: JobIdRenderer},
        'Creation Time': {cellRenderer: DateRenderer},
        Phase:    {cellRenderer: PhaseRenderer},
        Control:  {cellRenderer: ControlRenderer},
    };

    return (
        <TablePanel rowHeight={32} {...{tbl_id, help_id, renderers}}
                    highlightedRowHandler = {()=>undefined}
                    {...{showToolbar: false, selectable:false, showFilters:true, showOptionButton:false}}
                    {...props}
        />
    );
}

function DateRenderer({cellInfo}) {
    const {value} = cellInfo;
    const formatted = moment.utc(value).format('YYYY-MM-DD HH:mm:ss');
    return <Stack mx={1} alignItems='center'><Typography level='body-sm'>{formatted}</Typography></Stack>;
}

function JobIdRenderer({cellInfo}) {
        const {value:jobId} = cellInfo;
        return  <Link mx={1} level='body-sm' underline='none' onClick={() => showJobInfo(jobId)}>{jobId}</Link>;
}

function PhaseRenderer({cellInfo}) {
    const {value} = cellInfo;
    const color = Phase.COMPLETED.is(value) ? 'success' : Phase.ERROR.is(value) ? 'danger' : 'warning';
    return (
        <Stack alignItems='center'>
            <Typography variant='soft' level='body-md' color={color}
                        sx={{width:'8em', mx:1, textAlign:'center'}}>
                {value}
            </Typography>
        </Stack>
);
}

function ControlRenderer({cellInfo}) {
    const {value:jobId} = cellInfo;
    const job = getJobInfo(jobId);
    if (!job?.jobId) return null;

    return  (
        <Stack mx={2} direction='row' justifyContent='right'>
            <Progress job={job}/>
            <Results job={job}/>
            <Abort job={job}/>
            <Archive job={job}/>
            <Delete job={job}/>
        </Stack>
    );
}

function Archive({job}) {
    if (isArchived(job) || isActive(job)) return null;
    return <IconButton title={`Archive job ${job.jobId}`} color='warning' onClick={() => dispatchJobArchive(job.jobId)}><ArchiveOutlinedIcon/></IconButton>;
}

function Delete({job}) {
    return <IconButton  title={`Delete job ${job.jobId}`} color='danger' onClick={() => dispatchJobRemove(job.jobId)}><DeleteOutlineOutlinedIcon/></IconButton>;
}

function Abort({job}) {
    if (!Phase.EXECUTING.is(job?.phase)) return null;
    return <IconButton  title={`Abort job ${job.jobId}`} color='danger' onClick={() => dispatchJobCancel(job.jobId)}><StopCircleOutlinedIcon/></IconButton>;
}

function Progress({job}) {
    if (!Phase.EXECUTING.is(job?.phase)) return null;
    return <Button loading title={job?.jobInfo?.progressDesc} variant='plain' />;
}

function Results({job}) {
    if (!isSuccess(job)) return null;
    if (isSearchJob(job)) {
        return <IconButton  title='Show Search Result' color='success' onClick={() => showTable(job.jobId)}><InsightsIcon/></IconButton>;
    } else if (job?.results?.length === 1) {
        return <DownloadBtn job={job}/>;
    } else {
        return <IconButton title='Show Download Requests' color='success' onClick={() => showMultiResults(job)}><ReadMoreIcon/></IconButton>;
    }
}

function DownloadBtn({job, index=0}) {
    const dlState = useStoreConnector( () => getJobInfo(job.jobId)?.downloadState?.[index], [job.jobId, index]);
    const stateMap = (state) => ({
        DONE: ['neutral', 'Downloaded'],
        FAIL: ['danger', 'Download may have failed or timed out'],
    }[state] ?? ['success', 'Click to download file']);
    const loading = dlState === 'WORKING';
    const color = stateMap(dlState)[0];
    const title = stateMap(dlState)[1];

    return  (
        <IconButton title='Download file' onClick={() => doDownload(job, index)} {...{color, loading, title}}><DownloadIcon/></IconButton>
    );
}

function showMultiResults(job) {
    const {results} = job;
    const popup = (
        <Stack>
            {results.map( (r, idx) =>
                (
                    <Stack direction='row' alignItems='center' gap={2}>
                        <Typography>{r?.id || `Item-${idx}`}</Typography>
                        <DownloadBtn key={idx} job={job} index={idx}/>
                    </Stack>
                ))
            }
        </Stack>
    );
    showInfoPopup(popup);
}

function convertToTableModel(jobs, tbl_id) {

    const columns = [
        {name: 'Job ID', width: 26, resizable: false},
        {name: 'Creation Time', width: 18, resizable: false},
        {name: 'Phase', width: 16, resizable: false},
        {name: 'Control', width: 14, sortable: false, filterable:false, resizable: false}
    ];

    const data = jobs?.map((job) =>
        [
            job.jobId,
            job.startTime,
            job.phase,
            job.jobId
        ]);

    // add enum values for Phase column
    const phaseIdx = columns.findIndex((c) => c.name === 'Phase');
    const colValues = data.map((rowData) => rowData[phaseIdx]);
    columns[phaseIdx].enumVals = uniq(colValues.filter((d) => d)).join(',');

    const totalRows = data.length;
    const {origTableModel:prevTable, request:prevReq} = getTblById(tbl_id) || {};
    const request = (prevTable?.totalRows < totalRows || !prevReq) ? defaultRequest() : prevReq;  // if a new job is added or no previous request, use default
    let table = { tbl_id, request, totalRows, tableData: { columns, data }};
    if (request.sortInfo || request.filters) {
        table = processRequest(table, request);
    }
    return table;
}

function defaultRequest() {
    const sortInfo = SortInfo.newInstance(SORT_DESC, 'Creation Time').serialize();
    const filters = "Phase IN ('EXECUTING', 'COMPLETED', 'ERROR', 'ABORTED')";
    return {sortInfo, filters};
}

function showTable(jobId) {
    const request = getRequestFromJob(jobId);
    if (request) {
        dispatchTableSearch(request);
        showBackgroundMonitor(false);
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
        .filter((job) => job?.jobInfo?.monitored)                   // only monitored jobs
        .sort((a,b) => b.startTime?.localeCompare(a.startTime));
}
