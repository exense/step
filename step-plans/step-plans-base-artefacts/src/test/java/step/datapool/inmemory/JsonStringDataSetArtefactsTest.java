package step.datapool.inmemory;

import step.artefacts.DataSetArtefact;
import step.artefacts.ForBlock;
import step.core.dynamicbeans.DynamicValue;
import step.datapool.AbstractDataPoolTest;
import step.datapool.DataPoolConfiguration;
import java.io.IOException;

public class JsonStringDataSetArtefactsTest extends AbstractDataPoolTest {

	protected boolean supportDataSetUpdate() {
		return true;
	}

	protected boolean isInMemory() {
		return true;
	}
	protected DataPoolConfiguration getDataPoolConfiguration() throws IOException {
		JsonStringDataPoolConfiguration pool = new JsonStringDataPoolConfiguration();
		pool.setJson(new DynamicValue<String>("{ \"Col1\" : [\"row11\", \"row21\"], \"Col2\" : [\"row12\", \"row22\"] }"));
		return pool;
	}

	protected String getDataSourceType() {
		return "json";
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
