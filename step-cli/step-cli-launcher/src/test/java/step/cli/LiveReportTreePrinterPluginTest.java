/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.cli;

import org.apache.commons.io.output.TeeOutputStream;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.CallFunction;
import step.artefacts.Sequence;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.FunctionPlugin;
import step.engine.plugins.LocalFunctionPlugin;
import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;
import step.threadpool.ThreadPoolPlugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static step.planbuilder.BaseArtefacts.check;
import static step.planbuilder.BaseArtefacts.childrenBlock;
import static step.planbuilder.BaseArtefacts.echo;
import static step.planbuilder.BaseArtefacts.sequence;
import static step.planbuilder.BaseArtefacts.set;
import static step.planbuilder.BaseArtefacts.sleep;
import static step.planbuilder.BaseArtefacts.testCase;
import static step.planbuilder.BaseArtefacts.testSet;
import static step.planbuilder.BaseArtefacts.threadGroup;
import static step.planbuilder.FunctionArtefacts.keywordWithDynamicKeyValues;

public class LiveReportTreePrinterPluginTest {

    // Long enough for no node of these plans to be printed by the hold timer, except where the test intends it
    private static final Duration LONG_HOLD_DELAY = Duration.ofMinutes(1);

    @Rule
    public final TemporaryFolder reportDirectory = new TemporaryFolder();

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private Charset outputCharset;

    @Test
    public void printsThePathOfEveryNodeAndTheStartOfTheNodesContainingOthers() {
        Sequence sequence = sequence();
        sequence.setBefore(childrenBlock(echo("'before'")));
        sequence.setAfter(childrenBlock(echo("'after'")));
        Plan plan = PlanBuilder.create()
            .startBlock(sequence)
            .add(echo("'main'"))
            .startBlock(sequence())
            .add(check("true"))
            .endBlock()
            .endBlock().build();

        PlanRunnerResult result = execute(plan, LONG_HOLD_DELAY);

        Assert.assertEquals(ReportNodeStatus.PASSED, result.getResult());
        Assert.assertEquals(List.of(
            "▶ Sequence",
            "✔ Sequence > [BEFORE] > Echo(before) : PASSED (<duration>)",
            "✔ Sequence > Echo(main) : PASSED (<duration>)",
            "▶ Sequence > Sequence",
            "✔ Sequence > Sequence > Check(true) : PASSED (<duration>)",
            "✔ Sequence > Sequence : PASSED (<duration>)",
            "✔ Sequence > [AFTER] > Echo(after) : PASSED (<duration>)",
            "✔ Sequence : PASSED (<duration>)"
        ), outputLines());
    }

    @Test
    public void printsTheErrorOfAFailingNode() {
        Plan plan = PlanBuilder.create()
            .startBlock(sequence())
            .add(check("false"))
            .endBlock().build();

        PlanRunnerResult result = execute(plan, LONG_HOLD_DELAY);

        Assert.assertEquals(ReportNodeStatus.FAILED, result.getResult());
        List<String> lines = outputLines();
        Assert.assertEquals(lines.toString(), 3, lines.size());
        Assert.assertTrue(lines.get(1), lines.get(1).startsWith("✖ Sequence > Check(false) : FAILED (<duration>) "));
        Assert.assertEquals("✖ Sequence : FAILED (<duration>)", lines.get(2));
    }

    @Test
    public void fallsBackToAsciiMarkersWhenTheOutputCannotEncodeTheOthers() {
        Plan plan = PlanBuilder.create()
            .startBlock(sequence())
            .add(echo("'main'"))
            .add(check("false"))
            .endBlock().build();

        execute(plan, LONG_HOLD_DELAY, StandardCharsets.US_ASCII);

        Assert.assertEquals(List.of(
            "> Sequence",
            "+ Sequence > Echo(main) : PASSED (<duration>)"
        ), outputLines().subList(0, 2));
        Assert.assertTrue(outputLines().get(2), outputLines().get(2).startsWith("x Sequence > Check(false) : FAILED (<duration>) "));
        Assert.assertEquals("x Sequence : FAILED (<duration>)", outputLines().get(3));
    }

    @Test
    public void printsTheStartOfALeafRunningForLongerThanTheHoldDelay() {
        Plan plan = PlanBuilder.create()
            .startBlock(sequence())
            .add(echo("'quick'"))
            .add(sleep(1000))
            .endBlock().build();

        execute(plan, Duration.ofMillis(300));

        // The start of the sequence is printed once, before its first child, although the sequence also outlasts the delay
        Assert.assertEquals(List.of(
            "▶ Sequence",
            "✔ Sequence > Echo(quick) : PASSED (<duration>)",
            "▶ Sequence > Sleep",
            "✔ Sequence > Sleep : PASSED (<duration>)",
            "✔ Sequence : PASSED (<duration>)"
        ), outputLines());
    }

    @Test
    public void printsTheThreadsOfAThreadGroupInThePathOfTheirNodes() {
        Plan plan = PlanBuilder.create()
            .startBlock(threadGroup(2, 5))
            .add(echo("'iteration'"))
            .add(sleep(10))
            .endBlock().build();

        execute(plan, LONG_HOLD_DELAY);

        List<String> lines = outputLines();
        // Every line is written in one piece: an interleaving of the threads cannot break one
        Pattern wellFormedLine = Pattern.compile("(▶ ThreadGroup( > [^>]+)*|✔ ThreadGroup( > [^>]+)* : PASSED \\(<duration>\\))");
        lines.forEach(line -> Assert.assertTrue(line, wellFormedLine.matcher(line).matches()));

        Assert.assertEquals("▶ ThreadGroup", lines.get(0));
        Assert.assertEquals("✔ ThreadGroup : PASSED (<duration>)", lines.get(lines.size() - 1));
        for (String thread : List.of("Thread 1", "Thread 2")) {
            Assert.assertTrue(lines.toString(), lines.contains("▶ ThreadGroup > " + thread));
            long echoes = lines.stream().filter(line -> line.startsWith("✔ ThreadGroup > " + thread + " > ")
                && line.contains(" > Echo(iteration) : PASSED")).count();
            Assert.assertEquals(thread + " should have run 5 iterations: " + lines, 5, echoes);
        }
    }

    /**
     * A plan as written for functional testing: test cases chaining keyword calls, each call checking the output of
     * its keyword and setting the part of it the next call takes as input.
     */
    @Test
    public void printsAFunctionalTestPlan() throws IOException {
        Plan plan = PlanBuilder.create()
            .startBlock(testSet("Online shop"))
            .startBlock(testCase("Order an article"))
            .add(echo("'Logging in as alice'"))
            .startBlock(keyword("Log in", "user", "'alice'"))
            .add(check("output.token == 'token-alice'"))
            .add(set("token", "output.token"))
            .endBlock()
            .add(echo("'Searching the article'"))
            .startBlock(keyword("Search article", "token", "token", "query", "'keyboard'"))
            .add(check("output.articleId == 'article-keyboard'"))
            .add(set("articleId", "output.articleId"))
            .endBlock()
            .add(echo("'Adding the article to the cart'"))
            .startBlock(keyword("Add article to cart", "token", "token", "articleId", "articleId"))
            .add(check("output.itemCount == '1'"))
            .add(set("cartId", "output.cartId"))
            .endBlock()
            .add(echo("'Placing the order'"))
            .startBlock(keyword("Place order", "token", "token", "cartId", "cartId"))
            .add(check("output.orderId == 'order-1'"))
            .endBlock()
            .endBlock()
            .startBlock(testCase("Search an unknown article"))
            .add(echo("'Logging in as bob'"))
            .startBlock(keyword("Log in", "user", "'bob'"))
            .add(set("token", "output.token"))
            .endBlock()
            .add(echo("'Searching an article which does not exist'"))
            .startBlock(keyword("Search article", "token", "token", "query", "'unicorn'"))
            .add(check("output.articleId == 'article-unicorn'"))
            .endBlock()
            .endBlock()
            .startBlock(testCase("Order with an expired token"))
            .add(echo("'Placing an order with an expired token'"))
            .startBlock(keyword("Place order", "token", "'expired'", "cartId", "'cart-1'"))
            .add(check("output.orderId != null"))
            .endBlock()
            .endBlock()
            .endBlock().build();

        PlanRunnerResult result = execute(plan, LONG_HOLD_DELAY);

        Assert.assertEquals(ReportNodeStatus.TECHNICAL_ERROR, result.getResult());
        Assert.assertEquals(List.of(
            "▶ Online shop",
            "▶ Online shop > Order an article",
            "▶ Online shop > Order an article > Session",
            "✔ Online shop > Order an article > Session > Echo(Logging in as alice) : PASSED (<duration>)",
            "▶ Online shop > Order an article > Session > Log in",
            "✔ Online shop > Order an article > Session > Log in > Check(output.token == 'token-alice') : PASSED (<duration>)",
            "✔ Online shop > Order an article > Session > Log in > Set(token = token-alice) : PASSED (<duration>)",
            "✔ Online shop > Order an article > Session > Log in(Input={\"user\":\"alice\"}, Output={\"token\":\"token-alice\"}) : PASSED (<duration>)",
            "✔ Online shop > Order an article > Session > Echo(Searching the article) : PASSED (<duration>)",
            "▶ Online shop > Order an article > Session > Search article",
            "✔ Online shop > Order an article > Session > Search article > Check(output.articleId == 'article-keyboard') : PASSED (<duration>)",
            "✔ Online shop > Order an article > Session > Search article > Set(articleId = article-keyboard) : PASSED (<duration>)",
            "✔ Online shop > Order an article > Session > Search article(Input={\"token\":\"token-alice\",\"query\":\"keyboard\"}, Output={\"articleId\":\"article-keyboard\"}) : PASSED (<duration>)",
            "✔ Online shop > Order an article > Session > Echo(Adding the article to the cart) : PASSED (<duration>)",
            "▶ Online shop > Order an article > Session > Add article to cart",
            "✔ Online shop > Order an article > Session > Add article to cart > Check(output.itemCount == '1') : PASSED (<duration>)",
            "✔ Online shop > Order an article > Session > Add article to cart > Set(cartId = cart-1) : PASSED (<duration>)",
            "✔ Online shop > Order an article > Session > Add article to cart(Input={\"token\":\"token-alice\",\"articleId\":\"article-keyboard\"}, Output={\"cartId\":\"cart-1\",\"itemCount\":\"1\"}) : PASSED (<duration>)",
            "✔ Online shop > Order an article > Session > Echo(Placing the order) : PASSED (<duration>)",
            "▶ Online shop > Order an article > Session > Place order",
            "✔ Online shop > Order an article > Session > Place order > Check(output.orderId == 'order-1') : PASSED (<duration>)",
            "✔ Online shop > Order an article > Session > Place order(Input={\"token\":\"token-alice\",\"cartId\":\"cart-1\"}, Output={\"orderId\":\"order-1\"}) : PASSED (<duration>)",
            "✔ Online shop > Order an article > Session : PASSED (<duration>)",
            "✔ Online shop > Order an article : PASSED (<duration>)",
            "▶ Online shop > Search an unknown article",
            "▶ Online shop > Search an unknown article > Session",
            "✔ Online shop > Search an unknown article > Session > Echo(Logging in as bob) : PASSED (<duration>)",
            "▶ Online shop > Search an unknown article > Session > Log in",
            "✔ Online shop > Search an unknown article > Session > Log in > Set(token = token-bob) : PASSED (<duration>)",
            "✔ Online shop > Search an unknown article > Session > Log in(Input={\"user\":\"bob\"}, Output={\"token\":\"token-bob\"}) : PASSED (<duration>)",
            "✔ Online shop > Search an unknown article > Session > Echo(Searching an article which does not exist) : PASSED (<duration>)",
            "▶ Online shop > Search an unknown article > Session > Search article",
            "○ Online shop > Search an unknown article > Session > Search article > Check(output.articleId == 'article-unicorn') : NORUN (<duration>)",
            "✖ Online shop > Search an unknown article > Session > Search article(Input={\"token\":\"token-bob\",\"query\":\"unicorn\"}, Output={}) : FAILED (<duration>) No article found for 'unicorn'",
            "✖ Online shop > Search an unknown article > Session : FAILED (<duration>)",
            "✖ Online shop > Search an unknown article : FAILED (<duration>)",
            "▶ Online shop > Order with an expired token",
            "▶ Online shop > Order with an expired token > Session",
            "✔ Online shop > Order with an expired token > Session > Echo(Placing an order with an expired token) : PASSED (<duration>)",
            "▶ Online shop > Order with an expired token > Session > Place order",
            "○ Online shop > Order with an expired token > Session > Place order > Check(output.orderId != null) : NORUN (<duration>)",
            "✖ Online shop > Order with an expired token > Session > Place order(Input={\"token\":\"expired\",\"cartId\":\"cart-1\"}, Output={}) : TECHNICAL_ERROR (<duration>) The token 'expired' is no longer valid",
            "✖ Online shop > Order with an expired token > Session : TECHNICAL_ERROR (<duration>)",
            "✖ Online shop > Order with an expired token : TECHNICAL_ERROR (<duration>)",
            "✖ Online shop : TECHNICAL_ERROR (<duration>)"
        ), outputLines());

        result.printTree();
    }

    /**
     * The attachments of the nodes of a test case taking a screenshot of the page and then failing on an exception
     */
    @Test
    public void writesTheAttachmentsOfTheNodesAndPrintsWhereBelowThem() throws IOException {
        Plan plan = PlanBuilder.create()
            .startBlock(testCase("Order with an expired token"))
            .add(keyword("Take screenshot"))
            .add(keyword("Place order", "token", "'expired'", "cartId", "'cart-1'"))
            .endBlock().build();
        plan.addAttribute(AbstractOrganizableObject.NAME, "Online shop");
        AttachmentExporter exporter = new AttachmentExporter(reportDirectory.getRoot().toPath(), LocalDateTime.of(2026, 11, 2, 14, 30, 12));

        PlanRunnerResult result = execute(plan, LONG_HOLD_DELAY, StandardCharsets.UTF_8, exporter);

        Path runDirectory = exporter.getRunDirectory();
        Assert.assertEquals(reportDirectory.getRoot().toPath().resolve("20261102-143012-000000-local"), runDirectory);
        Assert.assertEquals(List.of(
            "▶ Order with an expired token",
            "▶ Order with an expired token > Session",
            "✔ Order with an expired token > Session > Take screenshot(Input={}, Output={}) : PASSED (<duration>)",
            "    attachment: <run>/Online_shop/0001-Take_screenshot/page.png",
            "    attachment: <run>/Online_shop/0001-Take_screenshot/page-2.png",
            "    attachment: <run>/Online_shop/0001-Take_screenshot/console__12_11_.log",
            "✖ Order with an expired token > Session > Place order(Input={\"token\":\"expired\",\"cartId\":\"cart-1\"}, Output={}) : TECHNICAL_ERROR (<duration>) The token 'expired' is no longer valid",
            "    attachment: <run>/Online_shop/0002-Place_order/exception.log",
            "✖ Order with an expired token > Session : TECHNICAL_ERROR (<duration>)",
            "✖ Order with an expired token : TECHNICAL_ERROR (<duration>)"
        ), withRunDirectoryReplaced(outputLines(), runDirectory));

        Path screenshots = runDirectory.resolve("Online_shop").resolve("0001-Take_screenshot");
        Assert.assertEquals("before", Files.readString(screenshots.resolve("page.png")));
        Assert.assertEquals("after", Files.readString(screenshots.resolve("page-2.png")));
        Assert.assertEquals("console", Files.readString(screenshots.resolve("console__12_11_.log")));
        String exceptionLog = Files.readString(runDirectory.resolve("Online_shop").resolve("0002-Place_order").resolve("exception.log"));
        Assert.assertTrue(exceptionLog, exceptionLog.contains("The token 'expired' is no longer valid"));

        List<String> index = Files.readAllLines(runDirectory.resolve(AttachmentExporter.INDEX_FILE_NAME)).stream()
            .map(line -> line.replaceAll("\\b[0-9a-f]{24}\\b", "<node id>"))
            .collect(Collectors.toList());
        Assert.assertEquals(List.of(
            "Plan\tPath\tStatus\tReport node\tAttachment\tFile\tSkipped reason",
            "Online shop\tOrder with an expired token > Session > Take screenshot\tPASSED\t<node id>\tpage.png\tOnline_shop/0001-Take_screenshot/page.png\t",
            "Online shop\tOrder with an expired token > Session > Take screenshot\tPASSED\t<node id>\tpage.png\tOnline_shop/0001-Take_screenshot/page-2.png\t",
            "Online shop\tOrder with an expired token > Session > Take screenshot\tPASSED\t<node id>\tconsole: 12/11?.log\tOnline_shop/0001-Take_screenshot/console__12_11_.log\t",
            "Online shop\tOrder with an expired token > Session > Place order\tTECHNICAL_ERROR\t<node id>\texception.log\tOnline_shop/0002-Place_order/exception.log\t"
        ), index);

        // The report tree printed at the end of the execution tells where they are as well
        StringWriter tree = new StringWriter();
        result.printTree(tree, true, false, exporter::describeAttachments);
        Assert.assertEquals(List.of(
            "Order with an expired token:TECHNICAL_ERROR:",
            " Session:TECHNICAL_ERROR:",
            "  Take screenshot(Input={}, Output={}):PASSED:",
            "   attachment: <run>/Online_shop/0001-Take_screenshot/page.png",
            "   attachment: <run>/Online_shop/0001-Take_screenshot/page-2.png",
            "   attachment: <run>/Online_shop/0001-Take_screenshot/console__12_11_.log",
            "  Place order(Input={\"token\":\"expired\",\"cartId\":\"cart-1\"}, Output={}):TECHNICAL_ERROR:The token 'expired' is no longer valid",
            "   attachment: <run>/Online_shop/0002-Place_order/exception.log"
        ), withRunDirectoryReplaced(Arrays.asList(tree.toString().split("\\R")), runDirectory));
    }

    @Test
    public void writesNothingForAnExecutionWithoutAttachments() {
        Plan plan = PlanBuilder.create()
            .startBlock(sequence())
            .add(echo("'main'"))
            .endBlock().build();
        AttachmentExporter exporter = new AttachmentExporter(reportDirectory.getRoot().toPath());

        execute(plan, LONG_HOLD_DELAY, StandardCharsets.UTF_8, exporter);

        Assert.assertFalse(Files.exists(exporter.getRunDirectory()));
        Assert.assertEquals(0, exporter.getFileCount());
    }

    private static List<String> withRunDirectoryReplaced(List<String> lines, Path runDirectory) {
        // The URI of a directory ends with a slash
        String runDirectoryUri = runDirectory.toAbsolutePath().toUri().toString().replaceAll("/$", "");
        return lines.stream()
            .map(line -> line.replace(runDirectoryUri, "<run>"))
            .collect(Collectors.toList());
    }

    @Test
    public void formatsDurations() {
        Assert.assertEquals("0 ms", LiveReportTreePrinterPlugin.formatDuration(0));
        Assert.assertEquals("999 ms", LiveReportTreePrinterPlugin.formatDuration(999));
        Assert.assertEquals("1.0 s", LiveReportTreePrinterPlugin.formatDuration(1000));
        Assert.assertEquals("61.3 s", LiveReportTreePrinterPlugin.formatDuration(61270));
    }

    private PlanRunnerResult execute(Plan plan, Duration holdDelay) {
        return execute(plan, holdDelay, StandardCharsets.UTF_8);
    }

    private PlanRunnerResult execute(Plan plan, Duration holdDelay, Charset charset) {
        return execute(plan, holdDelay, charset, null);
    }

    private PlanRunnerResult execute(Plan plan, Duration holdDelay, Charset charset, AttachmentExporter attachmentExporter) {
        outputCharset = charset;
        // Also written to the standard output, for the report to be reviewed in the output of the tests
        PrintStream out = new PrintStream(new TeeOutputStream(output, System.out), true, charset);
        try (ExecutionEngine executionEngine = ExecutionEngine.builder()
            .withPlugin(new LiveReportTreePrinterPlugin(out, holdDelay, attachmentExporter))
            .withPlugin(new ThreadPoolPlugin())
            .withPlugin(new BaseArtefactPlugin())
            .withPlugin(new FunctionPlugin())
            .withPlugin(new LocalFunctionPlugin())
            .build()) {
            return executionEngine.execute(plan);
        }
    }

    /**
     * A call of a keyword taking the given inputs, each of them given as an expression
     */
    private static CallFunction keyword(String keywordName, String... inputExpressions) {
        CallFunction call = keywordWithDynamicKeyValues(keywordName, false, inputExpressions);
        call.getAttributes().put(AbstractOrganizableObject.NAME, keywordName);
        return call;
    }

    /**
     * The keywords of {@link #printsAFunctionalTestPlan()}, executed in the JVM of the test
     */
    public static class OnlineShopKeywords extends AbstractKeyword {

        @Keyword(name = "Log in")
        public void logIn() {
            output.add("token", "token-" + input.getString("user"));
        }

        @Keyword(name = "Search article")
        public void searchArticle() {
            String query = input.getString("query");
            if (query.equals("unicorn")) {
                output.setBusinessError("No article found for '" + query + "'");
            } else {
                output.add("articleId", "article-" + query);
            }
        }

        @Keyword(name = "Add article to cart")
        public void addArticleToCart() {
            output.add("cartId", "cart-1");
            output.add("itemCount", "1");
        }

        @Keyword(name = "Take screenshot")
        public void takeScreenshot() {
            // Twice the same name, as a keyword taking a screenshot before and after an action would
            output.addAttachment(AttachmentHelper.generateAttachmentFromByteArray("before".getBytes(StandardCharsets.UTF_8), "page.png"));
            output.addAttachment(AttachmentHelper.generateAttachmentFromByteArray("after".getBytes(StandardCharsets.UTF_8), "page.png"));
            output.addAttachment(AttachmentHelper.generateAttachmentFromByteArray("console".getBytes(StandardCharsets.UTF_8), "console: 12/11?.log"));
        }

        @Keyword(name = "Place order")
        public void placeOrder() {
            if (!input.getString("token").startsWith("token-")) {
                throw new IllegalStateException("The token '" + input.getString("token") + "' is no longer valid");
            }
            output.add("orderId", "order-1");
        }
    }

    private List<String> outputLines() {
        String text = output.toString(outputCharset)
            .replaceAll("\\d+(\\.\\d)? (ms|s)\\)", "<duration>)");
        return Arrays.stream(text.split("\\R")).collect(Collectors.toList());
    }
}
