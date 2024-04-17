import {Box, Link, Stack, Typography} from '@mui/joy';
import React from 'react';
import {array, arrayOf, func, object, oneOfType, string} from 'prop-types';
import {CONE_CHOICE_KEY} from '../../visualize/ui/CommonUIKeys.js';
import {CheckboxGroupInputField} from '../CheckboxGroupInputField.jsx';
import {FormPanel} from '../FormPanel.jsx';
import {showInfoPopup} from '../PopupUtil.jsx';
import {useFieldGroupValue} from '../SimpleComponent.jsx';
import {hasSpatialTypes, isSpatialTypeSupported} from './DLGenAnalyzeSearch.js';
import {CONE_AREA_KEY} from './DynamicDef.js';

export function DLuiRootSearchPanel({groupKey, children, submitSearch, setClickFunc, docRows, concurrentSearchDef}) {

    const coneAreaChoice = useFieldGroupValue(CONE_AREA_KEY)?.[0]() ?? CONE_CHOICE_KEY;
    let cNames, disableNames;
    if (concurrentSearchDef?.length && coneAreaChoice) {
        cNames = concurrentSearchDef
            .filter((c) => hasSpatialTypes(c.serviceDef) && isSpatialTypeSupported(c.serviceDef, coneAreaChoice))
            .map((c) => c.desc);
        disableNames = concurrentSearchDef
            .filter((c) => hasSpatialTypes(c.serviceDef) && !isSpatialTypeSupported(c.serviceDef, coneAreaChoice))
            .map((c) => c.desc);
    }

    const disDesc = coneAreaChoice === CONE_AREA_KEY ? 'w/ cone' : 'w/ polygon';

    const options = (cNames || disableNames) ? (
        <Stack {...{alignItems: 'flex-start',}}>
            {Boolean(cNames?.length) &&
                <CheckboxGroupInputField
                    wrapperStyle={{marginTop: 5}} fieldKey='searchOptions'
                    alignment='horizontal' labelWidth={115} labelStyle={{fontSize: 'larger'}}
                    label={`Include Search${cNames.length > 1 ? 'es' : ''} of: `}
                    options={cNames.map((c) => ({label: c, value: c}))}
                    initialState={{value: cNames.join(' '), tooltip: 'Additional Searches', label: ''}}
                />}
            {Boolean(disableNames?.length) && (
                <Stack {...{direction: 'row'}}>
                    <Typography color='warning'>
                        <span>{`Warning - search${disableNames.length > 1 ? 'es' : ''} disabled ${disDesc}:`}</span>
                    </Typography>
                    {disableNames.map((d) => (<Typography level='body-sm'>{d}</Typography>))}
                </Stack>)
            }
        </Stack>

    ) : undefined;

    return (
        <FormPanel groupKey={groupKey}
                   onSuccess={submitSearch}
                   onError={() => showInfoPopup('Fix errors and search again', 'Error')}
                   help_id={'search-collections-general'}
                   slotProps={{
                       input: {border:0, p:0, mb:1},
                       cancelBtn: {component:null},
                       searchBar: {actions: <DocRows key='root' docRows={docRows}/>},
                       completeBtn: {
                           getDoOnClickFunc:(f) => setClickFunc({onClick: f})
                       }
                   }}>
            <Stack {...{alignItems: 'flex-start',}}>
                {children}
                {options}
            </Stack>
        </FormPanel>
    );
}


DLuiRootSearchPanel.propTypes= {
    groupKey: string,
    children: oneOfType([object,array]),
    submitSearch: func,
    setClickFunc: func,
    docRows: array,
    concurrentSearchDef: arrayOf(object),
};

const DocRows= ({docRows=[], showLabel=true}) => (
    <Box key='help' sx={{px:1, pb:1/2, alignSelf: 'flex-end'}}>
        {
            docRows.map((row, idx) => (
                <Link level='body-sm'
                      href={row.accessUrl} key={idx + ''} target='documentation'>
                    {showLabel && `Documentation: ${row.desc}`}
                </Link>)
            )
        }
    </Box>
);

