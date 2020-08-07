#!/bin/bash

function runCluster () {
  $JVM water.H2O -name $CLUSTER_NAME -baseport $CLUSTER_BASEPORT -ga_opt_out 1> $OUTDIR/out.1 2>&1 & PID_1=$!
  $JVM water.H2O -name $CLUSTER_NAME -baseport $CLUSTER_BASEPORT -ga_opt_out 1> $OUTDIR/out.2 2>&1 & PID_2=$!
  $JVM water.H2O -name $CLUSTER_NAME -baseport $CLUSTER_BASEPORT -ga_opt_out 1> $OUTDIR/out.3 2>&1 & PID_3=$!
  $JVM water.H2O -name $CLUSTER_NAME -baseport $CLUSTER_BASEPORT -ga_opt_out 1> $OUTDIR/out.4 2>&1 & PID_4=$!
}