import React from 'react';
import { ValidationField, FieldGroup, emailValidator, floatValidator } from '../../index.js';

const EXAMPLE_CODE = `\
import { ValidationField, FieldGroup, emailValidator } from '@ipac/firefly-component-library';

() => (
    <FieldGroup groupKey='my-form'>
        <ValidationField
            fieldKey='email'
            label='Email'
            tooltip='Enter your email address'
            initialState={{ value: '', validator: emailValidator('Email') }}
        />
    </FieldGroup>
)`;

export default {
    title: 'Inputs/ValidationField',
    component: ValidationField,
    tags: ['autodocs'],
    parameters: {
        controls: { disable: true }, actions: { disable: true },
        docs: { source: { code: EXAMPLE_CODE } },
    },
    decorators: [
        (Story, ctx) => (
            <FieldGroup groupKey={ctx.id}>
                <Story />
            </FieldGroup>
        ),
    ],
};

// ─── Basic ────────────────────────────────────────────────────────────────────

export const Basic = () => (
    <ValidationField
        fieldKey='email'
        label='Email'
        tooltip='Enter your email address'
        initialState={{ value: '', validator: emailValidator('Email') }}
    />
);
Basic.parameters = { storyDescription: 'Email field using the built-in emailValidator. Empty values are valid until something is typed.' };

// ─── Start Invalid ────────────────────────────────────────────────────────────

export const StartInvalid = () => (
    <ValidationField
        fieldKey='email'
        label='Email'
        initialState={{ value: '', valid: false, message: 'Required', validator: emailValidator('Email') }}
    />
);
StartInvalid.storyName = 'Start Invalid';
StartInvalid.parameters = { storyDescription: 'Field starts in an invalid state. Useful for required fields before user interaction.' };

// ─── Number Range ─────────────────────────────────────────────────────────────

export const NumberRange = () => (
    <ValidationField
        fieldKey='dec'
        label='Declination (deg)'
        tooltip='Enter a declination between 0 and 90'
        initialState={{ value: '45', validator: floatValidator(0, 90, 'Declination') }}
    />
);
NumberRange.storyName = 'Number Range';
NumberRange.parameters = { storyDescription: 'Float range validator that accepts numbers between 0 and 90.' };

// ─── Multiple Fields ──────────────────────────────────────────────────────────

export const MultipleFields = () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <ValidationField
            fieldKey='ra'
            label='RA (deg)'
            tooltip='Right ascension in degrees'
            initialState={{ value: '', validator: floatValidator(0, 360, 'RA') }}
        />
        <ValidationField
            fieldKey='dec'
            label='Dec (deg)'
            tooltip='Declination in degrees'
            initialState={{ value: '', validator: floatValidator(-90, 90, 'Dec') }}
        />
    </div>
);
MultipleFields.storyName = 'Multiple Fields';
MultipleFields.parameters = { storyDescription: 'Multiple fields sharing a single FieldGroup. State is tracked together in the store.' };
