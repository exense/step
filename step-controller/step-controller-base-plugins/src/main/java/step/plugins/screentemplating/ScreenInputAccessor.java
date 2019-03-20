package step.plugins.screentemplating;

import java.util.List;

import step.core.accessors.CRUDAccessor;

public interface ScreenInputAccessor extends CRUDAccessor<ScreenInput> {

	List<ScreenInput> getScreenInputsByScreenId(String screenId);

}