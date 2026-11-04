# Лаборатори №8 — Mock объектууд: Mockito-гоор хамаарлыг орлуулж тестлэх

**Оюутан:** Т. Амармэнд
**Оюутны код:** B232270036
**Хичээл:** F.CSA313 — Программ хангамжийн чанарын баталгаа ба тест (2026)
**Репозитор:** https://github.com/Nonsense727/lab08-mockito

---

## 1. Орчны хувилбар

```
$ java -version
openjdk version "21.0.12.1" 2026-08-18
OpenJDK Runtime Environment Homebrew (build 21.0.12.1)
OpenJDK 64-Bit Server VM Homebrew (build 21.0.12.1, mixed mode, sharing)

$ mvn -version
Apache Maven 3.10.0 (c43a36b8d67be7e0805a411bc0898af1a51f5472)
Maven home: /opt/homebrew/Cellar/maven/3.10.0/libexec
Java version: 21.0.12.1, vendor: Homebrew, runtime: /opt/homebrew/Cellar/openjdk@21/21.0.12.1/libexec/openjdk.jdk/Contents/Home
Default locale: en_MN, platform encoding: UTF-8, time zone: Asia/Ulaanbaatar
OS name: "mac os x", version: "27.0.1", arch: "aarch64", family: "mac"
```

JDK 21 дээр тест ажиллуулахад `WARNING: A Java agent has been loaded dynamically
(… byte-buddy-agent …)` гэсэн анхааруулга гардаг — энэ нь Mockito-гийн inline
mock maker-ийн хэвийн анхааруулга, алдаа биш (зааврын дагуу засах шаардлагагүй).

## 2. Тестийн тоо

**10** — `results/mvn-test.txt` файлын сүүлийн `Tests run:` мөрийн тоо.

```
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

7 энгийн тест (A1–A7) + A8-ийн `@CsvSource`-ийн 3 мөр (мөр бүр тусдаа тест
болж тоологдоно) = 10.

## 3. Тестлэгдэх систем

`CourseRegistrationService.register(studentId, courseId)` — Лекц 5-ын бүртгэлийн
API-гийн бизнес логик, нэгжийн түвшинд. Хоёр хамаарал constructor-оор ирнэ
(dependency injection): `StudentRepository`, `EmailSender`. Хоёр интерфейс хоёулаа
**бодит хэрэгжүүлэлтгүй** — өгөгдлийн сан, SMTP сервергүйгээр логикийг шалгахын
тулд хоёуланг нь Mockito-гийн mock объектоор орлуулсан.

Спецификаци (oracle) нь `register()` методын javadoc дээр бичигдсэн:

| Буцах утга | Нөхцөл |
|---|---|
| `OK` | бүртгэл хадгалагдаж, мэдэгдэл илгээгдэнэ |
| `ERROR_NO_STUDENT` | `findStudent` нь `null` буцаасан |
| `ERROR_INACTIVE_STUDENT` | оюутан идэвхгүй |
| `ERROR_NO_COURSE` | `courseExists` нь `false` |
| `ERROR_PREREQUISITES` | `prerequisites − coursesTaken ≠ ∅` |
| `IllegalArgumentException` | `studentId` эсвэл `courseId` нь `null` |

## 4. Хэсэг A — Найман тест

`src/test/java/mn/edu/must/sqat/CourseRegistrationServiceTest.java`

| № | Тест | Гол хэрэгсэл | Юу шалгадаг |
|---|---|---|---|
| A1 | `register_идэвхтэйОюутан_OKБуцаж_хадгалагдаж_иМэйлЯвагдана` | `when` + `verify` | `"OK"`; `saveRegistration` дуудагдсан; `send` дуудагдсан |
| A2 | `register_байхгүйОюутан_ERROR_NO_STUDENT_хадгалахгүй_иМэйлЯвуулахгүй` | `verify(..., never())` | `ERROR_NO_STUDENT`; сан руу хадгалаагүй, и-мэйл яваагүй |
| A3 | `register_идэвхгүйОюутан_ERROR_INACTIVE_STUDENT_иМэйлтэйХарьцахгүй` | `verifyNoInteractions` | `ERROR_INACTIVE_STUDENT`; `emailSender`-тэй огт харьцаагүй |
| A4 | `register_дутууӨмнөхХолбоо_ERROR_PREREQUISITES_хадгалахгүй` | `when` + `never` | `ERROR_PREREQUISITES`; хадгалаагүй |
| A5 | `register_иМэйлАлдаатайЧ_бүртгэл_OK_хадгалалтХийгдсэн` | `doThrow(...).when(...)` | и-мэйл `RuntimeException` шидсэн ч `"OK"`, хадгалалт хийгдсэн |
| A6 | `register_nullОролт_IllegalArgumentException_хамаарлуудтайХарьцахгүй` | `assertThrows` | `IllegalArgumentException`; хамаарлуудтай огт харьцаагүй |
| A7 | `register_илгээгдсэнИМэйлийнБиед_хичээлийнКодОрсон` | `ArgumentCaptor` | илгээсэн и-мэйлийн `to`/`subject`/`body`-г барьж, биед `courseId` орсныг шалгана |
| A8 | `register_гурванАлдааныАнги_харгалзахКодБуцаана` | `@ParameterizedTest` + `@CsvSource` | 3 мөр: `NO_STUDENT`, `INACTIVE_STUDENT`, `NO_COURSE` |

**Matcher-ийн дүрэм.** A1 болон A5-д `send`-ийг `eq(STUDENT_EMAIL), anyString(),
anyString()` хэлбэрээр шалгасан — нэг аргументад matcher хэрэглэсэн бол бүгдэд нь
matcher хэрэглэх ёстой тул `to`-г `eq(...)`-ээр бичсэн (түүхий `STUDENT_EMAIL`-тэй
хольж болохгүй). A7-д matcher огт хэрэглээгүй — `ArgumentCaptor.capture()` бүх
аргументыг барих тул тэнд matcher шаардлагагүй.

### A8 ба `lenient()` — яагаад хэрэгтэй болсон бэ

`@CsvSource`-ийн мөр бүр өөр stub ашигладаг ч `switch` дотор бүх scenario-г
салгасан хэдий ч Mockito-гийн `MockitoExtension` нь **strict stubs** горимд
ажилладаг тул A8-ийн 2 дахь мөр (`INACTIVE_STUDENT`) дээр `courseExists(...)`
stub нь `register()`-ийн `!student.active()` салаанд эрт буцсанаас болж
**ашиглагдалгүй үлдэж** `UnnecessaryStubbingException` шидэж байв. Үүнийг
баримтаар нотолсон: `lenient()`-гүй хувилбарт тест

```
[ERROR] Tests run: 10, Failures: 0, Errors: 1, Skipped: 0
[ERROR]   ...register_гурванАлдааныАнги_харгалзахКодБуцаана(String, String)[2] » UnnecessaryStubbing
```

гэж унадаг. Тиймээс A8-д `lenient().when(...)` ашигласан — параметржүүлсэн нэг
тестийн дотор нэг л stub идэвхтэй болох тул бусдыг нь "заавал ашиглагдах ёстой"
гэж шаардах нь буруу. Бусад 7 тест дээр `lenient()` ашиглаагүй — тэнд strict
горим нь үнэхээр хэрэгтэй (stub мартсан, эсвэл логик эвдэрснийг илрүүлдэг).

## 5. Хэсэг B — Тестээ тестлэх (мини мутаци)

`src/main/java/mn/edu/must/sqat/CourseRegistrationService.java` файлд 3 удаа
зориуд эвдрэл хийж, тестүүд барих эсэхийг шалгаад, мутаци бүрийн дараа кодыг
**заавал сэргээсэн**. Эцэст нь `diff`-ээр эх хувилбартай яг ижил болсныг
баталсан.

| Мутант | Хийсэн эвдрэл | Унасан тест | Төрөл |
|---|---|---|---|
| **M1** | `if (!student.active()) …` мөрийг комментлосон | A3 `register_идэвхгүйОюутан_...`<br>A8 [2] `INACTIVE_STUDENT` | 2 Failure |
| **M2** | `repository.saveRegistration(...)` дуудалтыг комментлосон | A1 `register_идэвхтэйОюутан_...`<br>A5 `register_иМэйлАлдаатайЧ_...` | 2 Failure |
| **M3** | `\|\| courseId == null` хэсгийг устгасан | A6 `register_nullОролт_...` | 1 Failure |

Бүх мутантын гаралт `results/mvn-test-mutant-1..3.txt` файлд хадгалагдсан бөгөөд
тус бүрд `Failures ≥ 1` ба `BUILD FAILURE` байна:

```
$ grep "Tests run:" results/mvn-test-mutant-1.txt | tail -1
[ERROR] Tests run: 10, Failures: 2, Errors: 0, Skipped: 0
$ grep "Tests run:" results/mvn-test-mutant-2.txt | tail -1
[ERROR] Tests run: 10, Failures: 2, Errors: 0, Skipped: 0
$ grep "Tests run:" results/mvn-test-mutant-3.txt | tail -1
[ERROR] Tests run: 10, Failures: 1, Errors: 0, Skipped: 0
```

**M1 — яагаад A3 унасан бэ.** `!student.active()` шалгалт арилснаар идэвхгүй
оюутан `courseExists` хүртэл явж, тэнд stub хийгээгүй тул `false` буцаж
`ERROR_NO_COURSE` гарсан: `expected: <ERROR_INACTIVE_STUDENT> but was:
<ERROR_NO_COURSE>`. A8-ийн `INACTIVE_STUDENT` мөрөнд `courseExists` нь
`lenient().when(...).thenReturn(true)` тул цааш явж `"OK"` болсон:
`expected: <ERROR_INACTIVE_STUDENT> but was: <OK>`.

**M2 — яагаад A1, A5 унасан бэ.** `saveRegistration` дуудагдахаа болиход
`verify(repository).saveRegistration(...)` нь `Wanted but not invoked:` гэж
мэдэгдэв. Анхаарах нь: A2/A4-ийн `never()` assertion-ууд энэ мутантыг **бариагүй** —
тэдгээр нь "дуудагдахгүй" гэж шаарддаг тул дуудагдахгүй байх нь тэдэнд саад болохгүй.
Өөрөөр хэлбэл зөвхөн **эерэг** verify нь дуудалт алга болсныг барьдаг.

**M3 — A6 барьсан.** `courseId == null` шалгалт арилснаар
`service.register(STUDENT_ID, null)` дуудалт `IllegalArgumentException` шидэхээ
болив: `Expected java.lang.IllegalArgumentException to be thrown, but nothing was
thrown`. Хэрэв A6-г зөвхөн `register(null, COURSE_ID)` гэж бичсэн бол энэ мутант
**баригдахгүй** байх байсан — тиймээс A6-г эхнээсээ гурван тохиолдлоор
(`null, COURSE_ID`; `STUDENT_ID, null`; `null, null`) бичсэн.

**Дүгнэлт (Хэсэг B).** Гурван мутантыг гурвууланг нь тестүүд барьсан — гэхдээ
мутант бүрийг **нэг** тест биш, өөр өөр тестүүд барьсан нь тестийн багц бүхэлдээ
ажиллаж байгааг харуулна. Хамгийн сул цэг нь M2 байв: бүртгэл хадгалагдахгүй
болсон ноцтой алдааг зөвхөн 2 тест барьсан, учир нь `never()` assertion-ууд
энэ чиглэлийн эвдрэлийг зарчмын хувьд барьж чаддаггүй. Мөн M1 дээр A8-ийн
`INACTIVE_STUDENT` мөр `"OK"` болтол явсан нь `courseExists`-ийн буцах утга
мутантын нөлөөг "далдлаж" байсныг харуулж байна — ийм далдлалт нь тестийн
тохируулга дахь анхаарах зүйл юм.

## 6. Дүгнэлт

1. Mock ба гараар бичсэн fake-ийн гол ялгаа нь **хяналт ба нотолгоо** юм.
   Гараар бичсэн fake нь интерфейсийн бодит (хялбаршуулсан) хэрэгжүүлэлт тул
   төлөв хадгалж, дуудалтын дарааллыг "жинхэнэ" мэт дуурайдаг; харин mock нь
   хэрэгжүүлэлтгүй, зөвхөн бидний заасан хариуг буцаадаг. Энэ лабораторид
   `StudentRepository`-г fake-ээр бичвэл in-memory Map удирдах код нэмэгдэх байсан
   бол mock нь `when(...).thenReturn(...)` гэсэн 1 мөрөөр л шийдэгдэж,
   `verify`-ээр "юу дуудагдсан" гэдгийг шууд нотолж чадсан.
2. Гараар бичсэн fake нь дахин ашиглагдана (олон тестэд нэг fake), mock нь
   ихэвчлэн тест тус бүрдээ шинээр тохируулагдана. Гэхдээ fake-ийн логик өөрөө
   алдаатай бол тестүүд "ногоон" байх атлаа хуурамч итгэл төрүүлдэг — mock нь
   энэ эрсдэлээс ангид, гэхдээ "хэт их mock" нь тестийг хэрэгжүүлэлтийн дэлгэрэнгүй
   мэдээлэлд хэт холбодог.
3. **Юуг mock хийх ёсгүй вэ.** `Student` гэх мэт өгөгдлийн объект (record)-ыг
   mock хийх ёсгүй — тэр нь зүгээр л утга агуулагч, бодит объект үүсгэх нь
   хямд бөгөөд тодорхой. Мөн `List`, `String` гэх мэт JDK-ийн классуудыг,
   болон тестэлж буй класс өөрөө (`CourseRegistrationService`)-г mock хийх
   ёсгүй. Ерөнхий дүрэм: **гадаад, хяналтаас гадуур, хүнд эсвэл тодорхойгүй
   хамаарлыг** (сүлжээ, өгөгдлийн сан, цаг, санамсаргүй тоо) mock хийнэ; цэвэр
   функц, утгын объект, өөрийн кодоо бүтнээр нь ажиллуулна.
4. `verify`-г хэтрүүлэх эрсдэл нь **тестийг хэрэгжүүлэлтэд хэт холбох** явдал.
   `verify` нь "ямар үр дүн гарсан" биш, "ямар метод дуудагдсан" гэдгийг
   шалгадаг тул рефакторинг хийхэд (жишээ нь `saveRegistration`-ийг өөр
   методоор солих, эсвэл дуудалтын дараалал өөрчлөгдөх) тест гэмтээгүй атлаа
   унадаг — энэ нь **хуурамч сөрөг (false negative)** бий болгож, хөгжүүлэгчийг
   тестээ "засахад" хүргэдэг.
5. Энэ лабораторид хамгийн тод жишээ нь M2 мутант байв: `never()`-ээр бичсэн
   A2, A4 тестүүд `saveRegistration` дуудагдахаа больсныг барьж чадсангүй, учир
   нь зөвхөн **эерэг** `verify` нь дуудалт байгааг, `never()` нь байхгүйг
   нотолдог. Тиймээс `verify`-г "үр дүнгийн assertion"-тай (буцах утга,
   хадгалагдсан төлөв) хослуулах нь чухал — зөвхөн дуудалтын тоо хангалтгүй.
6. Нөгөө талаас `verify` нь буцах утгаараа илрэхгүй **гаж нөлөөг** (side effect)
   барих цорын ганц хэрэгсэл юм. `EmailSender.send(...)` нь юу ч буцаадаггүй тул
   "и-мэйл явах ёстой/явах ёсгүй" гэдгийг `verify`/`never`-гүйгээр шалгах
   боломжгүй — энэ тохиолдолд `verify` нь зөв хэрэглээ.
7. Хэт их `verify`-ийн өөр нэг эрсдэл нь тестийн **эмзэг байдал**: `verify(repo,
   times(3))` гэх мэт тоон шалгалт нь логик зөв атлаа кэш нэмэгдэх, эсвэл
   дуудалт нэгтгэгдэх үед унадаг. Мөн `InOrder`-оор дараалал шаардах нь
   хэрэгжүүлэлтийн дотоод дарааллыг тестэд шингээж, рефакторингийг хаадаг.
8. Дүгнэвэл: mock нь нэгжийн тестийг хурдан, тусгаарлагдмал, детерминист болгодог
   боловч тэнцвэр чухал — **гадаад хамаарлыг mock хийж, дотоод логикийг бүтнээр
   ажиллуулж, assertion-уудыг үр дүн ба зөвхөн утга учиртай гаж нөлөөнд
   төвлөрүүлэх** нь эрүүл тестийн багцын үндэс юм. Хэсэг B-гийн мутацийн шинжилгээ
   үүнийг практикт батлав: мутант бүрийг барихын тулд тест бүр **өөр өөр**
   зүйл шалгах ёстой байсан бөгөөд 10 тест 3/3 мутантыг барьсан.

## 7. Нэмэлт даалгавар — AI өнцөг (Сонголт 2)

`CourseRegistrationService.java`-г AI туслахад өгөөд «Mockito-гоор бүрэн тестийн
класс бич» гэж хүссэн. AI 11 тесттэй класс буцаасан (`ai-review/AiGenerated…Test.java`).
Түүнийг **зөвхөн ажиллуулаад зогсохгүй, Хэсэг B-гийн 3 мутантаар эвдэж үзсэн** —
үр дүн нь:

| Мутант | Миний тест (10) | AI-ийн тест (11) |
|---|---|---|
| M1 `!student.active()` коммент | ✅ барьсан (2 Failure) | ✅ барьсан (2 Failure) |
| M2 `saveRegistration` коммент | ✅ барьсан (2 Failure) | ✅ барьсан (3 Failure) |
| M3 `\|\| courseId == null` устгасан | ✅ барьсан (1 Failure) | ❌ **барьж чадсангүй — BUILD SUCCESS** |
| M4 (нэмэлт) алдааны замд ч и-мэйл явуулав | ✅ барьсан (2 Failure) | ❌ **барьж чадсангүй — BUILD SUCCESS** |

Гаралтууд: `results/mvn-test-mutant-N.txt` (миний) ба `results/ai-vs-mutant-N.txt` (AI).

**(1) Хэв маяг ба юу мартсан.** AI-ийн тестүүд минийхтэй ижил тооны тесттэй
(11 vs 10) боловч нэр нь `testSuccessfulRegistration`, `testNullStudentId` гэх мэт
ерөнхий байсан — зааврын `юуг_ямарНөхцөлд_юуХүлээгдэнэ` хэв маягийг дагаагүй.
AI нэмж `InOrder` (дуудалтын дараалал) ба `times(1)` шалгалтыг санаачилсан нь
миний тестэд байгаагүй — гэхдээ дараалал нь хэрэгжүүлэлтийн дэлгэрэнгүйг тестэд
шингээдэг тул (дээрх дүгнэлт #4) энэ нь давуу тал биш. AI мөн `@InjectMocks`
ашигласан — миний `@BeforeEach` дэх гараар `new` хийсэнтэй төстэй.

**(2) «Сул» тест байна уу — тийм.** AI-ийн тестүүд код **зөв үед** ногоон болдог ч
**эвдэрсэн үед ч** ногоон хэвээр үлддэг. M3-д AI-ийн `testNullStudentId` нь зөвхөн
`service.register(null, "F.CSA313")` гэж дуудсан — `courseId == null`-ийг шалгаагүй
тул мутантыг мэдэрсэнгүй. M4-д AI-ийн `testStudentNotFound` нь зөвхөн буцах утгыг
(`ERROR_NO_STUDENT`) `assertEquals`-ээр шалгасан бөгөөд «алдааны замд и-мэйл
явах ёсгүй» гэдгийг `verify(…, never())`-ээр **огт шалгаагүй** — тиймээс код
алдаатай хэрэглэгчид и-мэйл илгээж эхэлснийг ч илрүүлээгүй. Энэ бол сонгодог
**сул assertion**: үр дүнг л харж, гаж нөлөөг (side effect) алдсан. Миний A2/A3
тестүүд `never()` ба `verifyNoInteractions`-оор яг үүнийг хамарсан тул хоёр
мутантыг хоёуланг нь барьсан.

**(3) Дүгнэлт.** AI-ийн тест 4 мутантын 2-ыг л барьсан; миний тест 4/4-ийг барьсан.
Гол ялгаа нь AI **тоо хэмжээг** (илүү тест, илүү хэрэгсэл) нэмсэн бол би **сөрөг
замыг** (юу болох ёсгүй вэ) хамарсан. AI-ийн гаргасан тестийг шүүмжгүйгээр
аваад `mvn test` ногоон болсныг л харвал «бүх зүйл хамгаалагдсан» гэсэн хуурамч
итгэл төрөх байсан — мутацаар эвдэж үзсэнээр л жинхэнэ цоорхой илэрсэн. Заавар
«AI-ийн бичсэн тестийг өөрөө ажиллуулж, Хэсэг B-ээр эвдэж үзээгүй … ажлыг хуулбар
гэж үзнэ» гэсэн нь яг энэ шалтгаантай нийцэж байна.

## 8. Репозиторийн бүтэц

```
lab08-mockito/
├── pom.xml
├── .gitignore                    # target/, .idea/, .DS_Store
├── README.md                     # энэ файл
├── ai-review/
│   └── AiGeneratedCourseRegistrationServiceTest.java   # AI-ийн тест (харьцуулалт)
├── results/
│   ├── mvn-test.txt              # эцсийн ногоон: Tests run: 10, BUILD SUCCESS
│   ├── mvn-test-mutant-1.txt     # Failures: 2, BUILD FAILURE
│   ├── mvn-test-mutant-2.txt     # Failures: 2, BUILD FAILURE
│   ├── mvn-test-mutant-3.txt     # Failures: 1, BUILD FAILURE
│   ├── ai-vs-mutant-1.txt        # AI тест: Failures: 2, BUILD FAILURE
│   ├── ai-vs-mutant-2.txt        # AI тест: Failures: 3, BUILD FAILURE
│   ├── ai-vs-mutant-3.txt        # AI тест: Failures: 0, BUILD SUCCESS ← цоорхой
│   └── ai-vs-mutant-4.txt        # AI тест: Failures: 0, BUILD SUCCESS ← цоорхой
└── src/
    ├── main/java/mn/edu/must/sqat/
    │   ├── Student.java
    │   ├── StudentRepository.java
    │   ├── EmailSender.java
    │   └── CourseRegistrationService.java
    └── test/java/mn/edu/must/sqat/
        └── CourseRegistrationServiceTest.java
```

Ажиллуулах:

```bash
mvn test                       # 10 тест ногоон
mvn test 2>&1 | tee results/mvn-test.txt
```

## 9. Ажлын явц (commit-ийн түүх)

Ажлын үе шат бүрийг тусдаа commit-оор хийсэн — бүгдийг нэг дор биш:

1. `Lab08: starter skeleton (pom.xml, src/main) + .gitignore` — starter төсөл
2. `Section A: 8 Mockito tests (stub, verify/never, doThrow, ArgumentCaptor, parameterized)`
3. `Section B: mutation testing results (3 mutants, source restored)`
4. README — дүгнэлт, мутацийн хүснэгт
5. `Optional: AI-generated test compared against the same mutants` — AI өнцөг

`git log --oneline` командаар шалгаж болно.
