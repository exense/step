package step.grid.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class AttachmentHelper {

	static final String HEXES = "0123456789ABCDEF";
	
	public static String getHexRepresentation(File file) throws IOException {
		FileInputStream s = null;
		try {
			s = new FileInputStream(file);
			byte fileContent[] = new byte[(int)file.length()];
			s.read(fileContent);
			return getHex(fileContent);
		} finally {
			if(s!=null) {
				s.close();							
			}
		}
	}
	
	public static String getHex(byte[] raw) {
		if (raw == null) {
			return null;
		}
		final StringBuilder hex = new StringBuilder(2 * raw.length);
		for (final byte b : raw) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
		}
		return hex.toString();
	}
	
	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

}
