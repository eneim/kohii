#!/bin/bash

# Clean any previous Dokka docs.
rm -rf docs/api

# Build the Dokka docs.
./gradlew clean \
  :kohii-core:dokka \
  :kohii-exoplayer:dokka \
  :kohii-androidx:dokka \
  :kohii-ads:dokka

sed -e '/full documentation here/ { N; d; }' <README.md >docs/index.md
cp -R art docs/art
# cp CONTRIBUTING.md docs/contributing.md
cp CHANGELOG.md docs/changelog.md
cp CODE_OF_CONDUCT.md docs/code_of_conduct.md
# cp coil-gif/README.md docs/gifs.md
# cp coil-svg/README.md docs/svgs.md
# cp logo.svg docs/logo.svg

# Deploy to Github pages.
mkdocs serve
