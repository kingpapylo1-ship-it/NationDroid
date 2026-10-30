#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
check_dir=$(mktemp -d)
trap 'rm -rf "$check_dir"' EXIT
javac -d "$check_dir" app/src/main/java/com/nationdroid/launcher/AppRules.java app/src/main/java/com/nationdroid/launcher/WindowRules.java tests/AppRulesTest.java tests/WindowRulesTest.java app/src/main/java/com/nationdroid/launcher/SphereMath.java app/src/main/java/com/nationdroid/launcher/SettingsRules.java tests/SphereMathTest.java tests/SettingsRulesTest.java
java -cp "$check_dir" AppRulesTest
java -cp "$check_dir" WindowRulesTest

java -cp "$check_dir" SphereMathTest
java -cp "$check_dir" SettingsRulesTest
