package step.engine.plugins;

import step.grid.TokenPretender;
import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Interest;
import step.grid.tokenpool.SimpleAffinityEvaluator;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StepTokenAffinityEvaluator extends SimpleAffinityEvaluator {
    public static final String TOKEN_ATTRIBUTE_DOCKER_IMAGE = "$dockerImage";
    public static final String TOKEN_ATTRIBUTE_DOCKER_SUPPORT = "$supportsCustomDockerImage";

    @Override
    public int getAffinityScore(Identity i1, Identity i2) {
        return super.getAffinityScore(replaceCriteria(i1), replaceCriteria(i2));
    }

    private static TokenPretender replaceCriteria(Identity i1) {
        Map<String, Interest> newInterests;
        Map<String, Interest> interests = i1.getInterests();
        if(interests != null) {
            newInterests = interests.entrySet().stream().map(e -> {
                if (e.getKey().equals(TOKEN_ATTRIBUTE_DOCKER_IMAGE)) {
                    return Map.entry(TOKEN_ATTRIBUTE_DOCKER_SUPPORT, new Interest(Pattern.compile("true"), true));
                } else {
                    return e;
                }
            }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        } else {
            newInterests = null;
        }
        TokenPretender changedI1 = new TokenPretender(i1.getAttributes(), newInterests);
        return changedI1;
    }
}
