package step.plugins.entity;

import step.core.AbstractContext;
import step.core.GlobalContext;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.deployment.ControllerServiceException;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectFilter;
import step.core.objectenricher.ObjectHook;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

import static step.controller.services.entities.AbstractEntityServices.CUSTOM_FIELD_LOCKED;

@Plugin(dependencies = ObjectHookControllerPlugin.class)
public class EntityLockingPlugin extends AbstractControllerPlugin {

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		super.serverStart(context);
		ObjectHookRegistry objectHookRegistry = context.require(ObjectHookRegistry.class);
		objectHookRegistry.add(new ObjectHook() {
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
			public boolean isObjectAcceptableInContext(AbstractContext context, EnricheableObject object) {
				if (object instanceof AbstractIdentifiableObject) {
					AbstractIdentifiableObject a = (AbstractIdentifiableObject) object;
					Boolean locked = a.getCustomField(CUSTOM_FIELD_LOCKED, Boolean.class);
					if (locked != null && locked) {
						throw new ControllerServiceException("This entity is locked and cannot be edited");
					}
				}
				return true;
			}
		});
	}
}
