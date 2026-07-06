import { CssVarsProvider, extendTheme } from '@mui/joy';
import React from 'react';
import { ArgTypes, Description, Source, Title } from '@storybook/blocks';
import { defaultTheme } from 'firefly/ui/ThemeSetup.js';
import { initFirefly } from '../src/init.js';

const theme = extendTheme(defaultTheme());

export const beforeAll = async () => {
    // In local dev Storybook runs on a different port than Firefly, so fall
    // back to the conventional local address.  In production the Storybook
    // site is served from the same WAR as Firefly, so the origin is correct.
    const serverUrl = window.location.hostname === 'localhost'
        ? 'http://localhost:8080/firefly/'
        : `${window.location.origin}/firefly/`;
    await initFirefly({ serverUrl });
};

export const decorators = [
    (Story, ctx) => {
        const desc = ctx.parameters.storyDescription;
        return (
            <CssVarsProvider defaultMode='system' theme={theme}>
                {desc && (
                    <p style={{ margin: '0 0 12px 0', color: 'darkslateblue', fontSize: 16, fontStyle: 'italic' }}>
                        {desc}
                    </p>
                )}
                <Story />
            </CssVarsProvider>
        );
    },
];

const DocsPage = () => (
    <>
        <Title />
        <Description />
        <ArgTypes />
        <h3 style={{ marginTop: 24 }}>Example</h3>
        <Source />
    </>
);

export const parameters = {
    controls: {
        matchers: {
            color: /(background|color)$/i,
            date: /Date$/i,
        },
    },
    docs: {
        page: DocsPage,
    },
    options: {
        storySort: {
            order: ['Introduction', 'Charts', 'Images', 'Inputs', 'Tables'],
        },
    },
};
