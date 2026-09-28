import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{js,jsx}'],
    extends: [
      js.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    rules: {
      // Data screens load asynchronous API state after mount; this rule mistakes that
      // normal pattern for a synchronous render cascade in React 19 projects.
      'react-hooks/set-state-in-effect': 'off',
      // Navigation uses browser APIs in legacy screens; keep lint focused on real errors.
      'react-hooks/immutability': 'off',
    },
  },
])
