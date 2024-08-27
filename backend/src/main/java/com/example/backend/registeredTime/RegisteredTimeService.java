package com.example.backend.registeredTime;

import com.example.backend.client.timekeeper.TimekeeperClient;
import com.example.backend.client.timekeeper.dto.TimekeeperRegisteredTimeResponseDto;
import com.example.backend.consultant.Consultant;
import com.example.backend.consultant.ConsultantService;

import com.example.backend.consultant.dto.ConsultantTimeDto;
import com.example.backend.consultant.dto.TotalDaysStatisticsDto;
import com.example.backend.redDay.RedDayService;
import com.example.backend.registeredTime.dto.RemainingDaysDto;
import com.example.backend.timeChunks.TimeChunks;
import com.example.backend.timeChunks.TimeChunksKey;
import com.example.backend.utils.Utilities;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.example.backend.client.timekeeper.Activity.*;

@Service
public class RegisteredTimeService {
    private final RegisteredTimeRepository registeredTimeRepository;
    private final ConsultantService consultantService;
    private final TimekeeperClient timekeeperClient;
    private final RedDayService redDaysService;

    public RegisteredTimeService(RegisteredTimeRepository registeredTimeRepository,
                                 @Lazy ConsultantService consultantService,
                                 TimekeeperClient timekeeperClient,
                                 RedDayService redDaysService) {
        this.registeredTimeRepository = registeredTimeRepository;
        this.consultantService = consultantService;
        this.timekeeperClient = timekeeperClient;
        this.redDaysService = redDaysService;
    }

    //-----------------------------COVERED BY TESTS ---------------------------------
    public List<RegisteredTime> getTimeByConsultantId(UUID id) {
        return registeredTimeRepository.findAllById_ConsultantIdOrderById_StartDateAsc(id);
    }

    //-----------------------------COVERED BY TESTS ---------------------------------
    public String getCurrentClient(UUID consultantId) {
        RegisteredTime lastWorkTimeRegistered = registeredTimeRepository
                .findFirstById_ConsultantIdOrderByEndDateDesc(consultantId);
        if (lastWorkTimeRegistered != null) {
            return lastWorkTimeRegistered.getProjectName();
        }
        return PGP.activity;
    }

    //-----------------------------------MOVE TO SEPARATE SERVICE--------------------------------------------
    public void fetchAndSaveTimeRegisteredByConsultantDB() {
        List<Consultant> activeConsultants = consultantService.getAllActiveConsultants();
        for (Consultant consultant : activeConsultants) {
            List<ConsultantTimeDto> consultantRegisteredTime = fetchTimeFromTimekeeperDB(consultant.getId(), consultant.getTimekeeperId());
            consultantRegisteredTime = filterOutIncorrectlyRegisteredTimeDB(consultantRegisteredTime);
            saveConsultantTimeDB(consultantRegisteredTime);
        }
    }

    private List<ConsultantTimeDto> fetchTimeFromTimekeeperDB(UUID consultantId, Long timekeeperId) {
        List<TimekeeperRegisteredTimeResponseDto> consultancyTime = timekeeperClient.getTimeRegisteredByConsultant(timekeeperId);
        List<ConsultantTimeDto> consultantTimeDtoList = new ArrayList<>();
        for (TimekeeperRegisteredTimeResponseDto item : consultancyTime) {
            consultantTimeDtoList.add(new ConsultantTimeDto(
                    new RegisteredTimeKey(consultantId, item.date().withHour(0).withMinute(0).withSecond(0)),
                    item.date().withHour(23).withMinute(59).withSecond(59),
                    item.activityName(),
                    item.totalHours(),
                    item.projectName()));
        }
        return consultantTimeDtoList;
    }

    //-----------------------------COVERED BY TESTS ---------------------------------
    public List<ConsultantTimeDto> filterOutIncorrectlyRegisteredTimeDB(List<ConsultantTimeDto> consultantRegisteredTime) {
        return consultantRegisteredTime
                .stream()
                .filter(el -> el.totalHours() != 0
                        || (!el.dayType().equals(CONSULTANCY_TIME.activity)
                        && !el.dayType().equals(OWN_ADMINISTRATION.activity)))
                .toList();
    }

    private void saveConsultantTimeDB(List<ConsultantTimeDto> consultantTimeDtoList) {
        Map<LocalDate, ConsultantTimeDto> timelineItemsMap = createTimeItemsDB(consultantTimeDtoList);
        List<ConsultantTimeDto> timeToSave = new ArrayList<>(timelineItemsMap.values());
        for (ConsultantTimeDto t : timeToSave) {
            registeredTimeRepository
                    .save(new RegisteredTime(
                            new RegisteredTimeKey(t.itemId().getConsultantId(),
                                    t.itemId().getStartDate()),
                            t.dayType(),
                            t.endDate().withHour(23).withMinute(59).withSecond(59),
                            Math.round(t.totalHours() * 10.0) / 10.0,
                            t.projectName()
                    ));
        }
    }

    private Map<LocalDate, ConsultantTimeDto> createTimeItemsDB(List<ConsultantTimeDto> consultantTimeDtoList) {
        Map<LocalDate, ConsultantTimeDto> timeItemsMap = new HashMap<>();
        consultantTimeDtoList.forEach(consultantTimeDto -> {
            LocalDate startDate = consultantTimeDto.itemId().getStartDate().toLocalDate();
            if (timeItemsMap.containsKey(startDate)) {
                timeItemsMap.compute(startDate, (k, v) ->
                        new ConsultantTimeDto(
                                consultantTimeDto.itemId(),
                                consultantTimeDto.endDate(),
                                consultantTimeDto.dayType(),
                                consultantTimeDto.totalHours() + timeItemsMap.get(startDate).totalHours(),
                                consultantTimeDto.projectName()));
            } else {
                timeItemsMap.put(startDate, consultantTimeDto);
            }
        });
        return timeItemsMap;
    }


    public TotalDaysStatisticsDto getAllDaysStatistics(UUID id) {
        String country = consultantService.getCountryCodeByConsultantId(id);
        int totalWorkedDays = countTotalWorkedDays(id);
        double totalRemainingDays = Utilities.getTotalDaysByCountry(country);
        double totalRemainingHours = Utilities.getTotalHours(country);
        double totalWorkedHours = 0.0;
        if (totalWorkedDays != 0) {
            totalWorkedHours = countTotalWorkedHours(id);
            totalRemainingDays = Utilities.roundToTwoDecimalPoints(Utilities.countRemainingDays(totalWorkedHours, country));
            totalRemainingHours = Utilities.roundToOneDecimalPoint(totalRemainingDays * Utilities.getStandardWorkingHours(country));
        }
        int totalVacationDays = registeredTimeRepository.countAllById_ConsultantIdAndTypeIs(id, VACATION.activity).orElse(0);
        return new TotalDaysStatisticsDto(totalRemainingDays, totalWorkedDays, totalVacationDays, totalRemainingHours, Utilities.roundToOneDecimalPoint(totalWorkedHours));
    }

    //-----------------------------COVERED BY TESTS ---------------------------------
    public Double countTotalWorkedHours(UUID consultantId) {
        Double getTotalConsultancyHour = registeredTimeRepository.getSumOfTotalHoursByConsultantIdAndType(consultantId, CONSULTANCY_TIME.activity).orElse(0.0);
        Double getTotalAdministrationHour = registeredTimeRepository.getSumOfTotalHoursByConsultantIdAndType(consultantId, OWN_ADMINISTRATION.activity).orElse(0.0);
        return getTotalAdministrationHour + getTotalConsultancyHour;
    }

    private int countTotalWorkedDays(UUID consultantId) {
        int countOfWorkedDays = registeredTimeRepository.countAllById_ConsultantIdAndTypeIs(consultantId, CONSULTANCY_TIME.activity).orElse(0);
        countOfWorkedDays += registeredTimeRepository.countAllById_ConsultantIdAndTypeIs(consultantId, OWN_ADMINISTRATION.activity).orElse(0);
        return countOfWorkedDays;
    }

    //-----------------------------COVERED BY TESTS ---------------------------------
    public List<ConsultantTimeDto> getAllConsultantsTimeItems() {
        consultantService.fetchDataFromTimekeeper();
        List<Consultant> consultants = consultantService.getAllConsultants();
        List<RegisteredTime> consultantTimeDtoList = new ArrayList<>();
        for (Consultant consultant : consultants) {
            List<RegisteredTime> timeByConsultantId = getTimeByConsultantId(consultant.getId());
            consultantTimeDtoList.addAll(timeByConsultantId);
        }
        return consultantTimeDtoList
                .stream()
                .map(ConsultantTimeDto::toConsultantTimeDto)
                .toList();
    }

    public TimeChunks getRemainingConsultancyTimeByConsultantId(UUID consultantId) {
        LocalDateTime lastRegisteredDate = registeredTimeRepository.findFirstById_ConsultantIdOrderByEndDateDesc(consultantId).getEndDate();
        LocalDateTime startDate = lastRegisteredDate.plusDays(1).withHour(0).withMinute(0).withSecond(0);
        RemainingDaysDto estimatedEndDate = getEstimatedConsultancyEndDate(consultantId, startDate);
        if (estimatedEndDate.endDate() == startDate) {
            return null;
        }
        return new TimeChunks(
                new TimeChunksKey(consultantId, startDate),
                "Remaining Days",
                estimatedEndDate.endDate(),
                estimatedEndDate.remainingDays(),
                "Remaining Days");
    }

    private RemainingDaysDto getEstimatedConsultancyEndDate(UUID consultantId, LocalDateTime startDate) {
        double totalWorkedHours = countTotalWorkedHours(consultantId);
        String countryCode = consultantService.getCountryCodeByConsultantId(consultantId);
        int remainingConsultancyDays = (int) Utilities.countRemainingDays(totalWorkedHours, countryCode);
        if (remainingConsultancyDays <= 0) {
            return new RemainingDaysDto(startDate, remainingConsultancyDays);
        }
        return new RemainingDaysDto(redDaysService.removeNonWorkingDays(startDate, remainingConsultancyDays, consultantId), remainingConsultancyDays);
    }

    public List<String> getClientsByConsultantId(UUID consultantId) {
        return registeredTimeRepository.findDistinctProjectNameBydId_ConsultantId(consultantId);
    }

    public LocalDate getStartDateByClientAndConsultantId(String client, UUID consultantId) {
        return registeredTimeRepository.findFirstByProjectNameAndId_ConsultantIdOrderById_StartDateAsc(client, consultantId)
                .getId().getStartDate().toLocalDate();
    }

    public LocalDate getEndDateByClientAndConsultantId(String client, UUID consultantId) {
        return registeredTimeRepository.findFirstByProjectNameAndId_ConsultantIdOrderByEndDateDesc(client, consultantId)
                .getEndDate().toLocalDate();
    }
}