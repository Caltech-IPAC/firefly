/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {parseWorldPt} from 'firefly/visualize/Point.js';
import {ServerParams} from 'firefly/data/ServerParams.js';
import {convertAngle} from 'firefly/visualize/VisUtil.js';
import {makeTblRequest, makeVOCatalogRequest} from 'firefly/tables/TableRequestUtil.js';
import {dispatchTableSearch} from 'firefly/tables/TablesCntlr.js';
import {LOCALFILE, UploadOptionsDialog} from 'firefly/ui/UploadOptionsDialog.jsx';
import {FieldGroup} from 'firefly/ui/FieldGroup.jsx';
import {getWorkspaceConfig} from 'firefly/visualize/WorkspaceCntlr.js';
import {VoSearchPanel} from 'firefly/ui/VoSearchPanel.jsx';
import {NedSearchPanel} from 'firefly/ui/NedSearchPanel.jsx';
import {irsaCatalogGroupKey} from 'firefly/visualize/ui/CatalogSelectViewPanel.jsx';
import {FormPanel} from 'firefly/ui/FormPanel.jsx';
import {showInfoPopup} from 'firefly/ui/PopupUtil.jsx';



const FormTemplate= ({children, onSuccess,groupKey, help_id}) => (
        <div style={{width:'100%'}} >
            <FormPanel width='auto' height='auto' groupKey={groupKey} onSubmit={onSuccess}
                       params={{hideOnInvalid: false}} buttonStyle={{justifyContent: 'left'}}
                       submitBarStyle={{padding: '2px 3px 3px'}} help_id = {help_id} >
                <FieldGroup groupKey={groupKey} keepState={true}> {children} </FieldGroup>
            </FormPanel>
        </div>
    );


export const ClassicCatalogUploadPanel= () => (
    <FormTemplate groupKey='CLASSIC_UPLOAD' onSuccess={(request) => doLoadTable(request)} help_id= 'catalogs.owncatalogs'>
        <div style={{width:'100%'}}>
            <div style={{width:750, height:'calc(100% - 40px)', padding: 20, display: 'flex', flexDirection:'column', justifyContent: 'space-between'}}>
                <div style={{width: '70%'}}>
                    <UploadOptionsDialog fromGroupKey={irsaCatalogGroupKey}
                                         preloadWsFile={false}
                                         fieldKeys={{local: 'fileUpload',
                                             workspace: 'workspaceUpload',
                                             location: 'fileLocation'}}
                                         workspace={getWorkspaceConfig()}
                                         tooltips={{local: 'Select an IPAC catalog table file to upload',
                                             workspace: 'Select an IPAC catalog table file from workspace to upload'}}/>
                    <div>
                        <em style={{color:'gray'}}>Custom catalog in IPAC, CSV, TSV, VOTABLE, or FITS table format</em>
                    </div>
                </div>
            </div>
        </div>
    </FormTemplate>
);

export const ClassicVOCatalogPanel= () => (
    <FormTemplate groupKey='VO_CLASSIC' onSuccess={(request) => doVoSearch(request)} help_id='catalogs.voscs' >
        <VoSearchPanel fieldKey='vopanel'/>
    </FormTemplate>
);

export const ClassicNedSearchPanel= () => (
    <FormTemplate groupKey='NED_CLASSIC' onSuccess={(request) => doVoSearch(request,'NED')} help_id= 'catalogs.nedcatalogs'>
        <NedSearchPanel/>
    </FormTemplate>
);



function doLoadTable(request) {
    const fileLocation = request?.fileLocation ?? LOCALFILE;
    const tReq = makeTblRequest('userCatalogFromFile', '', {
        filePath: ( fileLocation === LOCALFILE) ? request.fileUpload : request.workspaceUpload,
        sourceFrom: fileLocation
    });
    dispatchTableSearch(tReq);
}


/**
 * VO search using 'ConeSearchByURL' search processor
 * N.B.: radius in degree!
 * @param request
 * @param providerName serves to distinguish between any user input SCS provider and specific provider such as 'NED'
 */
function doVoSearch(request, providerName = '') {
    //VO url that work http://vizier.u-strasbg.fr/viz-bin/votable/-A?-source=J/A+A/402/549
    let radius;//arcsec
    let accessUrl;//.replace('&', 'URL_PARAM_SEP');
    const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
    if (!wp) {
        showInfoPopup('Target is required');
        return false;
    }
    const nameUsed = wp.getObjName() || wp.toString();
    let name;
    let conesize;

    if(providerName === 'NED'){
        accessUrl = 'http://ned.ipac.caltech.edu/cgi-bin/NEDobjsearch?search_type=Near+Position+Search&of=xml_main&';//http://vo.ned.ipac.caltech.edu/services/sia?TARGET='
        conesize = request.nedconesize;
        radius = convertAngle('deg', 'arcsec', conesize);
        name = `${nameUsed} (NED SCS ${radius}")`;
    }else{
        accessUrl = request.vourl.trim();//.replace('&', 'URL_PARAM_SEP');
        conesize = request.conesize;
        radius = convertAngle('deg', 'arcsec', conesize);
        name = `${nameUsed} (VO SCS ${radius}")`;
    }
    const tReq = makeVOCatalogRequest(name,
        {
            [ServerParams.USER_TARGET_WORLD_PT]: request[ServerParams.USER_TARGET_WORLD_PT],
            SearchMethod: 'Cone',
            radius: conesize, //degree!
            providerName,
            accessUrl
        }
    );
    dispatchTableSearch(tReq, {backgroundable:true});
}
