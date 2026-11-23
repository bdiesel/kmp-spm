#!/usr/bin/env bash
# Builds a distributable SpaceSdk Swift package for the separate Swift repo.
#
#   scripts/release-swift-package.sh [version]
#
# 1. Exports the release Swift package with Kotlin's Swift export
#    (:space-sdk:exportReleaseSwiftPackage -> space-sdk/SwiftPackage/Release).
# 2. Zips the compiled Kotlin (SpaceSdkKotlin.xcframework) for a GitHub release.
# 3. Rewrites Package.swift so the binary target is downloaded by URL + checksum
#    instead of read from a local path.
#
# Output: build/swift-release/<version>/
#   package/                         -> commit to the Swift repo and tag <version>
#   SpaceSdkKotlin.xcframework.zip   -> attach to the GitHub release <version>
#
# Nothing is pushed or uploaded; see README "Publishing SpaceSdk".
set -euo pipefail

cd "$(dirname "$0")/.."

VERSION="${1:-$(grep '^spacesdk.version=' gradle.properties | cut -d= -f2)}"
SWIFT_REPO="${SPACESDK_SWIFT_REPO:-bdiesel/SpaceSdk-swift}"
BINARY="SpaceSdkKotlin"
OUT="build/swift-release/$VERSION"
URL="https://github.com/$SWIFT_REPO/releases/download/$VERSION/$BINARY.xcframework.zip"

echo "==> Exporting release Swift package (SpaceSdk $VERSION)"
./gradlew :space-sdk:exportReleaseSwiftPackage --console=plain -q

SRC="space-sdk/SwiftPackage/Release"
[ -f "$SRC/Package.swift" ] || { echo "error: $SRC/Package.swift not found"; exit 1; }

rm -rf "$OUT"
mkdir -p "$OUT/package"

echo "==> Zipping $BINARY.xcframework"
ditto -c -k --sequesterRsrc --keepParent "$SRC/$BINARY.xcframework" "$OUT/$BINARY.xcframework.zip"
CHECKSUM="$(swift package compute-checksum "$OUT/$BINARY.xcframework.zip")"

echo "==> Writing package sources"
cp -R "$SRC/Sources" "$OUT/package/Sources"
cp LICENSE "$OUT/package/LICENSE"
# Replace the local binary target with a remote one (URL + checksum).
awk -v name="$BINARY" -v url="$URL" -v sum="$CHECKSUM" '
  $0 ~ "name: \"" name "\"," && prev ~ /\.binaryTarget\(/ { print; getline; print "            url: \"" url "\","; print "            checksum: \"" sum "\""; prev=""; next }
  { print; prev=$0 }
' "$SRC/Package.swift" > "$OUT/package/Package.swift"

grep -q "checksum: \"$CHECKSUM\"" "$OUT/package/Package.swift" \
  || { echo "error: could not rewrite the binary target in Package.swift"; exit 1; }

cat > "$OUT/package/README.md" <<EOF
# SpaceSdk for Swift

NASA Astronomy Picture of the Day and Spaceflight News, from a Kotlin
Multiplatform SDK exported to Swift. Generated from
https://github.com/bdiesel/kmp-spm — do not edit by hand.

## Install

Xcode: **File ▸ Add Package Dependencies…** → \`https://github.com/$SWIFT_REPO\`
→ version \`$VERSION\` → product **SpaceSdkLibrary**. Requires iOS 18.

\`\`\`swift
import SpaceSdk

let sdk = SpaceSdk()
let result = try await sdk.getPictureOfTheDay()
\`\`\`
EOF

echo
echo "Done: $OUT"
echo "  package/                        commit to https://github.com/$SWIFT_REPO and tag $VERSION"
echo "  $BINARY.xcframework.zip  attach to release $VERSION (checksum $CHECKSUM)"
