import React from "react";
import Header from "./Header";
import "../../styles/dashboard.css";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="layout-root">
      {" "}
      {/* 이름을 다르게 바꿔줍니다 */}
      <Header />
      <main className="layout-content">{children}</main>
    </div>
  );
}
