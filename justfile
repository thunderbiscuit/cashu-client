test:
  ./gradlew test

onetest TEST:
  ./gradlew test --tests {{TEST}}

serve:
  mkdocs serve
