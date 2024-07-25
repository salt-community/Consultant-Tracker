package com.example.backend.consultant;

import com.example.backend.client.TimekeeperClient;
import com.example.backend.client.dto.TimekeeperRegisteredTimeResponseDto;
import com.example.backend.client.dto.TimekeeperUserDto;
import com.example.backend.consultant.dto.ConsultantResponseDto;
import com.example.backend.consultant.dto.ConsultantResponseListDto;
import com.example.backend.consultant.dto.ConsultantTimeDto;
import com.example.backend.exceptions.ConsultantNotFoundException;
import com.example.backend.registeredTime.RegisteredTime;
import com.example.backend.registeredTime.RegisteredTimeKey;
import com.example.backend.registeredTime.RegisteredTimeService;
import com.example.backend.registeredTime.dto.RegisteredTimeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.backend.client.Activity.CONSULTANCY_TIME;


@Service
@RequiredArgsConstructor
public class ConsultantService {
    private final ConsultantRepository consultantRepository;
    private final TimekeeperClient timekeeperClient;
    private final RegisteredTimeService registeredTimeService;

    public ConsultantResponseListDto getAllConsultantDtos(int page, int pageSize) {
        Page<Consultant> consultantsList = getAllConsultantsPageable(page, pageSize);
        List<ConsultantResponseDto> consultants = consultantsList.stream().map(el-> findConsultantDtoById(el.getId())).toList();
        return new ConsultantResponseListDto(
                page,
                consultantsList.getTotalPages(),
                consultantsList.getTotalElements(),
                consultants);

    }
    public List<RegisteredTimeDto> getConsultantTimeDto(List<RegisteredTime> consultantTimeDtoList) {
        List<RegisteredTimeDto> listOfRegisteredTime = new ArrayList<>();
        AtomicReference<String> activityTypePrev = new AtomicReference<>(CONSULTANCY_TIME.activity);
        AtomicReference<LocalDateTime> startTime = new AtomicReference<>(null);
        AtomicReference<LocalDateTime> endTime = new AtomicReference<>();
        AtomicInteger countDays = new AtomicInteger();

        for (int i = 0; i < consultantTimeDtoList.size(); i++) {
            RegisteredTime consultantTimeDtoEl = consultantTimeDtoList.get(i);
            if (consultantTimeDtoEl.getType().equals(activityTypePrev.get())) {
                if (startTime.get() == null) {
                    startTime.set(consultantTimeDtoEl.getId().getStartDate());
                } else {
                    if (endTime.get().getDayOfMonth() != consultantTimeDtoEl.getId().getStartDate().getDayOfMonth() ||
                            endTime.get().getMonth() != consultantTimeDtoEl.getId().getStartDate().getMonth() ||
                            endTime.get().getYear() != consultantTimeDtoEl.getId().getStartDate().getYear()
                    ) {
                        countDays.getAndIncrement();
                    }
                }
                endTime.set(consultantTimeDtoEl.getEndDate());
                activityTypePrev.set(consultantTimeDtoEl.getType());
                if (i == consultantTimeDtoList.size() - 1) {
                    listOfRegisteredTime.add(new RegisteredTimeDto(
                            UUID.randomUUID(),
                            startTime.get(),
                            consultantTimeDtoEl.getEndDate(),
                            consultantTimeDtoEl.getType()));
                }
            } else {
                listOfRegisteredTime.add(new RegisteredTimeDto(
                        UUID.randomUUID(),
                        startTime.get(),
                        endTime.get(),
                       activityTypePrev.get()));
                countDays.set(0);
                startTime.set(consultantTimeDtoEl.getId().getStartDate());
                endTime.set(consultantTimeDtoEl.getEndDate());
                activityTypePrev.set(consultantTimeDtoEl.getType());

            }
        }
        return listOfRegisteredTime;
    }

    public Page<Consultant> getAllConsultantsPageable(int page, int pageSize) {
        Pageable pageRequest = PageRequest.of(page, pageSize);
       return consultantRepository.findAllByActiveTrue(pageRequest);
    }

    public List<Consultant> getAllConsultants() {
        return consultantRepository.findAll();
    }

    //    @Scheduled(cron = "0 0 0 * * *")
    public void fetchDataFromTimekeeper() {
        List<TimekeeperUserDto> timekeeperUserDto = timekeeperClient.getUsers();
        assert timekeeperUserDto != null;
        List<Long> timekeeperIdsToAdd = checkTimekeeperUsersWithDatabase(timekeeperUserDto);
        if (!timekeeperIdsToAdd.isEmpty()) {
            timekeeperIdsToAdd.forEach(id -> {
                TimekeeperUserDto tkUser = timekeeperUserDto.stream()
                        .filter(u -> Objects.equals(u.id(), id)).findFirst().orElse(null);
                if (tkUser != null) {
                    Consultant consultant = new Consultant(
                            UUID.randomUUID(),
                            tkUser.firstName().trim().concat(" ").concat(tkUser.lastName().trim()),
                            tkUser.email(),
                            tkUser.phone(),
                            id,
                            tkUser.isActive()
                    );
                    createConsultant(consultant);
                }
            });
        }
        fetchRecordedTimeForConsultant();
    }

    private void fetchRecordedTimeForConsultant() {
        List<Consultant> consultants = getAllActiveConsultants();
        for (Consultant consultant : consultants) {
            List<ConsultantTimeDto> consultantRegisteredTime = getConsultantTimeDto(consultant.getId(), consultant.getTimekeeperId());
            registeredTimeService.saveConsultantTime(consultantRegisteredTime);
        }
    }

    private List<Consultant> getAllActiveConsultants() {
        return consultantRepository.findAllByActiveTrue();
    }

    private List<Long> checkTimekeeperUsersWithDatabase(List<TimekeeperUserDto> timekeeperUserResponseDto) {
        List<Long> idsToAdd = new ArrayList<>();
        List<Consultant> consultants = getAllConsultants();
        timekeeperUserResponseDto.forEach(tkUser -> {
            if (!consultantRepository.existsByTimekeeperId(tkUser.id())) {
                idsToAdd.add(tkUser.id());
            } else {
                consultants.stream()
                        .filter(consultant -> consultant.getTimekeeperId().equals(tkUser.id()))
                        .forEach(consultant -> {
                            if (consultant.isActive() != tkUser.isActive() || consultant.isActive() != tkUser.isEmployee()) {
                                consultant.setActive(tkUser.isActive() && tkUser.isEmployee());
                                consultantRepository.save(consultant);
                            }
                        });
            }
        });
        return idsToAdd;
    }

    private void createConsultant(Consultant consultant) {
        consultantRepository.save(consultant);
    }

    public Consultant findConsultantById(UUID id) {
        return consultantRepository.findById(id).orElse(null);
    }

    public ConsultantResponseDto findConsultantDtoById(UUID id) {
        Consultant consultant = findConsultantById(id);
        assert consultant != null;
        List<RegisteredTime> timeByConsultantId = registeredTimeService.getTimeByConsultantId(consultant.getId());
        List<RegisteredTimeDto> consultantTimeDto = getConsultantTimeDto(timeByConsultantId);
        return ConsultantResponseDto.toDto(consultant, consultantTimeDto);
    }

    public Double getConsultancyHoursByUserId(UUID id) {
        Consultant consultant = consultantRepository.findById(id).orElseThrow(() -> new ConsultantNotFoundException("Consultant not found"));
        List<TimekeeperRegisteredTimeResponseDto> consultancyTime = timekeeperClient.getTimeRegisteredByConsultant(consultant.getTimekeeperId());
        AtomicReference<Double> totalHoursResponse = new AtomicReference<>(0.0);
        consultancyTime.forEach(el -> totalHoursResponse.updateAndGet(v -> v + el.totalHours()));
        return totalHoursResponse.get();
    }

    public List<ConsultantTimeDto> getAllConsultantsTimeItems() {
        fetchDataFromTimekeeper();
        List<Consultant> consultants = getAllConsultants();
        List<RegisteredTime> consultantTimeDtoList = new ArrayList<>();
        for (Consultant consultant : consultants) {
            List<RegisteredTime> timeByConsultantId = registeredTimeService.getTimeByConsultantId(consultant.getId());
            consultantTimeDtoList.addAll(timeByConsultantId);
        }
        return consultantTimeDtoList
                .stream()
                .map(el -> new ConsultantTimeDto(
                        el.getId(),
                        el.getEndDate(),
                        el.getType(),
                        el.getTotalHours()))
                .toList();
    }


    private List<ConsultantTimeDto> getConsultantTimeDto(UUID consultantId, Long timekeeperId) {
        List<TimekeeperRegisteredTimeResponseDto> consultancyTime = timekeeperClient.getTimeRegisteredByConsultant(timekeeperId);
        List<ConsultantTimeDto> consultantTimeDtoList = new ArrayList<>();
        for (TimekeeperRegisteredTimeResponseDto item : consultancyTime) {
            consultantTimeDtoList.add(new ConsultantTimeDto(
                    new RegisteredTimeKey(consultantId, item.date().withHour(0).withMinute(0).withSecond(0)),
                    item.date().withHour(23).withMinute(59).withSecond(59),
                    item.activityName(),
                    //TODO overtime
                    8));
        }
        return consultantTimeDtoList;
    }
}