package step.artefacts.handlers;

import static org.junit.Assert.*;

import org.json.XML;
import org.junit.Test;

public class TestXMLtoJSON {

	@Test
	public void test() {		
		XML.toJSONObject("<KW100 Para1=\"TEST\" />");
	}

}
