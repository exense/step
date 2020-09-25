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
package step.functions.type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import step.attachments.FileResolver;
import step.functions.Function;
import step.grid.GridFileService;

public class FunctionTypeRegistryImpl implements FunctionTypeRegistry {

	protected final FileResolver fileResolver;

	protected final GridFileService gridFileServices;
	
	protected final FunctionTypeConfiguration functionTypeConfiguration;
	
	public FunctionTypeRegistryImpl(FileResolver fileResolver, GridFileService gridFileServices) {
		this(fileResolver, gridFileServices, new FunctionTypeConfiguration());
	}

	public FunctionTypeRegistryImpl(FileResolver fileResolver, GridFileService gridFileServices,
			FunctionTypeConfiguration functionTypeConfiguration) {
		super();
		this.fileResolver = fileResolver;
		this.gridFileServices = gridFileServices;
		this.functionTypeConfiguration = functionTypeConfiguration;
	}

	private final Map<String, AbstractFunctionType<Function>> functionTypes = new ConcurrentHashMap<>();
	
	@SuppressWarnings("unchecked")
	@Override
	public void registerFunctionType(AbstractFunctionType<? extends Function> functionType) {
		functionType.setFunctionTypeConfiguration(functionTypeConfiguration);
		functionType.setFileResolver(fileResolver);
		functionType.setGridFileServices(gridFileServices);
		functionType.init();
		functionTypes.put(functionType.newFunction().getClass().getName(), (AbstractFunctionType<Function>) functionType);
	}
	
	@Override
	public AbstractFunctionType<Function> getFunctionTypeByFunction(Function function) {
		return getFunctionType(function.getClass().getName());
	}

	@Override
	public AbstractFunctionType<Function> getFunctionType(String functionType) {
		AbstractFunctionType<Function> type = (AbstractFunctionType<Function>) functionTypes.get(functionType);
		if(type==null) {
			throw new RuntimeException("Unknown function type '"+functionType+"'");
		} else {
			return type;
		}
	}

}
