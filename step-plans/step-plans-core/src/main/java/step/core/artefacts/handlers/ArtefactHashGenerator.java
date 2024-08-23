package step.core.artefacts.handlers;

import jakarta.xml.bind.DatatypeConverter;
import org.apache.commons.codec.digest.DigestUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ArtefactHashGenerator {

    public String generateArtefactHash(String currentPath, String artefactId) {
        return generateArtefactHash(getPath(currentPath, artefactId));
    }

    public static String getPath(String currentPath, String artefactId) {
        return currentPath != null ? currentPath + artefactId : artefactId;
    }

    public String generateArtefactHash(String path) {
        // The result of DigestUtils.getMd5Digest() is not thread safe.
        // TODO use ThreadLocal or cache if necessary
        byte[] digest = DigestUtils.getMd5Digest().digest(path.getBytes());
        return DatatypeConverter.printHexBinary(digest);
    }

}
