package info.seleniumcucumber.methods;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Class-level, conservative static impact analysis for annotation-based Cucumber Java. */
public final class ImpactHelper {
    private static final Set<String> STEPS = set("Given", "When", "Then", "And", "But");
    private static final Set<String> GLOBAL = set("Before", "After", "BeforeStep", "AfterStep",
            "BeforeAll", "AfterAll", "ParameterType", "DataTableType", "DefaultParameterTransformer",
            "DefaultDataTableEntryTransformer", "DefaultDataTableCellTransformer", "DocStringType",
            "CucumberOptions", "ConfigurationParameter", "ConfigurationParameters");

    public static final class Result {
        private final String baseCommit;
        private final String headCommit;
        private final Set<String> changedJavaFiles;
        private final Set<String> affectedJavaFiles;
        private final List<String> selectors;
        private final List<String> reasons;
        public Result(String baseCommit, String headCommit, Set<String> changedJavaFiles, Set<String> affectedJavaFiles, List<String> selectors, List<String> reasons) {
            this.baseCommit = baseCommit;
            this.headCommit = headCommit;
            this.changedJavaFiles = Collections.unmodifiableSet(new TreeSet<>(changedJavaFiles));
            this.affectedJavaFiles = Collections.unmodifiableSet(new TreeSet<>(affectedJavaFiles));
            this.selectors = Collections.unmodifiableList(new ArrayList<>(selectors));
            this.reasons = Collections.unmodifiableList(new ArrayList<>(reasons));
        }
        public String baseCommit() { return baseCommit; }
        public String headCommit() { return headCommit; }
        public Set<String> changedJavaFiles() { return changedJavaFiles; }
        public Set<String> affectedJavaFiles() { return affectedJavaFiles; }
        public List<String> selectors() { return selectors; }
        public List<String> reasons() { return reasons; }
        public void writeTo(Path directory) throws IOException {
            Files.createDirectories(directory);
            Files.write(directory.resolve("impacted-scenarios.txt"), selectors, StandardCharsets.UTF_8);
            String report = "Base: " + baseCommit + "\nHead: " + headCommit
                    + "\n\nChanged Java files:\n" + String.join("\n", changedJavaFiles)
                    + "\n\nAffected Java files (transitive):\n" + String.join("\n", affectedJavaFiles)
                    + "\n\nSelection reasons:\n" + String.join("\n", reasons)
                    + "\n\nCucumber selectors:\n" + String.join("\n", selectors) + "\n";
            Files.write(directory.resolve("impact-report.txt"), report.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static final class Source {
        final Set<String> names = new HashSet<>(), references = new HashSet<>(), parents = new HashSet<>();
        final List<String> expressions = new ArrayList<>();
        boolean global, unknownExpression;
    }
    private static final class Scenario {
        final int line;
        final List<String> steps;
        Scenario(int line, List<String> steps) { this.line = line; this.steps = steps; }
    }
    private static final class Feature {
        final List<Scenario> scenarios;
        final boolean unsupported;
        Feature(List<Scenario> scenarios, boolean unsupported) {
            this.scenarios = scenarios; this.unsupported = unsupported;
        }
    }

    @SafeVarargs
    private static <T> Set<T> set(T... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private static byte[] readBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = stream.read(buffer)) != -1) output.write(buffer, 0, length);
        return output.toByteArray();
    }

    /** Analyze committed snapshots. Checkout headRef before executing the returned selectors. */
    public static Result analyze(Path repository, String baseRef, String headRef) throws IOException {
        return analyze(repository, baseRef, headRef, false);
    }

    /** forceAllOnJavaChange is useful for repositories with reflection or runtime dependency wiring. */
    public static Result analyze(Path repository, String baseRef, String headRef,
                                 boolean forceAllOnJavaChange) throws IOException {
        Path root = repository.toAbsolutePath().normalize();
        String baseTip = git(root, "rev-parse", "--verify", "--end-of-options", baseRef + "^{commit}").trim();
        String head = git(root, "rev-parse", "--verify", "--end-of-options", headRef + "^{commit}").trim();
        String base = git(root, "merge-base", baseTip, head).trim();
        // --no-renames deliberately represents renames as deletion + addition, retaining both paths.
        Set<String> changed = splitNull(git(root, "diff", "--name-only", "-z", "--no-renames", base, head, "--"));
        Set<String> javaChanges = changed.stream().filter(ImpactHelper::javaPath)
                .collect(Collectors.toCollection(TreeSet::new));
        Map<String, String> before = snapshot(root, base), after = snapshot(root, head);
        Map<String, Source> oldSources = parseSources(before), newSources = parseSources(after);
        Map<String, Set<String>> reverse = new HashMap<>();
        addGraph(oldSources, reverse);
        addGraph(newSources, reverse);
        Set<String> affected = closure(javaChanges, reverse);
        List<String> reasons = new ArrayList<>();
        List<String> expressions = new ArrayList<>();
        boolean all = forceAllOnJavaChange && !javaChanges.isEmpty();
        if (all) reasons.add("Full-suite mode: Java files changed.");
        for (String path : affected) {
            for (Map<String, Source> revision : Arrays.asList(oldSources, newSources)) {
                Source source = revision.get(path);
                if (source == null) continue;
                expressions.addAll(source.expressions);
                if (source.global || source.unknownExpression) {
                    all = true;
                    reasons.add("Shared hook/configuration or unresolved step expression: " + path);
                }
            }
        }
        // Each changed file must independently reach glue; another mapped file must not mask it.
        for (String changedPath : javaChanges) {
            boolean mapped = closure(set(changedPath), reverse).stream().anyMatch(path ->
                    hasGlue(oldSources.get(path)) || hasGlue(newSources.get(path)));
            if (!mapped) {
                all = true;
                reasons.add("No static path to Cucumber glue for " + changedPath + "; selecting all features.");
            }
        }
        Map<String, Feature> features = new TreeMap<>();
        after.forEach((path, text) -> { if (path.endsWith(".feature")) features.put(path, parseFeature(text)); });
        // If an affected expression has no current matching step, avoid returning a misleading empty set.
        for (String expression : new LinkedHashSet<>(expressions)) {
            boolean found = features.values().stream().flatMap(f -> f.scenarios.stream())
                    .flatMap(s -> s.steps.stream()).anyMatch(step -> matches(expression, step));
            if (!found) {
                all = true;
                reasons.add("Affected expression has no current matching step: " + expression + "; selecting all features.");
            }
        }
        List<String> selectors = new ArrayList<>();
        for (Map.Entry<String, Feature> entry : features.entrySet()) {
            String path = entry.getKey();
            Feature feature = entry.getValue();
            if (all || changed.contains(path) || (!javaChanges.isEmpty() && feature.unsupported)) {
                selectors.add(path);
                if (changed.contains(path)) reasons.add("Changed feature: " + path);
                if (feature.unsupported && !javaChanges.isEmpty()) reasons.add("Unsupported Gherkin syntax; whole feature: " + path);
            } else if (!javaChanges.isEmpty()) {
                for (Scenario scenario : feature.scenarios) {
                    if (scenario.steps.stream().anyMatch(step -> expressions.stream().anyMatch(e -> matches(e, step)))) {
                        String selector = path + ":" + scenario.line;
                        selectors.add(selector);
                        reasons.add("Affected class contains a step matching " + selector);
                    }
                }
            }
        }
        if (features.isEmpty() && !javaChanges.isEmpty())
            throw new IOException("No tracked .feature files found at head; cannot derive scenarios.");
        reasons.add("Static class-level estimate. Reflection, external services, resources and runtime wiring may require a full suite.");
        return new Result(base, head, javaChanges, affected, selectors, reasons.stream().distinct().collect(Collectors.toList()));
    }

    private static boolean hasGlue(Source s) {
        return s != null && (s.global || s.unknownExpression || !s.expressions.isEmpty());
    }

    private static boolean javaPath(String path) {
        return path.endsWith(".java") && (path.startsWith("src/main/java/") || path.startsWith("src/test/java/")
                || path.contains("/src/main/java/") || path.contains("/src/test/java/"));
    }

    private static Map<String, String> snapshot(Path root, String revision) throws IOException {
        Map<String, String> result = new TreeMap<>();
        for (String path : splitNull(git(root, "ls-tree", "-r", "--name-only", "-z", revision))) {
            if (javaPath(path) || path.endsWith(".feature")) result.put(path, git(root, "show", revision + ":" + path));
        }
        return result;
    }

    private static Set<String> splitNull(String value) {
        return Arrays.stream(value.split("\u0000")).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static String git(Path root, String... arguments) throws IOException {
        List<String> command = new ArrayList<>(Arrays.asList("git", "-C", root.toString()));
        command.addAll(Arrays.asList(arguments));
        Process process = new ProcessBuilder(command).start();
        // Drain stderr independently: warnings must not contaminate NUL-delimited filenames or source.
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> {
            try (InputStream stream = process.getErrorStream()) { errors.write(readBytes(stream)); }
            catch (IOException ignored) { /* Nonzero exit still propagates below. */ }
        }, "impact-git-stderr");
        reader.start();
        try {
            byte[] output = readBytes(process.getInputStream());
            int status = process.waitFor();
            reader.join();
            if (status != 0) throw new IOException("Git failed (" + String.join(" ", arguments) + "): "
                    + new String(errors.toByteArray(), StandardCharsets.UTF_8));
            return new String(output, StandardCharsets.UTF_8);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while reading Git", e);
        } finally { process.getInputStream().close(); }
    }

    private static Map<String, Source> parseSources(Map<String, String> files) throws IOException {
        JavaParser parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
        Map<String, Source> result = new TreeMap<>();
        for (Map.Entry<String, String> entry : files.entrySet()) {
            if (!javaPath(entry.getKey())) continue;
            com.github.javaparser.ParseResult<com.github.javaparser.ast.CompilationUnit> parsed = parser.parse(entry.getValue());
            if (!parsed.isSuccessful() || !parsed.getResult().isPresent()) {
                throw new IOException("Cannot parse " + entry.getKey() + ": " + parsed.getProblems());
            }
            Source info = new Source();
            parsed.getResult().get().walk(node -> {
                if (node instanceof TypeDeclaration<?>) {
                    TypeDeclaration<?> type = (TypeDeclaration<?>) node;
                    info.names.add(type.getNameAsString());
                }
                // Include qualified names (imports, annotations) and all simple identifiers.
                if (node instanceof SimpleName) {
                    SimpleName name = (SimpleName) node;
                    info.references.add(name.asString());
                }
                if (node instanceof Name) {
                    Name name = (Name) node;
                    info.references.add(name.getIdentifier());
                }
                if (node instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) node;
                    type.getExtendedTypes().forEach(parent -> addParent(parent, info));
                    type.getImplementedTypes().forEach(parent -> addParent(parent, info));
                }
                if (node instanceof EnumDeclaration) {
                    EnumDeclaration type = (EnumDeclaration) node;
                    type.getImplementedTypes().forEach(parent -> addParent(parent, info));
                }
                if (node instanceof RecordDeclaration) {
                    RecordDeclaration type = (RecordDeclaration) node;
                    type.getImplementedTypes().forEach(parent -> addParent(parent, info));
                }
                if (node instanceof AnnotationExpr) {
                    AnnotationExpr annotation = (AnnotationExpr) node;
                    String name = annotation.getName().getIdentifier();
                    if (GLOBAL.contains(name)) info.global = true;
                    if (STEPS.contains(name)) {
                        Expression argument = null;
                        if (annotation instanceof SingleMemberAnnotationExpr) {
                            SingleMemberAnnotationExpr single = (SingleMemberAnnotationExpr) annotation;
                            argument = single.getMemberValue();
                        } else if (annotation instanceof NormalAnnotationExpr) {
                            NormalAnnotationExpr normal = (NormalAnnotationExpr) annotation;
                            argument = normal.getPairs().stream()
                                    .filter(pair -> pair.getNameAsString().equals("value"))
                                    .map(pair -> pair.getValue()).findFirst().orElse(null);
                        }
                        String literal = constantString(argument);
                        if (literal != null) info.expressions.add(literal);
                        else info.unknownExpression = true;
                    }
                }
            });
            result.put(entry.getKey(), info);
        }
        return result;
    }

    private static void addParent(ClassOrInterfaceType parent, Source info) {
        parent.findAll(ClassOrInterfaceType.class).forEach(type -> info.parents.add(type.getNameAsString()));
    }

    private static String constantString(Expression expression) {
        if (expression instanceof StringLiteralExpr) return ((StringLiteralExpr) expression).asString();
        if (expression instanceof TextBlockLiteralExpr) return ((TextBlockLiteralExpr) expression).asString();
        if (expression instanceof EnclosedExpr) return constantString(((EnclosedExpr) expression).getInner());
        if (expression instanceof BinaryExpr && ((BinaryExpr) expression).getOperator() == BinaryExpr.Operator.PLUS) {
            BinaryExpr binary = (BinaryExpr) expression;
            String left = constantString(binary.getLeft()), right = constantString(binary.getRight());
            if (left != null && right != null) return left + right;
        }
        return null;
    }

    private static void addGraph(Map<String, Source> sources, Map<String, Set<String>> reverse) {
        Map<String, Set<String>> declarations = new HashMap<>();
        sources.forEach((path, source) -> source.names.forEach(name ->
                declarations.computeIfAbsent(name, k -> new HashSet<>()).add(path)));
        sources.forEach((path, source) -> {
            for (String reference : source.references) {
                for (String dependency : declarations.getOrDefault(reference, set()))
                    reverse.computeIfAbsent(dependency, k -> new HashSet<>()).add(path);
            }
            // Include interface/base-class consumers when an implementation/subclass changes.
            for (String parent : source.parents) {
                for (String declaration : declarations.getOrDefault(parent, set()))
                    reverse.computeIfAbsent(path, k -> new HashSet<>()).add(declaration);
            }
        });
    }

    private static Set<String> closure(Set<String> seeds, Map<String, Set<String>> reverse) {
        Set<String> result = new TreeSet<>(seeds);
        Deque<String> queue = new ArrayDeque<>(seeds);
        while (!queue.isEmpty()) {
            for (String user : reverse.getOrDefault(queue.removeFirst(), set()))
                if (result.add(user)) queue.addLast(user);
        }
        return result;
    }

    private static Feature parseFeature(String text) {
        List<Scenario> scenarios = new ArrayList<>();
        List<String> background = new ArrayList<>();
        List<String> active = null;
        boolean unsupported = false;
        String docDelimiter = null;
        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (docDelimiter != null) {
                if (line.equals(docDelimiter)) docDelimiter = null;
                continue;
            }
            if (line.startsWith("\"\"\"") || line.startsWith("```")) {
                docDelimiter = line.substring(0, 3); continue;
            }
            if (line.startsWith("# language:") && !line.substring(11).trim().equals("en")) unsupported = true;
            if (line.startsWith("Rule:")) unsupported = true; // Select whole feature; rule backgrounds have their own scope.
            if (line.startsWith("Background:")) { active = background; continue; }
            if (line.matches("(?:Scenario Outline|Scenario Template|Scenario|Example):.*")) {
                active = new ArrayList<>(background);
                scenarios.add(new Scenario(i + 1, active));
                continue;
            }
            java.util.regex.Matcher matcher = Pattern.compile("^(?:Given|When|Then|And|But|\\*)\\s+(.*)$").matcher(line);
            if (matcher.matches() && active != null) active.add(matcher.group(1));
        }
        if (scenarios.isEmpty() || docDelimiter != null) unsupported = true;
        return new Feature(scenarios, unsupported);
    }

    /** Deliberately overmatches custom parameters, optional/alternative syntax and outline placeholders. */
    private static boolean matches(String expression, String step) {
        if (step.matches(".*<[^>]+>.*")) return true; // Select the entire outline (all example rows).
        if (expression.startsWith("^") || expression.endsWith("$") ||
                (expression.startsWith("/") && expression.endsWith("/"))) {
            String regex = expression.startsWith("/") && expression.endsWith("/")
                    ? expression.substring(1, expression.length() - 1) : expression;
            try { return Pattern.compile(regex).matcher(step).matches(); }
            catch (RuntimeException invalidRegex) { return true; }
        }
        // Complex Cucumber expressions are kept broad rather than emulating the complete grammar.
        if (expression.matches(".*[()/\\\\].*")) return true;
        StringBuilder regex = new StringBuilder("^");
        int start = 0;
        while (start < expression.length()) {
            int open = expression.indexOf('{', start);
            if (open < 0) { regex.append(Pattern.quote(expression.substring(start))); break; }
            int close = expression.indexOf('}', open);
            if (close < 0) return true;
            regex.append(Pattern.quote(expression.substring(start, open))).append(".*");
            start = close + 1;
        }
        return Pattern.compile(regex.append('$').toString(), Pattern.DOTALL).matcher(step).matches();
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 5 || (args.length == 5 && !args[4].equals("--full-suite"))) {
            throw new IllegalArgumentException("Usage: ImpactHelper [repo] [base=master] [head=HEAD] [output=target/impact] [--full-suite]");
        }
        Path repo = Paths.get(args.length > 0 ? args[0] : ".");
        Path output = args.length > 3 ? Paths.get(args[3]) : repo.resolve("target/impact");
        Result result = analyze(repo, args.length > 1 ? args[1] : "master", args.length > 2 ? args[2] : "HEAD", args.length == 5);
        result.writeTo(output);
        System.out.println("Selected " + result.selectors.size() + " scenario/feature selectors. Report: "
                + output.toAbsolutePath().resolve("impact-report.txt"));
        if (result.selectors.isEmpty()) System.out.println("No impacted scenarios found. Skip the Cucumber invocation; do not pass an empty filter.");
    }
}
