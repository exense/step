package step.plugins.encryption;

import step.core.encryption.EncryptionManager;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

/**
 * Plugins requiring an {@link EncryptionManager} should depend on this plugin.
 * The initialization of the {@link EncryptionManager} is done by 3rd party plugins
 * that we'll be guaranteed to run before this plugin
 *
 */
@Plugin
public class EncryptionManagerDependencyPlugin extends AbstractControllerPlugin {

}
