package step.core.deployment;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.server.ExtendedUriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.objectenricher.ObjectHookRegistry;

@Secured
@Provider
public class ObjectHookInterceptor extends AbstractServices implements ReaderInterceptor, WriterInterceptor  {

	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(ObjectHookInterceptor.class);

	@Inject
	private ExtendedUriInfo extendendUriInfo;
	
	private ObjectHookRegistry objectHookRegistry;
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		objectHookRegistry = getContext().get(ObjectHookRegistry.class);
	}

	@Override
	public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
		Object proceed = context.proceed();

		// TODO implement right validation to prevent malicious usage of this header
		if(!context.getHeaders().containsKey("ignoreContext") || !context.getHeaders().get("ignoreContext").contains("true")) {
			Unfiltered annotation = extendendUriInfo.getMatchedResourceMethod().getInvocable().getHandlingMethod().getAnnotation(Unfiltered.class);
			if(annotation == null) {
				Session session = getSession();
				objectHookRegistry.getObjectEnricher(session).accept(proceed);
			}
		}

		return proceed;
	}

	@Override
	public void aroundWriteTo(WriterInterceptorContext context) 
			throws IOException, WebApplicationException {
		Unfiltered annotation = extendendUriInfo.getMatchedResourceMethod().getInvocable().getHandlingMethod().getAnnotation(Unfiltered.class);
		if(annotation == null) {
			Object entity = context.getEntity();
			if(entity instanceof List) {
				List<?> list = (List<?>)entity;
				Session session = getSession();
				final List<?> newList = list.stream().filter(objectHookRegistry.getObjectFilter(session)).collect(Collectors.toList());
				context.setEntity(newList);
			}
		}
		context.proceed();
	}
}