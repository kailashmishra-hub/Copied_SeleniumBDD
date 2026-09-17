selenium-cucumber-java
=================

selenium-cucumber : Automation Testing Using Java

selenium-cucumber is a behavior driven development (BDD) approach to write automation test script to test Web.
It enables you to write and execute automated acceptance/unit tests.
It is cross-platform, open source and free.
Automate your test cases with minimal coding.
[More Details](http://seleniumcucumber.info/)

Documentation
-------------
* [Installation](doc/installation.md)
* [Predefined steps](doc/canned_steps.md)

Download a Framework
--------------
* Maven - https://github.com/selenium-cucumber/selenium-cucumber-java-maven-example

Writing a test
--------------

The cucumber features goes in the `features` library and should have the ".feature" extension.

You can start out by looking at `features/my_first.feature`. You can extend this feature or make your own features using some of the [predefined steps](doc/canned_steps.md) that comes with selenium-cucumber.


Predefined steps
-----------------
By using predefined steps you can automate your test cases more quickly, more efficiently and without much coding.

The predefined steps are located [here](doc/canned_steps.md)

Running test
--------------

Go to your project directory from terminal and hit following commands
* `mvn test (defualt will run on local firefox browser)`
* `mvn test "-Dbrowser=chrome" (to use any other browser)`
* `mvn test "-Dcloud_config=saucelab_windows_chrome52" (to run test on cloud test platforms)`

Using canned tests in your project
----------------------------------

In your TestRunner class add a glue option:

```
package stepDefintions;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(
	plugin = {"html:target/cucumberHtmlReport"},
	features = "classpath:features",
	glue = {"info.seleniumcucumber.stepdefinitions"}
)

public class RunCukeTest {
}
```

Maven/Gradle Dependency
-----------------------

See https://jitpack.io/#selenium-cucumber/selenium-cucumber-java .


PR impact analysis
------------------
The helper lives alongside the framework's existing utility classes:
[`ImpactHelper.java`](src/main/java/info/seleniumcucumber/methods/ImpactHelper.java).
It uses JavaParser, has no `sun.*` or `com.sun.*` imports, and is compatible with
the framework's Java 8 target (including use from Java 17+). Its dependency is
included in the main `pom.xml`; there is no separate module.

Call it before Cucumber starts discovering scenarios:

```java
import info.seleniumcucumber.methods.ImpactHelper;
import java.nio.file.Paths;

ImpactHelper.Result result = ImpactHelper.analyze(Paths.get("."), "origin/master", "HEAD");
result.writeTo(Paths.get("target/impact"));
result.selectors().forEach(System.out::println);
```

First fetch master with `git fetch origin master`. The helper compares the merge
base of the supplied refs to the PR head, using committed files only. It follows
class dependencies from changes under `src/main/java` and `src/test/java` to
Cucumber step definitions, including old/deleted classes and expressions.
Run Cucumber from a checkout matching the analyzed head so line numbers agree.

The output files are `target/impact/impacted-scenarios.txt` (one feature or
feature:line selector per line) and `target/impact/impact-report.txt` (selection
reasons). This repository uses Cucumber 1.2.5, so pass the selectors through its
legacy `cucumber.options` setting. From the repository root in PowerShell:

```powershell
$selectors = @(Get-Content "target/impact/impacted-scenarios.txt" | Where-Object { $_.Trim() })
if ($selectors.Count -gt 0) {
    $quotedSelectors = $selectors | ForEach-Object { "'" + $_ + "'" }
    mvn test "-Dcucumber.options=$($quotedSelectors -join ' ')"
    if ($LASTEXITCODE -ne 0) { throw "Cucumber tests failed" }
} else {
    Write-Host "No impacted scenarios; skipping the Cucumber invocation."
}
```

Single quotes preserve paths containing spaces for Cucumber 1.2.5. Paths with
literal single quotes require another invocation strategy. An empty options
string would leave the runner's default feature selection active, so skip the
invocation explicitly when there are no selectors. The helper examines all
tracked feature files, including `src/test/resources/Homerunner Login.feature`,
which is outside the runner's default `classpath:features` selection.

This is a conservative static estimate, not proof of complete runtime coverage.
English backgrounds are included; outlines containing placeholders are selected
with all their rows. Rules/non-English features, complex expressions, hooks,
unresolved mappings and expressions without matching scenarios can broaden
selection. Most predefined steps share one class, so a change reaching that
class may select much of the suite. Class names are matched by simple identifier;
reflection, dependency injection, resources and external services need separate
coverage. Pass `true` as the fourth `analyze` argument to select all features
whenever Java files change.

Run only the helper's integration tests (without launching browsers):

```powershell
mvn -Dtest=ImpactHelperTest test
```

License
-------

(The MIT License)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the 'Software'), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
