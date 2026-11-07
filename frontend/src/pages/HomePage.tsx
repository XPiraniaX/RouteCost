import { ArrowUpRight, LogOut, ShieldCheck } from "lucide-react";
import { Brand } from "../components/Brand";
import { useAuth } from "../features/auth/useAuth";

export function HomePage() {
  const { user, signOut } = useAuth();

  return (
    <main className="home-page">
      <header className="home-header">
        <Brand />
        <button type="button" className="logout-button" onClick={signOut}>
          <LogOut size={16} /> Wyloguj się
        </button>
      </header>
      <section className="home-content">
        <div className="home-status"><span /> KONTO AKTYWNE</div>
        <p className="home-eyebrow">TWOJA PRZESTRZEŃ ROUTECOST</p>
        <h1>Witaj, {user?.firstName}.</h1>
        <p className="home-intro">Zalogowano jako <strong>{user?.email}</strong>. To bezpieczna strona startowa Twojego konta.</p>
        <div className="home-card">
          <div className="home-card-icon"><ShieldCheck size={20} /></div>
          <div>
            <h2>Autoryzacja działa poprawnie</h2>
            <p>Twoje konto jest połączone z RouteCost. Kolejne etapy dodadzą zarządzanie pojazdami i kosztami.</p>
          </div>
          <ArrowUpRight size={17} className="home-card-arrow" />
        </div>
      </section>
      <footer className="home-footer">RouteCost <span>•</span> Prościej planuj. Jedź spokojniej.</footer>
    </main>
  );
}
