package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AI-ийн санал болгосон тестийн класс (Сонголт 2 — AI өнцөг).
 *
 * Энэ файлыг AI туслах CourseRegistrationService.java-г уншаад
 * "Mockito-гоор бүрэн тестийн класс бич" гэсэн хүсэлтэд хариу болгон гаргасан.
 * Харьцуулалтын зорилгоор ai-review/ хавтаст хадгалагдсан — үндсэн
 * src/test дахь миний тесттэй ИЖИЛ БИШ, тусад нь.
 */
@ExtendWith(MockitoExtension.class)
class AiGeneratedCourseRegistrationServiceTest {

    @Mock
    private StudentRepository repository;

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private CourseRegistrationService service;

    private Student activeStudent;

    @BeforeEach
    void setUp() {
        activeStudent = new Student("B232270036", "amarmend.t@techpack.mn", true,
                List.of("CS101"));
    }

    @Test
    @DisplayName("Амжилттай бүртгэл OK буцаана")
    void testSuccessfulRegistration() {
        when(repository.findStudent("B232270036")).thenReturn(activeStudent);
        when(repository.courseExists("F.CSA313")).thenReturn(true);
        when(repository.prerequisites("F.CSA313")).thenReturn(Collections.emptyList());

        String result = service.register("B232270036", "F.CSA313");

        assertEquals("OK", result);
        verify(repository).saveRegistration("B232270036", "F.CSA313");
        verify(emailSender).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Оюутан олдохгүй бол ERROR_NO_STUDENT")
    void testStudentNotFound() {
        when(repository.findStudent("X")).thenReturn(null);

        assertEquals("ERROR_NO_STUDENT", service.register("X", "F.CSA313"));
    }

    @Test
    @DisplayName("Идэвхгүй оюутан ERROR_INACTIVE_STUDENT")
    void testInactiveStudent() {
        Student inactive = new Student("B232270036", "a@b.mn", false, List.of());
        when(repository.findStudent("B232270036")).thenReturn(inactive);

        assertEquals("ERROR_INACTIVE_STUDENT", service.register("B232270036", "F.CSA313"));
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    @Test
    @DisplayName("Хичээл байхгүй бол ERROR_NO_COURSE")
    void testCourseNotFound() {
        when(repository.findStudent("B232270036")).thenReturn(activeStudent);
        when(repository.courseExists("NOPE")).thenReturn(false);

        assertEquals("ERROR_NO_COURSE", service.register("B232270036", "NOPE"));
    }

    @Test
    @DisplayName("Өмнөх холбоо дутуу бол ERROR_PREREQUISITES")
    void testPrerequisitesMissing() {
        when(repository.findStudent("B232270036")).thenReturn(activeStudent);
        when(repository.courseExists("F.CSA313")).thenReturn(true);
        when(repository.prerequisites("F.CSA313")).thenReturn(List.of("CS101", "CS102"));

        assertEquals("ERROR_PREREQUISITES", service.register("B232270036", "F.CSA313"));
    }

    @Test
    @DisplayName("И-мэйл илгээх үед алдаа гарсан ч OK буцаана")
    void testEmailFailureStillOk() {
        when(repository.findStudent("B232270036")).thenReturn(activeStudent);
        when(repository.courseExists("F.CSA313")).thenReturn(true);
        when(repository.prerequisites("F.CSA313")).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("smtp down"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        assertEquals("OK", service.register("B232270036", "F.CSA313"));
        verify(repository).saveRegistration("B232270036", "F.CSA313");
    }

    @Test
    @DisplayName("null оюутны ID IllegalArgumentException шидэнэ")
    void testNullStudentId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register(null, "F.CSA313"));
    }

    @Test
    @DisplayName("И-мэйлийн агуулга зөв эсэхийг шалгах")
    void testEmailContent() {
        when(repository.findStudent("B232270036")).thenReturn(activeStudent);
        when(repository.courseExists("F.CSA313")).thenReturn(true);
        when(repository.prerequisites("F.CSA313")).thenReturn(Collections.emptyList());

        service.register("B232270036", "F.CSA313");

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("amarmend.t@techpack.mn"), anyString(), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("F.CSA313"));
    }

    @ParameterizedTest
    @CsvSource({
            "ERROR_NO_STUDENT, X",
            "ERROR_INACTIVE_STUDENT, Y"
    })
    @DisplayName("Алдааны код буцаах параметржүүлсэн тест")
    void testErrorCodes(String expected, String studentId) {
        if (studentId.equals("X")) {
            when(repository.findStudent("X")).thenReturn(null);
        } else {
            when(repository.findStudent("Y"))
                    .thenReturn(new Student("Y", "y@b.mn", false, List.of()));
        }

        assertEquals(expected, service.register(studentId, "F.CSA313"));
    }

    @Test
    @DisplayName("Дуудалтын дараалал зөв эсэх")
    void testCallOrder() {
        when(repository.findStudent("B232270036")).thenReturn(activeStudent);
        when(repository.courseExists("F.CSA313")).thenReturn(true);
        when(repository.prerequisites("F.CSA313")).thenReturn(Collections.emptyList());

        service.register("B232270036", "F.CSA313");

        InOrder inOrder = Mockito.inOrder(repository, emailSender);
        inOrder.verify(repository).findStudent("B232270036");
        inOrder.verify(repository).saveRegistration("B232270036", "F.CSA313");
        verify(repository, times(1)).saveRegistration("B232270036", "F.CSA313");
        verify(repository, times(1)).courseExists("F.CSA313");
        assertNotNull(service);
    }
}
