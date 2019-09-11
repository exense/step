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
