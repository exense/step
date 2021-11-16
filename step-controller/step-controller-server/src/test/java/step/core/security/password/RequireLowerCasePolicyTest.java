package step.core.security.password;

import org.junit.Test;

public class RequireLowerCasePolicyTest {

    @Test(expected = PasswordPolicyViolation.class)
    public void testLowerCasePolicyNotMet() throws Exception{
        new RequireLowerCasePolicy().verify("4567890XYZ123");
    }

    @Test
    public void testLowerCasePolicyMet() throws Exception{
        new RequireLowerCasePolicy().verify("12345678M9ABCDx");
    }
}