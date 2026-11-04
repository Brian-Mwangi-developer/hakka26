#!/usr/bin/env bash
set -euo pipefail

app_dir="${1:?usage: detect_stack.sh <app-dir>}"

if [ ! -d "$app_dir" ]; then
  echo unknown
  exit 0
fi

find_in_app() {
  find "$app_dir" -maxdepth "$1" \( -name node_modules -o -name build -o -name .gradle -o -name Pods \) -prune -o "${@:2}" -print 2>/dev/null
}

gradle_files="$(find_in_app 3 \( -name 'build.gradle' -o -name 'build.gradle.kts' -o -name 'libs.versions.toml' \))"

if [ -f "$app_dir/pubspec.yaml" ]; then
  echo flutter
elif [ -f "$app_dir/package.json" ] && grep -q '"react-native"' "$app_dir/package.json"; then
  echo react-native
elif [ -z "$gradle_files" ] && [ -n "$(find_in_app 2 \( -name '*.xcodeproj' -o -name 'Package.swift' \))" ]; then
  echo ios
elif [ -n "$gradle_files" ] && echo "$gradle_files" | xargs grep -lE 'kotlin\("multiplatform"\)|kotlin\.multiplatform|org\.jetbrains\.kotlin\.multiplatform' >/dev/null 2>&1; then
  echo kmp
elif [ -n "$gradle_files" ]; then
  echo android
else
  echo unknown
fi
