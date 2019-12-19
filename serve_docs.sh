#!/bin/bash

# Clean any previous Dokka docs.
rm -rf docs/api

# Build the Dokka docs.
./gradlew clean \
  :kohii-core:dokka \
  :kohii-exoplayer:dokka \
  :kohii-androidx:dokka \
  :kohii-ads:dokka

cp -R art docs/
cp CHANGELOG.md docs/changelog.md
cp CODE_OF_CONDUCT.md docs/code_of_conduct.md

# Serve docs for local preview
mkdocs serve
