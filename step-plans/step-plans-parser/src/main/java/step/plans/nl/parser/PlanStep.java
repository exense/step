package step.plans.nl.parser;

import step.repositories.parser.AbstractStep;

public class PlanStep extends AbstractStep {

	String name;
	
	String description;
	
	String command;
	
	String line;

	public PlanStep(String name, String command, String description) {
		super();
		this.name = name;
		this.command = command;
		this.description = description;
	}

	public PlanStep(String name, String description, String command, String line) {
		super();
		this.name = name;
		this.description = description;
		this.command = command;
		this.line = line;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getLine() {
		return line;
	}

	public void setLine(String line) {
		this.line = line;
	}
}
