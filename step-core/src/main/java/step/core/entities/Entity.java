package step.core.entities;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.CRUDAccessor;
import step.core.imports.Importer;

public class Entity<A extends AbstractIdentifiableObject, T extends CRUDAccessor<A>> {
	
	private String name;
	private T accessor;
	private Class<A> entityClass;
	private Importer<A,T> importer;
	
	public Entity(String name, T accessor, Class<A> entityClass, Importer<A,T> importer) {
		super();
		this.name = name;
		this.accessor = accessor;
		this.entityClass = entityClass;
		this.importer = importer;
		this.importer.init(this);
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public T getAccessor() {
		return accessor;
	}
	
	public void setAccessor(T accessor) {
		this.accessor = accessor;
	}

	public Class<A> getEntityClass() {
		return entityClass;
	}

	public void setEntityClass(Class<A> entityClass) {
		this.entityClass = entityClass;
	}

	public Importer<A,T> getImporter() {
		return importer;
	}

	public void setImporter(Importer<A,T> importer) {
		this.importer = importer;
	}

}
