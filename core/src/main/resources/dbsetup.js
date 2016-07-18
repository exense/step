db.reports.createIndex( { executionID: 1 } );
db.reports.createIndex( { parentID: 1 } );
db.reports.createIndex( { status: 1 } );
db.executions.createIndex( { startTime: 1 } );
db.executions.createIndex( { description: 1 } );
db.executions.createIndex( { "executionParameters.userID": 1 } );