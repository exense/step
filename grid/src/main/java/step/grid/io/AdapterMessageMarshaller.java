package step.grid.io;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;

public class AdapterMessageMarshaller<T extends AdapterMessage> {

	protected JAXBContext inputJaxbContext;
	
	public AdapterMessageMarshaller(Class<T> clazz) {
		super();
		try {
			inputJaxbContext = JAXBContext.newInstance(clazz);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String marshall(T input) throws JAXBException, XMLStreamException {
		StringWriter writer = new StringWriter();
		marshall(input, writer);
		return writer.toString();
	}
	
	public void marshall(T input, Writer writer) throws JAXBException, XMLStreamException {
		Marshaller marshaller = inputJaxbContext.createMarshaller();
		marshaller.marshal(input, writer);
	}
	
	public T unmarshall(String inputXML) throws JAXBException, ParserException {
		StringReader reader = new StringReader(inputXML);
		return unmarshall(reader);
	}
	
	public T unmarshall(Reader reader) throws JAXBException, ParserException {
		Unmarshaller unmarshaller = inputJaxbContext.createUnmarshaller();
		@SuppressWarnings("unchecked")
		T message = (T) unmarshaller.unmarshal(reader);
		return message;
	}
}
