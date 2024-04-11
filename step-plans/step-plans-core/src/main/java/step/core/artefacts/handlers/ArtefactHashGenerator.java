package step.core.artefacts.handlers;

import jakarta.xml.bind.DatatypeConverter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ArtefactHashGenerator {

    private static MessageDigest messageDigest;

    public ArtefactHashGenerator() {
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String generateArtefactHash(String currentPath, String artefactId) {
        return generateArtefactHash(getPath(currentPath, artefactId));
    }

    public static String getPath(String currentPath, String artefactId) {
        return currentPath != null ? currentPath + artefactId : artefactId;
    }

    public String generateArtefactHash(String path) {
        messageDigest.update(path.getBytes());
        byte[] digest = messageDigest.digest();
        return DatatypeConverter.printHexBinary(digest);
    }

}
