package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CourseRegistrationService-ийн нэгжийн тест.
 *
 * Хоёр хамаарал (StudentRepository, EmailSender) нь бодит хэрэгжүүлэлтгүй
 * интерфейс тул Mockito-гийн mock объектоор орлуулна. Ингэснээр өгөгдлийн сан,
 * SMTP сервергүйгээр зөвхөн үйлчилгээний логикийг шалгана.
 */
@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    private static final String STUDENT_ID = "B232270036";
    private static final String COURSE_ID = "F.CSA313";
    private static final String STUDENT_EMAIL = "amarmend.t@techpack.mn";

    @Mock
    private StudentRepository repository;

    @Mock
    private EmailSender emailSender;

    private CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    // ------------------------------------------------------------------
    // Туслах: идэвхтэй, өмнөх холбоо нь бүрэн хангагдсан оюутан
    // ------------------------------------------------------------------
    private Student activeStudent(String... coursesTaken) {
        return new Student(STUDENT_ID, STUDENT_EMAIL, true, List.of(coursesTaken));
    }

    private void stubHappyPath() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent("CS101"));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS101"));
    }

    // ==================================================================
    // A1 — stub (when/thenReturn) + verify
    // ==================================================================
    @Test
    @DisplayName("A1: идэвхтэй оюутан бүртгүүлэхэд OK буцаж, хадгалагдаж, и-мэйл явна")
    void register_идэвхтэйОюутан_OKБуцаж_хадгалагдаж_иМэйлЯвагдана() {
        stubHappyPath();

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(STUDENT_EMAIL), anyString(), anyString());
    }

    // ==================================================================
    // A2 — verify(..., never())
    // ==================================================================
    @Test
    @DisplayName("A2: байхгүй оюутанд ERROR_NO_STUDENT, хадгалахгүй, и-мэйл явуулахгүй")
    void register_байхгүйОюутан_ERROR_NO_STUDENT_хадгалахгүй_иМэйлЯвуулахгүй() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(null);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // ==================================================================
    // A3 — verifyNoInteractions
    // ==================================================================
    @Test
    @DisplayName("A3: идэвхгүй оюутанд ERROR_INACTIVE_STUDENT, и-мэйлтэй огт харьцахгүй")
    void register_идэвхгүйОюутан_ERROR_INACTIVE_STUDENT_иМэйлтэйХарьцахгүй() {
        when(repository.findStudent(STUDENT_ID))
                .thenReturn(new Student(STUDENT_ID, STUDENT_EMAIL, false, List.of("CS101")));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verifyNoInteractions(emailSender);
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    // ==================================================================
    // A4 — when + never
    // ==================================================================
    @Test
    @DisplayName("A4: өмнөх холбоо хичээл дутуу бол ERROR_PREREQUISITES, хадгалахгүй")
    void register_дутууӨмнөхХолбоо_ERROR_PREREQUISITES_хадгалахгүй() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent("CS101"));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS101", "CS102"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // ==================================================================
    // A5 — doThrow
    // ==================================================================
    @Test
    @DisplayName("A5: и-мэйл илгээгч exception шидсэн ч бүртгэл OK, хадгалалт хийгдсэн")
    void register_иМэйлАлдаатайЧ_бүртгэл_OK_хадгалалтХийгдсэн() {
        stubHappyPath();
        doThrow(new RuntimeException("SMTP унасан"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(STUDENT_EMAIL), anyString(), anyString());
    }

    // ==================================================================
    // A6 — assertThrows (хоёр аргумент тус бүрийн null)
    // ==================================================================
    @Test
    @DisplayName("A6: null оролтод IllegalArgumentException, хамаарлуудтай харьцахгүй")
    void register_nullОролт_IllegalArgumentException_хамаарлуудтайХарьцахгүй() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, COURSE_ID));
        assertThrows(IllegalArgumentException.class, () -> service.register(STUDENT_ID, null));
        assertThrows(IllegalArgumentException.class, () -> service.register(null, null));

        verifyNoInteractions(repository, emailSender);
    }

    // ==================================================================
    // A7 — ArgumentCaptor
    // ==================================================================
    @Test
    @DisplayName("A7: илгээгдсэн и-мэйлийн биед хичээлийн код орсон")
    void register_илгээгдсэнИМэйлийнБиед_хичээлийнКодОрсон() {
        stubHappyPath();
        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        service.register(STUDENT_ID, COURSE_ID);

        verify(emailSender).send(toCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());
        assertEquals(STUDENT_EMAIL, toCaptor.getValue());
        assertEquals("Бүртгэл баталгаажлаа", subjectCaptor.getValue());
        assertTrue(bodyCaptor.getValue().contains(COURSE_ID));
    }

    // ==================================================================
    // A8 — @ParameterizedTest + @CsvSource (гурван алдааны анги)
    // ==================================================================
    @ParameterizedTest(name = "[{index}] {1} → {0}")
    @CsvSource({
            "ERROR_NO_STUDENT,       NO_STUDENT",
            "ERROR_INACTIVE_STUDENT, INACTIVE_STUDENT",
            "ERROR_NO_COURSE,        NO_COURSE"
    })
    @DisplayName("A8: гурван алдааны ангилал харгалзах кодыг буцаана")
    void register_гурванАлдааныАнги_харгалзахКодБуцаана(String expected, String scenario) {
        switch (scenario) {
            case "NO_STUDENT" ->
                    lenient().when(repository.findStudent(STUDENT_ID)).thenReturn(null);
            case "INACTIVE_STUDENT" -> {
                lenient().when(repository.findStudent(STUDENT_ID))
                        .thenReturn(new Student(STUDENT_ID, STUDENT_EMAIL, false, List.of()));
                lenient().when(repository.courseExists(COURSE_ID)).thenReturn(true);
            }
            case "NO_COURSE" -> {
                lenient().when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent());
                lenient().when(repository.courseExists(COURSE_ID)).thenReturn(false);
            }
            default -> throw new IllegalArgumentException("Танигдаагүй хувилбар: " + scenario);
        }

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}
