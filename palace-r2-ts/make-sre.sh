#!/bin/sh -ex

SRE_VERSION=5.0.0-rc.4

wget -c "https://registry.npmjs.org/speech-rule-engine/-/speech-rule-engine-${SRE_VERSION}.tgz"
sha256sum -c "speech-rule-engine-${SRE_VERSION}.tgz.sha256"

rm -rf sre-unpack
mkdir -p sre-unpack
tar -C sre-unpack -x -v -f "speech-rule-engine-${SRE_VERSION}.tgz"

rm -rf sre-dist
mkdir -p sre-dist
mkdir -p sre-dist/mathmaps

cp sre-unpack/package/lib/sre.js                sre-dist/sre.js
cp sre-unpack/package/lib/mathmaps/base.json    sre-dist/mathmaps/base.json
cp sre-unpack/package/lib/mathmaps/en.json      sre-dist/mathmaps/en.json
cp sre-unpack/package/lib/mathmaps/euro.json    sre-dist/mathmaps/euro.json

rsync -avz --delete sre-dist/ ../org.librarysimplified.r2.vanilla/src/main/resources/org/librarysimplified/r2/vanilla/readium/sre/
