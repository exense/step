#!/bin/bash

## Package dependencies:
## - wget
## - unzip
##
##  Description: install dependencies defined as keys/values in the STEP_DP environment variable to a container hosted on a Unix system
##  STEP_DP properties example :  STEP_DP="chromedriver=2.42,chrome=69.0.3497.92"
##  
## Usage :   STEP_DP="dependency1=version1,dependencyN=versionN" ./installProperties.sh
##

nexus_base_url="https://nexus-enterprise.exense.ch/repository/container-dependency/ch/exense/docker"
dependencies_path=".."

install_dependency() {
  local kv_pair=$1;
  key=${kv_pair%$delim*}; 
  value=${kv_pair##*$delim};
  dependency_name="${key}-${value}.zip";
  echo "Installing ${dependency_name}...";
  wget -nv "${nexus_base_url}/${key}/${value}/${dependency_name}"; 
  unzip -u -qq "${dependency_name}" -d "${dependencies_path}/${key}";
  pushd "${dependencies_path}/${key}" &> /dev/null;
  chmod +x install.sh && sudo ./install.sh;
  popd &> /dev/null;
  rm "${dependency_name}";
}

check_kv_pair() {
  local kv_pair=${1};
  if [[ ! "${kv_pair}" =~ ^[^=]+=[^=]+$ ]]; then
    echo "${kv_pair} is not a valid key/value pair, so it won't be processed" >&2;
	echo "A valid key/value pair format is : key=value" >&2;
	echo 1;
  else
    echo 0;
  fi
}

delim='=';

if [[ $# -eq 0 ]]; then
  IFS=',';
  mkdir "${dependencies_path}"
  echo "Installing dependencies defined in STEP_DP environment variable...";
  for kv_pair in ${STEP_DP}; do
	[[ $(check_kv_pair $kv_pair) -eq 0 ]] && install_dependency $kv_pair;
  done
  exit 0;
else
  echo "Invalid usage : ${0} requires no argument" >&2;
  echo "Usage: STEP_DP="dependency1=version1,dependencyN=versionN" ${0}" >&2;
  exit 1;
fi
