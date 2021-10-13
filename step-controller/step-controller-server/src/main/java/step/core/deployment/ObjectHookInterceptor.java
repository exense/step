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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.PojoFilter;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.ql.OQLFilterBuilder;

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
		Object entity = context.proceed();

		// TODO implement right validation to prevent malicious usage of this header
		if(!context.getHeaders().containsKey("ignoreContext") || !context.getHeaders().get("ignoreContext").contains("true")) {
			Unfiltered annotation = extendendUriInfo.getMatchedResourceMethod().getInvocable().getHandlingMethod().getAnnotation(Unfiltered.class);
			if(annotation == null) {
				Session session = getSession();
				
				if (!objectHookRegistry.isObjectAcceptableInContext(session, entity)) {
					throw newAccessDenierError();
				} else {
					ObjectEnricher objectEnricher = objectHookRegistry.getObjectEnricher(session);
					objectEnricher.accept(entity);
				}
			}
		}

		return entity;
	}

	private Predicate<Object> isNotAbstractOrganizableObject = e->!(e instanceof AbstractOrganizableObject);

	@Override
	public void aroundWriteTo(WriterInterceptorContext context) 
			throws IOException, WebApplicationException {
		Unfiltered annotation = extendendUriInfo.getMatchedResourceMethod().getInvocable().getHandlingMethod().getAnnotation(Unfiltered.class);
		if(annotation == null) {
			Object entity = context.getEntity();
			Session session = getSession();
			String oqlFilter = objectHookRegistry.getObjectFilter(session).getOQLFilter();
			PojoFilter<Object> filter = OQLFilterBuilder.getPojoFilter(oqlFilter);
			Predicate<Object> predicate = isNotAbstractOrganizableObject.or(filter);
			if(entity instanceof List) {
				List<?> list = (List<?>)entity;
				final List<?> newList = list.stream().filter(predicate).collect(Collectors.toList());
				context.setEntity(newList);
			} else {
				if(!predicate.test(entity)) {
					throw newAccessDenierError();
				}
			}
		}
		context.proceed();
	}

	private ControllerServiceException newAccessDenierError() {
		return new ControllerServiceException(HttpStatus.SC_FORBIDDEN, "You're not allowed to access this object");
	}
}
