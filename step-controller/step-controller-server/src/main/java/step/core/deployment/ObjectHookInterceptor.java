/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.deployment;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.access.User;
import step.core.collections.PojoFilter;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.objectenricher.ObjectAccessException;
import step.core.ql.OQLFilterBuilder;
import step.framework.server.Session;
import step.framework.server.security.Secured;

@Secured
@Provider
public class ObjectHookInterceptor extends AbstractStepServices implements ReaderInterceptor, WriterInterceptor  {

	public static final String ENTITY_ACCESS_DENIED = "ENTITY_ACCESS_DENIED";
	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(ObjectHookInterceptor.class);

	@Inject
	private ExtendedUriInfo extendendUriInfo;
	
	private ObjectHookRegistry objectHookRegistry;

	private final Predicate<Object> isNotEnricheable = e->!(e instanceof EnricheableObject);
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		objectHookRegistry = getContext().get(ObjectHookRegistry.class);
	}

	@Override
	public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
		Object entity = context.proceed();
		if(entity instanceof EnricheableObject) {
			EnricheableObject enricheableObject = (EnricheableObject) entity;
			Unfiltered annotation = extendendUriInfo.getMatchedResourceMethod().getInvocable().getHandlingMethod().getAnnotation(Unfiltered.class);
			if (annotation == null) {
				Session<User> session = getSession();
				ObjectAccessException accessException = objectHookRegistry.checkObjectAccess(session, enricheableObject);
				if (accessException != null) {
					throw new ControllerServiceException(
						HttpStatus.SC_FORBIDDEN, ENTITY_ACCESS_DENIED,
						accessException.getMessage(), accessException.getViolations()
					);
				} else {
					ObjectEnricher objectEnricher = objectHookRegistry.getObjectEnricher(session);
					objectEnricher.accept(enricheableObject);
				}
			}
		}
		return entity;
	}

	@Override
	public void aroundWriteTo(WriterInterceptorContext context) 
			throws IOException, WebApplicationException {
		Unfiltered annotation = extendendUriInfo.getMatchedResourceMethod().getInvocable().getHandlingMethod().getAnnotation(Unfiltered.class);
		if(annotation == null) {
			Object entity = context.getEntity();
			Session<User> session = getSession();
			String oqlFilter = objectHookRegistry.getObjectFilter(session).getOQLFilter();
			PojoFilter<Object> filter = OQLFilterBuilder.getPojoFilter(oqlFilter);
			Predicate<Object> predicate = isNotEnricheable.or(filter);
			//When returning list of entities we filter the entities accessible from the current context (predicate)
			if(entity instanceof List) {
				List<?> list = (List<?>)entity;
				final List<?> newList = list.stream().filter(predicate).collect(Collectors.toList());
				context.setEntity(newList);
			} else {
				//For single entity we first check access based on predicate, in case access is not granted we can get detailed violations with checkObjectAccess
				if(!predicate.test(entity)) {
					if (entity instanceof EnricheableObject) {
						ObjectAccessException accessException = objectHookRegistry.checkObjectAccess(session, (EnricheableObject) entity);
						if (accessException != null) {
							throw new ControllerServiceException(
								HttpStatus.SC_FORBIDDEN, ENTITY_ACCESS_DENIED,
								accessException.getMessage(), accessException.getViolations()
							);
						}
					}
					//We should not enter this case anymore unless there is a discrepancy between the predicate and checkObjectAccess implementation
					throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, "You're not allowed to access this object from within this context");
				}
			}
		}
		context.proceed();
	}
}
