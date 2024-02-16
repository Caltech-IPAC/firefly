import {IconButton, Sheet, Stack, Typography} from '@mui/joy';
import React, {useContext, useState} from 'react';
import {getBackgroundInfo} from '../../core/background/BackgroundUtil.js';
import {dispatchShowDropDown} from '../../core/LayoutCntlr.js';
import {AppPropertiesCtx} from '../../ui/AppPropertiesCtx.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {FileDropZone} from '../../visualize/ui/FileUploadViewPanel.jsx';
import MenuRoundedIcon from '@mui/icons-material/MenuRounded';
import NorthRoundedIcon from '@mui/icons-material/NorthRounded';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';

export function DefaultLandingPage() {
    const {appTitle,footer,showLandingHelp=true,
        fileDropEventAction='FileUploadDropDownCmd'} = useContext(AppPropertiesCtx);
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
                    const newEv = {type: 'drop', dataTransfer: {files: Array.from(dropEvent.dataTransfer.files)}};
                    dispatchShowDropDown({fileDropEventAction, initArgs: {searchParams: {dropEvent: newEv}}});
                },
            }}>
                <Stack justifyContent='space-between' width={1}>
                    <Stack spacing={10} width={1}>
                        <Stack {...{direction: 'row', justifyContent: 'space-between'}}>
                            {showLandingHelp &&
                                <Stack {...{alignSelf: 'flex-start', alignItems: 'center', spacing: 0, sx: {pt: 3, pl: 31}}}>
                                    {/*<PanToolAltOutlinedIcon {...{ sx:{width:'3rem', height:'3rem'} }}/>*/}
                                    <NorthRoundedIcon {...{sx: {width: '3rem', height: '3rem'}}}/>

                                    <Stack {...{direction: 'row',}}>
                                        <Typography level='title-lg'> Choose Search from menu </Typography>

                                    </Stack>
                                    <Typography level='body-md'>or</Typography>
                                    <Stack {...{direction: 'row', spacing: 1}}>
                                        <Typography level='title-lg'>use</Typography>
                                        <IconButton disabled={true} variant='outlined'>
                                            <MenuRoundedIcon/>
                                        </IconButton>
                                        <Typography level='title-lg'> to see the full list of searches </Typography>
                                    </Stack>
                                    <Typography level='body-md'>or</Typography>
                                    <Typography level='title-lg'> Drop a file anywhere</Typography>
                                </Stack>
                            }
                            {haveBgJobs &&
                                <Stack {...{alignSelf: 'flex-start', alignItems: 'center', spacing: 1, sx: {pt: 6, pr: 2}}}>
                                    <NorthRoundedIcon {...{sx: {width: '3rem', height: '3rem'}}}/>
                                    <Typography level='title-lg'>Load jobs from </Typography>
                                    <Typography level='title-lg'>Background Monitor </Typography>
                                </Stack>}
                        </Stack>

                        <Stack {...{alignItems: 'center', spacing: 3}}>
                            <Typography sx={{fontSize: '5rem'}} color='success'>
                                {`${appTitle??''} Ready`}
                            </Typography>
                            <Typography level={'h4'}>No Search Results Yet</Typography>
                            <Typography level={'body-lg'} startDecorator={<InfoOutlinedIcon/>}>
                                Note: This landing page is still being developed
                            </Typography>
                        </Stack>
                    </Stack>
                    {footer ? footer : undefined}
                </Stack>
            </FileDropZone>
        </Sheet>
    );
}