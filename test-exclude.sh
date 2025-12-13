#!/bin/bash
# Test script: Gradle source verification using javap
# Checks if the compiled MapActivity class contains CarLauncher-specific methods.

echo "🧪 Verifying MapActivity compilation..."
echo ""

# 1. Clean and Compile
echo "🔨 Compiling carlauncher variant..."
./gradlew :OsmAnd:compileCarlauncherOpengldebugFatDebugJavaWithJavac -x test

# 2. Find the compiled class file
# Location typically: OsmAnd/build/intermediates/javac/carlauncherOpengldebugFatDebug/classes/net/osmand/plus/activities/MapActivity.class
CLASS_FILE=$(find OsmAnd/build/intermediates/javac/carlauncherOpengldebugFatDebug -name "MapActivity.class" | head -1)

if [ -z "$CLASS_FILE" ]; then
    echo "❌ Error: Compiled MapActivity.class not found! Build might have failed."
    exit 1
fi

echo "found class file: $CLASS_FILE"
echo ""

# 3. Inspect with javap
echo "🔍 Inspecting class methods..."
if javap -p "$CLASS_FILE" | grep -q "setupCarLauncherUI"; then
    echo "✅ SUCCESS: 'setupCarLauncherUI' method FOUND."
    echo "   The compiled MapActivity IS the CarLauncher version."
else
    echo "❌ FAILURE: 'setupCarLauncherUI' method NOT found."
    echo "   The compiled MapActivity is the ORIGINAL version (override failed)."
fi
