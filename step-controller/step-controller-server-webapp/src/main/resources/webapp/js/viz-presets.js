function StaticPresets() {
    return {
        queries: [
            {
                "name": "RTM Measurements - File",
                "query": {
                    "inputtype": "Raw",
                    "type": "Simple",
                    "datasource": {
                        "service": {
                            "url": "/mocks/001_RESPONSE_Simple_RTM_Measurements.json",
                            "method": "Get",
                            "postproc": {
                                "transform": {
                                    "function": "function (response) {\r\n    var x = 'begin', y = 'value', z = 'name';\r\n    var retData = [], index = {};\r\n    var payload = response.data.payload;\r\n    for (var i = 0; i < payload.length; i++) {\r\n        retData.push({\r\n            x: payload[i][x],\r\n            y: payload[i][y],\r\n            z: payload[i][z]\r\n        });\r\n    }\r\n    return retData;\r\n}",
                                    "abs": { "title": "time", "unit": "seconds" },
                                    "ord": { "title": "duration", "unit": "ms" },
                                    "transformations": [{ "path": "timestamp", "function": "function () {Math.random().toString(36).substr(2, 9);}" }]
                                }
                            }
                        }
                    }
                }
            },
            {
                "name": "RTM Measurements - Call",
                "query": {
                    "inputtype": "Raw",
                    "type": "Simple",
                    "datasource": {
                        "service": {
                            "url": "/rtm/rest/measurement/find",
                            "method": "Post",
                            "data": "{\
                                \"selectors1\": [{ \"textFilters\": [{ \"key\": \"eId\", \"value\": \"JUnit_Dynamic\", \"regex\": \"false\" }, { \"key\": \"name\", \"value\": \"Transaction_1\", \"regex\": \"false\" }], \"numericalFilters\": [] }],\
                                \"serviceParams\": { \"measurementService.nextFactor\": \"0\", \"aggregateService.sessionId\": \"defaultSid\", \"aggregateService.granularity\": \"auto\", \"aggregateService.groupby\": \"name\", \"aggregateService.cpu\": \"1\", \"aggregateService.partition\": \"8\", \"aggregateService.timeout\": \"600\" }\
                            }",
                            "postproc": {
                                "transform": {
                                    "function": "function(response){var retData = [];var index = {};var payload = response.data.payload;for (var i = 0; i < payload.length; i++) {var curSeries = payload[i].name;if (!(curSeries in index)) {retData.push({values: [],key: curSeries});index[curSeries] = retData.length - 1;}retData[index[curSeries]].values.push({ x: payload[i].begin, y: payload[i].value });}return retData;}",
                                    "abs": { "title": "time", "unit": "seconds" }, "ord": { "title": "duration", "unit": "ms" },
                                    "transformations": [{ "path": "timestamp", "function": "function () {Math.random().toString(36).substr(2, 9);}" }]
                                }
                            }
                        }
                    }
                }
            },
            {
                "name": "RTM Aggregates - File",
                "query": {
                    "inputtype": "Raw",
                    "type": "Async",
                    "datasource": {
                        "service": {
                            "url": "/mocks/002_RESPONSE_Async_Get_RTM_Aggregates.json",
                            "method": "Get",
                            "data": "{\
                                \"selectors1\": [{ \"textFilters\": [{ \"key\": \"eId\", \"value\": \"JUnit_Dynamic\", \"regex\": \"false\" }, { \"key\": \"name\", \"value\": \"Transaction_1\", \"regex\": \"false\" }], \"numericalFilters\": [] }],\
                                \"serviceParams\": { \"measurementService.nextFactor\": \"0\", \"aggregateService.sessionId\": \"defaultSid\", \"aggregateService.granularity\": \"auto\", \"aggregateService.groupby\": \"name\", \"aggregateService.cpu\": \"1\", \"aggregateService.partition\": \"8\", \"aggregateService.timeout\": \"600\" }\
                            }",
                            "postproc": {
                                "save": {
                                    "function": "function(response){return [{ placeholder : '__streamedSessionId__', value : response.data.payload.streamedSessionId, isDynamic : false }];}",
                                }
                            }
                        },
                        "callback": {
                            "url": "/mocks/002_RESPONSE_Async_Refresh_RTM_Aggregates.json",
                            "method": "Get",
                            "data": {
                                "streamedSessionId": "__streamedSessionId__"
                            },
                            "preproc": {
                                "replace": {
                                    "target": "data",
                                    "function": "function(requestFragment, workData){var newRequestFragment = requestFragment;for(i=0;i<workData.length;i++){newRequestFragment = newRequestFragment.replace(workData[i].placeholder, workData[i].value);}return newRequestFragment;}",
                                }
                            },
                            "postproc": {
                                "asyncEnd": {
                                    "function": "function(response){return response.data.payload.stream.complete;}",
                                },
                                "transform": {
                                    "function": "function (response) {\r\n    var metric = 'avg';\r\n    var retData = [], series = {};\r\n\r\n    var payload = response.data.payload.stream.streamData;\r\n    var payloadKeys = Object.keys(payload);\r\n\r\n    for (i = 0; i < payloadKeys.length; i++) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[i]])\r\n        for (j = 0; j < serieskeys.length; j++) {\r\n            retData.push({\r\n                x: payloadKeys[i],\r\n                y: payload[payloadKeys[i]][serieskeys[j]][metric],\r\n                z: serieskeys[j]\r\n            });\r\n        }\r\n    }\r\n    return retData;\r\n}"
                                }
                            }
                        }
                    }
                }
            },
            {
                "name": "RTM Aggregates - Call",
                "query": {
                    "inputtype": "Raw",
                    "type": "Async",
                    "datasource": {
                        "service": {
                            "url": "/rtm/rest/aggregate/get",
                            "method": "Post",
                            "data": "{\
                                \"selectors1\": [{ \"textFilters\": [{ \"key\": \"eId\", \"value\": \".*\", \"regex\": \"true\" }, { \"key\": \"name\", \"value\": \"Transaction_1\", \"regex\": \"false\" }], \"numericalFilters\": [] }],\
                                \"serviceParams\": { \"measurementService.nextFactor\": \"0\", \"aggregateService.sessionId\": \"defaultSid\", \"aggregateService.granularity\": \"auto\", \"aggregateService.groupby\": \"name\", \"aggregateService.cpu\": \"1\", \"aggregateService.partition\": \"8\", \"aggregateService.timeout\": \"600\" }\
                            }",
                            "postproc": {
                                "save": {
                                    "function": "function(response){return [{ placeholder : '__streamedSessionId__', value : response.data.payload.streamedSessionId, isDynamic : false }];}",
                                }
                            }
                        },
                        "callback": {
                            "url": "/rtm/rest/aggregate/refresh",
                            "method": "Post",
                            "data": {
                                "streamedSessionId": "__streamedSessionId__"
                            },
                            "preproc": {
                                "replace": {
                                    "target": "data",
                                    "function": "function(requestFragment, workData){var newRequestFragment = requestFragment;for(i=0;i<workData.length;i++){newRequestFragment = newRequestFragment.replace(workData[i].placeholder, workData[i].value);}return newRequestFragment;}",
                                }
                            },
                            "postproc": {
                                "asyncEnd": {
                                    "function": "function(response){return response.data.payload.stream.complete;}",
                                },
                                "transform": {
                                    "function": "function (response) {\r\n    var metric = 'avg';\r\n    var retData = [], series = {};\r\n\r\n    var payload = response.data.payload.stream.streamData;\r\n    var payloadKeys = Object.keys(payload);\r\n\r\n    for (i = 0; i < payloadKeys.length; i++) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[i]])\r\n        for (j = 0; j < serieskeys.length; j++) {\r\n            retData.push({\r\n                x: payloadKeys[i],\r\n                y: payload[payloadKeys[i]][serieskeys[j]][metric],\r\n                z: serieskeys[j]\r\n            });\r\n        }\r\n    }\r\n    return retData;\r\n}",
                                }
                            }
                        }
                    }
                }
            }
        ],
        controls: {
            templates: [
                {
                    "name": 'RTM Measurements Template',
                    "placeholders": [{ "placeholder": "__eId__", "value": "?", "isDynamic" : false }, { "placeholder": "__name__", "value": "?", "isDynamic" : false }, { "placeholder": "__minValue__", "value": "0", "isDynamic" : false }, { "placeholder": "__maxValue__", "value": "999999999", "isDynamic" : false }],
                    "queryTemplate": {
                        "type": "Simple",
                        "datasource": {
                            "service": {
                                "url": "/rtm/rest/measurement/find",
                                "method": "Post",
                                "data": "{ \"selectors1\": [{ \"textFilters\": [{ \"key\": \"eId\", \"value\": \"__eId__\", \"regex\": \"false\" }, { \"key\": \"name\", \"value\": \"__name__\", \"regex\": \"false\" }], \"numericalFilters\": [{\"key\":\"begin\",\"minValue\":\"__minValue__\",\"maxValue\":\"__maxValue__\"}] }], \"serviceParams\": { \"measurementService.nextFactor\": \"0\", \"aggregateService.sessionId\": \"defaultSid\", \"aggregateService.granularity\": \"auto\", \"aggregateService.groupby\": \"name\", \"aggregateService.cpu\": \"1\", \"aggregateService.partition\": \"8\", \"aggregateService.timeout\": \"600\" } }",
                                "preproc": {
                                    "replace": {
                                        "target": "data",
                                        "function": "function(requestFragment, workData){var newRequestFragment = requestFragment;for(i=0;i<workData.length;i++){newRequestFragment = newRequestFragment.replace(workData[i].placeholder, workData[i].value);}return newRequestFragment;}",
                                    }
                                },
                                "postproc": {
                                    "transform": {
                                        "function": "function(response){var retData = [];var index = {};var payload = response.data.payload;for (var i = 0; i < payload.length; i++) {var curSeries = payload[i].name;if (!(curSeries in index)) {retData.push({values: [],key: curSeries});index[curSeries] = retData.length - 1;}retData[index[curSeries]].values.push({ x: payload[i].begin, y: payload[i].value });}return retData;}",
                                    }
                                }
                            }
                        }
                    }
                }
            ]
        },
        configs: [
            {
                name: 'defaultConfig',
                display: {
                    type: 'lineChart',
                    autorefresh: 'Off'
                }
            }
        ]
    };
}