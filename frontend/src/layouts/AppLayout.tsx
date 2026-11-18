import { useEffect, useRef, useState } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { CheckCircle2 } from "lucide-react";
import {
  Bell,
  CarFront,
  Fuel,
  LayoutDashboard,
  LogOut,
  Menu,
  ReceiptText,
  ShieldCheck,
  X,
} from "lucide-react";
import { Brand } from "../components/Brand";
import { useAuth } from "../features/auth/useAuth";
import { NotificationMenu } from "../features/notifications/NotificationMenu";
export function AppLayout() {
  const { user, signOut, authError } = useAuth();
  const [open, setOpen] = useState(false);
  const sidebarRef = useRef<HTMLElement>(null);
  const menuButtonRef = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (!open) return;
    const previousOverflow = document.body.style.overflow;
    const menuButton = menuButtonRef.current;
    document.body.style.overflow = "hidden";
    const controls = sidebarRef.current?.querySelectorAll<HTMLElement>(
      "a, button:not([disabled])",
    );
    controls?.[0]?.focus();
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
      if (event.key !== "Tab" || !controls?.length) return;
      const first = controls[0];
      const last = controls[controls.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last?.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first?.focus();
      }
    };
    document.addEventListener("keydown", handleKey);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKey);
      menuButton?.focus();
    };
  }, [open]);
  const location = useLocation();
  const notice = (location.state as { notice?: string } | null)?.notice;
  const currentView =
    location.pathname === "/"
      ? "Dashboard"
      : location.pathname.startsWith("/fuel")
        ? "Tankowania"
        : location.pathname.startsWith("/expenses")
          ? "Wydatki"
          : location.pathname.startsWith("/reminders")
            ? "Przypomnienia"
            : location.pathname.startsWith("/admin")
              ? "Administracja"
              : "Pojazdy";
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main">
        Przejdź do treści
      </a>
      {open && (
        <button
          className="sidebar-backdrop"
          aria-label="Zamknij menu"
          onClick={() => setOpen(false)}
        />
      )}
      <aside
        id="main-navigation"
        ref={sidebarRef}
        className={`sidebar ${open ? "sidebar-open" : ""}`}
        aria-label="Nawigacja główna"
      >
        <div className="sidebar-brand">
          <Brand />
          <button
            className="icon-button mobile-only"
            aria-label="Zamknij menu"
            onClick={() => setOpen(false)}
          >
            <X size={20} />
          </button>
        </div>
        <nav>
          <NavLink to="/" end onClick={() => setOpen(false)}>
            <LayoutDashboard size={18} />
            Dashboard
          </NavLink>
          <NavLink to="/vehicles" onClick={() => setOpen(false)}>
            <CarFront size={18} />
            Pojazdy
          </NavLink>
          <NavLink to="/fuel" onClick={() => setOpen(false)}>
            <Fuel size={18} />
            Tankowania
          </NavLink>
          <NavLink to="/expenses" onClick={() => setOpen(false)}>
            <ReceiptText size={18} />
            Wydatki
          </NavLink>
          <NavLink to="/reminders" onClick={() => setOpen(false)}>
            <Bell size={18} />
            Przypomnienia
          </NavLink>
          {user?.role === "ADMIN" && (
            <NavLink to="/admin" onClick={() => setOpen(false)}>
              <ShieldCheck size={18} />
              Administracja
            </NavLink>
          )}
        </nav>
      </aside>
      <div className="app-body">
        <header className="topbar">
          <div className="topbar-title">
            <button
              className="icon-button mobile-only"
              aria-label="Otwórz menu"
              aria-expanded={open}
              ref={menuButtonRef}
              aria-controls="main-navigation"
              onClick={() => setOpen(!open)}
            >
              <Menu size={21} />
            </button>
            <span>{currentView}</span>
          </div>
          <div className="user-menu">
            <NotificationMenu />
            <span className="avatar">
              {user?.firstName.slice(0, 1)}
              {user?.lastName.slice(0, 1)}
            </span>
            <div className="user-details">
              <strong>
                {user?.firstName} {user?.lastName}
              </strong>
              <span>{user?.email}</span>
            </div>
            <button
              className="icon-button"
              onClick={() => void signOut()}
              aria-label="Wyloguj się"
              title="Wyloguj się"
            >
              <LogOut size={18} />
            </button>
          </div>
        </header>
        <main id="main" className="app-main">
          {authError && (
            <div className="feedback feedback-error" role="alert">
              {authError}
            </div>
          )}
          {notice && (
            <div className="feedback feedback-success" role="status">
              <CheckCircle2 size={18} />
              {notice}
            </div>
          )}
          <Outlet />
        </main>
      </div>
    </div>
  );
}
