package step.datapool.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import step.core.variables.SimpleStringMap;
import step.datapool.DataPoolRow;

public class CSVReaderDataPool extends FileReaderDataPool {

	public static final Logger logger = LoggerFactory.getLogger(CSVReaderDataPool.class);

	protected Vector<String> headers;
	protected String delimiter;

	// indicates if a write operation using RowWrapper.put occurred
	protected AtomicBoolean hasChanges = new AtomicBoolean(false);
	// the temporary file in which the new file containing the write operations is
	// written to
	protected File tempFile;
	protected PrintWriter tempFileWriter;

	public CSVReaderDataPool(CSVDataPool configuration) {
		super(configuration);
		this.delimiter = configuration.getDelimiter().get();
	}

	@Override
	public void init() {
		super.init();

		// Write operations to rows (RowWrapper.put) are written to a temporary file
		// which
		// overrides the initial file when the data pool is closed
		tempFile = new File(filePath + ".tmp");
		try {
			tempFileWriter = new PrintWriter(new BufferedWriter(new FileWriter(tempFile)));

			// write headers to the temporary file
			if (headers!=null) {
				Iterator<String> iterator = headers.iterator();
				while (iterator.hasNext()) {
					String header = iterator.next();
					tempFileWriter.write(header);
					if (iterator.hasNext()) {
						tempFileWriter.write(delimiter);
					}
				}
			}
			tempFileWriter.println();
		} catch (IOException e) {
			logger.error("Error while creating temporary file " + tempFile.getAbsolutePath(), e);
		}
	}

	@Override
	public void close() {
		super.close();

		try {
			tempFileWriter.close();
			// persist the changes if necessary
			if (hasChanges.get()) {
				// move the initial file
				File initialFile = new File(filePath + ".initial");
				Files.move(new File(filePath), initialFile);
				// replace the initial file by the temporary file containing the changes
				Files.move(tempFile, new File(filePath));
				// delete the initial file
				initialFile.delete();
			}
			tempFile.delete();
		} catch (IOException e) {
			logger.error("Error while closing the CSV dataset", e);
		}
	}

	@Override
	public void writeRow(DataPoolRow row) throws IOException {
		super.writeRow(row);

		Object value = row.getValue();
		if (value != null && value instanceof CSVRowWrapper) {
			CSVRowWrapper csvRow = (CSVRowWrapper) value;

			Iterator<String> iterator = headers.iterator();
			while (iterator.hasNext()) {
				String header = iterator.next();
				tempFileWriter.print(csvRow.rowData.get(header).toString());
				if (iterator.hasNext()) {
					tempFileWriter.print(delimiter);
				}
			}
			tempFileWriter.println();
		}
	}

	@Override
	public Object postProcess(String line) {

		HashMap<String, Object> map = new HashMap<String, Object>();
		try {
			Vector<String> csv = splitCSV(line);
			for (int i = 0; i < csv.size(); i++) {
				map.put(headers.get(i), csv.get(i));
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new RuntimeException(
					e.getMessage() + " : headers=" + headers + "; row=" + line + "; delimiter=" + delimiter);
		}

		return new CSVRowWrapper(super.lineNr, map);
	}

	public class CSVRowWrapper extends SimpleStringMap {

		private HashMap<String, Object> rowData;

		public CSVRowWrapper(int rowNum, HashMap<String, Object> row) {
			super();

			if (rowNum < 1)
				throw new RuntimeException("Invalid row number:" + rowNum);
			this.rowData = row;
		}

		@Override
		public String put(String key, String value) {
			if (isRowCommitEnabled) {
				rowData.put(key, value);
				hasChanges.set(true);
			} else {
				throw new RuntimeException(
						"Row commit disabled. Writing to CSV data sets is not supported in this mode.");
			}
			return value;
		}

		@Override
		public String get(String key) {
			return (String) rowData.get(key);
		}

		@Override
		public int size() {
			return rowData.size();
		}

		@Override
		public boolean isEmpty() {
			return rowData.isEmpty();
		}

		@Override
		public Set<String> keySet() {
			return new HashSet<>(headers);
		}

	}

	public Vector<String> getHeaders(String readOneLine) {
		return readOneLine == null ? null : splitCSV(readOneLine);
	}

	public Vector<String> splitCSV(String readOneLine) {

		Vector<String> v = new Vector<String>();
		for (String s : readOneLine.split(this.delimiter, -1))
			v.add(s);

		return v;
	}

	@Override
	public void doFirst_() {
		this.headers = getHeaders(readOneLine());
	}

	@Override
	public void addRow(Object row) {
		throw new RuntimeException("Not implemented");
	}
}
