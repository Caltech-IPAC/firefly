import {Button, ChipDelete, FormHelperText, Stack, Typography} from '@mui/joy';
import React, {useEffect, useRef, useState} from 'react';
import {getAppOptions} from '../../core/AppDataCntlr';
import {FieldGroup} from '../../ui/FieldGroup';
import {InputField} from '../../ui/InputField';
import {ListBoxInputFieldView} from '../../ui/ListBoxInputField';
import {useFieldGroupValue} from '../../ui/SimpleComponent';
import {SwitchInputField} from '../../ui/SwitchInputField';
import {
    addGatorProtoUserService, deleteUserService, getGatorProtoService, getGatorProtoServiceOptions,
    getServiceNamesAsKey, GP_SERVICE_URL,
} from './GatorProtocolUtil';
import {IrsaCatalogSearch} from './IrsaCatalogSearch';

const DEFAULT_GP_PANEL_GROUP_KEY = 'GP_PANEL_GROUP_KEY';


export function GatorProtocolRootPanel({initArgs= {}, titleOn=false, title,
                                     lockService=false, lockedServiceUrl, lockedServiceName, lockTitle,
                                     groupKey=DEFAULT_GP_PANEL_GROUP_KEY }) {

    return (
        <FieldGroup groupKey={groupKey} keepState={true} key={groupKey} sx={{width: 1, height: 1}}>
            <GatorProtocolPanelImpl{...{initArgs, lockService, lockedServiceUrl, lockedServiceName, lockTitle, title}}/>
        </FieldGroup>
    );
}


function GatorProtocolPanelImpl({initArgs, lockedServiceUrl, lockedServiceName, lockService,
                                    lockTitle, titleOn, title= 'Gator Protocol'}) {

    const [getServiceUrl, setServiceUrl]= useFieldGroupValue(GP_SERVICE_URL);
    const serviceUrl= getServiceUrl();
    const gpOps= getGatorProtoServiceOptions();
    const servicesShowing= true;
    const showWarning= !serviceUrl;
    const [enterUrl,setEnterUrl]=  useFieldGroupValue('enterUrl');

    useEffect( () => {
        if (!serviceUrl) {
            const initUrl= getInitServiceUrl(initArgs,gpOps, lockedServiceUrl,lockedServiceName);
            setServiceUrl(initUrl);
            if (!initUrl) setEnterUrl(true);
        }
    },[]);


    const onGatorProtoServiceOptionSelect= (selectedOption) => {
        if (!selectedOption) return;
        const serviceUrl= selectedOption?.value;
        setServiceUrl(serviceUrl);
    };
    const searchOptionsMask= serviceUrl ?
        (getGatorProtoService(serviceUrl)?.searchOptionsMask ?? getAppOptions()?.gatorProtocol?.searchOptionsMask) : undefined;
    const showSqlSection= serviceUrl ? getGatorProtoService(serviceUrl)?.showSqlSection : true;

    return (
        <Stack height={1}>
            {titleOn &&<Typography {...{level:'h3', sx:{m:1} }}> Gator Protocol Searches </Typography>}
            <Services {...{serviceUrl, servicesShowing: (servicesShowing && !lockService),
                gpOps, onGatorProtoServiceOptionSelect, title}}/>
            {lockTitle && lockService &&
                <Typography {...{level:'title-lg', color:'primary', sx:{width:'17rem', mr:1, mb:1} }}>
                    {lockTitle}
                </Typography>
            }
            { showWarning ? <ServiceWarning {...{error:false,serviceUrl,title}}/> :
                <IrsaCatalogSearch {...{serviceUrl,
                    title: serviceUrl ? title : 'IRSA', searchOptionsMask, showSqlSection}} />
            }
        </Stack>
    );
}

function ServiceWarning({error,serviceUrl, title}) {
    if (!serviceUrl) {
        return (
            <Typography level='h3' color='warning' sx={{textAlign:'center', mt:5}}>
                {`Select a ${title} Service`}
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

function Services({serviceUrl, servicesShowing, gpOps, onGatorProtoServiceOptionSelect,title} ) {
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
                        Select Source
                    </Typography>
                    { Boolean(gpOps?.length) &&  <SwitchInputField {...{ size:'sm', endDecorator:'Enter my URL', fieldKey:'enterUrl',
                        initState:{value:false},
                        sx: {
                            alignSelf: 'flex-start',
                            '--Switch-trackWidth': '20px',
                            '--Switch-trackHeight': '12px',
                        },
                    }} /> }
                </Stack>
                <Stack>
                    {enterUrl ? (
                        <InputField orientation='horizontal'
                                    placeholder={`Enter a ${title} Url`}
                                    value={serviceUrl}
                                    actOn={['enter']}
                                    tooltip={`enter a ${title} URL or any URL that implements the Gator protocol`}
                                    slotProps={{input:{sx:{width:'40rem', height: '2.25rem'}}}}
                                    onChange={(val) => {
                                        onGatorProtoServiceOptionSelect(val);
                                        addGatorProtoUserService(val.value);
                                    }}
                        />

                    ) : (
                        <ListBoxInputFieldView {...{
                            sx:{'& .MuiSelect-root':{width:'46.5rem'}},
                            options:gpOps, value:serviceUrl,
                            placeholder:'Choose LSDB Service...',
                            startDecorator:!gpOps.length ? <Button loading={true}/> : undefined,
                            onChange:(ev, value) => {
                                onGatorProtoServiceOptionSelect({value});
                            },
                            renderValue:
                                ({value}) =>
                                    (<ServiceOpRender {...{ ops: gpOps, value,
                                        onGatorProtoServiceOptionSelect, clearServiceOnDelete:true}}/>),
                            decorator:
                                (label,value) => (<ServiceOpRender {...{ ops: gpOps, value,
                                    onGatorProtoServiceOptionSelect, clearServiceOnDelete:value===serviceUrl}}/>),
                        }} /> )}
                    <FormHelperText sx={{m: .25}}>
                        {enterUrl ? `Type the url of a ${title} service & press enter` : `Choose a ${title} service from the list`}
                    </FormHelperText>
                </Stack>
            </Stack>
        </Stack>
    );
}


function ServiceOpRender({ops, value, onGatorProtoServiceOptionSelect, sx, clearServiceOnDelete=false}) {
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
                                    if (clearServiceOnDelete) onGatorProtoServiceOptionSelect({value:''});
                                    else onGatorProtoServiceOptionSelect();
                                    e.stopPropagation?.();
                                }}
                    />
                }
            </Stack>
        </Stack>
    );
}

function getInitServiceUrl(initArgs,gpOps, lockedServiceUrl,lockedServiceName) {
    if (lockedServiceUrl) return lockedServiceUrl;
    if (lockedServiceName) {
        const url= gpOps.find( ({labelOnly}) => labelOnly===lockedServiceName)?.value;
        if (url) return url;
    }
    const {serviceUrl=gpOps?.[0]?.value} = gpOps;
    return serviceUrl;
}