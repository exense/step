/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.datapool.excel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WorkbookFile implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(WorkbookFile.class);
		
	private File file;
	
	private InputStream inputStream;
	
	private OutputStream outputStream;
	
	private Workbook workbook;

	public WorkbookFile() {
		super();
	}
	
	public WorkbookFile(File mainWorkbook, Integer maxWorkbookSize, boolean createIfNotExists) {
		open(mainWorkbook, maxWorkbookSize, createIfNotExists, false);
	}
	
	public WorkbookFile(File mainWorkbook, Integer maxWorkbookSize, boolean createIfNotExists, boolean forUpdate) {
		try {
			open(mainWorkbook, maxWorkbookSize, createIfNotExists, forUpdate);
		} catch(Exception e) {
			close();
			throw e;
		}
	}
	
	private void create(File mainWorkbook) {
		file = mainWorkbook;
		workbook = new XSSFWorkbook();
	}
	
	void open(File mainWorkbook, Integer maxWorkbookSize, boolean createIfNotExists, boolean forUpdate) {		
		file = mainWorkbook;
		
		if(mainWorkbook.exists()) {
			if(mainWorkbook.canRead()) {
				if(forUpdate && isFileIsOpenedByAnotherProcess()) {
					throw new RuntimeException("The file '" + file.getAbsolutePath() + "' is not writable. It might already be opened by another process.");
				}

				checkFileSize(mainWorkbook, maxWorkbookSize);
								
				try {
					inputStream = new BufferedInputStream(new FileInputStream(mainWorkbook));
					workbook = WorkbookFactory.create(inputStream);
				} catch (EncryptedDocumentException | IOException e) {
					throw new RuntimeException("Error while opening workbook '" + mainWorkbook.getName() + "'", e);
				}	
					
			} else {
				throw new RuntimeException("The workbook '" + mainWorkbook.getName() + "' cannot be read.");
			}
		} else {
			if(createIfNotExists) {
				create(mainWorkbook);
			} else {
				throw new RuntimeException("The workbook '" + mainWorkbook.getName() + "' doesn't exist.");				
			}
		}
	}
	
	void checkFileSize(File file, Integer maxWorkbookSize) {
		if(maxWorkbookSize!=null && file.length()>maxWorkbookSize) {
			throw new RuntimeException("The size of the workbook '" + file.getName() + "' exceeds the max size " + maxWorkbookSize + " bytes.");
		}
	}
	
	public void save() {
		workbook.setForceFormulaRecalculation(true);
		try {
			closeInputStream();

			try {
				openOutputStream(file);
			} catch(IOException e) {
				File backupFile = createBackupFile();
				logger.info("Unable to open workbook "+file.getAbsolutePath()+". Creating backup file."+backupFile.getAbsolutePath());
				try {
					openOutputStream(backupFile);
				} catch(IOException e2) {
					throw new RuntimeException("Unable to open backup workbook "+file.getAbsolutePath());
				}
			}
			
			writeWorkbookToOutputStream();
		} catch (IOException e) {
			logger.error("Error saving workbook "+file.getAbsolutePath(), e);
		} finally {
			closeOutputStream();
		}
	}

	private void closeOutputStream() {
		try {
			if(outputStream!=null) {
				outputStream.close();
			}
		} catch (IOException e) {
			logger.error("Error while closing outputstream",e);
		} finally {
			outputStream = null;
		}
	}
	
	private void closeInputStream() {
		try {
			if(inputStream!=null) {
				inputStream.close();
			}
		} catch (IOException e) {
			logger.error("Error while closing inputstream",e);
		} finally {
			inputStream = null;
		}
	}
	
	private static Pattern FILENAME_PATTERN = Pattern.compile("(.*)\\.(.+?)$");
	private static String DATE_FORMAT = "yyyyMMddhhmmss";

	private File createBackupFile() {
		Matcher m = FILENAME_PATTERN.matcher(file.getAbsolutePath());
		if(m.find()) {
			String filename = m.group(1);
			String extension = m.group(2);
			SimpleDateFormat f = new SimpleDateFormat(DATE_FORMAT);
			
			String backupFilename = filename + "_" + f.format(new Date()) + "." + extension;
			return new File(backupFilename);
		} else {
			throw new RuntimeException("Unable to create backup file. The path "+file.getAbsolutePath()+" doesn't match the expected pattern");
		}
	}
	
	private boolean isFileIsOpenedByAnotherProcess() {
		try(FileOutputStream outputStream = new FileOutputStream(file, true);) 
		{
			return false;
		} catch (IOException e) {
			return true;
		}
	}
	
	private void openOutputStream(File file) throws FileNotFoundException {
		outputStream = new BufferedOutputStream(new FileOutputStream(file));
	}
	

	private void writeWorkbookToOutputStream() throws IOException {
		workbook.write(outputStream);
	}
	
	public void close() {
		closeInputStream();
		closeOutputStream();
		
		try {
			if (workbook != null) {
				workbook.close();
			}
		} catch (IOException e) {}
	}

	public Workbook getWorkbook() {
		return workbook;
	}
}
