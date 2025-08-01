package io.cucumber.core.plugin;

import io.cucumber.core.backend.StubHookDefinition;
import io.cucumber.core.backend.StubStepDefinition;
import io.cucumber.core.feature.TestFeatureParser;
import io.cucumber.core.gherkin.Feature;
import io.cucumber.core.options.RuntimeOptionsBuilder;
import io.cucumber.core.plugin.ProgressFormatter.Ansi;
import io.cucumber.core.runtime.Runtime;
import io.cucumber.core.runtime.StubBackendSupplier;
import io.cucumber.core.runtime.StubFeatureSupplier;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static io.cucumber.core.plugin.Bytes.bytes;
import static io.cucumber.core.plugin.IsEqualCompressingLineSeparators.equalCompressingLineSeparators;
import static io.cucumber.core.plugin.ProgressFormatter.Ansi.Attributes.FOREGROUND_CYAN;
import static io.cucumber.core.plugin.ProgressFormatter.Ansi.Attributes.FOREGROUND_DEFAULT;
import static io.cucumber.core.plugin.ProgressFormatter.Ansi.Attributes.FOREGROUND_GREEN;
import static io.cucumber.core.plugin.ProgressFormatter.Ansi.Attributes.FOREGROUND_RED;
import static io.cucumber.core.plugin.ProgressFormatter.Ansi.Attributes.FOREGROUND_YELLOW;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;

class ProgressFormatterTest {

    private final Ansi GREEN = Ansi.with(FOREGROUND_GREEN);
    private final Ansi YELLOW = Ansi.with(FOREGROUND_YELLOW);
    private final Ansi RED = Ansi.with(FOREGROUND_RED);
    private final Ansi RESET = Ansi.with(FOREGROUND_DEFAULT);
    private final Ansi CYAN = Ansi.with(FOREGROUND_CYAN);

    @Test
    void prints_empty_line_for_empty_test_run() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Runtime.builder()
                .withFeatureSupplier(new StubFeatureSupplier())
                .withAdditionalPlugins(new ProgressFormatter(out))
                .withBackendSupplier(new StubBackendSupplier(
                    new StubStepDefinition("passed step")))
                .build()
                .run();

        assertThat(out, bytes(equalCompressingLineSeparators("\n")));
    }

    @Test
    void prints_green_dot_for_passed_step() {
        Feature feature = TestFeatureParser.parse("classpath:path/test.feature", "" +
                "Feature: feature name\n" +
                "  Scenario: passed scenario\n" +
                "    Given passed step\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Runtime.builder()
                .withFeatureSupplier(new StubFeatureSupplier(feature))
                .withAdditionalPlugins(new ProgressFormatter(out))
                .withBackendSupplier(new StubBackendSupplier(
                    new StubStepDefinition("passed step")))
                .build()
                .run();

        assertThat(out, bytes(equalCompressingLineSeparators(GREEN + "." + RESET + "\n")));
    }

    @Test
    void prints_dot_for_passed_step_without_color() {
        Feature feature = TestFeatureParser.parse("classpath:path/test.feature", "" +
                "Feature: feature name\n" +
                "  Scenario: passed scenario\n" +
                "    Given passed step\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Runtime.builder()
                .withRuntimeOptions(new RuntimeOptionsBuilder().setMonochrome().build())
                .withFeatureSupplier(new StubFeatureSupplier(feature))
                .withAdditionalPlugins(new ProgressFormatter(out))
                .withBackendSupplier(new StubBackendSupplier(
                    new StubStepDefinition("passed step")))
                .build()
                .run();

        assertThat(out, bytes(equalCompressingLineSeparators(".\n")));
    }

    @Test
    void prints_at_most_80_characters_per_line() {
        Feature[] features = new Feature[81];
        Arrays.fill(features,
            TestFeatureParser.parse("classpath:path/test.feature", "" +
                    "Feature: feature name\n" +
                    "  Scenario: passed scenario\n" +
                    "    Given passed step\n"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Runtime.builder()
                .withFeatureSupplier(new StubFeatureSupplier(features))
                .withAdditionalPlugins(new ProgressFormatter(out))
                .withBackendSupplier(new StubBackendSupplier(
                    new StubStepDefinition("passed step")))
                .build()
                .run();

        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            expected.append(GREEN).append(".").append(RESET);
        }
        expected.append("\n");
        expected.append(GREEN).append(".").append(RESET);
        expected.append("\n");

        assertThat(out, bytes(equalCompressingLineSeparators(expected.toString())));
    }

    @Test
    void print_yellow_U_for_undefined_step() {
        Feature feature = TestFeatureParser.parse("classpath:path/test.feature", "" +
                "Feature: feature name\n" +
                "  Scenario: undefined scenario\n" +
                "    Given undefined step\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Runtime.builder()
                .withFeatureSupplier(new StubFeatureSupplier(feature))
                .withAdditionalPlugins(new ProgressFormatter(out))
                .withBackendSupplier(new StubBackendSupplier())
                .build()
                .run();

        assertThat(out, bytes(equalCompressingLineSeparators(YELLOW + "U" + RESET + "\n")));
    }

    @Test
    void prints_green_dot_for_passed_hook() {
        Feature feature = TestFeatureParser.parse("classpath:path/test.feature", "" +
                "Feature: feature name\n" +
                "  Scenario: passed scenario\n" +
                "    Given passed step\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Runtime.builder()
                .withFeatureSupplier(new StubFeatureSupplier(feature))
                .withAdditionalPlugins(new ProgressFormatter(out))
                .withBackendSupplier(new StubBackendSupplier(
                    singletonList(new StubHookDefinition()),
                    singletonList(new StubStepDefinition("passed step")),
                    emptyList()))
                .build()
                .run();

        assertThat(out, bytes(equalCompressingLineSeparators(GREEN + "." + RESET + GREEN + "." + RESET + "\n")));
    }

    @Test
    void print_red_F_for_failed_step() {

        Feature feature = TestFeatureParser.parse("classpath:path/test.feature", "" +
                "Feature: feature name\n" +
                "  Scenario: failing scenario\n" +
                "    Given failed step\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Runtime.builder()
                .withFeatureSupplier(new StubFeatureSupplier(feature))
                .withAdditionalPlugins(new ProgressFormatter(out))
                .withBackendSupplier(new StubBackendSupplier(
                    new StubStepDefinition("failed step", new Exception("Boom"))))
                .build()
                .run();

        assertThat(out, bytes(equalCompressingLineSeparators(RED + "F" + RESET + "\n")));
    }

    @Test
    void print_red_F_for_failed_hook() {
        Feature feature = TestFeatureParser.parse("classpath:path/test.feature", "" +
                "Feature: feature name\n" +
                "  Scenario: failed hook\n" +
                "    Given passed step\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Runtime.builder()
                .withFeatureSupplier(new StubFeatureSupplier(feature))
                .withAdditionalPlugins(new ProgressFormatter(out))
                .withBackendSupplier(new StubBackendSupplier(
                    singletonList(new StubHookDefinition(new RuntimeException("Boom"))),
                    singletonList(new StubStepDefinition("passed step")),
                    emptyList()))
                .build()
                .run();

        assertThat(out, bytes(
            equalCompressingLineSeparators(RED + "F" + RESET + CYAN + "-" + RESET + "\n")));
    }

}
