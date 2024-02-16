package step.core.deployment;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import step.core.GlobalContext;
import step.framework.server.security.Secured;

import java.io.IOException;
import java.util.Map;

import static step.core.Controller.USER_ACTIVITY_MAP_KEY;


@Secured
@Provider
@Priority(Priorities.USER)
public class UserActivityFilter extends AbstractStepServices implements ContainerRequestFilter {

    private Map<String, Long> userActivityMap;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        userActivityMap = (Map<String, Long>) context.require(USER_ACTIVITY_MAP_KEY);
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        updateUserActivityMap();
    }

    private void updateUserActivityMap() {
        String username = getSession().getUser().getUsername();
        userActivityMap.put(username, System.currentTimeMillis());
    }
}
