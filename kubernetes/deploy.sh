#!/bin/bash
SCRIPT_PATH="${BASH_SOURCE[0]}";
if ([ -h "${SCRIPT_PATH}" ]) then
  while([ -h "${SCRIPT_PATH}" ]) do SCRIPT_PATH=`readlink "${SCRIPT_PATH}"`; done
fi
pushd . > /dev/null
cd `dirname ${SCRIPT_PATH}` > /dev/null
SCRIPT_PATH=`pwd`;
popd  > /dev/null

NAMESPACE=$(source $SCRIPT_PATH/config; echo $name)

kubectl create namespace $NAMESPACE

kubectl get secret regcred -n default -o yaml | sed s/"namespace: default"/"namespace: $NAMESPACE"/ | kubectl apply -n $NAMESPACE -f -

kubectl create secret generic postgres --from-literal=POSTGRES_USR=$POSTGRES_USR --from-literal=POSTGRES_PSW="$POSTGRES_PSW" -n $NAMESPACE

kubectl create secret generic storage --from-literal=STORAGE_USR=$STORAGE_USR --from-literal=STORAGE_PSW=$STORAGE_PSW -n $NAMESPACE

$SCRIPT_PATH/templater.sh $SCRIPT_PATH/deployment.yaml.template -s -f $SCRIPT_PATH/config > $SCRIPT_PATH/deployment.yaml

kubectl create -f $SCRIPT_PATH/deployment.yaml && rm -rf $SCRIPT_PATH/deployment.yaml
