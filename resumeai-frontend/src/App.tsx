import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { Toaster } from "./components/ui/sonner";

// Placeholder components
const Login = () => <div className="p-8">Login Page</div>;
const CandidateDashboard = () => <div className="p-8">Candidate Dashboard</div>;
const RecruiterDashboard = () => <div className="p-8">Recruiter Dashboard</div>;

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/candidate/*" element={<CandidateDashboard />} />
          <Route path="/recruiter/*" element={<RecruiterDashboard />} />
          <Route path="/" element={<Navigate to="/login" replace />} />
        </Routes>
        <Toaster />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
