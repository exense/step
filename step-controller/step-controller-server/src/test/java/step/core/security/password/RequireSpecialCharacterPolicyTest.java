package step.core.security.password;

import org.junit.Test;

public class RequireSpecialCharacterPolicyTest {
    @Test(expected = PasswordPolicyViolation.class)
    public void testSpecialCharacterPolicyNotMet() throws Exception{
        new RequireSpecialCharacterPolicy("!@#$%&*").verify("123");
    }

    @Test
    public void testSpecialCharacterPolicyMetSimple() throws Exception{
        new RequireSpecialCharacterPolicy("!@#$%&*").verify("*");
        new RequireSpecialCharacterPolicy("\\").verify("\\");
        new RequireSpecialCharacterPolicy("[").verify("[");
    }

    @Test
    public void testSpecialCharacterPolicyMetExtended() throws Exception{
        new RequireSpecialCharacterPolicy("Ä").verify("Ä");
        new RequireSpecialCharacterPolicy("ß").verify("ß");
    }

    @Test
    public void testSpecialCharacterPolicyMetUnicode() throws Exception{
        new RequireSpecialCharacterPolicy("✔").verify("✔");
    }

}
