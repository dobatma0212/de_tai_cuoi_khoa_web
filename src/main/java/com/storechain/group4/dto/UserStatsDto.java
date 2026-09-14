package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatsDto {
    private long totalUsers;
    private long activeUsers;
    private long lockedUsers;
    private long adminUsers;
    private long managerUsers;
    private long regularUsers;
}

