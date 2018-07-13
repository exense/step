package step.versionmanager;

import java.util.Date;

import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.accessors.MongoDBAccessorHelper;

public class VersionManager {
	
	private static final Logger logger = LoggerFactory.getLogger(VersionManager.class);
	
	private final GlobalContext context;
	
	private final MongoCollection controllerLogs;
	
	private ControllerLog latestControllerLog = null;
	
	public VersionManager(GlobalContext context) {
		super();
		this.context = context;
		controllerLogs = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "controllerlogs");
	}

	public void readLatestControllerLog() {
		MongoCursor<ControllerLog> cursor = controllerLogs.find().sort("{start:-1}").as(ControllerLog.class);
		
		if(cursor.count()>0) {
			latestControllerLog = controllerLogs.find().sort("{start:-1}").as(ControllerLog.class).next();
			logger.info("Last start of the controller: "+ latestControllerLog.toString());
		} else {
			logger.info("No start log found. Starting the controller for the first time against this DB...");
		}
	}
	
	public ControllerLog getLatestControllerLog() {
		return latestControllerLog;
	}

	public void setLatestControllerLog(ControllerLog latestControllerLog) {
		this.latestControllerLog = latestControllerLog;
	}

	public void insertControllerLog() {
		ControllerLog logEntry = new ControllerLog();
		logEntry.setStart(new Date());
		logEntry.setVersion(context.getCurrentVersion());
		controllerLogs.insert(logEntry);
	}
}
