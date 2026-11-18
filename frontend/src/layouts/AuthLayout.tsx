import { ArrowUpRight } from "lucide-react";
import { Link, Outlet, useLocation } from "react-router-dom";
import { Brand } from "../components/Brand";

export function AuthLayout() {
  const isRegister = useLocation().pathname === "/register";

  return (
    <main className="auth-page">
      <div className="auth-frame">
        <header className="auth-brand">
          <Link to="/login" aria-label="RouteCost — strona logowania">
            <span className="auth-brand-logo">
              <Brand compact />
            </span>
          </Link>
          <h1>RouteCost</h1>
        </header>

        <section
          className={`auth-panel${isRegister ? " auth-panel-register" : ""}`}
          aria-labelledby="auth-title"
        >
          <div className="auth-panel-heading">
            <div className="auth-overline">
              {isRegister ? "NOWE KONTO" : "STREFA UŻYTKOWNIKA"}
            </div>
            <h2 id="auth-title">
              {isRegister ? "Dołącz do RouteCost" : "Witaj ponownie"}
            </h2>
            <p>
              {isRegister
                ? "Utwórz konto i zacznij porządkować koszty."
                : "Zaloguj się, aby przejść do swojej przestrzeni."}
            </p>
          </div>

          <Outlet />

          <div className="auth-switch">
            <span>
              {isRegister ? "Masz już konto?" : "Nie masz jeszcze konta?"}
            </span>
            <Link to={isRegister ? "/login" : "/register"}>
              {isRegister ? "Zaloguj się" : "Utwórz konto"}
              <ArrowUpRight size={14} aria-hidden="true" />
            </Link>
          </div>
          <p className="auth-legal">
            Sesja nie jest utrwalana w pamięci przeglądarki.
          </p>
        </section>
      </div>
    </main>
  );
}
