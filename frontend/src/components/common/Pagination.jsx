import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/cn";

export function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;

  const pages = buildPages(page, totalPages);

  return (
    <nav className="flex items-center justify-center gap-1" aria-label="Pagination">
      <PageBtn onClick={() => onPageChange(page - 1)} disabled={page === 0} aria-label="Previous">
        <ChevronLeft className="h-4 w-4" />
      </PageBtn>

      {pages.map((p, i) =>
        p === "…" ? (
          <span key={`ellipsis-${i}`} className="px-2 text-muted-foreground">…</span>
        ) : (
          <PageBtn
            key={p}
            active={p === page}
            onClick={() => onPageChange(p)}
            aria-label={`Page ${p + 1}`}
            aria-current={p === page ? "page" : undefined}
          >
            {p + 1}
          </PageBtn>
        ),
      )}

      <PageBtn onClick={() => onPageChange(page + 1)} disabled={page >= totalPages - 1} aria-label="Next">
        <ChevronRight className="h-4 w-4" />
      </PageBtn>
    </nav>
  );
}

function PageBtn({ active, onClick, disabled, children, ...rest }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={cn(
        "flex h-9 min-w-[2.25rem] items-center justify-center rounded-md px-2 text-sm transition",
        active
          ? "bg-primary text-primary-foreground"
          : "hover:bg-muted text-foreground disabled:opacity-40",
      )}
      {...rest}
    >
      {children}
    </button>
  );
}

function buildPages(current, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i);
  const pages = [0];
  if (current > 2) pages.push("…");
  for (let i = Math.max(1, current - 1); i <= Math.min(total - 2, current + 1); i++) {
    pages.push(i);
  }
  if (current < total - 3) pages.push("…");
  pages.push(total - 1);
  return pages;
}
