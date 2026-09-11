package step.automation.packages.yaml;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Filename conventions for files derived from an entity name inside an automation package directory.
 * <p>
 * Extracted so that everything deriving a filename from an entity name (plan fragments written by
 * {@link AutomationPackageYamlFragmentManager}, and files stored alongside them such as the test case specs of the
 * IDE) stays in lockstep by construction.
 */
public class YamlFragmentFilenames {

    private YamlFragmentFilenames() {
    }

    /**
     * Makes an entity name safe to use as a filename. Characters that are illegal in a path (including
     * <code>/</code>, <code>\</code> and <code>:</code>) are percent encoded; spaces are preserved for readability.
     *
     * @param inputName the filename to sanitize, <b>including</b> its extension (e.g. <code>My Plan.yml</code>)
     * @return the sanitized filename
     */
    public static String sanitizeFilename(String inputName) {
        return URLEncoder.encode(inputName, StandardCharsets.UTF_8).replace("+", " ");
    }
}
