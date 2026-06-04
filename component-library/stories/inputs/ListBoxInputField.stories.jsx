import React from 'react';
import { ListBoxInputField, FieldGroup } from '../../index.js';

export default {
    title: 'Inputs/ListBoxInputField',
    component: ListBoxInputField,
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
    <ListBoxInputField
        fieldKey='band'
        label='Band'
        tooltip='Select a WISE band'
        options={[
            { label: 'W1 (3.4 µm)', value: 'w1' },
            { label: 'W2 (4.6 µm)', value: 'w2' },
            { label: 'W3 (12 µm)',  value: 'w3' },
            { label: 'W4 (22 µm)',  value: 'w4' },
        ]}
        initialState={{ value: 'w1' }}
    />
);
Basic.parameters = { storyDescription: 'Single selection with a pre-selected default.' };

export const Multiple = () => (
    <ListBoxInputField
        fieldKey='bands'
        label='Bands'
        tooltip='Select one or more bands'
        options={[
            { label: 'W1 (3.4 µm)', value: 'w1' },
            { label: 'W2 (4.6 µm)', value: 'w2' },
            { label: 'W3 (12 µm)',  value: 'w3' },
            { label: 'W4 (22 µm)',  value: 'w4' },
        ]}
        multiple={true}
        initialState={{ value: 'w1,w2' }}
    />
);
Multiple.parameters = { storyDescription: 'Multiple selection. Value is stored as a comma-separated string.' };

export const Vertical = () => (
    <ListBoxInputField
        fieldKey='band'
        label='Band'
        options={[
            { label: 'W1 (3.4 µm)', value: 'w1' },
            { label: 'W2 (4.6 µm)', value: 'w2' },
            { label: 'W3 (12 µm)',  value: 'w3' },
            { label: 'W4 (22 µm)',  value: 'w4' },
        ]}
        orientation='vertical'
        initialState={{ value: 'w1' }}
    />
);
Vertical.parameters = { storyDescription: 'Label stacked above the input.' };
