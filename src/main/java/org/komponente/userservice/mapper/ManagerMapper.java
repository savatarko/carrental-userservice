package org.komponente.userservice.mapper;

import org.komponente.dto.client.ClientCreateDto;
import org.komponente.dto.manager.ManagerCreateDto;
import org.komponente.dto.manager.ManagerDto;
import org.komponente.userservice.domain.Client;
import org.komponente.userservice.domain.Manager;
import org.komponente.userservice.security.PasswordSecurity;
import org.springframework.stereotype.Component;

@Component
public class ManagerMapper {
    public static ManagerDto managerToManagerDto(Manager client)
    {
        ManagerDto managerDto = new ManagerDto();
        managerDto.setId(client.getId());
        managerDto.setName(client.getName());
        managerDto.setSurname(client.getSurname());
        managerDto.setEmail(client.getEmail());
        managerDto.setNumber(client.getNumber());
        managerDto.setDateofbirth(client.getDateofbirth());
        managerDto.setUsername(client.getUsername());
        managerDto.setDateofbirth(client.getDateofemployment());
        managerDto.setCompanyId(client.getCompanyid());
        return managerDto;
    }
    public static Manager managerCreateDtoToManager(ManagerCreateDto clientCreateDto)
    {
        Manager manager = new Manager();
        manager.setName(clientCreateDto.getName());
        manager.setSurname(clientCreateDto.getSurname());
        manager.setNumber(clientCreateDto.getNumber());
        manager.setDateofbirth(clientCreateDto.getDateofbirth());
        //manager.setEmail(clientCreateDto.getLoginInfo().getEmail());
        //manager.setPassword(clientCreateDto.getLoginInfo().getPassword());
        manager.setEmail(clientCreateDto.getEmail());
        //manager.setPassword(clientCreateDto.getPassword());
        manager.setUsername(clientCreateDto.getUsername());
        manager.setHasaccess(true);
        manager.setTempPassword("");
        //manager.setHasaccess(false);
        manager.setCompanyid(clientCreateDto.getCompanyId());
        manager.setDateofemployment(clientCreateDto.getDateofemployment());
        manager.setPassword(PasswordSecurity.toHexString(PasswordSecurity.getSHA(clientCreateDto.getPassword())));
        manager.setConfirmTempPassword(0L);
        manager.setTempPassword("");
        return manager;
    }
}
