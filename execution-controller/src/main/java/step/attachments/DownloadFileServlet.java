/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
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
import step.commons.conf.Configuration;

public class DownloadFileServlet extends HttpServlet {
	
	private static final long serialVersionUID = 8922992243834734217L;

	private AttachmentManager attachmentManager = new AttachmentManager(Configuration.getInstance());
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String uuid = (String) request.getParameter("uuid");
		String deleteAfterDownload = (String) request.getParameter("deleteAfterDownload");
		
		File downloadFile = attachmentManager.getFileById(uuid);
				
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
			attachmentManager.deleteContainer(uuid);
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
