import { defineConfig } from '@apps-in-toss/web-framework/config';
export default defineConfig({
  appName: 'cmp-service-sample',
  permissions: [],
  navigationBar: { withBackButton: true, withHomeButton: true, theme: 'light' },
  webBundleDir: 'dist/apps-in-toss'
});
