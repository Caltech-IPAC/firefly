import React from 'react';
import {
    getDevCycle, getFireflyLibraryVersionStr, getVersion, isVersionFormalRelease, isVersionPreRelease
} from 'firefly/Firefly.js';

import {CopyToClipboard} from '../visualize/ui/MouseReadout.jsx';
import {showInfoPopup} from './PopupUtil.jsx';
import IPAC_LOGO from 'images/ipac_logo-56x40.png';
import CALTECH_LOGO from 'images/caltech-new-logo.png';
import FFTOOLS_ICO from 'html/images/fftools-logo-offset-small-42x42.png';
import {Stack, Typography, Link, Box, Button} from '@mui/joy';

const VER = 'Version';
const BUILT_ON = 'Built On';
const COMMIT = 'Git Commit';
const FF_LIB = 'Firefly Library Version';
const FF_COM = 'Firefly Git Commit';
const FF_TAG = 'Firefly Git Tag';


export function getVersionInfoStr(includeBuiltOnDate, includeBuildType) {
    const {BuildMajor = 0, BuildMinor, BuildRev, BuildType, BuildDate} = getVersion();
    const major = Number(BuildMajor);
    let version;
    if (major) {
        version = `v${BuildMajor}.${BuildMinor}`;
        version += BuildRev !== '0' ? `.${BuildRev}` : '';
    } else {
        version = getFireflyLibraryVersionStr();
    }
    if (includeBuildType) version += BuildType === 'Final' ? '' : `, ${BuildType}`;
    return includeBuiltOnDate ? version + `, Built On: ${BuildDate}` : version;
}

export const VersionInfo = ({onClick, asButton=false, includeBuiltOnDate=true, includeBuildType=true}) => {

    const text= getVersionInfoStr(includeBuiltOnDate, includeBuildType);
    const showDialog = () => {
        showFullVersionInfoDialog();
        onClick?.();
    };
    
    return asButton ?
        (<Button onClick={showDialog}> {text} </Button>) :
        (<Typography level='body-xs' sx={{color:'neutral.400'}} onClick={showDialog} > {text} </Typography>);
};

const Entry = ({desc, value}) =>
    (<>
        <Typography sx={{lineHeight:1.5, fontWeight:'bold',gridColumnStart:1, justifySelf:'end', whiteSpace: 'nowrap'}}>{desc}</Typography>
        <Typography justifyContent='flex-start' sx={{lineHeight:1.5,gridColumnStart:2, whiteSpace: 'nowrap'}}>{value}</Typography>
    </>);

function VersionInfoFull() {
    const {BuildTime, BuildMajor, BuildCommit, BuildFireflyTag:ffTag, BuildCommitFirefly:ffCommit} = getVersion();
    const major = Number(BuildMajor);
    const version = getVersionInfoStr(false, false);

    const versionAsText =
        `${VER}:    ${version}\n${BUILT_ON}:   ${BuildTime}\n${COMMIT}: ${BuildCommit}`
        + (major ? `\n${FF_LIB}: ${getFireflyLibraryVersionStr()}` : '')
        + (ffCommit ? `\n${FF_COM}:      ${ffCommit}` : '')
        + (ffTag ? `\n${FF_TAG}:         ${ffTag}` : '');

    return (
        <Stack spacing={1}>
            <Stack direction='row'>
                <Stack alignItems='center'>
                    <Link href='https://github.com/Caltech-IPAC/firefly' target='github-window'>
                        <img alt='Firefly' src={FFTOOLS_ICO}/>
                    </Link>
                    <Link href='https://www.caltech.edu' target='caltech-window'>
                        <img style={{width:70}} alt='Caltech' src={CALTECH_LOGO}/>
                    </Link>
                    <Link href='https://www.ipac.caltech.edu' target='ipac-window'>
                        <img alt='IPAC' src={IPAC_LOGO}/>
                    </Link>
                </Stack>
                <Stack justifyContent='space-evenly' direction='row' alignItems='flex-start' sx={{ml:5.75}}>
                    <Box sx={{display:'grid', gridTemplateColumns:'auto auto', gap:.75, userSelect:'text'}}>
                        <Entry desc={VER} value={version}/>
                        <Entry desc={BUILT_ON} value={BuildTime}/>
                        <Entry desc={COMMIT} value={BuildCommit}/>
                        {major ? <Entry desc={FF_LIB} value={getFireflyLibraryVersionStr()}/> : ''}
                        {ffCommit && <Entry desc={FF_COM} value={ffCommit}/>}
                        {ffTag && <Entry desc={FF_TAG} value={ffTag}/>}
                    </Box>
                    <CopyToClipboard style={{justifySelf: 'start', marginLeft:10}}
                                                               value={versionAsText} size={16} buttonStyle={{backgroundColor: 'unset'}}/>
                </Stack>
            </Stack>
            {!isVersionFormalRelease() &&
                <Stack direction='row' justifyContent={'center'}>
                    <Typography color='warning'>
                        {isVersionPreRelease() ?
                            `Warning: Early preview of Firefly ${getDevCycle()}` :
                            `Warning: Development build of Firefly on dev cycle ${getDevCycle()}`
                        }
                    </Typography>
                </Stack>
            }
            <Stack direction='row' justifyContent={'center'}>
                <Acknowledgement/>
            </Stack>
        </Stack>
    );
}

const Acknowledgement= () => (

    <Typography level='body-sm' width={'90%'} sx={{p:0.2, lineHeight:1.3}}>
            Firefly development by&nbsp;
        <Link href='https://ipac.caltech.edu' target='ipac-window'>IPAC</Link>
            &nbsp;at&nbsp;
        <Link href='https://www.caltech.edu' target='caltech-window'>Caltech</Link>
            &nbsp;has been supported by NASA, principally through&nbsp;
        <Link href='https://irsa.ipac.caltech.edu' target='ipac-window'>IRSA</Link>
           , and by the National Science Foundation, through the&nbsp;
        <Link href='https://www.lsst.org/' target='rubin-window'>Vera C. Rubin Observatory</Link>
            . Firefly is open-source software, available on&nbsp;
        <Link href='https://github.com/Caltech-IPAC/firefly' target='github-window'>GitHub</Link>
        &nbsp;and&nbsp;
        <Link href='https://hub.docker.com/repository/docker/ipac/firefly' target='dockerhub-window'>DockerHub</Link>
        .
    </Typography>

);



export const showFullVersionInfoDialog = (title = '') =>
    showInfoPopup( <VersionInfoFull/>,
        `${title} Version Information`,
        {'& .FF-Popup-Content': {width:'40em',maxWidth:'100%'}});
