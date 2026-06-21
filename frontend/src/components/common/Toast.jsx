/*
 * Minimal toast — lightweight alternative to a full toast library.
 * Usage: import { toast } from "@/components/common/Toast"; toast("message");
 * Also exports ToastContainer (mount once in StorefrontShell / App).
 */
import { createContext, useCallback, useContext, useRef, useState } from "react";
import { X } from "lucide-react";
import { cn } from "@/lib/cn";

const ToastCtx = createContext(null);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const counter = useRef(0);

  const add = useCallback((msg, type = "default") => {
    const id = ++counter.current;
    setToasts((t) => [...t, { id, msg, type }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 3500);
  }, []);

  const remove = (id) => setToasts((t) => t.filter((x) => x.id !== id));

  return (
    <ToastCtx.Provider value={add}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2" aria-live="polite">
        {toasts.map(({ id, msg, type }) => (
          <div
            key={id}
            className={cn(
              "flex items-center gap-3 rounded-lg border px-4 py-3 shadow-lg text-sm",
              type === "error"
                ? "border-danger/40 bg-danger/10 text-danger"
                : "border-border bg-card text-foreground",
            )}
          >
            <span className="flex-1">{msg}</span>
            <button onClick={() => remove(id)} aria-label="Dismiss">
              <X className="h-3.5 w-3.5 text-muted-foreground hover:text-foreground" />
            </button>
          </div>
        ))}
      </div>
    </ToastCtx.Provider>
  );
}

export function useToast() {
  return useContext(ToastCtx);
}
