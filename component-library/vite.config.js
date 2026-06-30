import { fileURLToPath } from 'node:url';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// Root of the firefly/ monorepo — used for path aliases and fs.allow.
// Exported so .storybook/main.js can import it directly.
export const root = fileURLToPath(new URL('..', import.meta.url));

function getGradleProps() {
    return Object.fromEntries(
        Object.keys(process.env)
            .filter((k) => k.startsWith('FF___'))
            .map((k) => [k.slice(5).replace(/___/g, '.'), JSON.stringify(process.env[k])])
    );
}

// ── Shared plugins (used by both the library build and Storybook) ──────────

// Firefly source imports web workers as  import Worker from '*.worker.js'.
// Appending ?worker&inline tells Vite to bundle the worker code as a base64
// blob, which works in both library builds and the Storybook dev server.
export const workerPlugin = {
    name: 'vite-worker-import',
    enforce: 'pre',
    transform(code) {
        if (!code.includes('.worker.js')) return null;
        return code.replace(
            /from\s+(['"])([^'"]*\.worker\.js)\1/g,
            "from '$2?worker&inline'"
        );
    },
};

// Some Firefly source files import MUI icons with a .js extension
// (e.g. '@mui/icons-material/MenuRounded.js').  That suffix is absent from
// the package's exports map, so Rollup cannot resolve it.  Strip it before
// the external check runs.
export const muiIconsNormalizePlugin = {
    name: 'vite-mui-icons-normalize',
    enforce: 'pre',
    transform(code) {
        if (!code.includes('@mui/icons-material/')) return null;
        return code.replace(
            /from\s+(['"])(@mui\/icons-material\/[^'"]+)\.js\1/g,
            (_, q, path) => `from ${q}${path}${q}`
        );
    },
};

// Firefly's src/ uses .js extension for files that contain JSX.
// Running esbuild enforce:'pre' converts them before Vite's import
// analyser sees the file, preventing "Unexpected token" parse errors.
export const jsJsxPlugin = {
    name: 'js-jsx-transform',
    enforce: 'pre',
    async transform(code, id) {
        if (!id.match(/\/src\/firefly\/.*\.js$/) || !code.includes('<')) return null;
        const { transform } = await import('esbuild');
        const result = await transform(code, { loader: 'jsx', jsx: 'automatic' });
        return { code: result.code, map: result.map || null };
    },
};

// Shared Vite config merged into both the library build and Storybook.
export const sharedConfig = {
    resolve: {
        alias: [
            // Redirect MUI icon imports to the /esm/ subpath at resolve time so
            // rolldown never CJS-wraps them (avoids the isNodeMode=1 TDZ bug).
            // This covers both bundled builds (Storybook) and external builds
            // (library), making muiIconsEsmPlugin a redundant safety net.
            {
                find:        /^@mui\/icons-material\/(?!esm\/)(.+)$/,
                replacement: '@mui/icons-material/esm/$1',
            },
            { find: 'firefly', replacement: `${root}/src/firefly/js` },
            { find: 'html',    replacement: `${root}/src/firefly/html` },
            { find: 'styles',  replacement: `${root}/src/firefly/html/css` },
            { find: 'images',  replacement: `${root}/src/firefly/html/images` },
            { find: '~images', replacement: `${root}/src/firefly/html/images` },
        ],
    },
    server: {
        // Allow serving files from the parent firefly/ directory,
        // which the aliases above point into.
        fs: { allow: [root] },
        hmr: { host: 'localhost' },
    },
    define: {
        // Bundled CJS packages (redux, redux-saga) reference the Node.js
        // `global` object; map it to the standard browser equivalent.
        global: 'globalThis',
        // Build-time constants read by Firefly's WebUtil.js at runtime.
        __PROPS__: {
            BUILD_ENV:           JSON.stringify(process.env.BUILD_ENV ?? 'dev'),
            SCRIPT_NAME:         JSON.stringify([]),
            MODULE_NAME:         JSON.stringify('firefly-components'),
            MIN_SAFARI_VERSION:  '17',
            MIN_CHROME_VERSION:  '130',
            MIN_FIREFOX_VERSION: '134',
            MIN_EDGE_VERSION:    '130',
            ...getGradleProps(),
        },
    },
    // Storybook uses Vite's dep optimiser; tell esbuild to parse .js files
    // as JSX so source files compile correctly during development.
    optimizeDeps: {
        esbuildOptions: {
            loader: { '.js': 'jsx' },
        },
    },
};

// ── Shared output plugin ──────────────────────────────────────────────────

// @mui/icons-material defaults to its CJS build.  Vite 6+ (rolldown) wraps
// CJS modules with isNodeMode=1, which makes import_Icon.default resolve to
// the whole module object instead of the React.memo component — causing the
// "React.jsx: type is invalid, got: object" / TDZ errors.
// Fix: rewrite icon specifiers in the output chunks to the /esm/ subpath so
// the output uses pure-ESM files with no CJS→ESM conversion needed.
// renderChunk only fires during builds (lib build and build-storybook), never
// during the dev server, so this is safe to include in all configs.
export const muiIconsEsmPlugin = {
    name: 'vite-mui-icons-esm',
    renderChunk(code) {
        if (!code.includes('@mui/icons-material/')) return null;
        return code.replace(
            /(['"])@mui\/icons-material\/(?!esm\/)([^'"]+)\1/g,
            (_, q, icon) => `${q}@mui/icons-material/esm/${icon}${q}`,
        );
    },
};

// ──────────────────────────────────────────────────────────────────────────

export default defineConfig(({ command }) => ({
    plugins: [
        workerPlugin,
        muiIconsNormalizePlugin,
        jsJsxPlugin,
        muiIconsEsmPlugin,     // output-chunk rewriter; no-op during dev
        react({
            include: /\.(jsx|js)$/,          // process .js files alongside .jsx
            babel: {
                plugins: ['@emotion/babel-plugin'],
            },
        }),
    ],
    ...sharedConfig,

    // Library build — skipped when Storybook runs the Vite dev server.
    ...(command === 'build' && {
        build: {
            lib: {
                entry: {
                    index:  fileURLToPath(new URL('index.js', import.meta.url)),
                    tables: fileURLToPath(new URL('src/tables/index.js', import.meta.url)),
                    charts: fileURLToPath(new URL('src/charts/index.js', import.meta.url)),
                    images: fileURLToPath(new URL('src/images/index.js', import.meta.url)),
                },
                formats:  ['es', 'cjs'],
                fileName: (format, entry) => `${entry}.${format === 'es' ? 'js' : 'cjs'}`,
            },
            rollupOptions: {
                external: [
                    // ── peerDependencies ─────────────────────────────────────
                    // Must be singletons shared with the host app.  Listed in
                    // peerDependencies in package.json; the consumer installs them.
                    /^react(\/|$)/,
                    /^react-dom(\/|$)/,
                    /^@mui\//,
                    /^@emotion\//,
                    /^moment$/,

                    // ── dependencies ─────────────────────────────────────────
                    // Auto-installed when the consumer runs yarn/npm install.
                    // Kept external so they are not bundled into the dist chunks
                    // (avoids the CJS-in-bundle double-processing problem).
                    'chroma-js',
                    'dompurify',
                    'fixed-data-table-2',
                    'immutability-helper',
                    'lodash',
                    /^lodash\//,
                    'md5',
                    'prop-types',
                    'shallowequal',

                    // redux / redux-saga / redux-thunk stay BUNDLED.
                    // They are Firefly's internal state machinery and their
                    // older releases lack clean ESM default exports.
                ],
                output: {
                    assetFileNames: 'firefly-components.[ext]',
                },
            },
        },
    }),
}));
