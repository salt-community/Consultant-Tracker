package com.example.backend.client.timekeeper.dto;

import com.example.backend.tag.Tag;

import java.util.List;

public record TimekeeperUserDto(String firstName,
                                String lastName,
                                String email,
                                String phone,
                                List<Tag> tags,
                                Long id,
                                boolean isActive,
                                String client,
                                String responsiblePT,
                                boolean isEmployee) {
}
