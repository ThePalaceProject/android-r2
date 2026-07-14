#!/bin/sh -ex

rm -rf dist

npm run prettier
npm run eslint-ts
npm run build
npm run test
