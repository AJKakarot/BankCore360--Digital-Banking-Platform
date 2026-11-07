import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Allow public access to AI Assistant and Loan Products/Calculator
  if (pathname === "/customer/ai-assistant" || pathname.startsWith("/customer/ai-assistant")) {
    return NextResponse.next();
  }
  if (pathname === "/customer/apply") {
    const userCookie = request.cookies.get("bankcore360_user");
    if (!userCookie) {
      return NextResponse.next();
    }
  }

  // Protected route prefixes
  const isCustomerRoute = pathname.startsWith("/customer");
  const isCheckerRoute = pathname.startsWith("/checker");
  const isManagerRoute = pathname.startsWith("/manager");
  const isAdminRoute = pathname.startsWith("/admin");

  if (!isCustomerRoute && !isCheckerRoute && !isManagerRoute && !isAdminRoute) {
    return NextResponse.next();
  }

  const userCookie = request.cookies.get("bankcore360_user");

  if (!userCookie || !userCookie.value) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("redirect", pathname);
    return NextResponse.redirect(loginUrl);
  }

  try {
    const user = JSON.parse(decodeURIComponent(userCookie.value));
    if (!user || !user.role) {
      return NextResponse.redirect(new URL("/login", request.url));
    }

    const role = user.role;

    // Strict Role-Based Route Access Control
    if (isCustomerRoute && role !== "CUSTOMER" && role !== "ADMIN") {
      if (role === "CHECKER") return NextResponse.redirect(new URL("/checker/dashboard", request.url));
      if (role === "MANAGER") return NextResponse.redirect(new URL("/manager/dashboard", request.url));
      return NextResponse.redirect(new URL("/login", request.url));
    }

    if (isCheckerRoute && role !== "CHECKER" && role !== "ADMIN") {
      if (role === "CUSTOMER") return NextResponse.redirect(new URL("/customer/dashboard", request.url));
      if (role === "MANAGER") return NextResponse.redirect(new URL("/manager/dashboard", request.url));
      return NextResponse.redirect(new URL("/login", request.url));
    }

    if (isManagerRoute && role !== "MANAGER" && role !== "ADMIN") {
      if (role === "CUSTOMER") return NextResponse.redirect(new URL("/customer/dashboard", request.url));
      if (role === "CHECKER") return NextResponse.redirect(new URL("/checker/dashboard", request.url));
      return NextResponse.redirect(new URL("/login", request.url));
    }

    if (isAdminRoute && role !== "ADMIN") {
      if (role === "CUSTOMER") return NextResponse.redirect(new URL("/customer/dashboard", request.url));
      if (role === "CHECKER") return NextResponse.redirect(new URL("/checker/dashboard", request.url));
      if (role === "MANAGER") return NextResponse.redirect(new URL("/manager/dashboard", request.url));
      return NextResponse.redirect(new URL("/login", request.url));
    }

    return NextResponse.next();
  } catch {
    return NextResponse.redirect(new URL("/login", request.url));
  }
}

export const config = {
  matcher: [
    "/customer/:path*",
    "/checker/:path*",
    "/manager/:path*",
    "/admin/:path*",
  ],
};

