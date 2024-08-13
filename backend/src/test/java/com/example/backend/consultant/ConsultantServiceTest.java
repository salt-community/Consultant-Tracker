package com.example.backend.consultant;

import com.example.backend.ApplicationTestConfig;
import com.example.backend.ObjectConstructor;
import com.example.backend.client.timekeeper.TimekeeperClient;
import com.example.backend.client.timekeeper.dto.TimekeeperUserDto;
import com.example.backend.consultant.dto.ConsultantResponseListDto;
import com.example.backend.registeredTime.MockedRegisteredTimeService;
import com.example.backend.registeredTime.RegisteredTimeService;
import com.example.backend.tag.Tag;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = ApplicationTestConfig.class)
class ConsultantServiceTest extends ApplicationTestConfig {
    @MockBean
    TimekeeperClient mockedTkClient;
    @MockBean
    ConsultantRepository mockedConsultantRepo;
    @MockBean
    RegisteredTimeService mockedRegisteredTimeService;
    @InjectMocks
    ConsultantService consultantService;
    private static Consultant mockedConsultant1;
    private static Consultant mockedConsultant2;
    private static Consultant mockedConsultant3;

    @BeforeEach
    void setUpBeforeEach() {
        // mock consultants
        mockedConsultant1 = new Consultant(
                UUID.fromString("68c670d6-3038-4fca-95be-2669aaf0b549"),
                "John Doe",
                "john.doe@gmail.com",
                null,
                1111L,
                "Jane Doe",
                "H&M",
                "Sverige",
                true);
        mockedConsultant2 = new Consultant(
                UUID.fromString("0239ceac-5e65-40a6-a949-5492c22b22e3"),
                "John Doe2",
                "john.doe2@gmail.com",
                null,
                2222L,
                "Jane Doe2",
                "H&M",
                "Sverige",
                true);
        mockedConsultant3 = new Consultant(
                UUID.fromString("1239cead-5e65-40a6-a949-5492c22b22a4"),
                "John Doe3",
                "john.doe3@gmail.com",
                null,
                3333L,
                "Jane Doe3",
                "H&M",
                "Sverige",
                true);
    }

    @AfterEach
    void setUpAfterEach() {
        MockedConsultantService.clearList();
    }

    @Test
    void resultShouldNotBeEmptyList() {
        /* ARRANGE */
        List<Consultant> mockedList = List.of(mockedConsultant1, mockedConsultant2);
        Mockito.when(mockedConsultantRepo.findAll()).thenReturn(mockedList);

        /* ACT */
        List<Consultant> consultantList = consultantService.getAllConsultants();

        /* ASSERT */
        assertFalse(consultantList.isEmpty());
    }

    @Test
    void getCountryCodeByConsultantId() {
        /* ARRANGE */
        Mockito.when(mockedConsultantRepo.findCountryById(any(UUID.class))).thenReturn("Sverige");
        String expectedResult = "Sverige";

        /* ACT */
        String result = consultantService.getCountryCodeByConsultantId(UUID.fromString("223152ac-6af6-4a4b-b86b-53c0707f433c"));

        /* ASSERT */
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldReturnConsultantResponseListDto() {
        /* ARRANGE */
        Page<Consultant> pageableConsultantsList = new PageImpl<>(List.of(mockedConsultant1));
        Mockito.when(mockedRegisteredTimeService
                .getConsultantTimelineItems(any(Consultant.class))).thenReturn(MockedRegisteredTimeService.getConsultantTimelineItemsMocked(mockedConsultant1));
        int expectedConsultantsFound = 1;

        /* ARRANGE FOR HELPER METHOD  getAllConsultantsPageable() */
        Mockito.when(mockedConsultantRepo.findAllByActiveTrueAndFilterByName(anyString(), any(Pageable.class))).thenReturn(pageableConsultantsList);

        /* ARRANGE FOR HELPER METHOD registeredTimeService.fetchAndSaveTimeRegisteredByConsultant() */
        Mockito.when(mockedRegisteredTimeService.fetchAndSaveTimeRegisteredByConsultant()).then()
        mockedFetchAndSaveTimeRegisteredByConsultant
        /* ACT */
        ConsultantResponseListDto mockedResult = consultantService.getAllConsultantDtos(0, 8, "mockJohn", "mockPt", "mockClient");

        /* ASSERT */
        assertEquals(expectedConsultantsFound, mockedResult.consultants().size());
    }

    @Test
    void shouldReturn2ActiveConsultants() {
        /* ARRANGE */
        Mockito.when(mockedConsultantRepo.findAllByActiveTrue()).thenReturn(List.of(mockedConsultant1, mockedConsultant2));
        int expectedListSize = 2;

        /* ACT */
        int actualListSize = consultantService.getAllActiveConsultants().size();

        /* ASSERT */
        assertEquals(expectedListSize, actualListSize);
    }

    @Test
    @SneakyThrows
    void shouldAddConsultantToList() {
        /* ARRANGE */
        var consultantServiceClass = new ConsultantService(mockedConsultantRepo, mockedTkClient, mockedRegisteredTimeService);
        var createConsultantMethod = consultantServiceClass.getClass().getDeclaredMethod("createConsultant", Consultant.class);
        createConsultantMethod.setAccessible(true);
        var listSizeBefore = MockedConsultantService.mockedGetConsultantsList().size();
        Mockito.when(mockedConsultantRepo.save(any(Consultant.class)))
                .thenReturn(MockedConsultantService.mockedCreateConsultant(mockedConsultant1));

        /* ACT */
        createConsultantMethod.invoke(consultantServiceClass, any(Consultant.class));

        /* ASSERT */
        var listSizeAfter = MockedConsultantService.mockedGetConsultantsList().size();
        int actualResult = listSizeAfter - listSizeBefore;
        assertEquals(1, actualResult);
    }

    @Test
    @SneakyThrows
    void should_UpdateConsultantActiveStatus_ToFalse() {
        /* ARRANGE */
        List<Consultant> mockedList = List.of(mockedConsultant1, mockedConsultant2, mockedConsultant3);
        for (var consultant : mockedList) {
            MockedConsultantService.mockedCreateConsultant(consultant);
        }

        UUID expectedResult = mockedConsultant3.getId();
        mockedConsultant3.setActive(false);

        Mockito.when(mockedConsultantRepo.save(any(Consultant.class)))
                .thenReturn(MockedConsultantService.mockedUpdateConsultant(mockedConsultant3));

        var consultantServiceClass = new ConsultantService(mockedConsultantRepo, mockedTkClient, mockedRegisteredTimeService);
        var updateIsActiveForExistingConsultantMethod = consultantServiceClass
                .getClass()
                .getDeclaredMethod("updateIsActiveForExistingConsultant", TimekeeperUserDto.class);
        updateIsActiveForExistingConsultantMethod.setAccessible(true);

        /* ARRANGE FOR HELPER METHOD getAllConsultants() */
        Mockito.when(mockedConsultantRepo.findAll()).thenReturn(MockedConsultantService.mockedGetConsultantsList());

        /* ACT */
        updateIsActiveForExistingConsultantMethod.invoke(
                consultantServiceClass,
                ObjectConstructor.convertConsultantToTimekeeperUserDto(mockedConsultant3));

        /* ASSERT */
        UUID actualResult = MockedConsultantService.mockedGetConsultantsList()
                .stream()
                .filter(c -> !c.isActive()).toList().get(0).getId();
        System.out.println("MockedConsultantService.mockedGetConsultantsList() = " + MockedConsultantService.mockedGetConsultantsList());
        assertEquals(expectedResult, actualResult);
    }

    /* this test might not be useful if the
     responsible PT is taken from Lucca later on */
    @Test
    void shouldUpdateResponsiblePtForAllConsultants() {
        /* ARRANGE */
        Mockito.when(mockedConsultantRepo.save(any(Consultant.class)))
                .thenReturn(MockedConsultantService.mockedCreateConsultant(mockedConsultant1));
        String possibleExpected1 = "Josefin Stål";
        String possibleExpected2 = "Anna Carlsson";

        /* ARRANGE FOR HELPER METHOD getAllActiveConsultants() */
        Mockito.when(mockedConsultantRepo.findAllByActiveTrue()).thenReturn(List.of(mockedConsultant1));

        /* ACT */
        consultantService.fillClientAndResponsiblePt();

        /* ASSERT */
        List<Consultant> resultList = MockedConsultantService.mockedGetConsultantsList();
        Consultant actualResult = resultList.stream().filter(c -> (c.getId() == mockedConsultant1.getId())
                && (c.getResponsiblePT().equals(possibleExpected1) || c.getResponsiblePT().equals(possibleExpected2))).toList().get(0);
        assertNotNull(actualResult);
        assertEquals(mockedConsultant1.getId(), actualResult.getId());
    }

    @Test
    @SneakyThrows
    void should_AddNewConsultant_FromTimekeeperUser() {
        /* ARRANGE */
        int listSizeBeforeAdding = MockedConsultantService.mockedGetConsultantsList().size();
        List<TimekeeperUserDto> mockedListTimekeeperUser = ObjectConstructor.getListOfTimekeeperUserDto(1);
        Mockito.when(mockedConsultantRepo.existsByTimekeeperId(anyLong())).thenReturn(false);
        try (MockedStatic<Tag> mockTag = mockStatic(Tag.class)) {
            mockTag.when(() -> Tag.extractCountryTagFromTimekeeperUserDto(any(TimekeeperUserDto.class))).thenReturn("Sverige");
        }
        var consultantServiceClass = new ConsultantService(mockedConsultantRepo, mockedTkClient, mockedRegisteredTimeService);
        var updateConsultantTableMethod = consultantServiceClass.getClass().getDeclaredMethod("updateConsultantTable", List.class);
        updateConsultantTableMethod.setAccessible(true);

        /* ARRANGE FOR HELPER METHOD createConsultant() */
        Mockito.when(mockedConsultantRepo.save(any(Consultant.class)))
                .thenReturn(MockedConsultantService.mockedCreateConsultant(
                        ObjectConstructor.convertTimekeeperUserDtoToConsultant(mockedListTimekeeperUser.getFirst())
                ));

        /* ACT */
        updateConsultantTableMethod.invoke(consultantServiceClass, mockedListTimekeeperUser);

        /* ASSERT */
        int listSizeAfterAdding = MockedConsultantService.mockedGetConsultantsList().size();
        assertEquals(1, listSizeAfterAdding - listSizeBeforeAdding);
    }

    @Test
    @SneakyThrows
    void should_UpdateActiveStatus_ForTimekeeperUser_ToFalse() {
        /* ARRANGE */
        MockedConsultantService.mockedCreateConsultant(mockedConsultant1);
        boolean statusBeforeChange = MockedConsultantService.mockedGetConsultantsList().getFirst().isActive();
        mockedConsultant1.setActive(false);
        List<TimekeeperUserDto> mockedListTimekeeperUser = List.of(
                ObjectConstructor.convertConsultantToTimekeeperUserDto(mockedConsultant1));

        Mockito.when(mockedConsultantRepo.existsByTimekeeperId(anyLong())).thenReturn(true);
        Mockito.when(mockedConsultantRepo.findAll()).thenReturn(MockedConsultantService.mockedGetConsultantsList());
        var consultantServiceClass = new ConsultantService(mockedConsultantRepo, mockedTkClient, mockedRegisteredTimeService);
        var updateConsultantTableMethod = consultantServiceClass.getClass().getDeclaredMethod("updateConsultantTable", List.class);
        updateConsultantTableMethod.setAccessible(true);

        /* ARRANGE FOR HELPER METHOD createConsultant() */
        Mockito.when(mockedConsultantRepo.save(any(Consultant.class)))
                .thenReturn(MockedConsultantService.mockedUpdateConsultant(mockedConsultant1));

        /* ACT */
        updateConsultantTableMethod.invoke(consultantServiceClass, mockedListTimekeeperUser);

        /* ASSERT */
        boolean statusAfterChange = MockedConsultantService.mockedGetConsultantsList().getFirst().isActive();
        assertNotEquals(statusBeforeChange, statusAfterChange);
    }

    @Test
    @SneakyThrows
    void should_NotUpdateActiveStatus_ForTimekeeperUser() {
        /* ARRANGE */
        MockedConsultantService.mockedCreateConsultant(mockedConsultant1);
        boolean statusBeforeChange = MockedConsultantService.mockedGetConsultantsList().getFirst().isActive();
        List<TimekeeperUserDto> mockedListTimekeeperUser = List.of(
                ObjectConstructor.convertConsultantToTimekeeperUserDto(mockedConsultant1));

        Mockito.when(mockedConsultantRepo.existsByTimekeeperId(anyLong())).thenReturn(true);
        var consultantServiceClass = new ConsultantService(mockedConsultantRepo, mockedTkClient, mockedRegisteredTimeService);
        var updateConsultantTableMethod = consultantServiceClass.getClass().getDeclaredMethod("updateConsultantTable", List.class);
        updateConsultantTableMethod.setAccessible(true);

        /* ARRANGE FOR HELPER METHOD updateIsActiveForExistingConsultant() */
        Mockito.when(mockedConsultantRepo.findAll()).thenReturn(MockedConsultantService.mockedGetConsultantsList());
        Mockito.when(mockedConsultantRepo.save(any(Consultant.class)))
                .thenReturn(MockedConsultantService.mockedUpdateConsultant(mockedConsultant1));

        /* ACT */
        updateConsultantTableMethod.invoke(consultantServiceClass, mockedListTimekeeperUser);

        /* ASSERT */
        boolean statusAfterChange = MockedConsultantService.mockedGetConsultantsList().getFirst().isActive();
        assertEquals(statusBeforeChange, statusAfterChange);
    }

    @Test
    @SneakyThrows
    void should_AddNewConsultant_ForTimekeeperUser_FetchDataFromTimekeeper() {
        /* ARRANGE */
        var consultantServiceClass = new ConsultantService(mockedConsultantRepo, mockedTkClient, mockedRegisteredTimeService);
        List<TimekeeperUserDto> mockedTkList = ObjectConstructor.getListOfTimekeeperUserDto(1);
        Mockito.when(mockedTkClient.getUsers()).thenReturn(mockedTkList);
        System.out.println("mockedTkList = " + mockedTkList);

        /* ARRANGE FOR HELPER METHOD - updateConsultantTable() */
        Mockito.when(mockedConsultantRepo.existsByTimekeeperId(anyLong())).thenReturn(true);
        try (MockedStatic<Tag> mockTag = mockStatic(Tag.class)) {
            mockTag.when(() -> Tag.extractCountryTagFromTimekeeperUserDto(any(TimekeeperUserDto.class))).thenReturn("Sverige");
        }
        /* ARRANGE FOR HELPER TO THE HELPER METHOD - createConsultant() */
        Mockito.when(mockedConsultantRepo.save(any(Consultant.class)))
                .thenReturn(MockedConsultantService.mockedCreateConsultant(ObjectConstructor.convertTimekeeperUserDtoToConsultant(mockedTkList.getFirst())));

        /* ASSERT */
        assertTrue(false);
    }

}