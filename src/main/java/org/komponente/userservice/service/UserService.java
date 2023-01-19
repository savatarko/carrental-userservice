package org.komponente.userservice.service;


import org.komponente.dto.client.ClientCreateDto;
import org.komponente.dto.client.ClientDto;
import org.komponente.dto.manager.ManagerCreateDto;
import org.komponente.dto.manager.ManagerDto;
import org.komponente.dto.rank.RankCreateDto;
import org.komponente.dto.rank.RankDto;
import org.komponente.dto.token.TokenRequestDto;
import org.komponente.dto.token.TokenResponseDto;
import org.komponente.dto.user.ChangeUserDto;
import org.komponente.dto.user.UserDto;
import org.komponente.userservice.domain.Rank;

import java.util.List;

public interface UserService {
    TokenResponseDto login(TokenRequestDto tokenRequestDto);

    ManagerDto add(ManagerCreateDto managerCreateDto);

    ClientDto add(ClientCreateDto clientCreateDto);

    void removeAccess(Long id);

    void giveAccess(Long id);

    RankDto add(RankCreateDto rankCreateDto);

    RankDto changeRank(Long id, RankCreateDto rankDto);

    Long getRank(Long userid);

    void changeAdmin(Long adminId, ChangeUserDto changeUserDto);
    void changeManager(Long managerId, ChangeUserDto changeUserDto);
    void changeClient(Long clientId, ChangeUserDto changeUserDto);
    void activateAccount(Long confirmation);
    Long getManagerCompanyId(Long id);
    UserDto findUser(Long id);
    String findClientMail(Long id);

    List<UserDto> getAllUsers();

    List<RankDto> getAllRanks();
    void confirmPassword(Long id);
}
