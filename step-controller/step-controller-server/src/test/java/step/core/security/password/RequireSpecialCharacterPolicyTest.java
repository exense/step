package step.core.security.password;

import org.junit.Test;

public class RequireSpecialCharacterPolicyTest {
    @Test(expected = PasswordPolicyViolation.class)
    public void testSpecialCharacterPolicyNotMet() throws Exception{
        new RequireSpecialCharacterPolicy("!@#$%&*").verify("123");
    }

    @Test
    public void testSpecialCharacterPolicyMet() throws Exception{
        new RequireSpecialCharacterPolicy("!@#$%&*").verify("a*&b%c$d#@e!");
    }
}
