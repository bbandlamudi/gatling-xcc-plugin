#!/usr/bin/env bash

simulations=(
    "AdvancedSimulation"
    "BasicSimulation"
    "CachedContentSourceSimulation"
    "ChecksSimulation"
    "FeederWithSessionSimulation"
    "GenerateTestDataSimulation"
    "JavaScriptSimulation"
    "JsonPathSimulation"
    "MapResultSimulation"
    "ModuleInvocationSimulation"
    "MultipleDocumentsSimulation"
    "ProtocolConfigurationSimulation"
    "QuickTestSimulation"
    "SaveItemsByIndexSimulation"
    "XPathExtractAndReuseSimulation"
    "XccsSecureSimulation"
    "XmlChainSimplifiedSimulation"
    "XmlResponseChainSimulation"
)

results=()

echo "========================================"
echo "Running All Gatling Simulations"
echo "========================================"
echo ""

total_count=${#simulations[@]}
current_count=0

for sim in "${simulations[@]}"; do
    current_count=$((current_count + 1))
    full_class_name="com.marklogic.gatling.xcc.example.$sim"

    echo "[$current_count/$total_count] Running: $sim"
    echo "----------------------------------------"

    output=$(mvn gatling:test "-Dgatling.simulationClass=$full_class_name" 2>&1)

    if echo "$output" | grep -q "BUILD SUCCESS"; then
        status="PASS"
    elif echo "$output" | grep -q "BUILD FAILURE"; then
        status="FAIL"
    else
        status="UNKNOWN"
    fi

    request_line=$(echo "$output" | grep -oP '> request count\s+\|\s+\d+\s+\|\s+\d+' | head -1)
    total_requests=0
    ok_requests=0
    if [ -n "$request_line" ]; then
        total_requests=$(echo "$request_line" | grep -oP '\d+' | sed -n '1p')
        ok_requests=$(echo "$request_line" | grep -oP '\d+' | sed -n '2p')
    fi

    results+=("$sim|$status|$total_requests|$ok_requests")

    echo "Status: $status | Requests: $ok_requests/$total_requests"
    echo ""
done

echo ""
echo "========================================"
echo "Summary"
echo "========================================"
echo ""

printf "%-32s %-8s %-15s %-18s\n" "Simulation" "Status" "TotalRequests" "SuccessfulRequests"
pass_count=0
fail_count=0
for result in "${results[@]}"; do
    IFS='|' read -r sim status total ok <<< "$result"
    printf "%-32s %-8s %-15s %-18s\n" "$sim" "$status" "$total" "$ok"
    if [ "$status" == "PASS" ]; then
        pass_count=$((pass_count + 1))
    elif [ "$status" == "FAIL" ]; then
        fail_count=$((fail_count + 1))
    fi
done

echo ""
echo "Total Simulations: $total_count"
echo "Passed: $pass_count"
echo "Failed: $fail_count"
echo ""

if [ "$fail_count" -eq 0 ]; then
    echo "All simulations passed successfully!"
else
    echo "Some simulations failed. Check the output above for details."
fi
