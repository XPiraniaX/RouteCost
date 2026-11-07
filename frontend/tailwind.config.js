/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#07111d",
        panel: "#0d1a29",
        muted: "#8292a5",
        line: "#1b2b3c",
        accent: "#66dfca",
        accentDeep: "#2caa9b",
      },
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"],
      },
      boxShadow: {
        glow: "0 0 48px rgba(102, 223, 202, 0.13)",
      },
    },
  },
  plugins: [],
};
