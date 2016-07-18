package step.attachments;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.types.ObjectId;

import step.attachments.AttachmentManager;

public class DownloadFileServlet extends HttpServlet {
	
	private static final long serialVersionUID = 8922992243834734217L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String uuid = (String) request.getParameter("uuid");
		String deleteAfterDownload = (String) request.getParameter("deleteAfterDownload");
		File downloadFile = AttachmentManager.getFileById(new ObjectId(uuid));
				
		FileInputStream inStream = new FileInputStream(downloadFile);

		ServletContext context = getServletContext();
		String mimeType = context.getMimeType(downloadFile.getName());
		if (mimeType == null) {
			mimeType = "application/octet-stream";
		}

		response.setContentType(mimeType);
		response.setContentLength((int) downloadFile.length());
		
		// forces download
		String headerKey = "Content-Disposition";
		String headerValue = String.format("attachment; filename=\"%s\"",
				downloadFile.getName());
		response.setHeader(headerKey, headerValue);
		// obtains response's output stream
		OutputStream outStream = response.getOutputStream();
		byte[] buffer = new byte[4096];
		int bytesRead = -1;
		while ((bytesRead = inStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, bytesRead);
		}
		inStream.close();
		outStream.close();
		
		if(deleteAfterDownload!=null && deleteAfterDownload.equals("true")) {
			//TODO: implement attachment deletion
		}
	}
	
	public static String getBaseURL() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw new RuntimeException("Error while getting base URL", e);
		}
	}
}
