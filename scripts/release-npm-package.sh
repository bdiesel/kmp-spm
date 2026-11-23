#!/usr/bin/env bash
# Builds the SpaceSdk npm package for GitHub Packages.
#
#   scripts/release-npm-package.sh [version]
#
# 1. Builds the Kotlin/JS library (:space-sdk:jsBrowserProductionLibraryDistribution):
#    ES modules + TypeScript definitions.
# 2. Copies it to build/npm-release/<version>/ and sets the package metadata:
#    scoped name @bdiesel/space-sdk, version, GitHub Packages registry, license.
#
# Publish (needs a token with write:packages):
#   cd build/npm-release/<version> && NODE_AUTH_TOKEN=$(gh auth token) npm publish
#
# Nothing is published by this script.
set -euo pipefail

cd "$(dirname "$0")/.."

VERSION="${1:-$(grep '^spacesdk.version=' gradle.properties | cut -d= -f2)}"
NAME="@bdiesel/space-sdk"
OUT="build/npm-release/$VERSION"

echo "==> Building the Kotlin/JS library (SpaceSdk $VERSION)"
./gradlew :space-sdk:jsBrowserProductionLibraryDistribution --console=plain -q

rm -rf "$OUT"
mkdir -p "$OUT"
cp -R space-sdk/build/dist/js/productionLibrary/. "$OUT/"
cp LICENSE "$OUT/" 2>/dev/null || true

cd "$OUT"
npm pkg set \
  name="$NAME" \
  version="$VERSION" \
  description="SpaceSdk: NASA Astronomy Picture of the Day and spaceflight news (Kotlin Multiplatform, TypeScript types included)" \
  license="MIT" \
  repository.type="git" \
  repository.url="git+https://github.com/bdiesel/kmp-spm.git" \
  publishConfig.registry="https://npm.pkg.github.com"
npm pkg delete devDependencies

# Auth for `npm publish`, read from the environment (never written to disk).
echo '//npm.pkg.github.com/:_authToken=${NODE_AUTH_TOKEN}' > .npmrc

cat > README.md <<EOF
# @bdiesel/space-sdk

NASA's Astronomy Picture of the Day and spaceflight news for the web, from the
SpaceSdk Kotlin Multiplatform SDK (https://github.com/bdiesel/kmp-spm).
TypeScript definitions included.

\`\`\`ts
import { SpaceSdk, PictureResult, NewsResult } from "$NAME";

const sdk = new SpaceSdk();
const result = await sdk.pictureOfTheDay.today();        // or forDate(2014, 9, 18)
if (result instanceof PictureResult.Success) console.log(result.picture.title);

const news = await sdk.news.latest(20);
if (news instanceof NewsResult.Success) console.log(news.articles.length);
\`\`\`

Install from GitHub Packages: add \`@bdiesel:registry=https://npm.pkg.github.com\`
to your \`.npmrc\` (with a token that has \`read:packages\`), then
\`npm install $NAME\`.
EOF

echo
echo "Done: $OUT"
echo "  Publish: cd $OUT && NODE_AUTH_TOKEN=\$(gh auth token) npm publish"
