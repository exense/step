package step.adapters.selenium;

import org.openqa.selenium.WebDriver;

import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public interface SeleniumScript {

	public OutputMessage run(WebDriver driver, InputMessage input);
}
