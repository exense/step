package step.artefacts;

import step.artefacts.handlers.SessionHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;

@Artefact(handler = SessionHandler.class)
public class Session extends AbstractArtefact {

}
