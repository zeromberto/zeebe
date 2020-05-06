#!/bin/sh -eux


export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -XX:MaxRAMFraction=$((LIMITS_CPU))"

mvn -o -B -T$LIMITS_CPU -s ${MAVEN_SETTINGS_XML} verify -P skip-unstable-ci,parallel-tests -Dzeebe.it.skip -DtestMavenId=1 -Dsurefire.rerunFailingTestsCount=5 | tee test-java.txt

if grep -q "\[WARNING\] Flakes:" test-java.txt; then
  grep "\[ERROR\]   Run 1: " test-java.txt | awk '{print $4}' >> ./target/FlakyTests.txt

  echo ERROR: Flaky Tests detected>&2
  rm test-java.txt
  exit 1
fi

rm test-java.txt
