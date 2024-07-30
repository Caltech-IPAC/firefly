/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {useEffect} from 'react';
import {dispatchAddPreference, getPreference} from '../../core/AppDataCntlr';
import {dispatchHideDialog, dispatchShowDialog} from '../../core/ComponentCntlr';
import {FieldGroup} from '../FieldGroup';
import {ValidationField} from '../ValidationField';
import CompleteButton from '../CompleteButton.jsx';
import DialogRootContainer from '../DialogRootContainer.jsx';
import {PopupPanel} from '../PopupPanel.jsx';
import {useFieldGroupValue} from '../SimpleComponent';

import HelpIcon from '../HelpIcon.jsx';
import {Box, Button, Card, FormLabel, List, ListItem, ListItemButton, Stack, Typography} from '@mui/joy';
import {tapHelpId} from './TapUtil';

const DIALOG_ID= 'resultTitleDialog';
const SEARCH_TITLE_KEY= 'searchTitle';
const GROUP_KEY= 'TITLE_OPTIONS';


export function showResultTitleDialog(userTitle, defaultTitle, onChange) {
    const popup = (
        <PopupPanel title={'Search Result Title'}>
                <ResultTitlePanel{...{userTitle, defaultTitle, onChange}}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(DIALOG_ID, popup);
    dispatchShowDialog(DIALOG_ID);
}


const DIALOG_HEIGHT= '28rem';
const RECENT_TITLE_HEIGHT= '17rem';
let lastClick;


function ResultTitlePanel({userTitle, defaultTitle, onChange}) {
    const [getTitle,setTitle]= useFieldGroupValue(SEARCH_TITLE_KEY,GROUP_KEY);

    useEffect(() => {
        setTitle(userTitle || defaultTitle);
    },[defaultTitle]);

    return (
        <FieldGroup groupKey={GROUP_KEY} sx={{minWidth:'35rem', minHeight:DIALOG_HEIGHT,
            display:'flex', flexDirection:'column', overflow:'auto', resize:'both'}}>
            <Stack {...{alignItems:'center', spacing:1, width:1, height:1, pt:1, flex:'1 1 auto' }} >
                <Stack width={1} height={1} spacing={4} flex='1 1 auto'>
                    <ValidationField fieldKey={SEARCH_TITLE_KEY} showFeedback={true} label='Search Title'
                                     onKeyPress={(ev,currValue) => {
                                         if (ev.key === 'Enter') {
                                             handleSuccess(currValue,defaultTitle,onChange,true);
                                         }
                                     }}
                                     initialState={{ value:'' }} />
                    <Stack {...{spacing:1/2, height:1, minWidth:440, sx:{flex: '1 1 auto'} }} >
                        <FormLabel>Or choose from recent titles</FormLabel>
                        <Card sx={{'--Card-padding': '1px', height: 1, sx:{flex: '1 1 auto'}}}>
                            <Box sx={{  width: 1, overflow:'auto', height:RECENT_TITLE_HEIGHT, flex:'1 1 auto'}} >
                                {getRecentTitles()?.length ?
                                    <List sx={{'--ListItem-minHeight': '.5rem'}}>
                                        {
                                            getRecentTitles().map( (t,idx) => (
                                                    <ListItem key={idx + ''}>
                                                        <ListItemButton onClick={ () => {
                                                            if (t===getTitle() && Date.now()-lastClick<1000) {
                                                                handleSuccess(t,defaultTitle,onChange,true);
                                                            }
                                                            else {
                                                                setTitle(t);
                                                                lastClick= Date.now();
                                                            }
                                                        }}>
                                                            {t}
                                                        </ListItemButton>
                                                    </ListItem>
                                                )
                                            )
                                        }
                                    </List>:
                                    <Typography color='warning' level='title-lg' sx={{textAlign:'center', mt:5}}>
                                        No recent search titles
                                    </Typography>
                                }
                            </Box>
                        </Card>
                    </Stack>
                </Stack>
                <Stack {...{textAlign:'center', direction:'row', justifyContent:'space-between', width:1, px:1, pb:1}}>
                    <Stack {...{textAlign:'center', direction:'row', spacing:1 }}>
                        <CompleteButton text='Update Title' dialogId={DIALOG_ID}
                                        onSuccess={(r) => handleSuccess(r[SEARCH_TITLE_KEY],defaultTitle,onChange) }/>
                        {defaultTitle!==getTitle() &&
                            <Button size='md' onClick={() => setTitle(defaultTitle)} > Restore default title </Button>}
                    </Stack>
                    <HelpIcon helpId={tapHelpId('title')} />
                </Stack>
            </Stack>
        </FieldGroup>
    );
}


function handleSuccess(title,defaultTitle,onChange, close=false) {
    if (title && title!==defaultTitle) {
        addToRecentTitles(title);
    }
    onChange?.(title);
    close && dispatchHideDialog(DIALOG_ID);
}


export const TITLE_LIST_PREF= 'RecentTitles';
const MAX_TITLE_LEN= 40;

function addToRecentTitles(title, setRecentAry) {
    let tAry= getRecentTitles();
    if (tAry.find( (t) => t===title)) {
        tAry= tAry.filter( (t) => t!==title);
    }
    else {
        if (tAry.length > MAX_TITLE_LEN-1) tAry = tAry.slice(0, MAX_TITLE_LEN-1);
    }
    tAry.unshift(title);
    dispatchAddPreference(TITLE_LIST_PREF, tAry);
    setRecentAry?.(tAry);
}


function getRecentTitles() {
    return getPreference(TITLE_LIST_PREF, []);
}
