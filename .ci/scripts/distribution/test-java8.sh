#!/bin/sh -eux


export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -XX:MaxRAMFraction=$((LIMITS_CPU))"

mvn -v

mvn -o -B -T$LIMITS_CPU -s ${MAVEN_SETTINGS_XML} verify -pl clients/java -DtestMavenId=3 -Dsurefire.rerunFailingTestsCount=5 | tee test.txt

if grep -q "\[WARNING\] Flakes:" test.txt; then
  grep "\[ERROR\]   Run 1: " test.txt | awk '{print $4}' >> ./target/FlakyTests.txt

  echo ERROR: Flaky Tests detected>&2
  rm test.txt
  exit 1
fi

rm test.txt
