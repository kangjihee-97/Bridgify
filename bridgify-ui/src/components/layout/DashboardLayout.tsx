import React from "react";
import Header from "./Header";
import "../../styles/dashboard.css";

export default function DashboardLayout({
  children,
  onReset,
  onSave,
  onPdf,
}: {
  children: React.ReactNode;
  onReset?: () => void;
  onSave?: () => void;
  onPdf?: () => void;
}) {
  return (
    <div className="layout-root">
      <Header onReset={onReset} onSave={onSave} onPdf={onPdf} />
      <main className="layout-content">{children}</main>
    </div>
  );
}
