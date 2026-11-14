#!/usr/bin/env bash
set -e

echo "Compiling GoCrawl main sources..."
mkdir -p target/classes
rm -f target/classes/module-info.class
javac -encoding UTF-8 -cp "lib/jsoup-1.18.3.jar:lib/flatlaf-3.8.jar" -d target/classes $(find src/main/java -name "*.java")

echo "Compiling test sources..."
mkdir -p target/test-classes
javac -encoding UTF-8 -cp "lib/jsoup-1.18.3.jar:lib/flatlaf-3.8.jar:target/classes" -d target/test-classes $(find src/test/java -name "*.java")

echo "Executing AllTests..."
java -cp "lib/jsoup-1.18.3.jar:lib/flatlaf-3.8.jar:target/classes:target/test-classes" id.gocrawl.AllTests
