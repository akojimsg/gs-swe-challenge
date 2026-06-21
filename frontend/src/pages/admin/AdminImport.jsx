/*
 * PAGE: Admin CSV Import  (the feature's centerpiece — per-row error report)
 * ---------------------------------------------------------------------------
 * Route:  /admin/import  ·  Access: Admin · Scope: core  ·  Shell: AdminShell
 * Spec:   gse-requirement-docs/frontend-design/specs/admin-csv-import.md
 *
 * API: startImport(file) → 202 { importId } (multipart, ≤10MB MIME guard) ·
 *      getImport(id) → { status(PROCESSING|COMPLETED), totalRows, imported,
 *      updated, skipped, errors[{ row, field, value, reason }], durationMs }.
 * STATES: idle · file chosen · rejected (>10MB/wrong type) · uploading ·
 *      processing (poll) · completed (clean / with errors) · error.
 * THE POINT: render the per-row error table — the deliberate sample-file errors
 *      (XSS stripped, "free"/negative skipped, dup-SKU updated, whitespace name)
 *      from gse-requirement-docs/Code Challenge E-Commerce.csv.
 * BUILD NOTE (MCP): CsvUploadDropzone + ImportJobStatus + CsvImportResultTable.
 * ---------------------------------------------------------------------------
 */
import { useRef, useState } from "react";
import { startImport, getImport } from "@/api/products";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { MAX_CSV_BYTES } from "@/lib/constants";

export default function AdminImport() {
  const [file, setFile] = useState(null);
  const [job, setJob] = useState(null);
  const [phase, setPhase] = useState("idle"); // idle|rejected|uploading|processing|done|error
  const [msg, setMsg] = useState(null);
  const pollRef = useRef(null);

  const onPick = (e) => {
    const f = e.target.files?.[0];
    if (!f) return;
    if (f.size > MAX_CSV_BYTES) return (setPhase("rejected"), setMsg("File exceeds 10MB."));
    if (!/\.csv$/i.test(f.name) && f.type !== "text/csv")
      return (setPhase("rejected"), setMsg("Please choose a .csv file."));
    setFile(f);
    setPhase("idle");
    setMsg(null);
  };

  const poll = (importId) => {
    pollRef.current = setInterval(async () => {
      try {
        const j = await getImport(importId);
        setJob(j);
        if (j.status === "COMPLETED") {
          clearInterval(pollRef.current);
          setPhase("done");
        }
      } catch {
        clearInterval(pollRef.current);
        setPhase("error");
      }
    }, 1500);
  };

  const onUpload = async () => {
    if (!file) return;
    setPhase("uploading");
    try {
      const { importId } = await startImport(file);
      setPhase("processing");
      poll(importId);
    } catch {
      setPhase("error");
      setMsg("Upload failed.");
    }
  };

  // Minimal proof-of-wiring render — MCP agent implements the dropzone + result table.
  return (
    <div className="max-w-2xl">
      <h1 className="mb-4 font-display text-xl font-extrabold">CSV Import</h1>

      <div className="rounded-lg border border-border p-4">
        <input type="file" accept=".csv,text/csv" onChange={onPick} aria-label="CSV file" />
        {msg && <p className="mt-2 text-sm text-danger">{msg}</p>}
        <Button className="mt-3" onClick={onUpload} disabled={!file || phase === "uploading" || phase === "processing"}>
          {phase === "uploading" ? "Uploading…" : phase === "processing" ? "Importing…" : "Import"}
        </Button>
      </div>

      {job && (
        <div className="mt-6">
          <div className="flex gap-3 text-sm">
            <Badge tone="success">Imported {job.imported}</Badge>
            <Badge tone="info">Updated {job.updated}</Badge>
            <Badge tone="warning">Skipped {job.skipped}</Badge>
          </div>
          {job.errors?.length > 0 && (
            <table className="mt-4 w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr><th className="py-1">Row</th><th>Field</th><th>Value</th><th>Reason</th></tr>
              </thead>
              <tbody>
                {job.errors.map((e, idx) => (
                  <tr key={idx} className="border-t border-border">
                    <td className="py-1">{e.row}</td>
                    <td>{e.field}</td>
                    <td className="max-w-[12rem] truncate">{e.value}</td>
                    <td>{e.reason}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
