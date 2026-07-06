import React from 'react';
import { SuggestBoxInputField, FieldGroup } from '../../index.js';

export default {
    title: 'Inputs/SuggestBoxInputField',
    component: SuggestBoxInputField,
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

export const Basic = () => (
    <SuggestBoxInputField
        fieldKey='catalog'
        label='Catalog'
        tooltip='Start typing to filter catalog names'
        placeholder='e.g. AllWISE'
        getSuggestions={(val) => {
            const catalogs = [
                'AllWISE', 'CatWISE2020', '2MASS Point Source', '2MASS Extended Source',
                'Gaia DR3', 'Gaia DR2', 'SDSS DR17', 'SDSS DR16',
                'GALEX AIS', 'GALEX MIS', 'Spitzer IRAC', 'Herschel PACS',
                'AKARI FIS', 'IRAS PSC', 'MSX Galactic Plane',
            ];
            return val ? catalogs.filter((s) => s.toLowerCase().startsWith(val.toLowerCase())) : catalogs;
        }}
        initialState={{ value: '', validator: () => ({ valid: true, message: '' }) }}
    />
);
Basic.parameters = { storyDescription: 'Suggests catalog names that start with the typed prefix.' };

export const AsyncSuggestions = () => (
    <SuggestBoxInputField
        fieldKey='catalog'
        label='Catalog (async)'
        tooltip='Suggestions load after a short delay'
        placeholder='e.g. Gaia'
        getSuggestions={(val) => {
            const catalogs = [
                'AllWISE', 'CatWISE2020', '2MASS Point Source', '2MASS Extended Source',
                'Gaia DR3', 'Gaia DR2', 'SDSS DR17', 'SDSS DR16',
                'GALEX AIS', 'GALEX MIS', 'Spitzer IRAC', 'Herschel PACS',
                'AKARI FIS', 'IRAS PSC', 'MSX Galactic Plane',
            ];
            const matches = val ? catalogs.filter((s) => s.toLowerCase().startsWith(val.toLowerCase())) : catalogs;
            return new Promise((resolve) => setTimeout(() => resolve(matches), 300));
        }}
        initialState={{ value: '', validator: () => ({ valid: true, message: '' }) }}
    />
);
AsyncSuggestions.storyName = 'Async Suggestions';
AsyncSuggestions.parameters = { storyDescription: 'getSuggestions returns a Promise, simulating a server-side lookup.' };

export const ColumnPicker = () => (
    <SuggestBoxInputField
        fieldKey='column'
        label='Column'
        tooltip='Type a column name'
        placeholder='e.g. w1mpro'
        getSuggestions={(val) => {
            const columns = ['ra', 'dec', 'designation', 'w1mpro', 'w2mpro', 'w3mpro', 'w4mpro',
                             'j_m', 'h_m', 'k_m', 'ph_qual', 'cc_flags', 'ext_key'];
            return val ? columns.filter((s) => s.toLowerCase().startsWith(val.toLowerCase())) : columns;
        }}
        initialState={{ value: '', validator: (v) => v
            ? { valid: true,  message: '',         value: v }
            : { valid: false, message: 'Required', value: v }
        }}
    />
);
ColumnPicker.storyName = 'Column Picker';
ColumnPicker.parameters = { storyDescription: 'Autocomplete for table column names with a required validator.' };
