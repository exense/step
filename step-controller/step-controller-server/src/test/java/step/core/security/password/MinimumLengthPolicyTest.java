package step.core.security.password;

import org.junit.Test;

public class MinimumLengthPolicyTest {

    @Test(expected = PasswordPolicyViolation.class)
    public void testMinimumLengthPolicyNotMet() throws Exception{
        new MinimumLengthPolicy(8).verify("123");
    }

    @Test
    public void testMinimumLengthPolicyMet() throws Exception{
        new MinimumLengthPolicy(8).verify("123456789");
    }
}