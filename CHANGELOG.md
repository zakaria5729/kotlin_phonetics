## 0.1.2

- Also available on JitPack as `com.github.zakaria5729.kotlin_phonetics`.

## 0.1.1

- Publishing set up for Maven Central, with signed artifacts, sources and Javadoc jars.
- POM and SCM links now point to the right GitHub repository.

## 0.1.0

- First release, a Kotlin Multiplatform port of the Dart package `flutter_phonetics` 0.1.1.
- `Phonetics` with `generate`, `generateWords`, `similarity`, `matches` and `rank`
  as `suspend` functions and as callbacks, running off the calling thread.
- `BanglaPhonetics` and `EnglishPhonetics`, and `PhoneticLanguage` to define more
  languages. Rules can be limited to the start, the end or the inside of a word.
- Nothing throws: calls return result objects with a `PhoneticException? error`.
- Cancellation with `PhoneticCancelToken`, `cancelAll()` and `dispose()`, and the
  `CancelPrevious` and `Queue` execution modes.
- `PhoneticEngine`, the same code without background execution.
- `phonetics-compose` with `rememberPhonetics`, `rememberPhoneticMatches` and friends.
- Targets: Android, JVM, JS, Wasm, iOS, macOS, tvOS, watchOS, Linux and Windows.
