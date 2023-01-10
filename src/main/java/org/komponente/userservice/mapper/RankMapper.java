package org.komponente.userservice.mapper;

import org.komponente.dto.manager.ManagerCreateDto;
import org.komponente.dto.manager.ManagerDto;
import org.komponente.dto.rank.RankCreateDto;
import org.komponente.dto.rank.RankDto;
import org.komponente.userservice.domain.Manager;
import org.komponente.userservice.domain.Rank;
import org.springframework.stereotype.Component;

@Component
public class RankMapper {
    public static RankDto rankToRankDto(Rank rank)
    {
        RankDto rankDto = new RankDto();
        rankDto.setName(rank.getName());
        rankDto.setNumberofdays(rank.getNumberofdays());
        rankDto.setValue(rank.getValue());
        rankDto.setId(rank.getId());
        return rankDto;
    }
    public static Rank rankCreateDtoToRank(RankCreateDto rankCreateDto)
    {
        Rank rank = new Rank();
        rank.setValue(rankCreateDto.getValue());
        rank.setNumberofdays(rankCreateDto.getNumberofdays());
        rank.setName(rankCreateDto.getName());
        return rank;
    }
}
