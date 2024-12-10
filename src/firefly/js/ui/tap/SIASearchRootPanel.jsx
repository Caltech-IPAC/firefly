/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import {Box, Button, ChipDelete, FormHelperText, IconButton, Skeleton, Stack, Typography} from '@mui/joy';
import React, {useContext, useEffect, useRef, useState} from 'react';
import {getAppOptions} from '../../core/AppDataCntlr';
import {dispatchHideDialog} from '../../core/ComponentCntlr';
import {dispatchHideDropDown} from '../../core/LayoutCntlr';
import {MetaConst} from '../../data/MetaConst';
import {makeFileRequest, setNoCache} from '../../tables/TableRequestUtil';
import {dispatchTableSearch} from '../../tables/TablesCntlr';
import {intValidator} from '../../util/Validate';
import {FieldGroup, FieldGroupCtx} from '../FieldGroup.jsx';
import {FormPanel} from '../FormPanel';
import {InputField} from '../InputField';
import {ListBoxInputFieldView} from '../ListBoxInputField';
import {showInfoPopup, showYesNoPopup} from '../PopupUtil';
import {useFieldGroupMetaState, useFieldGroupValue} from '../SimpleComponent.jsx';
import {SwitchInputField} from '../SwitchInputField';
import {ValidationField} from '../ValidationField';
import {ConstraintContext} from './Constraints';
import {showResultTitleDialog} from './ResultTitleDialog';
import {SiaUI} from './SiaUI';
import {getMaxrecHardLimit, makeNumberedTitle} from './TapUtil';
import {
    SIA_SERVICE_META, SIA_SERVICE_URL, SIA_USER_ENTERED_TITLE,
    addUserService,
    defSiaBrowserState, deleteUserService, getServiceNamesAsKey, getSiaServiceLabel, getSiaServiceOptions,
    loadSiaV2Meta, siaHelpId, getServiceHiPS
} from './SiaUtil.js';

const DEFAULT_SIA_PANEL_GROUP_KEY = 'SIAv2_PANEL_GROUP_KEY';


export function SIAv2SearchPanel({initArgs= {}, titleOn=false,
                                   lockService=false, lockedServiceUrl, lockedServiceName, lockTitle,
                                   groupKey=DEFAULT_SIA_PANEL_GROUP_KEY}) {

    return (
        <FieldGroup groupKey={groupKey} keepState={true} key={groupKey} sx={{width: 1, height: 1}}>
            <SIAV2SearchPanelImpl {...{initArgs, titleOn, lockService, lockedServiceUrl, lockedServiceName, lockTitle}}/>
        </FieldGroup>
    );
}



function SIAV2SearchPanelImpl({initArgs, titleOn, lockService, lockedServiceUrl, lockTitle, lockedServiceName}) {

    const {groupKey}= useContext(FieldGroupCtx);
    const [getServiceUrl, setServiceUrl]= useFieldGroupValue(SIA_SERVICE_URL);
    const [getServiceMeta, setServiceMeta]= useFieldGroupValue(SIA_SERVICE_META);
    const [getUserTitle,setUserTitle]= useFieldGroupValue(SIA_USER_ENTERED_TITLE);
    const [getSiaState,setSiaState]= useFieldGroupMetaState(defSiaBrowserState);
    const [servicesShowing, setServicesShowingInternal]= useState(getSiaState().lastServicesShowing);
    const [srvNameKey, setSrvNameKey]= useState(() => getServiceNamesAsKey());
    const [working, setWorking]= useState(() => false);
    const {current:clickFuncRef} = useRef({clickFunc:undefined});

    const siaOps= getSiaServiceOptions();
    const serviceUrl= getServiceUrl();

    useEffect( () => {
        if (!serviceUrl) {
            setServiceUrl(getInitServiceUrl(initArgs,siaOps, lockedServiceUrl,lockedServiceName));
        }
    },[]);

    useEffect(() => {
        if (!serviceUrl) return;
        setWorking(true);
        loadSiaV2Meta(serviceUrl).then( (meta) =>{
            setWorking(false);
            setServiceMeta(meta);
        });
    }, [serviceUrl]);


    const onSiaServiceOptionSelect= (selectedOption) => {
        if (!selectedOption) {
            setSrvNameKey(getServiceNamesAsKey());
            return;
        }
        const serviceUrl= selectedOption?.value;
        setServiceUrl(serviceUrl);
        setSrvNameKey(getServiceNamesAsKey());
        setSiaState( {...getSiaState(), }); // todo what should go here?
    };

    const ctx= {
        setConstraintFragment: (key,value) => {
            value ?
                getSiaState().constraintFragments.set(key,value) :
                getSiaState().constraintFragments.delete(key);
        }
    };

    const setServicesShowing= (showing) => {
        setServicesShowingInternal(showing);
        setSiaState({...getSiaState(), lastServicesShowing:showing});
    };

    return (
        <Box width={1} height={1}>
            <ConstraintContext.Provider value={ctx}>
                <FormPanel  onSuccess={(request) => onSIAv2SearchSubmit(request, serviceUrl, getServiceMeta(), getSiaState())}
                            cancelText=''
                            help_id = {siaHelpId('form')}
                            slotProps={{
                                completeBtn: {
                                    getDoOnClickFunc: (clickFunc) => (clickFuncRef.clickFunc= clickFunc),
                                    requireAllValid:false,
                                    includeUnmounted:true
                                },
                                input: {
                                    sx: {
                                        flex: '1 1 auto',
                                        overflow: 'scroll',
                                        height: 1,
                                    }
                                },
                                searchBar: {
                                    sx: {flex: '0 1 2rem',},
                                    px:1, py:1/2, alignItems:'center',
                                    actions: makeExtraWidgets(groupKey, initArgs,
                                        getUserTitle, setUserTitle, getSiaState())
                                }
                            }}>

                    <SiaPanelBase {...{
                        siaMeta:getServiceMeta(),
                        servicesShowing, setServicesShowing, lockService,
                        onSiaServiceOptionSelect,
                        lockedServiceName, srvNameKey, lockTitle,
                        initArgs, serviceUrl, titleOn, siaOps, working
                    }} />
                </FormPanel>
            </ConstraintContext.Provider>
        </Box>
    );
}


function SiaPanelBase ({serviceUrl, initArgs, siaMeta, onSiaServiceOptionSelect, siaOps, lockedServiceName, lockTitle,
                           servicesShowing, setServicesShowing, lockService, working, titleOn=true})  {
    const serviceLabel= getSiaServiceLabel(serviceUrl);
    const [error, setError] = useState(undefined);

    const showWarning= !siaMeta || !serviceUrl;

    useEffect(() => {
        if (error) setServicesShowing(true);
    }, [error]);

    useEffect(() => {
        if (!serviceUrl) setServicesShowing(true);
        if (serviceUrl) setError(undefined);
    }, [serviceUrl]);

    if (working) return <Skeleton/>;
    

    return (
        <Stack height={1}>
            {titleOn &&<Typography {...{level:'h3', sx:{m:1} }}> SIAv2 Searches </Typography>}
            <Services {...{serviceUrl, servicesShowing: (servicesShowing && !lockService),
                siaOps, onSiaServiceOptionSelect}}/>
            {lockTitle && lockService &&
                <Typography {...{level:'title-lg', color:'primary', sx:{width:'17rem', mr:1, mb:1} }}>
                    {lockTitle}
                </Typography>
            }
            { showWarning ?
                <ServiceWarning {...{error,serviceUrl,siaMeta}}/> :
                <SiaUI  {...{
                    serviceUrl, serviceLabel, initArgs, lockService,
                    lockedServiceName,  siaMeta,

                    servicesShowing, setServicesShowing, setError
                }} />
            }
        </Stack>
    );
}


function ServiceWarning({error,serviceUrl,siaMeta}) {
    if (!serviceUrl) {
        return (
            <Typography level='h3' color='warning' sx={{textAlign:'center', mt:5}}>Select a SIAv2 Service</Typography>
        );
    }
    else if (!siaMeta) {
        return (
            <Typography level='h3' color='warning' sx={{textAlign:'center', mt:5}}>
                This SIAv2 service is not responding or unavailable. Select another SIAv2 Service
            </Typography>
        );
    }
    else if (error)  {
        return (
            <Stack>
                <Typography level='h3' color='warning' sx={{textAlign:'center', mt:5}}>Error</Typography>
                <Typography color='warning' sx={{textAlign:'center', mt:5}}>{error}</Typography>
            </Stack>
        );
    }
    return <div/>;
}

function Services({serviceUrl, servicesShowing, siaOps, onSiaServiceOptionSelect} ) {
    const [extraStyle,setExtraStyle] = useState({overflow:'hidden'});
    const enterUrl=  useFieldGroupValue('enterUrl')[0]();

    useEffect(() => {
        if (servicesShowing) {
            setTimeout(() => setExtraStyle({overflow:'visible'}), 250);
        }
        else {
            setExtraStyle({overflow:'hidden'});
        }
    }, [servicesShowing]);

    return (
        <Stack sx={{
            height: servicesShowing ? 'auto' : 0,
            pb: servicesShowing ? 1.5 : 0,
            justifyContent:'space-between',
            alignItems:'center',
            transition: 'all .2s ease-in-out', //to animate height changes (hide/show Services)
            ...extraStyle}}>
            <Stack direction='row' spacing={1} sx={{alignItems:'center', width:1}}>
                <Stack alignItems='flex-start' spacing={1}>
                    <Typography {...{level:'title-lg', color:'primary', sx:{width:'17rem', mr:1} }}>
                        Select SIAv2 Service
                    </Typography>
                    <SwitchInputField {...{ size:'sm', endDecorator:'Enter my URL', fieldKey:'enterUrl',
                        initState:{value:false},
                        sx: {
                            alignSelf: 'flex-start',
                            '--Switch-trackWidth': '20px',
                            '--Switch-trackHeight': '12px',
                        },
                    }} />
                </Stack>
                <Stack>
                    {enterUrl ? (
                        <InputField orientation='horizontal'
                                    placeholder='Enter SIAv2 Url'
                                    value={serviceUrl}
                                    actOn={['enter']}
                                    tooltip='enter SIAv2 URL'
                                    slotProps={{input:{sx:{width:'40rem', height: '2.25rem'}}}}
                                    onChange={(val) => {
                                        onSiaServiceOptionSelect(val);
                                        addUserService(val.value);
                                    }}
                        />

                    ) : (
                        <ListBoxInputFieldView {...{
                            sx:{'& .MuiSelect-root':{width:'46.5rem'}},
                            options:siaOps, value:serviceUrl,
                            placeholder:'Choose SIAv2 Service...',
                            startDecorator:!siaOps.length ? <Button loading={true}/> : undefined,
                            onChange:(ev, value) => {
                                onSiaServiceOptionSelect({value});
                            },
                            renderValue:
                                ({value}) =>
                                    (<ServiceOpRender {...{ ops: siaOps, value,
                                        onSiaServiceOptionSelect, clearServiceOnDelete:true}}/>),
                            decorator:
                                (label,value) => (<ServiceOpRender {...{ ops: siaOps, value,
                                    onSiaServiceOptionSelect, clearServiceOnDelete:value===serviceUrl}}/>),
                        }} /> )}
                    <FormHelperText sx={{m: .25}}>
                        {enterUrl ? 'Type the url of a TAP service & press enter' : 'Choose a SIAv2 service from the list'}
                    </FormHelperText>
                </Stack>
            </Stack>
        </Stack>
    );
}


function ServiceOpRender({ops, value, onSiaServiceOptionSelect, sx, clearServiceOnDelete=false}) {
    const op = ops.find((t) => t.value === value);
    if (!op) return 'none';
    return (
        <Stack {...{alignItems:'flex-start', sx:{...sx, width:1}}}>
            <Stack {...{direction:'row', spacing:5, alignItems:'center', justifyContent:'space-between', width:1}}>
                <Stack {...{direction:'row', spacing:1, alignItems:'center'}}>
                    <Typography level='title-md'>
                        {`${op.labelOnly}: `}
                    </Typography>
                    <Typography level='body-md' >
                        {op.value}
                    </Typography>
                </Stack>
                { op.userAdded &&
                    <ChipDelete component='div' size='sm' sx={{zIndex:2}}
                                onClick={(e) => {
                                    deleteUserService(value);
                                    if (clearServiceOnDelete) onSiaServiceOptionSelect({value:''});
                                    else onSiaServiceOptionSelect();
                                    e.stopPropagation?.();
                                }}
                    />
                }
            </Stack>
        </Stack>
    );
}

function makeExtraWidgets(groupKey, initArgs, selectBy, setSelectBy, getUserTitle, setUserTitle) {
    const extraWidgets = [
        (<ValidationField {...{
            orientation: 'horizontal', fieldKey: 'maxrec', key: 'maxrec', groupKey,
            tooltip: 'Maximum number of rows to return (via MAXREC)', label: 'Row Limit:',
            validator: intValidator(1, getMaxrecHardLimit(), 'Maximum number of rows'),
            initialState: {
                value: Number(initArgs?.urlApi?.MAXREC) || Number(getAppOptions().tap?.defaultMaxrec ?? 50000),
            },
            sx: {pl: 3},
            slotProps : {
                input : {sx: { width: '8em' } }
            },
        }}/>)
    ];
    extraWidgets.push( <TitleCustomizeButton {...{key:'setTitle', groupKey}}/> );


    return extraWidgets;
}

function TitleCustomizeButton({groupKey}) {

    const [getUserTitle,setUserTitle]= useFieldGroupValue(SIA_USER_ENTERED_TITLE,groupKey);

    const getDefTitle= () =>'SIAv2 search'; // todo - figure out how to make a SIAv2 default title

    const onClick= () => {
        const defTitle= getDefTitle();
        showResultTitleDialog(getUserTitle(), defTitle, (newTitle) => setUserTitle(newTitle===defTitle ? undefined : newTitle) );
    };

    const title= getUserTitle() || getDefTitle();
    if (!title) return undefined;

    return (
        <Stack direction='row' alignItems='center' sx={{pl:3}}>
            <IconButton onClick={onClick} sx={{minWidth:0}}>
                <EditOutlinedIcon/>
            </IconButton>
            <Typography
                {...{
                    level: 'body-sm',
                    sx: {
                        textAlign:'left',
                        width: '13rem',
                        textWrap: 'nowrap',
                        textOverflow: 'ellipsis',
                        overflow: 'hidden'
                    }
                }}
            >
                <Typography level='title-md'>Title: </Typography>
                {`${getUserTitle() || getDefTitle() || ''}`}
            </Typography>

        </Stack>
    );
}

function getInitServiceUrl(initArgs,siaOps, lockedServiceUrl,lockedServiceName) {
    if (lockedServiceUrl) return lockedServiceUrl;
    if (lockedServiceName) {
        const url= siaOps.find( ({labelOnly}) => labelOnly===lockedServiceName)?.value;
        if (url) return url;
    }
    const {serviceUrl=siaOps[0].value} = siaOps;
    // initServiceUsingAPIOnce(true, () => {
    //     if (initArgs?.urlApi?.service) serviceUrl= initArgs.urlApi.service;
    // });
    return serviceUrl;
}




const noRowLimitMsg = (
    <div style={{width: 260}}>
        Disabling the row limit is not recommended. <br/>
        This may results in a HUGE amount of data. <br/><br/>
        Are you sure you want to continue?
    </div>
);


function onSIAv2SearchSubmit(request, serviceUrl, siaMeta, siaState, showErrors=true) {

    const cAry= [...siaState.constraintFragments.values()];
    const userTitle= request[SIA_USER_ENTERED_TITLE];

    const errors= cAry
        .map( (f) =>  f.constraintErrors.length ? f.constraintErrors : [])
        .filter( (c) => c?.length)
        .flat();

    if (errors.length) {
        if (showErrors) showInfoPopup(errors[0], 'Error');
        return false;
    }

    const constraints= cAry.map( (f) =>  f.siaConstraints).filter( (c) => c?.length).flat();

    if (!constraints.length && !errors.length) {
        if (showErrors) showInfoPopup('You much enter some search parameters', 'Error');
        return false;
    }

    const cStr= constraints.join('&');
    const hasMaxrec = !isNaN(parseInt(request.maxrec));
    const maxrec = parseInt(request.maxrec);
    var baseRequestUrl= `${serviceUrl}?${cStr}`;


    const doSubmit = () => {

        const url= `${baseRequestUrl}${hasMaxrec?'&MAXREC='+maxrec : ''}`;
        const additionalSiaMeta= {serviceLabel: getSiaServiceLabel(serviceUrl)};
        const hips= getServiceHiPS(serviceUrl);
        if (hips) additionalSiaMeta[MetaConst.COVERAGE_HIPS]= hips;
        const title= makeNumberedTitle(userTitle || 'SIA Search');
        const treq= makeFileRequest(title,new URL(url).toString());
        treq.META_INFO= {...treq.META_INFO, ...additionalSiaMeta};
        console.log('sia search: ' + url, new URL(url).toString());
        dispatchTableSearch(treq, {backgroundable: true, showFilters: true, showInfoButton: true});
    };

    if (!hasMaxrec ) {
        showYesNoPopup(noRowLimitMsg,(id, yes) => {
            if (yes) {
                doSubmit();
                dispatchHideDropDown();
            }
            dispatchHideDialog(id);
        });
    } else {
        doSubmit();
        return true;
    }
    return false;
}