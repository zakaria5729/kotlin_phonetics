# kotlin-phonetics

Finds sound-alike spellings of Bangla and English words, with a match percentage.
One Kotlin Multiplatform library for Android (Views and Compose), the JVM and
desktop, iOS and the other Apple platforms, the web, Linux and Windows. The
search runs off the calling thread, so the UI never waits for it.

The rules and the algorithm are the same as in the Flutter package
[`flutter_phonetics`](#flutter), and the tests compare more than 2,800 results
with the ones the Dart package produces.

```kotlin
// Bangla phonetics
val bangla = BanglaPhonetics.instance
val result = bangla.generate("মাসুদ", minScore = 90.0)
// মাসুদ 100%, মাসূদ 97%, মাশুদ 95%, মাষুদ 95%, মাশূদ 92.15%, মাষূদ 92.15%

// English phonetics
val english = EnglishPhonetics.instance
val names = english.generate("Philip", minScore = 90.0)
// Philip 100%, Filip 95%, Phillip 95%, Philipp 95%, Fillip 90.25%, ...
```

`generate` is a `suspend` function. Every call returns a result object with the
value and an `error`; nothing is thrown.

| Target | Runs on |
| --- | --- |
| Android, JVM and desktop (macOS, Linux, Windows) | a background thread (`Dispatchers.Default`) |
| iOS, macOS, tvOS, watchOS, Linux and Windows native | a background thread |
| Kotlin/JS and Kotlin/Wasm | the event loop, in 8 ms slices, so the page stays responsive and requests can still be cancelled |

## Modules

| Module | What it is |
| --- | --- |
| `phonetics` | The library: engine, languages, background execution, cancellation. |
| `phonetics-compose` | Compose helpers: remembered instances and state that follows the input. |
| `sample-android-xml` | An Android app built with XML layouts and the callback API. |
| `sample-compose` and `sample-compose-android` | A Compose Multiplatform app for Android, desktop, iOS and the web. |

## Install

Add the dependency to the module that needs it. In a Kotlin Multiplatform
project that is `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.zakaria5729:phonetics:0.1.1")
            // for Compose
            implementation("io.github.zakaria5729:phonetics-compose:0.1.1")
        }
    }
}
```

An Android or JVM only project uses the same coordinates:

```kotlin
dependencies {
    implementation("io.github.zakaria5729:phonetics:0.1.1")
}
```

The Android library needs `minSdk` 21. `phonetics-compose` needs a Compose
Multiplatform version that supports your targets (it has no Intel iOS or macOS
build, because Compose has none).

The artifacts are on Maven Central, so `mavenCentral()` is all you need in your
repositories. To try local changes, run `./gradlew publishToMavenLocal` in this
repository and add `mavenLocal()` as well.

## Usage

```kotlin
val phonetics = BanglaPhonetics.instance

val matches = phonetics.generate("আসাদ", minScore = 90.0, maxResults = 20)
matches.error?.let { println(it.message) }
matches.matches.forEach { println("${it.word} ${it.score}%") }

val score = phonetics.similarity("আসাদ", "আছাদ").score          // 85.0
val same = phonetics.matches("আসাদ", "আশাদ", minScore = 90.0).isMatch
val ranked = phonetics.rank("আসাদ", listOf("আছাদ", "রহিম", "আশাদ"), limit = 2)
```

Defaults are `minScore` 70 and `maxResults` 50. Each match has the `word`, the
`score` from 0 to 100 and the `changes` that turn the input into the word.

### Android with XML layouts, Java and Swift

Callbacks work without coroutines. They run on the main thread on Android and
Apple platforms, and the call returns a token that cancels the request:

```kotlin
val bangla = BanglaPhonetics()

input.addTextChangedListener { text ->
    bangla.generateAsync(text.toString(), maxResults = 20) { result ->
        results.text = result.matches.joinToString("\n") { "${it.word} ${it.score}%" }
    }
}

override fun onDestroy() {
    bangla.dispose()
    super.onDestroy()
}
```

`sample-android-xml` is a full example. In a view model, call the `suspend`
functions from `viewModelScope` or `lifecycleScope` instead.

For Java, use the `…Async` functions with a lambda that returns `Unit.INSTANCE`.
For Swift, the `suspend` functions become `async` functions and the callbacks
become closures:

```swift
let result = try await BanglaPhonetics.companion.instance.generate(input: "আসাদ")
print(result.matches.map { $0.word })

BanglaPhonetics.companion.instance.generateAsync(input: "আসাদ") { result in
    print(result.matches.count)
}
```

Build the XCFramework on a Mac with Xcode: `./gradlew :phonetics:assemblePhoneticsXCFramework`.
The Swift snippets above have not been compiled yet, because this was written on a machine without Xcode.

### Compose

```kotlin
@Composable
fun Names() {
    val phonetics = rememberBanglaPhonetics()
    var input by remember { mutableStateOf("আসাদ") }
    val result by rememberPhoneticMatches(phonetics, input, maxResults = 30, debounceMillis = 120)

    Column {
        OutlinedTextField(value = input, onValueChange = { input = it })
        LazyColumn {
            items(result?.matches.orEmpty()) { match -> Text("${match.word} ${match.score}%") }
        }
    }
}
```

`rememberPhonetics`, `rememberBanglaPhonetics` and `rememberEnglishPhonetics`
dispose the instance when it leaves the composition. `rememberPhoneticMatches`,
`rememberPhoneticSimilarity` and `rememberPhoneticRanking` return `null` until
the first result, keep the previous result while a new one is computed, and
cancel a running request when the input changes. It is the same code on
Android, desktop, iOS and the web; `sample-compose` shows it.

## Languages

`PhoneticLanguages.bangla` and `PhoneticLanguages.english` ship with the library.
A language is data: a code, a name, the rules, and how its text is cleaned up
before use.

```kotlin
val language = PhoneticLanguage(
    code = "xx",
    name = "Test",
    foldCase = true,
    canonicalSequences = mapOf("ß" to "ss"),
    marks = setOf(0x2D),
    rules = listOf(
        PhoneticRule(listOf("q", "x"), 0.9),
        PhoneticRule(listOf("ee", "i"), 0.8, position = RulePosition.WordEnd),
    ),
)
val phonetics = Phonetics(language)
```

Bangla rules treat ড় and ঢ় written with a nukta the same as the single letters.
English folds case, removes accents and gives the answer back in the case of the
input (`JOHN` gives `JOHN`, `John Smith` gives `John Smith`).

A `PhoneticRule` lists spellings that sound alike and how alike (above 0 and
below 1). `position` limits it to the start, the end or the inside of a word.
`scoreOnly` rules count when comparing words but are never used to build new
spellings. Extend the defaults with
`PhoneticLanguages.bangla.defaultRuleSet.extend(listOf(...))` and pass the result
as `rules`.

## Errors

Nothing is thrown. Bad arguments, a broken language or rule set, a cancelled
request and a disposed instance all come back in `error`:

| Error | Meaning |
| --- | --- |
| `PhoneticArgumentException` | an argument, rule or language definition was not valid |
| `PhoneticCancelledException` | the request was cancelled before it finished |
| `PhoneticDisposedException` | the instance was disposed |
| `PhoneticWorkerException` | something unexpected failed while working |

Constructors keep their problems in an `error` property too, so a bad setting is
reported on the first call instead of crashing at construction.

One thing is not turned into a result: if the coroutine that called a `suspend`
function is cancelled, Kotlin's `CancellationException` propagates as usual,
and the work stops.

## Cancelling and execution mode

```kotlin
val token = PhoneticCancelToken()
val call = scope.async { phonetics.rank(query, candidates, cancelToken = token) }
token.cancel()
call.await().error   // PhoneticCancelledException
```

`phonetics.cancelAll()` cancels everything that is pending, and `dispose()`
cancels it and stops the instance.

`PhoneticExecutionMode` decides what a new request does while one is running:

| Mode | Behaviour |
| --- | --- |
| `CancelPrevious` (default) | the running request is cancelled, which suits search-as-you-type |
| `Queue` | requests run one after another |

Pass it to the constructor or change it later with
`phonetics.setExecutionMode(...)`, also on the shared `BanglaPhonetics.instance`.

## Synchronous engine

`PhoneticEngine` is the same code without the background execution. Use it for
short work, in tests, or from your own dispatcher:

```kotlin
val engine = PhoneticEngine(PhoneticLanguages.english)
val result = engine.generate("Catherine")
```

## Memory and limits

The number of variants grows quickly as `minScore` falls, so raising it is the
strongest way to save memory. A call returns at most 100,000 results and about
20 million output characters, whatever you ask for.

| Device | `minScore` | `maxResults` |
| --- | --- | --- |
| Low-end phones and mobile browsers | 60 or more | 500 or less |
| Recommended (the defaults) | 70 | 50 |
| Desktop or batch work | 40 or more | 10,000 or less |

`rank` keeps only `limit` matches, so use a `limit` with big candidate lists.

## Complexity

n is the length of the input, R the number of results, m the length of the other
word and C the number of candidates.

| | Time | Extra space |
| --- | --- | --- |
| `generate` | O(n + R (n + log R)) | O(R n) |
| `similarity` | O(n m) | O(m) |
| `rank` | O(C n m), usually much less | O(n + m + limit) |

`generate` is a best-first search over letter swaps backed by a heap. It always
expands the highest-scoring swaps first and stops as soon as `maxResults` are
collected or the scores fall below `minScore`. `rank` stops scoring a candidate
as soon as it cannot reach the cut-off.

## Scores

Every swap has a similarity, for example স→শ is 0.95 and স→ছ is 0.85. A variant's
score is the product of its swaps, so two স→শ swaps give 90.25%. The default
rules are `banglaRules` and `englishRules`.

Scores are rounded to two decimals. When a score falls exactly between two
roundings, the last digit can differ by 0.01 from the Dart package, because the
platforms' `exp` and `ln` differ in the last bit.

## Benchmark

> 90% accuracy gain, measured by testing against public datasets.

Datasets:

- **Bangla**
  - <https://github.com/google/language-resources/tree/master/bn/data>
  - <https://github.com/google-research-datasets/dakshina>
  - <https://huggingface.co/datasets/ai4bharat/Aksharantar>
- **English**
  - <https://github.com/cmusphinx/cmudict>
  - <https://llds.phon.ox.ac.uk/llds/xmlui/handle/20.500.14106/0643>
  - <https://github.com/mhagiwara/github-typo-corpus>

## Flutter

This package is also available for Flutter and Dart as `flutter_phonetics`.

- pub.dev: <https://pub.dev/packages/flutter_phonetics>
- GitHub: <https://github.com/zakaria5729/flutter_phonetics>

## Development

```bash
./gradlew :phonetics:jvmTest
./gradlew :phonetics:jsNodeTest :phonetics:wasmJsNodeTest
./gradlew :phonetics:jsBrowserTest :phonetics:wasmJsBrowserTest   # needs Chrome
./gradlew :phonetics:testAndroidHostTest
./gradlew :phonetics-compose:jvmTest
./gradlew :sample-compose:run                                    # desktop sample
./gradlew :sample-compose:wasmJsBrowserDevelopmentRun            # web sample
./gradlew :sample-android-xml:installDebug
```

`phonetics/src/commonTest/.../GoldenData.kt` holds results from the Dart package.
To refresh it, run `dart run bin/generate.dart <path>` in `tools/golden`.

## License

MIT, see [LICENSE](LICENSE).

## Connect with Developer

[LinkedIn](https://www.linkedin.com/in/zakaria5729) |
[GitHub](https://github.com/zakaria5729) |
[Stack Overflow](https://stackoverflow.com/users/8641776/zakaria-hossain?tab=profile) |
[Medium](https://medium.com/@zakaria5729) |
[Substack](https://substack.com/@zakaria5729) |
[Twitter](https://twitter.com/zakaria5729) |
[Facebook](https://www.facebook.com/zakaria5729)
