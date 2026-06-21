import { useRef, useState } from "react";
import { Link } from "react-router-dom";
import { startImport, getImport } from "@/api/products";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { useToast } from "@/components/common/Toast";
import { MAX_CSV_BYTES } from "@/lib/constants";
import { Upload, FileText, CheckCircle2, XCircle, Loader2, ChevronLeft, AlertTriangle } from "lucide-react";

const POLL_MS = 2000;
const MAX_POLLS = 60;

function StatusBadge({ status }) {
  const tone = status === "COMPLETED" ? "success" : status === "FAILED" ? "danger" : "warning";
  return <Badge tone={tone}>{status}</Badge>;
}

function ErrorTable({ errors }) {
  if (!errors?.length) return null;
  return (
    <div className="mt-4">
      <p className="mb-2 flex items-center gap-1.5 text-sm font-medium text-danger">
        <AlertTriangle className="h-4 w-4" /> {errors.length} row error{errors.length !== 1 ? "s" : ""}
      </p>
      <div className="overflow-x-auto rounded-lg border border-border">
        <table className="w-full text-xs">
          <thead className="border-b border-border bg-muted/50">
            <tr>
              <th className="px-3 py-2 text-left font-medium">Row</th>
              <th className="px-3 py-2 text-left font-medium">Field</th>
              <th className="px-3 py-2 text-left font-medium">Value</th>
              <th className="px-3 py-2 text-left font-medium">Reason</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {errors.map((e, i) => (
              <tr key={i} className="hover:bg-muted/30">
                <td className="px-3 py-2">{e.row}</td>
                <td className="px-3 py-2 font-mono">{e.field ?? "—"}</td>
                <td className="px-3 py-2 font-mono max-w-[120px] truncate" title={e.value}>{e.value ?? "—"}</td>
                <td className="px-3 py-2 text-muted-foreground">{e.reason}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default function AdminImport() {
  const fileRef = useRef(null);
  const pollRef = useRef(null);
  const toast = useToast();

  const [file, setFile] = useState(null);
  const [fileError, setFileError] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [job, setJob] = useState(null);
  const [polling, setPolling] = useState(false);

  const pickFile = (f) => {
    setFileError(null);
    if (!f) { setFile(null); return; }
    if (!f.name.endsWith(".csv")) { setFileError("Only .csv files are accepted."); setFile(null); return; }
    if (f.size > MAX_CSV_BYTES) { setFileError(`File exceeds 10 MB limit.`); setFile(null); return; }
    setFile(f);
  };

  const onDrop = (e) => {
    e.preventDefault();
    pickFile(e.dataTransfer.files[0]);
  };

  const startPolling = (importId, attempt = 0) => {
    if (attempt >= MAX_POLLS) { setPolling(false); return; }
    pollRef.current = setTimeout(async () => {
      try {
        const j = await getImport(importId);
        setJob(j);
        if (j.status === "PROCESSING") {
          startPolling(importId, attempt + 1);
        } else {
          setPolling(false);
          if (j.status === "COMPLETED") {
            toast?.(`Import complete — ${j.imported} imported, ${j.updated} updated, ${j.skipped} skipped.`);
          }
        }
      } catch {
        startPolling(importId, attempt + 1);
      }
    }, POLL_MS);
  };

  const handleUpload = async () => {
    if (!file) return;
    setUploading(true);
    setJob(null);
    try {
      const { importId } = await startImport(file);
      setJob({ importId, status: "PROCESSING" });
      setPolling(true);
      setFile(null);
      if (fileRef.current) fileRef.current.value = "";
      startPolling(importId);
    } catch (err) {
      toast?.(err?.body?.message ?? "Upload failed.", "error");
    } finally {
      setUploading(false);
    }
  };

  const isDone = job && job.status !== "PROCESSING";

  return (
    <div className="mx-auto max-w-xl">
      <div className="mb-6 flex items-center gap-2">
        <Button asChild variant="ghost" size="icon">
          <Link to="/admin/products" aria-label="Back">
            <ChevronLeft className="h-5 w-5" />
          </Link>
        </Button>
        <div>
          <h1 className="text-xl font-bold">CSV Import</h1>
          <p className="text-sm text-muted-foreground">
            Bulk import or update products from a CSV file (max 10 MB).
          </p>
        </div>
      </div>

      {/* drop zone */}
      <div
        onDrop={onDrop}
        onDragOver={(e) => e.preventDefault()}
        onClick={() => fileRef.current?.click()}
        className="flex cursor-pointer flex-col items-center gap-3 rounded-xl border-2 border-dashed border-border bg-muted/30 px-6 py-10 text-center transition hover:border-brand hover:bg-muted/50"
        role="button"
        aria-label="Drop CSV or click to browse"
      >
        {file ? (
          <>
            <FileText className="h-10 w-10 text-brand" />
            <p className="text-sm font-medium">{file.name}</p>
            <p className="text-xs text-muted-foreground">{(file.size / 1024).toFixed(1)} KB</p>
          </>
        ) : (
          <>
            <Upload className="h-10 w-10 text-muted-foreground" />
            <p className="text-sm font-medium">Drop a CSV here, or click to browse</p>
            <p className="text-xs text-muted-foreground">Upserts by SKU · Max 10 MB · Row errors don't stop the batch</p>
          </>
        )}
        <input
          ref={fileRef}
          type="file"
          accept=".csv"
          className="sr-only"
          onChange={(e) => pickFile(e.target.files[0])}
        />
      </div>

      {fileError && (
        <p className="mt-2 text-sm text-danger">{fileError}</p>
      )}

      <Button
        onClick={handleUpload}
        disabled={!file || uploading || polling}
        className="mt-4 w-full gap-2"
        size="lg"
      >
        {uploading ? <><Loader2 className="h-4 w-4 animate-spin" /> Uploading…</> : "Start import"}
      </Button>

      {/* job status */}
      {job && (
        <div className="mt-6 rounded-xl border border-border bg-card p-5">
          <div className="mb-3 flex items-center justify-between">
            <div className="flex items-center gap-2">
              {polling && <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />}
              {isDone && job.status === "COMPLETED" && <CheckCircle2 className="h-4 w-4 text-success" />}
              {isDone && job.status !== "COMPLETED" && <XCircle className="h-4 w-4 text-danger" />}
              <span className="text-sm font-medium">Import job</span>
            </div>
            <StatusBadge status={job.status} />
          </div>

          {job.importId && (
            <p className="mb-2 font-mono text-xs text-muted-foreground">ID: {job.importId}</p>
          )}

          {isDone && (
            <div className="grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
              {[
                { label: "Total rows", value: job.totalRows ?? "—" },
                { label: "Imported", value: job.imported ?? "—", tone: "success" },
                { label: "Updated", value: job.updated ?? "—", tone: "info" },
                { label: "Skipped", value: job.skipped ?? "—", tone: "neutral" },
              ].map(({ label, value, tone }) => (
                <div key={label} className="rounded-lg border border-border bg-muted/30 p-3 text-center">
                  <p className={`text-lg font-bold ${
                    tone === "success" ? "text-success"
                    : tone === "info" ? "text-info"
                    : tone === "neutral" ? "text-muted-foreground"
                    : ""
                  }`}>{value}</p>
                  <p className="text-xs text-muted-foreground">{label}</p>
                </div>
              ))}
            </div>
          )}

          {isDone && job.durationMs && (
            <p className="mt-2 text-xs text-muted-foreground">
              Completed in {(job.durationMs / 1000).toFixed(1)}s
            </p>
          )}

          <ErrorTable errors={job.errors} />
        </div>
      )}

      {/* CSV spec */}
      <details className="mt-6">
        <summary className="cursor-pointer text-sm font-medium text-muted-foreground hover:text-foreground">
          CSV format reference
        </summary>
        <div className="mt-3 rounded-lg border border-border bg-muted/30 p-4 text-xs">
          <p className="mb-2 font-medium">Required columns: <code>name</code>, <code>sku</code>, <code>price</code></p>
          <p className="mb-2 text-muted-foreground">Optional: <code>description</code>, <code>category</code>, <code>stock</code>, <code>weight_kg</code>, <code>active</code></p>
          <ul className="list-disc space-y-1 pl-4 text-muted-foreground">
            <li>Duplicate SKU = update (upsert); never rejected</li>
            <li>Price accepts "$29.99" or "29.99"; "free" or "0" = $0</li>
            <li>Negative stock → skipped row</li>
            <li>XSS / SQL injection in string fields → sanitized</li>
            <li>Empty rows at end of file → ignored</li>
          </ul>
        </div>
      </details>
    </div>
  );
}
