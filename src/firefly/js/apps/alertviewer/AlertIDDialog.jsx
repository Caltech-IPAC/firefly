import React, {useEffect} from 'react';
import {Box, Button, Card, FormLabel, List, ListItem, ListItemButton, Stack, Typography} from '@mui/joy';
import {FieldGroup} from 'firefly/ui/FieldGroup';
import {ValidationField} from 'firefly/ui/ValidationField';
import CompleteButton from 'firefly/ui/CompleteButton.jsx';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {PopupPanel} from 'firefly/ui/PopupPanel.jsx';
import {useFieldGroupValue} from 'firefly/ui/SimpleComponent';
import {dispatchHideDialog, dispatchShowDialog} from 'firefly/core/ComponentCntlr';
import {dispatchAddPreference, getPreference} from 'firefly/core/AppDataCntlr';

const DIALOG_ID = 'alertIdDialog';
const ID_KEY = 'alertId';
const GROUP_KEY = 'ALERT_ID_OPTIONS';

let lastClick = 0;

export const ALERT_ID_LIST_PREF = 'AlertViewer.RecentUrls';
const MAX_ID_LEN = 40; //todo: arbitrary, confirm later...

export function getRecentAlertIDs() {
    return getPreference(ALERT_ID_LIST_PREF, []);
}

export function addToRecentAlertIDs(id) {
    let idAry = getRecentAlertIDs();
    if (idAry.includes(id)) {
        idAry = idAry.filter((u) => u !== id);
    } else if (idAry.length > MAX_ID_LEN - 1) {
        idAry = idAry.slice(0, MAX_ID_LEN - 1);
    }
    idAry.unshift(id);
    dispatchAddPreference(ALERT_ID_LIST_PREF, idAry);
}

export function showAlertIdDialog(currId, onChange) {
    const popup = (
        <PopupPanel title={'Recent Alerts'}>
            <AlertIdPanel {...{currId, onChange}}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(DIALOG_ID, popup);
    dispatchShowDialog(DIALOG_ID);
}

function AlertIdPanel({currId, onChange}) {
    const [getId, setId] = useFieldGroupValue(ID_KEY, GROUP_KEY);

    useEffect(() => {
        setId(currId || '');
    }, [currId]);

    return (
        <FieldGroup groupKey={GROUP_KEY} sx={{minWidth:'40rem', minHeight:'28rem',
            display:'flex', flexDirection:'column', overflow:'auto', resize:'both'}}>
            <Stack spacing={2} sx={{p:1, height:1}}>
                <ValidationField
                    groupKey={GROUP_KEY}
                    fieldKey={ID_KEY}
                    label='Enter Alert URL or ID'
                    initialState={{value:''}}
                    onKeyPress={(ev, v) => {
                        if (ev.key === 'Enter') handleSuccess(v, onChange, true);
                    }}
                    showFeedback={true}
                />

                <Stack spacing={0.5} sx={{flex:'1 1 auto'}}>
                    <FormLabel>Or choose from recent IDs</FormLabel>
                    <Card sx={{'--Card-padding':'1px', flex:'1 1 auto'}}>
                        <Box sx={{width:1, overflow:'auto', height:'17rem'}}>
                            {getRecentAlertIDs()?.length ? (
                                <List sx={{'--ListItem-minHeight': '.5rem'}}>
                                    {getRecentAlertIDs().map((u, idx) => (
                                        <ListItem key={idx}>
                                            <ListItemButton onClick={() => {
                                                if (u === getId() && Date.now() - lastClick < 1000) {
                                                    handleSuccess(u, onChange, true);
                                                } else {
                                                    setId(u);
                                                    lastClick = Date.now();
                                                }
                                            }}>
                                                {u}
                                            </ListItemButton>
                                        </ListItem>
                                    ))}
                                </List>
                            ) : (
                                <Typography color='warning' level='title-lg' sx={{textAlign:'center', mt:5}}>
                                    No recent IDs
                                </Typography>
                            )}
                        </Box>
                    </Card>
                </Stack>

                <Stack direction='row' justifyContent='space-between' sx={{pt:1}}>
                    <Stack direction='row' spacing={1}>
                        <CompleteButton
                            text='Use ID'
                            dialogId={DIALOG_ID}
                            onSuccess={(r) => handleSuccess(r[ID_KEY], onChange, true)}
                        />
                        <Button size='md' onClick={() => setId(currId || '')}>Restore current</Button>
                    </Stack>
                </Stack>
            </Stack>
        </FieldGroup>
    );
}

function handleSuccess(url, onChange, close=false) {
    const u = (url || '').trim();
    if (u) addToRecentAlertIDs(u);
    onChange?.(u);
    if (close) dispatchHideDialog(DIALOG_ID);
}