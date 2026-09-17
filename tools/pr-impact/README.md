# Cucumber PR impact helper

Java 17+ / Maven helper that compares a committed PR branch against `master`, follows Java class dependencies, and produces Cucumber scenario selectors. Source parsing uses [JavaParser](https://javaparser.org/getting-started.html), with no `sun.*` or `com.sun.*` imports. Git and Java 17+ are required to run it; a JDK and Maven are required to build it. JUnit is used only for the helper's tests.

## Build and run

This tool has a separate Maven build because the repository root targets Java 8 and Cucumber 1.2.5. Build from the repository root with `mvn -f tools/pr-impact/pom.xml clean package`, or use the following commands from this helper directory:

```powershell
mvn clean package
java -jar target/cucumber-impact-helper-1.0.0.jar "C:/work/my-bdd-project" master HEAD
```

The executable JAR includes JavaParser, so no additional runtime classpath is necessary. The arguments are repository root, base ref, head ref, output directory, and optionally `--full-suite`. Defaults are `.`, `master`, `HEAD`, and `<repository>/target/impact`. Explicit output paths are relative to the command's working directory.

For CI, first fetch the base branch in the **target repository**, then analyze its checked-out PR head:

```powershell
git -C "C:/work/my-bdd-project" fetch origin master
java -jar target/cucumber-impact-helper-1.0.0.jar "C:/work/my-bdd-project" origin/master HEAD
```

The helper does not fetch, checkout, modify Git configuration, or require a hosting-provider token. Supply any locally available PR branch/ref/commit as the third argument. Use the PR's head commit rather than a CI-generated merge commit. Shallow checkouts need enough history to find the merge base; missing refs/history cause a visible error.

The comparison is `git diff <merge-base(base,head)> <head>`, equivalent to the usual three-dot PR diff. This excludes changes made only on master after the branches diverged. To include the latest master changes in the tested code, merge/rebase in your normal PR workflow first. See [Git's diff documentation](https://git-scm.com/docs/git-diff).

Only committed content is analyzed. Uncommitted files are ignored. The working checkout used to run Cucumber must match the analyzed head commit, otherwise selector line numbers can be wrong.

## Output and execution

`target/impact/impacted-scenarios.txt` contains one selector per line:

```text
src/test/resources/features/checkout.feature:12
src/test/resources/features/login.feature:7
```

Whole-feature paths are emitted when broader selection is necessary. `impact-report.txt` records the resolved commit IDs, changed Java files, affected Java files and selection reasons.

This repository uses Cucumber **1.2.5**, which reads `cucumber.options` rather than the newer `cucumber.features` property. Run the following from the analyzed repository root, after generating the report. Use your normal supported Java/browser environment for the Selenium tests; Java 17+ is needed for the helper itself.

```powershell
$selectors = @(Get-Content "target/impact/impacted-scenarios.txt" | Where-Object { $_.Trim() })
if ($selectors.Count -gt 0) {
    # Cucumber 1.2.5 Shellwords recognizes single quotes around paths with spaces.
    $quotedSelectors = $selectors | ForEach-Object { "'" + $_ + "'" }
    mvn test "-Dcucumber.options=$($quotedSelectors -join ' ')"
    if ($LASTEXITCODE -ne 0) { throw "Cucumber tests failed" }
} else {
    Write-Host "No impacted Cucumber scenarios; skipping the Cucumber invocation."
}
```

The existing `RunCukeTest` runner supplies the glue package and report plugin. Explicit feature paths in `cucumber.options` replace its `classpath:features` selection. The helper scans all tracked `.feature` files, including `src/test/resources/Homerunner Login.feature`, which the runner's default path does not include. Review the generated selectors before executing. These commands do not run automatically when building the helper.

An empty selector file must be handled explicitly: an empty options string leaves the runner's default feature selection in effect. Other configured tag/name filters still apply. A path containing a literal single quote needs a different invocation strategy because of the legacy options parser.

Most predefined steps live together in `PredefinedStepDefinitions.java`. Changes reaching that class can select a large portion of the suite; affected step expressions with no matching current scenario trigger the full-suite fallback. Its screenshot/teardown hook examples are commented out, while the separate test `Hooks.java` contains an active tagged hook. The analyzer collects annotations only from code, not comments.

For a multi-module build, the selectors are relative to the Git root. Run a root-level Cucumber runner or rebase/filter selectors for the appropriate module before invoking that module's Maven tests.

## Embed the helper

For another framework targeting Java 17+, copy `src/main/java/info/seleniumcucumber/impact/ImpactHelper.java` and add this Maven dependency, or depend on the built artifact. Keep this tool separate in the current repository because its main build targets Java 8:

```xml
<dependency>
    <groupId>com.github.javaparser</groupId>
    <artifactId>javaparser-core</artifactId>
    <version>3.28.0</version>
</dependency>
```

Its public API does not require Cucumber on the analysis classpath:

```java
import info.seleniumcucumber.impact.ImpactHelper;
import java.nio.file.Path;

var result = ImpactHelper.analyze(Path.of("C:/work/my-bdd-project"), "origin/master", "HEAD");
result.writeTo(Path.of("target/impact"));
result.selectors().forEach(System.out::println);
```

Call it before starting the Cucumber test JVM. Running it from a Cucumber hook is too late to control scenario discovery.

## Selection logic

1. Find added, modified, deleted, and renamed `.java` files under `src/main/java` and `src/test/java`, including those directories in Maven modules. Renames retain both old and new paths.
2. Parse sources at both Git revisions using JavaParser's syntax tree API. The target framework does not have to compile or have dependencies available for this parse. Syntax errors stop analysis. The parser accepts the newest syntax supported by the pinned JavaParser version, independently of the JDK running the helper; syntax beyond that version's support produces a visible error.
3. Build a conservative class reference graph and follow dependents transitively. For example, `PriceService -> CheckoutPage -> CheckoutSteps`. Link implementations to their interfaces/base classes so consumers of those abstractions are included.
4. Read `@Given`, `@When`, `@Then`, `@And` and `@But` expressions from affected files in **both** revisions. Any change to a step class includes all its step definitions, rather than attempting method-level precision.
5. Match expressions to tracked `.feature` files at the head revision. Include English feature backgrounds. Select an entire Scenario Outline when it contains placeholders, covering all example rows. Changed feature files are selected even when no Java files changed.
6. Select all features if an affected hook/type transformer/recognized runner configuration is found, an annotation expression cannot be resolved, an affected expression has no current matching step, or any individual changed Java file has no discovered path to glue.

## Scope and accuracy

This is a static **candidate impact list**, not proof that omitted scenarios are unaffected. Review selection against a full regression baseline before using it to gate a PR.

| Case | Behavior |
| --- | --- |
| Literal step text and Java regex annotations | Match the step text |
| Basic Cucumber expressions, e.g. `I buy {int} items` | Parameters match any text; custom parameter types also overmatch |
| Optional text, alternatives or escaped expression syntax | Overmatch all scenario steps |
| Outline step containing `<parameter>` | Overmatch the outline, keeping all example rows |
| English feature Background | Include background steps for each scenario |
| Gherkin `Rule` or non-English `# language:` | Whole feature selected on any Java change |
| Same simple class name in multiple packages/modules | Include all matching declarations; can overselect |
| Tagged hooks | All features, even when a tag could narrow the scope |
| Reflection, dependency injection, generated code, external implementations | Static references may be incomplete; use full-suite mode |
| Java 8 lambda-style Cucumber DSL or localized Java step annotations | No dedicated support; unmapped changes trigger full-suite fallback, but mixed glue may still conceal dependencies |
| XML, properties, JSON, POM, resources or external service changes | Outside Java impact scope; account for these separately in CI |

The Gherkin reader handles common English scenarios, outlines, backgrounds, tables and doc strings. It is not a full Gherkin validator. Run Cucumber's normal validation/discovery too. Java references are matched by simple identifier name, not fully resolved symbols; this favors broader selection but cannot infer every runtime relationship. A full-suite fallback only covers uncertainty the helper actually detects.

For runtime wiring or a cautious initial rollout, select all features whenever Java changes:

```powershell
java -jar target/cucumber-impact-helper-1.0.0.jar "C:/work/my-bdd-project" origin/master HEAD "C:/work/my-bdd-project/target/impact" --full-suite
```

For stronger precision and coverage, extend this with a runtime scenario-to-class coverage map collected from your full regression suite. This version uses static analysis only.

## Verification

```powershell
mvn test
```

The tests create isolated Git repositories and exercise transitive production changes, test step changes, deletion, renames, interface implementations, hooks, unmapped changes, merge-base behavior, dirty working trees, backgrounds, doc strings, outlines, rules, regex annotations, feature-only changes, invalid input and report output.
