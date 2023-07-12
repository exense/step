package step.datapool.file;

import ch.exense.commons.io.FileHelper;
import step.artefacts.DataSetArtefact;
import step.artefacts.ForBlock;
import step.core.dynamicbeans.DynamicValue;
import step.datapool.AbstractDataPoolTest;
import step.datapool.DataPoolConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class CSVDataSetArtefactTest extends AbstractDataPoolTest {

	protected boolean supportDataSetUpdate() {
		return false;
	}

	@Override
	protected boolean isInMemory() {
		return false;
	}
	protected DataPoolConfiguration getDataPoolConfiguration() throws IOException {
		CSVDataPool pool = new CSVDataPool();
		File file = FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(), "File.csv");
		File tempFile = FileHelper.createTempFile();
		try (OutputStream os = new FileOutputStream(tempFile)) {
			Files.copy(file.toPath(), os);
		}
		pool.setFile(new DynamicValue<String>(tempFile.getAbsolutePath()));
		return pool;
	}

	protected String getDataSourceType() {
		return "csv";
	}

	protected DataSetArtefact getDataSetArtefact(boolean resetAtEnd) throws IOException {
		DataSetArtefact dataSetArtefact = new DataSetArtefact();
		dataSetArtefact.setDataSource(getDataPoolConfiguration());
		dataSetArtefact.setDataSourceType(getDataSourceType());
		dataSetArtefact.setResetAtEnd(new DynamicValue<>(resetAtEnd));
		return dataSetArtefact;
	}

	protected ForBlock getForBlock() throws IOException {
		ForBlock f = new ForBlock();
		f.setDataSourceType(getDataSourceType());
		f.setDataSource(getDataPoolConfiguration());
		f.setItem(new DynamicValue<>("item"));
		return f;
	}

}
