
import React, { useEffect, useState } from "react";
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

    useEffect(() => {
        document.body.classList.remove("admin-dashboard-body", "login-page-body");
        document.body.classList.add("candidate-body");
        return () => {
            document.body.classList.remove("candidate-body");
        };
    }, []);

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
        <div className="min-h-screen px-6 py-6">
            <div className="max-w-6xl mx-auto space-y-4">
            <div className="flex flex-wrap justify-between items-center gap-3 rounded-2xl border border-white/70 bg-white/80 p-4 shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)]">
                <select
                    value={language}
                    onChange={(e) => setLanguage(e.target.value)}
                    className="rounded-xl border border-slate-200 bg-white/90 px-3 py-2 text-sm text-slate-700 shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                    <option value="cpp">C++</option>
                    <option value="java">Java</option>
                    <option value="python">Python</option>
                </select>
                <button
                    onClick={runCode}
                    className="rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700"
                >
                    Run Code
                </button>
                <button
                    onClick={addTestCase}
                    className="rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-700"
                >
                    + Add Test Case
                </button>
            </div>

            <div className="rounded-2xl border border-white/70 bg-white/80 shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)] overflow-hidden">
                <div style={{ height: "400px", borderBottom: "1px solid #e2e8f0" }}>
                    <Editor
                        language={language}
                        value={code}
                        onChange={(val) => setCode(val || "")}
                    />
                </div>
            </div>

            <div className="rounded-2xl border border-white/70 bg-white/80 p-4 shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)] space-y-2">
                {testCases.map((tc, index) => (
                    <div key={index} className="flex gap-2 items-center">
                        <input
                            placeholder="Input"
                            value={tc.input}
                            onChange={(e) => updateTestCase(index, "input", e.target.value)}
                            className="w-1/2 rounded-xl border border-slate-200 bg-white/90 px-3 py-2 text-sm text-slate-700 shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                        <input
                            placeholder="Expected Output"
                            value={tc.expectedOutput}
                            onChange={(e) => updateTestCase(index, "expectedOutput", e.target.value)}
                            className="w-1/2 rounded-xl border border-slate-200 bg-white/90 px-3 py-2 text-sm text-slate-700 shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                ))}
            </div>

            <div className="rounded-2xl border border-white/70 bg-white/80 p-4 shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)] space-y-2">
                {results.map((r, index) => (
                    <div
                        key={index}
                        className={`p-3 border rounded-xl ${r.passed ? "bg-emerald-50 border-emerald-200" : "bg-red-50 border-red-200"}`}
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

            <p className="text-sm text-slate-600">Status: {status}</p>
            </div>
        </div>
    );
};

export default Run;
