/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        // You can use a single hex code or an object for shades
        primary: '#3b82f6',
        'primary-dark': '#1e3a8a',
        brand: {
          light: '#6ee7b7',
          DEFAULT: '#10b981', // used as 'bg-brand'
          dark: '#047857',
        },
      },
    },
  },
  plugins: [],
};
