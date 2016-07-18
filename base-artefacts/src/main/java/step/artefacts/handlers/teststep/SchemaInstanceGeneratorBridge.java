package step.artefacts.handlers.teststep;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.SchemaTypeSystem;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.xsd2inst.SampleXmlUtil;

public class SchemaInstanceGeneratorBridge {
	
    public static String xsd2inst(File[] schemaFiles, String rootName)
    {

        boolean dl = false; 
        boolean nopvr = false;
        boolean noupa = false;
        
        // Process Schema files
        List<XmlObject> sdocs = new ArrayList<>();
        for (int i = 0; i < schemaFiles.length; i++)
        {
            try
            {
                sdocs.add(XmlObject.Factory.parse(schemaFiles[i],
                        (new XmlOptions()).setLoadLineNumbers().setLoadMessageDigest()));
            }
            catch (Exception e)
            {
            	throw new RuntimeException("Can not load schema file: " + schemaFiles[i] + ": ",e);
            }
        }

        XmlObject[] schemas = (XmlObject[]) sdocs.toArray(new XmlObject[sdocs.size()]);

        SchemaTypeSystem sts = null;
        if (schemas.length > 0)
        {
            XmlOptions compileOptions = new XmlOptions();
            if (dl)
                compileOptions.setCompileDownloadUrls();
            if (nopvr)
                compileOptions.setCompileNoPvrRule();
            if (noupa)
                compileOptions.setCompileNoUpaRule();

            try
            {
                sts = XmlBeans.compileXsd(schemas, XmlBeans.getBuiltinTypeSystem(), compileOptions);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        if (sts == null)
        {
            throw new RuntimeException("No Schemas to process.");
        }
        SchemaType[] globalElems = sts.documentTypes();
        SchemaType elem = null;
        for (int i = 0; i < globalElems.length; i++)
        {
            if (rootName.equals(globalElems[i].getDocumentElementName().getLocalPart()))
            {
                elem = globalElems[i];
                break;
            }
        }

        if (elem == null)
        {
        	throw new RuntimeException("Could not find a global element with name \"" + rootName + "\"");
        }
        
        // Now generate it
        String result = SampleXmlUtil.createSampleForType(elem);

        return result;
    }

}
