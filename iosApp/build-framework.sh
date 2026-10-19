#!/bin/sh
set -eu
if [ "${OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED:-NO}" = "YES" ]; then
    exit 0
fi
if [ -z "${API_BASE_URL:-}" ]; then
    echo "error: Configure API_BASE_URL em iosApp/Configuration/Local.xcconfig."
    exit 1
fi
cd "$SRCROOT/.."
/bin/sh ./gradlew :sharedUi:embedAndSignAppleFrameworkForXcode "-PAPI_BASE_URL=$API_BASE_URL"
