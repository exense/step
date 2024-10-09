package step.core.artefacts.handlers;

import jakarta.xml.bind.DatatypeConverter;
import org.apache.commons.codec.digest.DigestUtils;
import step.core.artefacts.AbstractArtefact;

/**
 * this helper class is used to determine an artefact path in a plan and to generate corresponding hash
 */
public class ArtefactPathHelper {

    public static String generateArtefactHash(String currentPath, AbstractArtefact artefact) {
        String artefactPath = getPathOfArtefact(currentPath, artefact);
        byte[] digest = DigestUtils.md5(artefactPath.getBytes());
        return DatatypeConverter.printHexBinary(digest);
    }

    public static String getPathOfArtefact(String currentPath, AbstractArtefact artefact) {
        String artefactId = getArtefactId(artefact);
        return getPathOfArtefact(currentPath, artefactId);
    }

    public static String getPathOfArtefact(String currentPath, String artefactId) {
        return currentPath != null ? currentPath + artefactId : artefactId;
    }

    public static String getArtefactId(AbstractArtefact artefact) {
        return artefact.getId().toHexString();
    }

}
