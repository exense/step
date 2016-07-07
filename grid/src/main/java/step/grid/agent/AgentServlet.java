package step.grid.agent;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.grid.io.AdapterGridIOConstants;
import step.grid.io.AdapterMessageMarshaller;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.io.Input;
import step.grid.io.Output;
import step.grid.io.OutputBuilder;

public class AgentServlet extends HttpServlet {

	private static final long serialVersionUID = -3519671548889221607L;
	
	private static final Logger logger = LoggerFactory.getLogger(AgentServlet.class);
	
	private Agent agent;

	private AdapterMessageMarshaller<Input> inputMarshaller;
	
	private AdapterMessageMarshaller<Output> outputMarshaller;

	public AgentServlet(Agent agent) {
		super();
		this.agent = agent;
	}

	public void init() throws ServletException {

		try {			
			inputMarshaller = new AdapterMessageMarshaller<Input>(Input.class);
			outputMarshaller = new AdapterMessageMarshaller<Output>(Output.class);
		} catch (Exception e) {
			logger.error("Error while initializing servlet",e);
			throw new ServletException(e);
		}
	}

	public void service(HttpServletRequest request, HttpServletResponse response) {
		
		String service = request.getPathInfo();
		
		String tokenID = request.getParameter(AdapterGridIOConstants.TOKEN_ID_PARAM);
		
		if(service.equals(AdapterGridIOConstants.PROCESS_INPUT_CMD)) {
			OutputBuilder output = new OutputBuilder();
			
			String inputXML = request.getParameter(AdapterGridIOConstants.INPUT_PARAM);
			Input input = null;
			try {			
				input = inputMarshaller.unmarshall(inputXML);
			} catch (Exception e) {
				failOutput(output, "Unable to unmarshall input: " + inputXML, e);
			}

//			if(input != null) {
//				
//				AgentTokenWrapper token = tokenPool.getToken(tokenID);
//				if(token!=null) {
//					V resource = token.getResource();
//					try {
//						try {
//							AdapterExecutionContext ctx = new AdapterExecutionContext(adapterConf, token, input);
//							currentContext.set(ctx);
//							
//							if(resource == null) {
//								logger.debug("No resource associated to token : " + tokenID + ". Creating new resoure.");
//								resource = resourceFactory.create(ctx);	
//								token.setResource(resource);
//							} else {
//								boolean resourceValid = resourceFactory.validate(resource, ctx);
//								if(!resourceValid) {
//									logger.debug("Resource invalid. Destroying... TokenID: " + tokenID);
//									try {
//										resourceFactory.destroy(resource);
//									} catch(Exception e) {
//										logger.error("An error occurred while destroying resource for TokenID: " + tokenID,e);
//									}
//									
//									logger.debug("Creating new resource. TokenID: " + tokenID);
//									resource = resourceFactory.create(ctx);
//									token.setResource(resource);
//								}
//							}
//							
//							input.getParameters().putAll(getProperties());
//							
//							processInput(input, output, resource);
//						} catch (Throwable e) {
//							failOutput(output, e);					
//						} finally {
//							currentContext.remove();
//						}
//					} finally {
//						tokenPool.returnToken(tokenID);
//					}
//				} else {
//					failOutput(output, "Invalid token ID:" + tokenID);
//				}
//				
//				
//			}

			Writer writer = null;
			try {
				writer = response.getWriter();
				response.setContentType("text/xml;charset=utf-8");
				
				Output o = output.build();
				outputMarshaller.marshall(o, writer);
			} catch (JAXBException | XMLStreamException e1) {
				logger.error("Unable to marshall output: " + output.build().getPayload(), e1);
				try {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to marshall output"  + output.build().getPayload());
				} catch (IOException e) {}
			} catch (IOException e) {
				logger.error("Unable to send response to client.", e);
			}
			try {
				if (writer != null) {
					writer.flush();
				}
			} catch (IOException e) {
			}
		} else if (service.equals(AdapterGridIOConstants.PING_CMD)) {
			response.setContentType("text/plain");
			try {
				PrintWriter writer = response.getWriter();
				writer.write("OK");
				writer.flush();
			} catch (IOException e) {
				logger.error("Error occurred while get writer", e);
			}
		} else if (service.equals(AdapterGridIOConstants.TOKENS_CMD)) {
			Writer writer = null;
			try {
				writer = response.getWriter();
				response.setContentType("application/json;charset=utf-8");
				ObjectMapper mapper = new ObjectMapper();
				mapper.writeValue(writer, agent.getTokens());
				writer.flush();
			} catch (IOException e) {
				logger.error("Error occurred while get writer", e);
			}
//		} 
////		else if (service.equals(AdapterGridIOConstants.INTERRUPT_CMD)) {
////			registrationTask.interrupt();
////			registrationTask.unregister();
////		} else if (service.equals(AdapterGridIOConstants.RESUME_CMD)) {
////			registrationTask.resume();
		} else {
			logger.error("Unknown service: " + service);
			try {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unknown service: " + service);
			} catch (IOException e) {}
		}
	}
	
	protected void failOutput(OutputBuilder output, String errorMessage) {
		output.setTechnicalError(errorMessage);
		logger.error(errorMessage);
	}
	
	protected void failOutput(OutputBuilder output, Throwable e) {
		String errorMessage = "Unexpected exception occurred in the adapter: " + e.getMessage()!=null?e.getMessage():e.toString() + ". See exception.log for details.";
		logger.error(errorMessage, e);
		output.setTechnicalError(errorMessage);
		output.addAttachment(generateAttachmentForException(e));
	}
	
	protected void failOutput(OutputBuilder outputBuilder, String errorMessage, Throwable e) {
		outputBuilder.setTechnicalError(errorMessage);
		logger.error(errorMessage, e);
		outputBuilder.addAttachment(generateAttachmentForException(e));
	}

	@Override
	public void destroy() {		

	}

	
	protected Attachment generateAttachmentForException(Throwable e) {
		Attachment attachment = new Attachment();	
		attachment.setName("exception.log");
		StringWriter w = new StringWriter();
		e.printStackTrace(new PrintWriter(w));
		attachment.setHexContent(AttachmentHelper.getHex(w.toString().getBytes()));
		return attachment;
	}
}


