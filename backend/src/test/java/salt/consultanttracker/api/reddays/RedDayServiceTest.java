package salt.consultanttracker.api.reddays;

import salt.consultanttracker.api.ApplicationTestConfig;
import salt.consultanttracker.api.client.nager.NagerClient;
import salt.consultanttracker.api.consultant.ConsultantService;
import salt.consultanttracker.api.reddays.dto.RedDaysResponseDto;
import salt.consultanttracker.api.utils.Utilities;
import lombok.SneakyThrows;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = ApplicationTestConfig.class)
class RedDayServiceTest {
    private RedDayService redDaysService;
    @Mock
    private NagerClient workingDaysClient;
    @Mock
    private ConsultantService consultantService;
    @Mock
    private RedDayRepository redDayRepository;
    private MockedStatic<Utilities> mockUtilities;


    @BeforeEach
    public void setUp() {
        this.redDaysService = new RedDayService(consultantService, redDayRepository, workingDaysClient);
    }
    @Test
    @DisplayName("getRedDays")
    public void givenCountryCodeSE__whenGetRedDays__then31December2023() {
        Mockito.when(redDayRepository.findAllById_CountryCode("SE"))
                .thenReturn(RedDayServiceMockedData.createMockedRedDayList());
        List<LocalDate> actualResult = redDaysService.getRedDays("SE");
        assertEquals(2, actualResult.size());
        assertEquals(LocalDate.parse("2023-12-31"), actualResult.getFirst());
    }

    @Test
    @DisplayName("getRedDays")
    public void givenCountryCodeNO__whenGetRedDays__then1Jan2024And24Dec2024() {
        var expectedResult = Lists.newArrayList(LocalDate.parse("2024-01-01"), LocalDate.parse("2024-12-24"));
        Mockito.when(redDayRepository.findAllById_CountryCode("NO"))
                .thenReturn(RedDayServiceMockedData.createMockedRedDaysList());
        List<LocalDate> actualResult = redDaysService.getRedDays("NO");
        assertEquals(2, actualResult.size());
        assertEquals(expectedResult.get(1), actualResult.get(1));
    }

    @Test
    @DisplayName("isRedDay")
    public void given2Jan2024__whenIsRedDay__thenFalse() {
        Mockito.lenient().when(consultantService.getCountryCodeByConsultantId(
                        UUID.fromString("45ec353f-b0f5-4a51-867e-8d0d84d11573")))
                .thenReturn("SE");
        Mockito.lenient().when(redDayRepository.findAllById_CountryCode("SE"))
                .thenReturn(RedDayServiceMockedData.createMockedRedDayList());
        boolean actualResult = redDaysService.isRedDay(
                LocalDate.parse("2024-01-02"),
                UUID.fromString("45ec353f-b0f5-4a51-867e-8d0d84d11573"));
        assertFalse(actualResult);
    }

    @Test
    @DisplayName("removeNonWorkingDays")
    public void given0RemainingDays__whenRemoveNonWorkingDays__thenReturnStartDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime actualResult = redDaysService.removeNonWorkingDays(
                LocalDateTime.parse("2024-01-01 00:00:00", formatter),
                0,
                UUID.fromString("45ec353f-b0f5-4a51-867e-8d0d84d11573"));
        assertEquals(LocalDateTime.parse("2024-01-01 00:00:00", formatter), actualResult);
    }

    @Test
    @DisplayName("removeNonWorkingDays")
    public void givenNegativeRemainingDays__whenRemoveNonWorkingDays__thenReturnStartDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime actualResult = redDaysService.removeNonWorkingDays(
                LocalDateTime.parse("2024-02-01 00:00:00", formatter),
                -30,
                UUID.fromString("45ec353f-b0f5-4a51-867e-8d0d84d11573"));
        assertEquals(LocalDateTime.parse("2024-02-01 00:00:00", formatter), actualResult);
    }


    @Test
    @DisplayName("removeNonWorkingDays")
    public void given5RemainingDaysAnd3NonWorkingDays__whenRemoveNonWorkingDays__thenReturn8Jan2024() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        Mockito.lenient().when(consultantService.getCountryCodeByConsultantId(
                        UUID.fromString("45ec353f-b0f5-4a51-867e-8d0d84d11573")))
                .thenReturn("Sverige");
        Mockito.lenient().when(redDayRepository.findAllById_CountryCode("SE"))
                .thenReturn(RedDayServiceMockedData.createMockedRedDayList());
        mockUtilities = mockStatic(Utilities.class);
        mockUtilities.when(() -> Utilities.isWeekend(6)).thenReturn(true);
        mockUtilities.when(() -> Utilities.isWeekend(7)).thenReturn(true);
        mockUtilities.close();

        LocalDateTime expectedResult = LocalDateTime.parse("2024-01-08 23:59:59", formatter);
        LocalDateTime actualResult = redDaysService.removeNonWorkingDays(
                LocalDateTime.parse("2024-01-01 00:00:00", formatter),
                5,
                UUID.fromString("45ec353f-b0f5-4a51-867e-8d0d84d11573"));
        assertEquals(expectedResult, actualResult);
    }

    @Test
    @DisplayName("checkRedDaysOrWeekend")
    public void given15DaysBetweenAndStartDay2Jan2024AndMultipleCheck__whenCheckRedDaysOrWeekend__thenReturn5Days() {
        Mockito.lenient().when(consultantService.getCountryCodeByConsultantId(
                        UUID.fromString("45ec353f-b0f5-4a51-867e-8d0d84d11573")))
                .thenReturn("Sverige");
        Mockito.lenient().when(redDayRepository.findAllById_CountryCode("SE"))
                .thenReturn(RedDayServiceMockedData.createMockedRedDayList());
        mockUtilities = mockStatic(Utilities.class);
        mockUtilities.when(() -> Utilities.isWeekend(6)).thenReturn(true);
        mockUtilities.when(() -> Utilities.isWeekend(7)).thenReturn(true);
        mockUtilities.close();

        int actualResult = redDaysService.checkRedDaysOrWeekend(
                15L,
                LocalDate.parse("2023-12-31"),
                UUID.fromString("45ec353f-b0f5-4a51-867e-8d0d84d11573"),
                "multiple check");
        assertEquals(5, actualResult);
    }

    @Test
    @DisplayName("checkRedDaysOrWeekend")
    public void given15DaysBetweenAndStartDay31DecAndSingleCheck__whenCheckRedDaysOrWeekend__thenReturn1Day() {
        Mockito.lenient().when(consultantService.getCountryCodeByConsultantId(
                        UUID.fromString("45ec353f-b0f5-4a51-867e-8d0d84d11573")))
                .thenReturn("Sverige");
        Mockito.lenient().when(redDayRepository.findAllById_CountryCode("SE"))
                .thenReturn(RedDayServiceMockedData.createMockedRedDayList());
        mockUtilities = mockStatic(Utilities.class);
        mockUtilities.when(() -> Utilities.isWeekend(6)).thenReturn(true);
        mockUtilities.when(() -> Utilities.isWeekend(7)).thenReturn(true);
        mockUtilities.close();

        int actualResult = redDaysService.checkRedDaysOrWeekend(
                15L,
                LocalDate.parse("2023-12-31"),
                UUID.fromString("45ec353f-b0f5-4a51-867e-8d0d84d11573"),
                "single check");
        assertEquals(1, actualResult);
    }

    @Test
    @DisplayName("getAllRedDays")
    public void givenCountryCodes__whenGetAllRedDays__thenShouldReturn2Lists(){
        Mockito.lenient().when(redDayRepository.findAllById_CountryCode("SE"))
                .thenReturn(List.of(RedDayServiceMockedData.createMockedRedDaysFromRepositorySE()));
        Mockito.lenient().when(redDayRepository.findAllById_CountryCode("NO"))
                .thenReturn(List.of(RedDayServiceMockedData.createMockedRedDaysFromRepositoryNO()));
        RedDaysResponseDto actualResult = redDaysService.getAllRedDays();
        assertEquals(1, actualResult.redDaysSE().size());
        assertEquals(1, actualResult.redDaysNO().size());
        assertEquals(LocalDate.parse("2024-12-25"), actualResult.redDaysSE().get(0));
        assertEquals(LocalDate.parse("2024-12-24"), actualResult.redDaysNO().get(0));
    }


    //----------------------------- PRIVATE METHODS TESTS ---------------------------------

    @Test
    @SneakyThrows
    @DisplayName("getCountryCode")
    public void givenSweden_whenGetCountryCode_thenSE() {
        var redDaysServiceInstance = redDaysService;
        var getCountryCode = redDaysServiceInstance.getClass().getDeclaredMethod("getCountryCode", UUID.class);
        getCountryCode.setAccessible(true);
        Mockito.when(consultantService.getCountryCodeByConsultantId(
                        UUID.fromString("45ec353f-b0f5-4a51-867e-8d0d84d11573")))
                .thenReturn("Sverige");
        Assertions.assertEquals("SE",
                getCountryCode.invoke(redDaysServiceInstance,
                        UUID.fromString("45ec353f-b0f5-4a51-867e-8d0d84d11573")));
    }
}