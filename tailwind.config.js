const colors = require('tailwindcss/colors')

module.exports = {
  content: ["./src/main/**/*.{html,js,cljs}", "public/index.html"],
  theme: {
    colors: {
      brown: {
        800: "#87633b",
        600: "#b9946a",
        500: "#d5b086",
        400: "#eeceaa",
        200: "#fbe9d5"
      },
      gray: colors.gray,
      red: colors.red

    },
    extend: {},
  },
  plugins: [],
}

