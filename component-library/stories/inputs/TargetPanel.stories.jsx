import React from 'react';
import { TargetPanel, FieldGroup } from '../../index.js';

export default {
    title: 'Inputs/TargetPanel',
    component: TargetPanel,
    tags: ['autodocs'],
    decorators: [
        (Story, ctx) => (
            <FieldGroup groupKey={ctx.id}>
                <Story />
            </FieldGroup>
        ),
    ],
};

export const Basic = () => (
    <TargetPanel fieldKey='target' />
);
Basic.parameters = { storyDescription: 'Accepts coordinates (J2000 RA/Dec) or an object name resolved via NED or Simbad.' };

export const NoResolver = () => (
    <TargetPanel fieldKey='target' showResolveSourceOp={false} />
);
NoResolver.storyName = 'No Resolver';
NoResolver.parameters = { storyDescription: 'Resolver dropdown hidden. Accepts coordinates only.' };

export const NullAllowed = () => (
    <TargetPanel fieldKey='target' nullAllowed={true} />
);
NullAllowed.storyName = 'Null Allowed';
NullAllowed.parameters = { storyDescription: 'Empty value is valid. Useful when a target is optional.' };

export const NoExamples = () => (
    <TargetPanel fieldKey='target' showExample={false} />
);
NoExamples.storyName = 'No Examples';
NoExamples.parameters = { storyDescription: 'Example hints hidden for a more compact layout.' };
