#!/bin/sh -ex

MATHJAX_VERSION=4.1.3

wget -c "https://registry.npmjs.org/@mathjax/mathjax-newcm-font/-/mathjax-newcm-font-${MATHJAX_VERSION}.tgz"
sha256sum -c "mathjax-newcm-font-${MATHJAX_VERSION}.tgz.sha256"

rm -rf mathjax-unpack
mkdir -p mathjax-unpack
tar -C mathjax-unpack -x -v -f "mathjax-newcm-font-${MATHJAX_VERSION}.tgz"

rm -rf mathjax-dist
mkdir -p mathjax-dist
mkdir -p mathjax-dist/fonts/mathjax-newcm-font/chtml/woff2
mkdir -p mathjax-dist/fonts/mathjax-newcm-font/chtml/dynamic

cp ./MathJax-src/components/mjs/mml-chtml/mml-chtml.js mathjax-dist/
cp mathjax-unpack/package/chtml/woff2/*.woff2          mathjax-dist/fonts/mathjax-newcm-font/chtml/woff2/
cp mathjax-unpack/package/chtml/dynamic/*.js           mathjax-dist/fonts/mathjax-newcm-font/chtml/dynamic/

rsync -avz --delete mathjax-dist/ ../org.librarysimplified.r2.vanilla/src/main/resources/org/librarysimplified/r2/vanilla/readium/mathjax/
