'use strict';

// Builds a static Swagger UI site for api/openapi.yaml into api/site/,
// using the swagger-ui-dist assets (the same distribution that serves
// petstore.swagger.io) pointed at the local spec instead of the petstore
// demo API.

const fs = require('fs');
const path = require('path');
const swaggerUiDist = require('swagger-ui-dist');

const apiDir = path.join(__dirname, '..');
const siteDir = path.join(apiDir, 'site');
const assetsDir = swaggerUiDist.getAbsoluteFSPath();

const assetFiles = [
  'swagger-ui.css',
  'swagger-ui.css.map',
  'swagger-ui-bundle.js',
  'swagger-ui-bundle.js.map',
  'swagger-ui-standalone-preset.js',
  'swagger-ui-standalone-preset.js.map',
  'index.css',
  'favicon-16x16.png',
  'favicon-32x32.png',
  'oauth2-redirect.html',
];

fs.rmSync(siteDir, { recursive: true, force: true });
fs.mkdirSync(siteDir, { recursive: true });

for (const file of assetFiles) {
  fs.copyFileSync(path.join(assetsDir, file), path.join(siteDir, file));
}

fs.copyFileSync(path.join(apiDir, 'openapi.yaml'), path.join(siteDir, 'openapi.yaml'));

const indexHtml = `<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <title>GymBuddy API</title>
    <link rel="stylesheet" type="text/css" href="./swagger-ui.css" />
    <link rel="stylesheet" type="text/css" href="./index.css" />
    <link rel="icon" type="image/png" href="./favicon-32x32.png" sizes="32x32" />
    <link rel="icon" type="image/png" href="./favicon-16x16.png" sizes="16x16" />
  </head>
  <body>
    <div id="swagger-ui"></div>
    <script src="./swagger-ui-bundle.js" charset="UTF-8"></script>
    <script src="./swagger-ui-standalone-preset.js" charset="UTF-8"></script>
    <script src="./swagger-initializer.js" charset="UTF-8"></script>
  </body>
</html>
`;

const swaggerInitializer = `window.onload = function() {
  window.ui = SwaggerUIBundle({
    url: './openapi.yaml',
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: 'StandaloneLayout'
  });
};
`;

fs.writeFileSync(path.join(siteDir, 'index.html'), indexHtml);
fs.writeFileSync(path.join(siteDir, 'swagger-initializer.js'), swaggerInitializer);

console.log(`Swagger UI site written to ${siteDir}`);
