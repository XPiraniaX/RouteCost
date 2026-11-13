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
  X,
} from "lucide-react";
import { Brand } from "../components/Brand";
import { useAuth } from "../features/auth/useAuth";
export function AppLayout() {
  const { user, signOut } = useAuth();
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
        <p className="nav-caption">TWOJA PRZESTRZEŃ</p>
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
          <button disabled title="Moduł będzie dostępny w kolejnym etapie">
            <Bell size={18} />
            Przypomnienia
            <span>Wkrótce</span>
          </button>
        </nav>
        <div className="sidebar-bottom">
          <span className="small-label">ROUTECOST</span>
          <p>
            Mniej niewiadomych.
            <br />
            Więcej kontroli.
          </p>
          <span className="muted">Twoje pojazdy, w jednym miejscu.</span>
        </div>
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
            <span>
              Moja przestrzeń{" "}
              <span className="muted">
                /{" "}
                {location.pathname === "/"
                  ? "Dashboard"
                  : location.pathname.startsWith("/fuel")
                    ? "Tankowania"
                    : location.pathname.startsWith("/expenses")
                      ? "Wydatki"
                      : "Pojazdy"}
              </span>
            </span>
          </div>
          <div className="user-menu">
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
              onClick={signOut}
              aria-label="Wyloguj się"
              title="Wyloguj się"
            >
              <LogOut size={18} />
            </button>
          </div>
        </header>
        <main id="main" className="app-main">
          {notice && (
            <div className="feedback feedback-success" role="status">
              <CheckCircle2 size={18} />
              {notice}
            </div>
          )}
          <Outlet />
        </main>
        <footer className="app-footer">
          <span>RouteCost · Kontrola zaczyna się od danych.</span>
          <span>Twoja przestrzeń motoryzacyjna</span>
        </footer>
      </div>
    </div>
  );
}
