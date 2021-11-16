package step.core.security.password;

import org.junit.Test;

public class RequireNumericPolicyTest {

    @Test(expected = PasswordPolicyViolation.class)
    public void testNumericPolicyNotMet() throws Exception{
        new RequireNumericPolicy().verify("abcedfghihjklmnopqrstuvwzyzABCXYZ");
    }

    @Test
    public void testNumericPolicyMet() throws Exception{
        new RequireNumericPolicy().verify("a1X");
    }
}