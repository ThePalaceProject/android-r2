#!/bin/sh -ex

rm -rf dist

npm run prettier
npm run eslint-ts
npm run build
npm run test

cp ../org.librarysimplified.r2.vanilla/src/main/resources/org/librarysimplified/r2/vanilla/readium/scripts/crypto-sha256.js dist/
cp ../org.librarysimplified.r2.vanilla/src/main/resources/org/librarysimplified/r2/vanilla/readium/scripts/highlight.js dist/
cp ../org.librarysimplified.r2.vanilla/src/main/resources/org/librarysimplified/r2/vanilla/readium/readium-css/ReadiumCSS-before.css dist/
cp ../org.librarysimplified.r2.vanilla/src/main/resources/org/librarysimplified/r2/vanilla/readium/readium-css/ReadiumCSS-after.css dist/
cp demo/index.xhtml dist/
