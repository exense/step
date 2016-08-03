package step.grid.io;

import org.glassfish.jersey.internal.util.Base64;

public class AttachmentHelper {
	
	public static String getHex(byte[] raw) {
		return new String(Base64.encode(raw));
	}
	
	public static byte[] hexStringToByteArray(String s) {
		return Base64.decode(s.getBytes());
	}

}
