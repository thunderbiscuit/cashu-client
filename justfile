test:
  ./gradlew test

onetest TEST:
  ./gradlew test --tests {{TEST}}

serve:
  ./gradlew dokkaHtml && rm -rf ./docs/api/ && mv ./lib/build/dokka/html ./docs/api && mkdocs serve

publishlocal:
  ./gradlew publishToMavenLocal
