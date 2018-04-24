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
package step.datapool.excel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.model.ExternalLinksTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkbookSet implements AutoCloseable{
	
	private LinkedWorkbookFileResolver resolver;
	
	private List<WorkbookFile> workbooks = new ArrayList<>();
	
	private File mainWorkbookFile;
	
	private WorkbookFile mainWorkbook;
	
	private FormulaEvaluator mainFormulaEvaluator;
	
	private static final Logger logger = LoggerFactory.getLogger(WorkbookSet.class);

	public WorkbookSet(File mainWorkbookFile, Integer maxWorkbookSize, LinkedWorkbookFileResolver resolver, boolean createIfNotExists, boolean forUpdate) {
		this.resolver = resolver;
		
		this.mainWorkbookFile = mainWorkbookFile;
		
		mainWorkbook = openWorkbook(mainWorkbookFile, maxWorkbookSize, createIfNotExists, forUpdate);
		mainFormulaEvaluator = mainWorkbook.getWorkbook().getCreationHelper().createFormulaEvaluator();
		
		openReferencedWorkbooks(maxWorkbookSize, mainWorkbook.getWorkbook(), mainFormulaEvaluator);
	}	
	
	public WorkbookSet(File mainWorkbookFile, Integer maxWorkbookSize, boolean createIfNotExists, boolean forUpdate) {
		this(mainWorkbookFile, maxWorkbookSize, new LinkedWorkbookFileResolver() {
			@Override
			public File resolve(String linkedFilename) {
				if(linkedFilename.startsWith("file:///")) {
					return new File(linkedFilename.substring(8));
				} else {
					return resolveRelativePath(mainWorkbookFile, linkedFilename);
				}
			}
		}, createIfNotExists, forUpdate);
	}
	
	private static File resolveRelativePath(File mainWorkbookFile, String linkedFilename) {
		File parent = mainWorkbookFile.getParentFile();
		if(parent != null) {
			File absoluteLinkedFile = new File(parent + "/" + linkedFilename);
			if(absoluteLinkedFile.exists()) {
				return absoluteLinkedFile;
			} else {
				return resolveRelativePath(parent, linkedFilename);
			}
		} else {
			return null;
		}
	}
	
	public void save() throws IOException {
		mainWorkbook.save();
	}
	
	public void close() {
		for (WorkbookFile workbook : workbooks) {
			workbook.close();
		}
		workbooks = null;
	}

	public File getMainWorkbookFile() {
		return mainWorkbookFile;
	}

	public Workbook getMainWorkbook() {
		return mainWorkbook.getWorkbook();
	}

	public FormulaEvaluator getMainFormulaEvaluator() {
		return mainFormulaEvaluator;
	}

	private void openReferencedWorkbooks(Integer maxWorkbookSize, Workbook workbook, FormulaEvaluator evaluator) {
		Map<String,FormulaEvaluator> workbooks = new HashMap<String, FormulaEvaluator>();
		
		if(workbook instanceof XSSFWorkbook) {
			if(((XSSFWorkbook)workbook).getExternalLinksTable()!=null) {
				for (ExternalLinksTable sheet : ((XSSFWorkbook)workbook).getExternalLinksTable()) {
					String file = sheet.getLinkedFileName();
					File f = resolver.resolve(file);
					if(f!=null) {
						try {
							WorkbookFile book = openWorkbook(f, maxWorkbookSize, false, false);
							workbooks.put(file, book.getWorkbook().getCreationHelper().createFormulaEvaluator());
						} catch(Exception e) {
							logger.error("An error occured while opening referenced workbook '"+file+"'. Main workbook: '"+mainWorkbookFile+"'");
						}
					} else {
						logger.warn("Unable to resolve external workbook '"+file+"'. Main workbook: '"+mainWorkbookFile+"'");
					}
				}
			}
		}
		
		workbooks.put("this", evaluator);			
		evaluator.setupReferencedWorkbooks(workbooks);
	}
	
	private WorkbookFile openWorkbook(File mainWorkbook, Integer maxWorkbookSize, boolean createIfNotExists, boolean forUpdate) {
		WorkbookFile openedOprkbook = new WorkbookFile();
		openedOprkbook.open(mainWorkbook, maxWorkbookSize, createIfNotExists, forUpdate);
		
		workbooks.add(openedOprkbook);
		return openedOprkbook;
	}
	
	public interface LinkedWorkbookFileResolver {
		
		public File resolve(String linkedFilename);
		
	}
	
}
