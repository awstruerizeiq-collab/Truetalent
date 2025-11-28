package com.truerize.dto;
import java.util.List;

public class RunMultipleResponseDTO {

    private List<TestCaseResult> results;

    public static class TestCaseResult {
        private String input;
        private String expected;
        private String output;
        private boolean passed;
        private String error;

        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }

        public String getExpected() { return expected; }
        public void setExpected(String expected) { this.expected = expected; }

        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }

        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public List<TestCaseResult> getResults() { return results; }
    public void setResults(List<TestCaseResult> results) { this.results = results; }
}
