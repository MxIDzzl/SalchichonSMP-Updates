package com.escuadronsalchichon.api.dto;

import java.util.List;

public record ClanDetailResponse(ClanSummaryResponse clan, List<ClanMemberResponse> members) {
    public ClanDetailResponse {
        members = List.copyOf(members);
    }
}
