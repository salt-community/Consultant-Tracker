package com.example.backend.consultant.dto;

import com.example.backend.consultant.Consultant;
import com.example.backend.demo.demoConsultant.DemoConsultant;
import com.example.backend.meetings_schedule.dto.MeetingsDto;
import com.example.backend.redDay.CountryCode;

import java.util.List;
import java.util.UUID;

import static com.example.backend.utils.Country.SWEDEN;

public record SingleConsultantResponseListDto(UUID id,
                                              String fullName,
                                              String email,
                                              String phoneNumber,
                                              String responsiblePt,
                                              String client,
                                              String country,
                                              TotalDaysStatisticsDto totalDaysStatistics,
                                              List<ClientsListDto> clientsList,
                                              List<MeetingsDto> meetings) {

    public static SingleConsultantResponseListDto toDto(
            Consultant consultant,
            TotalDaysStatisticsDto totalDaysStatistics,
            List<ClientsListDto> clientsListDto,
            List<MeetingsDto> meetings) {
        return new SingleConsultantResponseListDto(
                consultant.getId(),
                consultant.getFullName(),
                consultant.getEmail(),
                consultant.getPhoneNumber(),
                consultant.getResponsiblePT(),
                consultant.getClient(),
                consultant.getCountry().equals(SWEDEN.country) ? CountryCode.SE.countryCode : CountryCode.NO.countryCode,
                totalDaysStatistics,
                clientsListDto,
                meetings
        );
    }

    public static SingleConsultantResponseListDto toDto(
            DemoConsultant consultant,
            TotalDaysStatisticsDto totalDaysStatistics,
            List<ClientsListDto> clientsListDto,
            List<MeetingsDto> meetings) {
        return new SingleConsultantResponseListDto(
                consultant.getId(),
                consultant.getFullName(),
                consultant.getEmail(),
                consultant.getPhoneNumber(),
                consultant.getResponsiblePT(),
                consultant.getClient(),
                consultant.getCountry().equals(SWEDEN.country) ? CountryCode.SE.countryCode : CountryCode.NO.countryCode,
                totalDaysStatistics,
                clientsListDto,
                meetings
        );
    }
}
