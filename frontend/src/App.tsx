import { Brand } from "./components/Brand";
import { useAuth } from "./context/AuthContext";
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

export default function App() {
  const { token, user, isBootstrapping } = useAuth();

  if (isBootstrapping) {
    return <AppLoading />;
  }

  if (!token || !user) {
    return <AuthPage />;
  }

  return <DashboardPage />;
}