import React from 'react';
import {
    getDevCycle, getFireflyLibraryVersionStr, getVersion, isVersionFormalRelease, isVersionPreRelease
} from 'firefly/Firefly.js';

import {CopyToClipboard} from '../visualize/ui/MouseReadout.jsx';
import {showInfoPopup} from './PopupUtil.jsx';
import './VersionInfo.css';
import IPAC_LOGO from 'images/ipac_logo-56x40.png';
import CALTECH_LOGO from 'images/caltech-new-logo.png';

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
        <div style={{display: 'flex', justifyContent: 'space-evenly'}}>
            <div style={{paddingRight:10, display: 'flex', flexDirection:'column', alignItems:'center'}}>
                <a href="https://www.caltech.edu" target='caltech-window'><img style={{width:70}} alt="Caltech" src={CALTECH_LOGO}/></a>
                <a href="https://www.ipac.caltech.edu" target='ipac-window'><img alt="IPAC" src={IPAC_LOGO}/></a>
            </div>
            <div className='Version-grid'>
                <Entry desc={VER} value={version}/>
                <Entry desc={BUILT_ON} value={BuildTime}/>
                <Entry desc={COMMIT} value={BuildCommit}/>
                {major ? <Entry desc={FF_LIB} value={getFireflyLibraryVersionStr()}/> : ''}
                {ffCommit && <Entry desc={FF_COM} value={ffCommit}/>}
                {ffTag && <Entry desc={FF_TAG} value={ffTag}/>}
                {
                    !isVersionFormalRelease() &&
                    <div className='Version-grid-warning'>
                        {isVersionPreRelease() ?
                            `Warning: Early preview of Firefly ${getDevCycle()}` :
                            `Warning: Development build of Firefly on dev cycle ${getDevCycle()}`
                        }
                    </div>
                }
            </div>
            <CopyToClipboard style={{justifySelf: 'end', marginLeft: 10}}
                             value={versionAsText} size={16} buttonStyle={{backgroundColor: 'unset'}}/>
        </div>
    );
}

export const showFullVersionInfoDialog = (title = '') => showInfoPopup( <VersionInfoFull/>, `${title} Version Information`);
