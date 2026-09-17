package info.seleniumcucumber.methods;

import org.junit.Test;
import org.junit.Rule;
import org.junit.Before;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class ImpactHelperTest {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
    Path repo;
    @Before public void setup() throws IOException { repo = temporaryFolder.newFolder().toPath(); }
    private static final String MAIN = "module/src/main/java/demo/";
    private static final String TEST = "module/src/test/java/demo/";
    private static final String FEATURE = "module/src/test/resources/features/shop.feature";

    private static byte[] readBytes(java.io.InputStream stream) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = stream.read(buffer)) != -1) output.write(buffer, 0, length);
        return output.toByteArray();
    }
    private void put(String path, String content) throws IOException {
        Path file = repo.resolve(path);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }
    private String git(String... args) throws Exception {
        List<String> cmd = new ArrayList<>(Arrays.asList("git", "-C", repo.toString()));
        cmd.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String output = new String(readBytes(process.getInputStream()), StandardCharsets.UTF_8);
        assertEquals(output, 0, process.waitFor());
        return output.trim();
    }
    private void commit() throws Exception {
        git("add", ".");
        git("-c", "user.name=Impact Test", "-c", "user.email=impact@example.invalid", "commit", "--quiet", "-m", "fixture");
    }
    private void baseline() throws Exception {
        git("init", "-b", "master");
        put(MAIN + "Service.java", "package demo; class Service { int price() { return 1; } }");
        put(TEST + "Page.java", "package demo; class Page { Service service; }");
        put(TEST + "BuySteps.java", "package demo; class BuySteps { Page page; @Given(\"I buy {int} items\") void buy() {} }");
        put(TEST + "LoginSteps.java", "package demo; class LoginSteps { @Given(\"I log in\") void login() {} }");
        put(FEATURE, "Feature: Shop\n  Scenario: Buy\n    Given I buy 2 items\n  Scenario: Login\n    Given I log in\n");
        commit();
        git("checkout", "-b", "pull-request");
    }
    private ImpactHelper.Result analyze() throws IOException { return ImpactHelper.analyze(repo, "master", "HEAD"); }

    @Test public void tracesTransitiveMainChangesAndWritesReport() throws Exception {
        baseline();
        put(MAIN + "Service.java", "package demo; class Service { int price() { return 2; } }");
        commit();
        ImpactHelper.Result result = analyze();
        assertEquals(Arrays.asList(FEATURE + ":2"), result.selectors());
        assertTrue(result.affectedJavaFiles().contains(TEST + "BuySteps.java"));
        result.writeTo(repo.resolve("target/impact"));
        assertEquals(result.selectors(), Files.readAllLines(repo.resolve("target/impact/impacted-scenarios.txt")));
    }
    @Test public void considersOldAndNewStepExpressions() throws Exception {
        baseline();
        put(TEST + "BuySteps.java", "package demo; class BuySteps { @Given(\"I log in\") void buy() {} }");
        commit();
        assertEquals(Arrays.asList(FEATURE + ":2", FEATURE + ":4"), analyze().selectors());
    }
    @Test public void deletedClassesRemainInDependencyGraph() throws Exception {
        baseline(); Files.delete(repo.resolve(MAIN + "Service.java")); commit();
        assertEquals(Arrays.asList(FEATURE + ":2"), analyze().selectors());
    }
    @Test public void interfaceConsumersAreAffectedByImplementationChange() throws Exception {
        baseline();
        put(MAIN + "Service.java", "package demo; interface Service {} ");
        put(MAIN + "RealService.java", "package demo; class RealService implements Service { int x = 1; }");
        commit(); git("branch", "-f", "master");
        put(MAIN + "RealService.java", "package demo; class RealService implements Service { int x = 2; }"); commit();
        assertEquals(Arrays.asList(FEATURE + ":2"), analyze().selectors());
    }
    @Test public void hooksSelectWholeSuite() throws Exception {
        baseline(); put(TEST + "Hooks.java", "package demo; class Hooks { @Before void setup() {} }"); commit();
        assertEquals(Arrays.asList(FEATURE), analyze().selectors());
    }
    @Test public void oneUnmappedChangeCannotBeMaskedByMappedChange() throws Exception {
        baseline();
        put(MAIN + "Other.java", "package demo; class Other {} ");
        put(MAIN + "Service.java", "package demo; class Service { int x; }"); commit();
        assertEquals(Arrays.asList(FEATURE), analyze().selectors());
    }
    @Test public void mergeBaseExcludesMasterOnlyChangesAndDirtyFiles() throws Exception {
        baseline(); git("checkout", "master");
        put(MAIN + "MasterOnly.java", "package demo; class MasterOnly {} "); commit();
        git("checkout", "pull-request");
        put(MAIN + "Service.java", "package demo; class Service { int x; }"); commit();
        put(TEST + "Uncommitted.java", "invalid java");
        assertEquals(new HashSet<>(Arrays.asList(MAIN + "Service.java")), analyze().changedJavaFiles());
        assertEquals(Arrays.asList(FEATURE + ":2"), analyze().selectors());
    }
    @Test public void unchangedSnapshotsProduceEmptySelection() throws Exception {
        baseline(); assertTrue(analyze().selectors().isEmpty());
    }
    @Test public void backgroundsSelectEveryScenarioAndDocStringsAreIgnored() throws Exception {
        baseline();
        put(FEATURE, "Feature: Shop\nBackground: Common\nGiven I buy 2 items\nScenario: A\nGiven I log in\nScenario: B\nGiven I log in\n\"\"\"\nScenario: fake\nGiven I buy 99 items\n\"\"\"\n");
        commit(); git("branch", "-f", "master");
        put(MAIN + "Service.java", "package demo; class Service { int x; }"); commit();
        assertEquals(Arrays.asList(FEATURE + ":4", FEATURE + ":6"), analyze().selectors());
    }
    @Test public void outlinesSelectAllRowsAndRulesFallBackToWholeFeature() throws Exception {
        baseline();
        put(FEATURE, "Feature: Shop\nScenario Outline: Buy\nGiven I buy <count> items\nExamples:\n| count |\n| 1 |\n| 2 |\n");
        put("rules.feature", "Feature: Rules\nRule: A\nScenario: A\nGiven I log in\n");
        commit(); git("branch", "-f", "master");
        put(MAIN + "Service.java", "package demo; class Service { int x; }"); commit();
        assertEquals(Arrays.asList(FEATURE + ":2", "rules.feature"), analyze().selectors());
    }
    @Test public void regexAndNamedAnnotationArgumentMatch() throws Exception {
        baseline();
        put(TEST + "BuySteps.java", "package demo; class BuySteps { @Given(value = \"^I buy [0-9]+ items$\") void buy() {} }"); commit();
        assertEquals(Arrays.asList(FEATURE + ":2"), analyze().selectors());
    }
    @Test public void changedFeatureIsSelectedWithoutJavaChanges() throws Exception {
        baseline(); put(FEATURE, "Feature: Shop\nScenario: New\nGiven I log in\n"); commit();
        assertEquals(Arrays.asList(FEATURE), analyze().selectors());
    }
    @Test public void unresolvedConstantAndForcedModeSelectWholeSuite() throws Exception {
        baseline();
        put(TEST + "BuySteps.java", "package demo; class BuySteps { @Given(Constants.EXPRESSION) void buy() {} }"); commit();
        assertEquals(Arrays.asList(FEATURE), analyze().selectors());
        assertEquals(Arrays.asList(FEATURE), ImpactHelper.analyze(repo, "master", "HEAD", true).selectors());
    }
    @Test public void invalidRefAndInvalidJavaFailVisibly() throws Exception {
        baseline(); assertThrows(IOException.class, () -> ImpactHelper.analyze(repo, "missing-base", "HEAD"));
        put(MAIN + "Service.java", "class Service { broken"); commit();
        assertThrows(IOException.class, this::analyze);
    }
    @Test public void renamedFileTracksBothOldAndNewPaths() throws Exception {
        baseline();
        Files.move(repo.resolve(MAIN + "Service.java"), repo.resolve(MAIN + "Renamed.java")); commit();
        assertEquals(new HashSet<>(Arrays.asList(MAIN + "Service.java", MAIN + "Renamed.java")), analyze().changedJavaFiles());
        assertEquals(Arrays.asList(FEATURE + ":2"), analyze().selectors());
    }

    @Test public void recordsAndEnumsPropagateThroughImplementedInterfaces() throws Exception {
        baseline();
        put(MAIN + "Service.java", "package demo; interface Service {} ");
        put(MAIN + "RecordService.java", "package demo; record RecordService(int price) implements Service {} ");
        put(MAIN + "EnumService.java", "package demo; enum EnumService implements Service { INSTANCE; int price = 1; }");
        commit(); git("branch", "-f", "master");
        put(MAIN + "RecordService.java", "package demo; record RecordService(int price, int tax) implements Service {} ");
        put(MAIN + "EnumService.java", "package demo; enum EnumService implements Service { INSTANCE; int price = 2; }");
        commit();
        ImpactHelper.Result result = analyze();
        assertEquals(Arrays.asList(FEATURE + ":2"), result.selectors());
        assertFalse(result.reasons().stream().anyMatch(reason -> reason.contains("No static path")));
    }

    @Test public void qualifiedTypesAndConcatenatedAnnotationStringsRemainMapped() throws Exception {
        baseline();
        put(TEST + "Page.java", "package demo; class Page { demo.Service service; }");
        put(TEST + "BuySteps.java", "package demo; class BuySteps { demo.Page page; @io.cucumber.java.en.Given(value = (\"I buy \" + \"{int} items\")) void buy() {} }");
        commit(); git("branch", "-f", "master");
        put(MAIN + "Service.java", "package demo; class Service { int price = 2; }"); commit();
        assertEquals(Arrays.asList(FEATURE + ":2"), analyze().selectors());
    }

    @Test public void textBlockAnnotationStringsAreDecoded() throws Exception {
        baseline();
        put(TEST + "BuySteps.java", "package demo; class BuySteps { Page page; @Given(\"\"\"\nI buy {int} items\"\"\") void buy() {} }");
        commit(); git("branch", "-f", "master");
        put(MAIN + "Service.java", "package demo; class Service { int price = 2; }"); commit();
        assertEquals(Arrays.asList(FEATURE + ":2"), analyze().selectors());
    }
}
