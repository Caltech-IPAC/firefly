import React from 'react';
import {
    getDevCycle, getFireflyLibraryVersionStr, getVersion, isVersionFormalRelease, isVersionPreRelease
} from 'firefly/Firefly.js';

import {CopyToClipboard} from '../visualize/ui/MouseReadout.jsx';
import {showInfoPopup} from './PopupUtil.jsx';
import './VersionInfo.css';
import IPAC_LOGO from 'images/ipac_logo-56x40.png';
import CALTECH_LOGO from 'images/caltech-new-logo.png';
import FFTOOLS_ICO from 'html/images/fftools-logo-offset-small-42x42.png';

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

export const VersionInfo = () => <div onClick={() => showFullVersionInfoDialog()}>{getVersionInfoStr(true, true)}</div>;

const Entry = ({desc, value}) =>
    (<React.Fragment>
        <div className='Version-grid-desc'>{desc}</div>
        <div className='Version-grid-value'>{value}</div>
    </React.Fragment>);

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
        <div style={{display: 'flex', justifyContent: 'space-evenly', flexDirection:'column'}}>
            <div style={{display: 'flex', justifyContent: 'flex-start'}}>
                <div style={{paddingRight:10, display: 'flex', flexDirection:'column', alignItems:'center', marginTop:-20}}>
                    <a href='https://github.com/Caltech-IPAC/firefly' target='github-window'><img alt='Firefly' src={FFTOOLS_ICO} style={{marginTop:10}}/></a>
                    <a href='https://www.caltech.edu' target='caltech-window'><img style={{width:70}} alt='Caltech' src={CALTECH_LOGO}/></a>
                    <a href='https://www.ipac.caltech.edu' target='ipac-window'><img alt='IPAC' src={IPAC_LOGO}/></a>
                </div>
                <div style={{display: 'flex', justifyContent: 'space-evenly', marginLeft:60, alignItems:'flex-start'}}>
                    <div className='Version-grid'>
                        <Entry desc={VER} value={version}/>
                        <Entry desc={BUILT_ON} value={BuildTime}/>
                        <Entry desc={COMMIT} value={BuildCommit}/>
                        {major ? <Entry desc={FF_LIB} value={getFireflyLibraryVersionStr()}/> : ''}
                        {ffCommit && <Entry desc={FF_COM} value={ffCommit}/>}
                        {ffTag && <Entry desc={FF_TAG} value={ffTag}/>}
                    </div>
                    <CopyToClipboard style={{justifySelf: 'start', marginLeft:10}}
                                     value={versionAsText} size={16} buttonStyle={{backgroundColor: 'unset'}}/>
                </div>
            </div>
            {
                !isVersionFormalRelease() &&
                <div className='Version-grid-warning'>
                    {isVersionPreRelease() ?
                        `Warning: Early preview of Firefly ${getDevCycle()}` :
                        `Warning: Development build of Firefly on dev cycle ${getDevCycle()}`
                    }
                </div>
            }
            <Acknowledgement/>
        </div>
    );
}


const Acknowledgement= () => (
    <div style={{padding:'10px 5px 3px 5px', width:600, fontSize:'smaller'}}>
        <span>
            Firefly development at&nbsp;
        </span>
        <a href='https://ipac.caltech.edu' target='ipac-window'>IPAC</a>
        <span>
            &nbsp;has been supported by NASA, principally through&nbsp;
        </span>
        <a href='https://irsa.ipac.caltech.edu' target='ipac-window'>IRSA</a>
        <span>
           , and by the National Science Foundation, through the&nbsp;
        </span>
        <a href='https://www.lsst.org/' target='rubin-window'>Vera C. Rubin Observatory</a>
        <span>
            . Firefly is open-source software, available on&nbsp;
        </span>
        <a href='https://github.com/Caltech-IPAC/firefly' target='github-window'>GitHub</a>
        <span> and </span>
        <a href='https://hub.docker.com/repository/docker/ipac/firefly' target='dockerhub-window'>DockerHub</a>
        <span>.</span>
    </div>

);



export const showFullVersionInfoDialog = (title = '') => showInfoPopup( <VersionInfoFull/>, `${title} Version Information`, true, {maxWidth:620});
