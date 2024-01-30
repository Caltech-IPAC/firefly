import {IconButton, Sheet, Stack, Typography} from '@mui/joy';
import React, {useState} from 'react';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {getBackgroundInfo} from '../../core/background/BackgroundUtil.js';
import {dispatchShowDropDown} from '../../core/LayoutCntlr.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {FileDropZone} from '../../visualize/ui/FileUploadViewPanel.jsx';
import MenuRoundedIcon from '@mui/icons-material/MenuRounded';
import NorthRoundedIcon from '@mui/icons-material/NorthRounded';

export const TriViewDefaultLandingPage = () => {
    const appTitle = getAppOptions()?.appTitle ?? '';
    const [dropEvent, setDropEvent] = useState(undefined);
    const {jobs = {}} = useStoreConnector(() => getBackgroundInfo());

    const items = Object.values(jobs)
        .filter((job) => job.jobInfo?.monitored && job.jobInfo?.type !== 'PACKAGE');
    const haveBgJobs = items.length > 0;

    return (
        <Sheet className='ff-ResultsPanel-StandardView' sx={{width: 1, height: 1}}>
            <FileDropZone {...{
                dropEvent, setDropEvent,
                setLoadingOp: () => {
                    const view = 'FileUploadDropDownCmd';
                    const newEv = {type: 'drop', dataTransfer: {files: Array.from(dropEvent.dataTransfer.files)}};
                    dispatchShowDropDown({view, initArgs: {searchParams: {dropEvent: newEv}}});
                },
                style: {display: 'flex', flexGrow: 1, height: '100%', width: '100%', flexDirection: 'row'}
            }}>

                <Stack spacing={10} width={1}>

                    <Stack {...{direction: 'row', justifyContent: 'space-between'}}>
                        <Stack {...{alignSelf: 'flex-start', alignItems: 'center', spacing: 1, sx: {pt: 6, pl: 18}}}>
                            {/*<PanToolAltOutlinedIcon {...{ sx:{width:'3rem', height:'3rem'} }}/>*/}
                            <NorthRoundedIcon {...{sx: {width: '3rem', height: '3rem'}}}/>

                            <Stack {...{direction: 'row',}}>
                                {/*<NorthRoundedIcon sx={{mt:-3, width:'3rem', height:'3rem', fontSize:'large', transform: 'rotate(-38deg)'}}/>*/}
                                <Typography level='h3'> Choose Search from menu </Typography>
                                {/*<NorthRoundedIcon sx={{mt:-3,width:'3rem', height:'3rem', transform: 'rotate(38deg)'}}/>*/}

                            </Stack>
                            <Typography level='body-md'>or</Typography>
                            <Stack {...{direction: 'row', spacing: 1}}>
                                <Typography level='h3'>use</Typography>
                                <IconButton disabled={true} variant='outlined'>
                                    <MenuRoundedIcon/>
                                </IconButton>
                                <Typography level='h3'> to see the full list of searches </Typography>
                            </Stack>
                            <Typography level='body-md'>or</Typography>
                            <Typography level='h3'> Drop a file anywhere</Typography>
                        </Stack>
                        {haveBgJobs &&
                            <Stack {...{alignSelf: 'flex-start', alignItems: 'center', spacing: 1, sx: {pt: 6, pr: 2}}}>
                                <NorthRoundedIcon {...{sx: {width: '3rem', height: '3rem'}}}/>
                                <Typography level='h3'>Load jobs from </Typography>
                                <Typography level='h3'>Background Monitor </Typography>
                            </Stack>}
                    </Stack>

                    <Stack {...{alignItems: 'center', spacing: 3}}>
                        <Typography sx={{fontSize: '5rem'}} color='success'>
                            {`${appTitle} Ready`}
                        </Typography>
                        <Typography level={'h4'}>No Search Results Yet</Typography>
                    </Stack>
                </Stack>
            </FileDropZone>
        </Sheet>
    );
};