import { ArrowUpRight, Check, ShieldCheck, Sparkles } from "lucide-react";
import { Link, Outlet, useLocation } from "react-router-dom";
import { Brand } from "../components/Brand";

export function AuthLayout() {
  const isRegister = useLocation().pathname === "/register";

  return (
    <main className="auth-page">
      <div className="auth-frame">
        <header className="auth-header">
          <Link to="/login" aria-label="RouteCost — strona logowania"><Brand /></Link>
          <span className="hidden items-center gap-2 text-xs font-medium text-slate-500 sm:flex">
            <ShieldCheck size={15} className="text-accent/80" /> Bezpieczny dostęp do Twojego konta
          </span>
        </header>

        <div className="auth-grid">
          <section className="auth-story" aria-label="O RouteCost">
            <div className="story-kicker"><span className="kicker-dot" /> KOSZTY SAMOCHODU POD KONTROLĄ</div>
            <h1 className="story-title">Każda trasa<br />ma <span>jasną stronę.</span></h1>
            <p className="story-copy">Zbieraj informacje o kosztach auta w jednym, uporządkowanym miejscu.</p>

            <div className="route-card">
              <div className="route-card-top">
                <div className="route-icon"><Sparkles size={16} /></div>
                <span>Twoja przestrzeń do planowania</span>
                <ArrowUpRight size={16} className="ml-auto text-slate-500" />
              </div>
              <div className="route-visual" aria-hidden="true">
                <div className="route-line"><span className="route-line-fill" /></div>
                <span className="route-point route-point-start"><span /></span>
                <span className="route-point route-point-end"><span /></span>
                <div className="route-label route-label-start"><b>START</b><span>Twoje auto</span></div>
                <div className="route-label route-label-end"><b>CEL</b><span>Pełna kontrola</span></div>
                <div className="route-mile-marker"><Check size={14} /></div>
              </div>
              <div className="route-card-footer">
                <span>Prościej planuj. Jedź spokojniej.</span>
                <span className="footer-status"><i /> ROUTECOST</span>
              </div>
            </div>

            <p className="story-footnote">Przejrzystość kosztów zaczyna się od dobrego planu.</p>
          </section>

          <section className="auth-panel">
            <div className="auth-panel-heading">
              <div className="mobile-brand"><Brand /></div>
              <div className="auth-overline">{isRegister ? "NOWE KONTO" : "STREFA UŻYTKOWNIKA"}</div>
              <h2>{isRegister ? "Dołącz do RouteCost" : "Witaj ponownie"}</h2>
              <p>{isRegister ? "Utwórz konto i zacznij porządkować koszty." : "Zaloguj się, aby przejść do swojej przestrzeni."}</p>
            </div>

            <Outlet />

            <div className="auth-switch">
              <span>{isRegister ? "Masz już konto?" : "Nie masz jeszcze konta?"}</span>
              <Link to={isRegister ? "/login" : "/register"}>
                {isRegister ? "Zaloguj się" : "Utwórz konto"}
                <ArrowUpRight size={14} aria-hidden="true" />
              </Link>
            </div>
            <p className="auth-legal">Sesja nie jest utrwalana w pamięci przeglądarki.</p>
          </section>
        </div>

        <footer className="auth-footer"><span>© {new Date().getFullYear()} RouteCost</span><span>Stworzone z myślą o spokojnej drodze.</span></footer>
      </div>
    </main>
  );
}
