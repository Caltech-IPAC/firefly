import React, { useState } from 'react';
import { AutoCompleteInput, AutoCompleteInputView, useAsyncOptions, FieldGroup } from '../../index.js';

export default {
    title: 'Inputs/AutoCompleteInput',
    component: AutoCompleteInput,
    tags: ['autodocs'],
    parameters: {
        controls: { disable: true }, actions: { disable: true },
    },
    decorators: [
        (Story, ctx) => (
            <FieldGroup groupKey={ctx.id}>
                <Story />
            </FieldGroup>
        ),
    ],
};

const CATALOGS = [
    'AllWISE', 'CatWISE2020', '2MASS Point Source', '2MASS Extended Source',
    'Gaia DR3', 'Gaia DR2', 'SDSS DR17', 'SDSS DR16',
    'GALEX AIS', 'GALEX MIS', 'Spitzer IRAC', 'Herschel PACS',
    'AKARI FIS', 'IRAS PSC', 'MSX Galactic Plane',
];

const COLUMNS = ['ra', 'dec', 'designation', 'w1mpro', 'w2mpro', 'w3mpro', 'w4mpro',
                 'j_m', 'h_m', 'k_m', 'ph_qual', 'cc_flags', 'ext_key'];

export const Basic = () => (
    <AutoCompleteInput
        fieldKey='catalog'
        label='Catalog'
        tooltip='Start typing to filter catalog names'
        placeholder='e.g. AllWISE'
        options={CATALOGS}
    />
);
Basic.parameters = { storyDescription:
    'Options as bare strings with no initialState - the shape the lightcurve column pickers use. ' +
    'Filtering is joy\'s default case-insensitive "contains".' };

export const ColumnPicker = () => (
    <AutoCompleteInput
        fieldKey='column'
        label='Column'
        tooltip='Type a column name'
        placeholder='e.g. w1mpro'
        options={COLUMNS}
        initialState={{ validator: (v) => v
            ? { valid: true,  message: '' }
            : { valid: false, message: 'Required' }
        }}
    />
);
ColumnPicker.storyName = 'Column Picker';
ColumnPicker.parameters = { storyDescription:
    'Table column names with a required validator. The view runs it on every keystroke and on a selection, ' +
    'so the field goes red mid-word - the same timing as ValidationField.' };


// a server-ish lookup: the results deliberately do NOT all contain the typed substring, so joy's
// default client filter would silently drop rows - filterOptions={(x) => x} is what keeps them.
const searchCatalogs = (val) => {
    if (!val) return [];
    const direct = CATALOGS.filter((s) => s.toLowerCase().includes(val.toLowerCase()));
    const aliased = /^(ir|infrared)/i.test(val) ? ['AllWISE', 'Spitzer IRAC', 'AKARI FIS'] : [];
    const hits = [...new Set([...direct, ...aliased])];
    return new Promise((resolve) => setTimeout(() => resolve(hits), 600));
};

export const AsyncSuggestions = () => {
    const [value, setValue] = useState('');
    const { options, loading } = useAsyncOptions(searchCatalogs, value);
    return (
        <AutoCompleteInputView
            label='Catalog (async)'
            tooltip='Suggestions load after a short delay. Try "ir" - the matches do not contain it.'
            placeholder='e.g. Gaia, or ir'
            value={value}
            options={options}
            loading={loading}
            filterOptions={(x) => x}
            fireValueChange={({ value: v }) => setValue(v ?? '')}
        />
    );
};
AsyncSuggestions.storyName = 'Async Suggestions';
AsyncSuggestions.parameters = { storyDescription:
    'useAsyncOptions resolves a promise and drops responses that are no longer the latest request. ' +
    'Typing "ir" returns rows that do not contain "ir" - they stay visible only because the caller ' +
    'turned joy\'s client-side filter off.' };


const NAIF_OBJECTS = [
    { name: 'Neptune', naifid: 899 },
    { name: 'Neptune Barycenter', naifid: 8 },
    { name: '951 Gaspra', naifid: 20000951 },
    { name: '2 Pallas', naifid: 20000002 },
];

// stand-in for NaifidPanel (which is not exported from the barrel): what is displayed is the object
// name, what is stored is "name;naifid".
export const CompositeValue = () => {
    const [stored, setStored] = useState('');
    const [display, setDisplay] = useState('');
    const options = NAIF_OBJECTS.map((o) => ({ ...o, value: o.name, label: o.name }));
    return (
        <div>
            <AutoCompleteInputView
                label='Moving Target Name'
                tooltip='Pick from the list - typing alone does not resolve an id'
                value={display}
                options={options}
                filterOptions={(x) => x}
                renderOption={(optProps, opt) => {
                    const { key, ...rest } = optProps ?? {};
                    return (
                        <li {...rest} key={key ?? opt.name}>
                            Name: <b>{opt.name}</b>, NAIF ID: <b>{opt.naifid}</b>
                        </li>
                    );
                }}
                onChange={(ev, selected) => {
                    if (!selected || typeof selected === 'string') return;
                    setDisplay(selected.name);
                    setStored(`${selected.name};${selected.naifid}`);
                }}
                fireValueChange={({ value: v }) => { setDisplay(v ?? ''); setStored(''); }}
            />
            <div style={{ marginTop: 8 }}>stored value: <b>{stored || '(none)'}</b></div>
        </div>
    );
};
CompositeValue.storyName = 'Composite Value (display ≠ stored)';
CompositeValue.parameters = { storyDescription:
    'Custom renderOption plus a caller-owned onChange that stores something other than what is shown. ' +
    'The reset-reason onInputChange that fires right after a selection must not clobber it. ' +
    'UCD token completion is the degenerate case of the same mechanism.' };
