#!/bin/bash
# Verify source files for carlauncher compilation without full build (dry-run)
# Definitive check for Stub+Shadow

echo "🔍 Verifying Gradle source inputs..."
echo ""

# Run dry-run compilation with info log level
# grep for MapActivity.java to see which one is being picked up
OUTPUT=$(./gradlew :OsmAnd:compileCarlauncherOpengldebugFatDebugJavaWithJavac --dry-run --info 2>&1)

echo "--- ANALYZING OUTPUT ---"

# Check for CarLauncher version
if echo "$OUTPUT" | grep -q "src-carlauncher/java/net/osmand/plus/activities/MapActivity.java"; then
    echo "✅ SUCCESS: src-carlauncher/MapActivity.java IS INCLUDED."
else
    echo "❌ FAILURE: src-carlauncher/MapActivity.java NOT FOUND in source list."
fi

# Check for Original version
if echo "$OUTPUT" | grep -q "OsmAnd/src/net/osmand/plus/activities/MapActivity.java"; then
    echo "❌ FAILURE: src/MapActivity.java IS ALSO INCLUDED (Duplicate/No Exclude)!"
else
    echo "✅ SUCCESS: src/MapActivity.java IS EXCLUDED."
fi

echo ""
echo "Raw grep output for MapActivity:"
echo "$OUTPUT" | grep "MapActivity.java" | grep -v "> Task"
