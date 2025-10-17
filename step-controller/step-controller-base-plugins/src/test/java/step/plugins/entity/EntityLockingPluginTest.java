package step.plugins.entity;

import org.junit.Test;
import step.core.GlobalContext;
import step.core.deployment.ControllerServiceException;
import step.core.objectenricher.ObjectAccessException;
import step.core.objectenricher.ObjectAccessViolation;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.Plan;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static step.controller.services.entities.AbstractEntityServices.CUSTOM_FIELD_LOCKED;

public class EntityLockingPluginTest {

	@Test
	public void test() throws Exception {
		EntityLockingPlugin entityLockingPlugin = new EntityLockingPlugin();
		GlobalContext globalContext = new GlobalContext();
		ObjectHookRegistry objectHooks = new ObjectHookRegistry();
		globalContext.put(ObjectHookRegistry.class, objectHooks);
		entityLockingPlugin.serverStart(globalContext);

		Plan p = new Plan();
		assertTrue(objectHooks.isObjectEditableInContext(null, p).isEmpty());
		p.addCustomField(CUSTOM_FIELD_LOCKED, true);
		Optional<ObjectAccessException> optionalViolations = objectHooks.isObjectEditableInContext(null, p);
		assertTrue(optionalViolations.isPresent());
		ObjectAccessException objectAccessException = optionalViolations.get();
		assertEquals("This entity is locked and cannot be edited", objectAccessException.getMessage());
		List<ObjectAccessViolation> violations = objectAccessException.getViolations();
		assertEquals(1, violations.size());
		assertEquals("This entity is locked and cannot be edited", violations.get(0).message);

	}

}