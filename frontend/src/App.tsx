import { useEffect, useState } from "react";
import { Brand } from "./components/Brand";
import { useAuth } from "./context/AuthContext";
import { api } from "./lib/api";
import { AuthPage } from "./pages/AuthPage";
import { DashboardPage } from "./pages/DashboardPage";

function AppLoading() {
  return (
    <main className="app-loading">
      <Brand />
      <div className="app-loading__bar">
        <span />
      </div>
      <p>Güvenli oturum kontrol ediliyor...</p>
    </main>
  );
}

function BackendWakeNotice() {
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    let cancelled = false;
    let retryId: number | undefined;

    async function checkBackend() {
      const ready = await api.system.isBackendReady();

      if (cancelled) {
        return;
      }

      if (ready) {
        setIsReady(true);
        return;
      }

      retryId = window.setTimeout(() => {
        void checkBackend();
      }, 5_000);
    }

    void checkBackend();

    return () => {
      cancelled = true;
      if (retryId) {
        window.clearTimeout(retryId);
      }
    };
  }, []);

  if (isReady) {
    return null;
  }

  return (
    <aside className="backend-wake-notice" aria-live="polite">
      <span className="backend-wake-notice__indicator" aria-hidden="true" />
      <div>
        <strong>Backend hazırlanıyor</strong>
        <p>
          Render sunucusu yeniden başlatılıyor. İşlem yapmak için lütfen biraz
          bekleyin; backend hazır olduğunda bu uyarı kendiliğinden kaybolur.
        </p>
      </div>
    </aside>
  );
}

export default function App() {
  const { token, user, isBootstrapping } = useAuth();

  let page = <DashboardPage />;

  if (isBootstrapping) {
    page = <AppLoading />;
  } else if (!token || !user) {
    page = <AuthPage />;
  }

  return (
    <>
      {page}
      <BackendWakeNotice />
    </>
  );
}
