package step.grid.io;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.glassfish.jersey.internal.util.Base64;

public class AttachmentHelper {
	
	public static String getHex(byte[] raw) {
		return new String(Base64.encode(raw));
	}
	
	public static byte[] hexStringToByteArray(String s) {
		return Base64.decode(s.getBytes());
	}

	public static Attachment generateAttachmentForException(Throwable e) {
		Attachment attachment = new Attachment();	
		attachment.setName("exception.log");
		StringWriter w = new StringWriter();
		e.printStackTrace(new PrintWriter(w));
		attachment.setHexContent(AttachmentHelper.getHex(w.toString().getBytes()));
		return attachment;
	}
}
