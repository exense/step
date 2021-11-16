package step.core.security.password;

import org.junit.Test;

public class RequireUpperCasePolicyTest {

    @Test(expected = PasswordPolicyViolation.class)
    public void testUpperCasePolicyNotMet() throws Exception{
        new RequireUpperCasePolicy().verify("4567890abcedfghihjklmnopqrstuvwzyz123");
    }

    @Test
    public void testUpperCasePolicyMet() throws Exception{
        new RequireUpperCasePolicy().verify("12345678M9");
    }
}