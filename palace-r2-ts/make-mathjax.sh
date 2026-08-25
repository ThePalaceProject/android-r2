#!/bin/sh -ex

MATHJAX_VERSION=4.1.3

wget -c "https://registry.npmjs.org/@mathjax/mathjax-newcm-font/-/mathjax-newcm-font-${MATHJAX_VERSION}.tgz"
sha256sum -c "mathjax-newcm-font-${MATHJAX_VERSION}.tgz.sha256"
wget -c "https://registry.npmjs.org/mathjax/-/mathjax-${MATHJAX_VERSION}.tgz"
sha256sum -c "mathjax-${MATHJAX_VERSION}.tgz.sha256"

rm -rf mathjax-unpack
mkdir -p mathjax-unpack
tar -C mathjax-unpack -x -v -f "mathjax-newcm-font-${MATHJAX_VERSION}.tgz"
tar -C mathjax-unpack -x -v -f "mathjax-${MATHJAX_VERSION}.tgz"

rm -rf mathjax-dist
mkdir -p mathjax-dist
mkdir -p mathjax-dist/fonts/mathjax-newcm-font/chtml/woff2
mkdir -p mathjax-dist/fonts/mathjax-newcm-font/chtml/dynamic

cp mathjax-unpack/package/chtml/woff2/*.woff2 mathjax-dist/fonts/mathjax-newcm-font/chtml/woff2/
cp mathjax-unpack/package/chtml/dynamic/*.js  mathjax-dist/fonts/mathjax-newcm-font/chtml/dynamic/
cp mathjax-unpack/package/mml-chtml.js        mathjax-dist/mml-chtml.js

rsync -avz --delete mathjax-dist/ ../org.librarysimplified.r2.vanilla/src/main/resources/org/librarysimplified/r2/vanilla/readium/mathjax/
