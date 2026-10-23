#!/usr/bin/env bash
set -e

echo "========================================================"
echo "  COMPILING VELOCITY X (JAVA SE 17+)"
echo "========================================================"

mkdir -p bin

javac -encoding UTF-8 -cp "lib/*" -d bin \
  src/com/velocityx/model/*.java \
  src/com/velocityx/db/*.java \
  src/com/velocityx/security/*.java \
  src/com/velocityx/util/*.java \
  src/com/velocityx/repository/*.java \
  src/com/velocityx/service/*.java \
  src/com/velocityx/controller/*.java \
  src/com/velocityx/Main.java

echo "[SUCCESS] Build finished. Class files generated in bin/"