#!/bin/sh -eux


export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -XX:MaxRAMFraction=$((LIMITS_CPU))"

mvn -o -B -T$LIMITS_CPU -s ${MAVEN_SETTINGS_XML} verify -P skip-unstable-ci,parallel-tests -pl qa/integration-tests -pl upgrade-tests -DtestMavenId=2 -Dsurefire.rerunFailingTestsCount=5 | tee test-it.txt
status=${PIPESTATUS[@]}

if grep -q "\[WARNING\] Flakes:" test-it.txt; then
  grep "\[ERROR\]   Run 1: " test-it.txt | awk '{print $4}' >> ./target/FlakyTests.txt

  echo ERROR: Flaky Tests detected>&2
  rm test-it.txt
  exit 1
fi

rm test-it.txt
exit $status
