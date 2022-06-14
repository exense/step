#!/bin/bash

upsert_property() {
  local kv_pair=$1;
  key=${kv_pair%$delim*};
  value=${kv_pair##*$delim};
  key_found=$(grep -q ${key} $file_to_update);
  if [[ $? -eq 0 ]]; then
    echo "Updating property ${key} value to ${value} in ${file_to_update}";
    sed -i -r "s:^#?(${key}${delim}).*:\1${value}:g" $file_to_update;
  else
    echo "Inserting property ${kv_pair} to ${file_to_update}";
    echo ${kv_pair} >> $file_to_update;
  fi
}

upsert_json_property() {
  local kv_pair=$1;
  key=${kv_pair%$delim*};
  value=${kv_pair##*$delim};
  jq ".${key}=${value}" ${file_to_update} | sponge ${file_to_update};
}

delim='=';
file_to_update=${1}

if [[ $# -eq 1 ]]; then
  file_to_update_extension=${file_to_update##*.};
  echo "${file_to_update} extension is ${file_to_update##*.}";
  old_IFS=$IFS;
  IFS=',';
  case ${file_to_update_extension} in
	"properties")
		echo "Merging values found in STEP_KV environment variable to ${file_to_update}...";
		echo -e "\n####### ADDITIONAL PROPERTIES ######\n" >> $file_to_update
		for kv_pair in ${STEP_KV}; do
			upsert_property $kv_pair;
		done
	;;
  
	"json")
		echo "Merging values found in STEP_KV environment variable to ${file_to_update}...";
		for kv_pair in ${STEP_KV}; do
			upsert_json_property $kv_pair;
		done
	;;
  
	*)
		echo "Invalid file type: must be json or property file";
		exit 2;
	;;
  esac
  IFS=$old_IFS;
  exit 0;
else
  echo "Invalid usage : ${0} requires 1 argument";
  echo "Usage: ${0} FILE_TO_UPDATE";
  exit 1;
fi
