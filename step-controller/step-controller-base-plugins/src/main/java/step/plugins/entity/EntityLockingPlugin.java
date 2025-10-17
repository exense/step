package step.plugins.entity;

import step.core.AbstractContext;
import step.core.GlobalContext;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.objectenricher.*;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

import java.util.Optional;

import static step.controller.services.entities.AbstractEntityServices.CUSTOM_FIELD_LOCKED;
import static step.core.deployment.ObjectHookInterceptor.ENTITY_ACCESS_DENIED;

@Plugin(dependencies = ObjectHookControllerPlugin.class)
public class EntityLockingPlugin extends AbstractControllerPlugin {

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		super.serverStart(context);
		ObjectHookRegistry objectHookRegistry = context.require(ObjectHookRegistry.class);
		objectHookRegistry.add(new ObjectHook() {
			public static final String ENTITY_LOCKING_ACCESS_CONTROL = "Entity Locking Access Control";

			@Override
			public ObjectFilter getObjectFilter(AbstractContext context) {
				return null;
			}

			@Override
			public ObjectEnricher getObjectEnricher(AbstractContext context) {
				return null;
			}

			@Override
			public void rebuildContext(AbstractContext context, EnricheableObject object) throws Exception {

			}

			@Override
			public String getHookIdentifier() {
				return ENTITY_LOCKING_ACCESS_CONTROL;
			}

			@Override
			public Optional<ObjectAccessViolation> isObjectEditableInContext(AbstractContext context, EnricheableObject object) {
				if (object instanceof AbstractIdentifiableObject) {
					AbstractIdentifiableObject a = (AbstractIdentifiableObject) object;
					Boolean locked = a.getCustomField(CUSTOM_FIELD_LOCKED, Boolean.class);
					if (locked != null && locked) {
						return Optional.of(new ObjectAccessViolation(ENTITY_LOCKING_ACCESS_CONTROL, ENTITY_ACCESS_DENIED, "This entity is locked and cannot be edited"));
					}
				}
				return Optional.empty();
			}
		});
	}
}
