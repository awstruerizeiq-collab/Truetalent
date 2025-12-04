
import React, { useState } from "react";
import Editor from "@monaco-editor/react";
import axios from "../../api/axiosConfig";


const DEFAULT_JAVA_CODE = `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int a = sc.nextInt();
        int b = sc.nextInt();
        System.out.println(a + b);
        sc.close();
    }
}`;

const Run = () => {
    
    const [code, setCode] = useState(DEFAULT_JAVA_CODE);
    const [language, setLanguage] = useState("java"); 
    const [testCases, setTestCases] = useState([{ input: "1 2", expectedOutput: "3" }]);
    const [results, setResults] = useState([]);
    const [status, setStatus] = useState("");

    const addTestCase = () => {
        setTestCases([...testCases, { input: "", expectedOutput: "" }]);
    };

    const updateTestCase = (index, field, value) => {
        const newTestCases = [...testCases];
        newTestCases[index][field] = value;
        setTestCases(newTestCases);
    };

    const runCode = async () => {
      
        if (!code.trim()) {
            setStatus("Error ❌: Code cannot be blank.");
            return;
        }
        if (!language) {
            setStatus("Error ❌: Language cannot be blank.");
            return;
        }

        setStatus("Running...");
        try {
            const res = await axios.post("/code/run", {
                source: code,
                language: language.toLowerCase(), 
                testCases,
            });
            setResults(res.data.results);
            setStatus("Finished ✅");
        } catch (err) {
            console.error(err);
            setStatus("Error ❌: " + err.response?.data?.message || err.message);
        }
    };

    return (
        <div className="space-y-4">
            <div className="flex justify-between items-center">
                <select
                    value={language}
                    onChange={(e) => setLanguage(e.target.value)}
                    className="border p-1 rounded"
                >
                    <option value="cpp">C++</option>
                    <option value="java">Java</option>
                    <option value="python">Python</option>
                </select>
                <button
                    onClick={runCode}
                    className="bg-blue-600 text-white px-4 py-2 rounded"
                >
                    Run Code
                </button>
                <button
                    onClick={addTestCase}
                    className="bg-green-600 text-white px-4 py-2 rounded"
                >
                    + Add Test Case
                </button>
            </div>

            <div style={{ height: "400px", border: "1px solid #ddd" }}>
                <Editor
                    language={language}
                    value={code}
                    onChange={(val) => setCode(val || "")}
                />
            </div>

            <div className="space-y-2">
                {testCases.map((tc, index) => (
                    <div key={index} className="flex gap-2 items-center">
                        <input
                            placeholder="Input"
                            value={tc.input}
                            onChange={(e) => updateTestCase(index, "input", e.target.value)}
                            className="border p-1 rounded w-1/2"
                        />
                        <input
                            placeholder="Expected Output"
                            value={tc.expectedOutput}
                            onChange={(e) => updateTestCase(index, "expectedOutput", e.target.value)}
                            className="border p-1 rounded w-1/2"
                        />
                    </div>
                ))}
            </div>

            <div className="space-y-2">
                {results.map((r, index) => (
                    <div
                        key={index}
                        className={`p-2 border rounded ${r.passed ? "bg-green-100" : "bg-red-100"}`}
                    >
                        <p>
                            <strong>Test Case {index + 1}</strong> -{" "}
                            {r.passed ? "✅ Passed" : "❌ Failed"}
                        </p>
                        <p>Input: {r.input}</p>
                        <p>Expected: {r.expected}</p>
                        <p>Output: {r.output}</p>
                        {r.error && <p className="text-red-600">Error: {r.error}</p>}
                    </div>
                ))}
            </div>

            <p>Status: {status}</p>
        </div>
    );
};

export default Run;
